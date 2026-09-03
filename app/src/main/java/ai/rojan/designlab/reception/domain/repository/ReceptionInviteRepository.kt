package ai.rojan.designlab.reception.domain.repository

/**
 * Preview shown before acceptance — deliberately carries no phone number or
 * any identity-confirming field, matching the real, confirmed
 * `GET /api/v1/invites/{token}` response shape (`InviteDetailsResponse`,
 * `ai.rojan.backend.api.invite.InviteDtos.kt`): `salonName` + `role` only,
 * public, no enumeration signal. No `status` field — `GetSalonInviteUseCase`
 * (backend) throws a uniform `SalonInviteNotFoundException` (404) for
 * "never existed", "expired", "revoked", *and* "already accepted" alike, by
 * design (see that use case's own doc comment) — so a caller of [getInvite]
 * can never actually distinguish those cases from the response shape; a
 * `status` field on this preview would have been unreachable dead code.
 */
data class SalonInvitePreview(
    val salonName: String,
    val role: String,
)

/**
 * Real success shape for `POST /api/v1/invites/{token}/accept`
 * (`SalonInviteAcceptedResponse`) — `salonId` + `role` only. No
 * `membershipId`/`salonName`/`active`: this repository's own contract
 * previously assumed a richer shape ahead of the backend actually
 * existing; confirmed against the real `InviteController`/`InviteDtos.kt`
 * this sprint and corrected to match exactly, rather than fabricating
 * those fields on the Android side. A caller that needs the accepted
 * salon's name/active membership state fetches it via a fresh
 * `CurrentUserIdentityContextRepository.getCurrentUserIdentityContext()`
 * call after accepting (the real, authoritative source for both) — see
 * [ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel].
 */
data class SalonInviteAcceptance(
    val salonId: String,
    val role: String,
)

/**
 * Real, backend-backed contract for the staff (Reception) QR-invite
 * acceptance flow — `InviteController` (`GET`/`POST .../accept` under
 * `/api/v1/invites/{token}`) is confirmed live this sprint (previously
 * documented here as a not-yet-built placeholder; that has shipped).
 * [getInvite] is unauthenticated (the confirmation screen a QR scan lands
 * on before any login); [acceptInvite] requires an existing session — the
 * caller performs its own OTP verification first, independently of
 * [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]
 * (whose `RECEPTION_GATE_ROLE` check would incorrectly reject a
 * newly-invited account that still carries the global `UserRole.CUSTOMER`
 * default — see [ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel]'s
 * own doc comment for why this flow never touches that ViewModel).
 *
 * See `ai.rojan.designlab.reception.data.BackendReceptionInviteRepository`
 * for the implementation.
 */
interface ReceptionInviteRepository {

    suspend fun getInvite(token: String): Result<SalonInvitePreview>

    suspend fun acceptInvite(token: String): Result<SalonInviteAcceptance>
}
