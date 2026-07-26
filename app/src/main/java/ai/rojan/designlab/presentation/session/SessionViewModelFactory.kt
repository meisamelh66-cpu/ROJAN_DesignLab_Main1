package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.data.identity.DemoIdentityProvider
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
 *
 * UX Refactor Phase 3: also constructs a [DemoIdentityProvider] so
 * [SessionViewModel] can resolve a restored person's roles — deliberately
 * a second, separate instance from [ai.rojan.designlab.presentation.auth.AuthViewModelFactory]'s;
 * see [SessionViewModel]'s doc comment for why that's safe here.
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
            DemoIdentityProvider(),
        ) as T
    }
}
