package ai.rojan.designlab.presentation.roleselection

import ai.rojan.designlab.di.RoleModule
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [RoleSelectionViewModel] — this project has no
 * dependency-injection framework wired up yet, so the use case is
 * constructed by hand via [RoleModule] rather than the ViewModel
 * reaching into DataStore/Context itself.
 */
class RoleSelectionViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoleSelectionViewModel(RoleModule.saveSelectedRoleUseCase(appContext)) as T
    }
}
