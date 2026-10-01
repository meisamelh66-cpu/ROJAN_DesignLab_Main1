package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.UpcomingSlot
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — today's calendar preview. Phase 2, M6: [slots]
 * is a real slice of today's non-cancelled appointments
 * ([ai.rojan.designlab.manager.data.computeTodaysUpcomingSlots]) instead
 * of 3 hardcoded sample rows; empty shows [EmptyUpcomingNotice] rather
 * than nothing. The "بیشتر" control routes to the real Calendar screen
 * via [onViewCalendarClick] — same handler as before, just relocated.
 *
 * Pixel-perfect pass: the standalone heading above the card is gone —
 * title + icon badge + "بیشتر" now form the card's own top row, matching
 * the reference exactly, and the bottom link row is removed (its
 * function fully replaced by the top-row control, not duplicated).
 */
@Composable
fun CalendarPreviewSection(
    modifier: Modifier = Modifier,
    slots: List<UpcomingSlot> = emptyList(),
    onViewCalendarClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RojanShapes.GlassCard,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .background(ManagerColors.BaseSecondary.copy(alpha = 0.3f), RojanShapes.GlassCard)
            .border(1.dp, ManagerColors.Turquoise.copy(alpha = 0.14f), RojanShapes.GlassCard)
            .padding(RojanDimens.SpaceMD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(ManagerColors.Turquoise.copy(alpha = 0.18f), RojanShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = ManagerColors.Turquoise,
                        size = RojanIconSize.Small,
                    )
                }
                Text(
                    text = "نوبت‌های امروز",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                )
            }

            Row(
                modifier = Modifier
                    .background(ManagerColors.Turquoise.copy(alpha = 0.14f), RojanShapes.Small)
                    .rojanPressable(onClick = onViewCalendarClick)
                    .padding(horizontal = RojanDimens.SpaceSM, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "بیشتر",
                    style = RojanTypography.Caption,
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

        Column(modifier = Modifier.padding(top = RojanDimens.SpaceMD)) {
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
        ClientInitialAvatar(name = slot.clientName)

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

/** Client's own real name, initial-lettered — no photo field exists on [UpcomingSlot], so this is the honest avatar rather than a placeholder image. */
@Composable
private fun ClientInitialAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(ManagerColors.Turquoise.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.toString() ?: "?",
            style = RojanTypography.Button,
            color = ManagerColors.TurquoiseLight,
        )
    }
}
