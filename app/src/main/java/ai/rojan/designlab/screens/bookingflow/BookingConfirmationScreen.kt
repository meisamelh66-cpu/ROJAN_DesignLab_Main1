package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 7: Confirmation — summarizes [BookingViewModel]'s
 * accumulated state before final confirmation. Reads from the
 * session-scoped ViewModel passed in by Navigation, not a global
 * singleton — see [BookingViewModel]'s own doc comment for why that
 * distinction matters.
 *
 * Booking Experience Refactor: payment method selection lives on this
 * same screen rather than a separate "Payment" screen — the spec's flow
 * lists "Date & Time → Payment → Booking Success" as a distinct step,
 * but "Do NOT increase the number of booking steps" takes priority;
 * merging payment into the existing confirmation screen delivers the
 * same functionality without adding a step.
 */
@Composable
fun BookingConfirmationScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val salon = bookingViewModel.state.salonId?.let { catalogEngine.findSalonById(it) }
    val specialist = bookingViewModel.state.specialistId?.let { catalogEngine.findSpecialistById(it) }
    val service = bookingViewModel.state.serviceId?.let { catalogEngine.findServiceById(it) }
    val dateLabel = bookingViewModel.state.selectedDateKey?.let { catalogEngine.dateLabelFor(it) }
    val time = bookingViewModel.state.selectedTime
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.WALLET) }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
            ) {
                GlassBackButton(onClick = onBackClick)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                Text("تایید رزرو", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        SummaryRow("سالن", salon?.name ?: "—")
                        SummaryRow("متخصص", specialist?.name ?: "انتخاب خودکار")
                        SummaryRow("خدمت", service?.name ?: "—")
                        SummaryRow("تاریخ", dateLabel ?: "—")
                        SummaryRow("ساعت", time ?: "—")
                        if (service != null) {
                            SummaryRow(
                                "مبلغ قابل پرداخت",
                                "${service.discountPrice ?: service.price} تومان",
                                highlight = true,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                Text("روش پرداخت", style = RojanTypography.Body, color = RojanTextOnGlass)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

                PaymentMethod.entries.forEach { method ->
                    PaymentMethodRow(
                        method = method,
                        isSelected = selectedPaymentMethod == method,
                        onSelect = { selectedPaymentMethod = method },
                    )
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                }
            }

            Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                PremiumButton(
                    text = "تایید نهایی رزرو",
                    onClick = onConfirmClick,
                )
            }
        }
    }
}

private enum class PaymentMethod(val label: String) {
    WALLET("کیف پول"),
    PAY_AT_SALON("پرداخت در محل"),
}

@Composable
private fun PaymentMethodRow(method: PaymentMethod, isSelected: Boolean, onSelect: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(method.label, style = RojanTypography.Body, color = RojanTextPrimary)
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "انتخاب شده" else "انتخاب نشده",
                tint = if (isSelected) RojanAIGlow else RojanTextSecondary,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = RojanTypography.Body, color = RojanTextSecondary)
        Text(
            text = value,
            style = RojanTypography.Body,
            color = if (highlight) RojanAIGlow else RojanTextPrimary,
        )
    }
}
