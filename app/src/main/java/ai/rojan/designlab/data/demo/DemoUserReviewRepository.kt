package ai.rojan.designlab.data.demo

/** The reviews *this* user has written — distinct from [DemoReviewRepository], which supplies reviews shown on a salon/specialist's own page (i.e. reviews written by other demo customers). */
object DemoUserReviewRepository {
    val reviews: List<DemoUserReview> = listOf(
        DemoUserReview("ureview_01", "استودیو luxe", "5.0", "آرایش فوق‌العاده‌ای برای مراسم عروسی خواهرم داشتم.", "۲۹ خرداد"),
        DemoUserReview("ureview_02", "گلدن تاچ", "4.5", "طراحی ناخن خیلی خوب و دقیق بود.", "۱۶ خرداد"),
    )
}
