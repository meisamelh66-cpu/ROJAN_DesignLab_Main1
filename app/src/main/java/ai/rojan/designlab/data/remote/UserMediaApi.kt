package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.UserResponseDto
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Customer Profile Personalization Phase 5B. Retrofit contract for the
 * backend's USER-owned profile media endpoints (`ROJAN_Backend` Phase 5A —
 * `POST` / `DELETE /api/v1/users/me/media/{avatar,cover}`, verified against
 * source). Every call is scoped to the authenticated caller by the backend
 * (JWT subject, no id parameter anywhere) and returns the updated
 * [UserResponseDto] with the resolved `avatarUrl` / `coverUrl`.
 *
 * Direct multipart upload (a single `file` part), mirroring [ManagerMediaApi]
 * — not a signed-URL flow.
 */
interface UserMediaApi {

    @Multipart
    @POST("api/v1/users/me/media/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserResponseDto

    @Multipart
    @POST("api/v1/users/me/media/cover")
    suspend fun uploadCover(@Part file: MultipartBody.Part): UserResponseDto

    @DELETE("api/v1/users/me/media/avatar")
    suspend fun deleteAvatar(): UserResponseDto

    @DELETE("api/v1/users/me/media/cover")
    suspend fun deleteCover(): UserResponseDto
}
