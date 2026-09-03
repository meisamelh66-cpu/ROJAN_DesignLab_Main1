package ai.rojan.designlab.ui.components.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN shared glass search field (UI Polish Sprint 3, Task 3).
 *
 * One search input for every app / screen, palette-driven. Two modes:
 *
 * - **Editable** (default): a real inline search box — leading search
 *   glyph, RTL-resolved text, an explicit **clear (×)** button once
 *   there's text, and an optional [trailing] slot for a *distinct*
 *   secondary action (e.g. a filter sheet). The clear button and the
 *   filter action being visually separate fixes the "the filter icon
 *   just does the same thing as the bar" confusion.
 * - **Tap-to-navigate** ([readOnly] = true, [onClick] set): renders the
 *   same bar as a pressable display element that routes to a dedicated
 *   search screen — the pattern the Home `AISearchBar` uses.
 *
 * RTL: text direction is resolved per keystroke via [withDirectionFor];
 * the leading glyph / trailing slot arrangement reads correctly in both
 * directions without mirroring (icon leads, content follows). The bar is
 * `heightIn(min = 48.dp)`.
 */
@Composable
fun RojanSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "جستجو...",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalRojanPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    val barModifier = if (readOnly && onClick != null) {
        Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick, interactionSource = interactionSource)
    } else {
        Modifier.fillMaxWidth()
    }

    PremiumGlassSurface(modifier = modifier.then(barModifier), shape = RojanShapes.Small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RojanDimens.MinTouchTarget)
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(RojanDimens.IconSizeMedium),
            )

            Box(modifier = Modifier.weight(1f)) {
                if (readOnly) {
                    Text(
                        text = value.ifBlank { placeholder },
                        style = RojanTypography.Body,
                        color = if (value.isBlank()) palette.textSecondary.copy(alpha = 0.7f) else palette.textPrimary,
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = RojanTypography.Body
                            .copy(color = palette.textPrimary)
                            .withDirectionFor(value.ifBlank { placeholder }),
                        cursorBrush = SolidColor(palette.textAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = RojanTypography.Body,
                                    color = palette.textSecondary.copy(alpha = 0.7f),
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            if (!readOnly && value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(RojanDimens.MinTouchTarget)
                        .rojanPressable(onClick = { onValueChange("") }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "پاک کردن",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(RojanDimens.IconSizeMedium),
                    )
                }
            }

            trailing?.invoke()
        }
    }
}
