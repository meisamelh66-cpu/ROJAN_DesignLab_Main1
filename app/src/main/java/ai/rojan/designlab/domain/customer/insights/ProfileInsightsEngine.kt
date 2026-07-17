package ai.rojan.designlab.domain.customer.insights

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.domain.customer.CustomerEcosystemState
import ai.rojan.designlab.domain.customer.rules.BeautyScoreRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderBeautyScoreRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderPreferredEntityRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderProfileCompletionRuleProvider
import ai.rojan.designlab.domain.customer.rules.PreferredEntityRuleProvider
import ai.rojan.designlab.domain.customer.rules.ProfileCompletionRuleProvider

/**
 * Profile "identity center" orchestrator — same discipline as
 * [ai.rojan.designlab.domain.booking.BookingEngine]/[ai.rojan.designlab.domain.customer.CustomerEcosystemEngine]:
 * this class orchestrates, it does not decide. Every genuine business
 * judgment call (what counts as a Beauty Score, what "preferred" means,
 * how completion is weighted) is delegated to an injected rule
 * provider, per "Business Rules are never embedded inside Engines."
 *
 * What *is* computed directly here — upcoming appointment, last visit,
 * recent activity — are factual lookups over existing state (nearest
 * future date, most recent past date, latest queued events), not policy
 * decisions with more than one reasonable answer. Gating those behind a
 * provider interface too would be over-engineering a fact-lookup as if
 * it were a judgment call.
 */
class ProfileInsightsEngine(
    private val beautyScoreRuleProvider: BeautyScoreRuleProvider = PlaceholderBeautyScoreRuleProvider(),
    private val profileCompletionRuleProvider: ProfileCompletionRuleProvider = PlaceholderProfileCompletionRuleProvider(),
    private val preferredEntityRuleProvider: PreferredEntityRuleProvider = PlaceholderPreferredEntityRuleProvider(),
) {

    fun computeInsights(state: CustomerEcosystemState): ProfileInsights {
        val hasCompletedAppointment = state.pastAppointments.any { it.status == AppointmentStatus.COMPLETED }
        val hasFavorite = state.favoriteSalonIds.isNotEmpty()
        val hasReview = state.reviews.isNotEmpty()

        val recentActivity = buildList {
            state.notificationQueue.items.takeLast(3).forEach {
                add(RecentActivityItem("نوبت تکمیل شد", it.createdAtLabel))
            }
            state.walletTransactions.take(2).forEach {
                add(RecentActivityItem(it.title, it.dateLabel))
            }
        }

        val lastVisit = state.pastAppointments.firstOrNull { it.status == AppointmentStatus.COMPLETED }

        return ProfileInsights(
            beautyScore = beautyScoreRuleProvider.calculate(state),
            profileCompletionPercent = profileCompletionRuleProvider.calculate(
                hasPhoto = true, // demo profile always has a placeholder avatar - genuinely true, not guessed
                hasBirthday = true, // static demo field, always set - see CustomerEcosystemState
                hasCompletedAppointment = hasCompletedAppointment,
                hasFavorite = hasFavorite,
                hasReview = hasReview,
            ),
            preferredSalonName = preferredEntityRuleProvider.preferredSalonName(state.appointments),
            preferredSpecialistName = preferredEntityRuleProvider.preferredSpecialistName(state.appointments),
            upcomingAppointment = state.upcomingAppointments.firstOrNull(),
            lastVisit = lastVisit,
            // Deterministic (Category 2, per the classification rule) - no provider needed.
            completedAppointmentCount = state.pastAppointments.count { it.status == AppointmentStatus.COMPLETED },
            daysSinceLastVisit = lastVisit?.daysAgo,
            recentActivity = recentActivity,
        )
    }
}
