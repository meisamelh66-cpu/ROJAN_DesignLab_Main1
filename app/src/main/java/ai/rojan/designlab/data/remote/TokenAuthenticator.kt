package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.RefreshRequestDto
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val MAX_RETRIES = 1

/**
 * On a 401, exchanges the stored refresh token for a new access/refresh
 * pair and retries the original request once. Requires the backend to
 * actually return 401 (not 403) for an invalid/missing bearer token —
 * OkHttp's [Authenticator] SPI only ever triggers on a real 401.
 *
 * Uses [plainAuthApi] (an [AuthApi] built on a client with no
 * [AuthInterceptor]/authenticator of its own — see
 * `di/BackendApiContainer.kt`) for the refresh call itself, so refreshing
 * never recurses back into this authenticator.
 *
 * Authentication Session Persistence fix: a genuinely failed refresh (the
 * refresh token itself expired/was revoked, not just a transient network
 * error) now also clears [authSessionRepository]'s persisted personId, not
 * just the token pair. Previously only [tokenRepository] was cleared here,
 * so the next cold start still read a stale personId from DataStore and
 * optimistically routed to CUSTOMER_HOME before discovering — again — that
 * the session was dead. `runBlocking` mirrors the refresh call just above,
 * for the same reason: [Authenticator.authenticate] is a synchronous OkHttp
 * SPI callback with no coroutine scope of its own.
 *
 * Concurrent-refresh guard (C1): OkHttp invokes [authenticate] on each
 * connection's own thread, so N in-flight requests that all 401 at once
 * would previously each fire an independent `/auth/refresh`. With
 * refresh-token rotation, only the first would succeed — every later one
 * would refresh with an already-invalidated refresh token, fail, and hit
 * the clear-session path above, logging the user out despite the first
 * refresh having succeeded. [refreshLock] serialises the refresh decision:
 * the first caller in performs the refresh; every later caller re-reads
 * the stored access token and, when it differs from the one that was on
 * its own failed request, simply retries with that already-refreshed
 * token instead of refreshing again. This is safe alongside the
 * synchronous `runBlocking` refresh because [plainAuthApi] has no
 * authenticator of its own — the refresh call can never re-enter this
 * lock, so there is no self-deadlock.
 */
class TokenAuthenticator(
    private val tokenRepository: TokenRepository,
    private val plainAuthApi: AuthApi,
    private val authSessionRepository: AuthSessionRepository,
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > MAX_RETRIES) {
            return null // already retried once — give up rather than loop
        }

        // The access token that was actually on the request that 401'd —
        // used below to tell "someone else already refreshed" apart from
        // "the stored token is still the expired one that caused this 401".
        val failedAccessToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            // Another caller may have refreshed while this one waited for
            // the lock. A non-null stored token is NOT enough to conclude
            // that — it could still be the same expired token. It only
            // means a successful refresh happened if it *differs* from the
            // token that was on this failed request.
            val currentAccessToken = tokenRepository.accessToken()
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != failedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenRepository.refreshToken() ?: return null

            val newTokens = runBlocking {
                runCatching { plainAuthApi.refresh(RefreshRequestDto(refreshToken)) }
            }.getOrElse {
                tokenRepository.clearTokens()
                runBlocking { authSessionRepository.clearPersonId() }
                return null
            }

            tokenRepository.saveTokens(newTokens.accessToken, newTokens.refreshToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
