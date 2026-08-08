package ai.rojan.designlab.manager.domain.dashboard

/**
 * The subset of the real `GET /dashboard/insights` response the Manager
 * Dashboard actually renders (Phase 2, M6) - not a 1:1 mirror of every
 * backend field, since nothing here currently reads month-over-month
 * booking counts or the per-service breakdown. Add fields only when a
 * screen actually needs them.
 */
data class ManagerDashboardInsights(
    val todaysRevenue: Double,
    val newCustomersThisMonth: Int,
    /** The single highest-priority recommendation from the backend's rule-based engine, if any fired. */
    val topRecommendationMessage: String?,
)

/** Real, non-fabricated identity fields for [ai.rojan.designlab.manager.components.SalonIdentityCard] - resolved from the same `GET /salons/mine` call [ai.rojan.designlab.manager.data.ManagerRepositories.initialize] already makes to learn the salon id, not a second network call. */
data class ManagerSalonSummary(
    val name: String,
    val description: String?,
    val active: Boolean,
)
