package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus
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
 * P1-6, Manager Salon Invite Management: owns the Loading/Error/Ready
 * lifecycle for [ai.rojan.designlab.manager.screens.staff.ManagerInvitesScreen]
 * plus create/revoke/QR mutations, via the real
 * `POST`/`GET`/`DELETE .../salons/{salonId}/invites` and
 * `GET .../invites/{inviteId}/qr-code` endpoints confirmed against backend
 * source this sprint.
 *
 * Every backend call this endpoint set exposes is owner-only server-side
 * (`Permission.MANAGE_MEMBERSHIP`) — a manager-role (non-owner) caller gets
 * a real `403` on any of them, which surfaces through [userMessageFor]
 * exactly like any other access-denied response in this codebase; no
 * client-side role gate exists here by design (this pass shows the invite
 * entry point unconditionally).
 *
 * [initialize] re-syncs [ManagerRepositories] before reading
 * [ManagerRepositories.invites] - same accepted trade-off P1-5's
 * [ManagerSpecialistServicesViewModel] already established. [inviteRepository]
 * is injected as a lambda rather than a captured instance for the same
 * "[ManagerRepositories.invites] is reassigned by `initialize()`" reason
 * that class documents.
 *
 * Create/revoke never locally infer the new list — both always re-fetch
 * [ManagerInviteRepository.listInvites] after a real success, since only
 * the backend knows the invite's real `id`/`status`/`expiresAt`, and
 * `EXPIRED` in particular is a read-time-computed value that this class
 * must never try to guess between fetches. QR bytes are fetched only when
 * explicitly requested for one invite, never prefetched for the whole
 * list.
 */
class ManagerInvitesViewModel(
    private val inviteRepository: () -> ManagerInviteRepository,
    private val initialize: suspend () -> Result<Unit>,
) : ViewModel() {

    var state by mutableStateOf<ManagerInvitesUiState>(ManagerInvitesUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = ManagerInvitesUiState.Loading
        viewModelScope.launch {
            initialize().onFailure {
                state = ManagerInvitesUiState.Error(userMessageFor(it))
                return@launch
            }
            fetchInvites()
        }
    }

    /** Re-attempts the initial load. Guarded against firing again while already loading, same reasoning as every other Reliability Sprint ViewModel's `retry()`. */
    fun retry() {
        if (state is ManagerInvitesUiState.Loading) return
        load()
    }

    private suspend fun fetchInvites() {
        inviteRepository().listInvites()
            .onSuccess { invites -> state = ManagerInvitesUiState.Ready(invites = invites) }
            .onFailure { state = ManagerInvitesUiState.Error(userMessageFor(it)) }
    }

    /** Creates an invite for [role]. On success, re-fetches the real list rather than locally appending the response — see this class's own doc comment. */
    fun createInvite(role: SalonMemberRole) {
        val current = state as? ManagerInvitesUiState.Ready ?: return
        if (current.isCreating) return

        state = current.copy(isCreating = true, mutationError = null)

        viewModelScope.launch {
            inviteRepository().issueInvite(role)
                .onSuccess { fetchInvitesPreservingCreating() }
                .onFailure { error ->
                    val latest = state as? ManagerInvitesUiState.Ready ?: return@onFailure
                    state = latest.copy(isCreating = false, mutationError = userMessageFor(error))
                }
        }
    }

    private suspend fun fetchInvitesPreservingCreating() {
        inviteRepository().listInvites()
            .onSuccess { invites ->
                val latest = state as? ManagerInvitesUiState.Ready ?: return@onSuccess
                state = latest.copy(invites = invites, isCreating = false)
            }
            .onFailure { error ->
                val latest = state as? ManagerInvitesUiState.Ready ?: return@onFailure
                state = latest.copy(isCreating = false, mutationError = userMessageFor(error))
            }
    }

    /** Revokes [inviteId] — mirrors the backend's own rule by refusing (silently, no-op) unless the invite's current, real status is [SalonInviteStatus.CREATED], not just relying on the screen hiding the action for a non-`CREATED` row. Guarded per-[inviteId] via [ManagerInvitesUiState.Ready.pendingIds] against a double-tap while in flight. */
    fun revokeInvite(inviteId: String) {
        val current = state as? ManagerInvitesUiState.Ready ?: return
        if (inviteId in current.pendingIds) return
        if (current.invites.firstOrNull { it.id == inviteId }?.status != SalonInviteStatus.CREATED) return

        state = current.copy(pendingIds = current.pendingIds + inviteId, mutationError = null)

        viewModelScope.launch {
            inviteRepository().revokeInvite(inviteId)
                .onSuccess {
                    inviteRepository().listInvites()
                        .onSuccess { invites ->
                            val latest = state as? ManagerInvitesUiState.Ready ?: return@onSuccess
                            state = latest.copy(invites = invites, pendingIds = latest.pendingIds - inviteId)
                        }
                        .onFailure { error -> clearPending(inviteId, userMessageFor(error)) }
                }
                .onFailure { error -> clearPending(inviteId, userMessageFor(error)) }
        }
    }

    private fun clearPending(inviteId: String, mutationError: String) {
        val latest = state as? ManagerInvitesUiState.Ready ?: return
        state = latest.copy(pendingIds = latest.pendingIds - inviteId, mutationError = mutationError)
    }

    /** Fetches QR bytes for [inviteId] on demand — same real-status guard as [revokeInvite], not just a screen-level assumption. Never called for more than one invite at a time by the screen (no prefetching). */
    fun loadQrCode(inviteId: String) {
        val current = state as? ManagerInvitesUiState.Ready ?: return
        if (current.invites.firstOrNull { it.id == inviteId }?.status != SalonInviteStatus.CREATED) return
        state = current.copy(qrCode = QrCodeState.Loading(inviteId))

        viewModelScope.launch {
            inviteRepository().getInviteQrCode(inviteId)
                .onSuccess { png ->
                    val latest = state as? ManagerInvitesUiState.Ready ?: return@onSuccess
                    state = latest.copy(qrCode = QrCodeState.Loaded(inviteId, png))
                }
                .onFailure { error ->
                    val latest = state as? ManagerInvitesUiState.Ready ?: return@onFailure
                    state = latest.copy(qrCode = QrCodeState.Error(inviteId, userMessageFor(error)))
                }
        }
    }

    /** Closes the QR dialog without discarding the rest of [ManagerInvitesUiState.Ready]. */
    fun dismissQrCode() {
        val current = state as? ManagerInvitesUiState.Ready ?: return
        state = current.copy(qrCode = null)
    }
}

sealed interface ManagerInvitesUiState {
    data object Loading : ManagerInvitesUiState
    data class Error(val message: String) : ManagerInvitesUiState
    data class Ready(
        val invites: List<SalonInvite>,
        val pendingIds: Set<String> = emptySet(),
        val isCreating: Boolean = false,
        val mutationError: String? = null,
        val qrCode: QrCodeState? = null,
    ) : ManagerInvitesUiState
}

sealed interface QrCodeState {
    val inviteId: String
    data class Loading(override val inviteId: String) : QrCodeState
    data class Loaded(override val inviteId: String, val png: ByteArray) : QrCodeState
    data class Error(override val inviteId: String, val message: String) : QrCodeState
}

/** Manual factory for [ManagerInvitesViewModel], same idiom as [ManagerSpecialistServicesViewModelFactory]. [context] must be an application context — see that sibling factory's own constructor doc for why. */
class ManagerInvitesViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerInvitesViewModel(
            inviteRepository = { ManagerRepositories.invites },
            initialize = { ManagerRepositories.initialize(context) },
        ) as T
    }
}
