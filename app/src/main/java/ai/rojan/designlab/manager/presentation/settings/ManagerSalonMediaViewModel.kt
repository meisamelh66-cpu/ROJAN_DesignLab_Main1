package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerIdentitySlot
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.repository.ManagerMediaRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central Salon Management — Salon Media UI: current logo/cover (whichever
 * `MediaAsset` is presently assigned to each identity slot, resolved from
 * [ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary]).
 * [isUploadingLogo]/[isUploadingCover] are separate so one section's
 * in-flight upload never disables the other. The gallery section (Media
 * System Evolution v2) is no longer owned by this ViewModel - it's a
 * self-contained [ai.rojan.designlab.manager.components.ManagerTargetedMediaGallery]
 * mounted directly by the screen, the same reusable component the
 * specialist-portfolio/service-images screens use, so gallery upload/list/
 * delete/reorder logic lives in exactly one place, not duplicated here.
 */
data class SalonMediaState(
    val logoUrl: String?,
    val coverUrl: String?,
    val isUploadingLogo: Boolean = false,
    val isUploadingCover: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Owns loading + logo/cover upload state for
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonMediaScreen] —
 * same ViewModel+Factory shape [ManagerSalonSetupViewModel]/
 * [ManagerWorkingHoursViewModel] already establish for this Settings
 * area. [salonId] resolved once by the factory from
 * [ManagerRepositories.salonId], same precondition as those two.
 *
 * Logo/cover upload is a compound action from the UI's perspective (pick
 * → [ManagerMediaRepository.upload] → [ManagerMediaRepository.assignIdentity])
 * even though it's two real backend calls, mirroring the backend's own
 * two-step design (see that repository's own doc comment). A successful
 * assign also pushes the fresh salon summary back into
 * [ManagerRepositories] so the Dashboard's identity card updates without
 * a full re-sync.
 */
class ManagerSalonMediaViewModel(
    private val salonId: String?,
    private val repository: ManagerMediaRepository,
) : ViewModel() {

    private val _loadState = MutableStateFlow<UiState<SalonMediaState>>(UiState.Loading)
    val loadState: StateFlow<UiState<SalonMediaState>> = _loadState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (salonId == null) {
            _loadState.value = UiState.Error("سالن فعال یافت نشد")
            return
        }
        val salon = ManagerRepositories.salon.value
        _loadState.value = UiState.Success(SalonMediaState(logoUrl = salon?.logoUrl, coverUrl = salon?.coverImageUrl))
    }

    fun uploadLogo(fileBytes: ByteArray, fileName: String, mimeType: String) =
        uploadIdentity(ManagerMediaType.LOGO, ManagerIdentitySlot.LOGO, fileBytes, fileName, mimeType)

    fun uploadCover(fileBytes: ByteArray, fileName: String, mimeType: String) =
        uploadIdentity(ManagerMediaType.COVER, ManagerIdentitySlot.COVER, fileBytes, fileName, mimeType)

    private fun uploadIdentity(
        mediaType: ManagerMediaType,
        slot: ManagerIdentitySlot,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ) {
        val id = salonId ?: return
        updateState { it.copy(errorMessage = null).markUploading(slot, true) }
        viewModelScope.launch {
            val result = repository.upload(id, mediaType, fileBytes, fileName, mimeType)
                .fold(
                    onSuccess = { asset -> repository.assignIdentity(id, slot, asset.id) },
                    onFailure = { Result.failure(it) },
                )
            result
                .onSuccess { salon ->
                    ManagerRepositories.updateSalon(salon)
                    updateState {
                        it.copy(logoUrl = salon.logoUrl, coverUrl = salon.coverImageUrl).markUploading(slot, false)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(errorMessage = userMessageFor(error)).markUploading(slot, false) }
                }
        }
    }

    private fun SalonMediaState.markUploading(slot: ManagerIdentitySlot, value: Boolean): SalonMediaState = when (slot) {
        ManagerIdentitySlot.LOGO -> copy(isUploadingLogo = value)
        ManagerIdentitySlot.COVER -> copy(isUploadingCover = value)
    }

    private fun currentState(): SalonMediaState? = (_loadState.value as? UiState.Success)?.data

    private fun updateState(transform: (SalonMediaState) -> SalonMediaState) {
        val state = currentState() ?: return
        _loadState.value = UiState.Success(transform(state))
    }
}
