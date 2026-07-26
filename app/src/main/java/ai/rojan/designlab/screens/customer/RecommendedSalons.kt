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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * UX Refactor Phase 1: renamed from "RecommendedServices" and repointed
 * from [CatalogEngine.allServices] to [CatalogEngine.allSalons] — the
 * target Customer Home spec calls for "ROJAN AI Recommended Salons," not
 * services. "AI reason" and "confidence" remain demo flavor text (no
 * real recommendation engine exists, per
 * [ai.rojan.designlab.domain.ai.NoOpAiRecommendationProvider]), same as
 * before this rename — only the entity being recommended changed.
 *
 * Visual language (AI badge, card size, glass construction) is
 * unchanged from the previous services version, per the Design Board's
 * "AI identity visible but subtle" rule — see the original
 * RecommendedServices doc history in version control for the full
 * rationale.
 *
 * [onSalonClick] wires this section to real navigation (Salon Profile) —
 * previously `clickable {}` (dead).
 */
private fun aiReasonFor(salon: DemoSalon): String =
    if (salon.facilities.isNotEmpty()) {
        "بر اساس امکانات و سلیقه شما"
    } else {
        "بر اساس بازدیدهای قبلی شما"
    }

private fun confidenceFor(salon: DemoSalon): String =
    "${90 + (salon.id.hashCode().mod(10))}٪ تطابق"

@Composable
fun RecommendedSalons(onSalonClick: (String) -> Unit = {}) {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.allSalons()) { salon ->
            Box(
                modifier = Modifier
                    .size(width = RojanDimens.CardWidthStandard, height = RojanDimens.CardHeightStandard)
                    .background(RojanAIGlow.copy(alpha = 0.12f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onSalonClick(salon.id) },
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        // The one AI-identity element on this card — small, quiet, single icon + label.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.AutoAwesome,
    contentDescription = null,
    tint = RojanAIGlow,
    size = RojanIconSize.Small,
)
                            Text(
                                text = "پیشنهاد AI برای شما",
                                style = RojanTypography.Caption,
                                color = RojanAIGlow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RojanAIGlow.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (salon.assetRes != null) {
                                RojanSampleImage(
                                    resId = salon.assetRes,
                                    contentDescription = salon.name,
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                RojanIconContainer(
    imageVector = Icons.Filled.Storefront,
    contentDescription = null,
    tint = RojanTextPrimary,
    size = RojanIconSize.Medium,
)
                            }
                        }

                        Text(
                            text = salon.name,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = aiReasonFor(salon),
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = confidenceFor(salon),
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                        )
                    }
                }
            }
        }
    }
}
