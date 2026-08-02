package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.ServiceCategoryResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Retrofit contract for the ROJAN backend's Service Category API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceCategoryApi {

    @GET("api/v1/salons/{salonId}/categories")
    suspend fun getCategories(@Path("salonId") salonId: String): List<ServiceCategoryResponseDto>
}
