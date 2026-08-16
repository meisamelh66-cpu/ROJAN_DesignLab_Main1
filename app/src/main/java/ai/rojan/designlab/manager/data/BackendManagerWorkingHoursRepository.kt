package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.WorkingHoursApi
import ai.rojan.designlab.data.remote.dto.SetWorkingHoursRequestDto
import ai.rojan.designlab.data.remote.dto.TimeIntervalRequestDto
import ai.rojan.designlab.data.remote.dto.TimeIntervalResponseDto
import ai.rojan.designlab.data.remote.dto.WorkingHoursResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.TimeInterval
import ai.rojan.designlab.manager.domain.repository.ManagerWorkingHoursRepository

/**
 * Real backend-backed [ManagerWorkingHoursRepository]. No cache, same
 * reasoning [BackendManagerSalonRepository] already documents — this
 * repository backs exactly one screen
 * ([ai.rojan.designlab.manager.screens.settings.ManagerWorkingHoursScreen])
 * with no list to keep in sync elsewhere.
 */
class BackendManagerWorkingHoursRepository(
    private val workingHoursApi: WorkingHoursApi,
) : ManagerWorkingHoursRepository {

    override suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>> =
        safeApiCall { workingHoursApi.getWorkingHours(salonId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun setWorkingHours(
        salonId: String,
        dayOfWeek: String,
        intervals: List<TimeInterval>,
    ): Result<SalonWorkingHours> =
        safeApiCall {
            workingHoursApi.setWorkingHours(
                salonId = salonId,
                dayOfWeek = dayOfWeek,
                request = SetWorkingHoursRequestDto(
                    intervals = intervals.map { TimeIntervalRequestDto(start = it.start, end = it.end) },
                ),
            )
        }.map { it.toDomain() }

    override suspend fun removeWorkingHours(salonId: String, dayOfWeek: String): Result<Unit> =
        safeApiCall { workingHoursApi.removeWorkingHours(salonId, dayOfWeek) }

    private fun WorkingHoursResponseDto.toDomain() = SalonWorkingHours(
        dayOfWeek = dayOfWeek,
        intervals = intervals.map { it.toDomain() },
    )

    private fun TimeIntervalResponseDto.toDomain() = TimeInterval(start = start, end = end)
}
