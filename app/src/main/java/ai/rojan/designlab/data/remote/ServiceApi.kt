package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.MediaAssetResponseDto
import ai.rojan.designlab.data.remote.dto.ServiceResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's Service API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceApi {

    @GET("api/v1/salons/{salonId}/categories/{categoryId}/services")
    suspend fun getServices(
        @Path("salonId") salonId: String,
        @Path("categoryId") categoryId: String,
    ): List<ServiceResponseDto>

    /** Media System Evolution v2 - reuses the same authenticated `GET .../media` read every other media consumer does, scoped to this service's SERVICE_IMAGE set. */
    @GET("api/v1/salons/{salonId}/media")
    suspend fun getMedia(
        @Path("salonId") salonId: String,
        @Query("mediaType") mediaType: String,
        @Query("targetId") targetId: String,
    ): List<MediaAssetResponseDto>
}
