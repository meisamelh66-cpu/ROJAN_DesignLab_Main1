package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AssignMembershipRequestDto
import ai.rojan.designlab.data.remote.dto.SalonMembershipResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Owner-only Retrofit contract for the ROJAN backend's Salon Membership
 * API (`ROJAN_Backend/API_CONTRACT.md`, `SalonMembershipController`) —
 * direct role assignment for an existing account only (MVP; no
 * invite-by-email flow exists yet).
 */
interface SalonMembershipApi {

    @GET("api/v1/salons/{salonId}/members")
    suspend fun list(@Path("salonId") salonId: String): List<SalonMembershipResponseDto>

    @PUT("api/v1/salons/{salonId}/members/{userId}")
    suspend fun assign(
        @Path("salonId") salonId: String,
        @Path("userId") userId: String,
        @Body request: AssignMembershipRequestDto,
    ): SalonMembershipResponseDto

    @DELETE("api/v1/salons/{salonId}/members/{userId}")
    suspend fun remove(@Path("salonId") salonId: String, @Path("userId") userId: String)
}
