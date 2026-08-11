package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class FollowedSalonsViewModelFactory(
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase,
    private val salonRepository: SalonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FollowedSalonsViewModel(getFollowedSalonsUseCase, salonRepository) as T
    }
}
