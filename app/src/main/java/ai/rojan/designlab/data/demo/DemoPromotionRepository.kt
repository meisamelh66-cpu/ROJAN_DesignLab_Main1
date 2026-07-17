package ai.rojan.designlab.data.demo

import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanPearlPink
import androidx.compose.ui.graphics.Color

/** Architecture Cleanup Sprint (Task 3): extracted from [ai.rojan.designlab.screens.customer.PromotionsSection]'s previous inline data. */
data class DemoPromotion(
    val title: String,
    val supportingText: String,
    val badge: String?,
    val tint: Color,
)

object DemoPromotionRepository {
    val promotions: List<DemoPromotion> = listOf(
        DemoPromotion("پیشنهاد ماه", "تخفیف ویژه برای اولین نوبت شما", "٪۲۰", RojanAquaMint),
        DemoPromotion("بسته زیبایی", "مو و پوست با قیمت ویژه", null, RojanPearlPink),
    )
}
