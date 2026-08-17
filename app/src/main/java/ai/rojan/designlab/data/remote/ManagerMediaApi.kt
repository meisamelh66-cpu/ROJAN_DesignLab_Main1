package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.MediaAssetResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Central Salon Management — Salon Media UI. Retrofit contract for the
 * real, already-shipped Media Foundation (`ROJAN_Backend/api/.../media/MediaController.kt`,
 * verified directly against source) — `POST` is direct multipart upload,
 * not a signed-URL two-phase flow (see `MediaStoragePort`'s own backend
 * doc comment for why). The identity-slot assignment endpoint
 * (`PUT /identity-media`) lives on [ManagerSalonApi] instead, since it's a
 * write on the `Salon` resource itself, not on `MediaAsset`.
 */
interface ManagerMediaApi {

    @Multipart
    @POST("api/v1/salons/{salonId}/media")
    suspend fun upload(
        @Path("salonId") salonId: String,
        @Part file: MultipartBody.Part,
        @Part("mediaType") mediaType: RequestBody,
    ): MediaAssetResponseDto

    @GET("api/v1/salons/{salonId}/media")
    suspend fun list(
        @Path("salonId") salonId: String,
        @Query("mediaType") mediaType: String? = null,
    ): List<MediaAssetResponseDto>

    @DELETE("api/v1/salons/{salonId}/media/{mediaId}")
    suspend fun delete(
        @Path("salonId") salonId: String,
        @Path("mediaId") mediaId: String,
    )
}
