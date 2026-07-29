package ai.rojan.designlab.presentation.booking

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Factory for [BookingViewModel] — built with the current, non-deprecated
 * `viewModelFactory { initializer { ... } }` + [createSavedStateHandle]
 * APIs (via `CreationExtras`), replacing the previous
 * `AbstractSavedStateViewModelFactory(owner, defaultArgs)` legacy
 * constructor.
 *
 * **Booking Flow Repair (P0):** that legacy constructor was the
 * confirmed root cause of Booking Confirmation receiving empty
 * `BookingState` after a real process death — it does not reliably hook
 * into Navigation-Compose's actual state-restoration path for a nested
 * graph's `NavBackStackEntry`, so the `SavedStateHandle` it produced
 * came back empty even though `persistState()` had genuinely written
 * every field before the process died. `createSavedStateHandle()`,
 * sourced from the `CreationExtras` supplied at the call site (see
 * `bookingViewModelFor` in `RojanNavGraph.kt`), is the API Navigation
 * itself is built to restore correctly.
 *
 * No `owner` parameter any more — scoping to the booking flow's nested
 * graph `NavBackStackEntry` now happens entirely via the `extras` passed
 * at the call site, not via this factory's constructor.
 */
val BookingViewModelFactory = viewModelFactory {
    initializer {
        BookingViewModel(savedStateHandle = createSavedStateHandle())
    }
}
