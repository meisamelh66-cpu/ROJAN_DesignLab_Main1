package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.ManagerDashboardStats
import ai.rojan.designlab.manager.data.computeManagerDashboardStats
import ai.rojan.designlab.manager.data.toPersianDigits
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class OverviewStat(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val accent: Color,
)

/** Turquoise + Gold, alternating — matches the reference's KPI row exactly; values come from [stats], not sample data. */
private fun overviewStatsFrom(stats: ManagerDashboardStats): List<OverviewStat> = listOf(
    OverviewStat(Icons.Filled.EventAvailable, "نوبت‌های امروز", stats.todaysAppointmentCount.toPersianDigits(), ManagerColors.Turquoise),
    OverviewStat(Icons.Filled.AttachMoney, "درآمد امروز", stats.todaysRevenueLabel, ManagerColors.Gold),
    OverviewStat(Icons.Filled.Groups, "مشتریان جدید", stats.newCustomerCount.toPersianDigits(), ManagerColors.Turquoise),
    OverviewStat(Icons.AutoMirrored.Filled.TrendingUp, "نرخ اشغال", "٪${stats.occupancyPercent.toPersianDigits()}", ManagerColors.Gold),
)

/**
 * Manager App workspace — "today's overview" stat grid. Values come from
 * [computeManagerDashboardStats] (real computation over
 * [ai.rojan.designlab.manager.data.ManagerRepositories], previously
 * built but unused) — no static sample data.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout unchanged.
 */
@Composable
fun TodayOverviewSection(modifier: Modifier = Modifier) {
    val overviewStats = remember { overviewStatsFrom(computeManagerDashboardStats()) }

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
                // Shared Premium Glass Design System spacing rhythm:
                // title-to-content gap everywhere — the title reads as
                // integrated into the section, not floating above it.
                .padding(top = RojanDimens.SpaceTitleToContent),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceCardToCard),
        ) {
            overviewStats.chunked(2).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceCardToCard),
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
