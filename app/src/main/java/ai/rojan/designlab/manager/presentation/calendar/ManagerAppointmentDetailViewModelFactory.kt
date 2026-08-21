package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.manager.data.ManagerRepositories
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerAppointmentDetailViewModel], same idiom as
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModelFactory] —
 * wires [ManagerRepositories]' in-memory repository in, nowhere else.
 */
class ManagerAppointmentDetailViewModelFactory(
    private val appointmentId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerAppointmentDetailViewModel(
            appointmentRepository = ManagerRepositories.appointments,
            appointmentId = appointmentId,
        ) as T
    }
}
