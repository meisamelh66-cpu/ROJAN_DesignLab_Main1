package ai.rojan.designlab.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary

/**
 * ROJAN AI shared back-navigation control — Production Readiness Fix 2.
 *
 * Deliberately excluded from Splash and the Welcome root screen — those
 * have no previous navigation level to return to. Intended for role
 * dashboards, secondary screens, and future internal pages, per the
 * fix's own scope.
 *
 * Design System Phase 2.5 spacing audit: the repeated raw `44.dp` (used
 * twice in this same file) consolidated into [RojanDimens.BackButtonSize].
 *
 * UI Consolidation Sprint v2.0: size bumped 44dp -> 48dp (item 10,
 * "Minimum touch size: 48dp"), and [rojanPressable] applied (item 4) -
 * this is the single highest-frequency clickable in the app (24 real
 * call sites), so it's the highest-value place for the shared press
 * feedback to land.
 *
 * Global GlassBackButton Fix: `ic_nav_back.webp` (previously rendered here
 * via a raw `Image`) turned out to be the same non-functional flat
 * gradient-square placeholder already found and fixed in
 * [ai.rojan.designlab.screens.customer.CustomerBottomBar]'s nav icons and
 * [ai.rojan.designlab.screens.customer.PopularServices]'s service icons —
 * every one of this control's ~24 real call sites was showing a plain
 * colored square instead of a back arrow. Replaced with
 * [Icons.AutoMirrored.Filled.ArrowBack] — the same icon
 * [ai.rojan.designlab.screens.profile.ProfileScreen]'s own separate back
 * button already uses, and the semantically correct choice for a true
 * back-navigation control (unlike the fixed-direction "leads forward"
 * chevrons documented in [ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen]/
 * [ai.rojan.designlab.screens.search.SearchScreen], "back" is relative to
 * reading direction, so it's the one place AutoMirrored is the right
 * call). Glass container, size, position, and press animation are all
 * unchanged — only the icon itself changed.
 */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .size(RojanDimens.BackButtonSize)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Circle,
    ) {
        Box(
            modifier = Modifier.size(RojanDimens.BackButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "بازگشت",
                tint = RojanTextPrimary,
                size = RojanIconSize.Medium,
            )
        }
    }
}
