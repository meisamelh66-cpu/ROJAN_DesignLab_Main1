package ai.rojan.designlab.manager.domain.appointment

/**
 * Manager Domain Foundation Phase 1 — normalized appointment record
 * (references [customerId]/[serviceId]/[specialistId] rather than
 * flattened display names, unlike the older screen-local
 * `CalendarAppointment` in [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen],
 * which is intentionally left as-is this phase — see Phase 1 report).
 *
 * [notes]/[endTime] (Manager Operational Foundation, Phase 6 Step 4): real
 * fields already present on the backend's `BookingResponseDto` but
 * previously dropped during DTO->domain mapping - added for the
 * Appointment Detail screen, not fabricated. [notes] is nullable (most
 * bookings have none); [endTime] mirrors [time]'s raw ISO-derived shape
 * (see [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s
 * own doc comment on the wizard-vs-backend format distinction that
 * already applies to [date]/[time]).
 */
data class Appointment(
    val id: String,
    val customerId: String,
    val serviceId: String,
    val specialistId: String,
    val date: String,
    val time: String,
    val status: AppointmentStatus,
    val endTime: String? = null,
    val notes: String? = null,
)
