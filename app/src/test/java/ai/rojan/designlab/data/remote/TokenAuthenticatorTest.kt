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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response as RetrofitResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Hermetic unit tests for [TokenAuthenticator] — no network, no Android
 * dependencies. Complements [BackendAuthFlowVerificationTest] (which runs
 * the same class against a real backend).
 *
 * Focus: C1 (concurrent-refresh race) plus regression coverage for the
 * pre-existing single-401, loop-guard, missing-refresh-token, and
 * clear-session-on-failed-refresh (C2) behaviour, which must not regress.
 */
class TokenAuthenticatorTest {

    private companion object {
        const val OLD_ACCESS = "OLD_ACCESS_TOKEN"
        const val VALID_REFRESH = "VALID_REFRESH_TOKEN"
        const val NEW_ACCESS = "NEW_ACCESS_TOKEN"
        const val NEW_REFRESH = "NEW_REFRESH_TOKEN"
    }

    // ---- fakes -----------------------------------------------------------

    private class FakeTokenRepository : TokenRepository {
        @Volatile private var access: String? = null
        @Volatile private var refresh: String? = null

        fun seed(accessToken: String?, refreshToken: String?) {
            access = accessToken
            refresh = refreshToken
        }

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

    private class RecordingAuthSessionRepository : AuthSessionRepository {
        val clearPersonIdCount = AtomicInteger(0)
        override suspend fun savePersonId(personId: String) = Unit
        override suspend fun clearPersonId() {
            clearPersonIdCount.incrementAndGet()
        }
        override fun observePersonId(): Flow<String?> = flowOf(null)
    }

    /**
     * Only [refresh] is exercised by [TokenAuthenticator]; every other
     * method fails loudly so an accidental call is obvious.
     */
    private class FakeAuthApi(
        var onRefresh: (RefreshRequestDto) -> AuthResponseDto,
    ) : AuthApi {
        override suspend fun refresh(request: RefreshRequestDto): AuthResponseDto = onRefresh(request)

        override suspend fun register(request: RegisterRequestDto): UserResponseDto =
            error("not used by TokenAuthenticator")
        override suspend fun login(request: LoginRequestDto): AuthResponseDto =
            error("not used by TokenAuthenticator")
        override suspend fun me(): UserResponseDto = error("not used by TokenAuthenticator")
        override suspend fun getSalonAccess(): SalonAccessResponseDto =
            error("not used by TokenAuthenticator")
        override suspend fun requestOtp(request: OtpRequestDto): OtpIssuedResponseDto =
            error("not used by TokenAuthenticator")
        override suspend fun verifyOtp(request: OtpVerifyRequestDto): AuthResponseDto =
            error("not used by TokenAuthenticator")
    }

    // ---- helpers -------------------------------------------------------

    private val tokenRepo = FakeTokenRepository()
    private val session = RecordingAuthSessionRepository()
    private val fakeApi = FakeAuthApi(onRefresh = { newTokensResponse() })
    private val authenticator = TokenAuthenticator(tokenRepo, fakeApi, session)

    private fun newTokensResponse(
        accessToken: String = NEW_ACCESS,
        refreshToken: String = NEW_REFRESH,
    ) = AuthResponseDto(
        user = UserResponseDto(id = "u1", fullName = "Test", role = NetworkUserRole.CUSTOMER),
        accessToken = accessToken,
        accessTokenExpiresAt = "2099-01-01T00:00:00",
        refreshToken = refreshToken,
        refreshTokenExpiresAt = "2099-01-01T00:00:00",
    )

    private fun response401(bearer: String?, prior: Response? = null): Response {
        val requestBuilder = Request.Builder().url("https://rojan.test/api/v1/protected")
        if (bearer != null) requestBuilder.header("Authorization", "Bearer $bearer")
        val builder = Response.Builder()
            .request(requestBuilder.build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
        if (prior != null) builder.priorResponse(prior)
        return builder.build()
    }

    /**
     * A genuine-rejection refresh failure, as `safeApiCall` (`SafeApiCall.kt`) actually produces
     * one: only its `catch (e: HttpException)` branch constructs a [BackendApiException] (from a
     * real Retrofit [HttpException]'s status code) - throwing a [BackendApiException] directly from
     * a fake API, as an earlier version of this test did, is caught by `safeApiCall`'s broader
     * `catch (e: IOException)` branch instead (`BackendApiException` extends `IOException`) and
     * silently becomes a non-genuine [NetworkUnavailableException], which is why that version of
     * this test failed despite looking right.
     */
    private fun genuineRejection(code: Int): HttpException =
        HttpException(RetrofitResponse.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

    // ---- A. single 401 ------------------------------------------------

    @Test
    fun `single 401 refreshes once and retries with the new access token`() {
        tokenRepo.seed(OLD_ACCESS, VALID_REFRESH)
        val refreshCount = AtomicInteger(0)
        fakeApi.onRefresh = {
            refreshCount.incrementAndGet()
            assertEquals(VALID_REFRESH, it.refreshToken)
            newTokensResponse()
        }

        val retried = authenticator.authenticate(null, response401(bearer = OLD_ACCESS))

        assertEquals(1, refreshCount.get())
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS", retried!!.header("Authorization"))
        assertEquals(NEW_ACCESS, tokenRepo.accessToken())
        assertEquals(NEW_REFRESH, tokenRepo.refreshToken())
        assertEquals(0, session.clearPersonIdCount.get())
    }

    // ---- B / C. concurrent 401 --------------------------------------

    private fun assertConcurrentBurstRefreshesOnce(callers: Int) {
        tokenRepo.seed(OLD_ACCESS, VALID_REFRESH)

        val startTogether = CyclicBarrier(callers)
        // counts DOWN once per fake refresh invocation
        val refreshInvoked = CountDownLatch(callers)
        // blocks every fake refresh until the test releases it
        val releaseRefresh = CountDownLatch(1)
        val refreshCount = AtomicInteger(0)

        fakeApi.onRefresh = {
            refreshCount.incrementAndGet()
            refreshInvoked.countDown()
            assertTrue("fake refresh was never released", releaseRefresh.await(10, TimeUnit.SECONDS))
            newTokensResponse()
        }

        val response = response401(bearer = OLD_ACCESS)
        val retried = arrayOfNulls<Request>(callers)
        val workers = (0 until callers).map { i ->
            thread(name = "auth-worker-$i") {
                // all workers enter authenticate() at the same instant
                startTogether.await(10, TimeUnit.SECONDS)
                retried[i] = authenticator.authenticate(null, response)
            }
        }

        // Deterministic race window. The racy (unsynchronised) code lets
        // *every* worker reach the fake refresh -> refreshInvoked hits 0
        // essentially immediately after the barrier. The fixed code lets
        // exactly one worker in; the rest park on refreshLock and never
        // call the fake -> refreshInvoked stays > 0 and this bounded await
        // returns false. The race is created by the barrier + missing
        // lock, NOT by this timeout.
        val everyCallerReachedRefresh = refreshInvoked.await(2, TimeUnit.SECONDS)

        releaseRefresh.countDown()
        workers.forEach { worker ->
            worker.join(10_000)
            assertFalse("worker ${worker.name} did not finish", worker.isAlive)
        }

        assertEquals(
            "a concurrent 401 burst of $callers must trigger exactly one /auth/refresh " +
                "(was ${refreshCount.get()})",
            1,
            refreshCount.get(),
        )
        assertFalse(
            "$callers callers all reached the refresh call — refreshLock is not serialising them",
            everyCallerReachedRefresh,
        )
        retried.forEachIndexed { i, request ->
            assertNotNull("caller $i produced no retry request", request)
            assertEquals(
                "caller $i retried with the wrong access token",
                "Bearer $NEW_ACCESS",
                request!!.header("Authorization"),
            )
        }
        assertEquals(NEW_ACCESS, tokenRepo.accessToken())
        assertEquals(NEW_REFRESH, tokenRepo.refreshToken())
        assertEquals(0, session.clearPersonIdCount.get())
    }

    @Test
    fun `two concurrent 401 callers perform exactly one refresh`() =
        assertConcurrentBurstRefreshesOnce(callers = 2)

    @Test
    fun `three concurrent 401 callers perform exactly one refresh`() =
        assertConcurrentBurstRefreshesOnce(callers = 3)

    // ---- D. loop guard ---------------------------------------------

    @Test
    fun `a 401 on an already-retried request does not refresh again`() {
        tokenRepo.seed(NEW_ACCESS, VALID_REFRESH)
        val refreshCount = AtomicInteger(0)
        fakeApi.onRefresh = { refreshCount.incrementAndGet(); newTokensResponse() }

        // response chain: original 401 -> retry -> retry also 401
        val firstFailure = response401(bearer = OLD_ACCESS)
        val retryAlsoFailed = response401(bearer = NEW_ACCESS, prior = firstFailure)

        val result = authenticator.authenticate(null, retryAlsoFailed)

        assertNull("must give up rather than loop", result)
        assertEquals("no refresh once the retry chain shows repeated 401", 0, refreshCount.get())
        assertEquals(0, session.clearPersonIdCount.get())
    }

    // ---- E. missing refresh token --------------------------------

    @Test
    fun `no refresh token means no refresh call and a null retry`() {
        tokenRepo.seed(OLD_ACCESS, refreshToken = null)
        val refreshCount = AtomicInteger(0)
        fakeApi.onRefresh = { refreshCount.incrementAndGet(); newTokensResponse() }

        val result = authenticator.authenticate(null, response401(bearer = OLD_ACCESS))

        assertNull(result)
        assertEquals(0, refreshCount.get())
        // tokens/session are left as they are — nothing to clear here
        assertEquals(OLD_ACCESS, tokenRepo.accessToken())
        assertEquals(0, session.clearPersonIdCount.get())
    }

    // ---- F. C2 — clear session on a failed refresh --------------

    @Test
    fun `a failed refresh clears the token pair and the persisted person id`() {
        // C2 (session-persistence fix), corrected (Final Blocker Closure Sprint): the real
        // implementation only clears the session on a *genuine* rejection - a BackendApiException
        // whose statusCode is in GENUINE_REJECTION_STATUS_CODES (400/401/403), confirmed by direct
        // read of TokenAuthenticator.authenticate(). A transient/transport failure (a plain
        // IOException - offline, timeout, 5xx) must NOT clear the session, since the same refresh
        // token may still work on a later attempt; this test previously simulated that transient
        // case while asserting the genuine-rejection outcome, which is why it failed. Simulating the
        // real genuine-rejection case here instead (see genuineRejection()'s own doc comment for why
        // a plain BackendApiException throw doesn't work) - this test had zero actual coverage of
        // that path before this fix.
        tokenRepo.seed(OLD_ACCESS, VALID_REFRESH)
        val refreshCount = AtomicInteger(0)
        fakeApi.onRefresh = {
            refreshCount.incrementAndGet()
            throw genuineRejection(401)
        }

        val result = authenticator.authenticate(null, response401(bearer = OLD_ACCESS))

        assertNull(result)
        assertEquals(1, refreshCount.get())
        assertNull("access token must be cleared", tokenRepo.accessToken())
        assertNull("refresh token must be cleared", tokenRepo.refreshToken())
        assertEquals("persisted personId must be cleared exactly once", 1, session.clearPersonIdCount.get())
    }

    // ---- G. C1 + C2 together -----------------------------------

    @Test
    fun `when the single refresh fails, other concurrent callers bail without a second refresh`() {
        tokenRepo.seed(OLD_ACCESS, VALID_REFRESH)

        val callers = 3
        val startTogether = CyclicBarrier(callers)
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val refreshCount = AtomicInteger(0)

        // Same genuine-rejection correction as the test above - a plain IOException (or a
        // BackendApiException thrown directly instead of via a real HttpException - see
        // genuineRejection()'s doc comment) is intentionally non-clearing in the real
        // implementation, so neither can be used to test that concurrent callers correctly observe
        // a cleared session.
        fakeApi.onRefresh = {
            refreshCount.incrementAndGet()
            refreshEntered.countDown()
            assertTrue(releaseRefresh.await(10, TimeUnit.SECONDS))
            throw genuineRejection(401)
        }

        val response = response401(bearer = OLD_ACCESS)
        val results = arrayOfNulls<Request>(callers)
        val workers = (0 until callers).map { i ->
            thread(name = "auth-fail-$i") {
                startTogether.await(10, TimeUnit.SECONDS)
                results[i] = authenticator.authenticate(null, response)
            }
        }

        assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))
        releaseRefresh.countDown()
        workers.forEach { it.join(10_000); assertFalse(it.isAlive) }

        assertEquals("only the first caller may attempt the refresh", 1, refreshCount.get())
        assertEquals("the session is cleared exactly once", 1, session.clearPersonIdCount.get())
        assertNull(tokenRepo.accessToken())
        assertNull(tokenRepo.refreshToken())
        results.forEachIndexed { i, r -> assertNull("caller $i must get a null retry", r) }
    }
}
