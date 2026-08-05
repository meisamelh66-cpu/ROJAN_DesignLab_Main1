package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.DashboardInsightsResponseDto
import retrofit2.http.GET

/**
 * `ROJAN_Backend/api/dashboard/DashboardController.kt`. No `salonId` parameter — the backend
 * resolves the caller's salon from the JWT itself, and documents exactly three non-200 outcomes:
 * 401 (missing/invalid token), 404 (caller owns no salon), 409 (caller owns more than one salon -
 * context can't be resolved implicitly). All three surface as a real [ai.rojan.designlab.data.remote.BackendApiException]
 * via [safeApiCall] with that status code - see `BackendDashboardInsightsRepository` for how the UI maps them.
 */
interface ManagerDashboardApi {

    @GET("api/v1/dashboard/insights")
    suspend fun insights(): DashboardInsightsResponseDto
}
