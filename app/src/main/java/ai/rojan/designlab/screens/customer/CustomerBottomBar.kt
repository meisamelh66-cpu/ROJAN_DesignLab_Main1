package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPremiumBorderRoseGold

/** Fake, local-only tab identifiers — no navigation graph change, purely this bar's own active-state tracking. */
enum class CustomerHomeTab { HOME, SEARCH, BOOKINGS, FAVORITES, PROFILE }

// TalkBack state announcements for each tab (Persian-first).
private const val TAB_STATE_ACTIVE = "فعال"
private const val TAB_STATE_INACTIVE = "غیرفعال"

// Quiet-luxury reference tokens (screen-local; match REFERENCE-SPEC-salon-detail
// / -customer-home — not promoted to the design system until the app-wide phase).
private val NavAccent = RojanPremiumBorderRoseGold          // #E0A67A — active only
private val NavHairline = Color.White.copy(alpha = 0.09f)

private data class CustomerNavTab(
    val tab: CustomerHomeTab,
    val icon: ImageVector,
    val label: String,
)

// Order unchanged from the previous implementation (Home stays centred), so
// nothing users have learned about tab positions moves.
private val customerNavTabs = listOf(
    CustomerNavTab(CustomerHomeTab.PROFILE, Icons.Outlined.Person, "پروفایل"),
    CustomerNavTab(CustomerHomeTab.FAVORITES, Icons.Outlined.FavoriteBorder, "علاقه‌ها"),
    CustomerNavTab(CustomerHomeTab.HOME, Icons.Outlined.Home, "خانه"),
    CustomerNavTab(CustomerHomeTab.BOOKINGS, Icons.Outlined.CalendarMonth, "نوبت‌ها"),
    CustomerNavTab(CustomerHomeTab.SEARCH, Icons.Outlined.Search, "جستجو"),
)

/**
 * Customer bottom navigation — Quiet Luxury / Dark Editorial pass.
 *
 * The single visual implementation, shared by [CustomerDashboardScreen]
 * (Home) and [CustomerHomeScreen] (Explore). Same 5 destinations, same
 * [CustomerHomeTab] active-state tracking, same "no navigation wiring here —
 * this bar only raises callbacks" scope as before; the public signature is
 * byte-identical and both call sites are unchanged in behaviour.
 *
 * Replaces the previous treatment — a protruding 64dp filled Home disc with
 * a rotating metallic ring + [ai.rojan.designlab.ui.components.effects.RojanAmbientGlow],
 * 20dp filled glyphs for the other four, wrapped in a 78%-width
 * [ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface] pill with
 * a metallic border + corner sparkles — with a flat bar: full width, a 1px
 * top hairline, five equal `weight(1f)` slots, 24dp **outlined** icons on one
 * baseline, active = rose-gold `#E0A67A` + a 3dp dot, inactive = muted. No
 * glass, no glow, no sparkle, no disc, no gradient, no oversized container.
 *
 * Each slot is a ≥ 48dp [selectable] target inside one [selectableGroup] with
 * a Persian `stateDescription`, so TalkBack still announces "tab N of 5" and
 * active/inactive — unchanged from the previous bar.
 */
@Composable
fun CustomerBottomBar(
    modifier: Modifier = Modifier,
    activeTab: CustomerHomeTab = CustomerHomeTab.HOME,
    onTabSelected: (CustomerHomeTab) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NavHairline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Opaque so scrolling content stays behind it and never shows
                // through the icon row; it sits on ~NavyBase already (the
                // ground gradient's bottom), so only the hairline delineates it.
                .background(HomeColors.NavyBase)
                .navigationBarsPadding()
                .padding(vertical = RojanDimens.SpaceSM)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            customerNavTabs.forEach { item ->
                val active = item.tab == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = RojanDimens.MinTouchTarget)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onTabSelected(item.tab) },
                        )
                        .semantics {
                            stateDescription = if (active) TAB_STATE_ACTIVE else TAB_STATE_INACTIVE
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (active) NavAccent else HomeColors.TextMuted,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (active) NavAccent else Color.Transparent),
                    )
                }
            }
        }
    }
}
