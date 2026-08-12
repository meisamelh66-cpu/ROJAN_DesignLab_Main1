package ai.rojan.designlab.domain.identity

/**
 * Identity & Session Architecture Cleanup: "who is currently logged in"
 * lives here exclusively; [IdentityProvider] only resolves IDs into
 * records, it never decides which one is current.
 *
 * [DemoSessionProvider] is the only implementation. Real backend sessions
 * (Customer/Manager OTP) already flow through it today via
 * [MutableSessionProvider.setSession]/`.logout()` — a real backend user id
 * plays the same role a demo person id originally did, so no
 * `BackendSessionProvider` was ever needed. The mock phone/OTP login this
 * interface originally modeled directly (`login`/`verifyOtp`/
 * `createFirstTimeUser`) was removed once nothing called it any more — see
 * `AuthViewModel`/`AuthScreen` for the real OTP flow.
 */
interface SessionProvider {
    fun currentPersonId(): String?
    fun currentSalonId(): String?
    fun currentOrganizationId(): String?

    /** Current auth state — [SessionState.LoggedOut] until a real login (see [MutableSessionProvider.setSession]) sets it. */
    fun currentSession(): SessionState

    /** Clears the session back to [SessionState.LoggedOut]. */
    fun logout()
}
