package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — confirmation dialog.
 *
 * The quiet-luxury replacement for the raw Material3 `AlertDialog` (light
 * surface, default type, LTR buttons) currently used for destructive
 * confirmations. A flat dark-navy card on the scrim: hairline border, no
 * glass, no glow. RTL button order — the "cancel / dismiss" text action on
 * the right (leading), the primary action on the left.
 *
 * Presentation only — the caller owns the [showing] boolean and both
 * callbacks. Touches no ViewModel, repository, navigation route, or API.
 * ========================================================================== */

/** Opaque navy card so it reads clearly against the dark dialog scrim. */
private val DialogSurface = HomeColors.NavyBase

@Composable
fun CustomerConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "انصراف",
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(CustomerCardShape)
                .background(DialogSurface)
                .border(1.dp, CustomerHairline, CustomerCardShape)
                .padding(RojanDimens.SpaceLG),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = RojanTypography.CardTitle,
                    color = HomeColors.TextPrimary,
                )
                Spacer(Modifier.height(RojanDimens.SpaceSM))
                Text(
                    message,
                    style = RojanTypography.Body,
                    color = HomeColors.TextSecondary,
                )
                Spacer(Modifier.height(RojanDimens.SpaceLG))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = RojanDimens.MinTouchTarget)
                            .clip(CustomerCardShape)
                            .rojanPressable(onClick = onDismiss, role = Role.Button),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dismissLabel, style = RojanTypography.Button, color = HomeColors.TextSecondary)
                    }
                    RefPrimaryButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
