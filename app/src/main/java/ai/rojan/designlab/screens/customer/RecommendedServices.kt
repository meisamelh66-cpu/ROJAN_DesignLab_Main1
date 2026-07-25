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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoService
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
 * Code Cleanup pass: migrated off its previous local `fakeRecommendations`
 * list (which duplicated service names under different labels) onto
 * [CatalogEngine.allServices] — real [DemoService] data. "AI reason" and
 * "confidence" remain demo flavor text (no real recommendation engine
 * exists, per [ai.rojan.designlab.domain.ai.NoOpAiRecommendationProvider]) —
 * but now describe genuinely real services, not a separate duplicate
 * dataset invented just for this section.
 */
private fun aiReasonFor(service: DemoService): String = when (service.categoryLabel) {
    "مو" -> "بر اساس انتخاب‌های قبلی شما"
    "پوست" -> "متناسب با نوع پوست شما"
    "ناخن" -> "بر اساس علاقه‌مندی‌های شما"
    else -> "پیشنهاد ویژه برای شما"
}

private fun confidenceFor(service: DemoService): String =
    "${90 + (service.id.hashCode().mod(10))}٪ تطابق"

/**
 * Customer Home AI recommendations — Design Board v1.0, AI-personalized
 * layer. Priority similar to [FeaturedSalons]/[TopSpecialists], below
 * HeroBookingCard/AISearchBar.
 *
 * AI identity is expressed through exactly one small element per card —
 * a compact "پیشنهاد AI برای شما" label with an [RojanAIGlow]-tinted
 * sparkle icon — not through a differently-colored card, a glow border,
 * or any per-card visual effect. This is deliberate: the Board explicitly
 * asks for AI identity that's "visible but subtle" and warns against
 * "excessive AI decoration" and "neon futuristic style." Everything else
 * about the card (size, glass construction, typography) matches
 * FeaturedSalons/TopSpecialists exactly, so the "AI-ness" of this section
 * reads as one quiet signal layered onto the same trusted visual
 * language, not a different, flashier design system bolted on for AI.
 *
 * The "AI reason" text (e.g. "بر اساس انتخاب‌های قبلی شما") and the
 * confidence indicator are what actually carry the "ROJAN AI understands
 * my beauty needs" feeling per the Board's brief — through specific,
 * personal-sounding copy, not through visual spectacle.
 *
 * Home Screen Production Pass, Task 7/8: card width was 170dp, the only
 * outlier among this screen's 5 identically-structured horizontal-scroll
 * cards (the other 4 already use 160dp) — moved onto
 * [RojanDimens.CardWidthStandard]/[RojanDimens.CardHeightStandard], the
 * exact token that exists for this pair (see its own doc comment). Task
 * 9: the star-badge icon moves from a bespoke 24dp onto
 * [RojanIconSize.Medium] (20dp).
 *
 * Production Asset Normalization: the badge circle now shows the
 * service's real sample image ([RojanSampleImage], circular crop) when
 * [DemoService.assetRes] is set — the star icon remains only as the
 * fallback for the null case, matching every other card's
 * image-with-icon-fallback pattern (previously this was the one card
 * family with no image slot at all, icon-only).
 */
@Composable
fun RecommendedServices() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.allServices()) { service ->
            Box(
                modifier = Modifier
                    .size(width = RojanDimens.CardWidthStandard, height = RojanDimens.CardHeightStandard)
                    .background(RojanAIGlow.copy(alpha = 0.12f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
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
                            if (service.assetRes != null) {
                                RojanSampleImage(
                                    resId = service.assetRes,
                                    contentDescription = service.name,
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                RojanIconContainer(
    imageVector = Icons.Filled.Star,
    contentDescription = null,
    tint = RojanTextPrimary,
    size = RojanIconSize.Medium,
)
                            }
                        }

                        Text(
                            text = service.name,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = aiReasonFor(service),
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = confidenceFor(service),
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                        )
                    }
                }
            }
        }
    }
}
