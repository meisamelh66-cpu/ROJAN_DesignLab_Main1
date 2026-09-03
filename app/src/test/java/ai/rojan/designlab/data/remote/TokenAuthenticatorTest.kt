package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AuthResponseDto
import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkUserRole
import ai.rojan.designlab.data.remote.dto.OtpIssuedResponseDto
import ai.rojan.designlab.data.remote.dto.OtpRequestDto
import ai.rojan.designlab.data.remote.dto.OtpVerifyRequestDto
import ai.rojan.designlab.data.remote.dto.RefreshRequestDto
import ai.rojan.designlab.data.remote.dto.RegisterRequestDto
import ai.rojan.designlab.data.remote.dto.SalonAccessResponseDto
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Auth Reliability test coverage: [TokenAuthenticator] in isolation.
 * [Authenticator.authenticate] is a plain synchronous JVM function (an
 * OkHttp SPI callback, not a suspend function), so these tests call it
 * directly — no coroutine test dispatcher, no ViewModel scope, matching
 * the class's own `runBlocking` usage rather than fighting it.
 *
 * Deliberately does **not** test any concurrency coordination between
 * multiple simultaneous [okhttp3.Authenticator.authenticate] invocations
 * (C1) or refresh-failure-type differentiation (C2) — both out of scope
 * for this task; [TokenAuthenticator]'s current behavior (one refresh
 * attempt per call, any failure uniformly clears both persistence layers)
 * is exactly what's under test here, not changed.
 */
class TokenAuthenticatorTest {

    private val originalRequest = Request.Builder().url("https://example.com/foo").build()

    private fun response(code: Int, priorResponse: Response? = null): Response =
        Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Unauthorized")
            .priorResponse(priorResponse)
            .build()

    private fun authResponse(accessToken: String, refreshToken: String) = AuthResponseDto(
        user = UserResponseDto(id = "user-1", email = "test@example.com", fullName = "Test User", role = NetworkUserRole.CUSTOMER),
        accessToken = accessToken,
        accessTokenExpiresAt = "2026-09-04T00:00:00Z",
        refreshToken = refreshToken,
        refreshTokenExpiresAt = "2026-10-04T00:00:00Z",
    )

    @Test
    fun `first 401 triggers exactly one refresh call and saves the new tokens`() {
        val tokenRepository = FakeTokenRepository(refreshToken = "old-refresh-token")
        val authApi = FakeAuthApi(refreshResult = Result.success(authResponse("new-access-token", "new-refresh-token")))
        val authSessionRepository = FakeAuthSessionRepository()
        val authenticator = TokenAuthenticator(tokenRepository, authApi, authSessionRepository)

        authenticator.authenticate(null, response(401))

        assertEquals(1, authApi.refreshCallCount)
        assertEquals("old-refresh-token", authApi.lastRefreshRequest?.refreshToken)
        assertEquals("new-access-token" to "new-refresh-token", tokenRepository.savedTokens)
    }

    @Test
    fun `successful refresh returns a retried request authorized with the new access token`() {
        val tokenRepository = FakeTokenRepository(refreshToken = "old-refresh-token")
        val authApi = FakeAuthApi(refreshResult = Result.success(authResponse("new-access-token", "new-refresh-token")))
        val authenticator = TokenAuthenticator(tokenRepository, authApi, FakeAuthSessionRepository())

        val retried = authenticator.authenticate(null, response(401))

        assertEquals("Bearer new-access-token", retried?.header("Authorization"))
    }

    @Test
    fun `a second 401 on the retried request returns null and does not attempt another refresh`() {
        val tokenRepository = FakeTokenRepository(refreshToken = "old-refresh-token")
        val authApi = FakeAuthApi(refreshResult = Result.success(authResponse("new-access-token", "new-refresh-token")))
        val authenticator = TokenAuthenticator(tokenRepository, authApi, FakeAuthSessionRepository())

        val firstResponse = response(401)
        val secondResponse = response(401, priorResponse = firstResponse)

        val result = authenticator.authenticate(null, secondResponse)

        assertNull(result)
        assertEquals(0, authApi.refreshCallCount) // never even attempted - gave up before calling refresh at all
    }

    @Test
    fun `no stored refresh token returns null without ever calling the network`() {
        val tokenRepository = FakeTokenRepository(refreshToken = null)
        val authApi = FakeAuthApi(refreshResult = Result.failure(IllegalStateException("should not be called")))
        val authenticator = TokenAuthenticator(tokenRepository, authApi, FakeAuthSessionRepository())

        val result = authenticator.authenticate(null, response(401))

        assertNull(result)
        assertEquals(0, authApi.refreshCallCount)
    }

    @Test
    fun `refresh failure clears both the token repository and the persisted personId`() {
        val tokenRepository = FakeTokenRepository(refreshToken = "old-refresh-token")
        val authApi = FakeAuthApi(refreshResult = Result.failure(IOException("network down")))
        val authSessionRepository = FakeAuthSessionRepository()
        val authenticator = TokenAuthenticator(tokenRepository, authApi, authSessionRepository)

        val result = authenticator.authenticate(null, response(401))

        assertNull(result)
        assertEquals(1, tokenRepository.clearCallCount)
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
        assertNull(tokenRepository.savedTokens) // no tokens were ever saved on the failure path
    }

    private class FakeTokenRepository(
        private var refreshToken: String?,
    ) : TokenRepository {
        var savedTokens: Pair<String, String>? = null
            private set
        var clearCallCount = 0
            private set

        override fun saveTokens(accessToken: String, refreshToken: String) {
            savedTokens = accessToken to refreshToken
        }

        override fun clearTokens() {
            clearCallCount++
        }

        override fun accessToken(): String? = error("not used")

        override fun refreshToken(): String? = refreshToken
    }

    private class FakeAuthSessionRepository : AuthSessionRepository {
        var clearPersonIdCallCount = 0
            private set

        override suspend fun savePersonId(personId: String) = error("not used")

        override suspend fun clearPersonId() {
            clearPersonIdCallCount++
        }

        override fun observePersonId(): Flow<String?> = flowOf(null)
    }

    private class FakeAuthApi(private val refreshResult: Result<AuthResponseDto>) : AuthApi {
        var refreshCallCount = 0
            private set
        var lastRefreshRequest: RefreshRequestDto? = null
            private set

        override suspend fun register(request: RegisterRequestDto): UserResponseDto = error("not used")
        override suspend fun login(request: LoginRequestDto): AuthResponseDto = error("not used")

        override suspend fun refresh(request: RefreshRequestDto): AuthResponseDto {
            refreshCallCount++
            lastRefreshRequest = request
            return refreshResult.getOrThrow()
        }

        override suspend fun me(): UserResponseDto = error("not used")
        override suspend fun getSalonAccess(): SalonAccessResponseDto = error("not used")
        override suspend fun requestOtp(request: OtpRequestDto): OtpIssuedResponseDto = error("not used")
        override suspend fun verifyOtp(request: OtpVerifyRequestDto): AuthResponseDto = error("not used")
    }
}
