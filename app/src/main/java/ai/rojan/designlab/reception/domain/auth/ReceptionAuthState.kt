package ai.rojan.designlab.reception.domain.auth

/**
 * Reception App's authentication gate state — same shape as
 * [ai.rojan.designlab.manager.domain.auth.ManagerAuthState], its own type
 * rather than a shared/imported one so the Reception flavor stays as
 * independent of the Manager flavor's package as Manager already is of
 * Customer's (see that class's own doc comment on why it avoids Customer's
 * demo-only identity machinery). Framework-free by design, matching every
 * other domain-layer type in this codebase.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
sealed interface ReceptionAuthState {

    /** Session restore in progress — the stored session (if any) is being validated against the backend. Splash stays visible for this state. */
    data object Checking : ReceptionAuthState

    /** A real backend session exists and was validated. See [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]'s own doc comment for the provisional nature of which accounts pass this check today. */
    data class Authenticated(val userId: String, val fullName: String) : ReceptionAuthState

    /** No valid session — either never logged in, the stored session failed validation, or the authenticated account doesn't pass the role gate. */
    data object Unauthenticated : ReceptionAuthState
}

/**
 * The OTP entry screen's own step machine — separate from [ReceptionAuthState]
 * for the same reason [ai.rojan.designlab.manager.domain.auth.ManagerOtpStep]
 * is separate from `ManagerAuthState`.
 */
sealed interface ReceptionOtpStep {

    /** Awaiting a mobile number to request a code for. */
    data object EnteringPhone : ReceptionOtpStep

    /** A code was just requested/resent for [phoneNumber]; awaiting the code the user received via SMS. */
    data class AwaitingCode(val phoneNumber: String, val canResendAfterSeconds: Long) : ReceptionOtpStep
}
