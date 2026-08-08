package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

import ai.rojan.designlab.domain.ai.AiRecommendationProvider
import ai.rojan.designlab.domain.ai.NoOpAiRecommendationProvider
import ai.rojan.designlab.domain.ai.RecommendationContext
import ai.rojan.designlab.screens.customer.hometheme.HomeCard
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.state.RojanComingSoonState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Home "ROJAN AI Recommended" section.
 *
 * Production Data Integrity Phase 1: previously rendered a hand-rolled,
 * per-salon "AI reason"/"confidence %" — the confidence figure was
 * `90 + (salon.id.hashCode().mod(10))`, a purely local calculation
 * presented as a real match score, and the reason text was two
 * hardcoded strings. That directly contradicted this codebase's own
 * already-established rule for this exact situation
 * ([AiRecommendationProvider]'s doc comment: "provider interface + empty
 * implementation is acceptable, fake AI is not" —
 * [NoOpAiRecommendationProvider]). This section now wires to that real
 * interface instead of fabricating its own numbers: today that means
 * [NoOpAiRecommendationProvider] (no recommendation engine exists yet),
 * so the section honestly renders its Coming Soon state — swapping in a
 * real [AiRecommendationProvider] implementation later needs no further
 * UI change here.
 */
@Composable
fun RecommendedSalons(
    onSalonClick: (String) -> Unit = {},
    recommendationProvider: AiRecommendationProvider = remember { NoOpAiRecommendationProvider() },
) {
    val recommendations = recommendationProvider.recommendationsFor(RecommendationContext(customerId = ""))

    if (recommendations.isEmpty()) {
        RojanComingSoonState(modifier = Modifier.fillMaxWidth())
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(recommendations) { index, recommendation ->
            HomeCard(
                accentColor = HomeColors.Glow,
                accentAlpha = 0.12f,
                onClick = { onSalonClick(recommendation.id) },
                index = index,
            ) {
                Text(
                    text = recommendation.title,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = HomeColors.Glow,
                        size = RojanIconSize.Small,
                    )
                    Text(
                        text = recommendation.reason,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
