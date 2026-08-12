package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
private const val RELATIONSHIP_PAGE_SIZE = 100

/**
 * Browses active salons from the real backend (`GET /api/v1/salons`).
 * Shared by [ai.rojan.designlab.screens.booking.SalonListScreen] and
 * [ai.rojan.designlab.screens.search.SearchScreen].
 *
 * Browse Experience fix (earlier session): `GET /api/v1/salons` requires
 * authentication, so browsing as an anonymous customer lands in
 * [UiState.Error]. [isUnauthorized] lets the screen show a real "log in"
 * action instead of a dead-end "retry".
 *
 * Salon Discovery: adds real pagination (page > 0 via [loadMore]),
 * cancellation-safe search (a new [load] cancels any in-flight previous
 * one, so a slow stale response can never overwrite a newer one), and
 * "cache last successful result" (a follow-up search that fails keeps the
 * previously-shown list visible rather than blanking it - only the very
 * first load falls through to a real [UiState.Error]). Follow/favorite
 * state is resolved once per successful list load via the same bounded
 * list-membership approach [ai.rojan.designlab.presentation.relationship.SalonRelationshipViewModel]
 * already uses - both use case params are nullable so this ViewModel still
 * works (without follow/favorite indicators) for any call site that
 * doesn't wire them.
 */
class SalonListViewModel(
    private val salonRepository: SalonRepository,
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase? = null,
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase? = null,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<Salon>>>(UiState.Loading)
        private set

    var isUnauthorized by mutableStateOf(false)
        private set

    /** True while a follow-up search/filter is in flight and a previous successful result is still on screen - distinct from the initial [UiState.Loading]. */
    var isSearching by mutableStateOf(false)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    var canLoadMore by mutableStateOf(false)
        private set

    var followedSalonIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var favoriteSalonIds by mutableStateOf<Set<String>>(emptySet())
        private set

    private var currentNameFilter: String? = null
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load(nameFilter: String? = null) {
        loadJob?.cancel()
        currentNameFilter = nameFilter
        val hasCachedResult = state is UiState.Success
        if (hasCachedResult) isSearching = true else state = UiState.Loading
        isUnauthorized = false

        loadJob = viewModelScope.launch {
            salonRepository.browseSalons(page = 0, size = PAGE_SIZE, nameFilter = nameFilter, sortDirection = "ASC")
                .onSuccess { paged ->
                    canLoadMore = paged.page + 1 < paged.totalPages
                    state = if (paged.content.isEmpty()) UiState.Empty else UiState.Success(paged.content)
                    loadRelationshipState()
                }
                .onFailure { error ->
                    isUnauthorized = error is BackendApiException && error.statusCode == 401
                    if (!hasCachedResult) {
                        state = UiState.Error(userMessageFor(error))
                    }
                }
            isSearching = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !canLoadMore) return
        val currentSalons = (state as? UiState.Success)?.data ?: return
        isLoadingMore = true
        viewModelScope.launch {
            val nextPage = (currentSalons.size + PAGE_SIZE - 1) / PAGE_SIZE
            salonRepository.browseSalons(page = nextPage, size = PAGE_SIZE, nameFilter = currentNameFilter, sortDirection = "ASC")
                .onSuccess { paged ->
                    canLoadMore = paged.page + 1 < paged.totalPages
                    state = UiState.Success(currentSalons + paged.content)
                }
            isLoadingMore = false
        }
    }

    private suspend fun loadRelationshipState() {
        val followedUseCase = getFollowedSalonsUseCase
        val favoriteUseCase = getFavoriteSalonsUseCase
        if (followedUseCase != null) {
            followedUseCase(page = 0, size = RELATIONSHIP_PAGE_SIZE).getOrNull()
                ?.let { followedSalonIds = it.map { follow -> follow.salonId }.toSet() }
        }
        if (favoriteUseCase != null) {
            favoriteUseCase(page = 0, size = RELATIONSHIP_PAGE_SIZE).getOrNull()
                ?.let { favoriteSalonIds = it.map { favorite -> favorite.salonId }.toSet() }
        }
    }

    fun retry() = load(currentNameFilter)
}
