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

/**
 * Real, non-fabricated identity fields for
 * [ai.rojan.designlab.manager.components.SalonIdentityCard] - resolved
 * from the same `GET /salons/mine` call
 * [ai.rojan.designlab.manager.data.ManagerRepositories.initialize]
 * already makes to learn the salon id, not a second network call.
 *
 * Phase A — Owner Salon Identity: [id]/[phone]/[email]/[address] added
 * (this type's own original doc comment already invited exactly this:
 * "add fields only when a screen actually needs them") so
 * [ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModel]
 * can reuse this one salon-identity type instead of introducing a second,
 * overlapping one. [SalonIdentityCard]'s existing call site is unaffected
 * - it already destructures individual fields, not this whole object.
 */
data class ManagerSalonSummary(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val email: String?,
    val address: String,
    val active: Boolean,
)
