package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.formatDurationMinutes
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Appointment Detail (Manager Operational
 * Foundation, Phase 6 Step 4): **read-only**. Reached from
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]'s
 * already-existing `onAppointmentClick` (previously unwired — this step's
 * only Calendar-adjacent change is that one navigation wire, in
 * `ManagerNavGraph.kt`; this screen and Calendar itself are otherwise
 * untouched).
 *
 * Deliberately has no status-change, cancel, or reschedule action: the
 * backend has no owner-side booking-mutation endpoint at all today
 * (`ManagerBookingApi` only has `list`/`createForCustomer` — confirmed by
 * direct inspection, not assumed). [ai.rojan.designlab.manager.data.BackendAppointmentRepository.update]/
 * `updateStatus`/`cancel` exist but are local-cache-only; wiring a button
 * to them here would look like it works while silently doing nothing real
 * on the backend, which is exactly what this screen avoids.
 *
 * Resolves [Appointment.customerId]/[serviceId]/[specialistId] to display
 * data via [ManagerRepositories], the identical resolution
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen.toDisplay]
 * already performs per row - no new lookup mechanism. Layout mirrors
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]
 * (identity header + stacked sections), the closer precedent for a
 * read-only single-record view than the Step 2/3 create/edit screens.
 */
@Composable
fun ManagerAppointmentDetailScreen(
    modifier: Modifier = Modifier,
    appointmentId: String,
    onBackClick: (() -> Unit)? = null,
) {
    val appointment = ManagerRepositories.appointments.getById(appointmentId)

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        if (appointment == null) {
            return@ManagerScaffold
        }

        val customer = ManagerRepositories.customers.getById(appointment.customerId)
        val service = ManagerRepositories.services.getById(appointment.serviceId)
        val specialist = ManagerRepositories.specialists.getById(appointment.specialistId)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { AppointmentIdentityHeader(appointment, customerName = customer?.name ?: "—") }
            item {
                ServiceSpecialistSection(
                    serviceName = service?.name ?: "—",
                    price = service?.price,
                    specialistName = specialist?.name ?: "—",
                )
            }
            item { DateTimeSection(appointment) }
            if (!appointment.notes.isNullOrBlank()) {
                item { NotesSection(appointment.notes) }
            }
        }
    }
}

/** Status label/color — mirrors [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]'s own private mapping (kept as a small, local duplicate rather than touching that unrelated file for one shared constant). */
private fun AppointmentStatus.displayLabelAndColor(): Pair<String, Color> = when (this) {
    AppointmentStatus.CONFIRMED -> "تایید شده" to ManagerColors.Turquoise
    AppointmentStatus.PENDING -> "در انتظار" to ManagerColors.Gold
    AppointmentStatus.COMPLETED -> "انجام شده" to ManagerColors.Turquoise
    AppointmentStatus.CANCELLED -> "لغو شده" to RojanErrorText
    AppointmentStatus.NO_SHOW -> "عدم حضور" to RojanErrorText
}

@Composable
private fun AppointmentIdentityHeader(appointment: Appointment, customerName: String) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ManagerColors.Turquoise.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = customerName.take(1),
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TurquoiseLight,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = customerName, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)

                val (statusLabel, statusColor) = appointment.status.displayLabelAndColor()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Text(text = statusLabel, style = RojanTypography.Caption, color = statusColor)
                }
            }
        }
    }
}

@Composable
private fun ServiceSpecialistSection(serviceName: String, price: Long?, specialistName: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "خدمت و متخصص",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Column(
                modifier = Modifier.padding(RojanDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                    ManagerIconContainer(imageVector = Icons.Filled.ContentCut, contentDescription = null, containerSize = 36.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = serviceName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                        if (price != null) {
                            Text(text = formatTomanPrice(price), style = RojanTypography.Caption, color = ManagerColors.GoldLight)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                    ManagerIconContainer(imageVector = Icons.Filled.Person, contentDescription = null, containerSize = 36.dp)
                    Text(text = specialistName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun DateTimeSection(appointment: Appointment) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "تاریخ و ساعت",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                RojanIconContainer(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    size = RojanIconSize.Medium,
                    tint = ManagerColors.Turquoise,
                )
                Column {
                    Text(text = appointment.date, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                    val timeRange = if (appointment.endTime != null) {
                        "${appointment.time} - ${appointment.endTime}"
                    } else {
                        appointment.time
                    }
                    Text(text = timeRange, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun NotesSection(notes: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "یادداشت",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Text(
                text = notes,
                style = RojanTypography.Body,
                color = ManagerColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerAppointmentDetailScreenPreview() {
    RojanTheme {
        ManagerAppointmentDetailScreen(appointmentId = "a1")
    }
}
