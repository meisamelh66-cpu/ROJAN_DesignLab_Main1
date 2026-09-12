package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingHistoryRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Release-readiness guard fix: [BookingHistoryViewModel.load] now cancels
 * any in-flight request before starting a new one — same pattern already
 * proven in [ai.rojan.designlab.presentation.salon.SalonListViewModel.load].
 * Real exposure: `retry()` is reachable from a tappable button
 * ([ai.rojan.designlab.screens.profile.AppointmentsScreen]) and is also
 * re-invoked immediately after a successful cancel-booking action, so a
 * rapid double-tap (or a cancel racing a manual retry) could otherwise
 * fire two concurrent loads racing to set [BookingHistoryViewModel.state].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun booking(id: String) = BookingWithDetails(
        booking = Booking(
            id = id,
            salonId = "salon-1",
            serviceId = "service-1",
            specialistId = "specialist-1",
            customerId = "customer-1",
            startTime = "2026-09-12T10:00:00",
            endTime = "2026-09-12T10:30:00",
            status = BookingStatus.CONFIRMED,
            notes = null,
        ),
        salonName = "Glow Salon",
        specialistName = "Sara",
    )

    private fun paged(vararg ids: String) = PagedResult(
        content = ids.map { booking(it) },
        page = 0,
        size = 50,
        totalElements = ids.size.toLong(),
        totalPages = 1,
    )

    /** Never resolves until told to - lets a test observe the moment between `load()` and the response arriving. */
    private class ScriptedBookingHistoryRepository : BookingHistoryRepository {
        val calls = mutableListOf<CompletableDeferred<Result<PagedResult<BookingWithDetails>>>>()

        override suspend fun myBookingsWithDetails(
            page: Int,
            size: Int,
            status: ai.rojan.designlab.domain.repository.BookingStatus?,
        ): Result<PagedResult<BookingWithDetails>> {
            val deferred = CompletableDeferred<Result<PagedResult<BookingWithDetails>>>()
            calls += deferred
            return deferred.await()
        }
    }

    @Test
    fun `a normal load resolves to Success`() = runTest {
        val repository = ScriptedBookingHistoryRepository()
        val viewModel = BookingHistoryViewModel(repository)
        advanceUntilIdle() // let init{load()}'s coroutine reach deferred.await()

        repository.calls[0].complete(Result.success(paged("a")))
        advanceUntilIdle()

        val state = viewModel.state as UiState.Success
        assertEquals(1, state.data.size)
    }

    @Test
    fun `calling load twice cancels the first request - only the second response is applied`() = runTest {
        val repository = ScriptedBookingHistoryRepository()
        val viewModel = BookingHistoryViewModel(repository)
        advanceUntilIdle()
        // init{} already fired the first load - this is the "duplicate" call
        // (e.g. a rapid double-tap on retry, or cancel-booking racing retry).
        viewModel.load()
        advanceUntilIdle()

        // The first, now-cancelled call's response arrives late.
        repository.calls[0].complete(Result.success(paged("stale")))
        // The second, real call's response arrives.
        repository.calls[1].complete(Result.success(paged("fresh")))
        advanceUntilIdle()

        val state = viewModel.state as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("fresh", state.data.first().booking.id)
    }

    @Test
    fun `a stale first response arriving after the second call must not overwrite the fresh result`() = runTest {
        val repository = ScriptedBookingHistoryRepository()
        val viewModel = BookingHistoryViewModel(repository)
        advanceUntilIdle()
        viewModel.load()
        advanceUntilIdle()

        // Second (real) call resolves first...
        repository.calls[1].complete(Result.success(paged("fresh")))
        advanceUntilIdle()
        // ...then the stale first call's response arrives late. Without the
        // cancellation guard this would clobber the fresh result.
        repository.calls[0].complete(Result.success(paged("stale")))
        advanceUntilIdle()

        val state = viewModel.state as UiState.Success
        assertEquals("fresh", state.data.first().booking.id)
    }
}
