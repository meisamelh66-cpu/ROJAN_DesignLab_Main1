package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Real backend-backed [AppointmentRepository] (Manager App Phase 2 —
 * Appointment Integration). `getAll()`/`getById()`/`getByCustomerId()`
 * read an in-memory cache populated by [sync], sourced from the real,
 * owner-authenticated `GET /api/v1/salons/{salonId}/bookings`
 * (`SalonBookingController`) — genuinely real data, not fabricated.
 *
 * `create`/`update`/`updateStatus`/`cancel` stay **local-cache only**,
 * matching the previous in-memory implementation's behavior, on purpose:
 * the backend has no owner-side booking-write endpoint. The only
 * booking-create endpoint that exists (`POST /api/v1/bookings`) derives
 * `customerId` from the caller's own JWT, not a request field — calling
 * it from the Manager app would silently attribute the booking to the
 * manager's own account, not the customer selected in the wizard, which
 * is worse than not integrating it. This is a real, confirmed backend
 * gap (see `ROJAN_Manager_App_Audit_Report.md`), not a shortcut taken
 * here.
 *
 * **Known pre-existing gap this surfaces, not fixed here:**
 * [ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek]'s own
 * doc comment states no real calendar/date library is wired into this
 * codebase - "today" and the week it shows are a static placeholder, not
 * derived from a real clock. Real backend appointments below are mapped
 * to their actual Gregorian date/time (correct, real data), but Calendar's
 * "today" grouping won't line them up with its still-fake reference week
 * until that separate, pre-existing gap is closed. Surfacing this rather
 * than silently reformatting real dates to fit the fake week.
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
