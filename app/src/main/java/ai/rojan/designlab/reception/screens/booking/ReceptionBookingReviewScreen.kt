package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun ReceptionBookingReviewScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.createdBookingId) {
        if (state.createdBookingId != null) onConfirmed()
    }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "بازبینی نوبت", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    ReviewRow(label = "مشتری", value = state.customer?.fullName ?: "—")
                    ReviewRow(label = "خدمت", value = state.service?.name ?: "—")
                    ReviewRow(label = "متخصص", value = state.specialist?.displayName ?: "—")
                    ReviewRow(label = "زمان", value = state.time?.replace('T', ' ') ?: "—")
                }
            }

            if (state.confirmError != null) {
                Text(text = state.confirmError.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            PremiumButton(
                text = "تأیید نهایی",
                onClick = { scope.launch { viewModel.confirm() } },
                enabled = !state.isSubmitting,
                loading = state.isSubmitting,
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column {
        Text(text = label, style = RojanTypography.Caption, color = ReceptionPalette.textSecondary)
        Text(text = value, style = RojanTypography.CardTitle, color = ReceptionPalette.textPrimary)
    }
}
