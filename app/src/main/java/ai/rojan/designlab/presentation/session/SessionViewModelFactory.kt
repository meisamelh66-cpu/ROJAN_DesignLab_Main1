package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [SessionViewModel] — constructs [AuthSessionRepositoryImpl]
 * so [SessionViewModel] can restore the persisted logged-in person, and a
 * [DemoIdentityProvider] (deliberately a second, separate instance from
 * [ai.rojan.designlab.presentation.auth.AuthViewModelFactory]'s — see
 * [SessionViewModel]'s doc comment for why that's safe here) so it can
 * resolve that person's roles.
 */
class SessionViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authSessionRepository = AuthSessionRepositoryImpl(appContext.applicationContext.authSessionDataStore)
        return SessionViewModel(
            authSessionRepository,
            DemoIdentityProvider(),
        ) as T
    }
}
