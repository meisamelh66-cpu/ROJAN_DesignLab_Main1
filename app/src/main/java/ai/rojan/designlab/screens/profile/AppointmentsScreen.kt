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
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.domain.customer.EcosystemEvent
import ai.rojan.designlab.domain.reminder.ReminderTime
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
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

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("نوبت‌های من", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            if (state.activeWaitlistEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "لیست انتظار من (${state.activeWaitlistEntries.size})",
                        style = RojanTypography.Body,
                        color = HomeColors.Glow,
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
                item { Text("پیش‌رو", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                itemsIndexed(upcoming) { index, appt ->
                    AppointmentCard(
                        appointment = appt,
                        onClick = { onAppointmentClick(appt.id) },
                        onCompleteDemo = { ecosystemViewModel.completeAppointment(appt.id) },
                        onCancel = { ecosystemViewModel.cancelAppointment(appt.id) },
                        onReschedule = { onRescheduleClick(appt.id) },
                        ecosystemViewModel = ecosystemViewModel,
                        animationDelayMillis = index * 60,
                    )
                }
            }

            if (past.isNotEmpty()) {
                item { Text("گذشته", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                itemsIndexed(past) { index, appt ->
                    AppointmentCard(
                        appointment = appt,
                        onClick = { onAppointmentClick(appt.id) },
                        onCompleteDemo = null,
                        animationDelayMillis = index * 60,
                    )
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
    appointment: DemoAppointment,
    onClick: () -> Unit,
    onCompleteDemo: (() -> Unit)?,
    onCancel: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    ecosystemViewModel: CustomerEcosystemViewModel? = null,
    animationDelayMillis: Int = 0,
) {
    val catalogEngine = remember { CatalogEngine() }
    val salon = appointment.salonId?.let { catalogEngine.findSalonById(it) }

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
                        .background(
                            (salon?.colorSeed ?: HomeColors.TextSecondary).copy(alpha = 0.5f),
                            RojanShapes.Small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (salon?.assetRes != null) {
                        RojanSampleImage(
                            resId = salon.assetRes,
                            contentDescription = salon.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
                    }
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
                            AppointmentStatus.UPCOMING -> "تایید شده"
                            AppointmentStatus.COMPLETED -> "انجام شده"
                            AppointmentStatus.CANCELLED -> "لغو شده"
                        },
                        style = RojanTypography.Caption,
                        color = if (appointment.status == AppointmentStatus.CANCELLED) HomeColors.TextSecondary else HomeColors.Gold,
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
