package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.UserProfileRepository
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Customer Profile Personalization Phase 5B — owns the avatar / profile-cover
 * upload + delete state for `ProfileScreen`. The bytes it receives are
 * already downscaled + JPEG-re-encoded by the shared
 * `ui/media/ImageDownscale.kt` pipeline.
 *
 * [isUploadingAvatar] / [isUploadingCover] are separate so one in-flight
 * action never disables the other. On any success the refreshed
 * [AuthenticatedUser] is pushed back through [onUserUpdated] (→
 * `AuthViewModel.applyUpdatedUser`), which is the single source of truth
 * `ProfileScreen` renders from — this ViewModel holds no user state of its
 * own. Errors become a Persian [ProfileMediaState.errorMessage] via the
 * shared [userMessageFor]; nothing here can crash a repository call
 * (`safeApiCall` wraps every request).
 */
data class ProfileMediaState(
    val isUploadingAvatar: Boolean = false,
    val isUploadingCover: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileMediaViewModel(
    private val repository: UserProfileRepository,
    private val onUserUpdated: (AuthenticatedUser) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileMediaState())
    val state: StateFlow<ProfileMediaState> = _state.asStateFlow()

    fun uploadAvatar(bytes: ByteArray, fileName: String, mimeType: String) =
        runSlot(Slot.AVATAR) { repository.uploadAvatar(bytes, fileName, mimeType) }

    fun uploadCover(bytes: ByteArray, fileName: String, mimeType: String) =
        runSlot(Slot.COVER) { repository.uploadCover(bytes, fileName, mimeType) }

    fun removeAvatar() = runSlot(Slot.AVATAR) { repository.removeAvatar() }

    fun removeCover() = runSlot(Slot.COVER) { repository.removeCover() }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private enum class Slot { AVATAR, COVER }

    private fun ProfileMediaState.withBusy(slot: Slot, busy: Boolean) = when (slot) {
        Slot.AVATAR -> copy(isUploadingAvatar = busy)
        Slot.COVER -> copy(isUploadingCover = busy)
    }

    private fun ProfileMediaState.isBusy(slot: Slot) = when (slot) {
        Slot.AVATAR -> isUploadingAvatar
        Slot.COVER -> isUploadingCover
    }

    private inline fun runSlot(slot: Slot, crossinline call: suspend () -> Result<AuthenticatedUser>) {
        if (_state.value.isBusy(slot)) return
        _state.value = _state.value.withBusy(slot, true).copy(errorMessage = null)
        viewModelScope.launch {
            call()
                .onSuccess { user ->
                    onUserUpdated(user)
                    _state.value = _state.value.withBusy(slot, false)
                }
                .onFailure { error ->
                    _state.value = _state.value.withBusy(slot, false).copy(errorMessage = userMessageFor(error))
                }
        }
    }
}
