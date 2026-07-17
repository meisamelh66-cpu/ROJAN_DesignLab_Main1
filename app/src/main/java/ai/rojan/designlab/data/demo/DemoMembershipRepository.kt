package ai.rojan.designlab.data.demo

object DemoMembershipRepository {
    val tier: DemoMembershipTier = DemoMembershipTier(
        currentTierName = "نقره‌ای",
        benefits = listOf(
            "۵٪ تخفیف دائمی روی تمام خدمات",
            "اولویت رزرو در ساعات پرتقاضا",
            "پشتیبانی اختصاصی",
        ),
        pointsToNextTier = 350,
        nextTierName = "طلایی",
    )
}
