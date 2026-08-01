package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Stable identifier for each quick action — navigation switches on this, never on [QuickAction.label] (display text, not an identifier). */
enum class ManagerQuickAction {
    NEW_APPOINTMENT, NEW_CUSTOMER, SERVICES, STAFF, SETTINGS
}

private data class QuickAction(
    val type: ManagerQuickAction,
    val icon: ImageVector,
    val label: String,
)

private val sampleQuickActions = listOf(
    QuickAction(ManagerQuickAction.NEW_APPOINTMENT, Icons.Filled.CalendarMonth, "نوبت جدید"),
    QuickAction(ManagerQuickAction.NEW_CUSTOMER, Icons.Filled.PersonAdd, "مشتری جدید"),
    QuickAction(ManagerQuickAction.SERVICES, Icons.Filled.ContentCut, "خدمات"),
    QuickAction(ManagerQuickAction.STAFF, Icons.Filled.Groups, "کارکنان"),
    QuickAction(ManagerQuickAction.SETTINGS, Icons.Filled.Settings, "تنظیمات"),
)

/**
 * Manager App workspace — quick-actions row. [onActionClick] is keyed by
 * [ManagerQuickAction] (typed) rather than the chip's display label, so
 * call sites route on a stable identifier that can't drift if the
 * Persian label copy ever changes. NEW_APPOINTMENT/NEW_CUSTOMER route to
 * real screens; SERVICES/STAFF/SETTINGS have no implemented screen
 * anywhere in the project yet (verified — no screen, nav entry,
 * repository-backed management UI, domain model for Settings, or any
 * other consumable implementation exists), so they stay visible and
 * tappable like every other chip, just with no handler wired for those
 * three cases at the call site — not disabled, not a placeholder.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerGlassSurface]/[ManagerIconContainer]) —
 * content/layout unchanged.
 */
@Composable
fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onActionClick: (ManagerQuickAction) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "دسترسی سریع",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        LazyRow(
            // Shared Premium Glass Design System spacing rhythm:
            // title-to-content gap everywhere.
            modifier = Modifier.padding(top = RojanDimens.SpaceTitleToContent),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceCardToCard),
        ) {
            items(sampleQuickActions) { action ->
                QuickActionChip(action = action, onClick = { onActionClick(action.type) })
            }
        }
    }
}

@Composable
private fun QuickActionChip(action: QuickAction, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .width(84.dp)
            .heightIn(min = 88.dp)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceSM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            ManagerIconContainer(
                imageVector = action.icon,
                contentDescription = action.label,
                containerSize = 40.dp,
            )
            Text(
                text = action.label,
                style = RojanTypography.Caption,
                color = ManagerColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
