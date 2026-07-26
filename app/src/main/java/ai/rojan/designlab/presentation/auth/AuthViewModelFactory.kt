package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.identity.DemoSessionProvider
import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [AuthViewModel], mirroring
 * [ai.rojan.designlab.presentation.session.SessionViewModelFactory].
 *
 * This is the composition root for the Mock Auth stack:
 * [DemoIdentityProvider] and [DemoSessionProvider] are constructed here,
 * once, and nowhere else — exactly one instance for the whole app run,
 * since [AuthViewModel] itself is root-scoped (constructed once in
 * [ai.rojan.designlab.navigation.RojanNavGraph], same pattern as
 * `SessionViewModel`). Swapping to a real backend later means replacing
 * only this factory's two `Demo*` constructions — [AuthViewModel], every
 * screen, and every other ViewModel stay exactly as they are.
 *
 * UX Refactor Phase 2: [appContext] added to also construct
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
        return AuthViewModel(sessionProvider, identityProvider, authSessionRepository) as T
    }
}
