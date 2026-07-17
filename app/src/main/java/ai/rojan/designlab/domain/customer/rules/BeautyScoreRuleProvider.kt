package ai.rojan.designlab.domain.customer.rules

import ai.rojan.designlab.domain.customer.CustomerEcosystemState

/** How a customer's Beauty Score is calculated — a real business decision, not derivable from raw data alone. Extension point. */
interface BeautyScoreRuleProvider {
    fun calculate(state: CustomerEcosystemState): Int
}

/**
 * TEMPORARY placeholder, pending BOOK 3 import. Formula (completed
 * appointments × 10, plus loyalty points ÷ 50, capped at 100) is an
 * illustrative demo value with no approved basis — same caveat as
 * [PlaceholderLoyaltyRuleProvider].
 */
class PlaceholderBeautyScoreRuleProvider : BeautyScoreRuleProvider {
    override fun calculate(state: CustomerEcosystemState): Int {
        val completedCount = state.pastAppointments.count {
            it.status == ai.rojan.designlab.data.demo.AppointmentStatus.COMPLETED
        }
        return (completedCount * 10 + state.loyaltyPoints / 50).coerceAtMost(100)
    }
}
