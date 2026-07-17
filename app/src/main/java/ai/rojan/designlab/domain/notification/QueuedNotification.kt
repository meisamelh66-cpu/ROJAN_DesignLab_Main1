package ai.rojan.designlab.domain.notification

/**
 * A queued notification — real data structure, no fake delivery
 * mechanism attached. This app has no push/local-notification
 * infrastructure anywhere (confirmed absent in every prior audit of
 * this codebase), so "connect Appointment Completed to the Notification
 * Queue" means genuinely enqueueing a record here, not simulating an
 * Android notification appearing — that would be exactly the kind of
 * fake infrastructure the "fake AI is not acceptable" instruction rules
 * out for a sibling system.
 */
data class QueuedNotification(
    val id: String,
    val type: NotificationType,
    val relatedEntityId: String,
    val createdAtLabel: String,
)

/** Extension point — only the one type this phase's real event (Appointment Completed) needs exists; more are BOOK 3/4 territory. */
enum class NotificationType {
    APPOINTMENT_COMPLETED,
    /** Appointment System completion (V1.0 Module 6 - Reminder). */
    APPOINTMENT_CANCELLED,
    APPOINTMENT_RESCHEDULED,
    /** Appointment System completion (V1.0 Module 6 - Waiting List). */
    WAITLIST_PROMOTED,
}
