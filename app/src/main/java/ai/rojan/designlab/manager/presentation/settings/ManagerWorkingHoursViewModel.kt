package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.TimeInterval
import ai.rojan.designlab.manager.domain.repository.ManagerWorkingHoursRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One day's editable working-hours state.
 *
 * Deliberately scoped to a single interval per day, not the backend's full
 * `List<TimeInterval>` shape — the common pilot case (one open/close range
 * per day) — same kind of disclosed, documented simplification
 * [ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository]
 * already applies to `.firstOrNull()` on the owner's salon list. Data-safety
 * correction (Phase B Working Hours Correction): that simplification is
 * only safe to *save* when the backend day already has 0 or 1 interval —
 * [hasMultipleIntervals] flags a day the backend already holds 2+ intervals
 * for, so [ManagerWorkingHoursViewModel.saveDay] can refuse to `PUT` a
 * single interval over it (`SetWorkingHoursUseCase.execute` on the backend
 * is a full replace, not a merge — `ROJAN_Backend/application/.../schedule/WorkingHoursUseCases.kt`
 * — so that `PUT` would silently discard every interval past the first).
 * [hasExistingRecord] tracks whether the backend currently has a
 * `WorkingHours` row for this day at all, so [ManagerWorkingHoursViewModel.saveDay]
 * knows whether turning a day off needs a real `DELETE` call or is already
 * a no-op.
 */
data class WorkingDayFormState(
    val dayOfWeek: String,
    val isOpen: Boolean = false,
    val start: String = "",
    val end: String = "",
    val hasExistingRecord: Boolean = false,
    val hasMultipleIntervals: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * Owns loading + per-day form + per-day save state for
 * [ai.rojan.designlab.manager.screens.settings.ManagerWorkingHoursScreen] —
 * ViewModel+Factory, same shape
 * [ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModel]
 * already establishes for this Settings area (not the
 * [ai.rojan.designlab.manager.data.ManagerRepositories] global-singleton
 * pattern other, older Manager screens use).
 *
 * [salonId] is resolved once by the factory from
 * [ai.rojan.designlab.manager.data.ManagerRepositories.salonId] (already
 * synced by the time Settings is reachable from the Dashboard — same
 * precondition [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel]
 * already relies on for the same field) rather than re-derived here, so it
 * stays a plain constructor parameter for hermetic testing.
 */
class ManagerWorkingHoursViewModel(
    private val salonId: String?,
    private val repository: ManagerWorkingHoursRepository,
) : ViewModel() {

    private val _loadState = MutableStateFlow<UiState<List<WorkingDayFormState>>>(UiState.Loading)
    val loadState: StateFlow<UiState<List<WorkingDayFormState>>> = _loadState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val id = salonId
        if (id == null) {
            _loadState.value = UiState.Error("سالن فعال یافت نشد")
            return
        }
        _loadState.value = UiState.Loading
        viewModelScope.launch {
            repository.getWorkingHours(id)
                .onSuccess { hours ->
                    val byDay = hours.associateBy { it.dayOfWeek }
                    _loadState.value = UiState.Success(
                        DAY_ORDER.map { day -> byDay[day].toFormState(day) },
                    )
                }
                .onFailure { _loadState.value = UiState.Error(userMessageFor(it)) }
        }
    }

    fun onToggleOpen(dayOfWeek: String, isOpen: Boolean) = updateDay(dayOfWeek) { it.copy(isOpen = isOpen, error = null) }

    fun onStartChange(dayOfWeek: String, value: String) = updateDay(dayOfWeek) { it.copy(start = value) }

    fun onEndChange(dayOfWeek: String, value: String) = updateDay(dayOfWeek) { it.copy(end = value) }

    /**
     * Persists exactly one day: `PUT` with a single interval when open,
     * `DELETE` when closed and a backend record already exists, or a local
     * no-op when closed with nothing to remove — mirrors
     * [WorkingHoursController]'s real per-day contract, never a bulk save
     * across all seven days.
     *
     * Data-safety guard (Phase B Working Hours Correction): refuses to save
     * an *open* day flagged [WorkingDayFormState.hasMultipleIntervals] —
     * this single-interval editor has no way to represent interval 2+, so a
     * `PUT` here would silently replace the backend's full list with just
     * one interval. Closing the day (`DELETE`) is still allowed even when
     * multiple intervals exist — that's the owner explicitly removing the
     * whole day's record, not a silent partial collapse.
     *
     * Initial Default Weekly Schedule: when this is the very first day this
     * salon has EVER had a working-hours record for (every other day still
     * has [WorkingDayFormState.hasExistingRecord] `false`), a successful
     * save also seeds every other still-blank day with this same interval —
     * see [applyInitialDefaultToOtherDays]. This is a one-time bootstrap,
     * not a standing "copy this day everywhere" behavior: once any other
     * day already has a record (from this bootstrap or a later manual
     * edit), [isEstablishingInitialSchedule] is false on every subsequent
     * save and no propagation happens — an owner's later per-day
     * customization (including closing a day via `DELETE`) always goes
     * through this same single-day path untouched.
     */
    fun saveDay(dayOfWeek: String) {
        val id = salonId ?: return
        val days = currentDays() ?: return
        val day = days.find { it.dayOfWeek == dayOfWeek } ?: return
        if (day.isOpen && (day.start.isBlank() || day.end.isBlank())) return
        if (day.isOpen && day.hasMultipleIntervals) return

        val isEstablishingInitialSchedule = day.isOpen && days.none { it.hasExistingRecord }
        val initialInterval = TimeInterval(start = day.start.trim(), end = day.end.trim())

        updateDay(dayOfWeek) { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result: Result<SalonWorkingHours> = when {
                day.isOpen -> repository.setWorkingHours(
                    salonId = id,
                    dayOfWeek = dayOfWeek,
                    intervals = listOf(initialInterval),
                )
                day.hasExistingRecord -> repository.removeWorkingHours(id, dayOfWeek)
                    .map { SalonWorkingHours(dayOfWeek = dayOfWeek, intervals = emptyList()) }
                else -> Result.success(SalonWorkingHours(dayOfWeek = dayOfWeek, intervals = emptyList()))
            }

            result
                .onSuccess { updated ->
                    updateDay(dayOfWeek) { updated.toFormState(dayOfWeek).copy(isSaving = false) }
                    if (isEstablishingInitialSchedule) {
                        applyInitialDefaultToOtherDays(salonId = id, sourceDay = dayOfWeek, interval = initialInterval)
                    }
                }
                .onFailure { error -> updateDay(dayOfWeek) { it.copy(isSaving = false, error = userMessageFor(error)) } }
        }
    }

    /**
     * Seeds [interval] onto every day other than [sourceDay] that still has
     * no backend record ([WorkingDayFormState.hasExistingRecord] `false`),
     * re-checked fresh from current state right before each write — a day
     * that already has a record (an explicit prior customization, or
     * already seeded by this same call) is never touched. Each day is a
     * real, independent `PUT` through the exact same existing
     * [ManagerWorkingHoursRepository.setWorkingHours] contract
     * [saveDay] itself uses — Backend ends up with seven genuine per-day
     * records, indistinguishable from the owner having set each one by
     * hand, so Customer App/Web (both already reading the same
     * `GET .../working-hours`) see the seeded days with no changes needed
     * on their side.
     */
    private fun applyInitialDefaultToOtherDays(salonId: String, sourceDay: String, interval: TimeInterval) {
        val blankDays = currentDays()?.filter { it.dayOfWeek != sourceDay && !it.hasExistingRecord }.orEmpty()
        blankDays.forEach { blankDay ->
            val dayOfWeek = blankDay.dayOfWeek
            updateDay(dayOfWeek) { it.copy(isSaving = true, error = null) }
            viewModelScope.launch {
                repository.setWorkingHours(salonId = salonId, dayOfWeek = dayOfWeek, intervals = listOf(interval))
                    .onSuccess { updated -> updateDay(dayOfWeek) { updated.toFormState(dayOfWeek).copy(isSaving = false) } }
                    .onFailure { error -> updateDay(dayOfWeek) { it.copy(isSaving = false, error = userMessageFor(error)) } }
            }
        }
    }

    private fun currentDays(): List<WorkingDayFormState>? = (_loadState.value as? UiState.Success)?.data

    private fun updateDay(dayOfWeek: String, transform: (WorkingDayFormState) -> WorkingDayFormState) {
        val days = currentDays() ?: return
        _loadState.value = UiState.Success(days.map { if (it.dayOfWeek == dayOfWeek) transform(it) else it })
    }

    /**
     * [hasExistingRecord] is keyed off whether an interval is actually
     * present, not whether [this] is `null` — after a successful
     * `removeWorkingHours` call, [saveDay] passes a locally-constructed,
     * non-null `SalonWorkingHours(dayOfWeek, emptyList())` placeholder to
     * represent "no longer has a record," which a plain `this != null`
     * check would have misread as still having one.
     */
    private fun SalonWorkingHours?.toFormState(dayOfWeek: String): WorkingDayFormState {
        val intervals = this?.intervals.orEmpty()
        val firstInterval = intervals.firstOrNull()
        return WorkingDayFormState(
            dayOfWeek = dayOfWeek,
            isOpen = firstInterval != null,
            start = firstInterval?.start.orEmpty(),
            end = firstInterval?.end.orEmpty(),
            hasExistingRecord = firstInterval != null,
            hasMultipleIntervals = intervals.size > 1,
        )
    }

    companion object {
        /** `java.time.DayOfWeek` names, Persian-week order (Saturday first) — same order/labels `SalonDetailsScreen.kt`'s `toPersianDayLabel` already uses. */
        val DAY_ORDER = listOf("SATURDAY", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
    }
}
