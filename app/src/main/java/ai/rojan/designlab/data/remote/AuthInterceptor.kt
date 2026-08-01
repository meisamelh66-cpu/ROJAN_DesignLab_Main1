package ai.rojan.designlab.data.remote

import ai.rojan.designlab.domain.repository.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the stored access token as a Bearer header to every request, when one is available. */
class AuthInterceptor(
    private val tokenRepository: TokenRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenRepository.accessToken()
            ?: return chain.proceed(request)

        val authorizedRequest = request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(authorizedRequest)
    }
}
