package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/** Teal + Gold, alternating — the Manager identity, not the Customer purple/pink palette. */
private val sampleOverviewStats = listOf(
    OverviewStat(Icons.Filled.EventAvailable, "نوبت‌های امروز", "۱۲", ManagerAccent.Teal),
    OverviewStat(Icons.Filled.AttachMoney, "درآمد امروز", "۴.۲م", ManagerAccent.Gold),
    OverviewStat(Icons.Filled.Groups, "مشتریان جدید", "۳", ManagerAccent.Teal),
    OverviewStat(Icons.AutoMirrored.Filled.TrendingUp, "نرخ اشغال", "٪۷۸", ManagerAccent.Gold),
)

/**
 * Manager App workspace — "today's overview" stat grid. Static sample
 * values only ("No backend" per this pass); built from shared glass/
 * typography/token primitives, same layered ambient-glow-behind-glass
 * pattern already used elsewhere in the design system (see
 * [ai.rojan.designlab.ui.components.cards.RojanHomeCard]).
 */
@Composable
fun TodayOverviewSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "نمای امروز",
            style = RojanTypography.SectionTitle,
            color = RojanGlassText,
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
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
        glassAlpha = ManagerGlass.Alpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
    ) {
        Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(stat.accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                RojanIconContainer(
                    imageVector = stat.icon,
                    contentDescription = stat.label,
                    size = RojanIconSize.Medium,
                    tint = stat.accent,
                )
            }
            Text(
                text = stat.value,
                style = RojanTypography.Display,
                color = RojanGlassText,
                modifier = Modifier.padding(top = RojanDimens.SpaceSM),
            )
            Text(
                text = stat.label,
                style = RojanTypography.Caption,
                color = RojanTextOnDarkSurface,
            )
        }
    }
}
