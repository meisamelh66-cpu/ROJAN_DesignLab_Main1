package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
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

/**
 * Manager Booking Journey Phase 2 — step 5: review the resolved
 * selections (ids -> display entities, via the repositories) before
 * confirming.
 *
 * **TEAM2 Booking Creation Integrity follow-up:** [ManagerBookingViewModel.confirm]
 * cannot create a real backend booking yet (see its own doc comment for
 * the exact missing backend contract) and no longer produces a fake
 * local one either — it returns `false` and sets [ai.rojan.designlab.manager.domain.booking.ManagerBookingState.submitError].
 * [onConfirmed] (which leads to the success screen) is only ever called
 * when `confirm()` returns `true` — never unconditionally — so this
 * screen cannot show a false "success" for a booking nothing was ever
 * persisted for.
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
                    ReviewRow(label = "ساعت", value = state.time ?: "—")
                }
            }

            if (state.submitError != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                // No retry action: this is a structural backend-contract
                // gap, not a transient failure - offering "تلاش مجدد"
                // would dishonestly imply trying again might succeed.
                ManagerErrorState(description = state.submitError!!)
            }

            ManagerPrimaryButton(
                text = "تایید نهایی",
                onClick = {
                    if (viewModel.confirm()) onConfirmed()
                },
                enabled = state.isReadyToConfirm,
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
