package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AssignMembershipRequestDto
import ai.rojan.designlab.data.remote.dto.SalonMembershipResponseDto
import ai.rojan.designlab.data.remote.dto.SetMembershipActiveRequestDto
import ai.rojan.designlab.data.remote.dto.StaffLookupResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Owner-only Retrofit contract for the ROJAN backend's Salon Membership
 * API (`ROJAN_Backend/API_CONTRACT.md`, `SalonMembershipController`) —
 * direct role assignment for an existing account only (MVP; no
 * invite-by-email flow exists yet).
 */
interface SalonMembershipApi {

    @GET("api/v1/salons/{salonId}/members")
    suspend fun list(@Path("salonId") salonId: String): List<SalonMembershipResponseDto>

    /** Staff Access Phase 1: resolves an existing ROJAN account by phone number before assigning it a staff role. */
    @GET("api/v1/salons/{salonId}/members/lookup")
    suspend fun lookup(@Path("salonId") salonId: String, @Query("phone") phone: String): StaffLookupResponseDto

    @PUT("api/v1/salons/{salonId}/members/{userId}")
    suspend fun assign(
        @Path("salonId") salonId: String,
        @Path("userId") userId: String,
        @Body request: AssignMembershipRequestDto,
    ): SalonMembershipResponseDto

    /** Staff Access Phase 1: disable/re-enable without deleting the membership - distinct from [remove]'s true removal. */
    @PATCH("api/v1/salons/{salonId}/members/{userId}/status")
    suspend fun setActive(
        @Path("salonId") salonId: String,
        @Path("userId") userId: String,
        @Body request: SetMembershipActiveRequestDto,
    ): SalonMembershipResponseDto

    @DELETE("api/v1/salons/{salonId}/members/{userId}")
    suspend fun remove(@Path("salonId") salonId: String, @Path("userId") userId: String)
}
