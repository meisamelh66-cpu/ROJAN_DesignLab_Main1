package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.repository.BackendAuthRepositoryImpl
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Verifies the networking/token stack (AuthApi, AuthInterceptor,
 * TokenAuthenticator, BackendAuthRepositoryImpl) end-to-end against a REAL
 * running backend — not a mock. **Requires `ROJAN_Backend`'s Spring Boot
 * app running locally at http://localhost:8080** (see
 * `ROJAN_Backend/README.md`) before running this test; it is a manual
 * connectivity-verification trigger, not a hermetic unit test, and is not
 * wired into any CI/build gate.
 *
 * Runs as a plain JVM test (not instrumented), so it talks to
 * `localhost:8080` directly rather than [NetworkConfig.BASE_URL]'s
 * emulator alias, and uses an in-memory [FakeTokenRepository] rather than
 * the real EncryptedSharedPreferences-backed
 * [ai.rojan.designlab.data.repository.TokenRepositoryImpl], since that
 * needs a real Android Keystore (only available via an instrumented test)
 * — this test's job is to verify the network/refresh logic against the
 * real server, not re-verify a well-established platform crypto API.
 */
class BackendAuthFlowVerificationTest {

    private class FakeTokenRepository : TokenRepository {
        private var access: String? = null
        private var refresh: String? = null

        override fun saveTokens(accessToken: String, refreshToken: String) {
            access = accessToken
            refresh = refreshToken
        }

        override fun clearTokens() {
            access = null
            refresh = null
        }

        override fun accessToken(): String? = access
        override fun refreshToken(): String? = refresh
    }

    /** Not under test here - TokenAuthenticator's clearPersonId-on-failed-refresh is covered elsewhere; this just satisfies the constructor. */
    private class FakeAuthSessionRepository : AuthSessionRepository {
        override suspend fun savePersonId(personId: String) {}
        override suspend fun clearPersonId() {}
        override fun observePersonId(): Flow<String?> = flowOf(null)
        override suspend fun saveRememberMe(remember: Boolean) {}
        override fun observeRememberMe(): Flow<Boolean> = flowOf(true)
    }

    private val baseUrl = "http://localhost:8080/"
    private val jsonConverterFactory = Json { ignoreUnknownKeys = true }
        .asConverterFactory("application/json".toMediaType())
    private val tokenRepository = FakeTokenRepository()
    private val authSessionRepository = FakeAuthSessionRepository()

    private val plainAuthApi: AuthApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(jsonConverterFactory)
        .build()
        .create(AuthApi::class.java)

    private val authApi: AuthApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenRepository))
                .authenticator(TokenAuthenticator(tokenRepository, plainAuthApi, authSessionRepository))
                .build(),
        )
        .addConverterFactory(jsonConverterFactory)
        .build()
        .create(AuthApi::class.java)

    private val repository = BackendAuthRepositoryImpl(authApi, tokenRepository)

    @Test
    fun `register, login, an authenticated call, and refresh all succeed against the live backend`() = runBlocking {
        val email = "android-verify.${System.nanoTime()}@example.com"
        val password = "supersecret123"

        val registered = repository.register(email = email, password = password, fullName = "Android Verify")
        assertTrue("register failed: ${registered.exceptionOrNull()}", registered.isSuccess)
        assertEquals(email, registered.getOrNull()?.email)

        val loggedIn = repository.login(email = email, password = password)
        assertTrue("login failed: ${loggedIn.exceptionOrNull()}", loggedIn.isSuccess)
        assertNotNull(tokenRepository.accessToken())
        assertNotNull(tokenRepository.refreshToken())

        // The one authenticated call: proves AuthInterceptor actually attaches the bearer token.
        val me = repository.currentUser()
        assertTrue("authenticated /me call failed: ${me.exceptionOrNull()}", me.isSuccess)
        assertEquals(email, me.getOrNull()?.email)

        // Force an expiry scenario by corrupting the stored access token (the server will
        // reject it, exactly like a real expired token would), then prove TokenAuthenticator
        // transparently refreshes and the retried call still succeeds.
        val refreshTokenBeforeForce = requireNotNull(tokenRepository.refreshToken())
        tokenRepository.saveTokens("deliberately-invalid-access-token", refreshTokenBeforeForce)

        val meAfterForcedExpiry = repository.currentUser()
        assertTrue(
            "authenticated call after forced token expiry failed: ${meAfterForcedExpiry.exceptionOrNull()}",
            meAfterForcedExpiry.isSuccess,
        )
        assertEquals(email, meAfterForcedExpiry.getOrNull()?.email)
        assertNotEquals("deliberately-invalid-access-token", tokenRepository.accessToken())
    }

    @Test
    fun `login with the wrong password fails cleanly`() = runBlocking {
        val email = "android-verify-badpass.${System.nanoTime()}@example.com"
        repository.register(email = email, password = "correct-password-123", fullName = "Bad Pass")

        val result = repository.login(email = email, password = "wrong-password")

        assertTrue(result.isFailure)
    }

    /** Direct low-level sanity check, independent of the repository layer above. */
    @Test
    fun `raw login call returns a well-formed token pair`() = runBlocking {
        val email = "android-verify-raw.${System.nanoTime()}@example.com"
        val password = "supersecret123"
        repository.register(email = email, password = password, fullName = "Raw Check")

        val response = plainAuthApi.login(LoginRequestDto(email = email, password = password))

        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
        assertNotEquals(response.accessToken, response.refreshToken)
        assertEquals(email, response.user.email)
    }
}
