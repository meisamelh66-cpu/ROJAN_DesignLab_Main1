package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.PaymentMethod
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.presentation.booking.BookingConfirmationViewModel
import ai.rojan.designlab.presentation.booking.BookingConfirmationViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingSummary
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.screens.bookingflow.components.BookingCardShape
import ai.rojan.designlab.screens.bookingflow.components.BookingScaffold
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.BookingSectionLabel
import ai.rojan.designlab.screens.bookingflow.components.BookingSurfaceFill
import ai.rojan.designlab.screens.bookingflow.components.RefPrimaryButton
import ai.rojan.designlab.screens.bookingflow.components.RefRowDivider
import ai.rojan.designlab.screens.bookingflow.components.RefSurface
import ai.rojan.designlab.screens.bookingflow.components.BookingAccent
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import kotlin.math.roundToInt

/**
 * Journey 1, Screen 7: Confirmation — summarises [BookingViewModel]'s
 * accumulated state before the final `POST /api/v1/bookings`.
 *
 * Quiet Luxury pass (visual only): the floating `GlassBackButton` orb, the
 * `HomeGlassSurface` summary + payment cards (metallic border + corner
 * sparkles), the violet price, the animated `RojanSuccessCheckmark` "beat" on
 * the payment radio, and the magenta→pink gradient `PremiumButton` are
 * replaced with the [BookingScaffold] shell and flat `bookingflow.components`
 * primitives: a divided `RefSurface` summary card, a distinct rose-gold price
 * row, a plain static radio, and a solid `RefPrimaryButton`.
 *
 * NOTHING about the data flow changed: every read of / call into
 * [confirmationViewModel] and [bookingViewModel] — `loadSummary`, `summary`,
 * `onPaymentMethodSelected`, `confirmBooking`, `isReadyForConfirmation`,
 * `isSubmitting` / `isLoadingSummary`, `submitError`, and the five
 * `onEdit*` routes — is byte-identical to before. No ViewModel, no booking
 * state/context, no API call, no navigation is modified here.
 *
 * Payment method selection still lives on this screen (merged, not a separate
 * step) and still writes through [BookingViewModel.onPaymentMethodSelected] —
 * unchanged from the Customer Journey Audit Phase A (P0-2) fix.
 */
@Composable
fun BookingConfirmationScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onConfirmClick: (backendBookingId: String, summary: BookingSummary) -> Unit,
    onEditSalon: () -> Unit = {},
    onEditSpecialist: () -> Unit = {},
    onEditService: () -> Unit = {},
    onEditDate: () -> Unit = {},
    onEditTime: () -> Unit = {},
    confirmationViewModel: BookingConfirmationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BookingConfirmationViewModelFactory(
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    LaunchedEffect(
        bookingViewModel.state.salonId,
        bookingViewModel.state.specialistId,
        bookingViewModel.state.serviceId,
    ) {
        confirmationViewModel.loadSummary(
            salonId = bookingViewModel.state.salonId,
            specialistId = bookingViewModel.state.specialistId,
            serviceId = bookingViewModel.state.serviceId,
        )
    }

    val summary = confirmationViewModel.summary
    val salon = summary.salon
    val specialist = summary.specialist
    val service = summary.service
    val dateLabel = bookingViewModel.state.selectedDateKey?.let { RollingBookingDates.labelFor(it) }
    val time = bookingViewModel.state.selectedTime
    val selectedPaymentMethod = bookingViewModel.state.paymentMethod

    BookingScaffold(
        title = "تایید رزرو",
        onBackClick = onBackClick,
        step = 5,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                confirmationViewModel.submitError?.let { message ->
                    Text(
                        message,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                    Spacer(Modifier.height(RojanDimens.SpaceSM))
                }
                RefPrimaryButton(
                    label = "تایید نهایی رزرو",
                    onClick = {
                        val state = bookingViewModel.state
                        confirmationViewModel.confirmBooking(
                            salonId = state.salonId,
                            serviceId = state.serviceId,
                            specialistId = state.specialistId,
                            dateKey = state.selectedDateKey,
                            time = state.selectedTime,
                            onSuccess = { backendBookingId ->
                                onConfirmClick(backendBookingId, confirmationViewModel.summary)
                            },
                        )
                    },
                    enabled = bookingViewModel.isReadyForConfirmation() &&
                        !confirmationViewModel.isSubmitting &&
                        !confirmationViewModel.isLoadingSummary,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = RojanDimens.SpaceLG),
        ) {
            RefSurface(modifier = Modifier.padding(horizontal = BookingScreenMargin)) {
                Column(Modifier.fillMaxWidth()) {
                    if (salon != null) {
                        SalonHeaderRow(name = salon.name, initial = salon.name.trim().take(1), onClick = onEditSalon)
                        RefRowDivider()
                    }
                    SummaryRow("متخصص", specialist?.displayName ?: "انتخاب خودکار", onEditSpecialist)
                    RefRowDivider()
                    SummaryRow("خدمت", service?.name ?: "—", onEditService)
                    RefRowDivider()
                    SummaryRow("تاریخ", dateLabel ?: "—", onEditDate)
                    RefRowDivider()
                    SummaryRow("ساعت", time ?: "—", onEditTime)
                    if (service != null) {
                        RefRowDivider()
                        PriceRow(amount = "${service.price.roundToInt()} تومان")
                    }
                }
            }

            Spacer(Modifier.height(RojanDimens.SpaceXL))

            BookingSectionLabel("روش پرداخت")
            Spacer(Modifier.height(RojanDimens.SpaceSM))

            RefSurface(modifier = Modifier.padding(horizontal = BookingScreenMargin)) {
                Column(Modifier.fillMaxWidth()) {
                    PaymentMethod.entries.forEachIndexed { index, method ->
                        if (index > 0) RefRowDivider()
                        PaymentRow(
                            label = method.label,
                            selected = selectedPaymentMethod == method,
                            onSelect = { bookingViewModel.onPaymentMethodSelected(method) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(RojanDimens.SpaceLG))
        }
    }
}

// --- rows -----------------------------------------------------------------

@Composable
private fun SalonHeaderRow(name: String, initial: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick, role = Role.Button)
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            name,
            style = RojanTypography.CardTitle,
            color = HomeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(RojanDimens.SpaceMD))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BookingSurfaceFill),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = RojanTypography.CardTitle, color = BookingAccent)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onEdit, role = Role.Button)
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(RojanDimens.SpaceSM))
        Text(
            value,
            style = RojanTypography.Body,
            color = HomeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(RojanDimens.SpaceMD))
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextMuted)
    }
}

@Composable
private fun PriceRow(amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(
            amount,
            style = RojanTypography.CardTitle,
            color = BookingAccent,
            modifier = Modifier.weight(1f),
        )
        Text(
            "مبلغ قابل پرداخت",
            style = RojanTypography.Body,
            color = HomeColors.TextSecondary,
        )
    }
}

@Composable
private fun PaymentRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onSelect)
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "انتخاب شده" else "انتخاب نشده"
            }
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) BookingAccent else HomeColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            label,
            style = RojanTypography.Body,
            color = if (selected) HomeColors.TextPrimary else HomeColors.TextSecondary,
        )
    }
}

/** Display label for [PaymentMethod] — kept in the UI layer, same pattern as the previous implementation. */
private val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.WALLET -> "کیف پول"
        PaymentMethod.PAY_AT_SALON -> "پرداخت در محل"
    }
