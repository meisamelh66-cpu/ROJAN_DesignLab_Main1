package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerDashboardApi
import ai.rojan.designlab.data.remote.dto.DashboardInsightsResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.dashboard.ManagerDashboardInsights

/**
 * Real backend-backed source for [ManagerDashboardInsights] (Phase 2,
 * M6) — replaces the Dashboard KPI grid's client-side
 * `computeManagerDashboardStats()` revenue/new-customer computation
 * (and the AI Insight card's static placeholder copy) with the real,
 * server-computed `GET /dashboard/insights`. No cache/sync split like
 * [BackendServiceRepository]/[BackendCustomerRepository] — there's a
 * single consumer ([ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen])
 * reading a single snapshot once per screen entry, not a list read from
 * many call sites.
 */
class BackendDashboardRepository(
    private val managerDashboardApi: ManagerDashboardApi,
    private val salonId: String,
) {

    suspend fun fetch(): Result<ManagerDashboardInsights> =
        safeApiCall { managerDashboardApi.insights(salonId) }.map { it.toDomain() }

    private fun DashboardInsightsResponseDto.toDomain() = ManagerDashboardInsights(
        todaysRevenue = revenue.today,
        newCustomersThisMonth = customers.newCustomers,
        topRecommendationMessage = recommendations.maxByOrNull { it.priority }?.message,
    )
}
