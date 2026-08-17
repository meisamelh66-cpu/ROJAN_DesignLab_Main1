package ai.rojan.designlab.manager.domain.auth

/**
 * OTP Authentication Entry Flow Integration — the Manager App's
 * authentication gate state. Deliberately independent of
 * [ai.rojan.designlab.domain.identity.SessionState] (Customer's session
 * machinery, which still carries the retired phone/OTP *mock* concepts
 * this integration does not reuse) and of
 * [ai.rojan.designlab.domain.identity.SessionProvider]/[ai.rojan.designlab.domain.identity.IdentityProvider]
 * (demo-only, disconnected from the real backend's [ai.rojan.designlab.domain.repository.AuthenticatedUser.role] —
 * confirmed by [ai.rojan.designlab.presentation.auth.AuthViewModel.currentPersonRoles]'s
 * own doc comment). Framework-free by design, matching every other
 * domain-layer type in this codebase.
 */
sealed interface ManagerAuthState {

    /** Session restore in progress — the stored session (if any) is being validated against the backend. Splash stays visible for this state; nothing below the gate may render yet. */
    data object Checking : ManagerAuthState

    /**
     * A real backend session exists and was validated. Whether the
     * account actually has salon access (owner/membership/specialist) is
     * a separate question, tracked by
     * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.activeSalonState] —
     * not this state, and not the account's global role.
     */
    data class Authenticated(val userId: String, val fullName: String) : ManagerAuthState

    /** No valid session — either never logged in or the stored session failed validation. */
    data object Unauthenticated : ManagerAuthState
}

/**
 * The OTP entry screen's own step machine — separate from [ManagerAuthState]
 * because the gate only cares about the end result (authenticated or not),
 * while the entry screen needs to track which of the two OTP steps
 * (phone vs. code) is currently showing.
 */
sealed interface ManagerOtpStep {

    /** Awaiting a mobile number to request a code for. */
    data object EnteringPhone : ManagerOtpStep

    /** A code was just requested/resent for [phoneNumber]; awaiting the code the user received via SMS. */
    data class AwaitingCode(val phoneNumber: String, val canResendAfterSeconds: Long) : ManagerOtpStep
}
