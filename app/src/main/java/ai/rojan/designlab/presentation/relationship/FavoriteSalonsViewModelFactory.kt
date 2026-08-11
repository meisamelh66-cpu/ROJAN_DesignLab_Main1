package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class FavoriteSalonsViewModelFactory(
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase,
    private val salonRepository: SalonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FavoriteSalonsViewModel(getFavoriteSalonsUseCase, salonRepository) as T
    }
}
