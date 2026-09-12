package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerButtonRadius
import ai.rojan.designlab.screens.customer.components.CustomerConfirmDialog
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.components.StatusPill
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 2: My Appointments.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the glass `PremiumCardShell` cards with the salon-accent
 * colour tile + filled `Storefront`, the violet `HomeColors.Glow` "تغییر
 * زمان" link, the gold status text, the glass `RojanLoadingState` /
 * `RojanErrorState` / `RojanEmptyState`, and the raw Material `AlertDialog`
 * cancel prompt are replaced with the [CustomerScaffold] shell and flat
 * foundation primitives: [RefSurface] cards, a tinted status pill, outlined
 * icons, the [CustomerLoadingState] / [CustomerErrorState] / [CustomerEmptyState]
 * states, and a [CustomerConfirmDialog] for cancel.
 *
 * NOTHING about behaviour changed: the list is still
 * [BookingHistoryViewModel] over `GET /api/v1/bookings/my`; cancel still
 * calls `bookingRepository.cancelBooking(id)` then `viewModel.retry()`;
 * reschedule still calls [onRescheduleClick]; the reminder toggle still
 * drives [ReminderViewModel]. No ViewModel, repository, API, booking data
 * model, navigation route, or cancel/reschedule logic is touched.
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

    CustomerScaffold(title = "نوبت‌های من", onBackClick = onBackClick) {
        when (val state = viewModel.state) {
            is UiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 5,
                rowHeight = 96,
            )

            is UiState.Error -> CustomerErrorState(
                message = state.message,
                onRetry = { viewModel.retry() },
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "هنوز نوبتی ندارید",
                body = "برای رزرو نوبت جدید به صفحه اصلی بازگردید",
                icon = Icons.Outlined.EventBusy,
            )

            is UiState.Success -> {
                val upcoming = state.data.filter {
                    it.booking.status == BookingStatus.PENDING || it.booking.status == BookingStatus.CONFIRMED
                }
                val past = state.data.filter {
                    it.booking.status == BookingStatus.COMPLETED || it.booking.status == BookingStatus.CANCELLED
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = RojanDimens.SpaceLG),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    if (upcoming.isNotEmpty()) {
                        item(key = "header-upcoming") { CustomerSectionLabel("پیش‌رو") }
                        items(upcoming, key = { it.booking.id }) { item ->
                            AppointmentCard(
                                item = item,
                                onClick = { onAppointmentClick(item.booking.id) },
                                modifier = Modifier.padding(horizontal = CustomerScreenMargin),
                                onCancel = {
                                    coroutineScope.launch { bookingRepository.cancelBooking(item.booking.id) }
                                    viewModel.retry()
                                },
                                onReschedule = { onRescheduleClick(item.booking.id) },
                                reminderViewModel = reminderViewModel,
                            )
                        }
                    }

                    if (past.isNotEmpty()) {
                        if (upcoming.isNotEmpty()) {
                            item(key = "gap") { Spacer(Modifier.height(RojanDimens.SpaceMD)) }
                        }
                        item(key = "header-past") { CustomerSectionLabel("گذشته") }
                        items(past, key = { it.booking.id }) { item ->
                            AppointmentCard(
                                item = item,
                                onClick = { onAppointmentClick(item.booking.id) },
                                modifier = Modifier.padding(horizontal = CustomerScreenMargin),
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Appointment card ---------------------------------------------------

@Composable
private fun AppointmentCard(
    item: BookingWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    reminderViewModel: ReminderViewModel? = null,
) {
    val booking = item.booking

    RefSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .rojanPressable(onClick = onClick, role = Role.Button)
                    .padding(RojanDimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(CustomerButtonRadius))
                        .background(CustomerSurfaceFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = CustomerAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(RojanDimens.SpaceMD))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        item.salonName ?: booking.salonId,
                        style = RojanTypography.Body,
                        color = HomeColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.specialistName?.let { name ->
                        Spacer(Modifier.height(RojanDimens.SpaceXS))
                        Text(
                            name,
                            style = RojanTypography.Caption,
                            color = HomeColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(RojanDimens.SpaceXS))
                        Text(
                            booking.startTime.substringBefore('T') +
                                "  ·  " +
                                booking.startTime.substringAfter('T').take(5),
                            style = RojanTypography.Caption,
                            color = HomeColors.TextMuted,
                        )
                    }
                }
                Spacer(Modifier.width(RojanDimens.SpaceSM))
                StatusPill(booking.status)
            }

            if (onReschedule != null || onCancel != null) {
                RefRowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    onReschedule?.let { CardAction("تغییر زمان", CustomerAccent, it) }
                    onCancel?.let { cancel ->
                        var showCancelConfirm by remember { mutableStateOf(false) }
                        CardAction("لغو نوبت", RojanErrorText) { showCancelConfirm = true }
                        if (showCancelConfirm) {
                            CustomerConfirmDialog(
                                title = "لغو نوبت",
                                message = "مطمئن هستید می‌خواهید این نوبت را لغو کنید؟",
                                confirmLabel = "لغو نوبت",
                                onConfirm = {
                                    showCancelConfirm = false
                                    cancel()
                                },
                                onDismiss = { showCancelConfirm = false },
                            )
                        }
                    }
                }
            }

            if (reminderViewModel != null && booking.status == BookingStatus.CONFIRMED) {
                RefRowDivider()
                val preference = reminderViewModel.reminderPreferenceFor(booking.id)
                val isEnabled = preference?.enabled ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceXS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(RojanDimens.SpaceXS))
                        Text(
                            "یادآوری نوبت",
                            style = RojanTypography.Caption,
                            color = HomeColors.TextSecondary,
                        )
                    }
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CustomerOnAccent,
                            checkedTrackColor = CustomerAccent,
                            checkedBorderColor = CustomerAccent,
                            uncheckedThumbColor = HomeColors.TextMuted,
                            uncheckedTrackColor = CustomerSurfaceFill,
                            uncheckedBorderColor = CustomerHairline,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label,
        style = RojanTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = Modifier
            .rojanPressable(onClick = onClick, role = Role.Button)
            .padding(vertical = RojanDimens.SpaceXS),
    )
}
