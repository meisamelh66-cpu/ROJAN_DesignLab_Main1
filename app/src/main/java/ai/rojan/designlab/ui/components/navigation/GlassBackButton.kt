package ai.rojan.designlab.ui.components.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN AI shared back-navigation control — Production Readiness Fix 2.
 *
 * A glass circle, consistent size/spacing, rendering
 * [ai.rojan.designlab.ui.assets.RojanAssetNames.NAV_BACK]
 * (`ic_nav_back`, a real placeholder asset already present — see the
 * Production Readiness Fixes v1 asset-naming pass).
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
            Image(
                painter = painterResource(id = R.drawable.ic_nav_back),
                contentDescription = "بازگشت",
                modifier = Modifier.size(RojanDimens.IconSizeMedium),
            )
        }
    }
}
