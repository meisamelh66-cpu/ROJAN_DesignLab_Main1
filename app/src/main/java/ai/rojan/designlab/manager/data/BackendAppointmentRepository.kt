package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Real backend-backed [AppointmentRepository] (Manager App Phase 2 —
 * Appointment Integration; Final Release Validation — Real Booking
 * Calendar Integration). `getAll()`/`getById()`/`getByCustomerId()` read
 * an in-memory cache populated by [sync], sourced from the real,
 * owner-authenticated `GET /api/v1/salons/{salonId}/bookings`
 * (`SalonBookingController`) — genuinely real data, not fabricated.
 *
 * `create`/`update`/`updateStatus`/`cancel` stay **local-cache only** —
 * the backend has no owner-side update/cancel endpoint for a booking
 * already made (only create). [createForCustomer] is the one real write:
 * `POST /api/v1/salons/{salonId}/bookings`, the owner-authorized
 * counterpart to the customer-self-service `POST /api/v1/bookings` (which
 * derives `customerId` from the caller's own JWT and so was never usable
 * from the Manager app). A successful [createForCustomer] also appends
 * the real, backend-returned appointment into the local cache, so
 * Calendar reflects it immediately without a full [sync].
 *
 * [ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek] is
 * now a real, clock-driven week too (previously a static placeholder),
 * so appointments mapped below line up with Calendar's "today" grouping
 * correctly — the drift this class's doc comment used to flag is closed.
 */
class BackendAppointmentRepository(
    private val managerBookingApi: ManagerBookingApi,
    private val salonId: String,
) : AppointmentRepository {

    private var cache: List<Appointment> = emptyList()

    /** Fetches this salon's bookings from the backend and repopulates the cache. Call before first read, and to refresh. */
    suspend fun sync(): Result<Unit> = safeApiCall {
        managerBookingApi.list(salonId, page = 0, size = 200)
    }.map { paged ->
        cache = paged.content.map { it.toDomain() }
    }

    override fun getAll(): List<Appointment> = cache

    override fun getById(id: String): Appointment? = cache.find { it.id == id }

    override fun getByCustomerId(customerId: String): List<Appointment> =
        cache.filter { it.customerId == customerId }

    override fun create(appointment: Appointment): Appointment {
        cache = cache + appointment
        return appointment
    }

    override fun update(appointment: Appointment): Appointment? {
        if (cache.none { it.id == appointment.id }) return null
        cache = cache.map { if (it.id == appointment.id) appointment else it }
        return appointment
    }

    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? {
        val existing = cache.find { it.id == id } ?: return null
        val updated = existing.copy(status = status)
        cache = cache.map { if (it.id == id) updated else it }
        return updated
    }

    override fun cancel(id: String): Appointment? = updateStatus(id, AppointmentStatus.CANCELLED)

    override suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment> =
        safeApiCall {
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
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    private fun BookingResponseDto.toDomain(): Appointment {
        // startTime is an ISO-8601 *local* date-time with no offset (see CreateBookingRequestDto's
        // own doc comment, e.g. "2026-09-01T10:00:00") - LocalDateTime.parse, not OffsetDateTime.
        val start = LocalDateTime.parse(startTime)
        return Appointment(
            id = id,
            customerId = customerId,
            serviceId = serviceId,
            specialistId = specialistId,
            date = start.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
            time = start.format(DateTimeFormatter.ofPattern("HH:mm")),
            status = status.toDomain(),
        )
    }

    private fun NetworkBookingStatus.toDomain(): AppointmentStatus = when (this) {
        NetworkBookingStatus.PENDING -> AppointmentStatus.PENDING
        NetworkBookingStatus.CONFIRMED -> AppointmentStatus.CONFIRMED
        NetworkBookingStatus.CANCELLED -> AppointmentStatus.CANCELLED
        NetworkBookingStatus.COMPLETED -> AppointmentStatus.COMPLETED
    }
}
