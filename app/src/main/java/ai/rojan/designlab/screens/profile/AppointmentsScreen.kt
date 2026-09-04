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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.customer.EcosystemEvent
import ai.rojan.designlab.domain.reminder.ReminderTime
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.presentation.profile.AppointmentsViewModel
import ai.rojan.designlab.presentation.profile.AppointmentsViewModelFactory
import ai.rojan.designlab.presentation.profile.BookingAppointment
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

/**
 * Journey 2, Screen 2: My Appointments.
 *
 * **TEAM2-004 (Real Customer Booking Data):** the appointment list itself
 * now comes from [AppointmentsViewModel] — the customer's real backend
 * bookings (`GET /api/v1/bookings/mine`) — replacing the local/demo
 * `CustomerEcosystemViewModel.state.upcomingAppointments`/`pastAppointments`
 * lists this screen read directly before. [ecosystemViewModel] is still
 * used for the pieces of this screen that have no backend equivalent and
 * aren't part of this change: the waiting-list link, the last-event-cascade
 * summary, and the per-appointment reminder toggle (keyed by the
 * appointment's id, which is now the real backend booking id — the local
 * reminder store doesn't care whose id-space that came from).
 *
 * "تکمیل نوبت (نمایشی)" (demo-complete), the earlier flagship
 * cross-module-cascade preview, is not carried over: it operated by
 * looking an appointment id up in `CustomerEcosystemState.appointments`,
 * which real backend booking ids were never added to — keeping it would
 * mean a button that silently does nothing when tapped, which is exactly
 * the "silent failure" this change is meant to eliminate, not preserve
 * under new data. See `TEAM2_RESULT_CUSTOMER_BOOKINGS.md` for the full
 * reasoning and the (disclosed, not silent) limitation this leaves on
 * appointment details/reschedule navigation.
 */
@Composable
fun AppointmentsScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit = {},
    onWaitlistClick: () -> Unit = {},
    viewModel: AppointmentsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppointmentsViewModelFactory(
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    val ecosystemState = ecosystemViewModel.state

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("نوبت‌های من", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            if (ecosystemState.activeWaitlistEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "لیست انتظار من (${ecosystemState.activeWaitlistEntries.size})",
                        style = RojanTypography.Body,
                        color = HomeColors.Glow,
                        modifier = Modifier.clickable(onClick = onWaitlistClick),
                    )
                }
            }

            if (ecosystemViewModel.lastEvents.isNotEmpty()) {
                item { EventCascadeSummary(ecosystemViewModel.lastEvents) }
            }

            when (val bookingsState = viewModel.state) {
                is UiState.Loading -> item {
                    RojanLoadingState(message = "در حال بارگذاری نوبت‌های شما...")
                }

                is UiState.Error -> item {
                    RojanErrorState(
                        description = bookingsState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }

                is UiState.Empty -> item {
                    RojanEmptyState(
                        title = "هنوز نوبتی ندارید",
                        description = "برای رزرو نوبت جدید به صفحه اصلی بازگردید",
                    )
                }

                is UiState.Success -> {
                    val (upcoming, past) = bookingsState.data.partition {
                        it.status == BookingStatus.PENDING || it.status == BookingStatus.CONFIRMED
                    }
                    val sortedUpcoming = upcoming.sortedBy { it.startTime }
                    val sortedPast = past.sortedByDescending { it.startTime }

                    if (sortedUpcoming.isNotEmpty()) {
                        item { Text("پیش‌رو", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                        itemsIndexed(sortedUpcoming) { index, appt ->
                            AppointmentCard(
                                appointment = appt,
                                onClick = { onAppointmentClick(appt.id) },
                                onCancel = { viewModel.cancelBooking(appt.id) },
                                onReschedule = { onRescheduleClick(appt.id) },
                                isCancelling = viewModel.cancellingBookingId == appt.id,
                                ecosystemViewModel = ecosystemViewModel,
                                animationDelayMillis = index * 60,
                            )
                        }
                    }

                    if (sortedPast.isNotEmpty()) {
                        item { Text("گذشته", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                        itemsIndexed(sortedPast) { index, appt ->
                            AppointmentCard(
                                appointment = appt,
                                onClick = { onAppointmentClick(appt.id) },
                                animationDelayMillis = index * 60,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCascadeSummary(events: List<EcosystemEvent>) {
    HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
            Text("این اتفاق افتاد:", style = RojanTypography.Body, color = HomeColors.TextPrimary)
            events.forEach { event ->
                Text(
                    text = "• ${describeEvent(event)}",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
                )
            }
        }
    }
}

private fun describeEvent(event: EcosystemEvent): String = when (event) {
    is EcosystemEvent.BeautyTimelineEntryAdded -> "تاریخچه زیبایی به‌روزرسانی شد"
    is EcosystemEvent.LoyaltyPointsEarned -> "${event.points} امتیاز وفاداری کسب کردید"
    is EcosystemEvent.WalletCashbackAdded -> "${event.amount} تومان بازگشت وجه به کیف پول اضافه شد"
    is EcosystemEvent.MembershipProgressUpdated -> "${event.incrementPoints} امتیاز به پیشرفت عضویت اضافه شد"
    is EcosystemEvent.ReviewRequestCreated -> "درخواست ثبت نظر ایجاد شد"
    is EcosystemEvent.CouponRedeemed -> "${event.discountAmount} تومان تخفیف اعمال شد"
    is EcosystemEvent.CouponRejected -> "کد تخفیف قابل استفاده نیست"
    is EcosystemEvent.ReviewRejected -> "نظر شما قبلاً ثبت شده است"
    is EcosystemEvent.NotificationEnqueued -> "اعلان جدید ثبت شد"
    is EcosystemEvent.AppointmentBooked -> "نوبت شما با موفقیت رزرو شد"
    is EcosystemEvent.AppointmentStatusChanged -> "وضعیت نوبت به‌روزرسانی شد"
    is EcosystemEvent.AppointmentRescheduled -> "زمان نوبت تغییر کرد"
    is EcosystemEvent.WaitlistJoined -> "به لیست انتظار اضافه شدید"
    is EcosystemEvent.WaitlistJoinRejected -> event.reason
    is EcosystemEvent.WaitlistLeft -> "از لیست انتظار خارج شدید"
    is EcosystemEvent.WaitlistPromoted -> "یک نوبت از لیست انتظار برای شما رزرو شد"
    is EcosystemEvent.ReviewLifecycleAdvanced -> "وضعیت نظر به‌روزرسانی شد"
    is EcosystemEvent.ReviewSubmitted -> "نظر شما ثبت شد"
    is EcosystemEvent.FavoriteSalonToggled ->
        if (event.isNowFavorite) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد"
    is EcosystemEvent.WalletDebited -> "${event.amount} تومان از کیف پول کسر شد"
    is EcosystemEvent.WalletDebitRejected -> "موجودی کیف پول کافی نیست"
}

@Composable
private fun AppointmentCard(
    appointment: BookingAppointment,
    onClick: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    isCancelling: Boolean = false,
    ecosystemViewModel: CustomerEcosystemViewModel? = null,
    animationDelayMillis: Int = 0,
) {
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
                        // The backend has no per-salon color/image concept
                        // (same caveat BookingConfirmationScreen.kt's
                        // colorSeedFor documents) - every real booking
                        // renders this one generic fallback icon rather than
                        // a demo-catalog lookup that could never match a
                        // real backend salon id.
                        .background(HomeColors.TextSecondary.copy(alpha = 0.5f), RojanShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
                }

                Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))

                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.salonName, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Text(
                        "${appointment.serviceName} • ${appointment.specialistName}",
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                    Text(appointment.dateLabel, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${appointment.price} تومان", style = RojanTypography.Caption, color = HomeColors.Glow)
                    Text(
                        text = when (appointment.status) {
                            BookingStatus.PENDING -> "در انتظار تایید"
                            BookingStatus.CONFIRMED -> "تایید شده"
                            BookingStatus.COMPLETED -> "انجام شده"
                            BookingStatus.CANCELLED -> "لغو شده"
                        },
                        style = RojanTypography.Caption,
                        color = if (appointment.status == BookingStatus.CANCELLED) HomeColors.TextSecondary else HomeColors.Gold,
                    )
                }
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
                            text = if (isCancelling) "در حال لغو..." else "لغو نوبت",
                            style = RojanTypography.Caption,
                            color = RojanErrorText,
                            modifier = if (isCancelling) Modifier else Modifier.clickable { showCancelConfirm = true },
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

            if (ecosystemViewModel != null &&
                (appointment.status == BookingStatus.PENDING || appointment.status == BookingStatus.CONFIRMED)
            ) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                val preference = ecosystemViewModel.state.reminderPreferenceFor(appointment.id)
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
                            ecosystemViewModel.setReminderPreference(
                                appointmentId = appointment.id,
                                enabled = checked,
                                reminderTime = preference?.reminderTime ?: ReminderTime.H3,
                                appointmentDateLabel = appointment.dateLabel,
                                appointmentTime = appointment.time,
                            )
                        },
                    )
                }
            }
        }
    }
}
