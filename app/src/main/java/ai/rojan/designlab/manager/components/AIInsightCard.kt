package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — AI insight card. Phase 2, M6: [message] is the
 * real, single highest-priority recommendation from the backend's
 * deterministic rule-based engine (`RuleBasedRecommendationEngine` — no
 * external LLM call, computed server-side from real revenue/booking/
 * customer/service data) — [ai.rojan.designlab.manager.domain.dashboard.ManagerDashboardInsights.topRecommendationMessage],
 * already resolved to the single highest-priority message by
 * [ai.rojan.designlab.manager.data.BackendDashboardRepository], not
 * recomputed here (screens/components don't own business logic in this
 * codebase). `null` means either the fetch hasn't completed yet, failed
 * independently (see `ManagerRepositories.initialize`'s own doc comment),
 * or the engine's rules genuinely found nothing worth surfacing right now
 * (e.g. a brand-new salon with no booking history yet) - all three are
 * real, honest states shown as a neutral "nothing to report" message, not
 * a fabricated placeholder standing in for it.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerGlassSurface]/[ManagerIconContainer], Gold
 * accent for the AI signal) — content/copy unchanged from that pass.
 */
@Composable
fun AIInsightCard(modifier: Modifier = Modifier, message: String? = null) {
    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "پیشنهاد هوش مصنوعی",
                containerSize = 56.dp,
                accentColor = ManagerColors.Gold,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "پیشنهاد هوش مصنوعی",
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
    }
}
