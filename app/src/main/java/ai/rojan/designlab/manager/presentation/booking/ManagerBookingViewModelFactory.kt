package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.data.ManagerRepositories
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerBookingViewModel], same idiom as
 * [ai.rojan.designlab.presentation.auth.AuthViewModelFactory] — wires
 * [ManagerRepositories]' in-memory repositories in, nowhere else.
 */
class ManagerBookingViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerBookingViewModel(
            customerRepository = ManagerRepositories.customers,
            serviceRepository = ManagerRepositories.services,
            specialistRepository = ManagerRepositories.specialists,
            appointmentRepository = ManagerRepositories.appointments,
        ) as T
    }
}
