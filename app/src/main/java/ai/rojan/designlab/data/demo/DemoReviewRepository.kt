package ai.rojan.designlab.data.demo

object DemoReviewRepository {

    private val allReviews: List<DemoReview> = listOf(
        DemoReview("پریسا کریمی", "5.0", "کیفیت خدمات فوق‌العاده بود، حتماً دوباره میام.", daysAgo = 2),
        DemoReview("مهسا رضوی", "4.5", "برخورد پرسنل خیلی خوب بود و نتیجه کار عالی بود.", daysAgo = 5),
        DemoReview("زهرا نوری", "5.0", "بهترین تجربه‌ای بود که تا حالا داشتم.", daysAgo = 9),
        DemoReview("آیدا محمدی", "4.0", "خوب بود، فقط کمی طول کشید.", daysAgo = 14),
    )

    /** Same fixed review set reused everywhere — genuinely per-entity review data is out of this demo's scope. */
    fun reviewsFor(entityId: String): List<DemoReview> = allReviews
}
