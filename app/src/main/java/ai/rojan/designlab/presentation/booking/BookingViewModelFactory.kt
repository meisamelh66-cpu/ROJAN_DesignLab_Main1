package ai.rojan.designlab.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory for [BookingViewModel], mirroring the existing pattern used by every other ViewModel in this app. No dependencies needed — this class only holds in-memory state, nothing to inject. */
class BookingViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookingViewModel() as T
    }
}
