package ai.rojan.designlab.data.demo

object DemoCouponRepository {
    val coupons: List<DemoCoupon> = listOf(
        DemoCoupon("coupon_01", "۲۰٪ تخفیف اولین رزرو", "برای اولین رزرو خود در ROJAN AI استفاده کنید", 20, "۳۰ تیر", "ROJAN20"),
        DemoCoupon("coupon_02", "۱۵٪ تخفیف خدمات پوست", "ویژه خدمات پاکسازی و مراقبت پوست", 15, "۲۵ تیر", "SKIN15"),
        DemoCoupon("coupon_03", "۱۰٪ تخفیف تولد", "هدیه تولد ROJAN AI برای شما", 10, "۵ مرداد", "BIRTHDAY10"),
    )

    fun findById(id: String): DemoCoupon? = coupons.find { it.id == id }
}
