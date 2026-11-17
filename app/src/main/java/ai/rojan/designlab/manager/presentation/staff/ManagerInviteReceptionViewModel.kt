package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gap P0-3-REC-INVITE-UI - Idle/Creating/Created/Error, same shape every
 * other Manager Settings ViewModel in this codebase already uses.
 * [SalonInvite.token] only ever reaches [InviteReceptionUiState.Created] -
 * never logged, never sent anywhere except by the person this screen is
 * shown to, sharing it with the real Reception hire out-of-band.
 */
sealed interface InviteReceptionUiState {
    data object Idle : InviteReceptionUiState
    data object Creating : InviteReceptionUiState
    data class Created(val invite: SalonInvite) : InviteReceptionUiState
    data class Error(val message: String) : InviteReceptionUiState
}

/**
 * Owner-only in effect (the backend's own `Permission.MANAGE_MEMBERSHIP`
 * check is the real enforcement - see [ManagerInviteRepository]'s own doc
 * comment); this screen is only ever reached when
 * [ai.rojan.designlab.domain.repository.SalonPermissions.MANAGE_MEMBERSHIP]
 * is present in the active salon's resolved permission set (gated in
 * `ManagerNavGraph`, same "optional-callback-means-hidden" convention
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen]
 * already uses for its own owner/manager-gated entry rows). A caller who
 * somehow reaches this screen without that permission still gets a real
 * 403 from the backend, surfaced through the same generic
 * [InviteReceptionUiState.Error]/[userMessageFor] path as any other
 * failure - no separate "Forbidden" branch is needed.
 */
class ManagerInviteReceptionViewModel(
    private val salonId: String?,
    private val repository: ManagerInviteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<InviteReceptionUiState>(InviteReceptionUiState.Idle)
    val state: StateFlow<InviteReceptionUiState> = _state.asStateFlow()

    fun createInvite() {
        val id = salonId
        if (id == null) {
            _state.value = InviteReceptionUiState.Error("سالن فعال یافت نشد")
            return
        }
        _state.value = InviteReceptionUiState.Creating
        viewModelScope.launch {
            repository.issueInvite(id, SalonMemberRole.RECEPTIONIST)
                .onSuccess { invite -> _state.value = InviteReceptionUiState.Created(invite) }
                .onFailure { error -> _state.value = InviteReceptionUiState.Error(userMessageFor(error)) }
        }
    }
}
