package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.MarkAllReadResponseDto
import ai.rojan.designlab.data.remote.dto.NotificationResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UnreadCountResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the salon manager notifications API (`ROJAN_Backend/API_CONTRACT.md`).
 * All endpoints require Bearer JWT authentication and MANAGE_BOOKINGS permission on the salon.
 */
interface ManagerNotificationApi {

    @GET("api/v1/salons/{salonId}/notifications")
    suspend fun getNotifications(
        @Path("salonId") salonId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("category") category: String? = null,
    ): PagedResponseDto<NotificationResponseDto>

    @GET("api/v1/salons/{salonId}/notifications/unread-count")
    suspend fun getUnreadCount(
        @Path("salonId") salonId: String,
    ): UnreadCountResponseDto

    @PATCH("api/v1/salons/{salonId}/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("salonId") salonId: String,
        @Path("notificationId") notificationId: String,
    ): NotificationResponseDto

    @POST("api/v1/salons/{salonId}/notifications/mark-all-read")
    suspend fun markAllAsRead(
        @Path("salonId") salonId: String,
    ): MarkAllReadResponseDto
}
