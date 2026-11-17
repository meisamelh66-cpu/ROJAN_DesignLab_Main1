package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.settings.ManagerPaymentGatewayViewModel
import ai.rojan.designlab.manager.presentation.settings.PaymentGatewaySettingsState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Manager App workspace — Settings → Payment Gateway (P1 Per-Salon Payment Gateway Configuration,
 * owner and manager — MANAGE_PAYMENT_GATEWAY). Reached from [ManagerSalonSetupScreen] via a new
 * "درگاه پرداخت" entry point, same
 * standalone-screen shape as [ManagerWorkingHoursScreen]. Backend contract already exists
 * (`PaymentGatewayConfigController`) — this screen closes the only remaining gap, Manager write UI.
 *
 * The real merchant id is never shown again after save — only [PaymentGatewaySettingsState.maskedMerchantId]
 * is ever displayed, matching the backend's own "never returns plaintext" guarantee exactly.
 */
@Composable
fun ManagerPaymentGatewayScreen(
    viewModel: ManagerPaymentGatewayViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val current = state) {
            is UiState.Loading, is UiState.Empty -> PaymentGatewayLoading()
            is UiState.Error -> PaymentGatewayLoadError(message = current.message, onRetry = viewModel::load)
            is UiState.Success -> PaymentGatewayForm(
                form = current.data,
                onMerchantIdChange = viewModel::onMerchantIdInputChange,
                onSaveClick = viewModel::save,
                onTestClick = viewModel::testConnection,
                onActivateClick = viewModel::activate,
                onDeactivateClick = viewModel::deactivate,
            )
        }
    }
}

@Composable
private fun PaymentGatewayLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun PaymentGatewayLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = RojanTypography.Body, color = RojanErrorText)
        ManagerPrimaryButton(text = "تلاش مجدد", onClick = onRetry)
    }
}

@Composable
private fun PaymentGatewayForm(
    form: PaymentGatewaySettingsState,
    onMerchantIdChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onTestClick: () -> Unit,
    onActivateClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        item {
            Text(text = "درگاه پرداخت", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary)
        }

        item {
            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "زرین‌پال", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                        StatusPill(configured = form.configured, isActive = form.isActive)
                    }

                    if (form.configured) {
                        Text(
                            text = "شناسه ذخیره‌شده: ${form.maskedMerchantId.orEmpty()}",
                            style = RojanTypography.Caption,
                            color = ManagerColors.TextSecondary,
                        )
                    } else {
                        Text(
                            text = "هنوز درگاه پرداختی برای این سالن ثبت نشده است.",
                            style = RojanTypography.Caption,
                            color = ManagerColors.TextSecondary,
                        )
                    }

                    OutlinedTextField(
                        value = form.merchantIdInput,
                        onValueChange = onMerchantIdChange,
                        label = { Text("شناسه پذیرنده (Merchant ID) جدید") },
                        placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                        singleLine = true,
                        enabled = !form.isSaving,
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

                    if (form.testResult != null) {
                        Text(
                            text = if (form.testResult.success) "اتصال با موفقیت تأیید شد" else "تست اتصال ناموفق: ${form.testResult.reason.orEmpty()}",
                            style = RojanTypography.Caption,
                            color = if (form.testResult.success) ManagerColors.Turquoise else RojanErrorText,
                        )
                    }

                    if (form.error != null) {
                        Text(text = form.error, style = RojanTypography.Caption, color = RojanErrorText)
                    }

                    ManagerPrimaryButton(
                        text = if (form.isSaving) "در حال ذخیره..." else "ذخیره تنظیمات",
                        enabled = !form.isSaving && form.merchantIdInput.isNotBlank(),
                        onClick = onSaveClick,
                    )

                    if (form.configured) {
                        ManagerPrimaryButton(
                            text = if (form.isTesting) "در حال تست..." else "تست اتصال",
                            enabled = !form.isTesting,
                            onClick = onTestClick,
                        )

                        ManagerPrimaryButton(
                            text = when {
                                form.isTogglingActive -> "در حال اعمال..."
                                form.isActive -> "غیرفعال‌سازی"
                                else -> "فعال‌سازی"
                            },
                            enabled = !form.isTogglingActive,
                            onClick = if (form.isActive) onDeactivateClick else onActivateClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(configured: Boolean, isActive: Boolean) {
    val (label, color) = when {
        !configured -> "تنظیم‌نشده" to ManagerColors.TextSecondary
        isActive -> "فعال" to ManagerColors.Turquoise
        else -> "غیرفعال" to ManagerColors.Gold
    }
    Text(text = label, style = RojanTypography.Caption, color = color)
}
