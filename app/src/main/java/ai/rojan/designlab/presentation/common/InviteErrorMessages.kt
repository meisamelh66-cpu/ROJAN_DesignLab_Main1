package ai.rojan.designlab.presentation.common

import ai.rojan.designlab.data.remote.BackendApiException

/**
 * Backend error mapping preparation for the approved Invite flow
 * (`ROJAN_System1_Backend_Decision_v2.md` §2) — maps the specific error
 * codes that decision approved (`INVITE_EXPIRED`, `INVITE_ALREADY_ACCEPTED`,
 * `INVITE_REVOKED`, `INVITE_PHONE_MISMATCH`) to distinct Persian messages,
 * the same pattern already established by
 * [ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel]'s
 * own `bookingErrorMessage` for booking-specific codes. Falls back to the
 * shared [userMessageFor] for anything else (a plain `404` for an unknown
 * token, or a network/timeout/other failure) rather than re-deriving that
 * generic classification here.
 *
 * **Shared, not duplicated per app** — an invite error code means the same
 * thing regardless of whether Manager (issuing, `ManagerInviteRepository`)
 * or Reception (accepting, `ReceptionInviteRepository`) is the caller, so
 * this is one function serving both, not two per-flavor copies.
 *
 * **Preparation only — not yet called from any screen or ViewModel.**
 * Neither `ManagerInviteRepository` nor `ReceptionInviteRepository` has a
 * real implementation yet (no backend `InviteController` — re-confirmed
 * absent on `origin/feature/auth-rate-limit-finalization`), so there is
 * nothing to wire this into today. It exists so the mapping is typed,
 * reviewed, and ready ahead of that work — not invented later under time
 * pressure once the backend ships. A future `InviteViewModel`'s
 * `onFailure { error -> ... }` calling this instead of the generic
 * [userMessageFor] is the only wiring this needs.
 */
fun inviteErrorMessage(error: Throwable): String {
    val apiError = (error as? BackendApiException)?.apiError
    return when (apiError?.errorCode) {
        "INVITE_EXPIRED" -> "این دعوت‌نامه منقضی شده است."
        "INVITE_ALREADY_ACCEPTED" -> "این دعوت‌نامه قبلاً پذیرفته شده است."
        "INVITE_REVOKED" -> "این دعوت‌نامه توسط مالک سالن لغو شده است."
        "INVITE_PHONE_MISMATCH" -> "این دعوت‌نامه برای شماره موبایل دیگری صادر شده است."
        else -> userMessageFor(error)
    }
}
