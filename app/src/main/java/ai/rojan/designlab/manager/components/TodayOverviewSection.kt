package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Static placeholder metric — no backend wired yet. */
private data class OverviewStat(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val accent: Color,
)

/** Turquoise + Gold, alternating — matches the reference's KPI row exactly. */
private val sampleOverviewStats = listOf(
    OverviewStat(Icons.Filled.EventAvailable, "نوبت‌های امروز", "۱۲", ManagerColors.Turquoise),
    OverviewStat(Icons.Filled.AttachMoney, "درآمد امروز", "۴.۲م", ManagerColors.Gold),
    OverviewStat(Icons.Filled.Groups, "مشتریان جدید", "۳", ManagerColors.Turquoise),
    OverviewStat(Icons.AutoMirrored.Filled.TrendingUp, "نرخ اشغال", "٪۷۸", ManagerColors.Gold),
)

/**
 * Manager App workspace — "today's overview" stat grid. Static sample
 * values only ("No backend" per this pass); built from the Manager dark
 * luxury theme ([ManagerGlassSurface], [ManagerIconContainer]).
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout/data unchanged.
 */
@Composable
fun TodayOverviewSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "نمای امروز",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            sampleOverviewStats.chunked(2).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    rowStats.forEach { stat ->
                        StatCard(stat = stat, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(stat: OverviewStat, modifier: Modifier = Modifier) {
    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
            ManagerIconContainer(
                imageVector = stat.icon,
                contentDescription = stat.label,
                containerSize = 40.dp,
                accentColor = stat.accent,
            )
            Text(
                text = stat.value,
                style = RojanTypography.Display,
                color = ManagerColors.TextPrimary,
                modifier = Modifier.padding(top = RojanDimens.SpaceSM),
            )
            Text(
                text = stat.label,
                style = RojanTypography.Caption,
                color = ManagerColors.TextSecondary,
            )
        }
    }
}
