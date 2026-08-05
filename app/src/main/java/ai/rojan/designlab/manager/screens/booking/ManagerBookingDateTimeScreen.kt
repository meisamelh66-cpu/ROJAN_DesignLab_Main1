package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.appointment.CalendarWeekDay
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.booking.timeSlotLabel
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Manager Booking Journey Phase 2 — step 4: pick a date (from
 * [ManagerCalendarWeek], the same real, clock-driven reference week
 * Calendar itself uses) and an available time.
 *
 * Final Release Validation — Real Booking Calendar Integration: time
 * slots now come from [ManagerBookingViewModel.availableTimes], a
 * genuine network call to the backend's computed-availability API —
 * loaded via [LaunchedEffect] (re-run whenever the selected specialist or
 * date changes), with real loading/empty/error states, replacing the
 * previous synchronous local-list computation.
 */
@Composable
fun ManagerBookingDateTimeScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onContinueClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val specialistId = state.specialistId
    val selectedDateKey = state.dateKey
    val selectedTime = state.time

    var availableTimesResult by remember(specialistId, selectedDateKey) {
        mutableStateOf<Result<List<TimeSlot>>?>(null)
    }
    LaunchedEffect(specialistId, selectedDateKey) {
        availableTimesResult = if (specialistId != null && selectedDateKey != null) {
            viewModel.availableTimes(specialistId, selectedDateKey)
        } else {
            null
        }
    }

    ManagerScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            ) {
                item {
                    RtlSectionHeader(
                        text = "انتخاب تاریخ و ساعت",
                        style = RojanTypography.ScreenTitle,
                        color = ManagerColors.TextPrimary,
                        horizontalPadding = 0.dp,
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        items(ManagerCalendarWeek.days) { day ->
                            DateChip(
                                day = day,
                                selected = day.key == selectedDateKey,
                                onClick = { viewModel.selectDate(day.key) },
                            )
                        }
                    }
                }

                if (selectedDateKey != null) {
                    item {
                        Text(
                            text = "ساعت‌های آزاد",
                            style = RojanTypography.SectionTitle,
                            color = ManagerColors.TextPrimary,
                            modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                        )
                    }

                    val result = availableTimesResult
                    when {
                        result == null -> item { AvailabilityNotice(text = "در حال دریافت ساعت‌های آزاد...") }
                        result.isFailure -> item {
                            AvailabilityNotice(
                                text = result.exceptionOrNull()?.message
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "خطا در دریافت ساعت‌های آزاد: $it" }
                                    ?: "خطا در دریافت ساعت‌های آزاد. لطفاً دوباره تلاش کنید.",
                            )
                        }
                        result.getOrNull().isNullOrEmpty() -> item {
                            AvailabilityNotice(text = "ساعت خالی برای این متخصص در این روز وجود ندارد.")
                        }
                        else -> item {
                            val slots = result.getOrNull().orEmpty()
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.height(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                            ) {
                                items(slots) { slot ->
                                    TimeChip(
                                        label = timeSlotLabel(slot.start),
                                        selected = slot.start == selectedTime,
                                        onClick = { viewModel.selectTime(slot.start) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ManagerPrimaryButton(
                text = "ادامه",
                onClick = onContinueClick,
                enabled = selectedDateKey != null && selectedTime != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceMD),
            )
        }
    }
}

@Composable
private fun AvailabilityNotice(text: String) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = text,
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
        )
    }
}

@Composable
private fun DateChip(day: CalendarWeekDay, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .size(width = 64.dp, height = 72.dp)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceXS),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.label,
                style = RojanTypography.Caption,
                color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = day.dayNumber,
                style = RojanTypography.CardTitle,
                color = if (selected) ManagerColors.Turquoise else ManagerColors.TextPrimary,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS),
            )
        }
    }
}

@Composable
private fun TimeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Text(
            text = label,
            style = RojanTypography.Caption,
            color = if (selected) ManagerColors.Turquoise else ManagerColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RojanDimens.SpaceSM),
        )
    }
}
