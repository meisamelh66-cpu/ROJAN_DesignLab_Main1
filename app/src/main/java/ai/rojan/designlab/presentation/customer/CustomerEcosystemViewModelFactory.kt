package ai.rojan.designlab.presentation.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class CustomerEcosystemViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomerEcosystemViewModel() as T
    }
}
