package ai.rojan.designlab.manager.screens.auth

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.domain.auth.ManagerOtpStep
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
import ai.rojan.designlab.ui.components.input.RojanOtpField
import ai.rojan.designlab.ui.components.input.RojanTextField
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * OTP Authentication Entry Flow Integration — the Manager App's phone +
 * OTP entry screen. Navigation-agnostic like Customer's `AuthScreen`: it
 * never calls a NavController itself, only [onAuthenticated], invoked once
 * via a [LaunchedEffect] watching auth + active-salon state.
 *
 * All OTP/JWT logic lives in [viewModel] — this composable only reads
 * `otpStep` / `isSubmitting` / `errorMessage` and forwards input to
 * `requestOtp` / `verifyOtp` / `resendOtp` / `editPhoneNumber`.
 *
 * UI Polish Sprint 3 (Task 4): the hand-rolled raw `OutlinedTextField`
 * (which broke the glass language) and the masked `NumberPassword` code
 * entry are replaced with the shared [RojanTextField] (glass, palette-
 * bound, RTL-aware) and [RojanOtpField] (segmented, **unmasked**, one
 * cell per digit). The form now scrolls and respects the keyboard inset
 * (`imePadding`), so short screens no longer hide the field. Buttons show
 * a real loading state. Navigation, callbacks and screen structure are
 * unchanged.
 */
@Composable
fun ManagerOtpAuthScreen(
    viewModel: ManagerAuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val activeSalonState by viewModel.activeSalonState.collectAsStateWithLifecycle()
    val otpStep by viewModel.otpStep.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(authState, activeSalonState) {
        if (authState is ManagerAuthState.Authenticated && activeSalonState !is ActiveSalonUiState.Loading) {
            onAuthenticated()
        }
    }

    ManagerScaffold {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Sprint 5A-3: the keyboard (IME) inset is now applied once
                // by ManagerScaffold (WindowInsets.safeDrawing); this
                // screen only keeps the scroll so the field stays reachable
                // when the area shrinks.
                .verticalScroll(rememberScrollState())
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Text(
                text = "ورود مدیر",
                style = RojanTypography.HeroTitle,
                color = ManagerColors.TextPrimary,
            )

            when (val step = otpStep) {
                ManagerOtpStep.EnteringPhone -> {
                    Text(
                        text = "شماره موبایل مدیر را وارد کنید",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                    )
                    PhoneEntryStep(
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
                    CodeEntryStep(
                        isSubmitting = isSubmitting,
                        isError = errorMessage != null,
                        onVerify = viewModel::verifyOtp,
                        onResend = viewModel::resendOtp,
                        onEditPhoneNumber = viewModel::editPhoneNumber,
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = RojanTypography.Caption,
                    color = RojanErrorText,
                    // 5B-2: polite live region so the error is announced on
                    // appear / change. Text and layout unchanged.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun PhoneEntryStep(
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }

    RojanTextField(
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        label = "شماره موبایل",
        placeholder = "+989123456789",
        leadingIcon = Icons.Filled.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        enabled = !isSubmitting,
    )

    ManagerPrimaryButton(
        text = "ارسال کد تایید",
        onClick = { onSubmit(phoneNumber) },
        enabled = phoneNumber.isNotBlank(),
        loading = isSubmitting,
    )
}

@Composable
private fun CodeEntryStep(
    isSubmitting: Boolean,
    isError: Boolean,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onEditPhoneNumber: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    RojanOtpField(
        value = code,
        onValueChange = { code = it },
        length = 6,
        enabled = !isSubmitting,
        isError = isError,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        TextButton(onClick = onResend, enabled = !isSubmitting) {
            Text("ارسال مجدد کد", color = ManagerColors.Turquoise)
        }
        TextButton(onClick = onEditPhoneNumber, enabled = !isSubmitting) {
            Text("ویرایش شماره موبایل", color = ManagerColors.TextSecondary)
        }
    }

    ManagerPrimaryButton(
        text = "تایید و ورود",
        onClick = { onVerify(code) },
        enabled = code.length == 6,
        loading = isSubmitting,
    )
}
