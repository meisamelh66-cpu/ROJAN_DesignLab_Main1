package ai.rojan.designlab.manager.components

import ai.rojan.designlab.data.remote.dto.NetworkRecommendationPriority
import ai.rojan.designlab.data.remote.dto.RecommendationResponseDto
import ai.rojan.designlab.manager.data.ManagerRepositories
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — AI insight card (Final Backend Integration —
 * AI Insights Module). [message] is the highest-priority
 * [RecommendationResponseDto] from `ManagerRepositories.dashboardInsights`
 * (real `GET /api/v1/dashboard/insights`, `DashboardController`), not
 * static placeholder copy. `null` [dashboardInsights] (still loading, or
 * the one independent fetch failed — see `ManagerRepositories.initialize`'s
 * doc comment) and an empty `recommendations` list (genuinely no
 * recommendation right now) are both real, honest states — shown as quiet
 * copy, not an error, matching this codebase's established empty-state
 * convention (see `BackendAppointmentRepository`/`BackendServiceRepository`).
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerGlassSurface]/[ManagerIconContainer], Gold
 * accent for the AI signal) — content/copy unchanged from that pass.
 */
@Composable
fun AIInsightCard(modifier: Modifier = Modifier, refreshKey: Int = 0) {
    val message = remember(refreshKey) { highestPriorityMessage() }

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
                    text = message,
                    style = RojanTypography.Body,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
            }
        }
    }
}

private val PRIORITY_ORDER = listOf(
    NetworkRecommendationPriority.HIGH,
    NetworkRecommendationPriority.MEDIUM,
    NetworkRecommendationPriority.LOW,
)

private fun highestPriorityMessage(): String {
    val insights = ManagerRepositories.dashboardInsights ?: return "در حال دریافت پیشنهادهای هوش مصنوعی..."
    val recommendations = insights.recommendations
    if (recommendations.isEmpty()) return "در حال حاضر پیشنهاد هوش مصنوعی جدیدی برای سالن شما وجود ندارد."

    val best: RecommendationResponseDto = PRIORITY_ORDER
        .firstNotNullOfOrNull { priority -> recommendations.find { it.priority == priority } }
        ?: recommendations.first()
    return best.message
}
