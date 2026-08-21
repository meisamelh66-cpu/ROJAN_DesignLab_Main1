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
 * [Specialist.skills] is always empty here — the backend's real
 * eligibility mechanism is per-specialist
 * (`ManagerSpecialistApi.eligibleServiceIds`), which would mean one
 * extra call per roster member just to populate a filter, not a bulk
 * field this sync can fetch for free. Not wired in this step to keep it
 * scoped; [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.specialistsFor]'s
 * existing "fall back to the full active roster when nothing matches"
 * behavior already degrades to exactly that with an empty skill set, so
 * nothing dead-ends - the wizard just always shows every active
 * specialist for now, a disclosed simplification, not a silent one.
 * [Specialist.workingHours]/[Specialist.commissionRate] have no backend
 * equivalent either (business-internal figures the API doesn't expose);
 * `workingHours` stays `"—"` (matching the same honest-placeholder
 * convention [BackendCustomerRepository] uses) and `commissionRate` stays
 * `0.0` (never rendered anywhere in this codebase).
 *
 * [Specialist.photoUrl] (Media Sprint P0) is real and round-trips: read
 * from [SpecialistResponseDto.photoUrl], written back through
 * [CreateSpecialistRequestDto.photoUrl]/[UpdateSpecialistRequestDto.photoUrl] —
 * both already existed on these DTOs, unused until now.
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
                request = CreateSpecialistRequestDto(displayName = specialist.name, photoUrl = specialist.photoUrl),
            )
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    override suspend fun update(specialist: Specialist): Result<Specialist?> =
        safeApiCall {
            managerSpecialistApi.update(
                salonId = salonId,
                specialistId = specialist.id,
                request = UpdateSpecialistRequestDto(displayName = specialist.name, photoUrl = specialist.photoUrl),
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

    private fun SpecialistResponseDto.toDomain() = Specialist(
        id = id,
        name = displayName,
        skills = emptyList(),
        workingHours = "—",
        commissionRate = 0.0,
        active = active,
        photoUrl = photoUrl,
    )
}
