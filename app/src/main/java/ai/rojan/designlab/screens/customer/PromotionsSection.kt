package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.R
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividPurple
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home promotions — Design Board v1.0, Secondary Features layer,
 * the lowest-priority tier among ServiceCategories/FeaturedSalons/
 * TopSpecialists/Promotions per the Board's explicit ordering.
 *
 * Architecture Cleanup Sprint (Task 3): data now comes from
 * [ai.rojan.designlab.data.demo.DemoPromotionRepository] via
 * [CatalogEngine] — same visual output, restraint (2 cards, no
 * illustration block, optional badge) unchanged.
 *
 * Luxury Visual Refinement Phase: per audit feedback ("avoid text-only
 * cards... add meaningful visual weight"), each card now leads with a
 * real salon photo (`salon_demo_2`/`salon_demo_3`, the same approved
 * photography set already used across Featured/Nearby/Recommended
 * Salons — alternated by index, not new/fake imagery) instead of an
 * icon glyph. Card width grew (200dp -> 220dp) to fit the photo without
 * cramping the text; the rest of the content (title, supporting text,
 * optional badge) is unchanged.
 */
@Composable
fun PromotionsSection() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(catalogEngine.promotions()) { index, promotion ->
            val imageRes = if (index % 2 == 0) R.drawable.salon_demo_2 else R.drawable.salon_demo_3
            // Font-scale fix: was a hard `.size(width, height)` — at larger
            // system font sizes the title/supporting-text/badge stack could
            // need more vertical room than the fixed height allowed.
            // `heightIn(min = ...)` keeps the exact same height whenever
            // content fits (unchanged from before at default scale) and
            // only grows under accessibility font scaling.
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .background(promotion.tint.copy(alpha = 0.25f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 104.dp)
                        .clickable { },
                    shape = RojanShapes.Small,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RojanSampleImage(
                            resId = imageRes,
                            contentDescription = null,
                            shape = RojanShapes.Small,
                            modifier = Modifier.size(width = 60.dp, height = 84.dp),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                            ) {
                                RojanIconContainer(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = RojanVividPurple,
                                    size = RojanIconSize.Small,
                                )
                                Text(
                                    text = promotion.title,
                                    style = RojanTypography.Caption,
                                    color = RojanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Text(
                                text = promotion.supportingText,
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            promotion.badge?.let { badgeText ->
                                Box(
                                    modifier = Modifier
                                        .background(RojanVividPurple.copy(alpha = 0.15f), RojanShapes.Small)
                                        .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = RojanTypography.Caption,
                                        color = RojanVividPurple,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
