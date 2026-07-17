package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.di.RoleModule
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [SessionViewModel], mirroring [ai.rojan.designlab.presentation.roleselection.RoleSelectionViewModelFactory]. */
class SessionViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SessionViewModel(RoleModule.observeSelectedRoleUseCase(appContext)) as T
    }
}
