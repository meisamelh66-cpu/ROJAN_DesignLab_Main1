package ai.rojan.designlab.data.identity

import ai.rojan.designlab.domain.identity.IdentityIdFormat
import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.MutableSessionProvider
import ai.rojan.designlab.domain.identity.OtpVerificationResult
import ai.rojan.designlab.domain.identity.PersonIdentity
import ai.rojan.designlab.domain.identity.SessionState

/**
 * Infrastructure implementation — Identity Foundation Refinement (Patch 2),
 * evolved into a genuinely stateful Mock Authentication Provider for the
 * Booking Experience Refactor.
 *
 * Real (if mock) behavior, not stubs: [login] moves state to
 * [SessionState.AwaitingOtp]; [verifyOtp] checks the code against a
 * fixed mock value and looks up [phoneNumber] via [identityProvider] to
 * decide existing-user vs. first-time; [createFirstTimeUser] genuinely
 * registers a new [PersonIdentity] through [identityProvider] (the
 * single source of truth for persons, and the sole owner of ID
 * assignment — this class never constructs a [PersonIdentity] or casts
 * [identityProvider] to a concrete type; [SessionProvider] depends only
 * on the [IdentityProvider] interface throughout); [logout] genuinely
 * clears state.
 *
 * [currentPersonId] now reflects the actual logged-in person (`null`
 * when logged out) — a real change from the previous fixed-Owner
 * behavior. [currentSalonId]/[currentOrganizationId] remain fixed
 * reference values regardless of login state — this app's Customer
 * booking journey isn't about switching between salons-as-tenant, so
 * this is a deliberate, disclosed simplification, not an oversight.
 *
 * A future `BackendSessionProvider` implements the same [ai.rojan.designlab.domain.identity.SessionProvider]
 * interface with a real OTP service and real backend-issued sessions —
 * nothing above this class (UI, ViewModels, Navigation, Booking Journey)
 * needs to change when that swap happens.
 *
 * UX Refactor Phase 2: implements [MutableSessionProvider] (previously
 * unimplemented anywhere) so a cold-start restore can set [state]
 * directly to [SessionState.LoggedIn] for a previously-verified phone
 * number, without re-running the OTP flow — see
 * [ai.rojan.designlab.presentation.auth.AuthViewModel.restoreSession].
 */
class DemoSessionProvider(
    private val identityProvider: IdentityProvider,
) : MutableSessionProvider {

    private var state: SessionState = SessionState.LoggedOut

    override fun currentPersonId(): String? =
        (state as? SessionState.LoggedIn)?.personId

    override fun currentSalonId(): String = IdentityIdFormat.salonId(1)
    override fun currentOrganizationId(): String = "RJ-ORG-000001"

    override fun currentSession(): SessionState = state

    override fun login(phoneNumber: String) {
        state = SessionState.AwaitingOtp(phoneNumber)
    }

    override fun verifyOtp(code: String): OtpVerificationResult {
        val awaiting = state as? SessionState.AwaitingOtp
            ?: return OtpVerificationResult.InvalidCode

        if (code != MOCK_OTP_CODE) return OtpVerificationResult.InvalidCode

        val existing = identityProvider.personByPhone(awaiting.phoneNumber)
        return if (existing != null) {
            state = SessionState.LoggedIn(existing.id)
            OtpVerificationResult.ExistingUser(existing.id)
        } else {
            state = SessionState.AwaitingFirstName(awaiting.phoneNumber)
            OtpVerificationResult.FirstTimeUser
        }
    }

    override fun createFirstTimeUser(firstName: String): PersonIdentity {
        val awaiting = state as? SessionState.AwaitingFirstName
            ?: error("createFirstTimeUser called outside SessionState.AwaitingFirstName")

        val newPerson = identityProvider.registerPerson(
            displayName = firstName,
            verifiedMobile = awaiting.phoneNumber,
        )
        state = SessionState.LoggedIn(newPerson.id)
        return newPerson
    }

    override fun logout() {
        state = SessionState.LoggedOut
    }

    // salonId/organizationId are accepted for interface-shape compatibility
    // but intentionally unused here, same as currentSalonId()/currentOrganizationId()
    // above — this class's fixed reference values don't vary by session.
    override fun setSession(personId: String, salonId: String?, organizationId: String?) {
        state = SessionState.LoggedIn(personId)
    }

    override fun clearSession() {
        state = SessionState.LoggedOut
    }

    private companion object {
        /** DEMO CONFIGURATION — not a real OTP service. Any real backend replaces this with actual SMS delivery + verification. */
        const val MOCK_OTP_CODE = "1234"
    }
}
