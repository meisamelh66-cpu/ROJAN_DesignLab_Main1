package ai.rojan.designlab.data.remote.dto

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingParticipantSummary
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.CustomerSummary

/**
 * Shared `BookingResponseDto` -> domain mapping — extracted per
 * `ROJAN_Reception_Phase1_Review_Report_v1.md` §4 finding 2: this mapping
 * was previously duplicated, independently, in both
 * [ai.rojan.designlab.data.repository.BookingRepositoryImpl] and
 * [ai.rojan.designlab.reception.data.BackendReceptionBookingRepository] —
 * both consumed the identical [BookingResponseDto] into the identical
 * [Booking], with no compiler enforcement that a future DTO change (e.g.
 * a further enrichment field) got applied to both copies. One shared
 * mapper closes that drift risk; both repositories now call this instead
 * of maintaining their own private copy.
 */
fun BookingResponseDto.toDomain(): Booking = Booking(
    id = id,
    salonId = salonId,
    serviceId = serviceId,
    specialistId = specialistId,
    customerId = customerId,
    startTime = startTime,
    endTime = endTime,
    status = status.toDomain(),
    notes = notes,
    service = service?.toDomain(),
    specialist = specialist?.toDomain(),
    customer = customer?.toDomain(),
)

fun NetworkBookingStatus.toDomain(): BookingStatus = when (this) {
    NetworkBookingStatus.PENDING -> BookingStatus.PENDING
    NetworkBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
    NetworkBookingStatus.CANCELLED -> BookingStatus.CANCELLED
    NetworkBookingStatus.COMPLETED -> BookingStatus.COMPLETED
}

fun ServiceSummaryDto.toDomain() = BookingParticipantSummary(id = id, name = name)
fun SpecialistSummaryDto.toDomain() = BookingParticipantSummary(id = id, name = name)
fun CustomerSummaryDto.toDomain() = CustomerSummary(id = id, name = name, phone = phone)
