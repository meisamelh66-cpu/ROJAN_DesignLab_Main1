package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.settings.ManagerWorkingHoursViewModel
import ai.rojan.designlab.manager.presentation.settings.WorkingDayFormState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Owner working-hours edit (Owner Salon Profile
 * Completion, Android-only). Reached from
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen] via
 * the new "ساعات کاری" entry point. Backend contract for `GET`/`PUT`/
 * `DELETE .../working-hours/{dayOfWeek}` already exists and works (per
 * `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §2/§7) — the only
 * gap this screen closes is Android write UI, zero backend dependency.
 *
 * Each of the seven days saves independently (its own `PUT`/`DELETE` call,
 * its own loading/error state) — there is no single bulk-save action,
 * matching the backend's real per-day endpoint shape.
 */
@Composable
fun ManagerWorkingHoursScreen(
    viewModel: ManagerWorkingHoursViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val loadState by viewModel.loadState.collectAsState()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val state = loadState) {
            // Empty is unreachable here (getWorkingHours never yields it) - grouped
            // with Loading only to keep this `when` exhaustive over UiState's 4 cases.
            is UiState.Loading, is UiState.Empty -> WorkingHoursLoading()
            is UiState.Error -> WorkingHoursLoadError(message = state.message, onRetry = viewModel::load)
            is UiState.Success -> WorkingHoursForm(
                days = state.data,
                onToggleOpen = viewModel::onToggleOpen,
                onStartChange = viewModel::onStartChange,
                onEndChange = viewModel::onEndChange,
                onSaveDayClick = viewModel::saveDay,
            )
        }
    }
}

@Composable
private fun WorkingHoursLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun WorkingHoursLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = RojanTypography.Body, color = RojanErrorText)
        ManagerPrimaryButton(text = "تلاش مجدد", onClick = onRetry)
    }
}

@Composable
private fun WorkingHoursForm(
    days: List<WorkingDayFormState>,
    onToggleOpen: (String, Boolean) -> Unit,
    onStartChange: (String, String) -> Unit,
    onEndChange: (String, String) -> Unit,
    onSaveDayClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        item {
            Text(text = "ساعات کاری", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary)
        }

        items(days, key = { it.dayOfWeek }) { day ->
            WorkingDayCard(
                day = day,
                onToggleOpen = { isOpen -> onToggleOpen(day.dayOfWeek, isOpen) },
                onStartChange = { value -> onStartChange(day.dayOfWeek, value) },
                onEndChange = { value -> onEndChange(day.dayOfWeek, value) },
                onSaveClick = { onSaveDayClick(day.dayOfWeek) },
            )
        }
    }
}

@Composable
private fun WorkingDayCard(
    day: WorkingDayFormState,
    onToggleOpen: (Boolean) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = day.dayOfWeek.toPersianDayLabel(), style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                Switch(
                    checked = day.isOpen,
                    onCheckedChange = onToggleOpen,
                    enabled = !day.isSaving,
                    colors = SwitchDefaults.colors(checkedTrackColor = ManagerColors.Turquoise),
                )
            }

            if (day.isOpen && day.hasMultipleIntervals) {
                // Data-safety guard (Phase B Working Hours Correction): this
                // editor only reads/writes one interval - showing it here
                // would invite a save that silently discards the backend's
                // other interval(s). No editor, no destructive Save below.
                Text(
                    text = "این روز چند بازه کاری در سرور ثبت شده و امکان ویرایش از این صفحه وجود ندارد.",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            } else if (day.isOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    TimeField(
                        label = "شروع",
                        value = day.start,
                        onValueChange = onStartChange,
                        enabled = !day.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        label = "پایان",
                        value = day.end,
                        onValueChange = onEndChange,
                        enabled = !day.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(text = "تعطیل", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }

            if (day.error != null) {
                Text(text = day.error, style = RojanTypography.Caption, color = RojanErrorText)
            }

            if (!(day.isOpen && day.hasMultipleIntervals)) {
                ManagerPrimaryButton(
                    text = if (day.isSaving) "در حال ذخیره..." else "ذخیره",
                    enabled = !day.isSaving && (!day.isOpen || (day.start.isNotBlank() && day.end.isNotBlank())),
                    onClick = onSaveClick,
                )
            }
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("09:00") },
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = ManagerColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ManagerColors.TextPrimary,
            unfocusedTextColor = ManagerColors.TextPrimary,
            focusedBorderColor = ManagerColors.Turquoise,
            unfocusedBorderColor = ManagerColors.TextSecondary,
            focusedLabelColor = ManagerColors.Turquoise,
            unfocusedLabelColor = ManagerColors.TextSecondary,
            cursorColor = ManagerColors.Turquoise,
        ),
        modifier = modifier,
    )
}

/** `java.time.DayOfWeek`'s English enum name, as returned by the backend — same labels [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s own private `toPersianDayLabel` already uses, duplicated here rather than shared since that one is Customer-module-private. */
private fun String.toPersianDayLabel(): String = when (this) {
    "SATURDAY" -> "شنبه"
    "SUNDAY" -> "یکشنبه"
    "MONDAY" -> "دوشنبه"
    "TUESDAY" -> "سه‌شنبه"
    "WEDNESDAY" -> "چهارشنبه"
    "THURSDAY" -> "پنجشنبه"
    "FRIDAY" -> "جمعه"
    else -> this
}
