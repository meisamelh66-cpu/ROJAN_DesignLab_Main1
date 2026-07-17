package ai.rojan.designlab.ui.components.interaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale


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
): Modifier = composed {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by
    interactionSource.collectIsPressedAsState()


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
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}