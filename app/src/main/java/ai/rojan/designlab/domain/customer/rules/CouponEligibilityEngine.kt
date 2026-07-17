package ai.rojan.designlab.domain.customer.rules

import ai.rojan.designlab.data.demo.DemoCoupon
import ai.rojan.designlab.domain.customer.CustomerEcosystemState
import ai.rojan.designlab.domain.customer.CouponRejectionReason

sealed interface CouponEligibilityResult {
    data object Eligible : CouponEligibilityResult
    data class Ineligible(val reason: CouponRejectionReason) : CouponEligibilityResult
}

/**
 * Coupon Eligibility Engine — deliberately separate from redemption, per
 * explicit instruction to implement eligibility checking as its own
 * step before redemption, not folded into it.
 *
 * Real today: the "already used" check. Everything else a real coupon
 * system would check (membership-tier requirements, minimum spend,
 * category restrictions, expiry-date comparison against a real
 * calendar) is BOOK 3 territory not yet known — rather than guess at
 * any of it, this class only implements the one rule that's genuinely
 * derivable from data already in [CustomerEcosystemState] (which
 * coupons have been used), and is structured so additional checks are
 * simply additional branches here later, not a redesign.
 */
class CouponEligibilityEngine {
    fun check(state: CustomerEcosystemState, coupon: DemoCoupon): CouponEligibilityResult {
        if (coupon.id in state.usedCouponIds) {
            return CouponEligibilityResult.Ineligible(CouponRejectionReason.ALREADY_USED)
        }
        // Extension point: membership-tier eligibility, minimum spend,
        // category restrictions, real expiry-date comparison — all BOOK 3
        // territory, intentionally not guessed at here.
        return CouponEligibilityResult.Eligible
    }
}
