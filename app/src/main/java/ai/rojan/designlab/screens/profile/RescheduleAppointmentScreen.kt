package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.booking.RescheduleUiState
import ai.rojan.designlab.presentation.booking.RescheduleViewModel
import ai.rojan.designlab.presentation.booking.RescheduleViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.RefPrimaryButton
import ai.rojan.designlab.screens.customer.components.RefSelectableCell
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/** `TimeSlot.start` is a full local ISO datetime — this screen only ever shows/selects a bare "HH:mm". */
private fun TimeSlot.timeLabel(): String = start.substringAfter('T').take(5)

/**
 * Reschedule an appointment.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb + ad-hoc
 * `HomeBackgroundTheme` shell + 32sp `HeroTitle`, the `HomeGlassSurface`
 * date chips + time cells (✦ corners, metallic border, glow; a violet
 * `HomeColors.Glow` "selected" text with a dead `HomeColors.Primary`
 * reference), the magenta `HomeColors.Magenta` submit error, the gradient
 * `PremiumButton`, and the glass `RojanLoadingState` / `RojanErrorState` /
 * `RojanEmptyState` views are replaced with the [CustomerScaffold] shell and
 * flat foundation primitives — a rail of [RefSelectableCell] day chips
 * (weekday / date hierarchy, solid rose-gold when selected), the same
 * [RefSelectableCell] time grid `BookingTimeScreen` uses, the
 * [CustomerLoadingState] / [CustomerErrorState] / [CustomerEmptyState]
 * states, a calm secondary-text submit error, and a pinned solid rose-gold
 * [RefPrimaryButton] in the scaffold's bottom slot.
 *
 * NOTHING about behaviour changed: still [RescheduleViewModel] over
 * `PUT /bookings/{id}/reschedule` (loads the booking, then the real
 * `available-slots` calls); `viewModel::selectDate` / `::selectTime` /
 * `confirm(onRescheduled)` / `retry()` are called in the same places;
 * `timeLabel()` still yields the bare "HH:mm" `selectTime` /
 * `"${date}T$time:00"` expect. No ViewModel, repository, API, booking state,
 * navigation route, callback, or model is touched.
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
    val ready = viewModel.state as? RescheduleUiState.Ready

    CustomerScaffold(
        title = "تغییر زمان رزرو",
        onBackClick = onBackClick,
        bottomBar = if (ready != null) {
            {
                Column {
                    ready.submitError?.let {
                        Text(
                            it,
                            style = RojanTypography.Caption,
                            color = HomeColors.TextSecondary,
                        )
                        Spacer(Modifier.height(RojanDimens.SpaceSM))
                    }
                    RefPrimaryButton(
                        label = "تایید زمان جدید",
                        onClick = { viewModel.confirm(onRescheduled) },
                        enabled = ready.selectedTime != null && !ready.isSubmitting,
                    )
                }
            }
        } else {
            null
        },
    ) {
        when (val state = viewModel.state) {
            is RescheduleUiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 5,
                rowHeight = 56,
            )

            is RescheduleUiState.Error -> CustomerErrorState(
                message = state.message,
                onRetry = { viewModel.retry() },
            )

            is RescheduleUiState.Ready -> RescheduleContent(
                state = state,
                onDateSelected = viewModel::selectDate,
                onTimeSelected = viewModel::selectTime,
                onRetrySlots = { viewModel.selectDate(state.selectedDate) },
            )
        }
    }
}

@Composable
private fun ColumnScope.RescheduleContent(
    state: RescheduleUiState.Ready,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onRetrySlots: () -> Unit,
) {
    Spacer(Modifier.height(RojanDimens.SpaceLG))
    CustomerSectionLabel("انتخاب تاریخ")
    Spacer(Modifier.height(RojanDimens.SpaceSM))

    LazyRow(
        contentPadding = PaddingValues(horizontal = CustomerScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        items(state.dates, key = { it.first }) { (key, label) ->
            DateChip(
                label = label,
                selected = key == state.selectedDate,
                onClick = { onDateSelected(key) },
            )
        }
    }

    Spacer(Modifier.height(RojanDimens.SpaceLG))
    CustomerSectionLabel("انتخاب ساعت")
    Spacer(Modifier.height(RojanDimens.SpaceSM))

    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when (val slots = state.slots) {
            is UiState.Loading -> CustomerLoadingState(count = 4, rowHeight = 52)

            is UiState.Error -> CustomerErrorState(
                message = slots.message,
                onRetry = onRetrySlots,
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "زمانی برای این تاریخ موجود نیست",
                body = "لطفاً تاریخ دیگری را انتخاب کنید.",
                icon = Icons.Outlined.EventBusy,
            )

            is UiState.Success -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = CustomerScreenMargin,
                    end = CustomerScreenMargin,
                    top = RojanDimens.SpaceSM,
                    bottom = RojanDimens.SpaceXL,
                ),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(slots.data, key = { it.start }) { slot ->
                    val label = slot.timeLabel()
                    RefSelectableCell(
                        selected = label == state.selectedTime,
                        onClick = { onTimeSelected(label) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { contentColor ->
                        Text(
                            label,
                            style = RojanTypography.Body,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// --- Day chip (weekday / date hierarchy) --------------------------------

@Composable
private fun DateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val parts = label.split("،", limit = 2)
    val primary = parts.first().trim()
    val secondary = parts.getOrNull(1)?.trim()

    RefSelectableCell(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.width(112.dp),
    ) { contentColor ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                primary,
                style = RojanTypography.Body,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (secondary != null) {
                Text(
                    secondary,
                    style = RojanTypography.Caption,
                    color = if (selected) contentColor.copy(alpha = 0.82f) else HomeColors.TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
