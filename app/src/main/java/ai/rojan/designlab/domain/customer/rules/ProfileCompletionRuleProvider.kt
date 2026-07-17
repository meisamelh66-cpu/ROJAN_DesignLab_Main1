package ai.rojan.designlab.domain.customer.rules

/** Which profile fields count toward "completion," and their weight — a real product decision, not guessed at freely. Extension point. */
interface ProfileCompletionRuleProvider {
    fun calculate(hasPhoto: Boolean, hasBirthday: Boolean, hasCompletedAppointment: Boolean, hasFavorite: Boolean, hasReview: Boolean): Int
}

/**
 * TEMPORARY placeholder, pending BOOK 3 import. Five equally-weighted
 * checks (20% each) is the simplest possible scheme, not a considered
 * one — flagged the same way as its siblings.
 */
class PlaceholderProfileCompletionRuleProvider : ProfileCompletionRuleProvider {
    override fun calculate(
        hasPhoto: Boolean,
        hasBirthday: Boolean,
        hasCompletedAppointment: Boolean,
        hasFavorite: Boolean,
        hasReview: Boolean,
    ): Int {
        val checks = listOf(hasPhoto, hasBirthday, hasCompletedAppointment, hasFavorite, hasReview)
        return (checks.count { it } * 100) / checks.size
    }
}
