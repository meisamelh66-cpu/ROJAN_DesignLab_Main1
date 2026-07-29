package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
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
            color = RojanGlassText,
            horizontalPadding = 0.dp,
        )

        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
            elevation = RojanShadows.FloatingElevation,
            glassAlpha = ManagerGlass.Alpha,
            glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
            borderAlpha = ManagerGlass.BorderAlpha,
            borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
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
                                .background(RojanTextOnDarkSurface.copy(alpha = 0.16f)),
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
                        color = ManagerAccent.Teal,
                    )
                    RojanIconContainer(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        size = RojanIconSize.Small,
                        tint = ManagerAccent.Teal,
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ManagerAccent.Teal.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                size = RojanIconSize.Small,
                tint = ManagerAccent.Teal,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slot.clientName,
                style = RojanTypography.Body,
                color = RojanGlassText,
            )
            Text(
                text = slot.service,
                style = RojanTypography.Caption,
                color = RojanTextOnDarkSurface,
            )
        }

        Text(
            text = slot.time,
            style = RojanTypography.Caption,
            color = RojanTextOnDarkSurface,
        )
    }
}
