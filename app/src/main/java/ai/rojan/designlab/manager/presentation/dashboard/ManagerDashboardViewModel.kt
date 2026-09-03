package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.presentation.common.userMessageFor
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Reliability Sprint 01, P0-1: makes [ManagerRepositories.initialize]'s
 * [Result] visible and retryable instead of the previous
 * [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]
 * behavior of firing it from a bare `LaunchedEffect` and discarding the
 * return value — a genuine load failure (network down, backend error)
 * was indistinguishable from "salon genuinely has zero appointments
 * today," since every downstream section just kept reading
 * [ManagerRepositories]'s empty defaults either way.
 *
 * Scope is deliberately narrow: this only wraps the one real failure
 * point ([initialize] itself). Dashboard's actual content (salon
 * identity, today's overview, AI insight, calendar preview) still reads
 * [ManagerRepositories] directly once [state] is
 * [ManagerDashboardUiState.Ready] — wrapping every downstream read in
 * its own state too is a larger, separate concern (the `ManagerRepositories`
 * singleton-bypass pattern tracked as R-02), not this task's job.
 *
 * [initialize] is injected as a lambda rather than calling
 * [ManagerRepositories.initialize] directly so this class is unit-testable
 * without a real [Context]/network — the smallest seam available:
 * [ManagerRepositories] itself is untouched, only this ViewModel's
 * constructor takes the call as a parameter
 * ([ManagerDashboardViewModelFactory] supplies the real one).
 */
class ManagerDashboardViewModel(
    private val initialize: suspend () -> Result<Unit>,
) : ViewModel() {

    var state by mutableStateOf<ManagerDashboardUiState>(ManagerDashboardUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = ManagerDashboardUiState.Loading
        viewModelScope.launch {
            initialize()
                .onSuccess { state = ManagerDashboardUiState.Ready }
                .onFailure { error -> state = ManagerDashboardUiState.Error(userMessageFor(error)) }
        }
    }

    /**
     * Re-attempts the same load. Guarded against firing again while a load
     * is already in flight — the retry button unmounts the instant [state]
     * becomes [ManagerDashboardUiState.Loading] (it only exists in the
     * [ManagerDashboardUiState.Error] branch), but this guard covers the
     * same-frame double-tap window before that recomposition happens.
     */
    fun retry() {
        if (state is ManagerDashboardUiState.Loading) return
        load()
    }
}

sealed interface ManagerDashboardUiState {
    data object Loading : ManagerDashboardUiState
    data class Error(val message: String) : ManagerDashboardUiState
    data object Ready : ManagerDashboardUiState
}

/** Manual factory for [ManagerDashboardViewModel], same idiom as [ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModelFactory]. */
class ManagerDashboardViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerDashboardViewModel(initialize = { ManagerRepositories.initialize(context) }) as T
    }
}
