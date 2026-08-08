package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.booking.AppointmentDetailsData
import ai.rojan.designlab.presentation.booking.AppointmentDetailsViewModel
import ai.rojan.designlab.presentation.booking.AppointmentDetailsViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanComingSoonState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2: Appointment details.
 *
 * Production Data Integrity Phase 1: now backed by the real
 * `GET /api/v1/bookings/{id}` (via [AppointmentDetailsViewModel], which
 * also resolves salon/specialist/service name+price — bounded to this one
 * booking's salon, see that ViewModel's doc comment) instead of
 * `CustomerEcosystemViewModel.state.appointments`. Two sections removed
 * along with the demo data source, not carried over as fake content on a
 * real booking:
 * - The reviews section — no review DTO exists anywhere on the backend
 *   (gated, matching `MyReviewsScreen.kt`).
 * - The "Photos" placeholder tiles — decorative empty squares implying a
 *   photo-upload capability that never existed, demo or real.
 *
 * Phase 2 (C1): [onRebookClick] now also passes the booking's real `salonId`
 * — the previous single-arg version left `ServiceDetailsScreen` with no
 * salon to book against, surfacing its own disclosed "no salon" error on
 * every rebook. The real `Booking` always carries `salonId`, so there's
 * nothing to guess here.
 */
@Composable
fun AppointmentDetailsScreen(
    appointmentId: String,
    onBackClick: () -> Unit,
    onRebookClick: (serviceId: String, salonId: String) -> Unit,
    viewModel: AppointmentDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            AppointmentDetailsViewModelFactory(
                appointmentId = appointmentId,
                bookingRepository = container.bookingRepository,
                salonRepository = container.salonRepository,
                specialistRepository = container.specialistRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
            )
        },
    ),
) {
    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)
            Text(
                text = "جزئیات نوبت",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> Text("نوبت یافت نشد", color = HomeColors.TextPrimary, style = RojanTypography.Body)
                is UiState.Success -> AppointmentDetailsContent(state.data, onRebookClick)
            }
        }
    }
}

@Composable
private fun AppointmentDetailsContent(
    data: AppointmentDetailsData,
    onRebookClick: (serviceId: String, salonId: String) -> Unit,
) {
    val booking = data.booking

    LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        item {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                    Text(data.salonName ?: booking.salonId, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Text(
                        listOfNotNull(data.serviceName, data.specialistName).joinToString(" • "),
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                    Text(booking.startTime.replace('T', ' '), style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
            }
        }

        item {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                    Row {
                        Icon(Icons.Filled.Receipt, contentDescription = null, tint = HomeColors.Glow)
                        Text(" رسید و فاکتور", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    data.serviceName?.let { InvoiceRow("خدمت", it) }
                    data.servicePrice?.let { InvoiceRow("مبلغ", "${it.toInt()} تومان") }
                    InvoiceRow("وضعیت", booking.status.name)
                    InvoiceRow("شماره پیگیری", booking.id)
                }
            }
        }

        if (data.serviceName != null) {
            item {
                PremiumButton(
                    text = "رزرو مجدد",
                    onClick = { onRebookClick(booking.serviceId, booking.salonId) },
                )
            }
        }

        item { RojanComingSoonState() }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
        Text(value, style = RojanTypography.Caption, color = HomeColors.TextPrimary)
    }
}
