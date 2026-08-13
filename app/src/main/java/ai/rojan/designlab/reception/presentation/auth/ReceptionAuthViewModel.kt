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
 * CONFIRMED — `ROJAN_System1_Backend_Decision_v2.md` §1b (`ROJAN_Backend`):
 * System 1 decided **no** new global `UserRole.RECEPTIONIST` will be
 * added. Reception, Manager, and Owner accounts all authenticate under the
 * existing `UserRole.MANAGER` value permanently — ownership is resolved
 * via `Salon.ownerId`, and per-salon role/authorization is resolved via
 * `SalonMembership.role` (see [ActiveSalonUiState]/`SalonAccessType.MEMBER`),
 * never the global role. This was previously flagged provisional in this
 * file (`ROJAN_Reception_Implementation_Plan_v1.md` §4 item 5, open at the
 * time); that question is now closed, and this constant's value was
 * correct all along — no code change was needed here, only this comment.
 */
private const val RECEPTION_GATE_ROLE = "MANAGER"

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
                if (user.role == RECEPTION_GATE_ROLE) {
                    _authState.value = ReceptionAuthState.Authenticated(user.id, user.fullName)
                    refreshIdentityContext()
                } else {
                    clearSession()
                }
            }
            .onFailure { clearSession() }
    }

    /**
     * `GET /users/me/salon-access` — see [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.refreshIdentityContext]
     * for the identical reasoning. Not yet real on the backend (plan §4 item
     * 1) — fails, does not silently grant access, until it is.
     *
     * Phase 1 (Authentication completion, resilient blocked-state UX): the
     * failure branch also resolves [_activeSalonState] to
     * [ActiveSalonUiState.Error] — previously left it stuck at
     * [ActiveSalonUiState.Loading] forever, which meant
     * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph]'s
     * `activeSalonPending` gate (waiting specifically for `Loading` to end)
     * never released, so a real `/salon-access` failure showed an infinite
     * splash screen with no error and no way out short of force-quitting.
     * Both state flows must reach a terminal, observable value on every
     * path — success or failure — or the gate that watches them can hang.
     */
    private suspend fun refreshIdentityContext() {
        _identityContext.value = UiState.Loading
        _activeSalonState.value = ActiveSalonUiState.Loading
        currentUserIdentityContextRepository.getCurrentUserIdentityContext()
            .onSuccess { context ->
                _identityContext.value = UiState.Success(context)
                resolveActiveSalon(context)
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

    private suspend fun onVerified(user: AuthenticatedUser) {
        if (user.role != RECEPTION_GATE_ROLE) {
            // See RECEPTION_GATE_ROLE's own doc comment — a successfully
            // verified but non-gated-role account is a real, expected
            // outcome, not a bug; rejected client-side rather than granting
            // Dashboard access.
            tokenRepository.clearTokens()
            _errorMessage.value = "این شماره برای پذیرش ثبت نشده است"
            return
        }

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
