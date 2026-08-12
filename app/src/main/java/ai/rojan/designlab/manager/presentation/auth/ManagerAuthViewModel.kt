package ai.rojan.designlab.manager.presentation.auth

import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.AvailableSalon
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.domain.repository.availableSalons
import ai.rojan.designlab.domain.repository.toActiveSalonContext
import ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.domain.auth.ManagerOtpStep
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MANAGER_ROLE = "MANAGER"

/**
 * OTP Authentication Entry Flow Integration: owns the Manager App's entire
 * authentication lifecycle — cold-start session validation (the gate),
 * the OTP request/verify flow, and logout — reusing exclusively existing,
 * already-backend-integrated infrastructure:
 * [AuthSessionRepository] (persisted "who's logged in", same DataStore
 * mechanism [ai.rojan.designlab.presentation.session.SessionViewModel]
 * already established), [BackendAuthRepository] (real
 * `otp/request`/`otp/verify`/`me` calls, extended in this integration —
 * see that interface's own doc comment), and [TokenRepository] (same
 * encrypted JWT storage every other backend-integrated repository already
 * shares via `AuthInterceptor`/`TokenAuthenticator`). No new storage, no
 * new networking stack, no new authentication mechanism — only new
 * orchestration of what already exists, scoped to the Manager App and its
 * MANAGER-role requirement.
 *
 * Deliberately does **not** depend on
 * [ai.rojan.designlab.domain.identity.SessionProvider]/[ai.rojan.designlab.domain.identity.IdentityProvider]
 * the way [ai.rojan.designlab.presentation.auth.AuthViewModel] does — those
 * carry Customer's demo-only phone/OTP mock and role machinery, which is
 * disconnected from the real backend's role and would add nothing but risk
 * here (see [ManagerAuthState]'s own doc comment).
 */
class ManagerAuthViewModel(
    private val authSessionRepository: AuthSessionRepository,
    private val backendAuthRepository: BackendAuthRepository,
    private val tokenRepository: TokenRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
    private val activeSalonContextRepository: ActiveSalonContextRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<ManagerAuthState>(ManagerAuthState.Checking)
    val authState: StateFlow<ManagerAuthState> = _authState.asStateFlow()

    private val _otpStep = MutableStateFlow<ManagerOtpStep>(ManagerOtpStep.EnteringPhone)
    val otpStep: StateFlow<ManagerOtpStep> = _otpStep.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Identity & Session Architecture, Android Integration: the real
     * backend salon-access context - additive only this phase (Phase 6:
     * "do not immediately remove the existing Manager global-role check").
     * [authState]'s MANAGER-role gate is untouched; nothing reads this yet
     * to decide access. Fetched fresh on every real authentication
     * ([onVerified]) and every cold-start restore ([restoreSession]),
     * reset to [UiState.Loading] on [logout]/[clearSession] - never
     * persisted, never silently defaulted to a granted-looking state.
     */
    private val _identityContext =
        MutableStateFlow<UiState<CurrentUserIdentityContext>>(UiState.Loading)

    val identityContext: StateFlow<UiState<CurrentUserIdentityContext>> =
        _identityContext.asStateFlow()

    /**
     * Active Salon Context & Selection Flow - which salon this session
     * operates on, resolved from [identityContext] every time it refreshes
     * (see [resolveActiveSalon]). Reset to [ActiveSalonUiState.Loading] on
     * [logout]/[clearSession], same lifecycle as [identityContext].
     */
    private val _activeSalonState = MutableStateFlow<ActiveSalonUiState>(ActiveSalonUiState.Loading)

    val activeSalonState: StateFlow<ActiveSalonUiState> = _activeSalonState.asStateFlow()

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    /**
     * Cold-start gate check: a persisted personId is only a *claim* that a
     * session once existed — [BackendAuthRepository.currentUser] is the
     * real check, re-deriving identity from the currently stored access
     * token (transparently refreshed first if expired, via
     * `TokenAuthenticator`, exactly like
     * [ai.rojan.designlab.presentation.auth.AuthViewModel.restoreSession]
     * already does for Customer). A revoked/expired refresh token, a
     * network failure, or an account that isn't MANAGER-role all fail this
     * check the same way — back to [ManagerAuthState.Unauthenticated],
     * never a silent Dashboard entry.
     */
    private suspend fun restoreSession() {
        val personId = authSessionRepository.observePersonId().first()
        if (personId == null) {
            _authState.value = ManagerAuthState.Unauthenticated
            return
        }

        backendAuthRepository.currentUser()
            .onSuccess { user ->
                if (user.role == MANAGER_ROLE) {
                    _authState.value = ManagerAuthState.Authenticated(user.id, user.fullName)
                    refreshIdentityContext()
                } else {
                    // A real, valid session, but not a manager account — never
                    // grant Manager Dashboard access on session restore, same
                    // rule enforced fresh in verifyOtp() below.
                    clearSession()
                }
            }
            .onFailure { clearSession() }
    }

    /** `GET /users/me/salon-access` - see [ai.rojan.designlab.presentation.auth.AuthViewModel.refreshIdentityContext] for the identical Customer-side reasoning. A failure here never touches [authState] - the MANAGER-role check above already passed by the time this runs. */
    private suspend fun refreshIdentityContext() {
        _identityContext.value = UiState.Loading
        _activeSalonState.value = ActiveSalonUiState.Loading
        currentUserIdentityContextRepository.getCurrentUserIdentityContext()
            .onSuccess { context ->
                _identityContext.value = UiState.Success(context)
                resolveActiveSalon(context)
            }
            .onFailure { error -> _identityContext.value = UiState.Error(userMessageFor(error)) }
    }

    /**
     * Active Salon Context & Selection Flow: runs after every real identity
     * fetch (fresh login and cold-start restore both funnel through
     * [refreshIdentityContext]) - the one place this decision is made, per
     * the integration's own rule against inventing selection elsewhere.
     *
     * A previously-persisted salon id that's still among the available
     * options resolves straight to [ActiveSalonUiState.Active] without
     * prompting again - this is what "the selection may persist locally"
     * buys: the picker only appears when there's a real, unresolved choice
     * to make, not on every cold start. Exactly one available salon is
     * auto-selected and persisted the same way. More than one with no
     * valid persisted match requires [selectSalon].
     */
    private suspend fun resolveActiveSalon(context: CurrentUserIdentityContext) {
        val available = context.availableSalons()
        if (available.isEmpty()) {
            _activeSalonState.value = ActiveSalonUiState.Error("سالنی برای این حساب یافت نشد")
            return
        }

        val persistedSalonId = activeSalonContextRepository.observeActiveSalonId().first()
        val resolved = available.firstOrNull { it.salonId == persistedSalonId }
            ?: available.singleOrNull()

        if (resolved == null) {
            _activeSalonState.value = ActiveSalonUiState.SelectionRequired(available)
            return
        }

        if (resolved.salonId != persistedSalonId) {
            activeSalonContextRepository.saveActiveSalonId(resolved.salonId)
        }
        _activeSalonState.value = ActiveSalonUiState.Active(resolved.toActiveSalonContext())
    }

    /** Explicit user selection from the salon picker screen - only reachable while [activeSalonState] is [ActiveSalonUiState.SelectionRequired]. */
    fun selectSalon(salon: AvailableSalon) {
        viewModelScope.launch {
            activeSalonContextRepository.saveActiveSalonId(salon.salonId)
            _activeSalonState.value = ActiveSalonUiState.Active(salon.toActiveSalonContext())
        }
    }

    /** `POST /api/v1/auth/otp/request` — issues (or, called again, re-issues) a code for [phoneNumber]. No session exists yet; nothing is persisted here. */
    fun requestOtp(phoneNumber: String) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "شماره موبایل را وارد کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.requestOtp(trimmed)
                .onSuccess { issued ->
                    _otpStep.value = ManagerOtpStep.AwaitingCode(issued.phoneNumber, issued.canResendAfterSeconds)
                }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    /** `POST /api/v1/auth/otp/verify` — verifies [code] for the phone number captured in [ManagerOtpStep.AwaitingCode]. Only reachable from that step. */
    fun verifyOtp(code: String) {
        val step = _otpStep.value as? ManagerOtpStep.AwaitingCode ?: return
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            _errorMessage.value = "کد تایید را وارد کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.verifyOtp(step.phoneNumber, trimmedCode)
                .onSuccess { user -> onVerified(user) }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    private suspend fun onVerified(user: AuthenticatedUser) {
        if (user.role != MANAGER_ROLE) {
            // The backend auto-registers a brand-new phone number as
            // CUSTOMER (VerifyOtpUseCase's documented default) — the OTP
            // API itself has no way to request MANAGER at signup, and this
            // integration must not touch the backend to add one. A
            // successfully-verified but non-manager account is therefore a
            // real, expected outcome for an unrecognized number, not a bug
            // - reject it client-side rather than granting Dashboard
            // access to whatever role came back.
            tokenRepository.clearTokens()
            _errorMessage.value = "این شماره به عنوان مدیر ثبت نشده است"
            return
        }

        authSessionRepository.savePersonId(user.id)
        _authState.value = ManagerAuthState.Authenticated(user.id, user.fullName)
        refreshIdentityContext()
    }

    /** Requests a fresh code for the same phone number, same rate limits as [requestOtp] (backend-enforced, not duplicated here) — a resend is just another `otp/request` call, mirroring the backend's own `/otp/request`/`/otp/resend` equivalence. */
    fun resendOtp() {
        val step = _otpStep.value as? ManagerOtpStep.AwaitingCode ?: return
        requestOtp(step.phoneNumber)
    }

    /** Returns to the phone-entry step (e.g. "wrong number" back action) without touching any persisted session. */
    fun editPhoneNumber() {
        _errorMessage.value = null
        _otpStep.value = ManagerOtpStep.EnteringPhone
    }

    /** Discards the session (tokens + persisted identity) and returns to [ManagerAuthState.Unauthenticated]. */
    fun logout() {
        viewModelScope.launch {
            clearSession()
        }
    }

    private suspend fun clearSession() {
        tokenRepository.clearTokens()
        authSessionRepository.clearPersonId()
        activeSalonContextRepository.clearActiveSalonId()
        _otpStep.value = ManagerOtpStep.EnteringPhone
        _identityContext.value = UiState.Loading
        _activeSalonState.value = ActiveSalonUiState.Loading
        _authState.value = ManagerAuthState.Unauthenticated
    }
}
