package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.UserProfileRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ProfileMediaViewModel], mirroring the other Customer
 * ViewModel factories. [onUserUpdated] is wired to
 * `AuthViewModel.applyUpdatedUser` at the call site (`ProfileScreen`), so a
 * successful avatar/cover change flows straight back into the shared
 * `currentUser` state every screen renders from.
 */
class ProfileMediaViewModelFactory(
    private val repository: UserProfileRepository,
    private val onUserUpdated: (AuthenticatedUser) -> Unit,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProfileMediaViewModel(repository = repository, onUserUpdated = onUserUpdated) as T
}
