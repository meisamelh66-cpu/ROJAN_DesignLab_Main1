package ai.rojan.designlab.reception.data

import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.toDomain
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository

/**
 * Real backend-backed [ReceptionBookingRepository] — reuses the existing
 * [ManagerBookingApi] Retrofit interface as-is (it is a plain contract for
 * `SalonBookingController`, not flavor-restricted despite its name; no
 * duplicate Retrofit interface is created here). See
 * [ReceptionBookingRepository]'s own doc comment for the current
 * owner-only authorization status. DTO -> domain mapping is the shared
 * [ai.rojan.designlab.data.remote.dto.toDomain] (`BookingResponseMapper.kt`)
 * — not a private copy, per `ROJAN_Reception_Phase1_Review_Fixes_Report_v1.md`.
 */
class BackendReceptionBookingRepository(
    private val managerBookingApi: ManagerBookingApi,
) : ReceptionBookingRepository {

    override suspend fun listBookings(
        salonId: String,
        page: Int,
        size: Int,
        status: BookingStatus?,
    ): Result<PagedResult<Booking>> =
        safeApiCall { managerBookingApi.list(salonId = salonId, page = page, size = size, status = status?.name) }
            .map { dto ->
                PagedResult(
                    content = dto.content.map { it.toDomain() },
                    page = dto.page,
                    size = dto.size,
                    totalElements = dto.totalElements,
                    totalPages = dto.totalPages,
                )
            }

    override suspend fun createBookingForCustomer(
        salonId: String,
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Booking> = safeApiCall {
        managerBookingApi.createForCustomer(
            salonId = salonId,
            request = CreateBookingForCustomerRequestDto(
                customerId = customerId,
                serviceId = serviceId,
                specialistId = specialistId,
                startTime = startTime,
                notes = notes,
            ),
        )
    }.map { it.toDomain() }
}
