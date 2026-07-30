package ai.rojan.designlab.screens.customer.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN Customer glass text field — replaces the raw Material
 * `OutlinedTextField` (default grey outline/container) on [AuthScreen]
 * with the same translucent-glass language as [CustomerGlassSurface],
 * scaled down for an inset form control rather than a floating card:
 * one soft contact shadow (not the panel's full dual-shadow — glass
 * nested inside glass reads as muddy if both layers fight for depth),
 * a quiet Rose Gold/white edge unfocused, and an animated brighter
 * Rose Gold border + blurred ambient halo behind the field while
 * focused — the "elegant focus glow."
 */
@Composable
fun CustomerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusGlow by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(220),
        label = "customerTextFieldFocusGlow",
    )
    val shape = RojanShapes.Small

    Box(modifier = modifier.fillMaxWidth()) {
        if (focusGlow > 0f) {
            // `BlurredEdgeTreatment.Unbounded` lets this glow bleed
            // visibly *past* the field's own bounds instead of clipping
            // at them — a real halo, not just a stronger fill under the
            // same silhouette.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(26.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CustomerColors.RoseGold.copy(alpha = 0.65f * focusGlow),
                                CustomerColors.BlushPink.copy(alpha = 0.45f * focusGlow),
                            ),
                        ),
                        shape = shape,
                    ),
            )
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            CustomerColors.BlushPink.copy(alpha = 0.38f),
                        ),
                    ),
                    shape = shape,
                )
                .border(
                    width = if (isFocused) 2.dp else 1.4.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            colors = listOf(
                                CustomerColors.RoseGold.copy(alpha = 1f),
                                CustomerColors.RoseGold.copy(alpha = 0.70f),
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                CustomerColors.RoseGold.copy(alpha = 0.55f),
                                CustomerColors.RoseGold.copy(alpha = 0.28f),
                            ),
                        )
                    },
                    shape = shape,
                ),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = CustomerColors.RoseGold,
                focusedLabelColor = CustomerColors.RoseGold,
                unfocusedLabelColor = CustomerColors.TextSecondary,
                focusedPlaceholderColor = CustomerColors.TextSecondary.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = CustomerColors.TextSecondary.copy(alpha = 0.6f),
                focusedTextColor = CustomerColors.TextPrimary,
                unfocusedTextColor = CustomerColors.TextPrimary,
            ),
        )
    }
}
