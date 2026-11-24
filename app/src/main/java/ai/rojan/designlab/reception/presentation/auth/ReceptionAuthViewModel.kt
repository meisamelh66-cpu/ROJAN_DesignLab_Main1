package ai.rojan.designlab.reception.presentation.auth

import ai.rojan.designlab.domain.phone.normalizeIranianPhoneNumber
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
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import ai.rojan.designlab.reception.domain.auth.ReceptionOtpStep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reception Authentication Fix (Phase 4): the real gate is an active
 * [ai.rojan.backend.domain.salon.SalonRole.RECEPTIONIST]-equivalent salon
 * membership, resolved server-side via `GET /users/me/salon-access`
 * ([CurrentUserIdentityContextRepository], already real and working -
 * see [refreshIdentityContext]'s corrected doc comment below) - never the
 * global `User.role`.
 *
 * Supersedes the prior approach of comparing [AuthenticatedUser.role] to
 * the literal string `"MANAGER"`: that check was wrong on its own terms -
 * every account is created via phone/OTP with a permanent global role of
 * `CUSTOMER` ([ai.rojan.backend.application.auth.VerifyOtpUseCase] - no
 * code path anywhere promotes it, accepting a [ai.rojan.backend.domain.salon.SalonInvite]
 * included), so it rejected every real OTP-created receptionist outright,
 * while granting nothing extra in return (the backend's own authorization
 * for booking endpoints has always been membership-based, never global-role-based).
 * Widening the old constant's value would still be wrong for the same
 * reason the task that introduced this fix forbids it: the global role was
 * never the right signal to gate on in the first place, in either direction.
 */
private const val RECEPTIONIST_ROLE = "RECEPTIONIST"

/**
 * Owns the Reception App's entire authentication lifecycle — cold-start
 * session validation (the gate), the OTP request/verify flow, and logout.
 * Structurally identical to
 * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel] (same
 * reused infrastructure: [AuthSessionRepository], [BackendAuthRepository],
 * [TokenRepository]) — kept as Reception's own class, not a shared/reused
 * one, so the two flavors' auth lifecycles can diverge independently once
 * §4 item 5 is actually resolved (e.g. a real receptionist-specific gate)
 * without touching Manager at all.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
class ReceptionAuthViewModel(
    private val authSessionRepository: AuthSessionRepository,
    private val backendAuthRepository: BackendAuthRepository,
    private val tokenRepository: TokenRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
    private val activeSalonContextRepository: ActiveSalonContextRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<ReceptionAuthState>(ReceptionAuthState.Checking)
    val authState: StateFlow<ReceptionAuthState> = _authState.asStateFlow()

    private val _otpStep = MutableStateFlow<ReceptionOtpStep>(ReceptionOtpStep.EnteringPhone)
    val otpStep: StateFlow<ReceptionOtpStep> = _otpStep.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _identityContext =
        MutableStateFlow<UiState<CurrentUserIdentityContext>>(UiState.Loading)

    val identityContext: StateFlow<UiState<CurrentUserIdentityContext>> =
        _identityContext.asStateFlow()

    private val _activeSalonState = MutableStateFlow<ActiveSalonUiState>(ActiveSalonUiState.Loading)

    val activeSalonState: StateFlow<ActiveSalonUiState> = _activeSalonState.asStateFlow()

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    /** Cold-start gate check — see [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.restoreSession] for the identical reasoning. */
    private suspend fun restoreSession() {
        val personId = authSessionRepository.observePersonId().first()
        if (personId == null) {
            _authState.value = ReceptionAuthState.Unauthenticated
            return
        }

        backendAuthRepository.currentUser()
            .onSuccess { user ->
                _authState.value = ReceptionAuthState.Authenticated(user.id, user.fullName)
                refreshIdentityContext()
            }
            .onFailure { clearSession() }
    }

    /**
     * `GET /users/me/salon-access` — real and working on the backend
     * (`ai.rojan.backend.api.user.UserController.salonAccess`, backed by
     * `ai.rojan.backend.application.salon.ResolveMySalonAccessUseCase`) -
     * see [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.refreshIdentityContext]
     * for the identical fetch/populate reasoning.
     *
     * Reception Authentication Fix (Phase 4): this is also the one place
     * the real Reception gate lives now — a fetch that *succeeds* but
     * shows no active [RECEPTIONIST_ROLE] membership anywhere is a genuine,
     * expected rejection (a plain customer, or a manager/owner with no
     * Reception membership of their own), not a bug — logged out with a
     * clear message rather than silently shown that account's owner/manager
     * salons. Applying the check here, not only at login, closes the gap
     * [retryIdentityResolution] would otherwise leave: a retry after a
     * transient failure must re-apply the same gate, never skip it.
     *
     * Phase 1 (Authentication completion, resilient blocked-state UX): a
     * genuine fetch *failure* (network/server error - membership truly
     * unknown, not "confirmed absent") still resolves [_activeSalonState]
     * to [ActiveSalonUiState.Error], deliberately leaving [_authState]
     * untouched (already [ReceptionAuthState.Authenticated] by the time
     * this runs) so [retryIdentityResolution] stays reachable — previously
     * leaving it stuck at [ActiveSalonUiState.Loading] forever meant
     * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph]'s
     * `activeSalonPending` gate never released. Both state flows must
     * reach a terminal, observable value on every path.
     */
    private suspend fun refreshIdentityContext() {
        _identityContext.value = UiState.Loading
        _activeSalonState.value = ActiveSalonUiState.Loading
        currentUserIdentityContextRepository.getCurrentUserIdentityContext()
            .onSuccess { context ->
                val isReceptionist = context.memberships.any { it.membershipActive && it.role == RECEPTIONIST_ROLE }
                if (isReceptionist) {
                    _identityContext.value = UiState.Success(context)
                    resolveActiveSalon(context)
                } else {
                    clearSession()
                    _errorMessage.value = "این حساب برای پذیرش هیچ سالنی ثبت نشده است"
                }
            }
            .onFailure { error ->
                val message = userMessageFor(error)
                _identityContext.value = UiState.Error(message)
                _activeSalonState.value = ActiveSalonUiState.Error(message)
            }
    }

    /** Re-attempts salon-access resolution from the [ActiveSalonUiState.Error] screen's retry action — a thin wrapper so that screen doesn't need to know [refreshIdentityContext] is private. */
    fun retryIdentityResolution() {
        viewModelScope.launch {
            refreshIdentityContext()
        }
    }

    /** Same reasoning as [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.resolveActiveSalon]. */
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

    /** Explicit user selection from the salon picker screen. */
    fun selectSalon(salon: AvailableSalon) {
        viewModelScope.launch {
            activeSalonContextRepository.saveActiveSalonId(salon.salonId)
            _activeSalonState.value = ActiveSalonUiState.Active(salon.toActiveSalonContext())
        }
    }

    /**
     * `POST /api/v1/auth/otp/request`. [phoneNumber] is normalized to
     * E.164 ([normalizeIranianPhoneNumber]) before being sent — see
     * [ai.rojan.designlab.presentation.auth.AuthViewModel.requestOtp]'s
     * identical doc comment for why (System2 Android Parallel Work, Phase
     * A item 1).
     */
    fun requestOtp(phoneNumber: String) {
        val trimmed = normalizeIranianPhoneNumber(phoneNumber)
        if (trimmed.isBlank()) {
            _errorMessage.value = "شماره موبایل را وارد کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.requestOtp(trimmed)
                .onSuccess { issued ->
                    _otpStep.value = ReceptionOtpStep.AwaitingCode(issued.phoneNumber, issued.canResendAfterSeconds)
                }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    /** `POST /api/v1/auth/otp/verify`. */
    fun verifyOtp(code: String) {
        val step = _otpStep.value as? ReceptionOtpStep.AwaitingCode ?: return
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

    /**
     * Reception Authentication Fix (Phase 4): unconditional, same shape as
     * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel]'s
     * own `onVerified` — the real Reception-vs-everyone-else decision now
     * lives entirely in [refreshIdentityContext], applied uniformly to
     * login, cold-start restore, and retry alike, so it can never drift
     * between those three call sites.
     */
    private suspend fun onVerified(user: AuthenticatedUser) {
        authSessionRepository.savePersonId(user.id)
        _authState.value = ReceptionAuthState.Authenticated(user.id, user.fullName)
        refreshIdentityContext()
    }

    /** Requests a fresh code for the same phone number. */
    fun resendOtp() {
        val step = _otpStep.value as? ReceptionOtpStep.AwaitingCode ?: return
        requestOtp(step.phoneNumber)
    }

    /** Returns to the phone-entry step without touching any persisted session. */
    fun editPhoneNumber() {
        _errorMessage.value = null
        _otpStep.value = ReceptionOtpStep.EnteringPhone
    }

    /** Discards the session (tokens + persisted identity) and returns to [ReceptionAuthState.Unauthenticated]. */
    fun logout() {
        viewModelScope.launch {
            clearSession()
        }
    }

    private suspend fun clearSession() {
        tokenRepository.clearTokens()
        authSessionRepository.clearPersonId()
        activeSalonContextRepository.clearActiveSalonId()
        _otpStep.value = ReceptionOtpStep.EnteringPhone
        _identityContext.value = UiState.Loading
        _activeSalonState.value = ActiveSalonUiState.Loading
        _authState.value = ReceptionAuthState.Unauthenticated
    }
}
