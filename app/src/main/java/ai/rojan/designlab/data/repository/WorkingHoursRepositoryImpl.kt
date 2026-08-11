package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.WorkingHoursApi
import ai.rojan.designlab.data.remote.dto.TimeIntervalResponseDto
import ai.rojan.designlab.data.remote.dto.WorkingHoursResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.TimeInterval
import ai.rojan.designlab.domain.repository.WorkingHoursRepository

class WorkingHoursRepositoryImpl(
    private val workingHoursApi: WorkingHoursApi,
) : WorkingHoursRepository {

    override suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>> =
        safeApiCall { workingHoursApi.getWorkingHours(salonId) }.map { list -> list.map { it.toDomain() } }

    private fun WorkingHoursResponseDto.toDomain() = SalonWorkingHours(
        dayOfWeek = dayOfWeek,
        intervals = intervals.map { it.toDomain() },
    )

    private fun TimeIntervalResponseDto.toDomain() = TimeInterval(start = start, end = end)
}
