package ai.rojan.designlab.data.demo

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Architecture Cleanup Sprint (Task 3): extracted from
 * [ai.rojan.designlab.screens.customer.ServiceCategories]' previous
 * inline `private val fakeCategories` list — same data classification
 * pattern as [DemoSalon]/[DemoSpecialist] (Color/drawable-int fields
 * held directly, consistent with existing precedent, not a new one).
 *
 * Demo Content Population Pass: expanded from 5 to the full 10
 * categories the product spec calls for. The original 5 keep their
 * real custom-drawn [iconRes] assets. The 5 new ones
 * (ابرو/مژه/اپیلاسیون/ماساژ/لیزر) don't have custom icons designed yet,
 * so [fallbackIcon] is used instead — explicitly temporary, per "Use
 * temporary icons until the official ROJAN Icon Library is completed."
 * Exactly the same real/fallback pattern already established for salon
 * and specialist images ([DemoSalon.assetRes], [DemoSpecialist.assetRes]).
 */
data class DemoServiceCategory(
    val label: String,
    val tint: Color,
    val iconRes: Int? = null,
    val fallbackIcon: ImageVector? = null,
)

object DemoServiceCategoryRepository {
    val categories: List<DemoServiceCategory> = listOf(
        DemoServiceCategory("مو", RojanSoftLavender, iconRes = R.drawable.ic_service_hair),
        DemoServiceCategory("پوست", RojanAquaMint, iconRes = R.drawable.ic_service_skin),
        DemoServiceCategory("آرایش", RojanBlushPink, iconRes = R.drawable.ic_service_makeup),
        DemoServiceCategory("ناخن", RojanPearlPink, iconRes = R.drawable.ic_service_nails),
        DemoServiceCategory("ابرو", RojanBlushPink, fallbackIcon = Icons.Filled.RemoveRedEye),
        DemoServiceCategory("مژه", RojanPearlPink, fallbackIcon = Icons.Filled.AutoAwesome),
        DemoServiceCategory("اپیلاسیون", RojanAquaMint, fallbackIcon = Icons.Filled.WaterDrop),
        DemoServiceCategory("ماساژ", RojanSoftLavender, fallbackIcon = Icons.Filled.SelfImprovement),
        DemoServiceCategory("اسپا", RojanSoftLavender, iconRes = R.drawable.ic_service_spa),
        DemoServiceCategory("لیزر", RojanAquaMint, fallbackIcon = Icons.Filled.Bolt),
    )
}
