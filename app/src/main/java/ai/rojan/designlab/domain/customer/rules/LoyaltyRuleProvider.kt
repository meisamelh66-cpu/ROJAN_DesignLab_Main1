package ai.rojan.designlab.domain.customer.rules

import kotlin.math.roundToInt

/**
 * How many loyalty points an appointment earns — extension point, per
 * "never hardcode business decisions that may later conflict with the
 * Architecture Book." [PlaceholderLoyaltyRuleProvider] exists so the
 * demo shows *something*, but its formula is explicitly NOT an approved
 * business rule — see its own doc comment.
 */
interface LoyaltyRuleProvider {
    fun calculatePointsEarned(appointmentPrice: Int): Int
}

/**
 * TEMPORARY placeholder, pending BOOK 3 import. 10% of appointment price
 * is an illustrative demo value chosen so this codebase has *a* working
 * number to display — it was never derived from any approved source and
 * must not be read as a real business decision. Swappable for a real
 * implementation via constructor injection into
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine] without
 * touching that class or any UI.
 */
class PlaceholderLoyaltyRuleProvider : LoyaltyRuleProvider {
    // FIX-005: `.toInt()` truncated; round half-up so the placeholder 10%
    // doesn't silently lose a point on odd prices. Rate itself unchanged.
    override fun calculatePointsEarned(appointmentPrice: Int): Int = (appointmentPrice * 0.10).roundToInt()
}
