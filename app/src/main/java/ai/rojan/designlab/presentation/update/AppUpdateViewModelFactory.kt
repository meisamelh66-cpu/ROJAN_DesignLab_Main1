package ai.rojan.designlab.presentation.update

import ai.rojan.designlab.domain.repository.AppUpdateRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app (e.g. `ManagerDashboardViewModelFactory`). */
class AppUpdateViewModelFactory(
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppUpdateViewModel(appUpdateRepository) as T
}
