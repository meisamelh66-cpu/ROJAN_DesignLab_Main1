package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSalonInviteRequestDto
import ai.rojan.designlab.data.remote.dto.SalonInviteResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * P1-6, Manager Salon Invite Management: owner-only Retrofit contract for
 * `ROJAN_Backend`'s `SalonInviteController` — confirmed real against
 * backend source this sprint. Every method requires
 * `Permission.MANAGE_MEMBERSHIP`, which `SalonRole.MANAGER.permissions()`
 * never grants — a manager-role (non-owner) caller gets a real `403` on
 * every one of these, surfaced through the existing [ai.rojan.designlab.presentation.common.userMessageFor]
 * mapping like any other access-denied response, not special-cased here.
 *
 * [getQrCode] returns [ResponseBody] rather than a converted type —
 * Retrofit handles [ResponseBody] specially (no converter factory
 * invoked), which is required here since [BackendApiContainer]'s shared
 * Retrofit instance only registers a JSON (kotlinx.serialization)
 * converter factory and this endpoint returns raw PNG bytes, not JSON.
 */
interface ManagerInviteApi {

    @POST("api/v1/salons/{salonId}/invites")
    suspend fun createInvite(
        @Path("salonId") salonId: String,
        @Body request: CreateSalonInviteRequestDto,
    ): SalonInviteResponseDto

    @GET("api/v1/salons/{salonId}/invites")
    suspend fun listInvites(
        @Path("salonId") salonId: String,
    ): List<SalonInviteResponseDto>

    @DELETE("api/v1/salons/{salonId}/invites/{inviteId}")
    suspend fun revokeInvite(
        @Path("salonId") salonId: String,
        @Path("inviteId") inviteId: String,
    )

    @GET("api/v1/salons/{salonId}/invites/{inviteId}/qr-code")
    suspend fun getQrCode(
        @Path("salonId") salonId: String,
        @Path("inviteId") inviteId: String,
        @Query("size") sizePx: Int = 512,
    ): ResponseBody
}
