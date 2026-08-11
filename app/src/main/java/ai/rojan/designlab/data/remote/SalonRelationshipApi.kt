package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.SalonFavoriteDto
import ai.rojan.designlab.data.remote.dto.SalonFollowDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the ROJAN backend's Customer Relationship API
 * (`ROJAN_Backend` commit db5faea - `SalonFollowController`/
 * `SalonFavoriteController`). All six endpoints are customer-self-service:
 * the caller's identity is resolved by the backend from the JWT
 * (`CurrentUserResolver`), never sent in the request - there is no
 * customerId path/body parameter to pass here by design.
 *
 * The list endpoints are `/followed-salons`/`/favorite-salons` - the real,
 * committed paths, not `/follows`/`/favorites` - confirmed directly against
 * `SalonFollowController.kt`/`SalonFavoriteController.kt` in `ROJAN_Backend`,
 * since the backend is the source of truth over any spec document.
 */
interface SalonRelationshipApi {

    @POST("api/v1/customer/salons/{salonId}/follow")
    suspend fun follow(@Path("salonId") salonId: String): SalonFollowDto

    @DELETE("api/v1/customer/salons/{salonId}/follow")
    suspend fun unfollow(@Path("salonId") salonId: String)

    @GET("api/v1/customer/followed-salons")
    suspend fun getFollowedSalons(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PagedResponseDto<SalonFollowDto>

    @POST("api/v1/customer/salons/{salonId}/favorite")
    suspend fun favorite(@Path("salonId") salonId: String): SalonFavoriteDto

    @DELETE("api/v1/customer/salons/{salonId}/favorite")
    suspend fun unfavorite(@Path("salonId") salonId: String)

    @GET("api/v1/customer/favorite-salons")
    suspend fun getFavoriteSalons(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PagedResponseDto<SalonFavoriteDto>
}
