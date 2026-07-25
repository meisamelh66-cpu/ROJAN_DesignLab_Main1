package ai.rojan.designlab.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGradients
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN AI primary CTA button — corrected per the PremiumButton
 * Correction Phase v1.0.
 *
 * Root cause of the prior version's problems: it never used the
 * Design Token Specification at all — [RojanDimens.ButtonWidth]/
 * [ButtonHeight], [RojanShapes.PremiumButton] (a proper pill shape), and
 * [RojanGradients.PremiumButton] (the brand purple→magenta gradient
 * already defined for exactly this button) all already existed in the
 * codebase and were simply never wired in. The previous version instead
 * hardcoded a 240×240dp *circle* with a non-brand pink/magenta gradient
 * (`#FF6FAE`/`#FF1B7A`) and a raw 35dp shadow — none of which came from
 * the token system.
 *
 * Fixed size: [RojanDimens.ButtonWidth] × [RojanDimens.ButtonHeight]
 * (240dp × 64dp) — a pill, not a circle. This is what actually fits
 * [ai.rojan.designlab.components.hero.HeroBookingCard]'s fixed
 * [RojanDimens.HeroHeight] (360dp) budget: title + subtitle + spacers +
 * this button now totals ~228dp, comfortably under the 360dp budget,
 * versus the previous version's ~404dp (a confirmed ~44dp overflow).
 *
 * Global Design System Refinement (Phase 1): explicit soft shadow color
 * (via [RojanShadows.PremiumShadow], same token now shared with
 * [ai.rojan.designlab.ui.components.glass.GlassSurface]'s shadow) added
 * — "Softer premium shadow, better visual depth" — size/gradient/radius/
 * position all untouched, per that same requirement's explicit "Keep"
 * list.
 *
 * UI Consolidation Sprint v2.0, item 8 (Button System): [enabled]/
 * [loading] added - previously this component had neither state at
 * all, despite being the app's primary CTA. Disabled: 50% opacity on
 * the whole button, no shadow (a disabled control shouldn't read as
 * "elevated/interactive"), click blocked. Loading: a small spinner
 * replaces the label, click blocked - same visual weight/position as
 * the text it replaces, so the button doesn't jump size when toggling.
 * Both states are mutually exclusive in practice (a loading button is
 * implicitly non-interactive) but modeled as two independent booleans
 * rather than a combined enum, since a caller may reasonably want
 * `loading=true` while still computing `enabled` from unrelated form
 * validity.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(
        width = RojanDimens.ButtonWidth,
        height = RojanDimens.ButtonHeight
    ),
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
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
                shape = RojanShapes.PremiumButton
            )
            .alpha(if (isInteractive) 1f else 0.5f)
            .let {
                if (isInteractive) {
                    it.rojanPressable(onClick = onClick, interactionSource = interactionSource)
                } else {
                    it
                }
            },

        contentAlignment = Alignment.Center
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
