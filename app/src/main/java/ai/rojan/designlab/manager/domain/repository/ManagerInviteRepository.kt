package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.membership.SalonMemberRole

/**
 * `CREATED` (issued, not yet acted on) / `ACCEPTED` (terminal, success) /
 * `EXPIRED` (terminal) / `REVOKED` (terminal, owner cancelled before
 * acceptance) - mirrors the backend's real
 * `ai.rojan.backend.domain.salon.SalonInviteStatus` exactly (corrected from
 * this file's own earlier `PENDING` guess, written before the backend
 * shipped). Own copy, not imported from
 * [ai.rojan.designlab.reception.domain.repository.SalonInviteStatus] - same
 * "each flavor owns its own domain types" precedent already established
 * between Manager and Reception elsewhere in this codebase.
 */
enum class SalonInviteStatus {
    CREATED,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

/**
 * An invite the salon owner has issued - the Manager-side (issuing) half of
 * the flow Reception's `ReceptionInviteRepository` implements the accepting
 * half of. [token] is the real, single-use acceptance credential
 * (`POST /api/v1/invites/{token}/accept`) - never logged, matching every
 * other credential-shaped value in this codebase.
 */
data class SalonInvite(
    val id: String,
    val salonId: String,
    val role: SalonMemberRole,
    val token: String,
    val status: SalonInviteStatus,
    val expiresAt: String,
)

/**
 * Gap P0-3-REC-INVITE-UI: the real, already-shipped backend contract is
 * role-only and token/QR-based (`ai.rojan.backend.api.salon.SalonInviteController`/
 * `CreateSalonInviteUseCase`) - there has never been a `phoneNumber` field on
 * the create request; this interface's earlier signature guessed one before
 * the backend existed and is corrected here to match the real contract.
 * Owner-only on the backend (`Permission.MANAGE_MEMBERSHIP`, never granted to
 * `SalonRole.MANAGER`) - this app enforces nothing itself; a non-owner caller
 * gets a real 403 from [issueInvite], surfaced like any other failure.
 */
interface ManagerInviteRepository {

    suspend fun issueInvite(salonId: String, role: SalonMemberRole): Result<SalonInvite>
}
