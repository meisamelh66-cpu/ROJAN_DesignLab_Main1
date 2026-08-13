package ai.rojan.designlab.reception.presentation.auth

import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.di.BackendApiContainerHolder
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ReceptionAuthViewModel], mirroring
 * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelFactory]'s
 * exact construction pattern (this codebase has no DI framework).
 *
 * [authSessionDataStore] resolves to the Reception APK's own private
 * DataStore file — Customer/Manager/Reception are separately installable
 * apps (different `applicationId`, see `app/build.gradle.kts`'s product
 * flavors), each with its own OS-level app-private storage sandbox, so
 * reusing the exact same [AuthSessionRepositoryImpl] class here never risks
 * reading or writing another flavor's session (identical reasoning to
 * `ManagerAuthViewModelFactory`'s own doc comment).
 */
class ReceptionAuthViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authSessionRepository = AuthSessionRepositoryImpl(appContext.applicationContext.authSessionDataStore)
        val backendApiContainer = BackendApiContainerHolder.get(appContext)
        return ReceptionAuthViewModel(
            authSessionRepository = authSessionRepository,
            backendAuthRepository = backendApiContainer.backendAuthRepository,
            tokenRepository = backendApiContainer.tokenRepository,
            currentUserIdentityContextRepository = backendApiContainer.currentUserIdentityContextRepository,
            activeSalonContextRepository = backendApiContainer.activeSalonContextRepository,
        ) as T
    }
}
