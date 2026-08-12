package ai.rojan.designlab.domain.repository

/**
 * Identity & Session Architecture, Android Integration: the real backend
 * salon-access context for the currently authenticated user - wraps
 * [AuthenticatedUser] (already the real OTP-session identity) with what
 * `GET /api/v1/users/me/salon-access` adds. Additive, not a replacement:
 * nothing about the existing OTP session/[AuthenticatedUser] changes
 * because this exists.
 *
 * Deliberately has no `activeSalonId` - no product flow selects one yet,
 * and inventing automatic selection (e.g. "first membership") would be
 * exactly the kind of guessed behavior this integration avoids. A caller
 * with more than one [ownedSalons]/[memberships]/[specialistLinks] entry
 * has no current-salon concept until a real selection flow is built.
 */
data class CurrentUserIdentityContext(
    val userId: String,
    val phoneNumber: String?,
    val email: String?,
    val fullName: String,
    val globalRole: String,
    val ownedSalons: List<OwnedSalonAccess>,
    val memberships: List<SalonMembershipAccess>,
    val specialistLinks: List<SpecialistAccess>,
)

data class OwnedSalonAccess(
    val salonId: String,
    val salonName: String,
    val active: Boolean,
    val permissions: Set<String>,
)

data class SalonMembershipAccess(
    val membershipId: String,
    val salonId: String,
    val salonName: String,
    val active: Boolean,
    val role: String,
    val permissions: Set<String>,
)

data class SpecialistAccess(
    val specialistId: String,
    val salonId: String,
    val salonName: String,
    val active: Boolean,
    val permissions: Set<String>,
)

/**
 * The exact permission vocabulary the backend can send
 * (`ai.rojan.backend.domain.salon.Permission`, mirrored by name only - no
 * role->permission mapping lives here or anywhere in this app). Provided
 * so call sites compare against a named constant instead of a string
 * literal; an unrecognized value arriving in a [OwnedSalonAccess.permissions]/
 * [SalonMembershipAccess.permissions]/[SpecialistAccess.permissions] set
 * simply never equals any of these and therefore never grants anything -
 * "unknown permission" fails safe by construction, not by an explicit check.
 */
object SalonPermissions {
    const val MANAGE_SALON = "MANAGE_SALON"
    const val MANAGE_MEMBERSHIP = "MANAGE_MEMBERSHIP"
    const val MANAGE_CATALOG = "MANAGE_CATALOG"
    const val MANAGE_STAFF = "MANAGE_STAFF"
    const val MANAGE_SCHEDULE_ALL = "MANAGE_SCHEDULE_ALL"
    const val MANAGE_SCHEDULE_OWN = "MANAGE_SCHEDULE_OWN"
    const val VIEW_CRM = "VIEW_CRM"
    const val MANAGE_CRM = "MANAGE_CRM"
    const val MANAGE_BOOKINGS = "MANAGE_BOOKINGS"
    const val MANAGE_OWN_BOOKINGS = "MANAGE_OWN_BOOKINGS"
}

/**
 * Talks to `GET /api/v1/users/me/salon-access`, combined with the existing
 * [BackendAuthRepository.currentUser] call - two real backend calls, one
 * domain result. Failures are surfaced explicitly via [Result.failure],
 * never coerced into an empty/successful context.
 */
interface CurrentUserIdentityContextRepository {
    suspend fun getCurrentUserIdentityContext(): Result<CurrentUserIdentityContext>
}
