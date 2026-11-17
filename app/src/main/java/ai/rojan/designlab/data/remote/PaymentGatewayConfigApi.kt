package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.PaymentGatewayConfigResponseDto
import ai.rojan.designlab.data.remote.dto.PaymentGatewayTestResponseDto
import ai.rojan.designlab.data.remote.dto.SavePaymentGatewayConfigRequestDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit contract for the ROJAN backend's P1 Per-Salon Payment Gateway Configuration API
 * (`ai.rojan.backend.api.payment.PaymentGatewayConfigController`). Every call is owner-only,
 * enforced server-side (`Permission.MANAGE_PAYMENT_GATEWAY`) - same convention as every other
 * salon-scoped write endpoint ([ManagerSalonApi]).
 */
interface PaymentGatewayConfigApi {

    @GET("api/v1/salons/{salonId}/payment-gateway")
    suspend fun getConfig(@Path("salonId") salonId: String): PaymentGatewayConfigResponseDto

    @PUT("api/v1/salons/{salonId}/payment-gateway")
    suspend fun saveConfig(
        @Path("salonId") salonId: String,
        @Body request: SavePaymentGatewayConfigRequestDto,
    ): PaymentGatewayConfigResponseDto

    @POST("api/v1/salons/{salonId}/payment-gateway/activate")
    suspend fun activate(@Path("salonId") salonId: String): PaymentGatewayConfigResponseDto

    @POST("api/v1/salons/{salonId}/payment-gateway/deactivate")
    suspend fun deactivate(@Path("salonId") salonId: String): PaymentGatewayConfigResponseDto

    @POST("api/v1/salons/{salonId}/payment-gateway/test")
    suspend fun testConnection(@Path("salonId") salonId: String): PaymentGatewayTestResponseDto
}
