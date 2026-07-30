package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Static placeholder appointment — no backend wired yet. */
private data class UpcomingSlot(
    val time: String,
    val clientName: String,
    val service: String,
)

private val sampleUpcomingSlots = listOf(
    UpcomingSlot("۱۰:۰۰", "سارا محمدی", "رنگ مو"),
    UpcomingSlot("۱۲:۳۰", "نیلوفر احمدی", "میکاپ عروس"),
    UpcomingSlot("۱۵:۰۰", "پریسا کریمی", "مانیکور"),
)

/**
 * Manager App workspace — today's calendar preview. Static sample slots
 * only; "مشاهده تقویم کامل" is inert for now ([onViewCalendarClick]
 * no-op default) since routing isn't wired in this pass.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout/navigation unchanged.
 */
@Composable
fun CalendarPreviewSection(
    modifier: Modifier = Modifier,
    onViewCalendarClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "برنامه امروز",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                sampleUpcomingSlots.forEachIndexed { index, slot ->
                    UpcomingSlotRow(slot = slot)
                    if (index != sampleUpcomingSlots.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RojanDimens.SpaceSM)
                                .height(1.dp)
                                .background(ManagerColors.TextSecondary.copy(alpha = 0.16f)),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RojanDimens.SpaceMD)
                        .rojanPressable(onClick = onViewCalendarClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "مشاهده تقویم کامل",
                        style = RojanTypography.Button,
                        color = ManagerColors.Turquoise,
                    )
                    RojanIconContainer(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        size = RojanIconSize.Small,
                        tint = ManagerColors.Turquoise,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingSlotRow(slot: UpcomingSlot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        ManagerIconContainer(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            containerSize = 36.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slot.clientName,
                style = RojanTypography.Body,
                color = ManagerColors.TextPrimary,
            )
            Text(
                text = slot.service,
                style = RojanTypography.Caption,
                color = ManagerColors.TextSecondary,
            )
        }

        Text(
            text = slot.time,
            style = RojanTypography.Caption,
            color = ManagerColors.TextSecondary,
        )
    }
}
