package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.booking.BookingTimeViewModel
import ai.rojan.designlab.presentation.booking.BookingTimeViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.bookingflow.components.BookingCenteredState
import ai.rojan.designlab.screens.bookingflow.components.BookingLoadingRows
import ai.rojan.designlab.screens.bookingflow.components.BookingScaffold
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.BookingSectionLabel
import ai.rojan.designlab.screens.bookingflow.components.BookingSectionLabelStyle
import ai.rojan.designlab.screens.bookingflow.components.RefSelectableCell
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * `TimeSlot.start` is a full local ISO datetime (`"2026-08-10T09:00:00"`) — this
 * app only ever displayed/selected a bare "HH:mm", and the callback + grid keep
 * conveying that, the same shape [BookingViewModel.state.selectedTime] /
 * Confirmation already expect.
 */
private fun TimeSlot.timeLabel(): String = start.substringAfter('T').take(5)

/**
 * Booking Time UX pass (2026-09-10): a flat list of every 15-minute slot in
 * one wide grid read as "too crowded" — this buckets the same, unmodified
 * slot list into day-part sections purely for presentation. [dayPartFor]
 * reads the hour off the already-computed "HH:mm" label; nothing about slot
 * generation, filtering, or the 15-minute granularity changes — every slot
 * the backend returned is still rendered, just under a section header
 * instead of in one undifferentiated wall of chips.
 */
private enum class DayPart(val label: String) {
    MORNING("صبح"),
    AFTERNOON("بعدازظهر"),
    EVENING("عصر"),
}

private fun dayPartFor(timeLabel: String): DayPart {
    val hour = timeLabel.substringBefore(':').toIntOrNull() ?: 0
    return when {
        hour < 12 -> DayPart.MORNING
        hour < 17 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }
}

/**
 * Journey 1, Screen 6: Booking — select time.
 *
 * Quiet Luxury pass (visual only): the floating `GlassBackButton` orb, the bare
 * `HeroTitle`, and the grid of `HomeGlassSurface` chips (metallic border +
 * corner sparkles + raw `Modifier.clickable`, no selected state) are replaced
 * with the [BookingScaffold] shell (step 4 of 5), a date-context label, and a
 * grid of flat [RefSelectableCell] time slots with a clear rose-gold selected
 * state and calm loading / empty / error states.
 *
 * **UX pass (2026-09-10):** the single flat 3-column grid is now a **4-column**
 * grid (more compact chips, fewer rows for the same slot count) split into
 * [DayPart] sections (صبح / بعدازظهر / عصر) via a full-width
 * [DayPartHeader] row inside the same [LazyVerticalGrid] — one
 * continuous scroll, no nested scrolling, no slot removed or re-bucketed in
 * time (every 15-minute slot the backend returned still renders as its own
 * chip). Grid item order stays left-to-right chronological within each row,
 * matching every other grid/list in this app (`DateCell`, `SalonCard`, …):
 * this app deliberately renders in a fixed, ambient-LTR [androidx.compose.ui.unit.LayoutDirection]
 * (see `RtlLayoutKit.kt`'s doc comment on why forcing global RTL was tried
 * and rejected), so a grid of same-size, non-textual chips reads correctly
 * left-to-right the same way a calendar or colour-swatch grid does; the
 * section label text itself still right-aligns via the shared bidi [Text].
 *
 * NOTHING about availability or the data flow changed: [BookingTimeViewModel]
 * still calls the real `available-slots` endpoint for [bookingViewModel]'s
 * salon / specialist / service and the [dateKey] chosen on the previous
 * screen, the factory is byte-identical, [onTimeSelected] still fires the bare
 * "HH:mm", and the backend still returns only free slots (there is no
 * "unavailable slot" in the data model — nothing to dim). No ViewModel,
 * booking context, availability logic, time calculation, API call, or
 * navigation is modified here. [RollingBookingDates.labelFor] is a read-only
 * call for the context label.
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
    BookingScaffold(
        title = "انتخاب ساعت",
        onBackClick = onBackClick,
        step = 4,
    ) {
        BookingSectionLabel(RollingBookingDates.labelFor(dateKey))
        Spacer(Modifier.height(RojanDimens.SpaceSM))

        when (val state = viewModel.state) {
            is UiState.Loading -> BookingLoadingRows(
                modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                count = 4,
                rowHeight = 52,
            )

            is UiState.Error -> BookingCenteredState(
                icon = Icons.Outlined.CloudOff,
                title = "مشکلی پیش آمد",
                body = state.message,
                actionLabel = "تلاش مجدد",
                onAction = { viewModel.retry() },
            )

            is UiState.Empty -> BookingCenteredState(
                icon = Icons.Outlined.EventBusy,
                title = "زمانی برای این تاریخ موجود نیست",
                body = "لطفاً تاریخ دیگری را انتخاب کنید.",
                actionLabel = "انتخاب تاریخ دیگر",
                onAction = onBackClick,
            )

            is UiState.Success -> {
                // Presentation-only grouping — every slot the backend
                // returned is preserved, just bucketed by day-part; order
                // within each group stays exactly as the backend returned
                // it (already chronological).
                val groups = state.data.groupBy { dayPartFor(it.timeLabel()) }
                val orderedGroups = DayPart.entries.mapNotNull { part -> groups[part]?.let { part to it } }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = BookingScreenMargin,
                        end = BookingScreenMargin,
                        top = RojanDimens.SpaceSM,
                        bottom = RojanDimens.SpaceXL,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    orderedGroups.forEach { (part, slots) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DayPartHeader(part, slots.size)
                        }
                        items(slots, key = { it.start }) { slot ->
                            val label = slot.timeLabel()
                            TimeChip(
                                label = label,
                                selected = label == bookingViewModel.state.selectedTime,
                                onClick = { onTimeSelected(label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width group header inside the same [LazyVerticalGrid] as the chips
 * (via `item(span = { GridItemSpan(maxLineSpan) })`) — deliberately NOT
 * [BookingSectionLabel] here: that component carries its own horizontal
 * margin, which would double up on top of the grid's own `contentPadding`
 * and misalign the header against the chips below it. Same
 * [BookingSectionLabelStyle] visual treatment, just without the extra inset.
 */
@Composable
private fun DayPartHeader(part: DayPart, count: Int) {
    Text(
        "${part.label} · ${count.toPersianDigits()}",
        style = BookingSectionLabelStyle,
        color = HomeColors.TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = RojanDimens.SpaceMD, bottom = RojanDimens.SpaceXS),
    )
}

private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

private fun Int.toPersianDigits(): String =
    toString().map { ch -> if (ch.isDigit()) persianDigits[ch - '0'] else ch }.joinToString("")

/** [RefSelectableCell] already enforces `heightIn(min = MinTouchTarget)` internally. */
@Composable
private fun TimeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    RefSelectableCell(
        selected = selected,
        onClick = onClick,
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
