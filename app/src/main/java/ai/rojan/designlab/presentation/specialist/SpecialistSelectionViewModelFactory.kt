package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SpecialistSelectionViewModelFactory(
    private val salonId: String,
    private val specialistRepository: SpecialistRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SpecialistSelectionViewModel(salonId, specialistRepository) as T
    }
}
