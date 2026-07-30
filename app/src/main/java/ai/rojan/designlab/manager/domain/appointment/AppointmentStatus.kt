package ai.rojan.designlab.manager.domain.appointment

/** Appointment lifecycle — Manager Domain Foundation Phase 1. */
enum class AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
}
