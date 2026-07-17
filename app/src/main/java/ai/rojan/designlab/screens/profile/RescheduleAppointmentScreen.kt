package ai.rojan.designlab.screens.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Appointment System completion (V1.0 Module 6 - Reschedule).
 *
 * Deliberately does NOT reuse [ai.rojan.designlab.presentation.booking.BookingViewModel]
 * — that ViewModel's state machine (salon/service/specialist selection)
 * doesn't apply here; this appointment already has all of that, only
 * the date/time changes. Instead this reuses the same engine layer
 * ([BookingEngine], [CatalogEngine]) directly, exactly like
 * [ai.rojan.designlab.screens.bookingflow.BookingDateScreen]/
 * [ai.rojan.designlab.screens.bookingflow.BookingTimeScreen] do
 * internally — genuine reuse of the real availability logic (duration-
 * aware slots, auto-preselect-first-available-day), not duplicated
 * business logic, just without the unrelated booking-flow ViewModel
 * dependency.
 */
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onRescheduled: () -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val bookingEngine = remember { BookingEngine() }
    val appointment = remember(appointmentId) {
        ecosystemViewModel.state.appointments.find { it.id == appointmentId }
    }

    if (appointment == null) {
        PremiumBackground {
            Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
                GlassBackButton(onClick = onBackClick)
                Text("نوبت یافت نشد", color = RojanTextOnGlass, style = RojanTypography.Body)
            }
        }
        return
    }

    val durationMinutes = appointment.relatedServiceId
        ?.let { catalogEngine.findServiceById(it)?.durationMinutes }
        ?: 30
    val specialistId = appointment.specialistId ?: "no_specialist_chosen"
    val dates = remember { catalogEngine.availableDates() }

    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val today = dates.firstOrNull() ?: return@LaunchedEffect
        selectedDateKey = if (bookingEngine.hasAnyAvailability(today.first, durationMinutes, specialistId)) {
            today.first
        } else {
            dates.firstOrNull { (key, _) -> bookingEngine.hasAnyAvailability(key, durationMinutes, specialistId) }?.first
        }
    }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "تغییر زمان نوبت",
                style = RojanTypography.HeroTitle,
                color = RojanTextOnGlass,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            Text(
                text = "${appointment.salonName} • ${appointment.serviceName}",
                style = RojanTypography.Body,
                color = RojanTextOnGlass,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("انتخاب تاریخ", style = RojanTypography.Body, color = RojanTextOnGlass)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                items(dates) { (key, label) ->
                    val isSelected = key == selectedDateKey
                    GlassSurface(
                        modifier = Modifier
                            .clickable {
                                selectedDateKey = key
                                selectedTime = null
                            },
                        shape = RojanShapes.Small,
                    ) {
                        Text(
                            text = label,
                            style = RojanTypography.Caption,
                            color = if (isSelected) RojanAIGlow else RojanTextPrimary,
                            modifier = Modifier.padding(RojanDimens.SpaceSM),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("انتخاب ساعت", style = RojanTypography.Body, color = RojanTextOnGlass)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

            val slots = selectedDateKey?.let { key ->
                bookingEngine.timeSlotsFor(key, durationMinutes, specialistId).filter { it.available }
            } ?: emptyList()

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(slots) { slot ->
                    val isSelected = slot.time == selectedTime
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTime = slot.time },
                        shape = RojanShapes.Small,
                    ) {
                        Text(
                            text = slot.time,
                            style = RojanTypography.Body,
                            color = if (isSelected) RojanAIGlow else RojanTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RojanDimens.SpaceSM),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            val selectedDateLabel = dates.find { it.first == selectedDateKey }?.second

            PremiumButton(
                text = "تایید زمان جدید",
                onClick = {
                    val time = selectedTime
                    val dateKey = selectedDateKey
                    val dateLabel = selectedDateLabel
                    if (time != null && dateKey != null && dateLabel != null) {
                        ecosystemViewModel.rescheduleAppointment(appointmentId, dateKey, dateLabel, time)
                        onRescheduled()
                    }
                },
            )
        }
    }
}
