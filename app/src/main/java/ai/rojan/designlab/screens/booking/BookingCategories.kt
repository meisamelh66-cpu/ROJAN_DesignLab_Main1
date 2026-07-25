package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanCategoryMakeupIcon
import ai.rojan.designlab.ui.theme.RojanCategoryNailsIcon
import ai.rojan.designlab.ui.theme.RojanCategorySkinIcon
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Module Design System Migration.
 *
 * Each category's distinct, deliberately-varied accent/gradient (purple
 * for hair, cyan for skin, pink for nails, orange for makeup) is
 * preserved via the new `RojanCategory*` tokens added for this migration
 * — these colors don't exist anywhere in the pre-existing palette, but
 * they were already part of the shipped, visible app before this
 * migration; centralizing them as named tokens doesn't introduce
 * anything visually new, it names what was already there.
 */
/**
 * Post Build UX Flow & Design System Audit, Issues 2/3/5: previously
 * each category card filled its ENTIRE background with a different
 * linear gradient (`RojanCategory{Hair,Skin,Nails,Makeup}{Start,End}` -
 * 8 distinct colors across 4 unrelated pairs) via a hand-rolled
 * `.background(Brush.linearGradient(...))`, never using [GlassSurface]
 * at all - exactly the reported "cards still look like colored
 * gradients instead of premium glass surfaces" and "category cards
 * still use unrelated gradients."
 *
 * Fixed: every card now renders through the one real, shared
 * [GlassSurface] (real frost/blur/unified border/unified shadow, same
 * as every other card in the app). Category distinction now comes from
 * the icon's tint only (a single accent color per category, not a
 * full-card gradient) - "Use Accent colors only. Do not create
 * independent card styles."
 */
@Composable
fun BookingCategories(
    onCategoryClick: () -> Unit = {},
) {

    val categories = listOf(
        CategoryData("مو", Icons.Default.ContentCut, RojanAIGlow),
        CategoryData("پوست", Icons.Default.Face, RojanCategorySkinIcon),
        CategoryData("ناخن", Icons.Default.Favorite, RojanCategoryNailsIcon),
        CategoryData("آرایش", Icons.Default.Brush, RojanCategoryMakeupIcon),
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)
    ) {
        categories.forEach {
            Category3DCard(item = it, onClick = onCategoryClick)
        }
    }
}

data class CategoryData(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
)

@Composable
private fun Category3DCard(item: CategoryData, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick, interactionSource = interactionSource),
        shape = RojanShapes.GlassCard,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = RojanDimens.SpaceLG, vertical = RojanDimens.SpaceMD),
        ) {

            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Text(
                text = item.title,
                style = RojanTypography.CardTitle.rojanPressedShadow(interactionSource),
                color = RojanTextPrimary,
            )
        }
    }
}
