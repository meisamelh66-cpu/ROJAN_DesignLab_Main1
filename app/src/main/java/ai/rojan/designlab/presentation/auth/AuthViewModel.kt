package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.MutableSessionProvider
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.SessionProvider
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.domain.identity.rolesForPersonAcrossAllSalons
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.phone.normalizeIranianPhoneNumber
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Customer Authentication Migration: Customer's only authentication method
 * is now phone number -> OTP -> session, via [BackendAuthRepository.requestOtp]/
 * [BackendAuthRepository.verifyOtp] — the same real, already-backend-integrated
 * OTP infrastructure [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel]
 * already uses for Manager. The email/password flow this class drove before
 * ([BackendAuthRepository.login]/[BackendAuthRepository.register]) is
 * retired from the Customer App entirely — those repository methods remain
 * (the backend explicitly keeps them live, and nothing else in this app
 * depends on them being removed), simply unused by this ViewModel now.
 *
 * [sessionProvider]/[identityProvider] are kept only for their still-live
 * [MutableSessionProvider.setSession]/[SessionProvider.logout] mechanics (a
 * real backend user id flows through the exact same session-state machinery
 * a demo person id used to) and for [currentPersonRoles]/[denyAccessAndLogout],
 * which back the business-login/staff-routing entry point — a separate,
 * still-demo-only, already-disabled feature this migration does not touch
 * (its entry point stays disabled in `RojanNavGraph.kt`).
 *
 * [authSessionRepository] persists which backend user id is logged in, so
 * [restoreSession] can survive a cold start. Session persistence is now
 * unconditional (no "Remember Me" concept) — see
 * [AuthSessionRepository]'s own doc comment.
 */
class AuthViewModel(
    private val sessionProvider: SessionProvider,
    private val identityProvider: IdentityProvider,
    private val authSessionRepository: AuthSessionRepository,
    private val backendAuthRepository: BackendAuthRepository,
    private val tokenRepository: TokenRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
) : ViewModel() {

    private val _sessionState =
        MutableStateFlow(sessionProvider.currentSession())

    val sessionState: StateFlow<SessionState> =
        _sessionState.asStateFlow()


    /** The OTP entry screen's own step (phone entry vs. awaiting code) — separate from [sessionState], which only cares about the end result. */
    private val _otpStep = MutableStateFlow<CustomerOtpStep>(CustomerOtpStep.EnteringPhone)
    val otpStep: StateFlow<CustomerOtpStep> = _otpStep.asStateFlow()


    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()


    private val _isSubmitting =
        MutableStateFlow(false)

    val isSubmitting: StateFlow<Boolean> =
        _isSubmitting.asStateFlow()


    /** The real backend account behind the current [SessionState.LoggedIn], if any — populated by [verifyOtp]/[restoreSession]. */
    private val _currentUser =
        MutableStateFlow<AuthenticatedUser?>(null)

    val currentUser: StateFlow<AuthenticatedUser?> =
        _currentUser.asStateFlow()


    /**
     * Identity & Session Architecture, Android Integration: the real
     * backend salon-access context, fetched fresh (never persisted to
     * disk) every time [onAuthenticated] runs - after a real OTP verify
     * and on every cold-start [restoreSession]. Additive to [sessionState]/
     * [currentUser], not a replacement - a failure here never touches
     * either of those. Starts (and resets on [logout]/[denyAccessAndLogout])
     * at [UiState.Loading], never a silently-granted empty success, so a
     * future guard built on this can safely treat "not yet [UiState.Success]"
     * as "no access" rather than needing its own separate not-loaded check.
     */
    private val _identityContext =
        MutableStateFlow<UiState<CurrentUserIdentityContext>>(UiState.Loading)

    val identityContext: StateFlow<UiState<CurrentUserIdentityContext>> =
        _identityContext.asStateFlow()


    val currentDisplayName: String?
        get() =
            _currentUser.value?.fullName
                ?: sessionProvider.currentPersonId()
                    ?.let { personId -> identityProvider.personById(personId)?.displayName }


    /**
     * Checks current user's role.
     *
     * If there is no active person or salon context,
     * user is not considered a customer.
     */
    fun isCurrentUserCustomer(): Boolean {

        val personId =
            sessionProvider.currentPersonId()
                ?: return false

        val salonId =
            sessionProvider.currentSalonId()
                ?: return false

        return PersonRole.CUSTOMER in
                identityProvider.rolesFor(
                    personId,
                    salonId
                )
    }


    /**
     * UX Refactor Phase 3: every [PersonRole] the current logged-in person
     * holds, across **every** salon — not just [SessionProvider.currentSalonId]'s
     * fixed reference salon. Backs the (now demo-only, entry-point-disabled)
     * business-login/staff-routing flow exclusively — a real backend user id
     * never matches any demo person, so this correctly yields an empty set
     * for every real session. Empty set if no one is logged in.
     */
    fun currentPersonRoles(): Set<PersonRole> {

        val personId =
            sessionProvider.currentPersonId()
                ?: return emptySet()

        return identityProvider.rolesForPersonAcrossAllSalons(personId)
    }


    /**
     * UX Refactor Phase 3: logs out and clears the persisted session — same
     * effect as [logout] — while also surfacing [message] via [errorMessage],
     * for a login attempt that succeeded at the identity layer but must
     * still be rejected one level up. Retained for the (now demo-only)
     * business-login denial path only.
     */
    fun denyAccessAndLogout(message: String) {

        sessionProvider.logout()

        viewModelScope.launch {
            authSessionRepository.clearPersonId()
        }

        _errorMessage.value = message
        _otpStep.value = CustomerOtpStep.EnteringPhone
        _identityContext.value = UiState.Loading

        _sessionState.value =
            sessionProvider.currentSession()
    }


    /**
     * `POST /api/v1/auth/otp/request` — issues (or, called again, re-issues)
     * a code for [phoneNumber]. No session exists yet; nothing is
     * persisted here.
     *
     * [phoneNumber] is normalized to E.164 ([normalizeIranianPhoneNumber])
     * before being sent — the backend rejects a local-format number
     * (`0912xxxxxxx`) outright with `400 INVALID_PHONE_NUMBER`, and a user
     * typing their number the way they naturally would (with the leading
     * `0`) is the common case, not an edge case.
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
                    _otpStep.value = CustomerOtpStep.AwaitingCode(issued.phoneNumber, issued.canResendAfterSeconds)
                }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    /**
     * `POST /api/v1/auth/otp/verify` — verifies [code] for the phone number
     * captured in [CustomerOtpStep.AwaitingCode]. Only reachable from that
     * step. [fullName], if provided, is used by the backend only the first
     * time this phone number completes verification (new-account creation)
     * and silently ignored for an existing account — see
     * `OtpVerifyRequestDto`'s own doc comment. This is the real,
     * backend-compatible replacement for the old (demo-only, unreachable,
     * now-deleted) `FirstTimeNameScreen`-driven name capture: there is no
     * server-side
     * "is this a new account" signal to gate a separate post-verify screen
     * on, and no profile-rename endpoint to fix a name after the fact, so
     * the name is collected inline, once, alongside the code — harmless to
     * leave blank for a returning user.
     */
    fun verifyOtp(code: String, fullName: String? = null) {
        val step = _otpStep.value as? CustomerOtpStep.AwaitingCode ?: return
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            _errorMessage.value = "کد تایید را وارد کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.verifyOtp(step.phoneNumber, trimmedCode, fullName?.trim()?.takeIf { it.isNotBlank() })
                .onSuccess { user -> onAuthenticated(user) }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    /** Requests a fresh code for the same phone number, same rate limits as [requestOtp] (backend-enforced, not duplicated here) — a resend is just another `otp/request` call. */
    fun resendOtp() {
        val step = _otpStep.value as? CustomerOtpStep.AwaitingCode ?: return
        requestOtp(step.phoneNumber)
    }

    /** Returns to the phone-entry step (e.g. "wrong number" back action) without touching any persisted session. */
    fun editPhoneNumber() {
        _errorMessage.value = null
        _otpStep.value = CustomerOtpStep.EnteringPhone
    }


    private suspend fun onAuthenticated(user: AuthenticatedUser) {
        _currentUser.value = user

        (sessionProvider as? MutableSessionProvider)?.setSession(
            personId = user.id,
            salonId = sessionProvider.currentSalonId(),
            organizationId = sessionProvider.currentOrganizationId(),
        )

        _sessionState.value = sessionProvider.currentSession()

        authSessionRepository.savePersonId(user.id)

        refreshIdentityContext()
    }

    /**
     * `GET /users/me/salon-access`, combined with the [user] identity
     * [onAuthenticated] already has - real OTP verify and every cold-start
     * [restoreSession] both funnel through here, matching "re-fetch context
     * during session restoration." A failure here is captured as
     * [UiState.Error] only - it never rolls back [sessionState]/[currentUser],
     * since `/users/me` (the real session check) already succeeded by the
     * time this runs.
     */
    private suspend fun refreshIdentityContext() {
        _identityContext.value = UiState.Loading
        currentUserIdentityContextRepository.getCurrentUserIdentityContext()
            .onSuccess { context -> _identityContext.value = UiState.Success(context) }
            .onFailure { error -> _identityContext.value = UiState.Error(userMessageFor(error)) }
    }


    /** Discards the real backend session (tokens + persisted identity) and reverts to [SessionState.LoggedOut] / [CustomerOtpStep.EnteringPhone]. */
    fun logout() {

        sessionProvider.logout()
        tokenRepository.clearTokens()
        _currentUser.value = null
        _errorMessage.value = null
        _otpStep.value = CustomerOtpStep.EnteringPhone
        _identityContext.value = UiState.Loading

        viewModelScope.launch {
            authSessionRepository.clearPersonId()
        }

        _sessionState.value =
            sessionProvider.currentSession()
    }


    /**
     * Cold-start restore. [personId] comes from [ai.rojan.designlab.presentation.session.SessionViewModel]'s
     * synchronous DataStore read — but the real source of truth here is
     * [BackendAuthRepository.currentUser], not [personId] itself: it
     * re-derives identity from the currently stored access token
     * (transparently refreshing it first if expired, via
     * `data/remote/TokenAuthenticator.kt`), so a revoked/expired refresh
     * token correctly fails restoration instead of trusting a stale local
     * id.
     *
     * Authentication Session Persistence fix: `suspend`, not fire-and-forget
     * `viewModelScope.launch` — the sole caller (`RojanNavGraph`'s cold-start
     * gate) needs to await real completion before deciding a start
     * destination, rather than optimistically routing to `CUSTOMER_HOME`
     * before this resolves (the confirmed bug this fixes: an expired/
     * revoked refresh token used to still land the user on the Dashboard
     * for one frame before things silently started failing).
     */
    suspend fun restoreSession(personId: String) {
        backendAuthRepository.currentUser()
            .onSuccess { user -> onAuthenticated(user) }
            .onFailure {
                tokenRepository.clearTokens()
                authSessionRepository.clearPersonId()
                _identityContext.value = UiState.Loading
            }
    }
}
