package ai.rojan.designlab.manager.screens.auth

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.domain.auth.ManagerOtpStep
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
import ai.rojan.designlab.ui.text.Text
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
 * OTP Authentication Entry Flow Integration — the Manager App's phone +
 * OTP entry screen. Navigation-agnostic like [ai.rojan.designlab.screens.auth.AuthScreen]
 * (Customer's own auth screen, untouched by this integration): it never
 * calls a NavController itself, only [onAuthenticated], invoked once via a
 * [LaunchedEffect] watching [ManagerAuthViewModel.authState] — same
 * callback pattern that screen already established.
 *
 * All OTP/JWT logic lives in [viewModel] — this composable only reads
 * [ManagerAuthViewModel.otpStep]/`isSubmitting`/`errorMessage` and forwards
 * user input to `requestOtp`/`verifyOtp`/`resendOtp`/`editPhoneNumber`.
 */
@Composable
fun ManagerOtpAuthScreen(
    viewModel: ManagerAuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val otpStep by viewModel.otpStep.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        if (authState is ManagerAuthState.Authenticated) onAuthenticated()
    }

    ManagerScaffold {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "ورود مدیر",
                style = RojanTypography.HeroTitle,
                color = ManagerColors.TextPrimary,
            )
            Spacer()

            when (val step = otpStep) {
                ManagerOtpStep.EnteringPhone -> {
                    Text(
                        text = "شماره موبایل مدیر را وارد کنید",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                    )
                    Spacer()
                    PhoneEntryCard(
                        isSubmitting = isSubmitting,
                        onSubmit = viewModel::requestOtp,
                    )
                }

                is ManagerOtpStep.AwaitingCode -> {
                    Text(
                        text = "کد ارسال‌شده به ${step.phoneNumber} را وارد کنید",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
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

    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            ManagerTextField(
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
    ManagerPrimaryButton(
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

    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            ManagerTextField(
                value = code,
                onValueChange = { code = it },
                label = "کد تایید",
                placeholder = "------",
                keyboardType = KeyboardType.NumberPassword,
                enabled = !isSubmitting,
            )

            TextButton(onClick = onResend, enabled = !isSubmitting) {
                Text("ارسال مجدد کد", color = ManagerColors.Turquoise)
            }
            TextButton(onClick = onEditPhoneNumber, enabled = !isSubmitting) {
                Text("ویرایش شماره موبایل", color = ManagerColors.TextSecondary)
            }
        }
    }
    Spacer()
    ManagerPrimaryButton(
        text = "تایید و ورود",
        onClick = { onVerify(code) },
        enabled = !isSubmitting,
    )
}

/** Minimal, self-contained Manager-themed text field — this screen's only text-entry need; no shared Manager `TextField` component exists yet to reuse (Customer's `HomeTextField` is theme-bound to `HomeColors`, not appropriate here). */
@Composable
private fun ManagerTextField(
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
        textStyle = LocalTextStyle.current.copy(color = ManagerColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ManagerColors.TextPrimary,
            unfocusedTextColor = ManagerColors.TextPrimary,
            focusedBorderColor = ManagerColors.Turquoise,
            unfocusedBorderColor = ManagerColors.TextSecondary,
            focusedLabelColor = ManagerColors.Turquoise,
            unfocusedLabelColor = ManagerColors.TextSecondary,
            cursorColor = ManagerColors.Turquoise,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
}
