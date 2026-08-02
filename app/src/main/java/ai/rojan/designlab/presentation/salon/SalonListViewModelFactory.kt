package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.SalonRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SalonListViewModelFactory(
    private val salonRepository: SalonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalonListViewModel(salonRepository) as T
    }
}
