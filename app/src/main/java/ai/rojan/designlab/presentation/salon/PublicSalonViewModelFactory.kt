package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.PublicSalonRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring [SalonDetailsViewModelFactory] and every other ViewModel factory in this app. */
class PublicSalonViewModelFactory(
    private val slug: String,
    private val publicSalonRepository: PublicSalonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PublicSalonViewModel(slug = slug, publicSalonRepository = publicSalonRepository) as T
    }
}
