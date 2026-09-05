package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.profile.RescheduleAppointmentViewModel
import ai.rojan.designlab.presentation.profile.RescheduleAppointmentViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/** `TimeSlot.start` is a full local ISO datetime — same slicing [ai.rojan.designlab.screens.bookingflow.BookingTimeScreen]'s private `TimeSlot.timeLabel()` uses. */
private fun timeLabelFor(isoStart: String): String = isoStart.substringAfter('T').take(5)

/**
 * Appointment System completion (V1.0 Module 6 - Reschedule).
 *
 * **TEAM2-003 (Complete Booking API Contract):** now backed end to end by
 * [RescheduleAppointmentViewModel] — the real `GET /bookings/{id}` for the
 * booking being rescheduled, the real `available-slots` endpoint for date/
 * time selection (same one
 * [ai.rojan.designlab.presentation.booking.BookingTimeViewModel] already
 * uses), and the real `PUT /bookings/{id}/reschedule` to confirm — in
 * place of the previous [ai.rojan.designlab.domain.booking.BookingEngine]/
 * [ai.rojan.designlab.domain.catalog.CatalogEngine] demo logic, which
 * looked the appointment up in
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel]'s
 * local state — a lookup that could never find a real backend booking id
 * (TEAM2-004 moved `AppointmentsScreen`'s list onto real ids). No
 * `ecosystemViewModel` dependency remains: nothing on this screen has a
 * local/demo counterpart to fall back to.
 */
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    onBackClick: () -> Unit,
    onRescheduled: () -> Unit,
    viewModel: RescheduleAppointmentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = RescheduleAppointmentViewModelFactory(
            bookingId = appointmentId,
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            availabilityRepository = BackendApiContainerHolder.get(LocalContext.current).availabilityRepository,
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    val dates = remember { RollingBookingDates.next7Days() }

    // Defaults the date row to today once the booking itself has loaded,
    // so the customer sees a time list immediately rather than an empty
    // "pick a date first" screen — same spirit as the original demo
    // version's auto-selection, simplified to "today" rather than
    // skipping ahead to the first day with availability (that refinement,
    // BookingDateViewModel's territory, wasn't duplicated here to keep
    // this change scoped to the reschedule contract itself).
    LaunchedEffect(viewModel.targetState) {
        if (viewModel.targetState is UiState.Success && viewModel.selectedDateKey == null) {
            viewModel.selectDate(dates.first().first)
        }
    }

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "تغییر زمان نوبت",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            when (val targetState = viewModel.targetState) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری اطلاعات نوبت...")
                is UiState.Error -> RojanErrorState(
                    description = targetState.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retryLoadTarget() },
                )
                // A single-booking fetch has no "empty" outcome — Success or Error only. Handled for UiState's exhaustiveness, never actually reached.
                is UiState.Empty -> Unit
                is UiState.Success -> {
                    val target = targetState.data

                    Text(
                        text = "${target.salonName} • ${target.serviceName}",
                        style = RojanTypography.Body,
                        color = HomeColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

                    Text("انتخاب تاریخ", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        items(dates) { (key, label) ->
                            val isSelected = key == viewModel.selectedDateKey
                            HomeGlassSurface(
                                modifier = Modifier.clickable { viewModel.selectDate(key) },
                                shape = RojanShapes.Small,
                            ) {
                                Text(
                                    text = label,
                                    style = RojanTypography.Caption,
                                    color = if (isSelected) HomeColors.Glow else HomeColors.TextPrimary,
                                    modifier = Modifier.padding(RojanDimens.SpaceSM),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

                    Text("انتخاب ساعت", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

                    Column(modifier = Modifier.weight(1f)) {
                        when (val slotsState = viewModel.slotsState) {
                            is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری زمان‌های خالی...")
                            is UiState.Error -> RojanErrorState(
                                description = slotsState.message,
                                actionLabel = "تلاش مجدد",
                                onAction = { viewModel.retryLoadSlots() },
                            )
                            is UiState.Empty -> RojanEmptyState(
                                title = "برای این تاریخ زمانی موجود نیست",
                                description = "تاریخ دیگری را از بالا انتخاب کنید",
                            )
                            is UiState.Success -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                                    items(slotsState.data) { slot ->
                                        val label = timeLabelFor(slot.start)
                                        val isSelected = label == viewModel.selectedTime
                                        HomeGlassSurface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.selectTime(label) },
                                            shape = RojanShapes.Small,
                                        ) {
                                            Text(
                                                text = label,
                                                style = RojanTypography.Body,
                                                color = if (isSelected) HomeColors.Glow else HomeColors.TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(RojanDimens.SpaceSM),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                    val submitError = viewModel.submitError
                    if (submitError != null) {
                        RojanErrorState(
                            description = submitError,
                            actionLabel = "تلاش مجدد",
                            onAction = { viewModel.confirmReschedule(onSuccess = onRescheduled) },
                        )
                        Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                    }

                    PremiumButton(
                        text = "تایید زمان جدید",
                        onClick = { viewModel.confirmReschedule(onSuccess = onRescheduled) },
                        enabled = viewModel.selectedTime != null && !viewModel.isSubmitting,
                    )
                }
            }
        }
    }
}
