package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.PaymentGatewayConfigApi
import ai.rojan.designlab.data.remote.dto.PaymentGatewayConfigResponseDto
import ai.rojan.designlab.data.remote.dto.PaymentGatewayTestResponseDto
import ai.rojan.designlab.data.remote.dto.SavePaymentGatewayConfigRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.repository.ManagerPaymentGatewayRepository
import ai.rojan.designlab.manager.domain.repository.PaymentGatewayConfig
import ai.rojan.designlab.manager.domain.repository.PaymentGatewayTestResult

/**
 * Real backend-backed [ManagerPaymentGatewayRepository]. No cache, same reasoning
 * [BackendManagerWorkingHoursRepository] already documents - this repository backs exactly one
 * screen ([ai.rojan.designlab.manager.screens.settings.ManagerPaymentGatewayScreen]) with no list
 * to keep in sync elsewhere.
 */
class BackendManagerPaymentGatewayRepository(
    private val api: PaymentGatewayConfigApi,
) : ManagerPaymentGatewayRepository {

    override suspend fun getConfig(salonId: String): Result<PaymentGatewayConfig> =
        safeApiCall { api.getConfig(salonId) }.map { it.toDomain() }

    override suspend fun save(salonId: String, provider: String, merchantId: String): Result<PaymentGatewayConfig> =
        safeApiCall { api.saveConfig(salonId, SavePaymentGatewayConfigRequestDto(provider, merchantId)) }.map { it.toDomain() }

    override suspend fun activate(salonId: String): Result<PaymentGatewayConfig> =
        safeApiCall { api.activate(salonId) }.map { it.toDomain() }

    override suspend fun deactivate(salonId: String): Result<PaymentGatewayConfig> =
        safeApiCall { api.deactivate(salonId) }.map { it.toDomain() }

    override suspend fun testConnection(salonId: String): Result<PaymentGatewayTestResult> =
        safeApiCall { api.testConnection(salonId) }.map { it.toDomain() }

    private fun PaymentGatewayConfigResponseDto.toDomain() = PaymentGatewayConfig(
        configured = configured,
        provider = provider,
        isActive = isActive,
        maskedMerchantId = maskedMerchantId,
        updatedAt = updatedAt,
    )

    private fun PaymentGatewayTestResponseDto.toDomain() = PaymentGatewayTestResult(success = success, reason = reason)
}
