package ai.rojan.designlab.reception.screens.invite

import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.navigation.ReceptionAcceptedSessionGraph
import ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceUiState
import ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel
import ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModelFactory
import ai.rojan.designlab.reception.presentation.invite.ReceptionInviteOtpStep
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * P1-1, Reception Invite Acceptance Flow — the entire "confirm salon → OTP
 * login → accept → real Dashboard" journey for [token], reached from
 * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph] *outside*
 * (before) the existing [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]-gated
 * app — see [ReceptionInviteAcceptanceViewModel]'s own doc comment for why.
 * Built on the same [ReceptionScaffold]/[ReceptionGlassSurface]/[PremiumButton]
 * primitives as [ai.rojan.designlab.reception.screens.auth.ReceptionOtpAuthScreen]/
 * [ai.rojan.designlab.reception.screens.auth.ReceptionAccessErrorScreen] —
 * no new visual system introduced for this flow.
 */
@Composable
fun ReceptionInviteAcceptanceScreen(token: String) {
    val context = LocalContext.current
    val viewModel: ReceptionInviteAcceptanceViewModel = viewModel(
        factory = ReceptionInviteAcceptanceViewModelFactory(context.applicationContext, token),
    )

    when (val state = viewModel.state) {
        is ReceptionInviteAcceptanceUiState.Loading -> ReceptionInviteLoadingContent()
        is ReceptionInviteAcceptanceUiState.Error ->
            ReceptionInviteErrorContent(message = state.message, onRetry = viewModel::retry)
        is ReceptionInviteAcceptanceUiState.Preview ->
            ReceptionInvitePreviewContent(salonName = state.salonName, role = state.role, viewModel = viewModel)
        is ReceptionInviteAcceptanceUiState.Accepted ->
            ReceptionAcceptedSessionGraph(
                salonId = state.salonId,
                fullName = state.fullName,
                phoneNumber = state.phoneNumber,
            )
    }
}

@Composable
private fun ReceptionInviteLoadingContent() {
    ReceptionScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ReceptionPalette.textAccent)
        }
    }
}

@Composable
private fun ReceptionInviteErrorContent(message: String, onRetry: () -> Unit) {
    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "این دعوت نامعتبر است",
                        style = RojanTypography.CardTitle,
                        color = ReceptionPalette.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = message,
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    )
                }
            }
            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))
            PremiumButton(text = "تلاش مجدد", onClick = onRetry)
        }
    }
}

@Composable
private fun ReceptionInvitePreviewContent(
    salonName: String,
    role: String,
    viewModel: ReceptionInviteAcceptanceViewModel,
) {
    val otpStep by viewModel.otpStep.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()

    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "دعوت به همکاری", style = RojanTypography.HeroTitle, color = ReceptionPalette.textPrimary)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
            Text(
                text = "سالن «$salonName» شما را برای پیوستن به‌عنوان ${roleLabel(role)} دعوت کرده است.",
                style = RojanTypography.Body,
                color = ReceptionPalette.textSecondary,
            )
            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            when (val step = otpStep) {
                ReceptionInviteOtpStep.EnteringPhone -> {
                    Text(
                        text = "برای پذیرش دعوت، شماره موبایل خود را وارد کنید",
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                    InvitePhoneEntryCard(isSubmitting = isSubmitting, onSubmit = viewModel::requestOtpCode)
                }

                is ReceptionInviteOtpStep.AwaitingCode -> {
                    Text(
                        text = "کد ارسال‌شده به ${step.phoneNumber} را وارد کنید",
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                    InviteCodeEntryCard(
                        isSubmitting = isSubmitting,
                        onVerify = viewModel::verifyOtpCode,
                        onResend = viewModel::resendOtpCode,
                        onEditPhoneNumber = viewModel::editPhoneNumber,
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "MANAGER" -> "مدیر"
    "RECEPTIONIST" -> "پذیرش"
    else -> role
}

@Composable
private fun InvitePhoneEntryCard(isSubmitting: Boolean, onSubmit: (String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }

    ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            InviteTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "شماره موبایل",
                placeholder = "+989123456789",
                keyboardType = KeyboardType.Phone,
                enabled = !isSubmitting,
            )
        }
    }
    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
    PremiumButton(text = "ارسال کد تایید", onClick = { onSubmit(phoneNumber) }, enabled = !isSubmitting)
}

@Composable
private fun InviteCodeEntryCard(
    isSubmitting: Boolean,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onEditPhoneNumber: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            InviteTextField(
                value = code,
                onValueChange = { code = it },
                label = "کد تایید",
                placeholder = "------",
                keyboardType = KeyboardType.NumberPassword,
                enabled = !isSubmitting,
            )
            TextButton(onClick = onResend, enabled = !isSubmitting) {
                Text("ارسال مجدد کد", color = ReceptionPalette.textAccent)
            }
            TextButton(onClick = onEditPhoneNumber, enabled = !isSubmitting) {
                Text("ویرایش شماره موبایل", color = ReceptionPalette.textSecondary)
            }
        }
    }
    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
    PremiumButton(text = "تایید و پذیرش دعوت", onClick = { onVerify(code) }, enabled = !isSubmitting)
}

/** Isolated, private copy of the same minimal text field [ai.rojan.designlab.reception.screens.auth.ReceptionOtpAuthScreen] already carries — kept local rather than exported to avoid touching that unrelated file for this change. */
@Composable
private fun InviteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = ReceptionPalette.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ReceptionPalette.textPrimary,
            unfocusedTextColor = ReceptionPalette.textPrimary,
            focusedBorderColor = ReceptionPalette.textAccent,
            unfocusedBorderColor = ReceptionPalette.textSecondary,
            focusedLabelColor = ReceptionPalette.textAccent,
            unfocusedLabelColor = ReceptionPalette.textSecondary,
            cursorColor = ReceptionPalette.textAccent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
