package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
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
 * P1-5, Manager Specialist <-> Service Assignment: owns the Loading/Error/
 * Ready lifecycle for [ai.rojan.designlab.manager.screens.staff.ManagerSpecialistServicesScreen]
 * plus per-service assign/unassign mutations, via the real
 * `PUT`/`DELETE .../specialists/{id}/services/{serviceId}` endpoints
 * confirmed against backend source this sprint.
 *
 * [initialize] re-syncs the salon's real service catalog through
 * [ManagerRepositories.initialize] before reading it — same pattern
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]/
 * [ai.rojan.designlab.manager.presentation.services.ManagerServicesViewModel]
 * already established (P0-1/P0-2), and the same accepted trade-off: this
 * re-syncs more than just services, but there is no narrower catalog-only
 * refresh call exposed today.
 *
 * [specialistRepository] and [getServices] are injected as lambdas rather
 * than captured instances — [ManagerRepositories.specialists] is
 * *reassigned* by [ManagerRepositories.initialize] (a fresh
 * [ai.rojan.designlab.manager.data.BackendSpecialistRepository] each
 * time), so a captured reference taken before [load] calls [initialize]
 * could otherwise point at a stale instance. Resolving both fresh at each
 * call site avoids that trap structurally, and keeps this class fully
 * fakeable in tests without touching [ManagerRepositories] itself.
 *
 * Mutation success never locally infers the new assigned-service set —
 * [toggleService] re-fetches [SpecialistRepository.getAssignedServiceIds]
 * after a successful assign/unassign, since the backend alone decides what
 * "empty means eligible for every service" resolves to after any given
 * mutation; this class never guesses. A failed mutation never touches
 * [ManagerSpecialistServicesUiState.Ready.assignedServiceIds] at all — the
 * visible toggle state was never optimistically changed in the first
 * place, so there is nothing to roll back, only [ManagerSpecialistServicesUiState.Ready.pendingServiceIds]
 * to clear and [ManagerSpecialistServicesUiState.Ready.mutationError] to
 * surface.
 */
class ManagerSpecialistServicesViewModel(
    private val specialistId: String,
    private val specialistRepository: () -> SpecialistRepository,
    private val initialize: suspend () -> Result<Unit>,
    private val getServices: () -> List<Service>,
) : ViewModel() {

    var state by mutableStateOf<ManagerSpecialistServicesUiState>(ManagerSpecialistServicesUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = ManagerSpecialistServicesUiState.Loading
        viewModelScope.launch {
            initialize().onFailure {
                state = ManagerSpecialistServicesUiState.Error(userMessageFor(it))
                return@launch
            }
            val assignedServiceIds = specialistRepository().getAssignedServiceIds(specialistId).getOrElse {
                state = ManagerSpecialistServicesUiState.Error(userMessageFor(it))
                return@launch
            }
            state = ManagerSpecialistServicesUiState.Ready(
                services = getServices(),
                assignedServiceIds = assignedServiceIds.toSet(),
                isAssignedToEveryService = assignedServiceIds.isEmpty(),
            )
        }
    }

    /** Re-attempts the initial load. Guarded against firing again while already loading, same reasoning as every other Reliability Sprint ViewModel's `retry()`. */
    fun retry() {
        if (state is ManagerSpecialistServicesUiState.Loading) return
        load()
    }

    /**
     * Assigns [serviceId] if it isn't currently assigned, unassigns it if it
     * is — a single real `PUT`/`DELETE` call, guarded per-[serviceId] via
     * [ManagerSpecialistServicesUiState.Ready.pendingServiceIds] so a
     * second tap on the *same* row while its own mutation is in flight is a
     * no-op, while a different row's toggle is unaffected and mutates
     * independently.
     *
     * Routes on raw explicit membership ([ManagerSpecialistServicesUiState.Ready.assignedServiceIds]),
     * never [ManagerSpecialistServicesUiState.Ready.isServiceAssigned] — that
     * helper's "everything means every switch shows on" fallback is display
     * logic only. Reusing it here would make every tap while
     * [ManagerSpecialistServicesUiState.Ready.isAssignedToEveryService] is
     * true resolve to `unassignService` (a no-op on the backend, since no
     * explicit row exists yet to remove), leaving a manager with no way to
     * ever restrict a specialist away from "eligible for everything" — the
     * first tap in that state must call `assignService`, creating the first
     * explicit row.
     */
    fun toggleService(serviceId: String) {
        val current = state as? ManagerSpecialistServicesUiState.Ready ?: return
        if (serviceId in current.pendingServiceIds) return
        val wasAssigned = serviceId in current.assignedServiceIds

        state = current.copy(pendingServiceIds = current.pendingServiceIds + serviceId, mutationError = null)

        viewModelScope.launch {
            val mutation = if (wasAssigned) {
                specialistRepository().unassignService(specialistId, serviceId)
            } else {
                specialistRepository().assignService(specialistId, serviceId)
            }

            mutation
                .onSuccess { refreshAssignedServiceIds(serviceId) }
                .onFailure { error -> clearPending(serviceId, mutationError = userMessageFor(error)) }
        }
    }

    private suspend fun refreshAssignedServiceIds(serviceId: String) {
        specialistRepository().getAssignedServiceIds(specialistId)
            .onSuccess { ids ->
                val latest = state as? ManagerSpecialistServicesUiState.Ready ?: return
                state = latest.copy(
                    assignedServiceIds = ids.toSet(),
                    isAssignedToEveryService = ids.isEmpty(),
                    pendingServiceIds = latest.pendingServiceIds - serviceId,
                )
            }
            .onFailure { error -> clearPending(serviceId, mutationError = userMessageFor(error)) }
    }

    private fun clearPending(serviceId: String, mutationError: String) {
        val latest = state as? ManagerSpecialistServicesUiState.Ready ?: return
        state = latest.copy(pendingServiceIds = latest.pendingServiceIds - serviceId, mutationError = mutationError)
    }
}

sealed interface ManagerSpecialistServicesUiState {
    data object Loading : ManagerSpecialistServicesUiState
    data class Error(val message: String) : ManagerSpecialistServicesUiState
    data class Ready(
        val services: List<Service>,
        val assignedServiceIds: Set<String>,
        val isAssignedToEveryService: Boolean,
        val pendingServiceIds: Set<String> = emptySet(),
        val mutationError: String? = null,
    ) : ManagerSpecialistServicesUiState {
        /** [isAssignedToEveryService] means every service is eligible today, regardless of [assignedServiceIds]'s (irrelevant, empty) contents - never treat empty as "assigned to nothing." */
        fun isServiceAssigned(serviceId: String): Boolean = isAssignedToEveryService || serviceId in assignedServiceIds
    }
}

/** Manual factory for [ManagerSpecialistServicesViewModel], same idiom as [ai.rojan.designlab.manager.presentation.services.ManagerServicesViewModelFactory]. [context] must be an application context — see that sibling factory's own constructor doc for why. */
class ManagerSpecialistServicesViewModelFactory(
    private val specialistId: String,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerSpecialistServicesViewModel(
            specialistId = specialistId,
            specialistRepository = { ManagerRepositories.specialists },
            initialize = { ManagerRepositories.initialize(context) },
            getServices = { ManagerRepositories.services.getAll() },
        ) as T
    }
}
