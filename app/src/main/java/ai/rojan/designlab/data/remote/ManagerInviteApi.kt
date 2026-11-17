package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSalonInviteRequestDto
import ai.rojan.designlab.data.remote.dto.SalonInviteResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Owner-only Retrofit contract for the ROJAN backend's staff-invite API
 * (`ai.rojan.backend.api.salon.SalonInviteController`,
 * `Permission.MANAGE_MEMBERSHIP`). Only [create] is wired - Gap
 * P0-3-REC-INVITE-UI's scope is issuing a Reception invite and showing the
 * result, not listing or revoking existing invites.
 */
interface ManagerInviteApi {

    @POST("api/v1/salons/{salonId}/invites")
    suspend fun create(
        @Path("salonId") salonId: String,
        @Body request: CreateSalonInviteRequestDto,
    ): SalonInviteResponseDto
}
