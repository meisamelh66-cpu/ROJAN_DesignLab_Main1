package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — the single flat outlined text field.
 *
 * Engineering Cleanup Phase 4 (P1). Extracted from three byte-identical
 * hand-rolled copies:
 *   • `SearchScreen.SearchField`      — flat surface + leading search icon
 *   • `SalonListScreen.SearchField`   — identical, different placeholder string
 *   • `AuthScreen.AuthField`          — same surface + a caption label above
 *
 * The visual (flat `CustomerSurfaceFill` box, 1px `CustomerHairline`,
 * `CustomerCardShape`, rose-gold cursor), the RTL heuristic
 * (`withDirectionFor` on the value, falling back to placeholder then label),
 * the single-line / keyboard / enabled / placeholder behaviour, and the
 * 48dp min touch target are all carried over exactly. No design change.
 *
 * Adds no design-system token and edits no other shared component.
 * ========================================================================== */

/**
 * [label] renders a caption above the field (AuthScreen style); omit it for a
 * bare field (search style). [leadingIcon] sits inside the box before the text
 * (search style). [placeholder] shows while [value] is empty.
 */
@Composable
fun CustomerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                label,
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(RojanDimens.SpaceXS))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RojanDimens.MinTouchTarget)
                .clip(CustomerCardShape)
                .background(CustomerSurfaceFill)
                .border(1.dp, CustomerHairline, CustomerCardShape)
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = HomeColors.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(RojanDimens.SpaceSM))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = RojanTypography.Body
                    .copy(color = HomeColors.TextPrimary)
                    .withDirectionFor(value.ifEmpty { placeholder ?: label ?: "" }),
                cursorBrush = SolidColor(CustomerAccent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = RojanTypography.Body, color = HomeColors.TextMuted)
                    }
                    inner()
                },
            )
        }
    }
}

/** Search-styled convenience: flat field with a leading search glyph. */
@Composable
fun CustomerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) = CustomerTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    placeholder = placeholder,
    leadingIcon = Icons.Outlined.Search,
)
