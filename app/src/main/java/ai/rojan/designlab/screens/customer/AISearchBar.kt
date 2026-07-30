package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home AI search bar — Design Board v1.0, Section 1 (AI Search
 * layer): "fast discovery assistant."
 *
 * Sits on [GlassSurface], not directly on the dark canvas — per the
 * two-surface text model, its label text uses the *original* light-system
 * token ([RojanTextSecondary]), not an on-dark token, since the local
 * surface under it is glass, regardless of the canvas around it.
 *
 * Smart Search area, Phase 2: the trailing icon is now a filter glyph
 * (not the previous AI sparkle) — search on one side, filter on the
 * opposite side, per this phase's explicit "Search Bar" spec. Kept
 * deliberately subtle (same [RojanTextSecondary] tint as the search icon)
 * so this doesn't visually compete with the Hero Booking Area immediately
 * below it, per the Design Board's original rule.
 *
 * [RojanShapes.Small] (a tighter radius than the Hero Card's
 * [RojanShapes.GlassCard]) is used here intentionally — a search bar
 * reads as an input control, not a hero-level surface, and should look
 * the part.
 *
 * Home Screen Production Pass, Task 6: vertical padding reduced from
 * [RojanDimens.SpaceMD] to [RojanDimens.SpaceSM] (horizontal unchanged)
 * — this bar's row was the only one of Home's glass surfaces using the
 * larger 16dp padding on every edge; every card elsewhere on this screen
 * uses 8dp. Both icons are now [RojanIconSize.Medium] (20dp, matching
 * the search icon's pre-existing size) instead of the sparkle's previous
 * bespoke 18dp, so the two icons read as the same visual weight.
 *
 * Home Premium Polish Phase: border softened via [GlassSurface]'s own
 * exposed `borderAlpha`/`borderSecondaryAlpha` params (0.18f/0.08f ->
 * 0.10f/0.04f) rather than a new border mechanism, and the placeholder
 * now uses [RojanHintText] — an existing token whose own doc comment
 * already names "search bar placeholder" as its intended use but which
 * no call site had actually wired in until now — instead of the more
 * general [RojanTextSecondary].
 *
 * 3D Glassmorphism Depth Pass: a blurred [RojanRose] wash now sits behind
 * the bar (background ambient glow, matching this section's established
 * rose accent — see [SearchModeTabs]'s selected pill), and the tap target
 * switched from a plain `Modifier.clickable` to [rojanPressable] (scale
 * down + smooth return on touch), sharing the same [interactionSource]
 * [rojanPressedShadow] already reads so the two stay in sync.
 */
@Composable
fun AISearchBar(
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(visible = visible, delayMillis = 60),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RojanDimens.MinTouchTarget)
                .blur(16.dp)
                .background(HomeColors.Glow.copy(alpha = 0.20f), RojanShapes.Small),
        )

        HomeGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .rojanPressable(onClick = onClick, interactionSource = interactionSource),
            shape = RojanShapes.Small,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = RojanDimens.MinTouchTarget)
                    .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            RojanIconContainer(
    imageVector = Icons.Filled.Search,
    contentDescription = null,
    tint = HomeColors.TextSecondary,
    size = RojanIconSize.Medium,
)

            Text(
                text = "جستجوی سالن، خدمات یا متخصص...",
                style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                color = HomeColors.TextMuted,
                modifier = Modifier.weight(1f),
            )

            RojanIconContainer(
    imageVector = Icons.Filled.FilterList,
    contentDescription = "فیلتر",
    tint = HomeColors.TextSecondary,
    size = RojanIconSize.Medium,
)
            }
        }
    }
}
