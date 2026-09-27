package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory, mirroring every other ViewModel factory in this app (e.g. `ManagerDashboardViewModelFactory`).
 *
 * Phase B retarget (Customers ViewModel migration): [customerRepository] type follows
 * [ManagerCustomersViewModel]'s current constructor (Phase 3A) - [CustomerRepository], the same
 * working Manager CRM abstraction [ai.rojan.designlab.manager.data.ManagerRepositories.customers]
 * already exposes, not the old [ai.rojan.designlab.domain.repository.SalonCustomerRepository]. This
 * factory only declares the dependency; supplying the real instance at a call site is a separate,
 * not-yet-authorized wiring phase.
 */
class ManagerCustomersViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerCustomersViewModel(customerRepository, currentUserIdentityContextRepository) as T
    }
}
