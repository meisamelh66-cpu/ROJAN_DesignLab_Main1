package ai.rojan.designlab.reception.domain.repository

/**
 * `PENDING` (issued, not yet acted on) / `ACCEPTED` (terminal, success) /
 * `EXPIRED` (terminal) / `REVOKED` (terminal, owner cancelled before
 * acceptance) — approved states, `ROJAN_System1_Backend_Decision_v2.md` §2.
 */
enum class SalonInviteStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

/** Preview shown before acceptance — deliberately carries no phone number or any identity-confirming field, matching the approved `GET /api/v1/invites/{token}` response shape (public, no enumeration signal). */
data class SalonInvitePreview(
    val salonName: String,
    val role: String,
    val status: SalonInviteStatus,
)

/** Approved success shape for `POST /api/v1/invites/{token}/accept` (§2). */
data class SalonInviteAcceptance(
    val membershipId: String,
    val salonId: String,
    val salonName: String,
    val role: String,
    val active: Boolean,
)

/**
 * **Integration placeholder only — not backed by a real network call.**
 * `ROJAN_System1_Backend_Decision_v2.md` §2 approved this contract, but the
 * backend (`InviteController` / `SalonInvite` domain / `salon_invites`
 * persistence) does not exist yet — re-confirmed absent on
 * `origin/feature/auth-rate-limit-finalization` at the time of this
 * interface's creation. No `InviteApi` Retrofit interface, no
 * implementation, and no DI wiring exist for this — adding a Retrofit
 * method against a route that 404s at runtime would be worse than no
 * binding at all (same reasoning `ManagerBookingApi.kt`'s own doc comment
 * already applies to a different unbuilt endpoint in this codebase).
 *
 * This interface exists purely so the domain-layer contract is captured
 * and typed ahead of the backend — a future `BackendReceptionInviteRepository`
 * implementing it is the only change needed once `InviteController` ships,
 * nothing else in this file should need to change for that.
 *
 * **Also blocked independently of the missing endpoint:**
 * `ROJAN_Reception_Phase1_Updated_Plan_v2.md` §3 surfaced an unresolved
 * gap System 1's decision does not address — a brand-new phone number's
 * first OTP verification auto-registers as `UserRole.CUSTOMER`, which
 * would fail `ReceptionAuthViewModel`'s `MANAGER`-role gate before an
 * invited receptionist with no prior account could ever reach the accept
 * step. No UI screens are built against this interface for that reason —
 * see `ROJAN_System2_Reception_Phase1_Status_Report_v1.md` for the current
 * status.
 */
interface ReceptionInviteRepository {

    suspend fun getInvite(token: String): Result<SalonInvitePreview>

    suspend fun acceptInvite(token: String): Result<SalonInviteAcceptance>
}
