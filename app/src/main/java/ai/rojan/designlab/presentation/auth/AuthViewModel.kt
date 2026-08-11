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
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android <-> Backend Full Integration milestone: the phone/OTP flow this
 * class used to drive ([DemoSessionProvider]'s mock OTP mechanism) is
 * retired in favor of the real backend's email/password auth
 * ([BackendAuthRepository]) — the backend has no phone/OTP concept at all.
 * [sessionProvider]/[identityProvider] are kept only for their still-live
 * [MutableSessionProvider.setSession]/[SessionProvider.logout] mechanics
 * (a real backend user id flows through the exact same session-state
 * machinery a demo person id used to) and for [currentPersonRoles] /
 * [denyAccessAndLogout], which back the business-login/staff-routing
 * entry point — a separate, still-demo-only feature this milestone
 * intentionally does not touch (its entry point is disabled elsewhere,
 * in `RojanNavGraph.kt`, rather than this class being reworked around it).
 *
 * [authSessionRepository] persists which backend user id is logged in
 * (now a real UUID, not a demo person id) and the "Remember Me" choice,
 * so [restoreSession] can survive a cold start — see that method's own
 * doc comment for how the two interact.
 */
class AuthViewModel(
    private val sessionProvider: SessionProvider,
    private val identityProvider: IdentityProvider,
    private val authSessionRepository: AuthSessionRepository,
    private val backendAuthRepository: BackendAuthRepository,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _sessionState =
        MutableStateFlow(sessionProvider.currentSession())

    val sessionState: StateFlow<SessionState> =
        _sessionState.asStateFlow()


    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()


    private val _isSubmitting =
        MutableStateFlow(false)

    val isSubmitting: StateFlow<Boolean> =
        _isSubmitting.asStateFlow()


    /** The real backend account behind the current [SessionState.LoggedIn], if any — populated by [login]/[register]/[restoreSession]. */
    private val _currentUser =
        MutableStateFlow<AuthenticatedUser?>(null)

    val currentUser: StateFlow<AuthenticatedUser?> =
        _currentUser.asStateFlow()


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

        _sessionState.value =
            sessionProvider.currentSession()
    }


    /** Real `POST /api/v1/auth/login` — persists the returned tokens (via [BackendAuthRepository]/[TokenRepository]) and flips [sessionState] to [SessionState.LoggedIn]. */
    fun login(email: String, password: String, rememberMe: Boolean) {

        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            _errorMessage.value = "ایمیل و رمز عبور را وارد کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.login(trimmedEmail, password)
                .onSuccess { user -> onAuthenticated(user, rememberMe) }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }


    /** Real `POST /api/v1/auth/register` — registration alone returns no tokens (see [BackendAuthRepository.register]), so a successful register immediately chains into [login] rather than leaving the user stranded on a "registered but not logged in" state. */
    fun register(email: String, password: String, fullName: String, rememberMe: Boolean) {

        val trimmedEmail = email.trim()
        val trimmedName = fullName.trim()

        if (trimmedEmail.isBlank() || password.isBlank() || trimmedName.isBlank()) {
            _errorMessage.value = "همه فیلدها را تکمیل کنید"
            return
        }

        _errorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            backendAuthRepository.register(trimmedEmail, password, trimmedName)
                .onSuccess {
                    backendAuthRepository.login(trimmedEmail, password)
                        .onSuccess { user -> onAuthenticated(user, rememberMe) }
                        .onFailure { error -> _errorMessage.value = userMessageFor(error) }
                }
                .onFailure { error -> _errorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }


    private suspend fun onAuthenticated(user: AuthenticatedUser, rememberMe: Boolean) {
        _currentUser.value = user

        (sessionProvider as? MutableSessionProvider)?.setSession(
            personId = user.id,
            salonId = sessionProvider.currentSalonId(),
            organizationId = sessionProvider.currentOrganizationId(),
        )

        _sessionState.value = sessionProvider.currentSession()

        authSessionRepository.savePersonId(user.id)
        authSessionRepository.saveRememberMe(rememberMe)
    }


    /**
     * Discards the real backend session (tokens + persisted identity) and
     * reverts to [SessionState.LoggedOut]. Security Review fix: also resets
     * the persisted "Remember Me" flag — previously left at its prior
     * value, which was harmless only because [authSessionRepository]'s
     * `observePersonId()` already returns `null` whenever `personId` itself
     * is cleared (as it is here) regardless of this flag, but a future
     * caller reading "Remember Me" independently of `personId` should not
     * see a stale `true` surviving an explicit logout — "no cached previous
     * account data" applies to this flag too, not just the identity itself.
     */
    fun logout() {

        sessionProvider.logout()
        tokenRepository.clearTokens()
        _currentUser.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            authSessionRepository.clearPersonId()
            authSessionRepository.saveRememberMe(false)
        }

        _sessionState.value =
            sessionProvider.currentSession()
    }


    /**
     * Retained for [ai.rojan.designlab.screens.auth.FirstTimeNameScreen] —
     * that screen only ever composes for a demo-only, phone-verified
     * [SessionState.AwaitingFirstName], which nothing in the real
     * email/password flow produces any more (see this class's own doc
     * comment). Left in place, unreachable, rather than ripped out, to
     * keep this milestone's change surgical.
     */
    fun submitFirstName(rawFirstName: String) {

        val firstName = rawFirstName.trim()

        if (firstName.isBlank()) {
            _errorMessage.value = "نام را وارد کنید"
            return
        }

        _errorMessage.value = null

        val createdPerson = sessionProvider.createFirstTimeUser(firstName)

        viewModelScope.launch {
            authSessionRepository.savePersonId(createdPerson.id)
        }

        _sessionState.value = sessionProvider.currentSession()
    }


    /** Retained for [ai.rojan.designlab.screens.auth.FirstTimeNameScreen]'s back button — see [submitFirstName]'s doc comment. */
    fun editPhoneNumber() {

        sessionProvider.logout()

        viewModelScope.launch {
            authSessionRepository.clearPersonId()
        }

        _errorMessage.value = null

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
     * id. "Remember Me" is enforced upstream, inside
     * [ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl.observePersonId] —
     * it never emits a [personId] at all when the last session wasn't
     * marked to be remembered, so this method is simply never called in
     * that case.
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
            .onSuccess { user -> onAuthenticated(user, rememberMe = true) }
            .onFailure {
                tokenRepository.clearTokens()
                authSessionRepository.clearPersonId()
            }
    }
}
