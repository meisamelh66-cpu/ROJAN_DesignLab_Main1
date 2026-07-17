package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.usecase.ObserveSelectedRoleUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Whether the persisted role has finished loading from storage yet. */
sealed interface SessionRestoreState {
    /** Still reading from DataStore — the very first frame(s) after launch only. */
    data object Loading : SessionRestoreState

    /** Finished reading; [role] is `null` for a first-time user, non-null for a returning one. */
    data class Restored(val role: Role?) : SessionRestoreState
}

/**
 * Restores the previously selected [Role] (if any) once per app launch,
 * so a returning user is routed straight to their dashboard instead of
 * seeing the Welcome/role-selection screen again.
 *
 * Production fix (physical-device launch freeze): previously this used
 * `observeSelectedRole().stateIn(...)` directly — if the upstream Flow
 * ever failed *or* simply never emitted for some reason not covered by
 * [ai.rojan.designlab.data.repository.RoleRepositoryImpl]'s own recovery,
 * [restoreState] would stay at `Loading` forever, with nothing else in
 * the app able to notice or recover. This class now adds a second,
 * independent layer of protection on top of that repository-level fix:
 * a hard timeout. If no value has arrived within
 * [SESSION_RESTORE_TIMEOUT_MS], restoration fails safe to
 * `Restored(role = null)` — the same state a genuine first-time user
 * produces — so [ai.rojan.designlab.navigation.RojanNavGraph] proceeds to
 * Welcome instead of freezing. This is deliberately independent of the
 * repository-level fix: either one alone would have prevented the
 * originally-diagnosed freeze, and keeping both means a *different*,
 * not-yet-seen failure mode still can't reproduce it.
 */
class SessionViewModel(
    observeSelectedRole: ObserveSelectedRoleUseCase,
) : ViewModel() {

    private val _restoreState =
        MutableStateFlow<SessionRestoreState>(SessionRestoreState.Loading)

    val restoreState: StateFlow<SessionRestoreState> = _restoreState.asStateFlow()

    init {
        viewModelScope.launch {

            // Timeout guard: if the first role value hasn't arrived by
            // the deadline, fail safe to "no role" rather than staying on
            // Loading forever. compareAndSet only applies this fallback
            // if restoreState is still genuinely Loading — if the real
            // value already arrived first, this is a harmless no-op.
            val timeoutGuard = launch {
                delay(SESSION_RESTORE_TIMEOUT_MS)
                _restoreState.compareAndSet(
                    SessionRestoreState.Loading,
                    SessionRestoreState.Restored(role = null)
                )
            }

            observeSelectedRole()
                .catch { throwable ->
                    // Defense in depth: RoleRepositoryImpl already recovers
                    // from read failures on its own, but if some future
                    // change to that layer regresses this, session
                    // restoration still must not die here either.
                    // CancellationException is never swallowed.
                    if (throwable is CancellationException) throw throwable
                    emit(null)
                }
                .collect { role ->
                    timeoutGuard.cancel()
                    _restoreState.value = SessionRestoreState.Restored(role)
                }
        }
    }

    private companion object {
        /**
         * Generous relative to a normal DataStore read (typically single
         * milliseconds), but short enough that a genuinely broken restore
         * doesn't leave a real user staring at a loading screen for long.
         */
        const val SESSION_RESTORE_TIMEOUT_MS = 5_000L
    }
}
