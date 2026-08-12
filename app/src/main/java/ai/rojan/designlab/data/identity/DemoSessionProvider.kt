package ai.rojan.designlab.data.identity

import ai.rojan.designlab.domain.identity.IdentityIdFormat
import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.MutableSessionProvider
import ai.rojan.designlab.domain.identity.SessionState

/**
 * Infrastructure implementation of [MutableSessionProvider] — now a thin
 * session-state holder, not a mock authentication provider. Real
 * authentication (Customer/Manager OTP) lives entirely in
 * [ai.rojan.designlab.presentation.auth.AuthViewModel]/
 * `ManagerAuthViewModel` against the real backend; this class's only job
 * is holding [state] and reacting to [setSession]/[logout] the same way
 * regardless of which real flow authenticated the user.
 *
 * Identity & Session Architecture Cleanup: the mock phone/OTP methods this
 * class used to implement directly (`login`/`verifyOtp`/
 * `createFirstTimeUser`) are removed — confirmed zero real callers
 * remained once `AuthScreen`/`AuthViewModel` moved to the real backend OTP
 * flow. [identityProvider] is kept only for constructor-shape
 * compatibility with existing call sites; nothing in this class currently
 * reads it.
 *
 * [currentPersonId] reflects the actual logged-in person (`null` when
 * logged out). [currentSalonId]/[currentOrganizationId] remain fixed
 * reference values regardless of login state — this app's Customer
 * booking journey isn't about switching between salons-as-tenant, so
 * this is a deliberate, disclosed simplification, not an oversight.
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
}
