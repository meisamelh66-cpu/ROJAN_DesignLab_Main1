package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpecialistResponseDto(
    val id: String,
    val salonId: String,
    val userId: String? = null,
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val mobileNumber: String? = null,
    val specialty: String? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/** `CreateSpecialistRequest` (backend, `SpecialistController`) — owner-only. [mobileNumber]/[specialty] are API-optional (backend's own `CreateSpecialistRequest` doc comment: added after this endpoint already had real callers). */
@Serializable
data class CreateSpecialistRequestDto(
    val userId: String? = null,
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val mobileNumber: String? = null,
    val specialty: String? = null,
)

/**
 * `UpdateSpecialistRequest` (backend, `SpecialistController`) — owner-only,
 * full replace (not a PATCH merge like Customer's update) for
 * [displayName]/[bio]/[photoUrl]. [mobileNumber]/[specialty] are the one
 * exception - the backend's own `Specialist.update()` doc comment: `null`
 * here means "leave whatever this specialist already has, don't touch it,"
 * never "clear it," so omitting them on an update can't silently wipe
 * contact info a specialist already has.
 */
@Serializable
data class UpdateSpecialistRequestDto(
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val mobileNumber: String? = null,
    val specialty: String? = null,
)
