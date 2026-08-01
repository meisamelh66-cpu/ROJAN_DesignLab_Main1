package ai.rojan.designlab.di

import ai.rojan.designlab.data.local.createTokenPreferences
import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.AuthInterceptor
import ai.rojan.designlab.data.remote.NetworkConfig
import ai.rojan.designlab.data.remote.TokenAuthenticator
import ai.rojan.designlab.data.repository.BackendAuthRepositoryImpl
import ai.rojan.designlab.data.repository.TokenRepositoryImpl
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual composition root for the backend-auth networking stack. This
 * codebase has no DI framework (Hilt/Koin/etc.) — every other feature
 * wires its dependencies by hand at the point of use (see
 * `presentation/auth/AuthViewModelFactory.kt`). This mirrors that same
 * pattern for the network layer, since several pieces (interceptor,
 * authenticator, repository) need to share one [TokenRepository] instance.
 *
 * Construct once (e.g. held by the call site the same way
 * `AuthViewModelFactory` is currently constructed once in `RojanNavGraph`)
 * rather than per-use — `tokenRepository` opens the encrypted preferences
 * file, which isn't free to redo repeatedly.
 */
class BackendAuthContainer(context: Context) {

    val tokenRepository: TokenRepository = TokenRepositoryImpl(context.createTokenPreferences())

    val backendAuthRepository: BackendAuthRepository = BackendAuthRepositoryImpl(
        authApi = buildAuthApi(tokenRepository),
        tokenRepository = tokenRepository,
    )

    private companion object {

        fun buildAuthApi(tokenRepository: TokenRepository): AuthApi {
            val jsonConverterFactory = Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType())

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            // No AuthInterceptor/authenticator — used only for the refresh call itself
            // inside TokenAuthenticator, so refreshing never recurses back into itself.
            val plainAuthApi: AuthApi = Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(OkHttpClient.Builder().addInterceptor(loggingInterceptor).build())
                .addConverterFactory(jsonConverterFactory)
                .build()
                .create(AuthApi::class.java)

            val authenticatedClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(AuthInterceptor(tokenRepository))
                .authenticator(TokenAuthenticator(tokenRepository, plainAuthApi))
                .build()

            return Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(authenticatedClient)
                .addConverterFactory(jsonConverterFactory)
                .build()
                .create(AuthApi::class.java)
        }
    }
}
