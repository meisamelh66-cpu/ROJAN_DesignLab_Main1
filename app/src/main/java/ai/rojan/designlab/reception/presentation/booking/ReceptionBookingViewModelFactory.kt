package ai.rojan.designlab.reception.presentation.booking

import ai.rojan.designlab.reception.data.ReceptionRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ReceptionBookingViewModel], same idiom as every other factory in this codebase (no DI framework). [salonId] must already be resolved — see [ReceptionRepositories]'s own doc comment. */
class ReceptionBookingViewModelFactory(
    private val appContext: Context,
    private val salonId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repositories = ReceptionRepositories.from(appContext, salonId)
        return ReceptionBookingViewModel(
            salonId = salonId,
            bookingRepository = repositories.bookingRepository,
            customerRepository = repositories.customerRepository,
            serviceRepository = repositories.serviceRepository,
            serviceCategoryRepository = repositories.serviceCategoryRepository,
            specialistRepository = repositories.specialistRepository,
            availabilityRepository = repositories.availabilityRepository,
        ) as T
    }
}
