package ai.rojan.designlab.di

import ai.rojan.designlab.data.local.activeSalonDataStore
import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.local.createTokenPreferences
import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.AuthInterceptor
import ai.rojan.designlab.data.remote.AvailabilityApi
import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.ManagerDashboardApi
import ai.rojan.designlab.data.remote.ManagerSalonApi
import ai.rojan.designlab.data.remote.ManagerServiceApi
import ai.rojan.designlab.data.remote.ManagerSpecialistApi
import ai.rojan.designlab.data.remote.NetworkConfig
import ai.rojan.designlab.data.remote.PublicSalonApi
import ai.rojan.designlab.data.remote.SalonApi
import ai.rojan.designlab.data.remote.SalonMembershipApi
import ai.rojan.designlab.data.remote.SalonRelationshipApi
import ai.rojan.designlab.data.remote.ServiceApi
import ai.rojan.designlab.data.remote.ServiceCategoryApi
import ai.rojan.designlab.data.remote.SpecialistApi
import ai.rojan.designlab.data.remote.TokenAuthenticator
import ai.rojan.designlab.data.remote.WorkingHoursApi
import ai.rojan.designlab.data.repository.ActiveSalonContextRepositoryImpl
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.data.repository.AvailabilityRepositoryImpl
import ai.rojan.designlab.data.repository.BackendAuthRepositoryImpl
import ai.rojan.designlab.data.repository.BookingHistoryRepositoryImpl
import ai.rojan.designlab.data.repository.BookingRepositoryImpl
import ai.rojan.designlab.data.repository.CurrentUserIdentityContextRepositoryImpl
import ai.rojan.designlab.data.repository.CustomerRelationshipRepositoryImpl
import ai.rojan.designlab.data.repository.PublicSalonRepositoryImpl
import ai.rojan.designlab.data.repository.SalonRepositoryImpl
import ai.rojan.designlab.data.repository.ServiceCategoryRepositoryImpl
import ai.rojan.designlab.data.repository.ServiceRepositoryImpl
import ai.rojan.designlab.data.repository.SpecialistRepositoryImpl
import ai.rojan.designlab.data.repository.TokenRepositoryImpl
import ai.rojan.designlab.data.repository.WorkingHoursRepositoryImpl
import ai.rojan.designlab.domain.beauty.BeautyProfileRepository
import ai.rojan.designlab.domain.beauty.InMemoryBeautyProfileRepository
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.BookingHistoryRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository
import ai.rojan.designlab.domain.repository.PublicSalonRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.domain.repository.WorkingHoursRepository
import ai.rojan.designlab.manager.data.BackendSalonMembershipRepository
import ai.rojan.designlab.manager.domain.ai.InactiveCustomerInsightProvider
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsightProvider
import ai.rojan.designlab.manager.domain.repository.SalonMembershipRepository
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual composition root for backend networking dependencies.
 *
 * Shared authenticated Retrofit instance:
 * - AuthInterceptor adds bearer token
 * - TokenAuthenticator handles refresh flow
 *
 * All repositories and APIs use the same authenticated client.
 */
class BackendApiContainer(context: Context) {

    val tokenRepository: TokenRepository =
        TokenRepositoryImpl(context.createTokenPreferences())

    // Same DataStore singleton (per-Context, via the `by preferencesDataStore(...)`
    // delegate) that AuthViewModelFactory/SessionViewModelFactory each already
    // construct their own AuthSessionRepositoryImpl from - needed here too so
    // TokenAuthenticator can clear the persisted personId on a genuinely failed
    // refresh, not just the token pair (Authentication Session Persistence fix).
    private val authSessionRepository: AuthSessionRepository =
        AuthSessionRepositoryImpl(context.authSessionDataStore)

    private val retrofit: Retrofit = buildAuthenticatedRetrofit(tokenRepository, authSessionRepository)

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    val backendAuthRepository: BackendAuthRepository =
        BackendAuthRepositoryImpl(authApi = authApi, tokenRepository = tokenRepository)

    val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository =
        CurrentUserIdentityContextRepositoryImpl(authApi = authApi, backendAuthRepository = backendAuthRepository)

    // Active Salon Context & Selection Flow: own DataStore file, separate
    // from authSessionDataStore - same DataStore-per-Context singleton
    // reasoning as authSessionRepository above.
    val activeSalonContextRepository: ActiveSalonContextRepository =
        ActiveSalonContextRepositoryImpl(context.activeSalonDataStore)

    val salonRepository: SalonRepository =
        SalonRepositoryImpl(
            retrofit.create(SalonApi::class.java)
        )

    val serviceCategoryRepository: ServiceCategoryRepository =
        ServiceCategoryRepositoryImpl(
            retrofit.create(ServiceCategoryApi::class.java)
        )

    val serviceRepository: ServiceRepository =
        ServiceRepositoryImpl(
            retrofit.create(ServiceApi::class.java)
        )

    val specialistRepository: SpecialistRepository =
        SpecialistRepositoryImpl(
            retrofit.create(SpecialistApi::class.java)
        )

    val availabilityRepository: AvailabilityRepository =
        AvailabilityRepositoryImpl(
            retrofit.create(AvailabilityApi::class.java)
        )

    val bookingRepository: BookingRepository =
        BookingRepositoryImpl(
            retrofit.create(BookingApi::class.java)
        )


    // -----------------------------
    // Manager APIs
    // -----------------------------

    val managerSalonApi: ManagerSalonApi =
        retrofit.create(ManagerSalonApi::class.java)

    val managerServiceApi: ManagerServiceApi =
        retrofit.create(ManagerServiceApi::class.java)

    val managerBookingApi: ManagerBookingApi =
        retrofit.create(ManagerBookingApi::class.java)

    val managerCustomerApi: ManagerCustomerApi =
        retrofit.create(ManagerCustomerApi::class.java)

    val managerDashboardApi: ManagerDashboardApi =
        retrofit.create(ManagerDashboardApi::class.java)

    val managerSpecialistApi: ManagerSpecialistApi =
        retrofit.create(ManagerSpecialistApi::class.java)


    // Raw APIs exposed for manager repositories
    val serviceApi: ServiceApi =
        retrofit.create(ServiceApi::class.java)

    val serviceCategoryApi: ServiceCategoryApi =
        retrofit.create(ServiceCategoryApi::class.java)

    val specialistApi: SpecialistApi =
        retrofit.create(SpecialistApi::class.java)

    val bookingHistoryRepository: BookingHistoryRepository =
        BookingHistoryRepositoryImpl(bookingRepository, salonRepository, specialistRepository)

    val workingHoursRepository: WorkingHoursRepository =
        WorkingHoursRepositoryImpl(retrofit.create(WorkingHoursApi::class.java))

    val customerRelationshipRepository: CustomerRelationshipRepository =
        CustomerRelationshipRepositoryImpl(retrofit.create(SalonRelationshipApi::class.java))

    // Not backend-networked (unlike every other member of this container) -
    // no customer beauty-profile endpoint exists yet. Held here anyway as
    // the single composition root/singleton this app already has, so
    // navigating away from and back to Beauty DNA keeps the same in-memory
    // data for the process lifetime, same reasoning as every real
    // repository above just without a Retrofit client backing it. See
    // domain/beauty/BeautyProfileRepository.kt's own doc comment.
    val beautyProfileRepository: BeautyProfileRepository = InMemoryBeautyProfileRepository()

    // Deliberately NOT built on [retrofit] - the public salon QR journey is
    // unauthenticated by design (see PublicSalonApi's own doc comment), so
    // it gets its own plain client with no AuthInterceptor/TokenAuthenticator,
    // same reasoning as plainAuthApi below.
    val publicSalonRepository: PublicSalonRepository =
        PublicSalonRepositoryImpl(buildPlainRetrofit().create(PublicSalonApi::class.java))

    // Manager-only, owner-authenticated, but exposed fully wrapped here
    // (unlike the raw manager*Api section above) - unlike
    // BackendServiceRepository/BackendCustomerRepository/etc.,
    // SalonMembershipApi takes salonId per call rather than needing it
    // baked into the repository at construction time, so there's no
    // salon-scoped composition step this container would have to
    // duplicate. Data layer only this phase (Phase 2, M4) - no screen
    // consumes this yet.
    val salonMembershipRepository: SalonMembershipRepository =
        BackendSalonMembershipRepository(retrofit.create(SalonMembershipApi::class.java))

    // Manager CRM AI Foundation, Phase 7 Step 3 - the first real,
    // deterministic implementation (surfaces the already-real
    // CustomerTag.INACTIVE classification, no invented logic - see that
    // class's own doc comment). Pure/synchronous, no network client of
    // its own, same as the NoOp it replaces.
    val managerCrmInsightProvider: ManagerCrmInsightProvider = InactiveCustomerInsightProvider()

    /** No AuthInterceptor/authenticator - a plain, no-token client, for endpoints that need none. */
    private fun buildPlainRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(loggingInterceptor).build())
            .addConverterFactory(jsonConverterFactory)
            .build()

    private companion object {

        val jsonConverterFactory = Json { ignoreUnknownKeys = true }
            .asConverterFactory("application/json".toMediaType())

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        fun buildAuthenticatedRetrofit(tokenRepository: TokenRepository, authSessionRepository: AuthSessionRepository): Retrofit {
            // Used only for the refresh call itself inside TokenAuthenticator,
            // so refreshing never recurses back into itself.
            val plainAuthApi: AuthApi = Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(OkHttpClient.Builder().addInterceptor(loggingInterceptor).build())
                .addConverterFactory(jsonConverterFactory)
                .build()
                .create(AuthApi::class.java)

            val authenticatedClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(AuthInterceptor(tokenRepository))
                .authenticator(TokenAuthenticator(tokenRepository, plainAuthApi, authSessionRepository))
                .build()

            return Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(authenticatedClient)
                .addConverterFactory(jsonConverterFactory)
                .build()
        }
    }
}