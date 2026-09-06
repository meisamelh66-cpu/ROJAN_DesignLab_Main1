package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerCustomersViewModel], mirroring every other
 * ViewModel factory in this app (e.g. `ManagerBookingViewModelFactory`).
 * Wires the real backend repositories from
 * [ai.rojan.designlab.di.BackendApiContainer], not
 * `manager.data.ManagerRepositories`' in-memory ones.
 */
class ManagerCustomersViewModelFactory(
    private val salonRepository: SalonRepository,
    private val salonCustomerRepository: SalonCustomerRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerCustomersViewModel(salonRepository, salonCustomerRepository) as T
    }
}
