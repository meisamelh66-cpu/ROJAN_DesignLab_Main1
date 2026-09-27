package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory, mirroring every other ViewModel factory in this app (e.g. `ManagerDashboardViewModelFactory`).
 *
 * Phase B retarget (Customers ViewModel migration): [customerRepositoryProvider] type follows
 * [ManagerCustomersViewModel]'s current constructor - the same working Manager CRM abstraction
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] already exposes, not the old
 * [ai.rojan.designlab.domain.repository.SalonCustomerRepository].
 *
 * Phase F2 Timing Fix: [customerRepositoryProvider] is `() -> CustomerRepository`, not a captured
 * instance - this factory just passes the lambda through unevaluated. The call site (e.g. `{
 * ManagerRepositories.customers }`) decides how it's resolved; this factory never reads
 * `ManagerRepositories.customers` itself.
 */
class ManagerCustomersViewModelFactory(
    private val customerRepositoryProvider: () -> CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerCustomersViewModel(customerRepositoryProvider, currentUserIdentityContextRepository) as T
    }
}
