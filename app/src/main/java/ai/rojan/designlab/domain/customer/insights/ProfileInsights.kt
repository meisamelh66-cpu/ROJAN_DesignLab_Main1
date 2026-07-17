package ai.rojan.designlab.domain.customer.insights

import ai.rojan.designlab.data.demo.DemoAppointment

/**
 * The Profile hub's "identity center" data — a computed snapshot, not
 * stored state. Nothing here is ever mutated directly; it's recomputed
 * from [ai.rojan.designlab.domain.customer.CustomerEcosystemState] on
 * every read via [ProfileInsightsEngine]. No Events/Reducer pair exists
 * for this — that pair is for discrete state *changes*, and nothing
 * here represents one; it's a read-only lens over state that already
 * changes through the normal event flow (completing an appointment,
 * etc.), not a second source of truth.
 */
data class ProfileInsights(
    val beautyScore: Int,
    val profileCompletionPercent: Int,
    val preferredSalonName: String?,
    val preferredSpecialistName: String?,
    val upcomingAppointment: DemoAppointment?,
    val lastVisit: DemoAppointment?,
    val completedAppointmentCount: Int,
    val daysSinceLastVisit: Int?,
    val recentActivity: List<RecentActivityItem>,
)

data class RecentActivityItem(
    val label: String,
    val dateLabel: String,
)
