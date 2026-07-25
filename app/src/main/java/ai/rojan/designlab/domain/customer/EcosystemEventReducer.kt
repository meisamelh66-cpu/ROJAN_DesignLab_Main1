package ai.rojan.designlab.domain.customer

import ai.rojan.designlab.data.demo.DemoWalletTransaction
import ai.rojan.designlab.data.demo.WaitlistStatus

/**
 * The one place [EcosystemEvent]s are applied to [CustomerEcosystemState].
 * [CustomerEcosystemEngine] only computes *what happened*; this class is
 * solely responsible for *how state changes as a result* — that
 * separation is what makes "every cross-module interaction must go
 * through Events rather than direct module updates" a real, enforced
 * structure rather than a description. Nothing outside this class ever
 * mutates [CustomerEcosystemState] directly.
 */
class EcosystemEventReducer {

    fun apply(state: CustomerEcosystemState, event: EcosystemEvent): CustomerEcosystemState = when (event) {

        is EcosystemEvent.LoyaltyPointsEarned ->
            state.copy(loyaltyPoints = state.loyaltyPoints + event.points)

        is EcosystemEvent.WalletCashbackAdded -> {
            val transaction = DemoWalletTransaction(
                id = "txn_cashback_${state.walletTransactions.size}_${event.sourceLabel.hashCode()}",
                title = event.sourceLabel,
                amount = event.amount,
                isCredit = true,
                dateLabel = event.dateLabel,
            )
            state.copy(
                walletBalance = state.walletBalance + event.amount,
                walletTransactions = listOf(transaction) + state.walletTransactions,
            )
        }

        is EcosystemEvent.MembershipProgressUpdated ->
            state.copy(membershipProgressPoints = state.membershipProgressPoints + event.incrementPoints)

        is EcosystemEvent.BeautyTimelineEntryAdded ->
            state.copy(beautyTimelineEntries = listOf(event.entry) + state.beautyTimelineEntries)

        is EcosystemEvent.ReviewRequestCreated ->
            state

        is EcosystemEvent.NotificationEnqueued ->
            state.copy(notificationQueue = state.notificationQueue.enqueue(event.notification))

        is EcosystemEvent.AppointmentStatusChanged ->
            state.copy(
                appointments = state.appointments.map {
                    if (it.id == event.appointmentId) {
                        it.copy(status = event.newStatus, daysAgo = event.newDaysAgo)
                    } else it
                }
            )

        is EcosystemEvent.AppointmentRescheduled ->
            state.copy(
                appointments = state.appointments.map {
                    if (it.id == event.appointmentId) {
                        it.copy(dateKey = event.newDateKey, dateLabel = event.newDateLabel, time = event.newTime)
                    } else it
                }
            )

        is EcosystemEvent.WaitlistJoined ->
            state.copy(
                waitlistEntries = state.waitlistEntries + event.entry,
                waitlistSequenceCounter = state.waitlistSequenceCounter + 1,
            )

        is EcosystemEvent.WaitlistJoinRejected -> state

        is EcosystemEvent.WaitlistLeft ->
            state.copy(
                waitlistEntries = state.waitlistEntries.map {
                    if (it.id == event.entryId) it.copy(status = WaitlistStatus.LEFT) else it
                }
            )

        is EcosystemEvent.WaitlistPromoted ->
            state.copy(
                waitlistEntries = state.waitlistEntries.map {
                    if (it.id == event.entryId) it.copy(status = WaitlistStatus.PROMOTED) else it
                },
                appointments = state.appointments + event.newAppointment,
            )

        is EcosystemEvent.AppointmentBooked ->
            state.copy(appointments = state.appointments + event.appointment)

        is EcosystemEvent.CouponRedeemed ->
            state.copy(usedCouponIds = state.usedCouponIds + event.couponId)

        is EcosystemEvent.CouponRejected ->
            state

        is EcosystemEvent.ReviewLifecycleAdvanced -> {
            val existing = state.pendingReviewFor(event.appointmentId)
            val updatedList = if (existing == null) {
                state.pendingReviews + PendingReview(event.appointmentId, event.newStatus)
            } else {
                state.pendingReviews.map {
                    if (it.appointmentId == event.appointmentId) it.copy(status = event.newStatus) else it
                }
            }
            state.copy(pendingReviews = updatedList)
        }

        is EcosystemEvent.ReviewSubmitted ->
            state.copy(reviews = state.reviews + event.review)

        is EcosystemEvent.ReviewRejected ->
            state

        is EcosystemEvent.FavoriteSalonToggled ->
            state.copy(
                favoriteSalonIds = if (event.isNowFavorite) {
                    state.favoriteSalonIds + event.salonId
                } else {
                    state.favoriteSalonIds - event.salonId
                }
            )

        is EcosystemEvent.WalletDebited -> {
            val transaction = DemoWalletTransaction(
                id = "txn_debit_${state.walletTransactions.size}_${event.sourceLabel.hashCode()}",
                title = event.sourceLabel,
                amount = event.amount,
                isCredit = false,
                dateLabel = event.dateLabel,
            )
            state.copy(
                walletBalance = state.walletBalance - event.amount,
                walletTransactions = listOf(transaction) + state.walletTransactions,
            )
        }

        is EcosystemEvent.WalletDebitRejected ->
            state
    }

    fun applyAll(state: CustomerEcosystemState, events: List<EcosystemEvent>): CustomerEcosystemState =
        events.fold(state) { acc, event -> apply(acc, event) }
}
