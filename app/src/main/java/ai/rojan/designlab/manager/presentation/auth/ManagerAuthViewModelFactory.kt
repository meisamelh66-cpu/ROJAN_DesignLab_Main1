package ai.rojan.designlab.manager.presentation.auth

import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.di.BackendApiContainerHolder
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerAuthViewModel], mirroring
 * [ai.rojan.designlab.presentation.auth.AuthViewModelFactory]'s exact
 * construction pattern (this codebase has no DI framework — see
 * `di/BackendApiContainer.kt`'s own doc comment) minus the Customer-only
 * `Demo*` identity/session providers, which [ManagerAuthViewModel]
 * deliberately does not depend on.
 *
 * [authSessionDataStore] resolves to the Manager APK's own private
 * DataStore file — Customer and Manager are separately installable apps
 * (different `applicationId`, see `app/build.gradle.kts`'s product
 * flavors), each with its own OS-level app-private storage sandbox, so
 * reusing the exact same [AuthSessionRepositoryImpl]/[ai.rojan.designlab.data.repository.TokenRepositoryImpl]
 * classes here never risks reading or writing Customer's session.
 */
class ManagerAuthViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authSessionRepository = AuthSessionRepositoryImpl(appContext.applicationContext.authSessionDataStore)
        val backendApiContainer = BackendApiContainerHolder.get(appContext)
        return ManagerAuthViewModel(
            authSessionRepository = authSessionRepository,
            backendAuthRepository = backendApiContainer.backendAuthRepository,
            tokenRepository = backendApiContainer.tokenRepository,
            currentUserIdentityContextRepository = backendApiContainer.currentUserIdentityContextRepository,
        ) as T
    }
}
