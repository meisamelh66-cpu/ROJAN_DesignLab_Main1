package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory, mirroring every other ViewModel factory in this app (e.g. `ManagerDashboardViewModelFactory`).
 *
 * Phase B retarget (Customers ViewModel migration): follows [ManagerCustomerProfileViewModel]'s
 * current constructor (Phase 3A) - [CustomerRepository] (the same working Manager CRM abstraction
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] already exposes) plus
 * [CurrentUserIdentityContextRepository] and [customerId]. `serviceCategoryRepository`/
 * `serviceRepository`/`specialistRepository` are gone - the retargeted ViewModel no longer needs
 * them, since [CustomerRepository.getServiceHistory] already returns fully cross-referenced,
 * pre-formatted entries. This factory only declares the dependency; supplying the real instance at
 * a call site is a separate, not-yet-authorized wiring phase.
 */
class ManagerCustomerProfileViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
    private val customerId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerCustomerProfileViewModel(
            customerRepository,
            currentUserIdentityContextRepository,
            customerId,
        ) as T
    }
}
