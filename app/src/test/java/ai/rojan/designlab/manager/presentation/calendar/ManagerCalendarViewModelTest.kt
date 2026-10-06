package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * [ManagerCalendarViewModel] is constructed from backend-facing repository interfaces
 * and targets the active salon from [ActiveSalonContextRepository].
 * Covers Loading/Success/Empty/Error/Unauthorized, salon switching, missing active salon,
 * page-size preservation, plus the two real status-update actions (confirm/complete).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerCalendarViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val salon = Salon("salon-1", "سالن رویان", null, "000", null, "تهران")

    private val pendingBooking = Booking(
        id = "booking-1",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = BookingStatus.PENDING,
        notes = null,
    )

    private fun viewModel(
        bookingRepository: FakeBookingRepository,
        activeSalonContextRepository: FakeActiveSalonContextRepository = FakeActiveSalonContextRepository("salon-1"),
        salons: List<Salon> = listOf(salon),
    ) = ManagerCalendarViewModel(
        activeSalonContextRepository = activeSalonContextRepository,
        salonRepository = FakeSalonRepository(Result.success(salons)),
        bookingRepository = bookingRepository,
        specialistRepository = FakeSpecialistRepository(
            Result.success(listOf(Specialist("specialist-1", "salon-1", "متخصص", null, null))),
        ),
        serviceCategoryRepository = FakeServiceCategoryRepository(
            Result.success(listOf(ServiceCategory("cat-1", "salon-1", "دسته", null))),
        ),
        serviceRepository = FakeServiceRepository(
            Result.success(listOf(Service("service-1", "salon-1", "cat-1", "خدمت", null, 30, 100_000.0))),
        ),
    )

    @Test
    fun `real salon bookings load into Success with resolved specialist and service names`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1))))

        val state = viewModel.state as UiState.Success
        val appointment = state.data.appointments.single()
        assertEquals("متخصص", appointment.specialistName)
        assertEquals("خدمت", appointment.serviceName)
        assertEquals("—", appointment.customerLabel)
    }

    @Test
    fun `an empty salon booking list is Empty, not an error`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(myBookingsResult = Result.success(PagedResult(emptyList(), 0, 100, 0, 0))))

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `missing active salon is Empty and does not query bookings or fall back to firstOwnedSalon`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = null)
        val bookingRepo = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
            salons = listOf(salon),
        )

        assertEquals(UiState.Empty, viewModel.state)
        assertEquals(0, bookingRepo.salonBookingsCallCount)
        assertNull(bookingRepo.lastRequestedSalonId)
    }

    @Test
    fun `Calendar uses active salon ID rather than first owned salon`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = "salon-active-99")
        val firstOwnedSalon = Salon("salon-owned-1", "مالک اول", null, "000", null, "تهران")
        val bookingRepo = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
            salons = listOf(firstOwnedSalon),
        )

        assertEquals("salon-active-99", bookingRepo.lastRequestedSalonId)
        val state = viewModel.state as UiState.Success
        assertEquals("salon-active-99", state.data.salonId)
    }

    @Test
    fun `changing active salon causes subsequent booking loads to target the new salon`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = "salon-1")
        val bookingRepo = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
        )

        assertEquals("salon-1", bookingRepo.lastRequestedSalonId)

        activeContext.saveActiveSalonId("salon-2")

        assertEquals("salon-2", bookingRepo.lastRequestedSalonId)
        val state = viewModel.state as UiState.Success
        assertEquals("salon-2", state.data.salonId)
    }

    @Test
    fun `page size behavior passes 100 to salonBookings`() = runBlocking {
        val bookingRepo = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        viewModel(bookingRepo)

        assertEquals(100, bookingRepo.lastRequestedPageSize)
    }

    @Test
    fun `a network failure loading salon bookings shows Error state`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(myBookingsResult = Result.failure(NetworkUnavailableException(Exception("offline")))))

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `a 401 sets requiresReauth instead of a retriable Error`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(myBookingsResult = Result.failure(BackendApiException(401, null))))

        assertTrue(viewModel.requiresReauth)
    }

    @Test
    fun `confirming a booking calls the real confirm endpoint and refreshes from the backend`() = runBlocking {
        val repository = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(repository)

        viewModel.confirmAppointment("booking-1")

        assertEquals("booking-1", repository.lastConfirmedBookingId)
        assertNull(viewModel.updatingBookingId)
        assertTrue(repository.salonBookingsCallCount >= 2)
    }

    @Test
    fun `completing a booking calls the real complete endpoint and refreshes from the backend`() = runBlocking {
        val repository = FakeBookingRepository(myBookingsResult = Result.success(PagedResult(listOf(pendingBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(repository)

        viewModel.completeAppointment("booking-1")

        assertEquals("booking-1", repository.lastCompletedBookingId)
        assertTrue(repository.salonBookingsCallCount >= 2)
    }
}

private class FakeActiveSalonContextRepository(initialSalonId: String? = "salon-1") : ActiveSalonContextRepository {
    private val flow = MutableStateFlow(initialSalonId)

    override suspend fun saveActiveSalonId(salonId: String) {
        flow.value = salonId
    }

    override suspend fun clearActiveSalonId() {
        flow.value = null
    }

    override fun observeActiveSalonId(): Flow<String?> = flow
}

private class FakeSalonRepository(private val result: Result<List<Salon>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by these tests")
    override suspend fun myOwnedSalons(): Result<List<Salon>> = result
}

private class FakeBookingRepository(private val myBookingsResult: Result<PagedResult<Booking>>) : BookingRepository {

    var salonBookingsCallCount = 0
        private set
    var lastRequestedSalonId: String? = null
        private set
    var lastRequestedPageSize: Int? = null
        private set
    var lastConfirmedBookingId: String? = null
        private set
    var lastCompletedBookingId: String? = null
        private set

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
        customerId: String?,
    ): Result<Booking> = error("not used by these tests")

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by these tests")

    override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used by these tests")

    override suspend fun confirmBooking(bookingId: String): Result<Booking> {
        lastConfirmedBookingId = bookingId
        return myBookingsResult.map { it.content.first() }
    }

    override suspend fun completeBooking(bookingId: String): Result<Booking> {
        lastCompletedBookingId = bookingId
        return myBookingsResult.map { it.content.first() }
    }

    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by these tests")

    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> {
        salonBookingsCallCount++
        lastRequestedSalonId = salonId
        lastRequestedPageSize = size
        return myBookingsResult
    }
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
