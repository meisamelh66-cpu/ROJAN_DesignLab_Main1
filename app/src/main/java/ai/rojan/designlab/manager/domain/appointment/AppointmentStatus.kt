package ai.rojan.designlab.manager.domain.appointment

/** Appointment lifecycle — Manager Domain Foundation Phase 1. */
enum class AppointmentStatus {
    PENDING,
    CONFIRMED,

    /** Reception Check-in (Phase 4): optional - an appointment can also go straight from CONFIRMED to COMPLETED. */
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
}
