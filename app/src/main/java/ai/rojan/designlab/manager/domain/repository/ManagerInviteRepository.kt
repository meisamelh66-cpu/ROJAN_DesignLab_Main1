package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.membership.SalonMemberRole

/**
 * `CREATED` (issued, not yet acted on — the only status [ManagerInviteRepository.revokeInvite]/
 * [ManagerInviteRepository.getInviteQrCode] work on) / `ACCEPTED` (terminal,
 * success) / `EXPIRED` (terminal — never a stored transition on the
 * backend; computed at read time from the invite's expiry, so the same
 * invite can report a different status across two consecutive
 * [ManagerInviteRepository.listInvites] calls with no mutation in
 * between) / `REVOKED` (terminal, owner cancelled before acceptance) —
 * confirmed exactly against the real backend `SalonInviteStatus` enum,
 * P1-6. Own copy, not imported from
 * [ai.rojan.designlab.reception.domain.repository.SalonInviteRepository] —
 * same "each flavor owns its own domain types" precedent already
 * established between Manager and Reception elsewhere in this codebase.
 */
enum class SalonInviteStatus {
    CREATED,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

/**
 * An invite the salon owner has issued — the Manager-side (issuing) half
 * of the flow Reception's `ReceptionInviteRepository`/
 * `ReceptionInviteAcceptanceViewModel` (P1-1) implements the accepting
 * half of. No [phoneNumber] field — confirmed against the real backend
 * `CreateSalonInviteRequest`/`SalonInviteResponse` (P1-6) that an invite is
 * anonymous and QR/link-redeemable, never targeted at a known contact;
 * this domain model previously assumed a phone-targeted shape ahead of the
 * backend actually existing, corrected here to match reality exactly, the
 * same class of pre-backend guess [ai.rojan.designlab.reception.domain.repository.SalonInviteAcceptance]
 * needed the same correction for in P1-1.
 */
data class SalonInvite(
    val id: String,
    val salonId: String,
    val role: SalonMemberRole,
    val status: SalonInviteStatus,
    val expiresAt: String,
    val createdAt: String,
)

/**
 * Real, backend-backed contract for the salon owner's staff-invite
 * management (create/list/revoke/QR) — `SalonInviteController`
 * (`POST`/`GET`/`DELETE /api/v1/salons/{salonId}/invites`,
 * `GET .../invites/{inviteId}/qr-code`) is confirmed live this sprint
 * (P1-6; previously documented here as a not-yet-built placeholder — that
 * has shipped). Every method is owner-only server-side
 * (`Permission.MANAGE_MEMBERSHIP`, never granted to `SalonRole.MANAGER`) —
 * a manager-role caller gets a real, backend-enforced `403`, surfaced
 * through the same generic error mapping every other access-denied
 * response in this codebase already uses; no client-side role gate exists
 * or is needed here.
 *
 * The complementary accepting half of this flow lives in
 * [ai.rojan.designlab.reception.domain.repository.ReceptionInviteRepository]
 * (P1-1) — entirely separate, untouched by this interface.
 *
 * See `ai.rojan.designlab.manager.data.BackendManagerInviteRepository` for
 * the implementation — [salonId] is baked into that implementation at
 * construction time (same convention
 * [ai.rojan.designlab.manager.data.BackendSpecialistRepository] already
 * established for P1-5), not a per-call parameter here.
 */
interface ManagerInviteRepository {

    suspend fun issueInvite(role: SalonMemberRole): Result<SalonInvite>

    suspend fun listInvites(): Result<List<SalonInvite>>

    suspend fun revokeInvite(inviteId: String): Result<Unit>

    /** Real PNG bytes encoding this invite's accept link — only succeeds while the invite is still [SalonInviteStatus.CREATED] (backend 404s otherwise, same rule as [revokeInvite]). Fetched on demand only — never prefetched for a whole list. */
    suspend fun getInviteQrCode(inviteId: String, sizePx: Int = 512): Result<ByteArray>
}
