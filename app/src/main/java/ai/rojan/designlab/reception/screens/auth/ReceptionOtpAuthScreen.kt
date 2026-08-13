package ai.rojan.designlab.reception.screens.auth

import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import ai.rojan.designlab.reception.domain.auth.ReceptionOtpStep
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Reception App's phone + OTP entry screen — same shape as
 * [ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen], rebuilt
 * on the shared, light [ai.rojan.designlab.ui.background.WarmBackground]
 * (via [ReceptionScaffold]) instead of Manager's bespoke dark theme (see
 * `ReceptionScaffold.kt`'s own doc comment). Navigation-agnostic: never
 * calls a NavController itself, only [onAuthenticated].
 *
 * Phase 1 (authentication completion): [onAuthenticated] no longer fires
 * the instant [authState] becomes [ReceptionAuthState.Authenticated]. That
 * happens synchronously, before salon-access resolution (an async network
 * call) has any chance to finish — firing on it alone meant the nav graph
 * unconditionally sent every fresh login straight to the Dashboard route,
 * even when the real next destination should have been Salon Selection or
 * (see [ActiveSalonUiState.Error]) the access-error screen. Now waits for
 * [ReceptionAuthViewModel.activeSalonState] to leave
 * [ActiveSalonUiState.Loading] too, so by the time [onAuthenticated] fires,
 * `ReceptionNavGraph.kt`'s caller can inspect the real, settled state and
 * route correctly — same "wait for the real result, don't guess" principle
 * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph] already
 * applies to cold-start restore.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionOtpAuthScreen(
    viewModel: ReceptionAuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val activeSalonState by viewModel.activeSalonState.collectAsStateWithLifecycle()
    val otpStep by viewModel.otpStep.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(authState, activeSalonState) {
        if (authState is ReceptionAuthState.Authenticated && activeSalonState !is ActiveSalonUiState.Loading) {
            onAuthenticated()
        }
    }

    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "ورود پذیرش",
                style = RojanTypography.HeroTitle,
                color = ReceptionPalette.textPrimary,
            )
            Spacer()

            when (val step = otpStep) {
                ReceptionOtpStep.EnteringPhone -> {
                    Text(
                        text = "شماره موبایل پذیرش را وارد کنید",
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                    )
                    Spacer()
                    PhoneEntryCard(
                        isSubmitting = isSubmitting,
                        onSubmit = viewModel::requestOtp,
                    )
                }

                is ReceptionOtpStep.AwaitingCode -> {
                    Text(
                        text = "کد ارسال‌شده به ${step.phoneNumber} را وارد کنید",
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                    )
                    Spacer()
                    CodeEntryCard(
                        isSubmitting = isSubmitting,
                        onVerify = viewModel::verifyOtp,
                        onResend = viewModel::resendOtp,
                        onEditPhoneNumber = viewModel::editPhoneNumber,
                    )
                }
            }

            if (errorMessage != null) {
                Spacer()
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }
        }
    }
}

@Composable
private fun PhoneEntryCard(
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }

    ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            ReceptionTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "شماره موبایل",
                placeholder = "+989123456789",
                keyboardType = KeyboardType.Phone,
                enabled = !isSubmitting,
            )
        }
    }
    Spacer()
    PremiumButton(
        text = "ارسال کد تایید",
        onClick = { onSubmit(phoneNumber) },
        enabled = !isSubmitting,
    )
}

@Composable
private fun CodeEntryCard(
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
            ReceptionTextField(
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
    Spacer()
    PremiumButton(
        text = "تایید و ورود",
        onClick = { onVerify(code) },
        enabled = !isSubmitting,
    )
}

/** Minimal, self-contained Reception-themed text field — same "no shared TextField component to reuse yet" precedent as `ManagerOtpAuthScreen.kt`'s own `ManagerTextField`. */
@Composable
private fun ReceptionTextField(
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

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
}
