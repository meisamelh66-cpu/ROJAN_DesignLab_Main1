package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.presentation.common.UiState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** `TimeSlot.start` is a full local ISO datetime — same slicing convention every real-availability screen in this app already uses (e.g. `BookingTimeScreen.kt`'s `timeLabel()`). */
private fun timeLabelFor(isoStart: String): String = isoStart.substringAfter('T').take(5)

/**
 * Manager Booking Journey — step 4: pick a date and an available time.
 *
 * **Manager Booking Creation Integrity follow-up:** the date row now
 * uses [RollingBookingDates]' real rolling 7-day window instead of
 * `ManagerCalendarWeek`'s hardcoded reference week, and time slots come
 * from [ManagerBookingViewModel.slotsState] — the real
 * `available-slots` endpoint (same one the Customer booking flow uses),
 * replacing the previous fixed-grid-minus-in-memory-conflicts
 * computation.
 */
@Composable
fun ManagerBookingDateTimeScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onContinueClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dates = remember { RollingBookingDates.next7Days() }
    val selectedDateKey = state.dateKey
    val selectedTime = state.time

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
                        items(dates) { (dateKey, label) ->
                            DateChip(
                                label = label,
                                selected = dateKey == selectedDateKey,
                                onClick = { viewModel.selectDate(dateKey) },
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

                    when (val slotsState = viewModel.slotsState) {
                        is UiState.Loading -> item { ManagerLoadingState(message = "در حال بارگذاری ساعت‌های خالی...") }
                        is UiState.Error -> item {
                            ManagerErrorState(
                                description = slotsState.message,
                                actionLabel = "تلاش مجدد",
                                onAction = { viewModel.retryLoadSlots() },
                            )
                        }
                        is UiState.Empty -> item {
                            ManagerEmptyState(title = "ساعت خالی برای این متخصص در این روز وجود ندارد.")
                        }
                        is UiState.Success -> item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                            ) {
                                items(slotsState.data) { slot ->
                                    val label = timeLabelFor(slot.start)
                                    TimeChip(
                                        time = label,
                                        selected = label == selectedTime,
                                        onClick = { viewModel.selectTime(label) },
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
private fun DateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .size(width = 96.dp, height = 56.dp)
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
                text = label,
                style = RojanTypography.Caption,
                color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TimeChip(time: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Text(
            text = time,
            style = RojanTypography.Caption,
            color = if (selected) ManagerColors.Turquoise else ManagerColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RojanDimens.SpaceSM),
        )
    }
}
