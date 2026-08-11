package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** A favorite salon as shown in a list - enriched with the salon's own display fields (name/address), never just a raw id. */
data class FavoriteSalonListItem(
    val salonId: String,
    val salonName: String?,
    val salonAddress: String?,
)

/** Loads the authenticated customer's own favorite salons - same self-scoping and enrichment reasoning as [FollowedSalonsViewModel]'s own doc comment. */
class FavoriteSalonsViewModel(
    private val getFavoriteSalonsUseCase: GetFavoriteSalonsUseCase,
    private val salonRepository: SalonRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<FavoriteSalonListItem>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val result = getFavoriteSalonsUseCase(page = 0, size = 50)
            state = result.fold(
                onSuccess = { favorites ->
                    if (favorites.isEmpty()) {
                        UiState.Empty
                    } else {
                        val salonById = favorites.map { it.salonId }.distinct()
                            .associateWith { salonId -> salonRepository.getSalon(salonId).getOrNull() }
                        UiState.Success(
                            favorites.map { favorite ->
                                val salon = salonById[favorite.salonId]
                                FavoriteSalonListItem(favorite.salonId, salon?.name, salon?.address)
                            },
                        )
                    }
                },
                onFailure = { UiState.Error(userMessageFor(it)) },
            )
        }
    }

    fun retry() = load()
}
