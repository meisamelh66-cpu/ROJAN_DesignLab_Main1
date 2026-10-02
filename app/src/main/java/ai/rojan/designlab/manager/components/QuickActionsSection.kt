package ai.rojan.designlab.manager.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Stable identifier for each quick action — navigation switches on this, never on [QuickAction.label] (display text, not an identifier). */
enum class ManagerQuickAction {
    NEW_APPOINTMENT, NEW_CUSTOMER, SERVICES, STAFF, SETTINGS
}

private data class QuickAction(
    val type: ManagerQuickAction,
    val icon: ImageVector,
    val label: String,
    /** The one action a manager reaches for most — gold accent, stronger fill/border. The rest stay neutral teal/turquoise, clearly subordinate. */
    val isPrimary: Boolean = false,
)

private val sampleQuickActions = listOf(
    QuickAction(ManagerQuickAction.NEW_APPOINTMENT, Icons.Filled.CalendarMonth, "نوبت جدید", isPrimary = true),
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
 * anywhere in the project yet, so they stay visible and tappable like
 * every other chip, just with no handler wired for those three cases at
 * the call site — not disabled, not a placeholder.
 *
 * Safe-redesign pass: a fixed-width [androidx.compose.foundation.lazy.LazyRow]
 * (84dp × 5 chips + gaps) could run wider than the screen and force
 * horizontal scrolling/clipping. This is a plain [Row] with
 * `fillMaxWidth()` and each chip at `weight(1f)` instead — a standard,
 * bounded layout (the Row is measured with finite width by its parent),
 * so the five actions always exactly fill the available width with no
 * scrolling, no overflow, and no `LazyRow` at all.
 */
@Composable
fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onActionClick: (ManagerQuickAction) -> Unit = {},
) {
    // Spacing-cleanup pass: the standalone "دسترسی سریع" heading is gone,
    // along with the title-to-content gap that existed only for it. The
    // action row is the whole of this composable now.
    // Spacing refinement pass: tightened from SpaceSM (8dp) to SpaceXS
    // (4dp, an existing token) — matches the Dashboard's other
    // outer-spacing gaps, a thin separation between the 5 action chips
    // rather than a wider gap. Chip sizes/content/padding unchanged.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
    ) {
        sampleQuickActions.forEach { action ->
            QuickActionChip(
                action = action,
                onClick = { onActionClick(action.type) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionChip(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Pixel-perfect pass: the reference gives "نوبت جدید" a solid, fully
    // saturated gold fill — matching the same Gold-fill/BaseDeep-content
    // CTA pattern already used by the Hero banner button and the AI
    // Insight CTA — not a faint tint. Every other chip stays a plain,
    // subordinate dark/teal panel.
    val fill = if (action.isPrimary) ManagerColors.Gold else ManagerColors.BaseSecondary.copy(alpha = 0.35f)
    val contentTint = if (action.isPrimary) ManagerColors.BaseDeep else ManagerColors.Turquoise
    val labelColor = if (action.isPrimary) ManagerColors.BaseDeep else ManagerColors.TextPrimary

    Column(
        modifier = modifier
            // Identical-sizing fix: a fixed height (not a minimum) so all
            // five chips are exactly the same size regardless of label
            // length/wrapping - see the Text below's matching maxLines=2.
            .height(76.dp)
            .shadow(
                elevation = 4.dp,
                shape = RojanShapes.Small,
                // The one chip the reference gives a soft gold emphasis to
                // — every other chip keeps a plain neutral shadow, so gold
                // stays a single, contained accent rather than a repeated
                // treatment.
                ambientColor = if (action.isPrimary) ManagerColors.Gold.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.22f),
                spotColor = if (action.isPrimary) ManagerColors.Gold.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.22f),
            )
            .background(fill, RojanShapes.Small)
            .border(
                1.dp,
                if (action.isPrimary) ManagerColors.GoldLight.copy(alpha = 0.5f) else ManagerColors.Turquoise.copy(alpha = 0.14f),
                RojanShapes.Small,
            )
            .rojanPressable(onClick = onClick)
            .padding(RojanDimens.SpaceSM),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Centers the icon+text group within the now-fixed height, so a
        // one-line label and a two-line label both sit visually balanced
        // instead of top-packed with leftover space at the bottom.
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS, Alignment.CenterVertically),
    ) {
        RojanIconContainer(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = contentTint,
            size = RojanIconSize.Large,
        )
        Text(
            text = action.label,
            style = RojanTypography.Caption,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
