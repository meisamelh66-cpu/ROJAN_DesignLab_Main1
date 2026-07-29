package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.components.ManagerAccent
import ai.rojan.designlab.manager.components.ManagerGlass
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
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

/** Status indicator — Teal/Gold extend naturally into "confirmed/pending"; [RojanErrorText] (existing token) covers "cancelled." */
private enum class AppointmentStatus(val label: String, val color: Color) {
    CONFIRMED("تایید شده", ManagerAccent.Teal),
    PENDING("در انتظار", ManagerAccent.Gold),
    CANCELLED("لغو شده", RojanErrorText),
}

/** Static placeholder appointment — no backend wired yet. */
private data class CalendarAppointment(
    val id: String,
    val time: String,
    val clientName: String,
    val service: String,
    val specialist: String,
    val status: AppointmentStatus,
)

private data class CalendarDay(val label: String, val dayNumber: String)

private val sampleWeekDays = listOf(
    CalendarDay("شنبه", "۲۵"),
    CalendarDay("یکشنبه", "۲۶"),
    CalendarDay("دوشنبه", "۲۷"),
    CalendarDay("سه‌شنبه", "۲۸"),
    CalendarDay("چهارشنبه", "۲۹"),
    CalendarDay("پنجشنبه", "۳۰"),
    CalendarDay("جمعه", "۱"),
)

private val sampleSpecialists = listOf("سارا کریمی", "مریم رضایی", "نگار احمدی")

/** Keyed by day index into [sampleWeekDays]. Static sample only — no backend. */
private val sampleAppointmentsByDay: Map<Int, List<CalendarAppointment>> = mapOf(
    0 to listOf(
        CalendarAppointment("a1", "۱۰:۰۰", "سارا محمدی", "رنگ مو", "سارا کریمی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("a2", "۱۱:۳۰", "نیلوفر احمدی", "میکاپ عروس", "مریم رضایی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("a3", "۱۲:۳۰", "پریسا کریمی", "مانیکور", "نگار احمدی", AppointmentStatus.PENDING),
        CalendarAppointment("a4", "۱۴:۰۰", "الناز حسینی", "کوتاهی مو", "سارا کریمی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("a5", "۱۶:۳۰", "مینا صادقی", "پاکسازی پوست", "مریم رضایی", AppointmentStatus.CANCELLED),
    ),
    1 to listOf(
        CalendarAppointment("b1", "۰۹:۳۰", "شیوا رستمی", "میکاپ", "نگار احمدی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("b2", "۱۳:۰۰", "دنیا فرهادی", "رنگ مو", "سارا کریمی", AppointmentStatus.PENDING),
    ),
    2 to listOf(
        CalendarAppointment("c1", "۱۰:۰۰", "آیدا مرادی", "مانیکور", "مریم رضایی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("c2", "۱۱:۰۰", "رویا نجفی", "کوتاهی مو", "نگار احمدی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("c3", "۱۵:۳۰", "بهار کاظمی", "پدیکور", "سارا کریمی", AppointmentStatus.PENDING),
    ),
    3 to listOf(
        CalendarAppointment("d1", "۱۲:۰۰", "ترانه یوسفی", "میکاپ عروس", "مریم رضایی", AppointmentStatus.CONFIRMED),
    ),
    4 to listOf(
        CalendarAppointment("e1", "۰۹:۰۰", "غزل امیری", "رنگ مو", "سارا کریمی", AppointmentStatus.CONFIRMED),
        CalendarAppointment("e2", "۱۴:۳۰", "سحر قاسمی", "مانیکور", "نگار احمدی", AppointmentStatus.PENDING),
    ),
    5 to emptyList(),
    6 to listOf(
        CalendarAppointment("g1", "۱۱:۰۰", "یاسمن رحیمی", "پاکسازی پوست", "مریم رضایی", AppointmentStatus.CONFIRMED),
    ),
)

/**
 * Manager App workspace — Calendar MVP. Additive-only: does not modify
 * [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]
 * or any of its components — built entirely from the same frozen
 * primitives ([ManagerScaffold]/`WarmBackground`, [GlassSurface] +
 * [ManagerGlass], [ManagerAccent] Teal+Gold, [RtlSectionHeader]).
 *
 * Static sample data only ("No backend" — same convention as the rest
 * of the Manager module). [onAppointmentClick] is the "appointment
 * detail entry point": inert (`{}` default) since no destination screen
 * or navigation wiring exists yet, matching this module's established
 * "no navigation changes" pattern.
 *
 * Specialist filter is a real, working local filter over the static
 * sample data (not just inert UI) — "foundation" for a future real
 * per-specialist data source, not a placeholder that does nothing.
 */
@Composable
fun ManagerCalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAppointmentClick: (String) -> Unit = {},
) {
    var viewMode by remember { mutableStateOf(ManagerCalendarViewMode.DAILY) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedSpecialist by remember { mutableStateOf<String?>(null) }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item {
                RtlSectionHeader(
                    text = "تقویم",
                    style = RojanTypography.ScreenTitle,
                    color = RojanGlassText,
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
                    selectedSpecialist = selectedSpecialist,
                    onSpecialistSelected = { selectedSpecialist = it },
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

                    val dayAppointments = (sampleAppointmentsByDay[selectedDayIndex] ?: emptyList())
                        .filter { selectedSpecialist == null || it.specialist == selectedSpecialist }

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
                            selectedSpecialist = selectedSpecialist,
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
    GlassSurface(
        modifier = modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        glassAlpha = if (selected) ManagerGlass.Alpha else ManagerGlass.SecondaryAlpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
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
                tint = if (selected) ManagerAccent.Teal else RojanTextOnDarkSurface,
            )
            Text(
                text = label,
                style = RojanTypography.Body,
                color = if (selected) RojanGlassText else RojanTextOnDarkSurface,
                modifier = Modifier.padding(start = RojanDimens.SpaceXS),
            )
        }
    }
}

@Composable
private fun SpecialistFilterRow(
    selectedSpecialist: String?,
    onSpecialistSelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        item {
            SpecialistChip(
                label = "همه",
                selected = selectedSpecialist == null,
                onClick = { onSpecialistSelected(null) },
            )
        }
        items(sampleSpecialists) { specialist ->
            SpecialistChip(
                label = specialist,
                selected = selectedSpecialist == specialist,
                onClick = { onSpecialistSelected(specialist) },
            )
        }
    }
}

@Composable
private fun SpecialistChip(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        glassAlpha = if (selected) ManagerGlass.Alpha else ManagerGlass.SecondaryAlpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
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
                    tint = ManagerAccent.Gold,
                )
            }
            Text(
                text = label,
                style = RojanTypography.Caption,
                color = if (selected) RojanGlassText else RojanTextOnDarkSurface,
            )
        }
    }
}

@Composable
private fun DaySelectorRow(selectedDayIndex: Int, onDaySelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(sampleWeekDays.indices.toList()) { index ->
            val day = sampleWeekDays[index]
            val selected = index == selectedDayIndex
            GlassSurface(
                modifier = Modifier
                    .size(width = 64.dp, height = 72.dp)
                    .rojanPressable(onClick = { onDaySelected(index) }),
                shape = RojanShapes.Small,
                glassAlpha = if (selected) ManagerGlass.Alpha else ManagerGlass.SecondaryAlpha,
                glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
                borderAlpha = ManagerGlass.BorderAlpha,
                borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceXS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day.label,
                        style = RojanTypography.Caption,
                        color = if (selected) RojanGlassText else RojanTextOnDarkSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = day.dayNumber,
                        style = RojanTypography.CardTitle,
                        color = if (selected) ManagerAccent.Teal else RojanGlassText,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: CalendarAppointment, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        glassAlpha = ManagerGlass.Alpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ManagerAccent.Teal.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                RojanIconContainer(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    size = RojanIconSize.Small,
                    tint = ManagerAccent.Teal,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = appointment.clientName, style = RojanTypography.Body, color = RojanGlassText)
                Text(
                    text = "${appointment.service} · ${appointment.specialist}",
                    style = RojanTypography.Caption,
                    color = RojanTextOnDarkSurface,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = appointment.time, style = RojanTypography.Caption, color = RojanTextOnDarkSurface)
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
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
        glassAlpha = ManagerGlass.Alpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
    ) {
        Text(
            text = "نوبتی برای این روز ثبت نشده است.",
            style = RojanTypography.Body,
            color = RojanTextOnDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeeklyOverview(
    selectedSpecialist: String?,
    onDayClick: (Int) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        items(sampleWeekDays.indices.toList()) { index ->
            val day = sampleWeekDays[index]
            val dayAppointments = (sampleAppointmentsByDay[index] ?: emptyList())
                .filter { selectedSpecialist == null || it.specialist == selectedSpecialist }

            GlassSurface(
                modifier = Modifier
                    .width(120.dp)
                    .height(220.dp)
                    .rojanPressable(onClick = { onDayClick(index) }),
                shape = RojanShapes.Small,
                glassAlpha = ManagerGlass.Alpha,
                glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
                borderAlpha = ManagerGlass.BorderAlpha,
                borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
            ) {
                Column(modifier = Modifier.padding(RojanDimens.SpaceSM)) {
                    Text(
                        text = "${day.label} ${day.dayNumber}",
                        style = RojanTypography.Caption,
                        color = RojanGlassText,
                    )
                    Column(
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        if (dayAppointments.isEmpty()) {
                            Text(
                                text = "خالی",
                                style = RojanTypography.Caption,
                                color = RojanTextOnDarkSurface,
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
                                        color = RojanTextOnDarkSurface,
                                    )
                                }
                            }
                            if (dayAppointments.size > 4) {
                                Text(
                                    text = "+${dayAppointments.size - 4} مورد دیگر",
                                    style = RojanTypography.Caption,
                                    color = ManagerAccent.Teal,
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
