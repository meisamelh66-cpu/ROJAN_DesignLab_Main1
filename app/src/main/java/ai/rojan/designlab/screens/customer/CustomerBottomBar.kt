package ai.rojan.designlab.screens.customer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/** Fake, local-only tab identifiers — no navigation graph change, purely this bar's own active-state tracking. */
enum class CustomerHomeTab { HOME, BOOKINGS, FAVORITES, PROFILE }

/**
 * @param assetRes Asset Readiness: target is one of
 * [ai.rojan.designlab.ui.assets.RojanAssetNames]'s `NAV_*` constants.
 * `null` today — falls back to [icon]. Active/inactive tinting is
 * preserved for the real asset too via [ColorFilter.tint], so a single
 * neutral-colored icon file works for both states without needing
 * separate active/inactive asset variants.
 */
private data class TabItem(
    val tab: CustomerHomeTab,
    val icon: ImageVector,
    val label: String,
    @DrawableRes val assetRes: Int? = null,
)

private val tabs = listOf(
    TabItem(CustomerHomeTab.HOME, Icons.Filled.Home, "خانه", R.drawable.ic_nav_home),
    TabItem(CustomerHomeTab.BOOKINGS, Icons.Filled.CalendarMonth, "نوبت‌ها", R.drawable.ic_nav_booking),
    TabItem(CustomerHomeTab.FAVORITES, Icons.Filled.Favorite, "علاقه‌مندی‌ها", R.drawable.ic_nav_favorite),
    TabItem(CustomerHomeTab.PROFILE, Icons.Filled.Person, "پروفایل", R.drawable.ic_nav_profile),
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

                Column(
                    modifier = Modifier.clickable { onTabSelected(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (item.assetRes != null) {
                        Image(
                            painter = painterResource(id = item.assetRes),
                            contentDescription = item.label,
                            colorFilter = ColorFilter.tint(tint),
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        RojanIconContainer(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            size = RojanIconSize.Medium,
                        )
                    }
                    Text(
                        text = item.label,
                        style = RojanTypography.Caption,
                        color = tint,
                    )
                }
            }
        }
    }
}
