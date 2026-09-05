package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.calendar.ManagerCalendarAppointment
import ai.rojan.designlab.manager.presentation.calendar.ManagerCalendarViewModel
import ai.rojan.designlab.manager.presentation.calendar.ManagerCalendarViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class ManagerCalendarViewMode { DAILY, WEEKLY }

/**
 * Status indicator — Turquoise/Gold extend naturally into
 * "confirmed/pending"; [RojanErrorText] (existing token) covers
 * "cancelled." TEAM2-002: NO_SHOW is gone — the real backend
 * [BookingStatus] has no such state (PENDING/CONFIRMED/CANCELLED/COMPLETED
 * only), so there's nothing real to map it from any more.
 */
private enum class AppointmentStatus(val label: String, val color: Color) {
    PENDING("در انتظار", ManagerColors.Gold),
    CONFIRMED("تایید شده", ManagerColors.Turquoise),
    COMPLETED("انجام شده", ManagerColors.Turquoise),
    CANCELLED("لغو شده", RojanErrorText),
}

private fun BookingStatus.toDisplayStatus(): AppointmentStatus = when (this) {
    BookingStatus.PENDING -> AppointmentStatus.PENDING
    BookingStatus.CONFIRMED -> AppointmentStatus.CONFIRMED
    BookingStatus.COMPLETED -> AppointmentStatus.COMPLETED
    BookingStatus.CANCELLED -> AppointmentStatus.CANCELLED
}

/** Display row for one real [ManagerCalendarAppointment]. */
private data class CalendarAppointment(
    val id: String,
    val time: String,
    val clientName: String,
    val service: String,
    val specialist: String,
    val rawStatus: BookingStatus,
    val status: AppointmentStatus,
)

private fun ManagerCalendarAppointment.toDisplay(): CalendarAppointment = CalendarAppointment(
    id = id,
    time = time,
    clientName = customerLabel,
    service = serviceName,
    specialist = specialistName,
    rawStatus = status,
    status = status.toDisplayStatus(),
)

/**
 * Manager App workspace — Calendar MVP.
 *
 * **TEAM2-002 (Manager Data Persistence):** now backed end to end by
 * [ManagerCalendarViewModel] — the salon's real bookings
 * (`GET /api/v1/salons/{salonId}/bookings`) — replacing the previous
 * direct reads of `manager.data.ManagerRepositories.appointments`/
 * `.specialists`. The day selector now uses [RollingBookingDates]' real
 * rolling 7-day window instead of `ManagerCalendarWeek`'s static
 * hardcoded reference week. Each row also exposes the two real
 * booking-lifecycle actions the backend already supports — "تایید"
 * (confirm, PENDING only) and "تکمیل" (complete, CONFIRMED only) — via
 * [ManagerCalendarViewModel.confirmAppointment]/`.completeAppointment`,
 * this task's "Update booking status" priority.
 *
 * [ManagerCalendarAppointment.customerLabel] renders as "—": the backend
 * has no endpoint that resolves a booking's customer to a name (confirmed
 * absent — see `TEAM2_RESULT_MANAGER_DATA_PERSISTENCE.md`), so this is an
 * honest placeholder, not a redesign of the row.
 */
@Composable
fun ManagerCalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAppointmentClick: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    viewModel: ManagerCalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ManagerCalendarViewModelFactory(
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    LaunchedEffect(viewModel.requiresReauth) {
        if (viewModel.requiresReauth) onRequireLogin()
    }

    var viewMode by remember { mutableStateOf(ManagerCalendarViewMode.DAILY) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedSpecialistId by remember { mutableStateOf<String?>(null) }
    val dates = remember { RollingBookingDates.next7Days() }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item {
                RtlSectionHeader(
                    text = "تقویم",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (val calendarState = viewModel.state) {
                is UiState.Loading -> item { ManagerLoadingState(message = "در حال بارگذاری نوبت‌های سالن...") }
                is UiState.Error -> item {
                    ManagerErrorState(
                        description = calendarState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }
                is UiState.Empty -> item {
                    ManagerEmptyState(
                        title = "نوبتی ثبت نشده است",
                        description = "نوبت‌های ثبت‌شده برای سالن شما اینجا نمایش داده می‌شوند.",
                    )
                }
                is UiState.Success -> {
                    val calendarData = calendarState.data

                    item {
                        ViewModeToggle(
                            viewMode = viewMode,
                            onViewModeChange = { viewMode = it },
                        )
                    }

                    item {
                        SpecialistFilterRow(
                            specialists = calendarData.specialists,
                            selectedSpecialistId = selectedSpecialistId,
                            onSpecialistSelected = { selectedSpecialistId = it },
                        )
                    }

                    when (viewMode) {
                        ManagerCalendarViewMode.DAILY -> {
                            item {
                                DaySelectorRow(
                                    dates = dates,
                                    selectedDayIndex = selectedDayIndex,
                                    onDaySelected = { selectedDayIndex = it },
                                )
                            }

                            val dayKey = dates[selectedDayIndex].first
                            val dayAppointments = calendarData.appointments
                                .filter {
                                    it.dateKey == dayKey &&
                                        (selectedSpecialistId == null || it.specialistId == selectedSpecialistId)
                                }
                                .sortedBy { it.time }
                                .map { it.toDisplay() }

                            if (dayAppointments.isEmpty()) {
                                item { EmptyDayNotice() }
                            } else {
                                items(dayAppointments) { appointment ->
                                    AppointmentRow(
                                        appointment = appointment,
                                        onClick = { onAppointmentClick(appointment.id) },
                                        isUpdating = viewModel.updatingBookingId == appointment.id,
                                        onConfirm = { viewModel.confirmAppointment(appointment.id) },
                                        onComplete = { viewModel.completeAppointment(appointment.id) },
                                    )
                                }
                            }
                        }

                        ManagerCalendarViewMode.WEEKLY -> {
                            item {
                                WeeklyOverview(
                                    dates = dates,
                                    appointmentsByDayKey = calendarData.appointments.groupBy { it.dateKey },
                                    selectedSpecialistId = selectedSpecialistId,
                                    onDayClick = { dayIndex ->
                                        selectedDayIndex = dayIndex
                                        viewMode = ManagerCalendarViewMode.DAILY
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: ManagerCalendarViewMode,
    onViewModeChange: (ManagerCalendarViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        ToggleChip(
            label = "روزانه",
            icon = Icons.Filled.ViewDay,
            selected = viewMode == ManagerCalendarViewMode.DAILY,
            onClick = { onViewModeChange(ManagerCalendarViewMode.DAILY) },
            modifier = Modifier.weight(1f),
        )
        ToggleChip(
            label = "هفتگی",
            icon = Icons.Filled.ViewWeek,
            selected = viewMode == ManagerCalendarViewMode.WEEKLY,
            onClick = { onViewModeChange(ManagerCalendarViewMode.WEEKLY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ManagerGlassSurface(
        modifier = modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RojanIconContainer(
                imageVector = icon,
                contentDescription = label,
                size = RojanIconSize.Small,
                tint = if (selected) ManagerColors.Turquoise else ManagerColors.TextSecondary,
            )
            Text(
                text = label,
                style = RojanTypography.Body,
                color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                modifier = Modifier.padding(start = RojanDimens.SpaceXS),
            )
        }
    }
}

@Composable
private fun SpecialistFilterRow(
    specialists: List<Specialist>,
    selectedSpecialistId: String?,
    onSpecialistSelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        item {
            SpecialistChip(
                label = "همه",
                selected = selectedSpecialistId == null,
                onClick = { onSpecialistSelected(null) },
            )
        }
        items(specialists) { specialist ->
            SpecialistChip(
                label = specialist.displayName,
                selected = selectedSpecialistId == specialist.id,
                onClick = { onSpecialistSelected(specialist.id) },
            )
        }
    }
}

@Composable
private fun SpecialistChip(label: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            if (selected) {
                RojanIconContainer(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    size = RojanIconSize.Small,
                    tint = ManagerColors.Gold,
                )
            }
            Text(
                text = label,
                style = RojanTypography.Caption,
                color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun DaySelectorRow(dates: List<Pair<String, String>>, selectedDayIndex: Int, onDaySelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(dates.indices.toList()) { index ->
            val (_, label) = dates[index]
            val selected = index == selectedDayIndex
            ManagerGlassSurface(
                modifier = Modifier
                    .size(width = 88.dp, height = 56.dp)
                    .rojanPressable(onClick = { onDaySelected(index) }),
                shape = RojanShapes.Small,
                fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
                borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceXS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = label,
                        style = RojanTypography.Caption,
                        color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(
    appointment: CalendarAppointment,
    onClick: () -> Unit,
    isUpdating: Boolean,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                ManagerIconContainer(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    containerSize = 36.dp,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = appointment.clientName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                    Text(
                        text = "${appointment.service} · ${appointment.specialist}",
                        style = RojanTypography.Caption,
                        color = ManagerColors.TextSecondary,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = appointment.time, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(appointment.status.color, CircleShape),
                        )
                        Text(
                            text = appointment.status.label,
                            style = RojanTypography.Caption,
                            color = appointment.status.color,
                        )
                    }
                }
            }

            // TEAM2-002: real PATCH /confirm and /complete calls, the two
            // lifecycle transitions the backend already supports.
            if (appointment.rawStatus == BookingStatus.PENDING || appointment.rawStatus == BookingStatus.CONFIRMED) {
                Row(
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    if (appointment.rawStatus == BookingStatus.PENDING) {
                        Text(
                            text = if (isUpdating) "در حال تایید..." else "تایید نوبت",
                            style = RojanTypography.Caption,
                            color = ManagerColors.Turquoise,
                            modifier = Modifier.clickable(enabled = !isUpdating, onClick = onConfirm),
                        )
                    }
                    if (appointment.rawStatus == BookingStatus.CONFIRMED) {
                        Text(
                            text = if (isUpdating) "در حال تکمیل..." else "تکمیل نوبت",
                            style = RojanTypography.Caption,
                            color = ManagerColors.Gold,
                            modifier = Modifier.clickable(enabled = !isUpdating, onClick = onComplete),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayNotice() {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = "نوبتی برای این روز ثبت نشده است.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeeklyOverview(
    dates: List<Pair<String, String>>,
    appointmentsByDayKey: Map<String, List<ManagerCalendarAppointment>>,
    selectedSpecialistId: String?,
    onDayClick: (Int) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        items(dates.indices.toList()) { index ->
            val (dateKey, label) = dates[index]
            val dayAppointments = (appointmentsByDayKey[dateKey] ?: emptyList())
                .filter { selectedSpecialistId == null || it.specialistId == selectedSpecialistId }
                .sortedBy { it.time }
                .map { it.toDisplay() }

            ManagerGlassSurface(
                modifier = Modifier
                    .width(120.dp)
                    .height(220.dp)
                    .rojanPressable(onClick = { onDayClick(index) }),
                shape = RojanShapes.Small,
            ) {
                Column(modifier = Modifier.padding(RojanDimens.SpaceSM)) {
                    Text(
                        text = label,
                        style = RojanTypography.Caption,
                        color = ManagerColors.TextPrimary,
                    )
                    Column(
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        if (dayAppointments.isEmpty()) {
                            Text(
                                text = "خالی",
                                style = RojanTypography.Caption,
                                color = ManagerColors.TextSecondary,
                            )
                        } else {
                            dayAppointments.take(4).forEach { appointment ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(appointment.status.color, CircleShape),
                                    )
                                    Text(
                                        text = appointment.time,
                                        style = RojanTypography.Caption,
                                        color = ManagerColors.TextSecondary,
                                    )
                                }
                            }
                            if (dayAppointments.size > 4) {
                                Text(
                                    text = "+${dayAppointments.size - 4} مورد دیگر",
                                    style = RojanTypography.Caption,
                                    color = ManagerColors.Turquoise,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
