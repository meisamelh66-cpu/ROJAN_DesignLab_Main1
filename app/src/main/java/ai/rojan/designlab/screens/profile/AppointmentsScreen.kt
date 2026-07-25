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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.domain.customer.EcosystemEvent
import ai.rojan.designlab.domain.reminder.ReminderTime
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 2: My Appointments — now reads the live, shared
 * [CustomerEcosystemViewModel.state] instead of the static repository
 * directly, and includes the flagship real cross-module interaction:
 * completing an upcoming appointment (a demo stand-in for "the
 * appointment time has passed") genuinely cascades through
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine] and
 * shows the resulting event chain inline — Wallet/Loyalty/Membership on
 * other screens reflect the change immediately since they read the same
 * shared state.
 */
@Composable
fun AppointmentsScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit = {},
    onWaitlistClick: () -> Unit = {},
) {
    val state = ecosystemViewModel.state
    val upcoming = state.upcomingAppointments
    val past = state.pastAppointments

    PremiumBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("نوبت‌های من", style = RojanTypography.HeroTitle, color = RojanTextOnGlass) }

            if (state.activeWaitlistEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "لیست انتظار من (${state.activeWaitlistEntries.size})",
                        style = RojanTypography.Body,
                        color = RojanAIGlow,
                        modifier = Modifier.clickable(onClick = onWaitlistClick),
                    )
                }
            }

            if (ecosystemViewModel.lastEvents.isNotEmpty()) {
                item { EventCascadeSummary(ecosystemViewModel.lastEvents) }
            }

            if (upcoming.isEmpty() && past.isEmpty()) {
                item {
                    RojanEmptyState(
                        title = "هنوز نوبتی ندارید",
                        description = "برای رزرو نوبت جدید به صفحه اصلی بازگردید",
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { Text("پیش‌رو", style = RojanTypography.Body, color = RojanTextOnGlass) }
                items(upcoming) { appt ->
                    AppointmentCard(
                        appointment = appt,
                        onClick = { onAppointmentClick(appt.id) },
                        onCompleteDemo = { ecosystemViewModel.completeAppointment(appt.id) },
                        onCancel = { ecosystemViewModel.cancelAppointment(appt.id) },
                        onReschedule = { onRescheduleClick(appt.id) },
                        ecosystemViewModel = ecosystemViewModel,
                    )
                }
            }

            if (past.isNotEmpty()) {
                item { Text("گذشته", style = RojanTypography.Body, color = RojanTextOnGlass) }
                items(past) { appt ->
                    AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt.id) }, onCompleteDemo = null)
                }
            }
        }
    }
}

@Composable
private fun EventCascadeSummary(events: List<EcosystemEvent>) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
            Text("این اتفاق افتاد:", style = RojanTypography.Body, color = RojanTextPrimary)
            events.forEach { event ->
                Text(
                    text = "• ${describeEvent(event)}",
                    style = RojanTypography.Caption,
                    color = RojanTextSecondary,
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
    appointment: DemoAppointment,
    onClick: () -> Unit,
    onCompleteDemo: (() -> Unit)?,
    onCancel: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    ecosystemViewModel: CustomerEcosystemViewModel? = null,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.salonName, style = RojanTypography.Body, color = RojanTextPrimary)
                    Text(
                        "${appointment.serviceName} • ${appointment.specialistName}",
                        style = RojanTypography.Caption,
                        color = RojanTextSecondary,
                    )
                    Text(appointment.dateLabel, style = RojanTypography.Caption, color = RojanTextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${appointment.price} تومان", style = RojanTypography.Caption, color = RojanAIGlow)
                    Text(
                        text = when (appointment.status) {
                            AppointmentStatus.UPCOMING -> "تایید شده"
                            AppointmentStatus.COMPLETED -> "انجام شده"
                            AppointmentStatus.CANCELLED -> "لغو شده"
                        },
                        style = RojanTypography.Caption,
                        color = if (appointment.status == AppointmentStatus.CANCELLED) RojanTextSecondary else RojanRatingGold,
                    )
                }
            }

            if (onCompleteDemo != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                Text(
                    text = "تکمیل نوبت (نمایشی)",
                    style = RojanTypography.Caption,
                    color = RojanStatusOnline,
                    modifier = Modifier.clickable(onClick = onCompleteDemo),
                )
            }

            if (onCancel != null || onReschedule != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
                    if (onReschedule != null) {
                        Text(
                            text = "تغییر زمان",
                            style = RojanTypography.Caption,
                            color = RojanAIGlow,
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

            if (ecosystemViewModel != null && appointment.status == AppointmentStatus.UPCOMING) {
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
                        color = RojanTextSecondary,
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
