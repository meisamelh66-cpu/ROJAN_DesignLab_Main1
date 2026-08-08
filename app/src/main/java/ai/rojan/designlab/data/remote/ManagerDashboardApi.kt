package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.DashboardInsightsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * `ROJAN_Backend/api/dashboard/DashboardController.kt`. The real endpoint's
 * `salonId` query param is optional there (falls back to the caller's
 * single owned salon, 409 if they own more than one) - always passed
 * explicitly here instead, since [ai.rojan.designlab.manager.data.ManagerRepositories.initialize]
 * already resolves a concrete salon id via `GET /salons/mine` before this
 * call, so there's no reason to rely on the implicit fallback (or risk its
 * 409 for a multi-salon owner).
 */
interface ManagerDashboardApi {

    @GET("api/v1/dashboard/insights")
    suspend fun insights(@Query("salonId") salonId: String): DashboardInsightsResponseDto
}
