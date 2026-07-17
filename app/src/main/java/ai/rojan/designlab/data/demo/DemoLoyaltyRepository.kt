package ai.rojan.designlab.data.demo

object DemoLoyaltyRepository {
    const val TOTAL_POINTS = 650

    val entries: List<DemoLoyaltyEntry> = listOf(
        DemoLoyaltyEntry("loy_01", "رزرو در سالن رویا", 69, isEarned = true, dateLabel = "۱۸ تیر"),
        DemoLoyaltyEntry("loy_02", "استفاده در سالن بهار", -120, isEarned = false, dateLabel = "۵ تیر"),
        DemoLoyaltyEntry("loy_03", "دعوت دوست", 150, isEarned = true, dateLabel = "۲۰ خرداد"),
        DemoLoyaltyEntry("loy_04", "رزرو در استودیو luxe", 150, isEarned = true, dateLabel = "۲۸ خرداد"),
    )
}
