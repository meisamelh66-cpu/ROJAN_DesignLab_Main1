package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.membership.SalonMemberRole

/**
 * `PENDING` (issued, not yet acted on) / `ACCEPTED` (terminal, success) /
 * `EXPIRED` (terminal) / `REVOKED` (terminal, owner cancelled before
 * acceptance) — approved states, `ROJAN_System1_Backend_Decision_v2.md`
 * §2. Own copy, not imported from
 * [ai.rojan.designlab.reception.domain.repository.SalonInviteStatus] —
 * same "each flavor owns its own domain types" precedent already
 * established between Manager and Reception elsewhere in this codebase.
 */
enum class SalonInviteStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

/** An invite the salon owner has issued — the Manager-side (issuing) half of the flow Reception's `ReceptionInviteRepository` implements the accepting half of. */
data class SalonInvite(
    val id: String,
    val salonId: String,
    val phoneNumber: String,
    val role: SalonMemberRole,
    val status: SalonInviteStatus,
)

/**
 * **Integration placeholder only — not backed by a real network call.**
 * `ROJAN_System1_Backend_Decision_v2.md` §2 approved the invite flow's
 * shape, including `POST /api/v1/salons/{salonId}/invites` (owner-only
 * issue) — but the backend (`InviteController` / `SalonInvite` domain /
 * `salon_invites` persistence) does not exist yet, re-confirmed absent on
 * `origin/feature/auth-rate-limit-finalization` at the time of this
 * interface's creation (System2 Android Parallel Work, Phase C). No
 * `InviteApi` Retrofit interface, no implementation, and no DI wiring
 * exist for this — same reasoning as
 * [ai.rojan.designlab.reception.domain.repository.ReceptionInviteRepository]'s
 * own doc comment, which this interface is the issuing-side counterpart
 * to: adding a Retrofit method against a route that 404s at runtime would
 * be worse than no binding at all.
 *
 * This interface exists purely so the domain-layer contract is captured
 * and typed ahead of the backend — a future `BackendManagerInviteRepository`
 * implementing it is the only change needed once `InviteController` ships.
 *
 * **No UI is built against this** — no screen, no ViewModel, no nav route.
 * Preparing the interface is the entire scope of this phase; issuing an
 * invite from the Manager app is a separate, later decision.
 */
interface ManagerInviteRepository {

    suspend fun issueInvite(salonId: String, phoneNumber: String, role: SalonMemberRole): Result<SalonInvite>

    suspend fun listInvites(salonId: String): Result<List<SalonInvite>>

    suspend fun revokeInvite(salonId: String, inviteId: String): Result<Unit>
}
