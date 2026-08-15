package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.di.BackendApiContainerHolder
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ManagerSalonSetupViewModel], same idiom as every other factory in this codebase (no DI framework) — e.g. [ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModelFactory]. */
class ManagerSalonSetupViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = BackendApiContainerHolder.get(appContext)
        return ManagerSalonSetupViewModel(salonRepository = container.managerSalonRepository) as T
    }
}
