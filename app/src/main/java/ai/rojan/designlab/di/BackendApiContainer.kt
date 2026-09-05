package ai.rojan.designlab.di

import ai.rojan.designlab.data.local.createTokenPreferences
import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.AuthInterceptor
import ai.rojan.designlab.data.remote.AvailabilityApi
import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.NetworkConfig
import ai.rojan.designlab.data.remote.SalonApi
import ai.rojan.designlab.data.remote.SalonBookingApi
import ai.rojan.designlab.data.remote.ServiceApi
import ai.rojan.designlab.data.remote.ServiceCategoryApi
import ai.rojan.designlab.data.remote.SpecialistApi
import ai.rojan.designlab.data.remote.TokenAuthenticator
import ai.rojan.designlab.data.repository.AvailabilityRepositoryImpl
import ai.rojan.designlab.data.repository.BackendAuthRepositoryImpl
import ai.rojan.designlab.data.repository.BookingRepositoryImpl
import ai.rojan.designlab.data.repository.SalonRepositoryImpl
import ai.rojan.designlab.data.repository.ServiceCategoryRepositoryImpl
import ai.rojan.designlab.data.repository.ServiceRepositoryImpl
import ai.rojan.designlab.data.repository.SpecialistRepositoryImpl
import ai.rojan.designlab.data.repository.TokenRepositoryImpl
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual composition root for every backend-networking repository. This
 * codebase has no DI framework (Hilt/Koin/etc.) — every other feature
 * wires its dependencies by hand at the point of use (see
 * `presentation/auth/AuthViewModelFactory.kt`). This mirrors that same
 * pattern for the network layer: one authenticated [Retrofit] instance
 * (shared [AuthInterceptor] + [TokenAuthenticator], so every repository
 * gets transparent bearer-token attachment and 401-refresh-retry for free)
 * backs every `*Api` this class builds.
 *
 * **Auth wired (Android <-> Backend Full Integration milestone):**
 * `presentation/auth/AuthViewModelFactory.kt` now constructs
 * [backendAuthRepository]/[tokenRepository] from this container alongside
 * the still-demo `Demo*` identity/session providers — see
 * `presentation/auth/AuthViewModel.kt`'s own doc comment for why both
 * exist together. [tokenRepository] is populated with a real access/
 * refresh token pair on a successful login, so every repository below
 * genuinely authenticates against the backend.
 *
 * Construct once (e.g. held by the call site the same way
 * `AuthViewModelFactory` is currently constructed once in `RojanNavGraph`)
 * rather than per-use — `tokenRepository` opens the encrypted preferences
 * file, which isn't free to redo repeatedly.
 */
class BackendApiContainer(context: Context) {

    val tokenRepository: TokenRepository = TokenRepositoryImpl(context.createTokenPreferences())

    private val retrofit: Retrofit = buildAuthenticatedRetrofit(tokenRepository)

    val backendAuthRepository: BackendAuthRepository =
        BackendAuthRepositoryImpl(authApi = retrofit.create(AuthApi::class.java), tokenRepository = tokenRepository)

    val salonRepository: SalonRepository =
        SalonRepositoryImpl(retrofit.create(SalonApi::class.java))

    val serviceCategoryRepository: ServiceCategoryRepository =
        ServiceCategoryRepositoryImpl(retrofit.create(ServiceCategoryApi::class.java))

    val serviceRepository: ServiceRepository =
        ServiceRepositoryImpl(retrofit.create(ServiceApi::class.java))

    val specialistRepository: SpecialistRepository =
        SpecialistRepositoryImpl(retrofit.create(SpecialistApi::class.java))

    val availabilityRepository: AvailabilityRepository =
        AvailabilityRepositoryImpl(retrofit.create(AvailabilityApi::class.java))

    val bookingRepository: BookingRepository =
        BookingRepositoryImpl(retrofit.create(BookingApi::class.java), retrofit.create(SalonBookingApi::class.java))

    private companion object {

        fun buildAuthenticatedRetrofit(tokenRepository: TokenRepository): Retrofit {
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
        }
    }
}
