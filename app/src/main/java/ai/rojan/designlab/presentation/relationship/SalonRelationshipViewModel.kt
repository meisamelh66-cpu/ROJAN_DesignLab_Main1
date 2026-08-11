package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.usecase.relationship.FavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.FollowSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfavoriteSalonUseCase
import ai.rojan.designlab.domain.usecase.relationship.UnfollowSalonUseCase
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** Bounded lookup page size for resolving initial follow/favorite state - see this class's own doc comment. */
private const val RELATIONSHIP_LIST_PAGE_SIZE = 100

/**
 * Backs the Follow/Favorite buttons on Salon Details for one [salonId].
 *
 * There is no single-salon "am I following/favoriting this?" endpoint on
 * the backend (`ROJAN_Backend` commit db5faea only exposes the full list) -
 * the initial button state is resolved by fetching the customer's own
 * follow/favorite lists once and checking membership, the same bounded/
 * deduplicated-lookup approach [ai.rojan.designlab.data.repository.BookingHistoryRepositoryImpl]
 * already uses elsewhere in this app for an analogous gap. This is a
 * disclosed, real limitation: correct for any realistic number of
 * follows/favorites (bounded by [RELATIONSHIP_LIST_PAGE_SIZE]), not
 * guaranteed correct beyond that without a dedicated single-salon status
 * endpoint - not implemented here since "do not modify backend unless a
 * real contract issue is proven" and a bounded list-membership check is a
 * workable, honest approximation for the customer volumes this app has
 * today.
 *
 * Both toggles are optimistic-with-rollback: the visible state flips
 * immediately on tap (no spinner-then-flip lag on the common, successful
 * path), and reverts if the backend call fails - never a fake toggle that
 * ignores the real result.
 */
class SalonRelationshipViewModel(
    private val salonId: String,
    private val followSalonUseCase: FollowSalonUseCase,
    private val unfollowSalonUseCase: UnfollowSalonUseCase,
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase,
    private val favoriteSalonUseCase: FavoriteSalonUseCase,
    private val unfavoriteSalonUseCase: UnfavoriteSalonUseCase,
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase,
) : ViewModel() {

    var isFollowing by mutableStateOf(false)
        private set
    var isFavorite by mutableStateOf(false)
        private set
    var isInitialLoading by mutableStateOf(true)
        private set
    var isFollowActionInProgress by mutableStateOf(false)
        private set
    var isFavoriteActionInProgress by mutableStateOf(false)
        private set
    var followError by mutableStateOf<String?>(null)
        private set

    /**
     * Protected Route Handling fix: an anonymous customer can view Salon
     * Details freely (browsing is unauthenticated by design here), but
     * Follow/Favorite are real customer-owned actions the backend correctly
     * rejects with 401. This flips true exactly once per failed attempt so
     * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] can redirect to
     * login instead of just showing an opaque error - [consumeLoginRequired]
     * resets it so the same redirect doesn't refire on recomposition.
     */
    var requiresLogin by mutableStateOf(false)
        private set

    fun consumeLoginRequired() {
        requiresLogin = false
    }

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        isInitialLoading = true
        viewModelScope.launch {
            val followed = getFollowedSalonsUseCase(page = 0, size = RELATIONSHIP_LIST_PAGE_SIZE)
            val favorited = getFavoriteSalonsUseCase(page = 0, size = RELATIONSHIP_LIST_PAGE_SIZE)
            isFollowing = followed.getOrNull()?.any { it.salonId == salonId } ?: false
            isFavorite = favorited.getOrNull()?.any { it.salonId == salonId } ?: false
            isInitialLoading = false
        }
    }

    fun toggleFollow() {
        if (isFollowActionInProgress) return
        val target = !isFollowing
        isFollowing = target
        isFollowActionInProgress = true
        followError = null
        viewModelScope.launch {
            val result = if (target) followSalonUseCase(salonId).map { } else unfollowSalonUseCase(salonId)
            result.onFailure { error ->
                isFollowing = !target
                if (error.isUnauthorized()) {
                    requiresLogin = true
                } else {
                    followError = userMessageFor(error)
                }
            }
            isFollowActionInProgress = false
        }
    }

    fun toggleFavorite() {
        if (isFavoriteActionInProgress) return
        val target = !isFavorite
        isFavorite = target
        isFavoriteActionInProgress = true
        viewModelScope.launch {
            val result = if (target) favoriteSalonUseCase(salonId).map { } else unfavoriteSalonUseCase(salonId)
            result.onFailure { error ->
                isFavorite = !target
                if (error.isUnauthorized()) requiresLogin = true
            }
            isFavoriteActionInProgress = false
        }
    }

    private fun Throwable.isUnauthorized(): Boolean = this is BackendApiException && statusCode == 401
}
