package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AvailabilityApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.TimeSlot

class AvailabilityRepositoryImpl(
    private val availabilityApi: AvailabilityApi,
) : AvailabilityRepository {

    override suspend fun getAvailableSlots(
        salonId: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int,
    ): Result<List<TimeSlot>> =
        safeApiCall {
            availabilityApi.getAvailableSlots(
                salonId = salonId,
                specialistId = specialistId,
                serviceId = serviceId,
                date = date,
                slotIntervalMinutes = slotIntervalMinutes,
            )
        }.map { list -> list.map { TimeSlot(start = it.start, end = it.end) } }
}
