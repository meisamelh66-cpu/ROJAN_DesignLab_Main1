package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.SpecialistApi
import ai.rojan.designlab.data.remote.dto.CreateSpecialistRequestDto
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSpecialistRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist

/**
 * Real backend-backed [SpecialistRepository] (Final Release Validation —
 * Real Booking Calendar Integration). Replaces `InMemorySpecialistRepository`,
 * which used fake ids (`"sp1"`/`"sp2"`/`"sp3"`) that the real
 * `available-slots`/`createForCustomer` endpoints would reject outright
 * (both require a real specialist `UUID`) — real availability could not
 * work correctly until this was real too, even though no ticket had
 * previously scoped a "Specialist Module."
 *
 * Reuses the same `SpecialistApi`/`SpecialistResponseDto` the Customer
 * flavor already talks to (`ROJAN_Backend/api/salon/SpecialistController.kt`)
 * rather than a new endpoint — same reuse the Service Manager repository
 * already established for `serviceApi`/`serviceCategoryApi`.
 *
 * **Fields with no real backend equivalent, mapped to honest defaults:**
 * [Specialist.skills] (backend has no skills/specialties list) -> empty
 * list. [ManagerBookingViewModel.specialistsFor] already falls back to the
 * full active roster when no specialist matches a skill, so an
 * always-empty skills list degrades to "show everyone," not a dead end.
 * [Specialist.workingHours] -> `"—"` (no schedule field on
 * `SpecialistResponse`; real working hours live behind
 * `SpecialistScheduleController`, out of scope here).
 * [Specialist.commissionRate] -> `0.0` (no such concept on the backend;
 * this was never real even before this phase — the in-memory sample data
 * had commission numbers with nothing behind them either).
 */
class BackendSpecialistRepository(
    private val specialistApi: SpecialistApi,
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
            specialistApi.createSpecialist(
                salonId = salonId,
                request = CreateSpecialistRequestDto(displayName = specialist.name),
            )
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    override suspend fun update(specialist: Specialist): Result<Specialist?> =
        safeApiCall {
            specialistApi.updateSpecialist(
                salonId = salonId,
                specialistId = specialist.id,
                request = UpdateSpecialistRequestDto(displayName = specialist.name),
            )
        }.map { dto ->
            dto.toDomain().also { updated ->
                cache = cache.map { if (it.id == updated.id) updated else it }
            }
        }

    private fun SpecialistResponseDto.toDomain() = Specialist(
        id = id,
        name = displayName,
        skills = emptyList(),
        workingHours = "—",
        commissionRate = 0.0,
        active = active,
    )
}
