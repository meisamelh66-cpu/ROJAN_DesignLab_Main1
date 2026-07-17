package ai.rojan.designlab.domain.notification

/**
 * Real, immutable queue value type — same discipline as
 * [ai.rojan.designlab.domain.booking.BookingState]: `enqueue` returns
 * a new instance, nothing is mutated in place. Held as a field on
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemState], applied
 * to that state exclusively through
 * [ai.rojan.designlab.domain.customer.EcosystemEvent.NotificationEnqueued]
 * — going through Events, not a direct module update, per the
 * event-driven architecture requirement.
 */
data class NotificationQueue(
    val items: List<QueuedNotification> = emptyList(),
) {
    fun enqueue(notification: QueuedNotification): NotificationQueue =
        copy(items = items + notification)
}
