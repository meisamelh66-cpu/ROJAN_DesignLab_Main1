package ai.rojan.designlab.domain.customer

import ai.rojan.designlab.data.demo.DemoCoupon
import ai.rojan.designlab.domain.customer.rules.PlaceholderCashbackRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderLoyaltyRuleProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FIX-005 — financial calculation safety. Confirms the discount / cashback
 * / loyalty math rounds half-up instead of truncating, and that whole
 * inputs are unaffected (no unintended value change). The placeholder
 * rates (5% / 10% / coupon percent) are unchanged — only the rounding.
 */
class MoneyCalculationSafetyTest {

    private fun coupon(percent: Int) =
        DemoCoupon(id = "c-$percent", title = "", description = "", discountPercent = percent, expiryLabel = "", code = "")

    @Test
    fun `coupon discount rounds half-up, not integer-division truncation`() {
        val events = CustomerEcosystemEngine().redeemCoupon(
            state = CustomerEcosystemState(),
            coupon = coupon(15),
            referencePrice = 1_000_005, // * 15 / 100 = 150_000.75
        )
        val redeemed = events.single() as EcosystemEvent.CouponRedeemed
        // Pre-FIX-005 integer division gave 150_000 (dropped .75).
        assertEquals(150_001, redeemed.discountAmount)
    }

    @Test
    fun `coupon discount on an exact multiple is unchanged`() {
        val events = CustomerEcosystemEngine().redeemCoupon(
            state = CustomerEcosystemState(),
            coupon = coupon(20),
            referencePrice = 1_250_000, // * 20 / 100 = 250_000 exactly
        )
        val redeemed = events.single() as EcosystemEvent.CouponRedeemed
        assertEquals(250_000, redeemed.discountAmount)
    }

    @Test
    fun `cashback rounds instead of truncating`() {
        val cashback = PlaceholderCashbackRuleProvider()
        // 5% of 690_014 = 34_500.7  -> pre-FIX-005 `.toInt()` gave 34_500
        assertEquals(34_501, cashback.calculateCashback(690_014))
        // exact case unchanged
        assertEquals(34_500, cashback.calculateCashback(690_000))
    }

    @Test
    fun `loyalty points round instead of truncating`() {
        val loyalty = PlaceholderLoyaltyRuleProvider()
        // 10% of 690_007 = 69_000.7 -> pre-FIX-005 `.toInt()` gave 69_000
        assertEquals(69_001, loyalty.calculatePointsEarned(690_007))
        assertEquals(69_000, loyalty.calculatePointsEarned(690_000))
    }
}
