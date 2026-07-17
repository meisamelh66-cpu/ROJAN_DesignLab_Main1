package ai.rojan.designlab.domain.identity

/**
 * Identity Foundation Refinement (Patch 2): "IdentityEngine must never
 * decide who the current user is" — that decision belongs exclusively
 * here. [IdentityEngine] asks this interface for the active IDs, then
 * asks [IdentityProvider] to resolve those IDs into real records.
 *
 * Booking Experience Refactor — Authentication (Mock): this interface
 * evolved from a static, read-only session into a genuinely mutable
 * one. This is a deliberate architectural decision, not scope creep —
 * the alternative (a separate parallel Auth layer) would duplicate
 * Session logic the Identity Foundation already owns. [DemoSessionProvider]
 * is the only implementation today; a future `BackendSessionProvider`
 * implements this same interface, and nothing above this interface
 * (UI, ViewModels, Navigation, Booking Journey) needs to change when
 * that swap happens.
 */
interface SessionProvider {
    fun currentPersonId(): String?
    fun currentSalonId(): String?
    fun currentOrganizationId(): String?

    /** Current auth state — [SessionState.LoggedOut] until [login] succeeds through to [verifyOtp]/[createFirstTimeUser]. */
    fun currentSession(): SessionState

    /** Mock: "sends" an OTP for [phoneNumber] — always succeeds, moves state to [SessionState.AwaitingOtp]. */
    fun login(phoneNumber: String)

    /** Mock: verifies the previously-sent OTP. Real behavior (not a stub): checks [code] against a fixed mock code, and — if correct — checks whether [phoneNumber] matches an existing [PersonIdentity] via the (separately supplied) lookup. */
    fun verifyOtp(code: String): OtpVerificationResult

    /** Only valid when [currentSession] is [SessionState.AwaitingFirstName] — creates a new in-memory [PersonIdentity] for this run, moves state to [SessionState.LoggedIn]. */
    fun createFirstTimeUser(firstName: String): PersonIdentity

    /** Clears the session back to [SessionState.LoggedOut]. */
    fun logout()
}
