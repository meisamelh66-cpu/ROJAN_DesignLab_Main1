package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.PublicSalonRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SpecialistSelectionViewModelFactory(
    private val salonId: String,
    private val specialistRepository: SpecialistRepository,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val slug: String? = null,
    private val hasSession: () -> Boolean = { true },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SpecialistSelectionViewModel(
            salonId = salonId,
            specialistRepository = specialistRepository,
            publicSalonRepository = publicSalonRepository,
            slug = slug,
            hasSession = hasSession,
        ) as T
    }
}
