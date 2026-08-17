package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerIdentitySlot
import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
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
 * Central Salon Management — Salon Media UI: current logo/cover
 * (whichever `MediaAsset` is presently assigned to each identity slot,
 * resolved from [ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary])
 * plus the gallery list (a plain `List<ManagerMediaAsset>`, no slot
 * concept). [isUploadingLogo]/[isUploadingCover]/[uploadingGalleryCount]
 * are separate so one section's in-flight upload never disables the
 * others.
 */
data class SalonMediaState(
    val logoUrl: String?,
    val coverUrl: String?,
    val gallery: List<ManagerMediaAsset>,
    val isUploadingLogo: Boolean = false,
    val isUploadingCover: Boolean = false,
    val uploadingGalleryCount: Int = 0,
    val errorMessage: String? = null,
)

/**
 * Owns loading + logo/cover/gallery upload state for
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
 * a full re-sync. Gallery images only ever call [ManagerMediaRepository.upload]/
 * [ManagerMediaRepository.delete] — no identity slot involved.
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
        val id = salonId
        if (id == null) {
            _loadState.value = UiState.Error("سالن فعال یافت نشد")
            return
        }
        _loadState.value = UiState.Loading
        viewModelScope.launch {
            repository.list(id, ManagerMediaType.GALLERY)
                .onSuccess { gallery ->
                    val salon = ManagerRepositories.salon
                    _loadState.value = UiState.Success(
                        SalonMediaState(logoUrl = salon?.logoUrl, coverUrl = salon?.coverImageUrl, gallery = gallery),
                    )
                }
                .onFailure { _loadState.value = UiState.Error(userMessageFor(it)) }
        }
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

    fun addGalleryImage(fileBytes: ByteArray, fileName: String, mimeType: String) {
        val id = salonId ?: return
        updateState { it.copy(errorMessage = null, uploadingGalleryCount = it.uploadingGalleryCount + 1) }
        viewModelScope.launch {
            repository.upload(id, ManagerMediaType.GALLERY, fileBytes, fileName, mimeType)
                .onSuccess { asset ->
                    updateState { it.copy(gallery = it.gallery + asset, uploadingGalleryCount = it.uploadingGalleryCount - 1) }
                }
                .onFailure { error ->
                    updateState {
                        it.copy(errorMessage = userMessageFor(error), uploadingGalleryCount = it.uploadingGalleryCount - 1)
                    }
                }
        }
    }

    fun deleteGalleryImage(mediaId: String) {
        val id = salonId ?: return
        val previous = currentState()?.gallery ?: return
        updateState { it.copy(errorMessage = null, gallery = it.gallery.filterNot { asset -> asset.id == mediaId }) }
        viewModelScope.launch {
            repository.delete(id, mediaId)
                .onFailure { error ->
                    updateState { it.copy(errorMessage = userMessageFor(error), gallery = previous) }
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
