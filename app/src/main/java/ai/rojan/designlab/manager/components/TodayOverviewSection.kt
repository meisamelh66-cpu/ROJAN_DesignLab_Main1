package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.toPersianDigits
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardStats
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — "today's overview" stat row.
 *
 * TEAM2-002 (Manager Data Persistence): [stats] comes from
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * — real computation over the salon's real backend bookings. This
 * screen still computes nothing itself — [OccupancyStatCard]'s ring is a
 * pure re-render of the already-real [ManagerDashboardStats.occupancyPercent],
 * not a new value.
 *
 * Safe-redesign pass: three real stats across one [Row], each tile at
 * `Modifier.weight(1f)` — a completely standard, bounded-constraint
 * pattern (the Row itself is measured with a finite width by its parent
 * `LazyColumn` item), not the flagged "weight inside unbounded parent"
 * risk. No `LazyRow`, no negative modifiers, no custom `Layout`.
 */
@Composable
fun TodayOverviewSection(stats: ManagerDashboardStats, modifier: Modifier = Modifier) {
    // Spacing-cleanup pass: the standalone "نمای امروز" heading is gone —
    // redundant next to the Salon Identity card above it — along with the
    // title-to-content gap that existed only for that heading. The KPI
    // row is the whole of this composable now.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceCardToCard),
    ) {
            StatCard(
                icon = Icons.Filled.EventAvailable,
                label = "نوبت‌های امروز",
                value = stats.todaysAppointmentCount.toPersianDigits(),
                accent = ManagerColors.Turquoise,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.AttachMoney,
                label = "درآمد امروز",
                value = stats.todaysRevenueLabel,
                accent = ManagerColors.Gold,
                modifier = Modifier.weight(1f),
            )
        OccupancyStatCard(
            percent = stats.occupancyPercent,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Shared shell for all three KPI tiles — identical padding/background/shadow/border, so the row reads as one consistent set of equal-geometry tiles. Plain neutral shadow + a thin turquoise hairline (controlled depth, not a metallic gold border). All values/shapes are fixed positive dp — no size-dependent or negative computation. Pixel-perfect pass: content is now centered (icon badge → number → label, vertically stacked), matching the reference's KPI card layout. */
@Composable
private fun FlatStatShell(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RojanShapes.Small,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .background(ManagerColors.BaseSecondary.copy(alpha = 0.35f), RojanShapes.Small)
            .border(1.dp, ManagerColors.Turquoise.copy(alpha = 0.14f), RojanShapes.Small)
            .padding(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Small filled color badge behind a KPI icon — matches the reference's icon treatment (a colored container, not a bare tinted glyph). */
@Composable
private fun KpiIconBadge(icon: ImageVector, accent: Color, contentDescription: String?) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(accent.copy(alpha = 0.18f), RojanShapes.Small),
        contentAlignment = Alignment.Center,
    ) {
        RojanIconContainer(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            size = RojanIconSize.Medium,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    FlatStatShell(modifier = modifier) {
        KpiIconBadge(icon = icon, accent = accent, contentDescription = label)
        Text(
            text = value,
            style = RojanTypography.CardTitle,
            color = ManagerColors.TextPrimary,
            modifier = Modifier.padding(top = RojanDimens.SpaceSM),
        )
        Text(
            text = label,
            style = RojanTypography.Caption,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.padding(top = RojanDimens.SpaceXS),
        )
    }
}

/** Same shell/footprint as [StatCard]; the value renders inside a fixed-size ring instead of beside an icon, since it is a percentage-of-whole rather than a raw count/amount. */
@Composable
private fun OccupancyStatCard(percent: Int, modifier: Modifier = Modifier) {
    val clampedPercent = percent.coerceIn(0, 100)

    FlatStatShell(modifier = modifier) {
        Box(
            modifier = Modifier.size(RojanDimens.IconSizeXLarge),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { clampedPercent / 100f },
                modifier = Modifier.fillMaxSize(),
                color = ManagerColors.Turquoise,
                trackColor = ManagerColors.TextSecondary.copy(alpha = 0.18f),
                strokeWidth = 4.dp,
            )
            Text(
                text = "٪${clampedPercent.toPersianDigits()}",
                style = RojanTypography.Caption,
                color = ManagerColors.TextPrimary,
            )
        }
        Text(
            text = "نرخ اشغال",
            style = RojanTypography.Caption,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.padding(top = RojanDimens.SpaceSM),
        )
    }
}
