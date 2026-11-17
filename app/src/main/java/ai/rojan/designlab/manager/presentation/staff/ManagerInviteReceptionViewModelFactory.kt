package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.data.ManagerRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ManagerInviteReceptionViewModel], same idiom as [ai.rojan.designlab.manager.presentation.settings.ManagerWorkingHoursViewModelFactory]. */
class ManagerInviteReceptionViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = BackendApiContainerHolder.get(appContext)
        return ManagerInviteReceptionViewModel(
            salonId = ManagerRepositories.salonId,
            repository = container.managerInviteRepository,
        ) as T
    }
}
