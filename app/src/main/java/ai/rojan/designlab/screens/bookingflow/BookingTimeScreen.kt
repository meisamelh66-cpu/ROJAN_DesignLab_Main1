package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 6: Booking — select time.
 *
 * Booking Engine completion: now reads [bookingViewModel]'s state to
 * find the selected service's real duration (falling back to 30 minutes
 * if no service was selected - shouldn't happen via normal navigation)
 * and specialist (falling back to a fixed "no specialist chosen yet"
 * sentinel, still deterministic, for flows that reach this screen
 * without going through the new Specialist Selection step) - both feed
 * real, duration-aware, per-specialist availability instead of a
 * generic per-salon list.
 *
 * Appointment System completion (V1.0 Module 6 - Waiting List):
 * [ecosystemViewModel] is optional (default `null`, so this screen's
 * signature stays backward-compatible for any caller that doesn't need
 * it) — when provided and zero slots are available for the selected
 * date, a real "Join Waiting List" action appears instead of an empty
 * grid, calling through to the same [CustomerEcosystemViewModel.joinWaitlist]
 * every other waitlist entry point uses.
 */
@Composable
fun BookingTimeScreen(
    dateKey: String,
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onTimeSelected: (String) -> Unit,
    ecosystemViewModel: CustomerEcosystemViewModel? = null,
) {
    val bookingEngine = remember { BookingEngine() }
    val catalogEngine = remember { CatalogEngine() }
    val durationMinutes = bookingViewModel.state.serviceId
        ?.let { catalogEngine.findServiceById(it)?.durationMinutes }
        ?: 30
    val specialistId = bookingViewModel.state.specialistId ?: "no_specialist_chosen"
    val slots = bookingEngine.timeSlotsFor(dateKey, durationMinutes, specialistId)
    val availableSlots = slots.filter { it.available }

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Text("انتخاب ساعت", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            if (availableSlots.isEmpty() && ecosystemViewModel != null) {
                WaitlistJoinPrompt(
                    dateKey = dateKey,
                    specialistId = bookingViewModel.state.specialistId,
                    bookingViewModel = bookingViewModel,
                    ecosystemViewModel = ecosystemViewModel,
                    catalogEngine = catalogEngine,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    // Booking Experience Refactor, spec section 12: "Do NOT
                    // display unavailable times." Filtered out entirely here,
                    // not just shown-disabled as before this fix.
                    items(availableSlots) { slot ->
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTimeSelected(slot.time) },
                            shape = RojanShapes.Small,
                        ) {
                            Text(
                                text = slot.time,
                                style = RojanTypography.Body,
                                color = RojanTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(RojanDimens.SpaceSM),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitlistJoinPrompt(
    dateKey: String,
    specialistId: String?,
    bookingViewModel: BookingViewModel,
    ecosystemViewModel: CustomerEcosystemViewModel,
    catalogEngine: CatalogEngine,
) {
    var joined by remember(dateKey) { mutableStateOf(false) }

    Column {
        Text(
            text = "برای این تاریخ زمانی موجود نیست",
            style = RojanTypography.Body,
            color = RojanTextOnGlass,
        )
        Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
        Text(
            text = "می‌توانید به لیست انتظار بپیوندید تا در صورت آزاد شدن زمانی، نوبت شما خودکار رزرو شود.",
            style = RojanTypography.Caption,
            color = RojanTextSecondary,
        )
        Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

        if (joined) {
            Text(
                text = "✓ به لیست انتظار پیوستید",
                style = RojanTypography.Body,
                color = RojanTextOnGlass,
            )
        } else {
            PremiumButton(
                text = "پیوستن به لیست انتظار",
                onClick = {
                    val salonId = bookingViewModel.state.salonId
                    val serviceId = bookingViewModel.state.serviceId
                    val salon = salonId?.let { catalogEngine.findSalonById(it) }
                    val service = serviceId?.let { catalogEngine.findServiceById(it) }
                    val dateLabel = catalogEngine.availableDates().find { it.first == dateKey }?.second ?: dateKey
                    if (salon != null && service != null) {
                        ecosystemViewModel.joinWaitlist(
                            salonId = salon.id,
                            salonName = salon.name,
                            serviceId = service.id,
                            serviceName = service.name,
                            specialistId = specialistId,
                            dateKey = dateKey,
                            dateLabel = dateLabel,
                        )
                        joined = true
                    }
                },
            )
        }
    }
}
