package ai.rojan.designlab.manager.domain.specialist

/**
 * Specialist Profile Expansion: mirrors the backend's real `Specialist`
 * fields (`ROJAN_Backend/domain/.../salon/Specialist.kt`,
 * `SpecialistResponseDto`) exactly, replacing the previous
 * [skills]/`workingHours`/`commissionRate` placeholders that had no
 * backend field, no DB column, and no endpoint at all - see
 * [ai.rojan.designlab.manager.data.BackendSpecialistRepository]'s own doc
 * comment on why those were removed rather than wired up. [bio]/[photoUrl]/
 * [mobileNumber]/[specialty] are all optional/nullable on the backend -
 * never fabricated here, always `null` when genuinely unset. [userId],
 * when non-null, means this specialist is already linked to a real user
 * account (set by the backend, not editable from this app - there is no
 * "link specialist to user" endpoint, unlike the Customer-side account-link
 * flow).
 */
data class Specialist(
    val id: String,
    val name: String,
    val bio: String?,
    val photoUrl: String?,
    val mobileNumber: String?,
    val specialty: String?,
    val userId: String?,
    val active: Boolean,
)
