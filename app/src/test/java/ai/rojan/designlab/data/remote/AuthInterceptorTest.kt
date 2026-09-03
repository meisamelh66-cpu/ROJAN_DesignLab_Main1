package ai.rojan.designlab.data.remote

import ai.rojan.designlab.domain.repository.TokenRepository
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Auth Reliability test coverage: [AuthInterceptor] in isolation, via a
 * hand-rolled [Interceptor.Chain] fake (the standard way to unit-test a
 * single OkHttp application interceptor without a real network stack or
 * MockWebServer — this project has neither as a test dependency, and this
 * task is scoped to plain JVM tests only). [FakeChain.proceed] records the
 * exact [Request] it was called with, so these tests can assert on the
 * request the interceptor actually forwarded rather than just the final
 * response.
 */
class AuthInterceptorTest {

    private val originalRequest = Request.Builder().url("https://example.com/foo").build()

    @Test
    fun `token present attaches it as a Bearer Authorization header`() {
        val tokenRepository = FakeTokenRepository(accessToken = "real-access-token")
        val interceptor = AuthInterceptor(tokenRepository)
        val chain = FakeChain(originalRequest)

        interceptor.intercept(chain)

        val forwarded = chain.proceededRequest ?: error("proceed() was never called")
        assertEquals("Bearer real-access-token", forwarded.header("Authorization"))
    }

    @Test
    fun `token absent proceeds with the request unmodified, no Authorization header`() {
        val tokenRepository = FakeTokenRepository(accessToken = null)
        val interceptor = AuthInterceptor(tokenRepository)
        val chain = FakeChain(originalRequest)

        interceptor.intercept(chain)

        val forwarded = chain.proceededRequest ?: error("proceed() was never called")
        assertNull(forwarded.header("Authorization"))
        assertEquals(originalRequest, forwarded)
    }

    private class FakeTokenRepository(private val accessToken: String?) : TokenRepository {
        override fun saveTokens(accessToken: String, refreshToken: String) = error("not used")
        override fun clearTokens() = error("not used")
        override fun accessToken(): String? = accessToken
        override fun refreshToken(): String? = error("not used")
    }

    /** Minimal [Interceptor.Chain] fake — only [request]/[proceed] have real behavior; every other member is unused by [AuthInterceptor] and left unimplemented on purpose. */
    private class FakeChain(private val request: Request) : Interceptor.Chain {
        var proceededRequest: Request? = null
            private set

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = error("not used")
        override fun connectTimeoutMillis(): Int = error("not used")
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = error("not used")
        override fun readTimeoutMillis(): Int = error("not used")
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = error("not used")
        override fun writeTimeoutMillis(): Int = error("not used")
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = error("not used")
    }
}
