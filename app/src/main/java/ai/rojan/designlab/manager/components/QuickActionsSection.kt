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

/** Static placeholder action — no navigation wired yet ("Do not connect routing yet"). */
private data class QuickAction(
    val icon: ImageVector,
    val label: String,
)

private val sampleQuickActions = listOf(
    QuickAction(Icons.Filled.CalendarMonth, "نوبت جدید"),
    QuickAction(Icons.Filled.PersonAdd, "مشتری جدید"),
    QuickAction(Icons.Filled.ContentCut, "خدمات"),
    QuickAction(Icons.Filled.Groups, "کارکنان"),
    QuickAction(Icons.Filled.Settings, "تنظیمات"),
)

/**
 * Manager App workspace — quick-actions row. Each chip is inert for now
 * (single [onActionClick] callback, no-op by default) since this pass
 * builds UI only, not routing.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerGlassSurface]/[ManagerIconContainer]) —
 * content/layout/navigation unchanged.
 */
@Composable
fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "دسترسی سریع",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        LazyRow(
            modifier = Modifier.padding(top = RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            items(sampleQuickActions) { action ->
                QuickActionChip(action = action, onClick = { onActionClick(action.label) })
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
