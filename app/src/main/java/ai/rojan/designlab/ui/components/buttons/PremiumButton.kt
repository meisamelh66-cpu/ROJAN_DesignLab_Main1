package ai.rojan.designlab.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanButtonStyle
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGradients
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN AI's single primary-CTA component — design-system refinement,
 * Phase 3 (Unified Premium Button System).
 *
 * Every app renders the same **behavior**: [RojanTypography.Button]
 * labels, [RojanShapes.PremiumButton] (`RojanRadius.Pill`) shape,
 * `Role.Button` + disabled/loading semantics, and [rojanPressable] press
 * feedback. The one thing that differs is [style] — the CTA **material** —
 * resolved from [LocalRojanPalette]'s [ai.rojan.designlab.ui.theme.RojanAppPalette.buttonStyle]
 * so no call site hardcodes an appearance:
 *
 * - [RojanButtonStyle.Gradient] — Customer's bold brand gradient fill.
 *   Byte-identical to this component's pre-unification body.
 * - [RojanButtonStyle.Glass] — [PremiumGlassSurface] + a low-alpha
 *   two-tone accent wash (`shadowSpot -> buttonAccent`). Manager's exact
 *   pre-unification `ManagerPrimaryButton` body; Reception's primary CTA
 *   also renders this way as of this pass (previously it had no per-app
 *   style and simply inherited Customer's Gradient — see
 *   [ai.rojan.designlab.ui.theme.ReceptionPalette]'s own doc comment).
 * - [RojanButtonStyle.Outline] — transparent fill, accent border + label
 *   only. Not the default for any app today; available for a screen that
 *   needs a lighter secondary action in the same shape/typography
 *   language, via `PremiumButton(..., style = RojanButtonStyle.Outline)`.
 *
 * [modifier]'s default (`RojanDimens.ButtonWidth` x `ButtonHeight`, a
 * fixed pill) is unchanged from before this pass and applies to every
 * style — every existing call site passes no `modifier` and keeps its
 * exact geometry; only the CTA's *material* changes per app.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(
        width = RojanDimens.ButtonWidth,
        height = RojanDimens.ButtonHeight,
    ),
    enabled: Boolean = true,
    loading: Boolean = false,
    style: RojanButtonStyle = LocalRojanPalette.current.buttonStyle,
) {
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }

    val semanticsModifier = Modifier.semantics {
        role = Role.Button
        if (!isInteractive) disabled()
        if (loading) stateDescription = "در حال پردازش"
    }

    when (style) {
        RojanButtonStyle.Gradient -> GradientPremiumButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            isInteractive = isInteractive,
            loading = loading,
            interactionSource = interactionSource,
            semanticsModifier = semanticsModifier,
        )

        RojanButtonStyle.Glass -> GlassPremiumButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            isInteractive = isInteractive,
            loading = loading,
            interactionSource = interactionSource,
            semanticsModifier = semanticsModifier,
        )

        RojanButtonStyle.Outline -> OutlinePremiumButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            isInteractive = isInteractive,
            loading = loading,
            interactionSource = interactionSource,
            semanticsModifier = semanticsModifier,
        )
    }
}

/** [RojanButtonStyle.Gradient] — unchanged from the pre-unification [PremiumButton] body. */
@Composable
private fun GradientPremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isInteractive: Boolean,
    loading: Boolean,
    interactionSource: MutableInteractionSource,
    semanticsModifier: Modifier,
) {
    Box(
        modifier = modifier
            .then(semanticsModifier)
            .let {
                if (isInteractive) {
                    it.shadow(
                        elevation = RojanShadows.PremiumElevation,
                        shape = RojanShapes.PremiumButton,
                        ambientColor = RojanShadows.PremiumShadow.copy(alpha = 0.25f),
                        spotColor = RojanShadows.PremiumShadow.copy(alpha = 0.25f),
                    )
                } else {
                    it
                }
            }
            .background(
                brush = RojanGradients.PremiumButton,
                shape = RojanShapes.PremiumButton,
            )
            .alpha(if (isInteractive) 1f else 0.5f)
            .let {
                if (isInteractive) {
                    it.rojanPressable(onClick = onClick, interactionSource = interactionSource)
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = RojanTextOnGlass,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = text,
                style = RojanTypography.Button.rojanPressedShadow(interactionSource),
                color = RojanTextOnGlass,
            )
        }
    }
}

/** [RojanButtonStyle.Glass] — unchanged from the pre-unification `ManagerPrimaryButton` body, palette-driven instead of hardcoded to `ManagerColors`. */
@Composable
private fun GlassPremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    isInteractive: Boolean,
    loading: Boolean,
    interactionSource: MutableInteractionSource,
    semanticsModifier: Modifier,
) {
    val palette = LocalRojanPalette.current

    PremiumGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .then(semanticsModifier)
            .let {
                if (isInteractive) it.rojanPressable(onClick = onClick, interactionSource = interactionSource) else it
            },
        shape = RojanShapes.PremiumButton,
        // A button reads as a more solid, filled action than a passive
        // glass card — double the passive-surface fill alpha, same as
        // ManagerPrimaryButton always used.
        fillAlpha = PremiumGlassTheme.FillAlpha * 2f,
        borderAlpha = PremiumGlassTheme.BorderAlpha,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(
                    if (isInteractive) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    palette.shadowSpot.copy(alpha = 0.20f),
                                    palette.buttonAccent.copy(alpha = 0.16f),
                                ),
                            ),
                            shape = RojanShapes.PremiumButton,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = RojanDimens.SpaceMD),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = palette.buttonAccent,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(
                    text = text,
                    style = RojanTypography.Button,
                    color = if (isInteractive) palette.buttonAccent else palette.textSecondary,
                )
            }
        }
    }
}

/**
 * [RojanButtonStyle.Outline] — transparent fill, [ai.rojan.designlab.ui.theme.RojanAppPalette.buttonAccent]
 * border + label. New this pass; not the default for any app, so it
 * carries no regression risk for an existing screen — opt in explicitly
 * with `style = RojanButtonStyle.Outline`.
 */
@Composable
private fun OutlinePremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isInteractive: Boolean,
    loading: Boolean,
    interactionSource: MutableInteractionSource,
    semanticsModifier: Modifier,
) {
    val palette = LocalRojanPalette.current
    val accent = if (isInteractive) palette.buttonAccent else palette.textSecondary

    Box(
        modifier = modifier
            .then(semanticsModifier)
            .border(width = 1.5.dp, color = accent, shape = RojanShapes.PremiumButton)
            .alpha(if (isInteractive) 1f else 0.5f)
            .let {
                if (isInteractive) it.rojanPressable(onClick = onClick, interactionSource = interactionSource) else it
            },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = accent,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = text,
                style = RojanTypography.Button,
                color = accent,
            )
        }
    }
}
