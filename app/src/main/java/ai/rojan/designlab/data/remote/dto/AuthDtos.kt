package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for the ROJAN backend's auth API — see
 * `ROJAN_Backend/API.md` for the authoritative contract. Deliberately
 * separate from any domain type ([ai.rojan.designlab.domain.repository.AuthenticatedUser]
 * is the domain-facing shape); nothing outside `data/remote` should depend
 * on these directly.
 */

@Serializable
enum class NetworkUserRole {
    CUSTOMER,
    MANAGER,
    SPECIALIST,
}

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val fullName: String,
    val role: NetworkUserRole,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: NetworkUserRole,
)

@Serializable
data class AuthResponseDto(
    val user: UserResponseDto,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
)

/**
 * OTP Authentication Entry Flow Integration: wire-format DTOs for the
 * backend's already-existing `POST /api/v1/auth/otp/request` and
 * `POST /api/v1/auth/otp/verify` (see `ROJAN_Backend/api/.../auth/OtpDtos.kt`
 * for the authoritative field shapes this mirrors exactly) — no backend
 * change, this only adds the mobile-side bindings for an API that was
 * already there and previously unused by this app.
 */
@Serializable
data class OtpRequestDto(val phoneNumber: String)

@Serializable
data class OtpVerifyRequestDto(val phoneNumber: String, val code: String, val fullName: String? = null)

@Serializable
data class OtpIssuedResponseDto(val phoneNumber: String, val expiresInSeconds: Long, val canResendAfterSeconds: Long)
