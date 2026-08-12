package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's Salon API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SalonApi {

    @GET("api/v1/salons")
    suspend fun browseSalons(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("name") name: String?,
        @Query("sortDirection") sortDirection: String,
    ): PagedResponseDto<SalonResponseDto>

    @GET("api/v1/salons/{salonId}")
    suspend fun getSalon(@Path("salonId") salonId: String): SalonResponseDto
}
