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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
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
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()

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
            Text(text = "ساعات کاری", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary, modifier = Modifier.fillMaxWidth())
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
                // RTL fix (Pattern 4): canonical RTL settings-row order is
                // control-left/label-right, the mirror of this Row's
                // previous label-first/switch-second composition (which
                // packed label-left, switch-right under this app's fixed-LTR
                // Arrangement.SpaceBetween).
                Switch(
                    checked = day.isOpen,
                    onCheckedChange = onToggleOpen,
                    enabled = !day.isSaving,
                    colors = SwitchDefaults.colors(checkedTrackColor = ManagerColors.Turquoise),
                )
                Text(text = day.dayOfWeek.toPersianDayLabel(), style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
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
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (day.isOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    // RTL fix (Pattern 4): "پایان" (end) coded first so it
                    // renders on the left and "شروع" (start) second so it
                    // renders on the right - matching RTL reading order for
                    // a start-to-end range, the mirror of the previous
                    // شروع-then-پایان composition.
                    TimeField(
                        label = "پایان",
                        value = day.end,
                        onValueChange = onEndChange,
                        enabled = !day.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        label = "شروع",
                        value = day.start,
                        onValueChange = onStartChange,
                        enabled = !day.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(text = "تعطیل", style = RojanTypography.Caption, color = ManagerColors.TextSecondary, modifier = Modifier.fillMaxWidth())
            }

            if (day.error != null) {
                Text(text = day.error, style = RojanTypography.Caption, color = RojanErrorText, modifier = Modifier.fillMaxWidth())
            }

            if (!(day.isOpen && day.hasMultipleIntervals)) {
                ManagerPrimaryButton(
                    text = if (day.isSaving) "در حال ذخیره..." else "ذخیره",
                    enabled = !day.isSaving && (!day.isOpen || (day.start.isCompleteTime() && day.end.isCompleteTime())),
                    onClick = onSaveClick,
                )
            }
        }
    }
}

/**
 * HH:mm time entry — two independent RTL concerns resolved separately:
 *
 * 1. The floating label ("شروع"/"پایان") stays right-anchored via the same
 *    scoped [LayoutDirection.Rtl] wrap as every other Manager field
 *    (unrelated to the value's own direction below).
 * 2. The time VALUE is a structured numeric token, never Persian prose -
 *    it is deliberately NOT run through [ai.rojan.designlab.ui.text.withDirectionFor]
 *    (which would see a letter-less string like "15:30", fall through to
 *    that resolver's documented "no strong character -> Persian-first
 *    default RTL" branch, and place digits+colon inside an RTL paragraph -
 *    the exact mixed/broken bidi rendering reported on-device). The value
 *    gets an explicit, unconditional `TextDirection.Ltr` + `TextAlign.Center`
 *    instead, so "15:30" always renders clean and stable regardless of the
 *    surrounding Persian UI.
 *
 * Cursor/editing fix: this field keeps its own [TextFieldValue] (text +
 * selection), not just the plain [String] `value`. The earlier
 * implementation wired `onValueChange = { raw -> onValueChange(formatTimeInput(raw)) } }`
 * directly against a plain-`String` `OutlinedTextField` - every keystroke
 * replaced the whole displayed string with a freshly-rebuilt one (to
 * insert ':'/clamp/pad), and with no explicit selection, Compose's default
 * String-diffing guessed a new cursor position and frequently guessed
 * wrong, so the cursor "jumped" and manual editing/deletion felt broken.
 * [applyTimeMask] now computes the masked text AND an explicit, correct
 * cursor position together, so the displayed value can still change
 * underneath the user's typing (inserting ':', padding a single hour
 * digit) without ever moving the cursor anywhere but where the user's own
 * edit actually landed.
 *
 * [onValueChange] (the plain-`String` callback up to the ViewModel) is
 * still called with only the formatted text, so [WorkingDayFormState]/
 * persistence/default-propagation are completely unaffected by this -
 * this fix is local presentation/editing behavior only.
 */
@Composable
private fun TimeField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    var fieldValue by remember {
        val display = displayTimeValue(value)
        mutableStateOf(TextFieldValue(text = display, selection = TextRange(display.length)))
    }

    // Sync an EXTERNAL change (initial load, switching to a different day's
    // card, a value arriving back from a successful save) into the local
    // cursor-aware state. Never fires from the user's OWN edit below, since
    // `fieldValue.text` already equals the new `value` by the time this
    // recomposes with it.
    LaunchedEffect(value) {
        val display = displayTimeValue(value)
        if (fieldValue.text != display) {
            fieldValue = TextFieldValue(text = display, selection = TextRange(display.length))
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { typed ->
                val masked = applyTimeMask(typed)
                fieldValue = masked
                onValueChange(masked.text)
            },
            label = { Text(label) },
            placeholder = { Text("09:00") },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(
                color = ManagerColors.TextPrimary,
                textDirection = TextDirection.Ltr,
                textAlign = TextAlign.Center,
            ),
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
}

/**
 * Takes the field's raw post-keystroke [TextFieldValue] (whatever Android's
 * text editing just produced - an insertion, a deletion, a pasted/replaced
 * selection, anywhere in the string) and returns the masked text paired
 * with a cursor position mapped to the same editing point, so the cursor
 * never jumps to an unrelated spot just because the displayed text
 * changed shape (colon inserted, a single hour digit padded).
 *
 * The mapping is digit-position-based: count how many digits sit before
 * the user's cursor in the text they just produced, find the same-numbered
 * digit in the newly-formatted text, and place the cursor right after it.
 * [formatTimeInput] can insert exactly one extra digit that was never
 * typed (the padding zero for a single-digit hour, e.g. "9" -> "09") -
 * [paddingOffset] accounts for that one shift so the cursor still lands
 * after the digit the user actually just typed, not one short of it.
 */
internal fun applyTimeMask(typed: TextFieldValue): TextFieldValue {
    val cursorIndex = typed.selection.end.coerceIn(0, typed.text.length)
    val rawDigits = typed.text.filter(Char::isDigit).take(4)
    val digitsBeforeCursor = typed.text.take(cursorIndex).count(Char::isDigit).coerceAtMost(rawDigits.length)

    val formatted = formatTimeInput(typed.text)
    val formattedDigitCount = formatted.count(Char::isDigit)
    val paddingOffset = formattedDigitCount - rawDigits.length

    val targetDigitCount = if (digitsBeforeCursor == 0) 0 else (digitsBeforeCursor + paddingOffset).coerceAtMost(formattedDigitCount)
    return TextFieldValue(text = formatted, selection = TextRange(positionAfterDigits(formatted, targetDigitCount)))
}

/** The character index in [text] right after its [digitCount]-th digit (e.g. `positionAfterDigits("09:30", 3) == 4`, right after the "3"). */
private fun positionAfterDigits(text: String, digitCount: Int): Int {
    if (digitCount <= 0) return 0
    var seen = 0
    for (i in text.indices) {
        if (text[i].isDigit()) {
            seen++
            if (seen == digitCount) return i + 1
        }
    }
    return text.length
}

/**
 * Progressive HH:mm digit mask: the user types only plain digits (max 4,
 * extra keystrokes are dropped); the ':' is inserted automatically and
 * each completed segment is clamped to a valid range (hour 00-23, minute
 * 00-59) so an out-of-range value can never be typed. A 3-digit total
 * (e.g. "930") is read as a single-digit hour auto-padded to two digits
 * plus a 2-digit minute ("09:30"); a 4-digit total (e.g. "1530") is read
 * as a 2-digit hour plus a 2-digit minute ("15:30") - a manager never
 * needs to type a leading zero or the colon itself.
 */
internal fun formatTimeInput(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    return when (digits.length) {
        0, 1, 2 -> digits
        3 -> {
            val hour = digits.substring(0, 1).toInt()
            val minute = digits.substring(1, 3).toInt().coerceAtMost(59)
            "%02d:%02d".format(hour, minute)
        }
        else -> {
            val hour = digits.substring(0, 2).toInt().coerceAtMost(23)
            val minute = digits.substring(2, 4).toInt().coerceAtMost(59)
            "%02d:%02d".format(hour, minute)
        }
    }
}

/**
 * The backend returns times as "HH:mm:ss" (`WorkingHoursDtos.kt`'s own doc
 * comment: e.g. "09:00:00") while this field edits/displays "HH:mm" - an
 * already-saved day loaded fresh from the backend is truncated to its
 * first 5 characters for display only. [WorkingDayFormState.start]/`.end`
 * themselves are NOT touched by this - if the manager never edits the
 * field, the untouched original "HH:mm:ss" string is still exactly what
 * gets re-sent on save, unchanged from this field's pre-existing behavior.
 */
internal fun displayTimeValue(raw: String): String =
    if (Regex("""^\d{2}:\d{2}:\d{2}$""").matches(raw)) raw.take(5) else raw

/** A fully-typed, valid time value - either this field's own "HH:mm" shape or a backend-sourced "HH:mm:ss" the manager never touched - distinct from a still-in-progress 1-2 digit prefix (no colon yet) that [formatTimeInput] hasn't completed. */
internal fun String.isCompleteTime(): Boolean = Regex("""^\d{2}:\d{2}(:\d{2})?$""").matches(this)

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
