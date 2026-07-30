package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home promotions — Home Visual Language Unification.
 *
 * Same horizontal list, same [CatalogEngine.promotions] data, same
 * position on Home as before. Card anatomy redesigned to match the
 * reference's "پیشنهادهای ویژه" cards — icon circle + title + discount
 * line + a "مشاهده" pill CTA — replacing the previous photo-leading
 * layout, per the approved "image presentation / CTA styling" visual-
 * anatomy change. No new interaction: the CTA was, and remains, a
 * decorative affordance (no destination existed before this pass either).
 */
@Composable
fun PromotionsSection() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(catalogEngine.promotions()) { index, promotion ->
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .background(promotion.tint.copy(alpha = 0.20f), RojanShapes.Small)
            ) {
                HomeGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(HomeColors.Glow.copy(alpha = 0.20f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            RojanIconContainer(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = HomeColors.Glow,
                                size = RojanIconSize.Medium,
                            )
                        }

                        Text(
                            text = promotion.title,
                            style = RojanTypography.Caption,
                            color = HomeColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = promotion.supportingText,
                            style = RojanTypography.Caption,
                            color = HomeColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        promotion.badge?.let { badgeText ->
                            Text(
                                text = badgeText,
                                style = RojanTypography.Caption,
                                color = HomeColors.Gold,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(HomeColors.Glow.copy(alpha = 0.16f), RojanShapes.Small)
                                .rojanPressable(onClick = {})
                                .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
                                imageVector = Icons.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = HomeColors.Glow,
                                size = RojanIconSize.Small,
                            )
                            Text(
                                text = "مشاهده",
                                style = RojanTypography.Caption,
                                color = HomeColors.Glow,
                            )
                        }
                    }
                }
            }
        }
    }
}
