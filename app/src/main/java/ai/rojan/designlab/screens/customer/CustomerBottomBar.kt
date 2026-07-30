package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/** Fake, local-only tab identifiers — no navigation graph change, purely this bar's own active-state tracking. */
enum class CustomerHomeTab { HOME, SEARCH, BOOKINGS, FAVORITES, PROFILE }

private data class TabItem(
    val tab: CustomerHomeTab,
    val icon: ImageVector,
    val label: String,
)

private val tabs = listOf(
    TabItem(CustomerHomeTab.PROFILE, Icons.Filled.Person, "پروفایل"),
    TabItem(CustomerHomeTab.FAVORITES, Icons.Filled.Favorite, "علاقه‌مندی‌ها"),
    TabItem(CustomerHomeTab.HOME, Icons.Filled.Home, "خانه"),
    TabItem(CustomerHomeTab.BOOKINGS, Icons.Filled.CalendarMonth, "نوبت‌ها"),
    TabItem(CustomerHomeTab.SEARCH, Icons.Filled.Search, "جستجو"),
)

/**
 * Customer Home bottom navigation — Home Visual Language Unification.
 *
 * Same 5 tabs, same active-state tracking, same "no real navigation
 * wiring here" scope as before — dark glass bar per the approved
 * reference, with the Home tab raised into a circular glowing button
 * (a size/shape treatment on an existing item, not a new tab or a new
 * interaction) when it's the active tab, matching the reference's
 * center Home button.
 */
@Composable
fun CustomerBottomBar(
    activeTab: CustomerHomeTab = CustomerHomeTab.HOME,
    onTabSelected: (CustomerHomeTab) -> Unit = {},
) {
    HomeGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RojanDimens.SpaceSM, horizontal = RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { item ->
                val isActive = item.tab == activeTab
                val interactionSource = remember { MutableInteractionSource() }

                if (item.tab == CustomerHomeTab.HOME) {
                    Column(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { onTabSelected(item.tab) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            RojanAmbientGlow(
                                modifier = Modifier.size(72.dp),
                                color = HomeColors.Glow,
                                alpha = 0.55f,
                            )
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(HomeColors.Glow, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                RojanIconContainer(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = HomeColors.TextPrimary,
                                    size = RojanIconSize.Medium,
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            style = RojanTypography.Caption.rojanPressedShadow(interactionSource),
                            color = HomeColors.Glow,
                        )
                    }
                } else {
                    val tint = if (isActive) HomeColors.Glow else HomeColors.TextSecondary

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
}
