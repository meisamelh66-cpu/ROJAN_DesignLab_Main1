package ai.rojan.designlab.reception.presentation.invite

import ai.rojan.designlab.data.local.authSessionDataStore
import ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl
import ai.rojan.designlab.di.BackendApiContainerHolder
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ReceptionInviteAcceptanceViewModel] — same
 * construction idiom as [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModelFactory]
 * (this codebase has no DI framework), captures [appContext] (never a raw
 * Activity `Context`) since this ViewModel, like every other `NavHost`/
 * screen-scoped ViewModel in this codebase, can outlive the Activity across
 * a configuration change.
 */
class ReceptionInviteAcceptanceViewModelFactory(
    private val appContext: Context,
    private val token: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val applicationContext = appContext.applicationContext
        val backendApiContainer = BackendApiContainerHolder.get(applicationContext)
        val authSessionRepository = AuthSessionRepositoryImpl(applicationContext.authSessionDataStore)

        return ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { backendApiContainer.receptionInviteRepository.getInvite(it) },
            requestOtp = { phoneNumber -> backendApiContainer.backendAuthRepository.requestOtp(phoneNumber) },
            verifyOtp = { phoneNumber, code -> backendApiContainer.backendAuthRepository.verifyOtp(phoneNumber, code) },
            acceptInvite = { backendApiContainer.receptionInviteRepository.acceptInvite(it) },
            getIdentityContext = { backendApiContainer.currentUserIdentityContextRepository.getCurrentUserIdentityContext() },
            saveActiveSalonId = { salonId -> backendApiContainer.activeSalonContextRepository.saveActiveSalonId(salonId) },
            savePersonId = { userId -> authSessionRepository.savePersonId(userId) },
        ) as T
    }
}
