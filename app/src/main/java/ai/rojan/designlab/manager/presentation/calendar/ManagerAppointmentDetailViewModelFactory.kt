package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.data.ManagerRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ManagerAppointmentDetailViewModel], same idiom as [ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModelFactory]. */
class ManagerAppointmentDetailViewModelFactory(
    private val appointmentId: String,
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = BackendApiContainerHolder.get(appContext)
        return ManagerAppointmentDetailViewModel(
            appointmentId = appointmentId,
            appointmentRepository = ManagerRepositories.appointments,
            genericBookingRepository = container.bookingRepository,
        ) as T
    }
}
