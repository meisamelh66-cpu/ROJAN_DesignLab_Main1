package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.BookingRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class BookingConfirmationViewModelFactory(
    private val bookingRepository: BookingRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookingConfirmationViewModel(bookingRepository) as T
    }
}
