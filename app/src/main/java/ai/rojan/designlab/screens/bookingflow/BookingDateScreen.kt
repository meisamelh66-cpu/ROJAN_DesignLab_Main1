package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 5: Booking — select date.
 *
 * Booking Engine completion, spec section 12: "If today's schedule is
 * full: Automatically preselect the first available day." Implemented
 * as a real availability check on load (not a guess) - the first day in
 * [CatalogEngine.availableDates] that [BookingEngine.hasAnyAvailability]
 * confirms has at least one valid slot for the selected service's
 * duration/specialist is auto-selected via [onDateSelected], skipping
 * the manual tap, *only* when today itself has none. If today has any
 * availability, this screen behaves exactly as before - no auto-skip.
 */
@Composable
fun BookingDateScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val bookingEngine = remember { BookingEngine() }
    val durationMinutes = bookingViewModel.state.serviceId
        ?.let { catalogEngine.findServiceById(it)?.durationMinutes }
        ?: 30
    val specialistId = bookingViewModel.state.specialistId ?: "no_specialist_chosen"
    val dates = catalogEngine.availableDates()

    LaunchedEffect(Unit) {
        // Back-navigation fix: only auto-advance on a genuinely fresh entry
        // into this screen (no date recorded yet for this booking session).
        // Without this guard, this effect re-runs every time the user
        // navigates back into this screen too - defeating Back whenever
        // today has no availability, since it would just auto-forward
        // again instead of showing the date list to reconsider.
        if (bookingViewModel.state.selectedDateKey != null) return@LaunchedEffect
        val today = dates.firstOrNull() ?: return@LaunchedEffect
        val todayHasAvailability = bookingEngine.hasAnyAvailability(today.first, durationMinutes, specialistId)
        if (!todayHasAvailability) {
            val firstAvailableDay = dates.firstOrNull { (key, _) ->
                bookingEngine.hasAnyAvailability(key, durationMinutes, specialistId)
            }
            if (firstAvailableDay != null) {
                onDateSelected(firstAvailableDay.first)
            }
        }
    }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Text("انتخاب تاریخ", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                items(dates) { (key, label) ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDateSelected(key) },
                        shape = RojanShapes.Small,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RojanDimens.SpaceMD),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = RojanAIGlow)
                            Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
                            Text(label, style = RojanTypography.Body, color = RojanTextPrimary)
                        }
                    }
                }
            }
        }
    }
}
