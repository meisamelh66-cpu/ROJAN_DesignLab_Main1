package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus as DomainAppointmentStatus
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private enum class ManagerCalendarViewMode { DAILY, WEEKLY }

/** Status indicator — Turquoise/Gold extend naturally into "confirmed/pending"; [RojanErrorText] (existing token) covers "cancelled/no-show." */
private enum class AppointmentStatus(val label: String, val color: Color) {
    CONFIRMED("تایید شده", ManagerColors.Turquoise),
    PENDING("در انتظار", ManagerColors.Gold),
    COMPLETED("انجام شده", ManagerColors.Turquoise),
    CANCELLED("لغو شده", RojanErrorText),
    NO_SHOW("عدم حضور", RojanErrorText),
}

private fun DomainAppointmentStatus.toDisplayStatus(): AppointmentStatus = when (this) {
    DomainAppointmentStatus.PENDING -> AppointmentStatus.PENDING
    DomainAppointmentStatus.CONFIRMED -> AppointmentStatus.CONFIRMED
    DomainAppointmentStatus.COMPLETED -> AppointmentStatus.COMPLETED
    DomainAppointmentStatus.CANCELLED -> AppointmentStatus.CANCELLED
    DomainAppointmentStatus.NO_SHOW -> AppointmentStatus.NO_SHOW
}

/** Display row for one real [Appointment] — customer/service/specialist ids resolved to names via [ManagerRepositories]. */
private data class CalendarAppointment(
    val id: String,
    val time: String,
    val clientName: String,
    val service: String,
    val specialist: String,
    val status: AppointmentStatus,
)

private fun Appointment.toDisplay(): CalendarAppointment = CalendarAppointment(
    id = id,
    time = time,
    clientName = ManagerRepositories.customers.getById(customerId)?.name ?: "—",
    service = ManagerRepositories.services.getById(serviceId)?.name ?: "—",
    specialist = ManagerRepositories.specialists.getById(specialistId)?.name ?: "—",
    status = status.toDisplayStatus(),
)

/**
 * Manager App workspace — Calendar MVP. Additive-only: does not modify
 * [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]
 * or any of its components.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerScaffold]/[ManagerGlassSurface]/
 * [ManagerIconContainer]) — content/data/navigation unchanged.
 *
 * Reads [ManagerRepositories.appointments]/`.customers`/`.services`/
 * `.specialists` directly (same in-memory singletons the booking wizard's
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.confirm]
 * writes to) instead of a screen-local hardcoded sample set, so a newly
 * booked appointment shows up here as soon as this screen is (re)entered.
 */
@Composable
fun ManagerCalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAppointmentClick: (String) -> Unit = {},
) {
    var viewMode by remember { mutableStateOf(ManagerCalendarViewMode.DAILY) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedSpecialistId by remember { mutableStateOf<String?>(null) }

    // Read directly on every recomposition (no remember) so an appointment
    // just created via the booking wizard (ManagerBookingViewModel.confirm(),
    // same ManagerRepositories.appointments instance) shows up as soon as
    // this screen is (re)entered, instead of the screen-local hardcoded
    // sample set this used to render regardless of what was actually booked.
    val specialists = ManagerRepositories.specialists.getAll()
    val appointmentsByDayKey = ManagerRepositories.appointments.getAll().groupBy { it.date }

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

            item {
                ViewModeToggle(
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                )
            }

            item {
                SpecialistFilterRow(
                    specialists = specialists,
                    selectedSpecialistId = selectedSpecialistId,
                    onSpecialistSelected = { selectedSpecialistId = it },
                )
            }

            when (viewMode) {
                ManagerCalendarViewMode.DAILY -> {
                    item {
                        DaySelectorRow(
                            selectedDayIndex = selectedDayIndex,
                            onDaySelected = { selectedDayIndex = it },
                        )
                    }

                    val dayKey = ManagerCalendarWeek.days[selectedDayIndex].key
                    val dayAppointments = (appointmentsByDayKey[dayKey] ?: emptyList())
                        .filter { selectedSpecialistId == null || it.specialistId == selectedSpecialistId }
                        .sortedBy { it.time }
                        .map { it.toDisplay() }

                    if (dayAppointments.isEmpty()) {
                        item { EmptyDayNotice() }
                    } else {
                        items(dayAppointments) { appointment ->
                            AppointmentRow(
                                appointment = appointment,
                                onClick = { onAppointmentClick(appointment.id) },
                            )
                        }
                    }
                }

                ManagerCalendarViewMode.WEEKLY -> {
                    item {
                        WeeklyOverview(
                            appointmentsByDayKey = appointmentsByDayKey,
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
                label = specialist.name,
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
private fun DaySelectorRow(selectedDayIndex: Int, onDaySelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(ManagerCalendarWeek.days.indices.toList()) { index ->
            val day = ManagerCalendarWeek.days[index]
            val selected = index == selectedDayIndex
            ManagerGlassSurface(
                modifier = Modifier
                    .size(width = 64.dp, height = 72.dp)
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
                        text = day.label,
                        style = RojanTypography.Caption,
                        color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = day.dayNumber,
                        style = RojanTypography.CardTitle,
                        color = if (selected) ManagerColors.Turquoise else ManagerColors.TextPrimary,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: CalendarAppointment, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
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
    appointmentsByDayKey: Map<String, List<Appointment>>,
    selectedSpecialistId: String?,
    onDayClick: (Int) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        items(ManagerCalendarWeek.days.indices.toList()) { index ->
            val day = ManagerCalendarWeek.days[index]
            val dayAppointments = (appointmentsByDayKey[day.key] ?: emptyList())
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
                        text = "${day.label} ${day.dayNumber}",
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

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerCalendarScreenPreview() {
    RojanTheme {
        ManagerCalendarScreen()
    }
}
