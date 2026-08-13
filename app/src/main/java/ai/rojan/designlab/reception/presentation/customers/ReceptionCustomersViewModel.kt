package ai.rojan.designlab.reception.presentation.customers

import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * `VIEW_CRM`-scoped customer search — real, backed by the owner-only
 * `CustomerController` today (see [ReceptionCustomerRepository]'s own doc
 * comment for the current authorization status).
 */
class ReceptionCustomersViewModel(
    private val salonId: String,
    private val customerRepository: ReceptionCustomerRepository,
) : ViewModel() {

    private val _customers = MutableStateFlow<UiState<List<ReceptionCustomer>>>(UiState.Loading)
    val customers: StateFlow<UiState<List<ReceptionCustomer>>> = _customers.asStateFlow()

    init {
        search(null)
    }

    fun search(query: String?) {
        _customers.value = UiState.Loading
        viewModelScope.launch {
            customerRepository.listCustomers(salonId, search = query)
                .onSuccess { result ->
                    _customers.value = if (result.content.isEmpty()) UiState.Empty else UiState.Success(result.content)
                }
                .onFailure { error -> _customers.value = UiState.Error(userMessageFor(error)) }
        }
    }
}
