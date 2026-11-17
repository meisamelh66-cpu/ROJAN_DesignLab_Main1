package ai.rojan.designlab.manager.domain.repository

/**
 * P1 Per-Salon Payment Gateway Configuration (Manager, owner-only). [merchantId] in
 * [ManagerPaymentGatewayRepository.save] is the only place a real plaintext credential appears on
 * this side of the wire - never stored locally (no cache, same reasoning
 * [ManagerWorkingHoursRepository] already documents), never present in [PaymentGatewayConfig]
 * itself. [configured] false means the salon has never saved a config - every other field is then
 * at its default (the "Empty / Not configured" UI state, not an error).
 */
data class PaymentGatewayConfig(
    val configured: Boolean,
    val provider: String?,
    val isActive: Boolean,
    val maskedMerchantId: String?,
    val updatedAt: String?,
)

data class PaymentGatewayTestResult(
    val success: Boolean,
    val reason: String?,
)

interface ManagerPaymentGatewayRepository {

    suspend fun getConfig(salonId: String): Result<PaymentGatewayConfig>

    suspend fun save(salonId: String, provider: String, merchantId: String): Result<PaymentGatewayConfig>

    suspend fun activate(salonId: String): Result<PaymentGatewayConfig>

    suspend fun deactivate(salonId: String): Result<PaymentGatewayConfig>

    suspend fun testConnection(salonId: String): Result<PaymentGatewayTestResult>
}
