package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.components.ReceptionUiStateList
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PERSIAN_WEEKDAY_INITIAL = mapOf(
    "MONDAY" to "د", "TUESDAY" to "س", "WEDNESDAY" to "چ", "THURSDAY" to "پ",
    "FRIDAY" to "ج", "SATURDAY" to "ش", "SUNDAY" to "ی",
)

/** Next 7 days (today included) — a fixed short-term window is enough for a walk-in/phone-booking reception flow; unlike Manager's calendar, this screen is not a full schedule browser. */
@Composable
fun ReceptionBookingDateTimeScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    val availableTimes by viewModel.availableTimes.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val dates = remember { (0..6).map { today.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "انتخاب تاریخ و ساعت", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                dates.forEach { date ->
                    DateChip(
                        date = date,
                        selected = date == selectedDate,
                        onClick = {
                            selectedDate = date
                            selectedTime = null
                            viewModel.loadAvailableTimes(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        },
                    )
                }
            }

            if (selectedDate == null) {
                Text(text = "ابتدا یک روز را انتخاب کنید", style = RojanTypography.Body, color = ReceptionPalette.textSecondary)
            } else {
                ReceptionUiStateList(
                    state = availableTimes,
                    emptyMessage = "زمان خالی برای این روز وجود ندارد",
                    modifier = Modifier.fillMaxSize(),
                ) { slot ->
                    TimeRow(
                        slot = slot,
                        selected = slot.start == selectedTime,
                        onClick = { selectedTime = slot.start; viewModel.selectTime(slot.start) },
                    )
                }
            }

            if (selectedTime != null) {
                PremiumButton(text = "ادامه", onClick = onContinueClick)
            }
        }
    }
}

@Composable
private fun DateChip(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    ReceptionGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) 0.28f else ai.rojan.designlab.ui.components.glass.PremiumGlassTheme.FillAlpha,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        ) {
            Text(
                text = PERSIAN_WEEKDAY_INITIAL[date.dayOfWeek.name] ?: date.dayOfWeek.name.take(1),
                style = RojanTypography.Caption,
                color = ReceptionPalette.textSecondary,
            )
            Text(text = date.dayOfMonth.toString(), style = RojanTypography.CardTitle, color = ReceptionPalette.textPrimary)
        }
    }
}

@Composable
private fun TimeRow(slot: TimeSlot, selected: Boolean, onClick: () -> Unit) {
    ReceptionGlassSurface(
        modifier = Modifier.fillMaxWidth().rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) 0.28f else ai.rojan.designlab.ui.components.glass.PremiumGlassTheme.FillAlpha,
    ) {
        Text(
            text = slot.start.substringAfter('T'),
            style = RojanTypography.CardTitle,
            color = ReceptionPalette.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
        )
    }
}
