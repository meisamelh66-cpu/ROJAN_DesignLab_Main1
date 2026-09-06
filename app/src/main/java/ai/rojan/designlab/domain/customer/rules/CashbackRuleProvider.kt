package ai.rojan.designlab.domain.customer.rules

import kotlin.math.roundToInt

/** Same reasoning as [LoyaltyRuleProvider] — extension point, not a guessed rule presented as final. */
interface CashbackRuleProvider {
    fun calculateCashback(appointmentPrice: Int): Int
}

/** TEMPORARY placeholder, pending BOOK 3 import — see [PlaceholderLoyaltyRuleProvider]'s doc comment for the same caveat. */
class PlaceholderCashbackRuleProvider : CashbackRuleProvider {
    // FIX-005: `.toInt()` truncated the fractional Toman of the 5% share.
    // Round half-up instead. The placeholder 5% rate itself is unchanged.
    override fun calculateCashback(appointmentPrice: Int): Int = (appointmentPrice * 0.05).roundToInt()
}
