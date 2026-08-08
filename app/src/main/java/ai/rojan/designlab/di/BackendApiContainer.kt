package ai.rojan.designlab.di

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

    private val retrofit: Retrofit =
        buildAuthenticatedRetrofit(tokenRepository)

    val backendAuthRepository: BackendAuthRepository =
        BackendAuthRepositoryImpl(
            authApi = retrofit.create(AuthApi::class.java),
            tokenRepository = tokenRepository
        )

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


    private companion object {

        fun buildAuthenticatedRetrofit(
            tokenRepository: TokenRepository
        ): Retrofit {

            val jsonConverterFactory =
                Json {
                    ignoreUnknownKeys = true
                }.asConverterFactory(
                    "application/json".toMediaType()
                )


            val loggingInterceptor =
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }


            // Used only for token refresh
            val plainAuthApi: AuthApi =
                Retrofit.Builder()
                    .baseUrl(NetworkConfig.BASE_URL)
                    .client(
                        OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .build()
                    )
                    .addConverterFactory(jsonConverterFactory)
                    .build()
                    .create(AuthApi::class.java)


            val authenticatedClient =
                OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(
                        AuthInterceptor(tokenRepository)
                    )
                    .authenticator(
                        TokenAuthenticator(
                            tokenRepository,
                            plainAuthApi
                        )
                    )
                    .build()


            return Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(authenticatedClient)
                .addConverterFactory(jsonConverterFactory)
                .build()
        }
    }
}