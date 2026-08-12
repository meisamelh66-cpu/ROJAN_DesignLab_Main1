package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SalonListViewModelFactory(
    private val salonRepository: SalonRepository,
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase? = null,
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalonListViewModel(salonRepository, getFollowedSalonsUseCase, getFavoriteSalonsUseCase) as T
    }
}
