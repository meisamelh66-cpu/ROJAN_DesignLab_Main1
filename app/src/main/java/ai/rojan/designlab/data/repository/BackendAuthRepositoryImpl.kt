package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkUserRole
import ai.rojan.designlab.data.remote.dto.RegisterRequestDto
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.TokenRepository

class BackendAuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenRepository: TokenRepository,
) : BackendAuthRepository {

    override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
        runCatching {
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
        runCatching {
            val response = authApi.login(LoginRequestDto(email = email, password = password))
            tokenRepository.saveTokens(response.accessToken, response.refreshToken)
            response.user.toDomain()
        }

    override suspend fun currentUser(): Result<AuthenticatedUser> =
        runCatching { authApi.me().toDomain() }

    private fun UserResponseDto.toDomain() = AuthenticatedUser(
        id = id,
        email = email,
        fullName = fullName,
        role = role.name,
    )
}
