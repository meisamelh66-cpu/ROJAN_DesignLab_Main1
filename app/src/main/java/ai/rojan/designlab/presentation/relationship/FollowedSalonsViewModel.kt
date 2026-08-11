package ai.rojan.designlab.presentation.relationship

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** A followed salon as shown in a list - enriched with the salon's own display fields (name/address), never just a raw id. */
data class FollowedSalonListItem(
    val salonId: String,
    val salonName: String?,
    val salonAddress: String?,
)

/**
 * Loads the authenticated customer's own followed salons -
 * [GetFollowedSalonsUseCase] is implicitly self-scoped (no customerId
 * parameter exists anywhere on this path), so there is no cross-customer
 * data to leak by construction.
 *
 * Enriches each row with the real salon name/address via [salonRepository],
 * deduplicated per unique salon id in the page - same bounded-lookup
 * technique [ai.rojan.designlab.data.repository.BookingHistoryRepositoryImpl]
 * already uses, kept here rather than in the repository layer since
 * [ai.rojan.designlab.domain.repository.CustomerRelationshipRepository]'s
 * own scope is the relationship data only.
 */
class FollowedSalonsViewModel(
    private val getFollowedSalonsUseCase: GetFollowedSalonsUseCase,
    private val salonRepository: SalonRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<FollowedSalonListItem>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val result = getFollowedSalonsUseCase(page = 0, size = 50)
            state = result.fold(
                onSuccess = { followed ->
                    if (followed.isEmpty()) {
                        UiState.Empty
                    } else {
                        val salonById = followed.map { it.salonId }.distinct()
                            .associateWith { salonId -> salonRepository.getSalon(salonId).getOrNull() }
                        UiState.Success(
                            followed.map { follow ->
                                val salon = salonById[follow.salonId]
                                FollowedSalonListItem(follow.salonId, salon?.name, salon?.address)
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
