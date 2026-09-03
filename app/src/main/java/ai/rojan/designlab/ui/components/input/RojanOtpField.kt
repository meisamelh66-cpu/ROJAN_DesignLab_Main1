package ai.rojan.designlab.ui.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN segmented OTP input (UI Polish Sprint 3, Task 4).
 *
 * Replaces the raw `OutlinedTextField` + `placeholder = "------"` +
 * `KeyboardType.NumberPassword` (masked) pattern the OTP screens
 * hand-rolled. Improvements:
 * - **Unmasked** — the customer can see and verify the digits they typed
 *   (a masked OTP is a well-known usability failure);
 * - one glass cell per digit, the next empty cell highlighted with the
 *   palette accent — clear progress feedback;
 * - palette-driven, so it matches each app;
 * - `error(...)` semantics + an inline error line beneath;
 * - the whole control is one ≥56dp-tall focus target; `contentDescription`
 *   announces the code length for TalkBack.
 *
 * State is fully hoisted: [value] is the raw digit string (length 0..[length]).
 * The caller decides when it is "complete" (`value.length == length`) and
 * triggers verification — this component never auto-submits.
 *
 * RTL: the cells render left-to-right because an OTP is a numeric
 * sequence read LTR even inside Persian UI (same rule the app already
 * uses for phone numbers). The label/error text around it follow the
 * app's normal RTL text handling.
 */
@Composable
fun RojanOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val palette = LocalRojanPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val sanitized = value.filter { it.isDigit() }.take(length)

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = sanitized,
            onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(length)) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics {
                    contentDescription = "کد $length رقمی تایید"
                    if (isError && errorText != null) error(errorText)
                },
            decorationBox = { innerTextField ->
                // The real editable field carries the cursor / key handling;
                // it is kept in the layout at zero size so the segmented
                // cells below are the only thing seen.
                Box(modifier = Modifier.size(0.dp)) { innerTextField() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    repeat(length) { i ->
                        val char = sanitized.getOrNull(i)
                        val isActiveCell = isFocused && (i == sanitized.length ||
                            (sanitized.length == length && i == length - 1))
                        val cellBorder: Color = when {
                            isError -> RojanErrorText
                            isActiveCell -> palette.textAccent
                            else -> palette.textSecondary.copy(alpha = 0.45f)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 56.dp)
                                .background(Color.White.copy(alpha = 0.10f), RojanShapes.Small)
                                .border(
                                    width = if (isActiveCell || isError) 2.dp else 1.4.dp,
                                    color = cellBorder,
                                    shape = RojanShapes.Small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = char?.toString() ?: "",
                                style = RojanTypography.CardTitle.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            },
        )

        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = RojanTypography.Caption,
                color = RojanErrorText,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS, start = RojanDimens.SpaceXS),
            )
        }
    }
}
