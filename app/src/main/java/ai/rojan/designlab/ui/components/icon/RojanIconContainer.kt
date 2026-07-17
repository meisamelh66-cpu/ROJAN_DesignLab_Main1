package ai.rojan.designlab.ui.components.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanTextPrimary

/**
 * Design System Refinement Phase 2 — Icon System. The single shared
 * primitive every icon in the app should render through, replacing 75
 * previously-independent raw `Icon(...)` call sites (each with its own
 * inline size/tint choice, no shared token behind any of them).
 *
 * Deliberately narrow scope: this wraps *rendering* concerns (size,
 * tint, optional glow, optional glass background, accessibility) only —
 * it never decides which icon to show or what it means, so it composes
 * cleanly into any existing card/row/button without coupling to
 * business logic, per "Do NOT touch ViewModels or domain logic."
 *
 * [showGlow]/[showGlassBackground] default to `false` — every existing
 * call site migrated onto this component keeps its current plain
 * appearance unless a screen explicitly opts into the premium treatment,
 * consistent with "Avoid... per-screen customization" while still
 * making the option available from one real, shared implementation
 * rather than each screen inventing its own glow effect.
 */
enum class RojanIconSize(val dp: Dp) {
    Small(RojanDimens.IconSizeSmall),
    Medium(RojanDimens.IconSizeMedium),
    Large(RojanDimens.IconSizeLarge),
    XLarge(RojanDimens.IconSizeXLarge),
}

/**
 * Design System Refinement Phase 2.5: [sizeOverride] added. Migrating
 * existing `Icon()` call sites onto this component must never change
 * their visual size ("Do NOT redesign UI") — many existing sizes
 * (18dp/22dp/24dp/etc.) don't exactly match the 4 tiers in [RojanIconSize]
 * (which were chosen from the *most common* values, not every value).
 * When set, [sizeOverride] takes priority over [size], preserving the
 * exact pre-migration dimension while the call site still gains this
 * component's shared structure (tint handling, accessibility, and the
 * glow/glass hooks available if a screen opts in later).
 */
@Composable
fun RojanIconContainer(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: RojanIconSize = RojanIconSize.Medium,
    sizeOverride: Dp? = null,
    tint: Color = RojanTextPrimary,
    showGlow: Boolean = false,
    showGlassBackground: Boolean = false,
) {
    val resolvedSize = sizeOverride ?: size.dp
    val glassPadding = resolvedSize * 0.5f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size(resolvedSize * 1.8f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0f)),
                        ),
                        shape = CircleShape,
                    )
            )
        }

        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .let {
                    if (showGlassBackground) {
                        it
                            .shadow(
                                elevation = RojanShadows.SoftElevation / 3,
                                shape = CircleShape,
                                ambientColor = RojanShadows.PremiumShadow.copy(alpha = 0.15f),
                                spotColor = RojanShadows.PremiumShadow.copy(alpha = 0.15f),
                            )
                            .background(RojanGlassWhite.copy(alpha = 0.20f), CircleShape)
                            .padding(glassPadding)
                    } else {
                        it
                    }
                }
                .size(resolvedSize),
        )
    }
}
