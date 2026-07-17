package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.OtpVerificationResult
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.SessionProvider
import ai.rojan.designlab.domain.identity.SessionState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val sessionProvider: SessionProvider,
    private val identityProvider: IdentityProvider,
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
            sessionProvider.verifyOtp(
                code.trim()
            )
        ) {

            is OtpVerificationResult.InvalidCode -> {

                _errorMessage.value =
                    "کد وارد شده صحیح نیست"
            }


            is OtpVerificationResult.ExistingUser,
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


        sessionProvider.createFirstTimeUser(
            firstName
        )


        _sessionState.value =
            sessionProvider.currentSession()
    }


    fun logout() {

        sessionProvider.logout()

        _sessionState.value =
            sessionProvider.currentSession()
    }


    fun editPhoneNumber() {

        sessionProvider.logout()

        _errorMessage.value = null

        _sessionState.value =
            sessionProvider.currentSession()
    }


    private fun isValidPhoneNumber(
        phoneNumber: String
    ): Boolean =
        Regex("^09\\d{9}$")
            .matches(phoneNumber)
}