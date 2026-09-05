package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToLong

/**
 * Manager Booking Journey — step 5: review the resolved selections before
 * confirming.
 *
 * **Manager Booking Creation Integrity follow-up:** confirming now fires
 * the real `POST /api/v1/bookings` with the selected real `customerId`
 * (via [ManagerBookingViewModel.confirm]). [onConfirmed] (which leads to
 * the success screen) is only ever invoked from `confirm`'s `onSuccess`
 * callback — never unconditionally — so this screen cannot show a false
 * "success" for a booking that wasn't actually persisted. A failure
 * renders [ManagerBookingViewModel.uiState]'s `submitError` via the
 * existing [ManagerErrorState] component with a real retry action (a
 * network/validation/slot-taken failure here genuinely can succeed on
 * retry, unlike the previous, structural block this same field
 * represented before the backend contract existed).
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
    val dateLabel = state.dateKey?.let { RollingBookingDates.fullLabelFor(it) }

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
                    ReviewRow(label = "مشتری", value = customer?.fullName ?: "—")
                    ReviewRow(label = "خدمت", value = service?.name ?: "—")
                    if (service != null) {
                        ReviewRow(label = "قیمت", value = formatTomanPrice(service.price.roundToLong()))
                    }
                    ReviewRow(label = "متخصص", value = specialist?.displayName ?: "—")
                    ReviewRow(label = "تاریخ", value = dateLabel ?: "—")
                    ReviewRow(label = "ساعت", value = state.time ?: "—")
                }
            }

            if (state.submitError != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                ManagerErrorState(
                    description = state.submitError!!,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.confirm(onSuccess = onConfirmed) },
                )
            }

            ManagerPrimaryButton(
                text = if (state.isSubmitting) "در حال ثبت..." else "تایید نهایی",
                onClick = { viewModel.confirm(onSuccess = onConfirmed) },
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
