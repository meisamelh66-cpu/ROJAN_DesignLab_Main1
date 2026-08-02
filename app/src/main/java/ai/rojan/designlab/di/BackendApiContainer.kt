package ai.rojan.designlab.di

import ai.rojan.designlab.data.local.createTokenPreferences
import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.AuthInterceptor
import ai.rojan.designlab.data.remote.AvailabilityApi
import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.NetworkConfig
import ai.rojan.designlab.data.remote.SalonApi
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
 * **Auth migration frozen (Android <-> Backend Full Integration
 * milestone):** the Android app's existing Phone+OTP UX is not being
 * replaced this milestone — see `presentation/auth/AuthViewModelFactory.kt`,
 * which still constructs the mock `Demo*` identity/session providers, not
 * [backendAuthRepository] below. Every repository in this container is
 * therefore fully built and wired, but nothing populates [tokenRepository]
 * with a real token yet — calls will genuinely receive `401 Unauthorized`
 * until a future backend milestone adds native Phone-OTP endpoints and a
 * later Android milestone points the auth screens at them. This is the
 * deliberate, ready-but-dormant integration point that decision implies:
 * no workaround, no bridge identity, no shared test token.
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
        BookingRepositoryImpl(retrofit.create(BookingApi::class.java))

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
