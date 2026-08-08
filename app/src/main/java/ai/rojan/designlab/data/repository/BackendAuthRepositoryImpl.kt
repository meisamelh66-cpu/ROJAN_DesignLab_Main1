package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkUserRole
import ai.rojan.designlab.data.remote.dto.OtpIssuedResponseDto
import ai.rojan.designlab.data.remote.dto.OtpRequestDto
import ai.rojan.designlab.data.remote.dto.OtpVerifyRequestDto
import ai.rojan.designlab.data.remote.dto.RegisterRequestDto
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.TokenRepository

class BackendAuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenRepository: TokenRepository,
) : BackendAuthRepository {

    override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
        safeApiCall {
            authApi.register(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    fullName = fullName,
                    role = NetworkUserRole.CUSTOMER,
                ),
            ).toDomain()
        }

    override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
        safeApiCall {
            val response = authApi.login(LoginRequestDto(email = email, password = password))
            tokenRepository.saveTokens(response.accessToken, response.refreshToken)
            response.user.toDomain()
        }

    override suspend fun currentUser(): Result<AuthenticatedUser> =
        safeApiCall { authApi.me().toDomain() }

    override suspend fun requestOtp(phoneNumber: String): Result<OtpIssued> =
        safeApiCall { authApi.requestOtp(OtpRequestDto(phoneNumber)).toDomain() }

    override suspend fun verifyOtp(phoneNumber: String, code: String): Result<AuthenticatedUser> =
        safeApiCall {
            val response = authApi.verifyOtp(OtpVerifyRequestDto(phoneNumber = phoneNumber, code = code))
            tokenRepository.saveTokens(response.accessToken, response.refreshToken)
            response.user.toDomain()
        }

    private fun UserResponseDto.toDomain() = AuthenticatedUser(
        id = id,
        email = email,
        fullName = fullName,
        role = role.name,
    )

    private fun OtpIssuedResponseDto.toDomain() = OtpIssued(
        phoneNumber = phoneNumber,
        expiresInSeconds = expiresInSeconds,
        canResendAfterSeconds = canResendAfterSeconds,
    )
}
