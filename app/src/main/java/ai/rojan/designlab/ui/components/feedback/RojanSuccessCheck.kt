package ai.rojan.designlab.ui.components.feedback

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanWarmWhite

/**
 * ROJAN Success feedback — Final Premium Polish, Phase 1's "Success
 * animations" primitive. Genuinely new: no success/checkmark component
 * existed anywhere in the design system before this (confirmed via a
 * full-tree audit) — [RojanStatusOnline] (the app's one green token)
 * existed but had only ever been used for static inline "online now"
 * text, never an animated confirmation moment.
 *
 * Plays once whenever [visible] turns `true`: the green disc scales in
 * first (a soft overshoot spring, not a linear ease), then the
 * checkmark glyph follows a beat behind it — reads as "confirmed, then
 * acknowledged" rather than both elements popping in flatly together.
 * Reusable anywhere a confirmation moment needs a premium visual beat —
 * e.g. Booking Confirmation's own confirm action, Booking Success.
 */
@Composable
fun RojanSuccessCheckmark(
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    var ringVisible by remember { mutableStateOf(false) }
    var checkVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            ringVisible = true
            delay(120)
            checkVisible = true
        } else {
            checkVisible = false
            ringVisible = false
        }
    }

    val ringScale by animateFloatAsState(
        targetValue = if (ringVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "rojan_success_ring_scale",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "rojan_success_check_scale",
    )

    if (ringScale <= 0f) return

    Box(
        modifier = modifier
            .size(size)
            .scale(ringScale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(RojanStatusOnline, RojanStatusOnline.copy(alpha = 0.82f)),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checkScale > 0f) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = RojanWarmWhite,
                modifier = Modifier
                    .size(size * 0.5f)
                    .scale(checkScale),
            )
        }
    }
}
