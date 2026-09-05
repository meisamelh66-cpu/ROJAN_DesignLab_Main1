package ai.rojan.designlab.presentation.profile

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TEAM2-004 (Real Customer Booking Data). [AppointmentsViewModel] is
 * constructed from nothing but backend-facing repository interfaces — no
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel],
 * no `data.demo` type, appears anywhere in its dependency list. These
 * tests confirm both what the constructor signature already proves
 * structurally (no local/demo dependency exists to fall back to) and the
 * runtime behavior: every state the UI can render is traceable to a real
 * [BookingRepository] call, never a fabricated substitute for one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val pendingBooking = Booking(
        id = "booking-pending-1",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = BookingStatus.PENDING,
        notes = null,
    )

    private val completedBooking = Booking(
        id = "booking-completed-1",
        salonId = "salon-1",
        serviceId = "service-2",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-01-05T09:00:00",
        endTime = "2026-01-05T09:45:00",
        status = BookingStatus.COMPLETED,
        notes = null,
    )

    private fun viewModel(
        bookingRepository: BookingRepository,
        salonRepository: SalonRepository = FakeSalonRepository(Result.success(Salon("salon-1", "Real Salon", null, "000", null, "addr"))),
        specialistRepository: SpecialistRepository = FakeSpecialistRepository(
            Result.success(Specialist("specialist-1", "salon-1", "Real Specialist", null, null)),
        ),
        serviceCategoryRepository: ServiceCategoryRepository = FakeServiceCategoryRepository(
            Result.success(listOf(ServiceCategory("cat-1", "salon-1", "Category", null))),
        ),
        serviceRepository: ServiceRepository = FakeServiceRepository(
            Result.success(
                listOf(
                    Service("service-1", "salon-1", "cat-1", "Real Service 1", null, 30, 200_000.0),
                    Service("service-2", "salon-1", "cat-1", "Real Service 2", null, 45, 350_000.0),
                ),
            ),
        ),
    ) = AppointmentsViewModel(bookingRepository, salonRepository, specialistRepository, serviceCategoryRepository, serviceRepository)

    @Test
    fun `real backend bookings are fetched and resolved into Success, never from local demo state`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(PagedResult(listOf(pendingBooking, completedBooking), 0, 100, 2, 1)))

        val viewModel = viewModel(repository)

        assertTrue(repository.myBookingsCalled)
        val state = viewModel.state as UiState.Success
        assertEquals(2, state.data.size)
        val pending = state.data.first { it.id == "booking-pending-1" }
        assertEquals("Real Salon", pending.salonName)
        assertEquals("Real Service 1", pending.serviceName)
        assertEquals("Real Specialist", pending.specialistName)
        assertEquals(BookingStatus.PENDING, pending.status)
    }

    @Test
    fun `an empty backend response shows Empty, not a fabricated list`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 100, 0, 0)))

        val viewModel = viewModel(repository)

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `a 401 (unauthorized) response surfaces as Error, not as an empty or successful list`() = runBlocking {
        val repository = FakeBookingRepository(Result.failure(BackendApiException(statusCode = 401, apiError = null)))

        val viewModel = viewModel(repository)

        val state = viewModel.state
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.isNotBlank())
    }

    @Test
    fun `a network failure shows Error state, not a silently empty or successful list`() = runBlocking {
        val repository = FakeBookingRepository(Result.failure(NetworkUnavailableException(Exception("offline"))))

        val viewModel = viewModel(repository)

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `a booking whose salon lookup fails shows an honest placeholder, never a fabricated demo name`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            repository,
            salonRepository = FakeSalonRepository(Result.failure(NetworkUnavailableException(Exception("offline")))),
        )

        val state = viewModel.state as UiState.Success
        // "—" is this codebase's existing honest-unknown placeholder
        // (BookingConfirmationScreen.kt's SummaryRows use the same one) -
        // never one of the seeded demo salon names (e.g. "سالن رویا").
        assertEquals("—", state.data.single().salonName)
    }

    @Test
    fun `cancelling a booking calls the real cancel endpoint and refreshes from the backend`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(repository)

        viewModel.cancelBooking("booking-pending-1")

        assertEquals("booking-pending-1", repository.lastCancelledBookingId)
        assertNull(viewModel.cancellingBookingId)
        // load() ran again after the cancel completed.
        assertTrue(repository.myBookingsCallCount >= 2)
    }
}

private class FakeBookingRepository(private val myBookingsResult: Result<PagedResult<Booking>>) : BookingRepository {

    var myBookingsCalled = false
        private set
    var myBookingsCallCount = 0
        private set
    var lastCancelledBookingId: String? = null
        private set

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
        customerId: String?,
    ): Result<Booking> = error("not used by AppointmentsViewModel")

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> {
        myBookingsCalled = true
        myBookingsCallCount++
        return myBookingsResult
    }

    override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used by AppointmentsViewModel")

    override suspend fun cancelBooking(bookingId: String): Result<Booking> {
        lastCancelledBookingId = bookingId
        return myBookingsResult.map { it.content.first() }
    }

    override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used by AppointmentsViewModel")
    override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used by AppointmentsViewModel")
    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by AppointmentsViewModel")
    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by AppointmentsViewModel")
}

private class FakeSalonRepository(private val result: Result<Salon>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = result
    override suspend fun myOwnedSalons(): Result<List<Salon>> = error("not used by these tests")
}

private class FakeSpecialistRepository(private val result: Result<Specialist>) : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used by these tests")
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = result
}

private class FakeServiceCategoryRepository(private val result: Result<List<ServiceCategory>>) : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = result
}

private class FakeServiceRepository(private val result: Result<List<Service>>) : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = result
}
