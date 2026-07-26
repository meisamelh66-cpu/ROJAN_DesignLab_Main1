package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.MutableSessionProvider
import ai.rojan.designlab.domain.identity.OtpVerificationResult
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.SessionProvider
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UX Refactor Phase 2: [authSessionRepository] persists which
 * [ai.rojan.designlab.domain.identity.PersonIdentity] is logged in, so
 * [SessionProvider]'s otherwise in-memory-only state survives a cold
 * start via [restoreSession] — see that method's doc comment.
 */
class AuthViewModel(
    private val sessionProvider: SessionProvider,
    private val identityProvider: IdentityProvider,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val _sessionState =
        MutableStateFlow(sessionProvider.currentSession())

    val sessionState: StateFlow<SessionState> =
        _sessionState.asStateFlow()


    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()


    val currentDisplayName: String?
        get() =
            sessionProvider.currentPersonId()
                ?.let { personId ->
                    identityProvider.personById(personId)?.displayName
                }


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


    fun submitPhoneNumber(
        rawPhoneNumber: String
    ) {

        val phoneNumber =
            rawPhoneNumber.trim()


        if (!isValidPhoneNumber(phoneNumber)) {

            _errorMessage.value =
                "شماره موبایل معتبر نیست"

            return
        }


        _errorMessage.value = null


        sessionProvider.login(
            phoneNumber = phoneNumber
        )


        _sessionState.value =
            sessionProvider.currentSession()
    }


    fun submitOtp(
        code: String
    ) {

        when (
            val result = sessionProvider.verifyOtp(
                code.trim()
            )
        ) {

            is OtpVerificationResult.InvalidCode -> {

                _errorMessage.value =
                    "کد وارد شده صحیح نیست"
            }


            is OtpVerificationResult.ExistingUser -> {

                _errorMessage.value = null

                // UX Refactor Phase 2: persist so the next cold start
                // restores this session instead of requiring OTP again.
                viewModelScope.launch {
                    authSessionRepository.savePersonId(result.personId)
                }
            }


            is OtpVerificationResult.FirstTimeUser -> {

                _errorMessage.value = null
            }
        }


        _sessionState.value =
            sessionProvider.currentSession()
    }


    fun submitFirstName(
        rawFirstName: String
    ) {

        val firstName =
            rawFirstName.trim()


        if (firstName.isBlank()) {

            _errorMessage.value =
                "نام را وارد کنید"

            return
        }


        _errorMessage.value = null


        val newPerson = sessionProvider.createFirstTimeUser(
            firstName
        )

        // Same persistence as the existing-user OTP branch above — a
        // first-time user is just as much a returning customer next time.
        viewModelScope.launch {
            authSessionRepository.savePersonId(newPerson.id)
        }


        _sessionState.value =
            sessionProvider.currentSession()
    }


    fun logout() {

        sessionProvider.logout()

        viewModelScope.launch {
            authSessionRepository.clearPersonId()
        }

        _sessionState.value =
            sessionProvider.currentSession()
    }


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
     * UX Refactor Phase 2: hydrates [sessionState] to [SessionState.LoggedIn]
     * for a [personId] restored from persistence on a cold start — bypasses
     * OTP entirely, since this phone number was already verified in a
     * previous session. Synchronous (no suspend calls): [sessionProvider]
     * is [MutableSessionProvider], so this is a plain in-memory update, safe
     * to call from a [androidx.compose.runtime.LaunchedEffect] with no
     * suspend point before it takes effect.
     */
    fun restoreSession(personId: String) {

        (sessionProvider as? MutableSessionProvider)?.setSession(
            personId = personId,
            salonId = sessionProvider.currentSalonId(),
            organizationId = sessionProvider.currentOrganizationId(),
        )

        _sessionState.value =
            sessionProvider.currentSession()
    }


    private fun isValidPhoneNumber(
        phoneNumber: String
    ): Boolean =
        Regex("^09\\d{9}$")
            .matches(phoneNumber)
}