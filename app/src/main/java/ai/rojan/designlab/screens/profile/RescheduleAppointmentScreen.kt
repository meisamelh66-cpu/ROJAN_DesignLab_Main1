package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanComingSoonState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Reschedule an appointment.
 *
 * Production Data Integrity Phase 1: gated. `BookingRepository` (the real
 * backend booking API) exposes `createBooking`/`myBookings`/`getBooking`/
 * `cancelBooking` only — no reschedule/update-booking endpoint exists
 * (confirmed by reading `domain/repository/BookingRepository.kt` directly).
 * The previous version computed a new time slot via the demo
 * `BookingEngine`/`CatalogEngine` and called
 * [CustomerEcosystemViewModel.rescheduleAppointment], an in-memory-only
 * action with no real backend effect. Entry point stays reachable; content
 * becomes a Coming Soon state until the backend adds a real reschedule
 * capability.
 */
@Composable
fun RescheduleAppointmentScreen(
    appointmentId: String,
    onBackClick: () -> Unit,
    onRescheduled: () -> Unit,
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
            RojanComingSoonState()
        }
    }
}
