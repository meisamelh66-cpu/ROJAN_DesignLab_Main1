package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.di.RoleModule
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [SessionViewModel], mirroring
 * [ai.rojan.designlab.presentation.roleselection.RoleSelectionViewModelFactory].
 *
 * UX Refactor Phase 2: also constructs [AuthSessionRepositoryImpl] so
 * [SessionViewModel] can restore the persisted logged-in customer
 * alongside the persisted [ai.rojan.designlab.domain.model.Role].
 */
class SessionViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authSessionRepository = AuthSessionRepositoryImpl(appContext.applicationContext.authSessionDataStore)
        return SessionViewModel(
            RoleModule.observeSelectedRoleUseCase(appContext),
            authSessionRepository,
        ) as T
    }
}
