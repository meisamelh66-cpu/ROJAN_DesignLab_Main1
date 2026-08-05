package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.booking.timeSlotLabel
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Manager Booking Journey Phase 2 — step 5: review the resolved
 * selections (ids -> display entities, via the repositories) before
 * confirming.
 *
 * Final Release Validation — Real Booking Calendar Integration: Confirm
 * now calls the real, suspend [ManagerBookingViewModel.confirm] and only
 * advances via [onConfirmed] on success — a real backend failure (e.g.
 * the selected customer has no linked account, or the slot was taken by
 * someone else between selection and confirm) is shown inline instead of
 * silently navigating forward regardless, which is what this screen used
 * to do. [ManagerBookingState.isSubmitting]/`confirmError` drive the
 * button's loading state and the error message — the screen itself still
 * does no business logic, just renders what the ViewModel already computed.
 */
@Composable
fun ManagerBookingReviewScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onConfirmed: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val customer = viewModel.customerById(state.customerId)
    val service = viewModel.serviceById(state.serviceId)
    val specialist = viewModel.specialistById(state.specialistId)
    val dateLabel = state.dateKey?.let { ManagerCalendarWeek.labelFor(it) }
    val timeLabel = state.time?.let { timeSlotLabel(it) }
    val coroutineScope = rememberCoroutineScope()

    ManagerScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize()) {
            RtlSectionHeader(
                text = "بررسی نوبت",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                horizontalPadding = 0.dp,
            )

            ManagerGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceMD),
                shape = RojanShapes.GlassCard,
            ) {
                Column(
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    ReviewRow(label = "مشتری", value = customer?.name ?: "—")
                    ReviewRow(label = "خدمت", value = service?.name ?: "—")
                    if (service != null) {
                        ReviewRow(label = "قیمت", value = formatTomanPrice(service.price))
                    }
                    ReviewRow(label = "متخصص", value = specialist?.name ?: "—")
                    ReviewRow(label = "تاریخ", value = dateLabel ?: "—")
                    ReviewRow(label = "ساعت", value = timeLabel ?: "—")
                }
            }

            if (state.confirmError != null) {
                Text(
                    text = state.confirmError.orEmpty(),
                    style = RojanTypography.Caption,
                    color = RojanErrorText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RojanDimens.SpaceMD),
                )
            }

            ManagerPrimaryButton(
                text = if (state.isSubmitting) "در حال ثبت..." else "تایید نهایی",
                onClick = {
                    coroutineScope.launch {
                        val result = viewModel.confirm()
                        if (result.isSuccess) {
                            onConfirmed()
                        }
                    }
                },
                enabled = state.isReadyToConfirm && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceLG),
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = RojanTypography.Body, color = ManagerColors.TextSecondary)
        Text(text = value, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
    }
}
