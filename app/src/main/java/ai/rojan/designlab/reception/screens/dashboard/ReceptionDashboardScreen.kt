package ai.rojan.designlab.reception.screens.dashboard

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.components.ReceptionUiStateList
import ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
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
 * The real Reception Dashboard — today's bookings for the active salon
 * (`GET /api/v1/salons/{salonId}/bookings`, real endpoint, owner-only
 * today — see [ReceptionDashboardViewModel]'s own doc comment for why an
 * authorization [ai.rojan.designlab.presentation.common.UiState.Error] is
 * the expected, honest result until `ROJAN_System1_Backend_Decision_v2.md`
 * §4 item 6 ships) plus quick actions into the booking wizard and customer
 * list. Replaces the Phase 0 static placeholder.
 *
 * Review-fixes phase, fix 3: each `PENDING`/`CONFIRMED` row now carries a
 * real confirm/complete action, wired to
 * [ReceptionDashboardViewModel.confirmBooking]/[ReceptionDashboardViewModel.completeBooking]
 * — the repository binding for these existed since the controlled-
 * implementation phase but had no UI call site until now.
 */
@Composable
fun ReceptionDashboardScreen(
    viewModel: ReceptionDashboardViewModel,
    onNewBookingClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val processingBookingId by viewModel.processingBookingId.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            RtlSectionHeader(
                text = "پذیرش",
                style = RojanTypography.ScreenTitle,
                color = ReceptionPalette.textPrimary,
                horizontalPadding = 0.dp,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                QuickAction(text = "نوبت جدید", onClick = onNewBookingClick, modifier = Modifier.weight(1f))
                QuickAction(text = "مشتریان", onClick = onCustomersClick, modifier = Modifier.weight(1f))
                QuickAction(text = "پروفایل", onClick = onProfileClick, modifier = Modifier.weight(1f))
            }

            RtlSectionHeader(
                text = "نوبت‌های امروز",
                style = RojanTypography.CardTitle,
                color = ReceptionPalette.textPrimary,
                horizontalPadding = 0.dp,
            )

            if (actionError != null) {
                Text(text = actionError.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            ReceptionUiStateList(
                state = bookings,
                emptyMessage = "نوبتی برای این سالن ثبت نشده است",
                modifier = Modifier.fillMaxSize(),
                onRetryClick = viewModel::refresh,
            ) { booking ->
                BookingRow(
                    booking = booking,
                    isProcessing = processingBookingId == booking.id,
                    onConfirmClick = { viewModel.confirmBooking(booking.id) },
                    onCompleteClick = { viewModel.completeBooking(booking.id) },
                )
            }
        }
    }
}

@Composable
private fun QuickAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ReceptionGlassSurface(
        modifier = modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = text,
            style = RojanTypography.Caption,
            color = ReceptionPalette.textAccent,
            modifier = Modifier.fillMaxWidth().padding(vertical = RojanDimens.SpaceMD, horizontal = RojanDimens.SpaceSM),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun BookingRow(
    booking: Booking,
    isProcessing: Boolean,
    onConfirmClick: () -> Unit,
    onCompleteClick: () -> Unit,
) {
    ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.customer?.name ?: booking.customerId,
                        style = RojanTypography.CardTitle,
                        color = ReceptionPalette.textPrimary,
                    )
                    Text(
                        text = booking.service?.name ?: booking.serviceId,
                        style = RojanTypography.Caption,
                        color = ReceptionPalette.textSecondary,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
                Column {
                    Text(
                        text = booking.startTime.substringAfter('T'),
                        style = RojanTypography.CardTitle,
                        color = ReceptionPalette.textPrimary,
                    )
                    Text(
                        text = booking.status.displayLabel,
                        style = RojanTypography.Caption,
                        color = ReceptionPalette.textSecondary,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
            }

            // PENDING -> Confirm, CONFIRMED -> Complete, CANCELLED/COMPLETED -> no action (terminal states).
            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                when (booking.status) {
                    BookingStatus.PENDING -> PremiumButton(
                        text = "تأیید نوبت",
                        onClick = onConfirmClick,
                        enabled = !isProcessing,
                        loading = isProcessing,
                    )
                    BookingStatus.CONFIRMED -> PremiumButton(
                        text = "ثبت انجام‌شدن",
                        onClick = onCompleteClick,
                        enabled = !isProcessing,
                        loading = isProcessing,
                    )
                    else -> Unit
                }
            }
        }
    }
}

private val BookingStatus.displayLabel: String
    get() = when (this) {
        BookingStatus.PENDING -> "در انتظار"
        BookingStatus.CONFIRMED -> "تأییدشده"
        BookingStatus.CANCELLED -> "لغوشده"
        BookingStatus.COMPLETED -> "انجام‌شده"
    }
