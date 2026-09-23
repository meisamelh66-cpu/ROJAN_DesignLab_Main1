package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.presentation.booking.BookingDateViewModel
import ai.rojan.designlab.presentation.booking.BookingDateViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.bookingflow.components.BookingCenteredState
import ai.rojan.designlab.screens.bookingflow.components.BookingLoadingRows
import ai.rojan.designlab.screens.bookingflow.components.BookingScaffold
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.RefSelectableCell
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import java.util.Calendar

/**
 * Journey 1, Screen 5: Booking — select date.
 *
 * Quiet Luxury pass (visual only): the floating `GlassBackButton` orb, the
 * bare `HeroTitle`, the `HomeGlassSurface` rows with a violet `CalendarMonth`
 * icon, and the silent blank `Empty` branch are replaced with the
 * [BookingScaffold] shell (step 3 of 5) and flat `bookingflow.components` — a
 * list of [RefSelectableCell] day cells with a clear weekday / date hierarchy,
 * a rose-gold selected state, and calm loading / empty / error states.
 *
 * NOTHING about the data or logic changed: [RollingBookingDates.next7Days] is
 * still the single source of the candidate list + ISO keys, the
 * `autoSelectedDate` auto-skip [LaunchedEffect] is byte-identical,
 * [onDateSelected] still fires the ISO key, and the [viewModel] factory
 * (salon / specialist / service / `skipAutoSkip` / availability repo) is
 * untouched. The weekday / month text below is derived from the ISO key for
 * display only — [RollingBookingDates] itself is not modified. No ViewModel,
 * booking context, date calculation, navigation, or API is changed here.
 */
@Composable
fun BookingDateScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onDateSelected: (String) -> Unit,
    viewModel: BookingDateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            BookingDateViewModelFactory(
                salonId = bookingViewModel.state.salonId,
                specialistId = bookingViewModel.state.specialistId,
                serviceId = bookingViewModel.state.serviceId,
                skipAutoSkip = bookingViewModel.state.selectedDateKey != null,
                availabilityRepository = container.availabilityRepository,
                publicSalonRepository = container.publicSalonRepository,
                slug = bookingViewModel.salonSlug,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    val dates = remember { RollingBookingDates.next7Days() }

    LaunchedEffect(viewModel.autoSelectedDate) {
        viewModel.autoSelectedDate?.let { onDateSelected(it) }
    }

    BookingScaffold(
        title = "انتخاب تاریخ",
        onBackClick = onBackClick,
        step = 3,
    ) {
        when (val state = viewModel.state) {
            is UiState.Loading -> BookingLoadingRows(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 6,
                rowHeight = 68,
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
                title = "تاریخی برای رزرو موجود نیست",
                body = "در حال حاضر زمان خالی برای این خدمت وجود ندارد.",
            )

            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = BookingScreenMargin,
                    vertical = RojanDimens.SpaceLG,
                ),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                itemsIndexed(dates, key = { _, pair -> pair.first }) { index, (key, _) ->
                    DateCell(
                        isoKey = key,
                        index = index,
                        selected = key == bookingViewModel.state.selectedDateKey,
                        onClick = { onDateSelected(key) },
                    )
                }
            }
        }
    }
}

// --- Day cell ------------------------------------------------------------

@Composable
private fun DateCell(
    isoKey: String,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val parts = remember(isoKey) { dateParts(isoKey) }
    val primary = when (index) {
        0 -> "امروز"
        1 -> "فردا"
        else -> parts.weekday
    }

    RefSelectableCell(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) { contentColor ->
        Icon(
            imageVector = if (selected) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(primary, style = RojanTypography.Body, color = contentColor)
            Text(
                parts.dayMonth,
                style = RojanTypography.Caption,
                color = if (selected) contentColor.copy(alpha = 0.82f) else HomeColors.TextMuted,
            )
        }
    }
}

// --- ISO-key → display text (presentation only; RollingBookingDates untouched) --

private data class DateParts(val weekday: String, val dayMonth: String)

private val weekdayNames = mapOf(
    Calendar.SATURDAY to "شنبه",
    Calendar.SUNDAY to "یکشنبه",
    Calendar.MONDAY to "دوشنبه",
    Calendar.TUESDAY to "سه‌شنبه",
    Calendar.WEDNESDAY to "چهارشنبه",
    Calendar.THURSDAY to "پنجشنبه",
    Calendar.FRIDAY to "جمعه",
)

private val monthNames = mapOf(
    Calendar.JANUARY to "ژانویه",
    Calendar.FEBRUARY to "فوریه",
    Calendar.MARCH to "مارس",
    Calendar.APRIL to "آوریل",
    Calendar.MAY to "می",
    Calendar.JUNE to "ژوئن",
    Calendar.JULY to "ژوئیه",
    Calendar.AUGUST to "اوت",
    Calendar.SEPTEMBER to "سپتامبر",
    Calendar.OCTOBER to "اکتبر",
    Calendar.NOVEMBER to "نوامبر",
    Calendar.DECEMBER to "دسامبر",
)

private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

private fun toPersianDigits(value: Int): String =
    value.toString().map { ch -> if (ch.isDigit()) persianDigits[ch - '0'] else ch }.joinToString("")

private fun dateParts(isoKey: String): DateParts {
    val nums = isoKey.split("-").mapNotNull { it.toIntOrNull() }
    if (nums.size != 3) return DateParts("", isoKey)
    val calendar = Calendar.getInstance().apply {
        clear()
        set(nums[0], nums[1] - 1, nums[2])
    }
    val weekday = weekdayNames[calendar.get(Calendar.DAY_OF_WEEK)].orEmpty()
    val month = monthNames[calendar.get(Calendar.MONTH)].orEmpty()
    return DateParts(weekday, "${toPersianDigits(nums[2])} $month")
}
