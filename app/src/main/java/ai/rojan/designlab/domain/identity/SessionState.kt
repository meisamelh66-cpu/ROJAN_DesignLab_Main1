package ai.rojan.designlab.domain.identity

/**
 * Booking Experience Refactor — Authentication (Mock). The current
 * state of the session's auth flow. This is genuinely mutable state
 * (unlike the rest of the Identity Foundation, which was read-only) —
 * introduced specifically because real login/logout requires it.
 */
sealed interface SessionState {
    /** No one is logged in. */
    data object LoggedOut : SessionState

    /** Phone number submitted, OTP has been "sent" (mocked) — awaiting the code. */
    data class AwaitingOtp(val phoneNumber: String) : SessionState

    /** OTP verified, but this phone number has no existing [PersonIdentity] — first name still needed. */
    data class AwaitingFirstName(val phoneNumber: String) : SessionState

    /** Fully authenticated. */
    data class LoggedIn(val personId: String) : SessionState
}

/** Result of [SessionProvider.verifyOtp] — distinguishes existing-user vs. first-time paths without the caller needing to inspect [SessionState] directly. */
sealed interface OtpVerificationResult {
    data class ExistingUser(val personId: String) : OtpVerificationResult
    data object FirstTimeUser : OtpVerificationResult
    data object InvalidCode : OtpVerificationResult
}
