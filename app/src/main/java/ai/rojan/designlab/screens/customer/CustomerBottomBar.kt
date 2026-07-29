package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/** Fake, local-only tab identifiers — no navigation graph change, purely this bar's own active-state tracking. */
enum class CustomerHomeTab { HOME, SEARCH, BOOKINGS, FAVORITES, PROFILE }

private data class TabItem(
    val tab: CustomerHomeTab,
    val icon: ImageVector,
    val label: String,
)

private val tabs = listOf(
    TabItem(CustomerHomeTab.HOME, Icons.Filled.Home, "خانه"),
    TabItem(CustomerHomeTab.SEARCH, Icons.Filled.Search, "جستجو"),
    TabItem(CustomerHomeTab.BOOKINGS, Icons.Filled.CalendarMonth, "نوبت‌ها"),
    TabItem(CustomerHomeTab.FAVORITES, Icons.Filled.Favorite, "علاقه‌مندی‌ها"),
    TabItem(CustomerHomeTab.PROFILE, Icons.Filled.Person, "پروفایل"),
)

/**
 * Customer Home bottom navigation — Design Board v1.0, Section 1 (Bottom
 * Navigation layer). Glass treatment (via [GlassSurface]) per the Design
 * Board's explicit requirement; compact, persistent, low visual weight
 * relative to the Hero Booking Area.
 *
 * Sits on glass, so per the two-surface text model, inactive-tab labels
 * use the original light-system [RojanTextSecondary] token; the active
 * tab is tinted with [RojanAIGlow] rather than the plain brand purple —
 * consistent with this being the one small AI-identity accent placed on
 * an otherwise-neutral navigation surface.
 *
 * @param activeTab which tab is currently selected. No real navigation
 * wiring here — this only tracks/displays active state, per this phase's
 * "do not change architecture" scope; wiring to the actual nav graph is
 * separate, later work.
 */
@Composable
fun CustomerBottomBar(
    activeTab: CustomerHomeTab = CustomerHomeTab.HOME,
    onTabSelected: (CustomerHomeTab) -> Unit = {},
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RojanDimens.SpaceSM, horizontal = RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { item ->
                val isActive = item.tab == activeTab
                val tint = if (isActive) RojanAIGlow else RojanTextSecondary
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = { onTabSelected(item.tab) },
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RojanIconContainer(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        size = RojanIconSize.Medium,
                    )
                    Text(
                        text = item.label,
                        style = RojanTypography.Caption.rojanPressedShadow(interactionSource),
                        color = tint,
                    )
                }
            }
        }
    }
}
