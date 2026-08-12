package ai.rojan.designlab.presentation.auth

/**
 * Customer Authentication Migration: the Customer App's OTP entry screen's
 * own step machine, mirroring [ai.rojan.designlab.manager.domain.auth.ManagerOtpStep]'s
 * shape — kept as a separate, Customer-local type (not shared with Manager)
 * since [AuthViewModel] otherwise carries Customer-only concerns
 * ([ai.rojan.designlab.domain.identity.SessionState], the demo business-login
 * bridge) that don't belong on Manager's side, and this task's scope is
 * explicitly Customer-only.
 */
sealed interface CustomerOtpStep {

    /** Awaiting a mobile number to request a code for. */
    data object EnteringPhone : CustomerOtpStep

    /** A code was just requested/resent for [phoneNumber]; awaiting the code the user received via SMS. */
    data class AwaitingCode(val phoneNumber: String, val canResendAfterSeconds: Long) : CustomerOtpStep
}
