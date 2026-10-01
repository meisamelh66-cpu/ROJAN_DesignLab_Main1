package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerSpecialistApi
import ai.rojan.designlab.data.remote.SpecialistApi
import ai.rojan.designlab.data.remote.dto.CreateSpecialistRequestDto
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSpecialistRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist

/**
 * Real backend-backed [SpecialistRepository] (Phase 2, M3) — replaces
 * [InMemorySpecialistRepository]'s hardcoded roster (fake ids like
 * `"sp1"`/`"sp2"`/`"sp3"` that the real `available-slots`/
 * `createForCustomer` endpoints would reject outright, since both require
 * a real specialist `UUID`). Reuses [SpecialistApi], the same read
 * endpoints Customer already calls (`GET .../specialists`), for [sync] —
 * matching [BackendServiceRepository]'s reuse of
 * `ServiceApi`/`ServiceCategoryApi` — plus [ManagerSpecialistApi] for the
 * owner-only writes, same "shared reads, owner-scoped writes" split
 * [BackendCustomerRepository] already established.
 *
 * Specialist Profile Expansion: [Specialist.bio]/[photoUrl]/[mobileNumber]/
 * [specialty]/[userId] now round-trip for real (previously dropped by this
 * mapper even though the DTOs already carried `bio`/`photoUrl`). The old
 * `skills`/`workingHours`/`commissionRate` placeholders are gone entirely -
 * none has a backend field, DB column, or endpoint; [eligibleServiceIds]
 * (`ManagerSpecialistApi.eligibleServiceIds`) is the real, already-defined
 * substitute for `skills` (per-specialist service eligibility - empty means
 * eligible for every service, per the backend's own contract), now actually
 * wired up rather than left unused.
 */
class BackendSpecialistRepository(
    private val specialistApi: SpecialistApi,
    private val managerSpecialistApi: ManagerSpecialistApi,
    private val salonId: String,
) : SpecialistRepository {

    private var cache: List<Specialist> = emptyList()

    /** Fetches this salon's specialists from the backend and repopulates the cache. Call before first read, and to refresh. */
    suspend fun sync(): Result<Unit> = safeApiCall {
        specialistApi.getSpecialists(salonId)
    }.map { list ->
        cache = list.map { it.toDomain() }
    }

    override fun getAll(): List<Specialist> = cache

    override fun getById(id: String): Specialist? = cache.find { it.id == id }

    override suspend fun create(specialist: Specialist): Result<Specialist> =
        safeApiCall {
            managerSpecialistApi.create(
                salonId = salonId,
                request = CreateSpecialistRequestDto(
                    displayName = specialist.name,
                    bio = specialist.bio,
                    photoUrl = specialist.photoUrl,
                    mobileNumber = specialist.mobileNumber,
                    specialty = specialist.specialty,
                ),
            )
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    override suspend fun update(specialist: Specialist): Result<Specialist?> =
        safeApiCall {
            managerSpecialistApi.update(
                salonId = salonId,
                specialistId = specialist.id,
                request = UpdateSpecialistRequestDto(
                    displayName = specialist.name,
                    bio = specialist.bio,
                    photoUrl = specialist.photoUrl,
                    mobileNumber = specialist.mobileNumber,
                    specialty = specialist.specialty,
                ),
            )
        }.map { dto ->
            dto.toDomain().also { updated ->
                cache = cache.map { if (it.id == updated.id) updated else it }
            }
        }

    override suspend fun delete(id: String): Result<Boolean> =
        safeApiCall {
            managerSpecialistApi.deactivate(salonId, id)
        }.map {
            cache = cache.filterNot { it.id == id }
            true
        }

    override suspend fun eligibleServiceIds(specialistId: String): Result<List<String>> =
        safeApiCall { managerSpecialistApi.eligibleServiceIds(salonId, specialistId) }

    private fun SpecialistResponseDto.toDomain() = Specialist(
        id = id,
        name = displayName,
        bio = bio,
        photoUrl = photoUrl,
        mobileNumber = mobileNumber,
        specialty = specialty,
        userId = userId,
        active = active,
    )
}
