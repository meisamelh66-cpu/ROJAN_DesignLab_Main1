package ai.rojan.designlab.presentation.roleselection

import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.usecase.SaveSelectedRoleUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Drives role selection on the Welcome screen: persists the tapped role
 * via [SaveSelectedRoleUseCase] and emits a single, one-shot navigation
 * event once persistence succeeds.
 *
 * Deliberately free of any Compose/navigation types — the presentation
 * layer talks to the domain use case only, and hands the UI layer a
 * plain [Role] to navigate on, per Clean Architecture.
 */
class RoleSelectionViewModel(
    private val saveSelectedRole: SaveSelectedRoleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoleSelectionUiState>(RoleSelectionUiState.Idle)
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    // A Channel, not a StateFlow/replaying SharedFlow: a Channel delivers
    // each navigation instruction to exactly one collector exactly once.
    // That, combined with the in-flight guard in [selectRole] below, is
    // what prevents a role selection from ever triggering navigation
    // twice — e.g. on recomposition, or a rapid double-tap.
    private val _navigationEvents = Channel<Role>(Channel.BUFFERED)
    val navigationEvents: Flow<Role> = _navigationEvents.receiveAsFlow()

    private var lastAttemptedRole: Role? = null

    /**
     * Persists [role] as the user's selection.
     *
     * While a save is already in flight, further calls are ignored
     * outright (not queued), so a rapid double-tap on a role card — or
     * on the Hero Card's CTA — can never fire two saves or two
     * navigation events for the same gesture.
     */
    fun selectRole(role: Role) {
        if (_uiState.value == RoleSelectionUiState.Saving) return

        lastAttemptedRole = role
        viewModelScope.launch {
            _uiState.value = RoleSelectionUiState.Saving
            runCatching { saveSelectedRole(role) }
                .onSuccess {
                    _uiState.value = RoleSelectionUiState.Idle
                    _navigationEvents.send(role)
                }
                .onFailure {
                    _uiState.value = RoleSelectionUiState.Error
                }
        }
    }

    /** Re-attempts persisting the last role that failed to save. No-op if nothing failed. */
    fun retry() {
        lastAttemptedRole?.let { selectRole(it) }
    }
}
