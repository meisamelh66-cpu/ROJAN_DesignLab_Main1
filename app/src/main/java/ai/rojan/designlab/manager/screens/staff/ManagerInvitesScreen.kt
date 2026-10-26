package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus
import ai.rojan.designlab.manager.presentation.staff.ManagerInvitesUiState
import ai.rojan.designlab.manager.presentation.staff.ManagerInvitesViewModel
import ai.rojan.designlab.manager.presentation.staff.ManagerInvitesViewModelFactory
import ai.rojan.designlab.manager.presentation.staff.QrCodeState
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — P1-6, Manager Salon Invite Management: lets the
 * salon owner create, list, and revoke staff invites, and display each
 * pending invite's real QR code, via the real
 * `POST`/`GET`/`DELETE .../salons/{salonId}/invites` and
 * `GET .../invites/{inviteId}/qr-code` endpoints. Reached from
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffScreen]'s new
 * "دعوت پذیرش" entry point — shown unconditionally this pass (not gated
 * to owner-only client-side); a manager-role (non-owner) caller sees the
 * real backend `403` surface through the normal [ManagerInvitesContent]
 * `Error`/`mutationError` states, the same generic access-denied handling
 * every other screen in this codebase already uses.
 *
 * Every mutation (create/revoke) re-fetches the real list afterward rather
 * than locally guessing the result — see [ai.rojan.designlab.manager.presentation.staff.ManagerInvitesViewModel]'s
 * own doc comment. QR/revoke actions are only offered on a
 * [SalonInviteStatus.CREATED] row, matching the backend's own rule.
 *
 * The complementary accepting half of this flow lives entirely in the
 * `reception` flavor (P1-1) — untouched by this file.
 */
@Composable
fun ManagerInvitesScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel: ManagerInvitesViewModel = viewModel(
        factory = ManagerInvitesViewModelFactory(context.applicationContext),
    )

    ManagerInvitesContent(
        uiState = viewModel.state,
        onRetry = viewModel::retry,
        onCreateInvite = viewModel::createInvite,
        onRevokeInvite = viewModel::revokeInvite,
        onShowQrCode = viewModel::loadQrCode,
        onDismissQrCode = viewModel::dismissQrCode,
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@Composable
private fun ManagerInvitesContent(
    uiState: ManagerInvitesUiState,
    onRetry: () -> Unit,
    onCreateInvite: (SalonMemberRole) -> Unit,
    onRevokeInvite: (String) -> Unit,
    onShowQrCode: (String) -> Unit,
    onDismissQrCode: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "دعوت پذیرش",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (uiState) {
                is ManagerInvitesUiState.Loading -> item { ManagerInvitesLoading() }
                is ManagerInvitesUiState.Error ->
                    item { ManagerInvitesError(message = uiState.message, onRetry = onRetry) }
                is ManagerInvitesUiState.Ready -> {
                    item {
                        ManagerPrimaryButton(
                            text = "ایجاد دعوت جدید",
                            onClick = { showCreateDialog = true },
                            enabled = !uiState.isCreating,
                        )
                    }
                    if (uiState.mutationError != null) {
                        item {
                            Text(
                                text = uiState.mutationError,
                                style = RojanTypography.Caption,
                                color = RojanErrorText,
                            )
                        }
                    }
                    if (uiState.invites.isEmpty()) {
                        item { EmptyInvitesNotice() }
                    } else {
                        items(uiState.invites, key = { it.id }) { invite ->
                            InviteRow(
                                invite = invite,
                                isPending = invite.id in uiState.pendingIds,
                                onRevoke = { onRevokeInvite(invite.id) },
                                onShowQrCode = { onShowQrCode(invite.id) },
                            )
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateInviteDialog(
                isCreating = (uiState as? ManagerInvitesUiState.Ready)?.isCreating == true,
                onConfirm = { role ->
                    onCreateInvite(role)
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false },
            )
        }

        val qrCode = (uiState as? ManagerInvitesUiState.Ready)?.qrCode
        if (qrCode != null) {
            QrCodeDialog(qrCode = qrCode, onDismiss = onDismissQrCode)
        }
    }
}

@Composable
private fun ManagerInvitesLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(RojanDimens.SpaceLG), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

/** Mirrors [ai.rojan.designlab.manager.screens.staff.ManagerSpecialistServicesScreen]'s (P1-5) error card — same components, same shape. */
@Composable
private fun ManagerInvitesError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "بارگذاری دعوت‌ها با خطا مواجه شد",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = RojanTypography.Body,
                    color = ManagerColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                )
            }
        }

        ManagerPrimaryButton(
            text = "تلاش مجدد",
            onClick = onRetry,
            modifier = Modifier.padding(top = RojanDimens.SpaceLG),
        )
    }
}

@Composable
private fun EmptyInvitesNotice() {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Text(
            text = "دعوتی برای این سالن ثبت نشده است.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InviteRow(
    invite: SalonInvite,
    isPending: Boolean,
    onRevoke: () -> Unit,
    onShowQrCode: () -> Unit,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = roleLabel(invite.role), style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = statusLabel(invite.status), style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }

            if (isPending) {
                CircularProgressIndicator(
                    color = ManagerColors.Turquoise,
                    modifier = Modifier.size(RojanDimens.SpaceLG),
                    strokeWidth = 2.dp,
                )
            } else if (invite.status == SalonInviteStatus.CREATED) {
                TextButton(onClick = onShowQrCode) {
                    Text("نمایش QR", color = ManagerColors.Turquoise)
                }
                TextButton(onClick = onRevoke) {
                    Text("لغو", color = RojanErrorText)
                }
            }
        }
    }
}

private fun roleLabel(role: SalonMemberRole): String = when (role) {
    SalonMemberRole.MANAGER -> "مدیر"
    SalonMemberRole.RECEPTIONIST -> "پذیرش"
}

private fun statusLabel(status: SalonInviteStatus): String = when (status) {
    SalonInviteStatus.CREATED -> "در انتظار پذیرش"
    SalonInviteStatus.ACCEPTED -> "پذیرفته‌شده"
    SalonInviteStatus.EXPIRED -> "منقضی‌شده"
    SalonInviteStatus.REVOKED -> "لغوشده"
}

@Composable
private fun CreateInviteDialog(
    isCreating: Boolean,
    onConfirm: (SalonMemberRole) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedRole by remember { mutableStateOf(SalonMemberRole.RECEPTIONIST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد دعوت جدید") },
        text = {
            Column {
                RoleOption(
                    label = "پذیرش",
                    selected = selectedRole == SalonMemberRole.RECEPTIONIST,
                    onClick = { selectedRole = SalonMemberRole.RECEPTIONIST },
                )
                RoleOption(
                    label = "مدیر",
                    selected = selectedRole == SalonMemberRole.MANAGER,
                    onClick = { selectedRole = SalonMemberRole.MANAGER },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedRole) }, enabled = !isCreating) {
                Text("ایجاد دعوت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("انصراف")
            }
        },
    )
}

@Composable
private fun RoleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = ManagerColors.Turquoise),
        )
        Text(text = label, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
    }
}

@Composable
private fun QrCodeDialog(qrCode: QrCodeState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کد QR دعوت") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().size(280.dp), contentAlignment = Alignment.Center) {
                when (qrCode) {
                    is QrCodeState.Loading -> CircularProgressIndicator(color = ManagerColors.Turquoise)
                    is QrCodeState.Error -> Text(
                        text = qrCode.message,
                        style = RojanTypography.Body,
                        color = RojanErrorText,
                        textAlign = TextAlign.Center,
                    )
                    is QrCodeState.Loaded -> {
                        val bitmap = remember(qrCode.png) {
                            BitmapFactory.decodeByteArray(qrCode.png, 0, qrCode.png.size)
                        }
                        if (bitmap != null) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "کد QR دعوت")
                        } else {
                            Text(
                                text = "کد QR قابل نمایش نیست",
                                style = RojanTypography.Body,
                                color = RojanErrorText,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerInvitesScreenLoadingPreview() {
    RojanTheme {
        ManagerInvitesContent(
            uiState = ManagerInvitesUiState.Loading,
            onRetry = {},
            onCreateInvite = {},
            onRevokeInvite = {},
            onShowQrCode = {},
            onDismissQrCode = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerInvitesScreenErrorPreview() {
    RojanTheme {
        ManagerInvitesContent(
            uiState = ManagerInvitesUiState.Error("اجازه دسترسی به این بخش را ندارید."),
            onRetry = {},
            onCreateInvite = {},
            onRevokeInvite = {},
            onShowQrCode = {},
            onDismissQrCode = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerInvitesScreenReadyPreview() {
    val sampleInvites = listOf(
        SalonInvite(id = "i1", salonId = "s1", role = SalonMemberRole.RECEPTIONIST, status = SalonInviteStatus.CREATED, expiresAt = "2026-09-05T00:00:00Z", createdAt = "2026-09-03T00:00:00Z"),
        SalonInvite(id = "i2", salonId = "s1", role = SalonMemberRole.MANAGER, status = SalonInviteStatus.ACCEPTED, expiresAt = "2026-09-05T00:00:00Z", createdAt = "2026-09-01T00:00:00Z"),
    )
    RojanTheme {
        ManagerInvitesContent(
            uiState = ManagerInvitesUiState.Ready(invites = sampleInvites, pendingIds = setOf("i2")),
            onRetry = {},
            onCreateInvite = {},
            onRevokeInvite = {},
            onShowQrCode = {},
            onDismissQrCode = {},
        )
    }
}
