package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.usecase.relationship.FavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.FollowSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfollowSalonUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SalonRelationshipViewModelFactory(
    private val salonId: String,
    private val followSalonUseCase: FollowSalonUseCase,
    private val unfollowSalonUseCase: UnfollowSalonUseCase,
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase,
    private val favoriteSalonUseCase: FavoriteSalonUseCase,
    private val unfavoriteSalonUseCase: UnfavoriteSalonUseCase,
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalonRelationshipViewModel(
            salonId = salonId,
            followSalonUseCase = followSalonUseCase,
            unfollowSalonUseCase = unfollowSalonUseCase,
            getFollowedSalonsUseCase = getFollowedSalonsUseCase,
            favoriteSalonUseCase = favoriteSalonUseCase,
            unfavoriteSalonUseCase = unfavoriteSalonUseCase,
            getFavoriteSalonsUseCase = getFavoriteSalonsUseCase,
        ) as T
    }
}
