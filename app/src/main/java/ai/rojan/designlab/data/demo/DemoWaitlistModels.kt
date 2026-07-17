package ai.rojan.designlab.data.demo

/**
 * Appointment System completion (V1.0 Module 6 - Waiting List).
 */
enum class WaitlistStatus { WAITING, PROMOTED, LEFT }

/**
 * A customer's request to be notified/booked automatically if a slot
 * opens up for a salon+service+date (optionally a specific specialist)
 * that currently has no availability. [requestedAtSequence] is a simple
 * monotonic counter (not a real timestamp - no clock dependency needed
 * for a demo, and it sorts identically) used for FIFO promotion order:
 * lower value = joined earlier = promoted first.
 */
data class DemoWaitlistEntry(
    val id: String,
    val salonId: String,
    val salonName: String,
    val serviceId: String,
    val serviceName: String,
    /** Null means "any specialist at this salon for this service" - broader match, promoted against any specialist's freed slot. */
    val specialistId: String?,
    val dateKey: String,
    val dateLabel: String,
    val requestedAtSequence: Int,
    val status: WaitlistStatus,
)
