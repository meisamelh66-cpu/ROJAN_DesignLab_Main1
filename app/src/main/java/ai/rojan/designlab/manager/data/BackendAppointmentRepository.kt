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
 * Appointment Integration; [create] made real in Phase 2, M1).
 * `getAll()`/`getById()`/`getByCustomerId()` read an in-memory cache
 * populated by [sync], sourced from the real, owner-authenticated
 * `GET /api/v1/salons/{salonId}/bookings` (`SalonBookingController`) —
 * genuinely real data, not fabricated.
 *
 * [create] calls the real `POST /api/v1/salons/{salonId}/bookings`
 * (`SalonBookingController.createForCustomer`) — the owner-authorized
 * counterpart to the customer-self-service `POST /api/v1/bookings` this
 * class's doc comment previously (and correctly, at the time) said
 * didn't exist for Manager: that customer-self endpoint derives
 * `customerId` from the caller's own JWT, so it genuinely couldn't be
 * used from here, but `createForCustomer` takes an explicit Customer CRM
 * id instead, which [ai.rojan.designlab.manager.data.BackendCustomerRepository]
 * (Phase 2, M2) now supplies for real. [appointment]'s `date`/`time`
 * fields are trusted to still be the wizard's own ISO `yyyy-MM-dd`/`HH:mm`
 * strings at this point (the only caller,
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.confirm],
 * builds [appointment] fresh from [ai.rojan.designlab.manager.domain.booking.ManagerBookingState]
 * immediately before calling this) — not the `yyyy/MM/dd` Gregorian
 * display format [toDomain] below produces for bookings read back from
 * the backend, a real, pre-existing format difference between
 * wizard-in-progress and backend-synced appointments this class does not
 * attempt to unify.
 *
 * **Known real backend constraint, surfaced not hidden:** per
 * `createForCustomer`'s own contract, the target customer must already
 * be linked to a real account or the call 409s - a walk-in CRM record
 * with no linked account cannot be booked this way today. [create]
 * returns that failure through [Result] rather than silently succeeding,
 * so the wizard can show it instead of claiming a booking that didn't
 * happen.
 *
 * [confirm]/[complete] (RBAC compatibility fix — Manager Android Pilot)
 * are real too, via `PATCH /api/v1/bookings/{id}/confirm`|`/complete` -
 * see [ai.rojan.designlab.data.remote.ManagerBookingApi]'s own doc comment.
 *
 * `update`/`updateStatus`/`cancel` stay **local-cache only**, matching
 * the previous in-memory implementation's behavior, on purpose: the
 * backend still has no owner-side cancel-booking endpoint - only
 * [create]/[confirm]/[complete] are real. Confirmed still true and still unreferenced
 * by any call site as of Phase 11 Step 3 - see
 * [ai.rojan.designlab.manager.domain.repository.AppointmentRepository.cancel]'s
 * doc comment for the real `PATCH .../cancel` contract (Phase 11 Step 2's
 * backend specification) these three should be replaced with, not
 * extended in place, once that endpoint exists. That replacement should
 * follow [create]/[createForCustomer]'s exact existing shape immediately
 * below: `safeApiCall { managerBookingApi.<newMethod>(...) }.map { dto ->
 * dto.toDomain().also { updated -> cache = ... } }` - updating [cache]
 * from the real response rather than requiring a full [sync] afterward.
 *
 * **Formerly a known pre-existing gap, closed in Phase 2, M7:**
 * [ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek]
 * used to be a static hardcoded reference week with no relation to the
 * real clock, so real backend appointments (mapped below to their
 * actual Gregorian date/time - always correct) never matched its
 * "today" grouping. `ManagerCalendarWeek` now computes a real rolling
 * window in the same `yyyy/MM/dd` format [toDomain] produces here, so
 * the two line up.
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

    override suspend fun create(appointment: Appointment): Result<Appointment> =
        safeApiCall {
            managerBookingApi.createForCustomer(
                salonId = salonId,
                request = CreateBookingForCustomerRequestDto(
                    customerId = appointment.customerId,
                    serviceId = appointment.serviceId,
                    specialistId = appointment.specialistId,
                    startTime = "${appointment.date}T${appointment.time}:00",
                ),
            )
        }.map { dto -> dto.toDomain().also { created -> cache = cache + created } }

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

    override suspend fun confirm(id: String): Result<Appointment> =
        safeApiCall { managerBookingApi.confirm(id) }
            .map { dto -> dto.toDomain().also { updated -> cache = cache.map { if (it.id == id) updated else it } } }

    override suspend fun complete(id: String): Result<Appointment> =
        safeApiCall { managerBookingApi.complete(id) }
            .map { dto -> dto.toDomain().also { updated -> cache = cache.map { if (it.id == id) updated else it } } }

    private fun BookingResponseDto.toDomain(): Appointment {
        // startTime/endTime are ISO-8601 *local* date-times with no offset (see CreateBookingRequestDto's
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
            endTime = LocalDateTime.parse(endTime).format(DateTimeFormatter.ofPattern("HH:mm")),
            notes = notes,
        )
    }

    private fun NetworkBookingStatus.toDomain(): AppointmentStatus = when (this) {
        NetworkBookingStatus.PENDING -> AppointmentStatus.PENDING
        NetworkBookingStatus.CONFIRMED -> AppointmentStatus.CONFIRMED
        NetworkBookingStatus.CANCELLED -> AppointmentStatus.CANCELLED
        NetworkBookingStatus.COMPLETED -> AppointmentStatus.COMPLETED
    }
}
