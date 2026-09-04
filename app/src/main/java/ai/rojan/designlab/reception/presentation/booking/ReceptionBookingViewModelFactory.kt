package ai.rojan.designlab.reception.presentation.booking

import ai.rojan.designlab.reception.data.ReceptionRepositories
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Factory for [ReceptionBookingViewModel] — built with
 * `viewModelFactory { initializer { ... } }` + [createSavedStateHandle]
 * (via `CreationExtras`), mirroring
 * [ai.rojan.designlab.presentation.booking.BookingViewModelFactory].
 *
 * **5B6-1:** `createSavedStateHandle()` sourced from the `CreationExtras`
 * supplied at the call site (`receptionBookingViewModelFor` in
 * `ReceptionNavGraph.kt`, which passes
 * `parentEntry.defaultViewModelCreationExtras`) is the API Navigation
 * itself restores through — so the wizard's selections survive process
 * death. [salonId] must already be resolved — see [ReceptionRepositories]'s
 * own doc comment.
 */
fun ReceptionBookingViewModelFactory(
    appContext: Context,
    salonId: String,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val repositories = ReceptionRepositories.from(appContext, salonId)
        ReceptionBookingViewModel(
            savedStateHandle = createSavedStateHandle(),
            salonId = salonId,
            bookingRepository = repositories.bookingRepository,
            customerRepository = repositories.customerRepository,
            serviceRepository = repositories.serviceRepository,
            serviceCategoryRepository = repositories.serviceCategoryRepository,
            specialistRepository = repositories.specialistRepository,
            availabilityRepository = repositories.availabilityRepository,
        )
    }
}
