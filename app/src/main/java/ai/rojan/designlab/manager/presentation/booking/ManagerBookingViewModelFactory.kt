package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.data.ManagerRepositories
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Factory for [ManagerBookingViewModel] — built with the current,
 * non-deprecated `viewModelFactory { initializer { ... } }` +
 * [createSavedStateHandle] APIs (via `CreationExtras`), mirroring
 * [ai.rojan.designlab.presentation.booking.BookingViewModelFactory].
 *
 * **5B6-1:** `createSavedStateHandle()` sourced from the `CreationExtras`
 * supplied at the call site (`managerBookingViewModelFor` in
 * `ManagerNavGraph.kt`, which passes
 * `parentEntry.defaultViewModelCreationExtras`) is the API Navigation
 * itself restores through — so the wizard's selections survive process
 * death. Wires [ManagerRepositories]' in-memory repositories in, nowhere
 * else.
 */
val ManagerBookingViewModelFactory = viewModelFactory {
    initializer {
        ManagerBookingViewModel(
            savedStateHandle = createSavedStateHandle(),
            customerRepository = ManagerRepositories.customers,
            serviceRepository = ManagerRepositories.services,
            specialistRepository = ManagerRepositories.specialists,
            appointmentRepository = ManagerRepositories.appointments,
        )
    }
}
