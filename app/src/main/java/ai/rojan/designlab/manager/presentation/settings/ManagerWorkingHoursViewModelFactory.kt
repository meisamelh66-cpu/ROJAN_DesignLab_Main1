package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.data.ManagerRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ManagerWorkingHoursViewModel], same idiom as [ManagerSalonSetupViewModelFactory]. */
class ManagerWorkingHoursViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = BackendApiContainerHolder.get(appContext)
        return ManagerWorkingHoursViewModel(
            salonId = ManagerRepositories.salonId,
            repository = container.managerWorkingHoursRepository,
        ) as T
    }
}
