package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.booking.RescheduleUiState
import ai.rojan.designlab.presentation.booking.RescheduleViewModel
import ai.rojan.designlab.presentation.booking.RescheduleViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/** `TimeSlot.start` is a full local ISO datetime — this screen only ever shows/selects a bare "HH:mm". */
private fun TimeSlot.timeLabel(): String = start.substringAfter('T').take(5)

/**
 * Reschedule an appointment.
 *
 * Phase 2 (C2): un-gated — `ROJAN_Backend`'s `BookingController` does
 * expose `PUT /bookings/{id}/reschedule` (confirmed by reading the
 * controller directly); Phase 1 gated this screen on the since-corrected
 * assumption that it didn't. Re-uses the exact date-chips + time-grid
 * shape `BookingDateScreen`/`BookingTimeScreen` already use for a new
 * booking, via [RescheduleViewModel] (loads the existing booking's salon/
 * specialist/service first, then the same real `available-slots` calls).
 */
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    onBackClick: () -> Unit,
    onRescheduled: () -> Unit,
    viewModel: RescheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            RescheduleViewModelFactory(
                appointmentId = appointmentId,
                bookingRepository = container.bookingRepository,
                availabilityRepository = container.availabilityRepository,
            )
        },
    ),
) {
    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)
            Text(
                text = "تغییر زمان نوبت",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            when (val state = viewModel.state) {
                is RescheduleUiState.Loading -> RojanLoadingState(message = "در حال بارگذاری نوبت...")
                is RescheduleUiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is RescheduleUiState.Ready -> RescheduleContent(
                    state = state,
                    onDateSelected = viewModel::selectDate,
                    onTimeSelected = viewModel::selectTime,
                    onConfirm = { viewModel.confirm(onRescheduled) },
                    onRetrySlots = { viewModel.selectDate(state.selectedDate) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.RescheduleContent(
    state: RescheduleUiState.Ready,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetrySlots: () -> Unit,
) {
    Text("انتخاب تاریخ", style = RojanTypography.Body, color = HomeColors.TextPrimary)
    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(state.dates) { (key, label) ->
            val isSelected = key == state.selectedDate
            HomeGlassSurface(
                modifier = Modifier.rojanPressable(onClick = { onDateSelected(key) }),
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

    when (val slots = state.slots) {
        is UiState.Loading -> RojanLoadingState(message = "در حال بررسی زمان‌های خالی...")
        is UiState.Error -> RojanErrorState(
            description = slots.message,
            actionLabel = "تلاش مجدد",
            onAction = onRetrySlots,
        )
        is UiState.Empty -> RojanEmptyState(title = "زمانی برای این تاریخ موجود نیست")
        is UiState.Success -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                modifier = Modifier.weight(1f),
            ) {
                items(slots.data) { slot ->
                    val label = slot.timeLabel()
                    val isSelected = label == state.selectedTime
                    HomeGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeSelected(label) },
                        shape = RojanShapes.Small,
                    ) {
                        Text(
                            text = label,
                            style = RojanTypography.Body,
                            color = if (isSelected) HomeColors.Glow else HomeColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) HomeColors.Glow.copy(alpha = 0.12f) else HomeColors.Primary.copy(alpha = 0f))
                                .padding(RojanDimens.SpaceSM),
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

    state.submitError?.let {
        Text(it, style = RojanTypography.Caption, color = HomeColors.Magenta)
        Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
    }

    PremiumButton(
        text = "تایید زمان جدید",
        onClick = onConfirm,
        enabled = state.selectedTime != null && !state.isSubmitting,
        loading = state.isSubmitting,
    )
}
