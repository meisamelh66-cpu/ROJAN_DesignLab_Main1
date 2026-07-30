package ai.rojan.designlab.screens.customer.hometheme

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
 * Customer Home dark-glass text field — Home Visual Language Unification.
 *
 * Same structure as [ai.rojan.designlab.screens.customer.theme.CustomerTextField]
 * (focus glow, glass fill, gradient border), tuned for the dark canvas:
 * a faint white/purple glass fill instead of white/blush-pink, and a
 * purple glow border/cursor/label instead of Rose Gold. `CustomerTextField`
 * itself stays untouched — its only 2 call sites (`AuthScreen`,
 * `FirstTimeNameScreen`) both move onto this instead.
 */
@Composable
fun HomeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
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
        label = "homeTextFieldFocusGlow",
    )
    val shape = RojanShapes.Small

    Box(modifier = modifier.fillMaxWidth()) {
        if (focusGlow > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(26.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HomeColors.Glow.copy(alpha = 0.55f * focusGlow),
                                HomeColors.Primary.copy(alpha = 0.40f * focusGlow),
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
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.24f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            HomeColors.Primary.copy(alpha = 0.10f),
                        ),
                    ),
                    shape = shape,
                )
                .border(
                    width = if (isFocused) 2.dp else 1.4.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            colors = listOf(
                                HomeColors.Glow.copy(alpha = 1f),
                                HomeColors.Glow.copy(alpha = 0.70f),
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                HomeColors.Glow.copy(alpha = 0.45f),
                                HomeColors.Glow.copy(alpha = 0.20f),
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
            leadingIcon = leadingIcon,
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
                cursorColor = HomeColors.Glow,
                focusedLabelColor = HomeColors.Glow,
                unfocusedLabelColor = HomeColors.TextSecondary,
                focusedPlaceholderColor = HomeColors.TextMuted,
                unfocusedPlaceholderColor = HomeColors.TextMuted,
                focusedTextColor = HomeColors.TextPrimary,
                unfocusedTextColor = HomeColors.TextPrimary,
            ),
        )
    }
}
