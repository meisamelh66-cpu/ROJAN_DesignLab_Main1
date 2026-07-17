package ai.rojan.designlab.domain.waitlist

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.data.demo.DemoWaitlistEntry
import ai.rojan.designlab.data.demo.WaitlistStatus
import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.customer.CustomerEcosystemState
import ai.rojan.designlab.domain.customer.EcosystemEvent
import ai.rojan.designlab.domain.notification.NotificationType
import ai.rojan.designlab.domain.notification.QueuedNotification

/**
 * Appointment System completion (V1.0 Module 6 - Waiting List). Pure
 * Kotlin, same discipline as [BookingEngine]/
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine] - zero
 * Compose/Android dependency, every method only *computes* a
 * `List<EcosystemEvent>`, never mutates state directly (
 * [ai.rojan.designlab.domain.customer.EcosystemEventReducer] remains
 * the sole place state is actually applied).
 */
class WaitlistEngine {

    /**
     * "Prevent duplicate entries": a customer can't join the same
     * salon+service+specialist+date combination twice while an existing
     * entry is still [WaitlistStatus.WAITING]. Sequence number for FIFO
     * ordering is simply the count of ALL entries ever created (not just
     * active ones) plus one - monotonic even across leaves/promotions,
     * so ordering never has gaps or collisions.
     */
    fun join(
        state: CustomerEcosystemState,
        salonId: String,
        salonName: String,
        serviceId: String,
        serviceName: String,
        specialistId: String?,
        dateKey: String,
        dateLabel: String,
    ): List<EcosystemEvent> {
        val isDuplicate = state.waitlistEntries.any {
            it.status == WaitlistStatus.WAITING &&
                it.salonId == salonId &&
                it.serviceId == serviceId &&
                it.specialistId == specialistId &&
                it.dateKey == dateKey
        }
        if (isDuplicate) {
            return listOf(EcosystemEvent.WaitlistJoinRejected("شما قبلاً برای این زمان در لیست انتظار هستید"))
        }

        val entry = DemoWaitlistEntry(
            id = "waitlist_${state.waitlistSequenceCounter}",
            salonId = salonId,
            salonName = salonName,
            serviceId = serviceId,
            serviceName = serviceName,
            specialistId = specialistId,
            dateKey = dateKey,
            dateLabel = dateLabel,
            requestedAtSequence = state.waitlistSequenceCounter,
            status = WaitlistStatus.WAITING,
        )

        return listOf(EcosystemEvent.WaitlistJoined(entry))
    }

    /** "Leave Waiting List": only meaningful for an entry that's still actually WAITING. */
    fun leave(state: CustomerEcosystemState, entryId: String): List<EcosystemEvent> {
        val entry = state.waitlistEntries.find { it.id == entryId } ?: return emptyList()
        if (entry.status != WaitlistStatus.WAITING) return emptyList()
        return listOf(EcosystemEvent.WaitlistLeft(entryId))
    }

    /**
     * "Automatic promotion when a time slot becomes available" +
     * "BookingEngine integration": called after a cancellation frees a
     * slot for (salonId, serviceId, freedSpecialistId, dateKey).
     * Matches WAITING entries for that salon+service+date where the
     * entry's specialist preference is either unset (any specialist) or
     * exactly the freed specialist, sorted FIFO
     * ([DemoWaitlistEntry.requestedAtSequence] ascending), and promotes
     * only the *first* genuinely-available match - real
     * [BookingEngine.hasAnyAvailability] check, not an assumption that
     * freeing one slot means it's still free by the time this runs.
     */
    fun checkForPromotion(
        state: CustomerEcosystemState,
        bookingEngine: BookingEngine,
        salonId: String,
        salonName: String,
        serviceId: String,
        serviceName: String,
        freedSpecialistId: String,
        freedSpecialistName: String,
        dateKey: String,
        durationMinutes: Int,
        priceForNewAppointment: Int,
    ): List<EcosystemEvent> {
        val candidates = state.waitlistEntries
            .filter {
                it.status == WaitlistStatus.WAITING &&
                    it.salonId == salonId &&
                    it.serviceId == serviceId &&
                    it.dateKey == dateKey &&
                    (it.specialistId == null || it.specialistId == freedSpecialistId)
            }
            .sortedBy { it.requestedAtSequence }

        val earliestSlot = bookingEngine.earliestAvailableSlot(dateKey, durationMinutes, freedSpecialistId)
            ?: return emptyList()

        val toPromote = candidates.firstOrNull() ?: return emptyList()

        val newAppointment = DemoAppointment(
            id = "appt_from_${toPromote.id}",
            salonName = salonName,
            serviceName = serviceName,
            specialistName = freedSpecialistName,
            dateLabel = toPromote.dateLabel,
            time = earliestSlot.time,
            status = AppointmentStatus.UPCOMING,
            price = priceForNewAppointment,
            relatedServiceId = serviceId,
            specialistId = freedSpecialistId,
        )

        val notification = QueuedNotification(
            id = "notif_promoted_${toPromote.id}",
            type = NotificationType.WAITLIST_PROMOTED,
            relatedEntityId = newAppointment.id,
            createdAtLabel = toPromote.dateLabel,
        )

        return listOf(
            EcosystemEvent.WaitlistPromoted(toPromote.id, newAppointment),
            EcosystemEvent.NotificationEnqueued(notification),
        )
    }
}
