package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import ai.rojan.designlab.ui.text.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.reminder.ReminderTime
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModel
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModelFactory
import ai.rojan.designlab.presentation.booking.ReminderViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

/**
 * Journey 2, Screen 2: My Appointments.
 *
 * Production Data Integrity Phase 1: now backed by the real
 * `GET /api/v1/bookings/my` (via [BookingHistoryViewModel]) instead of
 * `CustomerEcosystemViewModel.state.upcomingAppointments`/`pastAppointments`
 * (demo). Cancel was already real (`bookingRepository.cancelBooking`,
 * fired alongside a local demo cancel before this pass); it stays real,
 * minus the now-removed demo half. Dropped along with the demo data
 * source, not carried over as fake affordances on real bookings:
 * - "تکمیل نوبت (نمایشی)" — a demo-only status-flip button.
 * - The event-cascade summary — narrated fake loyalty/wallet/coupon/
 *   waitlist side effects that no longer exist (those screens are gated,
 *   see the Phase 1 plan).
 * - Price and service name — `Booking` has neither (see
 *   `BookingHistoryRepository`'s doc comment on why service name can't be
 *   resolved without a scan).
 * - The waitlist entry-count link — waitlist has no backend endpoint.
 *
 * The reminder toggle now uses the standalone [ReminderViewModel] — a
 * genuine on-device notification preference (see
 * `domain/reminder/ReminderScheduler.kt`), not backend business data, so
 * it's out of this phase's "no mock production data" scope, but no longer
 * needs the bigger `CustomerEcosystemViewModel` just to reach it (Task 7).
 */
@Composable
fun AppointmentsScreen(
    onBackClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit = {},
    onWaitlistClick: () -> Unit = {},
    viewModel: BookingHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BookingHistoryViewModelFactory(
            BackendApiContainerHolder.get(LocalContext.current).bookingHistoryRepository,
        ),
    ),
    reminderViewModel: ReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)
            Text(
                text = "نوبت‌های من",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری نوبت‌ها...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> RojanEmptyState(
                    title = "هنوز نوبتی ندارید",
                    description = "برای رزرو نوبت جدید به صفحه اصلی بازگردید",
                )
                is UiState.Success -> {
                    val upcoming = state.data.filter {
                        it.booking.status == BookingStatus.PENDING || it.booking.status == BookingStatus.CONFIRMED
                    }
                    val past = state.data.filter {
                        it.booking.status == BookingStatus.COMPLETED || it.booking.status == BookingStatus.CANCELLED
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
                        if (upcoming.isNotEmpty()) {
                            item { Text("پیش‌رو", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                            itemsIndexed(upcoming) { index, item ->
                                AppointmentCard(
                                    item = item,
                                    onClick = { onAppointmentClick(item.booking.id) },
                                    onCancel = {
                                        coroutineScope.launch { bookingRepository.cancelBooking(item.booking.id) }
                                        viewModel.retry()
                                    },
                                    onReschedule = { onRescheduleClick(item.booking.id) },
                                    reminderViewModel = reminderViewModel,
                                    animationDelayMillis = index * 60,
                                )
                            }
                        }

                        if (past.isNotEmpty()) {
                            item { Text("گذشته", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                            itemsIndexed(past) { index, item ->
                                AppointmentCard(
                                    item = item,
                                    onClick = { onAppointmentClick(item.booking.id) },
                                    animationDelayMillis = index * 60,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BookingStatus.label(): String = when (this) {
    BookingStatus.PENDING -> "در انتظار تایید"
    BookingStatus.CONFIRMED -> "تایید شده"
    BookingStatus.COMPLETED -> "انجام شده"
    BookingStatus.CANCELLED -> "لغو شده"
}

@Composable
private fun AppointmentCard(
    item: BookingWithDetails,
    onClick: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    reminderViewModel: ReminderViewModel? = null,
    animationDelayMillis: Int = 0,
) {
    val booking = item.booking

    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(salonAccentColorFor(booking.salonId).copy(alpha = 0.5f), RojanShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
                }

                Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.salonName ?: booking.salonId, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    item.specialistName?.let { name ->
                        Text(name, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                    }
                    Text(booking.startTime.replace('T', ' '), style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
                Text(
                    text = booking.status.label(),
                    style = RojanTypography.Caption,
                    color = if (booking.status == BookingStatus.CANCELLED) HomeColors.TextSecondary else HomeColors.Gold,
                )
            }

            if (onCancel != null || onReschedule != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
                    if (onReschedule != null) {
                        Text(
                            text = "تغییر زمان",
                            style = RojanTypography.Caption,
                            color = HomeColors.Glow,
                            modifier = Modifier.clickable(onClick = onReschedule),
                        )
                    }
                    if (onCancel != null) {
                        var showCancelConfirm by remember { mutableStateOf(false) }
                        Text(
                            text = "لغو نوبت",
                            style = RojanTypography.Caption,
                            color = RojanErrorText,
                            modifier = Modifier.clickable { showCancelConfirm = true },
                        )
                        if (showCancelConfirm) {
                            AlertDialog(
                                onDismissRequest = { showCancelConfirm = false },
                                title = { Text("لغو نوبت") },
                                text = { Text("مطمئن هستید می‌خواهید این نوبت را لغو کنید؟") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onCancel()
                                        showCancelConfirm = false
                                    }) {
                                        Text("لغو نوبت", color = RojanErrorText)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCancelConfirm = false }) {
                                        Text("انصراف")
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (reminderViewModel != null && booking.status == BookingStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                val preference = reminderViewModel.reminderPreferenceFor(booking.id)
                val isEnabled = preference?.enabled ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "یادآوری نوبت",
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            reminderViewModel.setReminderPreference(
                                appointmentId = booking.id,
                                enabled = checked,
                                reminderTime = preference?.reminderTime ?: ReminderTime.H3,
                                appointmentDateLabel = booking.startTime.substringBefore('T'),
                                appointmentTime = booking.startTime.substringAfter('T').take(5),
                            )
                        },
                    )
                }
            }
        }
    }
}
