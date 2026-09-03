package ai.rojan.designlab.manager.presentation.services

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
 * Reliability Sprint 01, P0-2: same fix as
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * (P0-1), applied to
 * [ai.rojan.designlab.manager.screens.services.ManagerServicesScreen].
 * That screen previously read [ManagerRepositories.services] directly with
 * no load/error signal at all, relying entirely on the Dashboard having
 * already synced it — a real sync failure and a genuinely-empty catalog
 * were indistinguishable, both rendering the same "no services" empty
 * notice.
 *
 * There is no narrower per-repository re-sync available to call from here:
 * [ai.rojan.designlab.manager.data.BackendServiceRepository.sync] is
 * declared on that concrete class, not the
 * [ai.rojan.designlab.manager.domain.repository.ServiceRepository]
 * interface [ManagerRepositories.services] is typed as, so
 * [ManagerRepositories.initialize] (which re-syncs salon/services/
 * appointments/specialists/customers together) is the only re-load
 * mechanism this screen can reach — the same one
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * already calls. Re-syncing everything on Services entry is accepted,
 * known overhead (Dashboard's own re-entry already does the same), not an
 * oversight.
 *
 * [initialize] is injected exactly like P0-1's ViewModel, for the same two
 * reasons: unit-testability without a real [Context]/network, and to keep
 * [ManagerServicesViewModelFactory] the only place a real [Context] is
 * ever touched — must be `applicationContext`, never a raw Activity
 * [Context], to avoid leaking it across this screen's `NavBackStackEntry`
 * -scoped ViewModel outliving the Activity on a configuration change (the
 * exact bug found and fixed in P0-1's factory).
 */
class ManagerServicesViewModel(
    private val initialize: suspend () -> Result<Unit>,
) : ViewModel() {

    var state by mutableStateOf<ManagerServicesUiState>(ManagerServicesUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = ManagerServicesUiState.Loading
        viewModelScope.launch {
            initialize()
                .onSuccess { state = ManagerServicesUiState.Ready }
                .onFailure { error -> state = ManagerServicesUiState.Error(userMessageFor(error)) }
        }
    }

    /** Re-attempts the same load. Guarded against firing again while a load is already in flight — same reasoning as P0-1's [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel.retry]. */
    fun retry() {
        if (state is ManagerServicesUiState.Loading) return
        load()
    }
}

sealed interface ManagerServicesUiState {
    data object Loading : ManagerServicesUiState
    data class Error(val message: String) : ManagerServicesUiState
    data object Ready : ManagerServicesUiState
}

/** Manual factory for [ManagerServicesViewModel], same idiom as [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelFactory]. [context] must be an application context — see this class's own constructor doc. */
class ManagerServicesViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerServicesViewModel(initialize = { ManagerRepositories.initialize(context) }) as T
    }
}
