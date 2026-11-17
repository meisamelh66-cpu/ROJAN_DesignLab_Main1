package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * P1 Per-Salon Payment Gateway Configuration (Manager). Mirrors the backend's
 * `ai.rojan.backend.api.payment.SavePaymentGatewayConfigRequest` exactly - [provider] is a plain
 * string (`"ZARINPAL"`, the only real value today) rather than a Kotlin enum shared with the
 * Customer module's own, unrelated payment-method types, since this is a Manager-only concern with
 * no Customer-side equivalent. [merchantId] is the real plaintext credential, sent exactly once in
 * this one request - never stored locally, never present in any response DTO below.
 */
@Serializable
data class SavePaymentGatewayConfigRequestDto(
    val provider: String,
    val merchantId: String,
)

/**
 * Mirrors `ai.rojan.backend.api.payment.PaymentGatewayConfigResponse` exactly. [configured] false
 * means the salon has never saved a config - every other field is then null (the "Empty / Not
 * configured" state, not an error). [maskedMerchantId] is a one-way display string (e.g.
 * `"••••3456"`) - the real merchant id and its ciphertext never appear in this DTO, by the backend's
 * own design.
 */
@Serializable
data class PaymentGatewayConfigResponseDto(
    val configured: Boolean,
    val provider: String?,
    val isActive: Boolean,
    val maskedMerchantId: String?,
    val updatedAt: String?,
)

/** Mirrors `ai.rojan.backend.api.payment.PaymentGatewayTestResponse` exactly. */
@Serializable
data class PaymentGatewayTestResponseDto(
    val success: Boolean,
    val reason: String?,
)
