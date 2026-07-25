package ai.rojan.designlab.ui.components.interaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

import ai.rojan.designlab.ui.theme.RojanShadows


/**
 * ROJAN AI Design System
 *
 * Shared touch feedback for all interactive components:
 *
 * - Buttons
 * - Cards
 * - Icons
 * - Navigation items
 * - Clickable rows
 *
 * Press animation:
 * 1.00f -> 1.06f
 *
 * Duration:
 * 150ms
 */
fun Modifier.rojanPressable(
    onClick: () -> Unit,
    scaleTarget: Float = 1.06f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {

    val source =
        interactionSource ?: remember {
            MutableInteractionSource()
        }

    val isPressed by
    source.collectIsPressedAsState()


    val scale by animateFloatAsState(
        targetValue =
            if (isPressed) scaleTarget else 1f,

        animationSpec =
            tween(
                durationMillis = 150
            ),

        label =
            "rojan_pressable_scale"
    )


    this
        .scale(scale)
        .clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
}

/**
 * Soft press shadow for text — the [rojanPressable]/pressed-state
 * counterpart for [TextStyle]. Pairs with the same [MutableInteractionSource]
 * driving [rojanPressable] on the enclosing container (or a hoisted source
 * shared with a plain `Modifier.clickable`), so any text inside a pressable
 * component gets a soft elevation only while pressed — reading the identical
 * press signal rather than a second, independent press-tracking
 * implementation. Same 150ms timing as [rojanPressable]'s scale animation;
 * the shadow is fully removed as soon as the press is released.
 */
@Composable
fun TextStyle.rojanPressedShadow(interactionSource: InteractionSource): TextStyle {
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.45f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "rojan_text_press_shadow",
    )

    if (shadowAlpha <= 0f) return this

    return this.copy(
        shadow = Shadow(
            color = RojanShadows.PremiumShadow.copy(alpha = shadowAlpha),
            offset = Offset(0f, 3f),
            blurRadius = 10f,
        )
    )
}