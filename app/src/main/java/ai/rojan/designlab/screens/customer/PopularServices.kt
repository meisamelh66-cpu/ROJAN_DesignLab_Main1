package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.cards.RojanHomeCard
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanCategoryHairEnd
import ai.rojan.designlab.ui.theme.RojanCategoryHairStart
import ai.rojan.designlab.ui.theme.RojanCategoryMakeupEnd
import ai.rojan.designlab.ui.theme.RojanCategoryMakeupStart
import ai.rojan.designlab.ui.theme.RojanCategoryNailsEnd
import ai.rojan.designlab.ui.theme.RojanCategoryNailsStart
import ai.rojan.designlab.ui.theme.RojanCategorySkinEnd
import ai.rojan.designlab.ui.theme.RojanCategorySkinStart
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Home "Popular Services" section — Phase 3.
 *
 * Same shell every other Home horizontal list already uses:
 * [CatalogEngine.allServices] (the canonical demo service list, same one
 * Salon Details/Service Details already read from — no new/fake data) laid
 * out via [RojanHomeCard] (the shared glass-card-with-shadow-and-press-
 * animation primitive [FeaturedSalons]/[TopSpecialists] already use, so
 * this section matches their spacing/corner-radius/motion exactly rather
 * than reintroducing its own).
 *
 * The title + "مشاهده همه" header row is new — no other Home section has
 * one yet — but uses only existing typography tokens ([RojanTypography.CardTitle]
 * for the title, [RojanTypography.Caption] for the action) and the same
 * accent color already established for actionable/active affordances
 * elsewhere on this screen ([RojanAIGlow], see [CustomerBottomBar]'s
 * active-tab tint and [SearchModeTabs]'s selected pill).
 *
 * [onViewAllClick]/each card's `onClick` are no-ops for now — Phase 3's
 * explicit scope is the section's layout only ("No navigation
 * implementation yet. No business logic.").
 *
 * Home Premium Polish Phase: each card's icon was a raster
 * [ai.rojan.designlab.ui.assets.SampleImageProvider]-supplied image, but
 * every shipped `ic_service_*.webp` file on disk turned out to be the
 * same kind of non-functional flat gradient-square placeholder already
 * found and fixed in [CustomerBottomBar]'s nav icons — not real
 * iconography. [categoryIcon] applies the identical fix here: a real,
 * recognizable Material vector icon per [ai.rojan.designlab.data.demo.DemoService.categoryLabel],
 * rendered inside the same circular soft-glass container.
 *
 * 3D Glassmorphism Depth Pass: the icon circle now sits in front of its
 * own blurred, oversized ambient-glow twin (background glow layer) and
 * carries a small blurred white highlight in its top-start corner
 * (inner glass reflection) — the same visual language [GlassSurface]'s
 * `showHighlight` already applies to every rectangular glass card, made
 * explicit here since this circle isn't a [GlassSurface] instance.
 *
 * Depth Pass V2: icon-circle shadow raised a tier (SoftElevation ->
 * FloatingElevation) so the icon reads as sitting above the glass
 * surface rather than flush with it.
 *
 * Luxury Visual Refinement Phase: the `ic_service_*.webp` files remain
 * confirmed non-functional (see above), and no other beauty-specific
 * image asset exists on disk to replace them with — so instead of a new
 * asset, [categoryGradient] gives each icon circle a richer two-tone
 * gradient drawn from [ai.rojan.designlab.ui.theme.RojanCategorySkinStart]
 * and its sibling category-accent token pairs (already defined in
 * `RojanTokens.kt`, previously unused by this file) rather than one flat
 * tone repeated at two alphas. Categories without a matching token pair
 * (اپیلاسیون/ماساژ/ابرو/لیزر) keep the existing [service.colorSeed]
 * behavior unchanged.
 */
@Composable
fun PopularServices(
    onViewAllClick: () -> Unit = {},
) {
    val catalogEngine = remember { CatalogEngine() }

    Column(
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "خدمات محبوب",
                style = RojanTypography.CardTitle,
                color = RojanTextPrimary,
            )
            Text(
                text = "مشاهده همه",
                style = RojanTypography.Caption,
                color = RojanAIGlow,
                modifier = Modifier.clickable(onClick = onViewAllClick),
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            itemsIndexed(catalogEngine.allServices()) { index, service ->
                RojanHomeCard(
                    accentColor = service.colorSeed,
                    onClick = { },
                    index = index,
                    height = 172.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val (gradientStart, gradientEnd) = categoryGradient(service.categoryLabel, service.colorSeed)

                        // Layer 1: background ambient glow — blurred, larger than the icon itself
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .blur(18.dp)
                                .background(gradientStart.copy(alpha = 0.5f), RojanShapes.Circle),
                        )

                        // Layer 2: glass surface (icon circle) + Layer 3: floating icon content
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = RojanShadows.FloatingElevation,
                                    shape = RojanShapes.Circle,
                                    ambientColor = gradientEnd.copy(alpha = 0.45f),
                                    spotColor = gradientEnd.copy(alpha = 0.45f),
                                )
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            gradientStart.copy(alpha = 0.65f),
                                            gradientEnd.copy(alpha = 0.35f),
                                        ),
                                    ),
                                    shape = RojanShapes.Circle,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 6.dp, start = 10.dp)
                                    .size(26.dp)
                                    .blur(8.dp)
                                    .background(Color.White.copy(alpha = 0.35f), RojanShapes.Circle),
                            )

                            RojanIconContainer(
                                imageVector = categoryIcon(service.categoryLabel),
                                contentDescription = service.name,
                                tint = RojanTextPrimary,
                                size = RojanIconSize.XLarge,
                            )
                        }
                    }

                    Text(
                        text = service.name,
                        style = RojanTypography.Body.copy(fontWeight = FontWeight.Bold),
                        color = RojanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = service.categoryLabel,
                        style = RojanTypography.Caption,
                        color = RojanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Category → richer two-tone gradient, drawn from the existing booking-category accent tokens. Falls back to [fallback] (repeated for both stops) for categories with no dedicated token pair. */
private fun categoryGradient(categoryLabel: String, fallback: Color): Pair<Color, Color> = when (categoryLabel) {
    "مو" -> RojanCategoryHairStart to RojanCategoryHairEnd
    "پوست" -> RojanCategorySkinStart to RojanCategorySkinEnd
    "آرایش" -> RojanCategoryMakeupStart to RojanCategoryMakeupEnd
    "ناخن" -> RojanCategoryNailsStart to RojanCategoryNailsEnd
    else -> fallback to fallback
}

/** Category → recognizable Material icon. اپیلاسیون/لیزر are both hair-removal categories and intentionally share no icon with each other — each gets the closest distinct match instead. */
private fun categoryIcon(categoryLabel: String): ImageVector = when (categoryLabel) {
    "مو" -> Icons.Filled.ContentCut
    "پوست" -> Icons.Filled.Face
    "آرایش" -> Icons.Filled.Brush
    "ناخن" -> Icons.Filled.BackHand
    "اپیلاسیون" -> Icons.Filled.Spa
    "ماساژ" -> Icons.Filled.SelfImprovement
    "ابرو" -> Icons.Filled.RemoveRedEye
    "لیزر" -> Icons.Filled.FlashOn
    else -> Icons.Filled.ContentCut
}
