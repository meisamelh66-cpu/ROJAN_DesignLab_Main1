package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AuthResponseDto
import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.remote.dto.OtpIssuedResponseDto
import ai.rojan.designlab.data.remote.dto.OtpRequestDto
import ai.rojan.designlab.data.remote.dto.OtpVerifyRequestDto
import ai.rojan.designlab.data.remote.dto.RefreshRequestDto
import ai.rojan.designlab.data.remote.dto.RegisterRequestDto
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Retrofit contract for the ROJAN backend's auth API (`ROJAN_Backend/API.md`). */
interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): UserResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): AuthResponseDto

    @GET("api/v1/users/me")
    suspend fun me(): UserResponseDto

    /** OTP Authentication Entry Flow Integration — already-existing backend endpoint, not previously bound on the mobile side. */
    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequestDto): OtpIssuedResponseDto

    /** OTP Authentication Entry Flow Integration — already-existing backend endpoint, not previously bound on the mobile side. */
    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequestDto): AuthResponseDto
}
