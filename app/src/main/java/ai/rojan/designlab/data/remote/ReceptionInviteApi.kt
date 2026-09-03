package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.InviteDetailsResponseDto
import ai.rojan.designlab.data.remote.dto.SalonInviteAcceptedResponseDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit contract for `ROJAN_Backend`'s `InviteController`. [getInvite]
 * is unauthenticated (the QR-scan confirmation screen, reachable before the
 * caller has logged in at all); [acceptInvite] requires an existing
 * session. Built on the same authenticated [retrofit2.Retrofit] client as
 * [AuthApi] — safe for an unauthenticated call too, since
 * [AuthApi.requestOtp]/[AuthApi.verifyOtp] already make unauthenticated
 * calls through this exact client ([AuthInterceptor] only adds a bearer
 * header when a token is actually stored).
 */
interface ReceptionInviteApi {

    @GET("api/v1/invites/{token}")
    suspend fun getInvite(@Path("token") token: String): InviteDetailsResponseDto

    @POST("api/v1/invites/{token}/accept")
    suspend fun acceptInvite(@Path("token") token: String): SalonInviteAcceptedResponseDto
}
