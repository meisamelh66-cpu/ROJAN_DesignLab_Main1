package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.toPersianDigits
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — AI insight card. Phase 2, M6: [message] is the
 * real, single highest-priority recommendation from the backend's
 * deterministic rule-based engine (`RuleBasedRecommendationEngine` — no
 * external LLM call, computed server-side from real revenue/booking/
 * customer/service data) —
 * [ai.rojan.designlab.manager.domain.dashboard.ManagerDashboardInsights.topRecommendationMessage],
 * already resolved to the single highest-priority message by
 * [ai.rojan.designlab.manager.data.BackendDashboardRepository], not
 * recomputed here. `null` means either the fetch hasn't completed yet,
 * failed independently, or the engine's rules genuinely found nothing
 * worth surfacing right now — all three are real, honest states shown as
 * a neutral "nothing to report" message, not a fabricated placeholder.
 *
 * AI Insight Presentation Layer, Phase 7 Step 4: [inactiveCustomerCount]
 * is a second, unrelated real signal -
 * [ai.rojan.designlab.manager.data.ManagerRepositories.crmInsights]'s
 * size. Rendered only when > 0.
 *
 * Safe-redesign pass: the card's identity comes from turquoise (robot
 * icon, badge tag) rather than gold — gold stays limited to the one real
 * CTA button below. All modifiers are fixed positive dp values.
 */
@Composable
fun AIInsightCard(
    modifier: Modifier = Modifier,
    message: String? = null,
    inactiveCustomerCount: Int = 0,
    onInactiveCustomersClick: () -> Unit = {},
) {
    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
        // `compact` drops the sparkle/sheen passes; the lower border alpha
        // keeps the remaining metallic stroke a quiet hairline.
        compact = true,
        borderAlpha = 0.32f,
        borderSecondaryAlpha = 0.28f,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            ) {
                ManagerIconContainer(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = "پیشنهاد هوشمند",
                    containerSize = 56.dp,
                    accentColor = ManagerColors.Turquoise,
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Pixel-perfect pass: single title, matching the
                    // reference exactly — the previous duplicated tag
                    // beside it repeated the same words twice.
                    Text(
                        text = "پیشنهاد هوشمند",
                        style = RojanTypography.CardTitle,
                        color = ManagerColors.TextPrimary,
                    )
                    Text(
                        text = message ?: "در حال حاضر پیشنهاد خاصی برای سالن شما وجود ندارد.",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
            }

            if (inactiveCustomerCount > 0) {
                Row(
                    modifier = Modifier
                        .padding(start = RojanDimens.SpaceMD, end = RojanDimens.SpaceMD, bottom = RojanDimens.SpaceMD)
                        .background(ManagerColors.Gold, RojanShapes.PremiumButton)
                        .rojanPressable(onClick = onInactiveCustomersClick)
                        .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    Text(
                        text = "${inactiveCustomerCount.toPersianDigits()} مشتری غیرفعال",
                        style = RojanTypography.Caption,
                        color = ManagerColors.BaseDeep,
                    )
                    RojanIconContainer(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        size = RojanIconSize.Small,
                        tint = ManagerColors.BaseDeep,
                    )
                }
            }
        }
    }
}
