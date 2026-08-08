package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.UpcomingSlot
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

/**
 * Manager App workspace — today's calendar preview. Phase 2, M6: [slots]
 * is a real slice of today's non-cancelled appointments
 * ([ai.rojan.designlab.manager.data.computeTodaysUpcomingSlots]) instead
 * of 3 hardcoded sample rows; empty shows [EmptyUpcomingNotice] rather
 * than nothing. "مشاهده تقویم کامل" routes to the real Calendar screen
 * via [onViewCalendarClick].
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout unchanged.
 */
@Composable
fun CalendarPreviewSection(
    modifier: Modifier = Modifier,
    slots: List<UpcomingSlot> = emptyList(),
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
                // Shared Premium Glass Design System spacing rhythm:
                // title-to-content gap everywhere.
                .padding(top = RojanDimens.SpaceTitleToContent),
            shape = RojanShapes.GlassCard,
        ) {
            Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                if (slots.isEmpty()) {
                    EmptyUpcomingNotice()
                }
                slots.forEachIndexed { index, slot ->
                    UpcomingSlotRow(slot = slot)
                    if (index != slots.lastIndex) {
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
private fun EmptyUpcomingNotice() {
    Text(
        text = "امروز نوبتی ثبت نشده است.",
        style = RojanTypography.Body,
        color = ManagerColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RojanDimens.SpaceSM),
    )
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
