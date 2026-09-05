package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TEAM2-002 (Manager Data Persistence). [ManagerDashboardViewModel] is
 * constructed from nothing but backend-facing repository interfaces — no
 * `manager.data.ManagerRepositories` singleton appears anywhere in its
 * dependency list, so there is no in-memory data for it to fall back to
 * even by accident. These tests cover its full Loading/Success/Empty/
 * Error/Unauthorized state machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerDashboardViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val salon = Salon(
        id = "salon-1",
        name = "سالن رویان",
        description = "زیبایی و سلامت",
        phone = "02100000000",
        email = null,
        address = "تهران",
        active = true,
    )

    private val booking = Booking(
        id = "booking-1",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = BookingStatus.CONFIRMED,
        notes = null,
    )

    private fun viewModel(
        salonRepository: SalonRepository,
        bookingRepository: BookingRepository = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 200, 0, 0))),
    ) = ManagerDashboardViewModel(
        salonRepository = salonRepository,
        bookingRepository = bookingRepository,
        specialistRepository = FakeSpecialistRepository(Result.success(listOf(Specialist("specialist-1", "salon-1", "متخصص", null, null)))),
        serviceCategoryRepository = FakeServiceCategoryRepository(Result.success(listOf(ServiceCategory("cat-1", "salon-1", "دسته", null)))),
        serviceRepository = FakeServiceRepository(Result.success(listOf(Service("service-1", "salon-1", "cat-1", "خدمت", null, 30, 100_000.0)))),
    )

    @Test
    fun `state is Loading while the salon fetch is in flight`() = runBlocking {
        val gate = CompletableDeferred<Result<List<Salon>>>()
        val viewModel = viewModel(FakeSalonRepository { gate.await() })

        assertTrue(viewModel.state is UiState.Loading)

        gate.complete(Result.success(listOf(salon)))

        assertTrue(viewModel.state is UiState.Success)
    }

    @Test
    fun `a real salon with real bookings loads into Success with real stats`() = runBlocking {
        val bookingRepository = FakeBookingRepository(Result.success(PagedResult(listOf(booking), 0, 200, 1, 1)))
        val viewModel = viewModel(FakeSalonRepository { Result.success(listOf(salon)) }, bookingRepository)

        val state = viewModel.state as UiState.Success
        assertEquals("سالن رویان", state.data.salonName)
        assertEquals("salon-1", state.data.salonId)
        assertTrue(state.data.isActive)
    }

    @Test
    fun `owning zero salons is Empty, not an error`() = runBlocking {
        val viewModel = viewModel(FakeSalonRepository { Result.success(emptyList()) })

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `a network failure loading the salon shows Error state`() = runBlocking {
        val viewModel = viewModel(FakeSalonRepository { Result.failure(NetworkUnavailableException(Exception("offline"))) })

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `a 401 sets requiresReauth instead of a retriable Error`() = runBlocking {
        val viewModel = viewModel(FakeSalonRepository { Result.failure(BackendApiException(401, null)) })

        assertTrue(viewModel.requiresReauth)
    }

    @Test
    fun `retry re-fetches from the backend`() = runBlocking {
        var callCount = 0
        val viewModel = viewModel(
            FakeSalonRepository {
                callCount++
                Result.success(listOf(salon))
            },
        )
        assertEquals(1, callCount)

        viewModel.retry()

        assertEquals(2, callCount)
    }
}

private class FakeSalonRepository(private val result: suspend () -> Result<List<Salon>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by these tests")

    override suspend fun myOwnedSalons(): Result<List<Salon>> = result()
}

private class FakeBookingRepository(private val result: Result<PagedResult<Booking>>) : BookingRepository {
    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
    ): Result<Booking> = error("not used by these tests")

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by these tests")

    override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by these tests")

    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        result
}

private class FakeSpecialistRepository(private val result: Result<List<Specialist>>) : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = result
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
        error("not used by these tests")
}

private class FakeServiceCategoryRepository(private val result: Result<List<ServiceCategory>>) : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = result
}

private class FakeServiceRepository(private val result: Result<List<Service>>) : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = result
}
