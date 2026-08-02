package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.presentation.booking.BookingDateViewModel
import ai.rojan.designlab.presentation.booking.BookingDateViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 5: Booking — select date.
 *
 * **Android <-> Backend Full Integration milestone:** the candidate date
 * list is now a real rolling calendar ([RollingBookingDates]), not the
 * demo's fixed, hardcoded-date table. "If today's schedule is full:
 * Automatically preselect the first available day" (spec section 12) is
 * still real, not a guess — [BookingDateViewModel] now confirms it against
 * the actual `available-slots` endpoint instead of the demo
 * `BookingEngine`; see its doc comment.
 */
@Composable
fun BookingDateScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onDateSelected: (String) -> Unit,
    viewModel: BookingDateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BookingDateViewModelFactory(
            salonId = bookingViewModel.state.salonId,
            specialistId = bookingViewModel.state.specialistId,
            serviceId = bookingViewModel.state.serviceId,
            skipAutoSkip = bookingViewModel.state.selectedDateKey != null,
            availabilityRepository = BackendApiContainerHolder.get(LocalContext.current).availabilityRepository,
        ),
    ),
) {
    val dates = remember { RollingBookingDates.next7Days() }

    LaunchedEffect(viewModel.autoSelectedDate) {
        viewModel.autoSelectedDate?.let { onDateSelected(it) }
    }

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Text("انتخاب تاریخ", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بررسی زمان‌های خالی...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> Unit
                is UiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        itemsIndexed(dates) { index, (key, label) ->
                            HomeGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .rojanEnterAnimation(delayMillis = index * 60)
                                    .rojanPressable(onClick = { onDateSelected(key) }),
                                shape = RojanShapes.Small,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(RojanDimens.SpaceMD),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = HomeColors.Glow)
                                    Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
                                    Text(label, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
