package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.ManagerCustomerProfileRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.manager.domain.customer.ManagerCustomerProfile
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * FIX-006. Backs
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]
 * with the customer's real backend CRM profile
 * (`GET /customer-records/{id}` + `/notes` + `/tags` + `/timeline` +
 * `/bookings`), replacing the in-memory
 * [ai.rojan.designlab.manager.data.InMemoryCustomerRepository] sample
 * record the screen read before.
 *
 * [accountId] is the id the Customers list navigates with - a linked
 * account's `UserId` from the legacy `GET /customers` roster. The repository
 * resolves it to the salon's CRM `CustomerId`.
 *
 * [state] follows this app's [UiState] convention: [UiState.Loading] while
 * fetching, [UiState.Error] on any failure (never fake data), [UiState.Empty]
 * when the account owns no salon or has no CRM record for this salon,
 * [UiState.Success] with the real profile otherwise.
 */
class ManagerCustomerProfileViewModel(
    private val accountId: String,
    private val salonRepository: SalonRepository,
    private val profileRepository: ManagerCustomerProfileRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerCustomerProfile>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val salonId = resolveSalonId() ?: return@launch
            profileRepository.loadProfileByAccountId(salonId, accountId)
                .onSuccess { profile ->
                    state = if (profile == null) UiState.Empty else UiState.Success(profile)
                }
                .onFailure { state = UiState.Error(userMessageFor(it)) }
        }
    }

    /** Same `GET /salons/mine` → first owned salon resolution the other Manager ViewModels use. */
    private suspend fun resolveSalonId(): String? =
        salonRepository.myOwnedSalons().fold(
            onSuccess = { salons ->
                val salon = salons.firstOrNull()
                if (salon == null) {
                    state = UiState.Empty
                    null
                } else {
                    salon.id
                }
            },
            onFailure = {
                state = UiState.Error(userMessageFor(it))
                null
            },
        )
}
