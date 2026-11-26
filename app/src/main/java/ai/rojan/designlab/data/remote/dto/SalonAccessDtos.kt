package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for `GET /api/v1/users/me/salon-access`
 * (`ROJAN_Backend/api/.../user/SalonAccessDtos.kt` — field names mirror it
 * exactly). [permissions] on every entry is always server-resolved
 * (`ai.rojan.backend.application.salon.SalonPermissionResolver`) — this app
 * never re-derives it from [role]/ownership itself. Carried as a raw
 * `Set<String>` deliberately, not a Kotlin enum: an unrecognized value
 * (e.g. a permission added backend-side before this app updates) must not
 * fail the whole response's deserialization, and must never be silently
 * interpreted as any known permission either — see
 * `domain/repository/CurrentUserIdentityContext.kt`'s `SalonPermissions`
 * for where that "unknown never grants" property actually holds.
 */
@Serializable
data class SalonAccessResponseDto(
    val ownedSalons: List<OwnedSalonAccessDto>,
    val memberships: List<MembershipAccessDto>,
    val specialistLinks: List<SpecialistAccessDto>,
)

@Serializable
data class OwnedSalonAccessDto(
    val salonId: String,
    val salonName: String,
    val active: Boolean,
    val permissions: Set<String>,
)

@Serializable
data class MembershipAccessDto(
    val membershipId: String,
    val salonId: String,
    val salonName: String,
    val active: Boolean,

    /**
     * Reception Authentication Fix (Phase 4): the membership's own enabled/disabled state -
     * distinct from [active] above (the salon's) - was never carried through to this app at all
     * before this field existed. [permissions] is already empty server-side when this is `false`
     * (mirrors the backend's own `MembershipAccessDto` doc comment), but this app checks the flag
     * directly rather than inferring "disabled" from an empty permission set, which could also
     * mean "this role genuinely has no permissions" for an unrelated reason.
     */
    val membershipActive: Boolean,
    val role: String,
    val permissions: Set<String>,
)

@Serializable
data class SpecialistAccessDto(
    val specialistId: String,
    val salonId: String,
    val salonName: String,
    val active: Boolean,
    val permissions: Set<String>,
)
