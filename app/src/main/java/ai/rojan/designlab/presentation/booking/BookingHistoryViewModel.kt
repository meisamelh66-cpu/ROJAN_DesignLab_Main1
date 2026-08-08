package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.BookingHistoryRepository
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

/**
 * Backs every appointment-history surface (Customer Home's Upcoming/
 * Previous sections, Profile's My Appointments) with one real
 * `myBookings` read via [BookingHistoryRepository] — a single page,
 * split into upcoming/past client-side by [ai.rojan.designlab.domain.repository.BookingStatus]
 * (a real field already on each booking, not a fabricated grouping).
 */
class BookingHistoryViewModel(
    private val bookingHistoryRepository: BookingHistoryRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<BookingWithDetails>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            bookingHistoryRepository.myBookingsWithDetails(page = 0, size = PAGE_SIZE)
                .onSuccess { paged ->
                    state = if (paged.content.isEmpty()) UiState.Empty else UiState.Success(paged.content)
                }
                .onFailure { error ->
                    state = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retry() = load()
}

class BookingHistoryViewModelFactory(
    private val bookingHistoryRepository: BookingHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookingHistoryViewModel(bookingHistoryRepository) as T
    }
}
