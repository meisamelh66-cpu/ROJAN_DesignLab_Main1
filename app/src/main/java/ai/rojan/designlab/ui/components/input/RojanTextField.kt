package ai.rojan.designlab.ui.components.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.motion.RojanMotion
import ai.rojan.designlab.ui.motion.rememberReducedMotion
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN shared glass text field (UI Polish Sprint 3, Task 4).
 *
 * One palette-driven glass input for every app, replacing the per-screen
 * hand-rolled fields (Manager's raw `OutlinedTextField`, Reception's own,
 * `CustomerTextField`/`HomeTextField`). Same visual language as
 * `HomeTextField` — glass fill, animated focus glow-border — but bound to
 * [LocalRojanPalette] instead of a hard-coded colour family, so it wears
 * each app's identity automatically.
 *
 * Accessibility (Task 4):
 * - `heightIn(min = 56.dp)` — comfortably above the 48dp touch minimum;
 * - real `label` + `isError` are passed to the Material field, so
 *   TalkBack announces the label and the error;
 * - `errorText` is also exposed via `semantics { error(...) }` and shown
 *   inline **beneath the field** (not only summarised elsewhere);
 * - `helperText` for non-error guidance.
 *
 * RTL: the text style is direction-resolved per keystroke via
 * [withDirectionFor], and label/error/helper strings run through the
 * app's [Text] (first-strong-character direction). No layout mirroring
 * needed — a single-column field reads the same both ways.
 *
 * Reduced motion: the focus glow snaps instead of fading.
 */
@Composable
fun RojanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    helperText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val palette = LocalRojanPalette.current
    val reduceMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusGlowRaw = if (isFocused) 1f else 0f
    val focusGlow by animateFloatAsState(
        targetValue = focusGlowRaw,
        animationSpec = tween(if (reduceMotion) 0 else RojanMotion.Quick),
        label = "rojan_text_field_focus",
    )

    val shape = RojanShapes.Small
    val borderColor: Color = when {
        isError -> RojanErrorText
        isFocused -> palette.textAccent
        else -> palette.textSecondary.copy(alpha = 0.45f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(
                    color = Color.White.copy(alpha = 0.10f + 0.02f * focusGlow),
                    shape = shape,
                )
                .border(
                    width = if (isFocused || isError) 2.dp else 1.4.dp,
                    color = borderColor.copy(alpha = if (isFocused || isError) 1f else 0.6f),
                    shape = shape,
                )
                .then(
                    if (isError && errorText != null) {
                        Modifier.semantics { error(errorText) }
                    } else {
                        Modifier
                    },
                ),
            enabled = enabled,
            isError = isError,
            singleLine = singleLine,
            textStyle = LocalTextStyle.current.withDirectionFor(value),
            label = label?.let { { Text(it, style = RojanTypography.Caption) } },
            placeholder = placeholder?.let { { Text(it, style = RojanTypography.Body, color = palette.textSecondary.copy(alpha = 0.7f)) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(it, contentDescription = null, tint = palette.textSecondary)
                }
            },
            trailingIcon = trailing,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = palette.textAccent,
                errorCursorColor = RojanErrorText,
                focusedLabelColor = palette.textAccent,
                unfocusedLabelColor = palette.textSecondary,
                errorLabelColor = RojanErrorText,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                disabledTextColor = palette.textSecondary,
                errorTextColor = palette.textPrimary,
            ),
        )

        val supporting = when {
            isError && !errorText.isNullOrBlank() -> errorText to RojanErrorText
            !helperText.isNullOrBlank() -> helperText to palette.textSecondary
            else -> null
        }
        supporting?.let { (message, color) ->
            Text(
                text = message,
                style = RojanTypography.Caption,
                color = color,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS, start = RojanDimens.SpaceXS),
            )
        }
    }
}
