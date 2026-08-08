package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format shape of `ROJAN_Backend`'s `GET /api/v1/dashboard/insights` (`DashboardController`/`DashboardDtos.kt`) — no `salonId` in the request, the backend resolves it from the caller's own JWT (401 if unauthenticated, 404 if the caller owns no salon, 409 if they own more than one). */
@Serializable
data class DashboardInsightsResponseDto(
    val revenue: RevenueResponseDto,
    val bookings: BookingCountsResponseDto,
    val customers: CustomerCountsResponseDto,
    val services: List<ServiceInsightResponseDto>,
    val recommendations: List<RecommendationResponseDto>,
)

@Serializable
data class RevenueResponseDto(val today: Double, val month: Double, val growthRate: Double)

@Serializable
data class BookingCountsResponseDto(val total: Long, val completed: Long, val cancelled: Long)

@Serializable
data class CustomerCountsResponseDto(val newCustomers: Int, val returningCustomers: Int)

@Serializable
data class ServiceInsightResponseDto(val name: String, val bookings: Long, val revenue: Double)

/** Ordinal order matches the backend's `RecommendationPriority` enum exactly - relied on for `Comparable` ordering to pick the single highest-priority recommendation to show. */
@Serializable
enum class NetworkRecommendationPriority { LOW, MEDIUM, HIGH }

/** [type] is decoded as a plain string, not a mirrored enum - nothing in this app reads it (only [priority]/[message] drive the UI), and a raw string can't break decoding if the backend adds a new recommendation type. */
@Serializable
data class RecommendationResponseDto(
    val type: String,
    val priority: NetworkRecommendationPriority,
    val message: String,
)
