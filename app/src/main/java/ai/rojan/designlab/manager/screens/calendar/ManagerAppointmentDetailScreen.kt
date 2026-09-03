package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.formatDurationMinutes
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModel
import ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModelFactory
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — Appointment Detail (Manager Operational
 * Foundation, Phase 6 Step 4; confirm/cancel/complete actions added for
 * the Android Pilot P0 gap). Reached from
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]'s
 * `onAppointmentClick`, via `ManagerNavGraph.kt`.
 *
 * Status-change actions are wired through
 * [ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModel],
 * which calls the real, existing `PATCH .../bookings/{id}/confirm|cancel|complete`
 * endpoints via the flavor-agnostic
 * [ai.rojan.designlab.domain.repository.BookingRepository] — the same
 * mechanism [ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModel]
 * already uses. See [ActionsSection] below for the per-status button set
 * and the cancel confirmation dialog (mirrors the one existing dialog
 * precedent in the codebase, `AppointmentsScreen.kt`'s cancel-confirm).
 *
 * Resolves [Appointment.customerId]/[serviceId]/[specialistId] to display
 * data via [ManagerRepositories], the identical resolution
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen.toDisplay]
 * already performs per row - no new lookup mechanism. Layout mirrors
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]
 * (identity header + stacked sections), the closer precedent for a
 * single-record view than the Step 2/3 create/edit screens.
 */
@Composable
fun ManagerAppointmentDetailScreen(
    modifier: Modifier = Modifier,
    appointmentId: String,
    onBackClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel: ManagerAppointmentDetailViewModel = viewModel(
        factory = ManagerAppointmentDetailViewModelFactory(appointmentId, context),
    )
    val appointment by viewModel.appointment.collectAsState()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        if (appointment == null) {
            return@ManagerScaffold
        }
        val current = appointment!!

        val customer = ManagerRepositories.customers.getById(current.customerId)
        val service = ManagerRepositories.services.getById(current.serviceId)
        val specialist = ManagerRepositories.specialists.getById(current.specialistId)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { AppointmentIdentityHeader(current, customerName = customer?.name ?: "—") }
            item {
                ServiceSpecialistSection(
                    serviceName = service?.name ?: "—",
                    price = service?.price,
                    specialistName = specialist?.name ?: "—",
                )
            }
            item { DateTimeSection(current) }
            if (!current.notes.isNullOrBlank()) {
                item { NotesSection(current.notes) }
            }
            item {
                ActionsSection(
                    status = current.status,
                    isSubmitting = viewModel.isSubmitting,
                    actionError = viewModel.actionError,
                    onConfirm = viewModel::confirm,
                    onComplete = viewModel::complete,
                    onCancel = viewModel::cancel,
                )
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

/**
 * Confirm/cancel/complete — the button set depends on [status]: `PENDING`
 * gets "تایید"/"لغو نوبت", `CONFIRMED` gets "انجام شد"/"لغو نوبت",
 * `COMPLETED`/`CANCELLED`/`NO_SHOW` are terminal and get none. Cancel is
 * the one destructive action, so it goes through an [AlertDialog]
 * confirmation first — same pattern as the Customer app's cancel flow in
 * `AppointmentsScreen.kt`; confirm/complete are non-destructive promotions
 * and fire directly.
 */
@Composable
private fun ActionsSection(
    status: AppointmentStatus,
    isSubmitting: Boolean,
    actionError: String?,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
        return
    }

    var showCancelConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (actionError != null) {
            Text(
                text = actionError,
                style = RojanTypography.Caption,
                color = RojanErrorText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RojanDimens.SpaceSM),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ManagerPrimaryButton(
                text = when {
                    isSubmitting -> "در حال ثبت..."
                    status == AppointmentStatus.PENDING -> "تایید"
                    else -> "انجام شد"
                },
                onClick = if (status == AppointmentStatus.PENDING) onConfirm else onComplete,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f),
            )

            TextButton(onClick = { showCancelConfirm = true }, enabled = !isSubmitting) {
                Text(text = "لغو نوبت", color = RojanErrorText)
            }
        }
    }

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
