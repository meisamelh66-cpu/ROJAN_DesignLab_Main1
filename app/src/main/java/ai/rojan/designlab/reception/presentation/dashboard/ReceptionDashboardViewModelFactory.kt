package ai.rojan.designlab.reception.presentation.dashboard

import ai.rojan.designlab.reception.data.ReceptionRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ReceptionDashboardViewModelFactory(
    private val appContext: Context,
    private val salonId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repositories = ReceptionRepositories.from(appContext, salonId)
        return ReceptionDashboardViewModel(
            salonId = salonId,
            bookingRepository = repositories.bookingRepository,
            genericBookingRepository = repositories.genericBookingRepository,
        ) as T
    }
}
