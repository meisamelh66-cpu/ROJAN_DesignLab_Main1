package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.data.ManagerRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [ManagerStaffAccessViewModel], same idiom as [ManagerInviteReceptionViewModelFactory]. */
class ManagerStaffAccessViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = BackendApiContainerHolder.get(appContext)
        return ManagerStaffAccessViewModel(
            salonId = ManagerRepositories.salonId,
            repository = container.salonMembershipRepository,
        ) as T
    }
}
