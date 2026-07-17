package ai.rojan.designlab.domain.customer.rules

/** Same reasoning as [LoyaltyRuleProvider]. */
interface MembershipProgressRuleProvider {
    fun calculateProgressIncrement(loyaltyPointsEarned: Int): Int
}

/**
 * TEMPORARY placeholder, pending BOOK 3 import. 1:1 with loyalty points
 * earned is the simplest possible relationship, not a considered
 * business decision — flagged the same way as its siblings.
 */
class PlaceholderMembershipProgressRuleProvider : MembershipProgressRuleProvider {
    override fun calculateProgressIncrement(loyaltyPointsEarned: Int): Int = loyaltyPointsEarned
}
