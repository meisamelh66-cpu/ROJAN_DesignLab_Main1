package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Booking Module Design System Migration. Same mapping rationale as
 * [BookingHeader]: `#8B5CF6` -> [RojanAIGlow], `#6E627A`/`#8B8195` close
 * enough to [RojanTextSecondary] to reuse directly, `#F5EFFF` close
 * enough to [RojanSoftLavender].
 */
/**
 * Booking Module Design System Migration. Same mapping rationale as
 * [BookingHeader]: `#8B5CF6` -> [RojanAIGlow], `#6E627A`/`#8B8195` close
 * enough to [RojanTextSecondary] to reuse directly, `#F5EFFF` close
 * enough to [RojanSoftLavender].
 *
 * Code Cleanup pass: this bar was fully inert (zero click handling at
 * all) - now genuinely functional. [selectedTab] lets the caller mark
 * which of the 3 items is current (defaults to Home, matching this
 * bar's only real usage today), rather than a hardcoded `selected = true`
 * on Home regardless of context.
 */
enum class BookingBottomTab { HOME, BOOKINGS, PROFILE }

@Composable
fun BookingBottomBar(
    selectedTab: BookingBottomTab = BookingBottomTab.HOME,
    onHomeClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .shadow(
                elevation = RojanShadows.PremiumElevation,
                shape = RojanShapes.GlassCard
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RojanGlassWhite.copy(alpha = 0.85f),
                        RojanSoftLavender.copy(alpha = 0.95f)
                    )
                ),
                RojanShapes.GlassCard
            )
            .padding(vertical = RojanDimens.SpaceMD),

        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomItem(
            icon = Icons.Default.Home,
            title = "خانه",
            selected = selectedTab == BookingBottomTab.HOME,
            onClick = onHomeClick,
        )
        BottomItem(
            icon = Icons.Default.CalendarMonth,
            title = "نوبت‌ها",
            selected = selectedTab == BookingBottomTab.BOOKINGS,
            onClick = onBookingsClick,
        )
        BottomItem(
            icon = Icons.Default.Person,
            title = "پروفایل",
            selected = selectedTab == BookingBottomTab.PROFILE,
            onClick = onProfileClick,
        )
    }
}

@Composable
private fun BottomItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {

        RojanIconContainer(
    imageVector = icon,
    contentDescription = title,
    tint = if (selected) RojanAIGlow else RojanTextSecondary,
    sizeOverride = 30.dp,
)

        Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))

        Text(
            text = title,
            style = RojanTypography.Caption,
            color = if (selected) RojanAIGlow else RojanTextSecondary
        )
    }
}
