package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.identity.DemoSessionProvider
import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.di.BackendApiContainerHolder
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [AuthViewModel], mirroring
 * [ai.rojan.designlab.presentation.session.SessionViewModelFactory].
 *
 * [DemoIdentityProvider]/[DemoSessionProvider] are still constructed here —
 * [AuthViewModel] keeps using their session-state machinery for a real
 * backend id too (see that class's own doc comment) — alongside the real
 * [ai.rojan.designlab.domain.repository.BackendAuthRepository]/
 * [ai.rojan.designlab.domain.repository.TokenRepository] from
 * [BackendApiContainerHolder], which is where login/register/logout
 * actually talk to the backend.
 *
 * UX Refactor Phase 2: [appContext] also constructs
 * [AuthSessionRepositoryImpl], the persistence layer that lets
 * [AuthViewModel] survive a cold start.
 */
class AuthViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val identityProvider = DemoIdentityProvider()
        val sessionProvider = DemoSessionProvider(identityProvider)
        val authSessionRepository = AuthSessionRepositoryImpl(appContext.applicationContext.authSessionDataStore)
        val backendApiContainer = BackendApiContainerHolder.get(appContext)
        return AuthViewModel(
            sessionProvider = sessionProvider,
            identityProvider = identityProvider,
            authSessionRepository = authSessionRepository,
            backendAuthRepository = backendApiContainer.backendAuthRepository,
            tokenRepository = backendApiContainer.tokenRepository,
        ) as T
    }
}
