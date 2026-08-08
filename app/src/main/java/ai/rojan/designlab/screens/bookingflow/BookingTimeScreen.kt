package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.booking.BookingTimeViewModel
import ai.rojan.designlab.presentation.booking.BookingTimeViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/** `TimeSlot.start` is a full local ISO datetime (`"2026-08-10T09:00:00"`) — this app only ever displayed/selected a bare "HH:mm", so the callback and grid keep conveying that, same shape [BookingViewModel.state.selectedTime]/Confirmation/waitlist already expect. */
private fun TimeSlot.timeLabel(): String = start.substringAfter('T').take(5)

/**
 * Journey 1, Screen 6: Booking — select time.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [BookingTimeViewModel] -> the real `available-slots` endpoint for
 * [bookingViewModel]'s salon/specialist/service and the [dateKey] chosen
 * on the previous screen (now a real ISO date, see
 * [ai.rojan.designlab.domain.booking.RollingBookingDates]) — replacing the
 * demo `BookingEngine.timeSlotsFor` call and its separate duration lookup
 * (the real endpoint takes `serviceId` directly and computes slot width
 * itself, so no duration resolution is needed here anymore).
 *
 * Production Data Integrity Phase 1: the "Join Waiting List" fallback
 * (shown when zero slots are available) is gated — no waitlist endpoint
 * exists on the backend (see `WaitlistScreen.kt`), and the previous
 * version called through to `CustomerEcosystemViewModel.joinWaitlist`, an
 * in-memory-only action, using `CatalogEngine` for salon/service name
 * lookups.
 */
@Composable
fun BookingTimeScreen(
    dateKey: String,
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onTimeSelected: (String) -> Unit,
    viewModel: BookingTimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BookingTimeViewModelFactory(
            salonId = bookingViewModel.state.salonId,
            specialistId = bookingViewModel.state.specialistId,
            serviceId = bookingViewModel.state.serviceId,
            date = dateKey,
            availabilityRepository = BackendApiContainerHolder.get(LocalContext.current).availabilityRepository,
        ),
    ),
) {
    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Text("انتخاب ساعت", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری زمان‌های خالی...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> RojanEmptyState(
                    title = "زمانی برای این تاریخ موجود نیست",
                    description = "لطفاً تاریخ دیگری را انتخاب کنید",
                )
                is UiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        items(state.data) { slot ->
                            HomeGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTimeSelected(slot.timeLabel()) },
                                shape = RojanShapes.Small,
                            ) {
                                Text(
                                    text = slot.timeLabel(),
                                    style = RojanTypography.Body,
                                    color = HomeColors.TextPrimary,
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
    }
}
