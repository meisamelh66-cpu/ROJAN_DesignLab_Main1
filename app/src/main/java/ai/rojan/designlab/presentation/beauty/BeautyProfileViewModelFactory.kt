package ai.rojan.designlab.presentation.beauty

import ai.rojan.designlab.domain.beauty.BeautyProfileRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class BeautyProfileViewModelFactory(
    private val customerId: String,
    private val repository: BeautyProfileRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BeautyProfileViewModel(customerId, repository) as T
    }
}
