package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.RefreshRequestDto
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
 * `di/BackendAuthContainer.kt`) for the refresh call itself, so refreshing
 * never recurses back into this authenticator.
 */
class TokenAuthenticator(
    private val tokenRepository: TokenRepository,
    private val plainAuthApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > MAX_RETRIES) {
            return null // already retried once — give up rather than loop
        }

        val refreshToken = tokenRepository.refreshToken() ?: return null

        val newTokens = runBlocking {
            runCatching { plainAuthApi.refresh(RefreshRequestDto(refreshToken)) }
        }.getOrElse {
            tokenRepository.clearTokens()
            return null
        }

        tokenRepository.saveTokens(newTokens.accessToken, newTokens.refreshToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
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
