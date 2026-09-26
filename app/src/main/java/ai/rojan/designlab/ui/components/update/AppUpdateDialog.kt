package ai.rojan.designlab.ui.components.update

import ai.rojan.designlab.domain.repository.AppUpdateInfo
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanButtonStyle
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Android App Update Client, Phase 2B: the one, shared, cross-flavor update
 * prompt — built directly on the already-unified [PremiumGlassSurface]/
 * [PremiumButton]/[LocalRojanPalette] mechanics (Shared Premium Glass Design
 * System, Phases 1 and 3), never on [ai.rojan.designlab.screens.customer.components.CustomerConfirmDialog]
 * (a Customer-only bespoke component hardcoded to that app's own colors) —
 * this renders correctly themed for Manager/Customer/Reception purely
 * because each Activity already provides the right [LocalRojanPalette]
 * value, with zero per-app branching in this file.
 *
 * [onUpdateClick] receives the real, server-provided [AppUpdateInfo.downloadUrl]
 * unchanged — this composable never constructs or fires an `Intent` itself;
 * the actual `Intent.ACTION_VIEW` call lives in
 * [ai.rojan.designlab.ui.components.update.AppUpdateGate], mirroring the
 * existing, only precedent for this in the app
 * ([ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s address-click
 * handler) exactly.
 *
 * [mandatory] disables both the system back gesture and tap-outside-to-dismiss
 * (`DialogProperties`) and never renders the «بعداً» control at all — there
 * is no code path in this composable that can close a mandatory prompt
 * without the user tapping «به‌روزرسانی».
 */
@Composable
fun AppUpdateDialog(
    info: AppUpdateInfo,
    mandatory: Boolean,
    onUpdateClick: (downloadUrl: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalRojanPalette.current

    Dialog(
        onDismissRequest = { if (!mandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !mandatory,
            dismissOnClickOutside = !mandatory,
        ),
    ) {
        PremiumGlassSurface(
            modifier = Modifier.widthIn(max = 360.dp),
            shape = RojanShapes.GlassCard,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG)) {
                Text(
                    text = if (mandatory) "به‌روزرسانی ضروری است" else "نسخه جدید ROJAN منتشر شد",
                    style = RojanTypography.CardTitle,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(RojanDimens.SpaceSM))

                if (mandatory) {
                    Text(
                        text = "برای ادامه استفاده از ROJAN لازم است برنامه را به آخرین نسخه به‌روزرسانی کنید.",
                        style = RojanTypography.Body,
                        color = palette.textSecondary,
                    )
                } else {
                    Text(
                        text = "نسخه ${info.latestVersionName} در دسترس است.",
                        style = RojanTypography.Body,
                        color = palette.textSecondary,
                    )
                    val releaseNotes = info.releaseNotes?.trim()?.takeIf { it.isNotEmpty() }
                    if (releaseNotes != null) {
                        Spacer(Modifier.height(RojanDimens.SpaceXS))
                        Text(
                            text = releaseNotes,
                            style = RojanTypography.Body,
                            color = palette.textSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(RojanDimens.SpaceLG))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    if (!mandatory) {
                        PremiumButton(
                            text = "بعداً",
                            onClick = onDismiss,
                            style = RojanButtonStyle.Outline,
                            modifier = Modifier.weight(1f).height(RojanDimens.ButtonHeight),
                        )
                    }
                    PremiumButton(
                        text = "به‌روزرسانی",
                        onClick = { onUpdateClick(info.downloadUrl) },
                        modifier = Modifier.weight(1f).height(RojanDimens.ButtonHeight),
                    )
                }
            }
        }
    }
}
