package ai.rojan.designlab.manager.domain.appointment

/**
 * Manager Domain Foundation Phase 1 — normalized appointment record
 * (references [customerId]/[serviceId]/[specialistId] rather than
 * flattened display names, unlike the older screen-local
 * `CalendarAppointment` in [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen],
 * which is intentionally left as-is this phase — see Phase 1 report).
 */
data class Appointment(
    val id: String,
    val customerId: String,
    val serviceId: String,
    val specialistId: String,
    val date: String,
    val time: String,
    val status: AppointmentStatus,
)
