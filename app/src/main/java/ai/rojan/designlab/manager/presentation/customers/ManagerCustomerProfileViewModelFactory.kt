package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.ManagerCustomerProfileRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerCustomerProfileViewModel], mirroring
 * [ManagerCustomersViewModelFactory]. Wires the real backend repositories
 * from [ai.rojan.designlab.di.BackendApiContainer].
 */
class ManagerCustomerProfileViewModelFactory(
    private val accountId: String,
    private val salonRepository: SalonRepository,
    private val profileRepository: ManagerCustomerProfileRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerCustomerProfileViewModel(accountId, salonRepository, profileRepository) as T
    }
}
