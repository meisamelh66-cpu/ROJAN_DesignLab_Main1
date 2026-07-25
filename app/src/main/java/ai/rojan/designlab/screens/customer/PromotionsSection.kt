package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
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
 */
@Composable
fun PromotionsSection() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.promotions()) { promotion ->
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 96.dp)
                    .background(promotion.tint.copy(alpha = 0.25f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(RojanDimens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
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
