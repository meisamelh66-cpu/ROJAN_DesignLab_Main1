package ai.rojan.designlab.domain.customer.rules

/** Same reasoning as [LoyaltyRuleProvider] — extension point, not a guessed rule presented as final. */
interface CashbackRuleProvider {
    fun calculateCashback(appointmentPrice: Int): Int
}

/** TEMPORARY placeholder, pending BOOK 3 import — see [PlaceholderLoyaltyRuleProvider]'s doc comment for the same caveat. */
class PlaceholderCashbackRuleProvider : CashbackRuleProvider {
    override fun calculateCashback(appointmentPrice: Int): Int = (appointmentPrice * 0.05).toInt()
}
