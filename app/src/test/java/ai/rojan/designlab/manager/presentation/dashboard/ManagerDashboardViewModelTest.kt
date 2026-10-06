package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.booking.RollingBookingDates
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
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ManagerDashboardViewModel] is constructed from backend-facing repository interfaces
 * and targets the active salon from [ActiveSalonContextRepository].
 * Tests verify Loading/Success/Empty/Error/Unauthorized, active salon resolution,
 * salon changes, empty/missing salon context, today's upcoming slots calculation, and page-size 100.
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
        bookingRepository: BookingRepository = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 100, 0, 0))),
        activeSalonContextRepository: FakeActiveSalonContextRepository = FakeActiveSalonContextRepository("salon-1"),
    ) = ManagerDashboardViewModel(
        activeSalonContextRepository = activeSalonContextRepository,
        salonRepository = salonRepository,
        bookingRepository = bookingRepository,
        specialistRepository = FakeSpecialistRepository(Result.success(listOf(Specialist("specialist-1", "salon-1", "متخصص", null, null)))),
        serviceCategoryRepository = FakeServiceCategoryRepository(Result.success(listOf(ServiceCategory("cat-1", "salon-1", "دسته", null)))),
        serviceRepository = FakeServiceRepository(Result.success(listOf(Service("service-1", "salon-1", "cat-1", "خدمت", null, 30, 100_000.0)))),
    )

    @Test
    fun `state is Loading while the salon fetch is in flight`() = runBlocking {
        val gate = CompletableDeferred<Result<Salon>>()
        val salonRepo = FakeSalonRepository(getSalonResult = { gate.await() })
        val viewModel = viewModel(salonRepository = salonRepo)

        assertTrue(viewModel.state is UiState.Loading)

        gate.complete(Result.success(salon))

        assertTrue(viewModel.state is UiState.Success)
    }

    @Test
    fun `a real salon with real bookings loads into Success with real stats and upcoming slots`() = runBlocking {
        val todayIso = RollingBookingDates.next7Days().first().first
        val todayBooking = booking.copy(
            id = "booking-today",
            startTime = "${todayIso}T14:30:00",
            status = BookingStatus.CONFIRMED,
        )
        val bookingRepository = FakeBookingRepository(Result.success(PagedResult(listOf(todayBooking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            salonRepository = FakeSalonRepository(getSalonResult = { Result.success(salon) }),
            bookingRepository = bookingRepository,
        )

        val state = viewModel.state as UiState.Success
        assertEquals("سالن رویان", state.data.salonName)
        assertEquals("salon-1", state.data.salonId)
        assertTrue(state.data.isActive)
        assertEquals(1, state.data.stats.todaysAppointmentCount)

        val slots = state.data.todaysUpcomingSlots
        assertEquals(1, slots.size)
        assertEquals("14:30", slots.first().time)
        assertEquals("خدمت", slots.first().service)
        assertEquals("—", slots.first().clientName)
    }

    @Test
    fun `missing active salon emits Empty and does not fall back to owned salons`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = null)
        val salonRepo = FakeSalonRepository(
            myOwnedSalonsResult = { Result.success(listOf(salon)) },
        )
        val bookingRepo = FakeBookingRepository(Result.success(PagedResult(listOf(booking), 0, 100, 1, 1)))
        val viewModel = viewModel(
            salonRepository = salonRepo,
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
        )

        assertEquals(UiState.Empty, viewModel.state)
        assertEquals(0, salonRepo.getSalonCallCount)
        assertEquals(0, bookingRepo.salonBookingsCallCount)
    }

    @Test
    fun `Dashboard uses active salon ID rather than first owned salon`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = "salon-active-42")
        val activeSalon = salon.copy(id = "salon-active-42", name = "سالن فعال")
        val firstOwnedSalon = salon.copy(id = "salon-owned-1", name = "سالن مالکی")
        val salonRepo = FakeSalonRepository(
            getSalonResult = { id ->
                if (id == "salon-active-42") Result.success(activeSalon) else Result.success(firstOwnedSalon)
            },
            myOwnedSalonsResult = { Result.success(listOf(firstOwnedSalon)) },
        )
        val bookingRepo = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 100, 0, 0)))
        val viewModel = viewModel(
            salonRepository = salonRepo,
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
        )

        assertEquals("salon-active-42", salonRepo.lastRequestedSalonId)
        assertEquals("salon-active-42", bookingRepo.lastRequestedSalonId)
        val state = viewModel.state as UiState.Success
        assertEquals("salon-active-42", state.data.salonId)
        assertEquals("سالن فعال", state.data.salonName)
    }

    @Test
    fun `changing active salon causes subsequent booking loads to target the new salon`() = runBlocking {
        val activeContext = FakeActiveSalonContextRepository(initialSalonId = "salon-1")
        val salonRepo = FakeSalonRepository(
            getSalonResult = { id -> Result.success(salon.copy(id = id, name = "نام $id")) },
        )
        val bookingRepo = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 100, 0, 0)))
        val viewModel = viewModel(
            salonRepository = salonRepo,
            bookingRepository = bookingRepo,
            activeSalonContextRepository = activeContext,
        )

        assertEquals("salon-1", salonRepo.lastRequestedSalonId)
        assertEquals("salon-1", bookingRepo.lastRequestedSalonId)

        activeContext.saveActiveSalonId("salon-2")

        assertEquals("salon-2", salonRepo.lastRequestedSalonId)
        assertEquals("salon-2", bookingRepo.lastRequestedSalonId)
        val state = viewModel.state as UiState.Success
        assertEquals("salon-2", state.data.salonId)
        assertEquals("نام salon-2", state.data.salonName)
    }

    @Test
    fun `page size behavior passes 100 to salonBookings`() = runBlocking {
        val bookingRepo = FakeBookingRepository(Result.success(PagedResult(emptyList(), 0, 100, 0, 0)))
        viewModel(
            salonRepository = FakeSalonRepository(getSalonResult = { Result.success(salon) }),
            bookingRepository = bookingRepo,
        )

        assertEquals(100, bookingRepo.lastRequestedPageSize)
    }

    @Test
    fun `a network failure loading the salon shows Error state`() = runBlocking {
        val viewModel = viewModel(FakeSalonRepository(getSalonResult = { Result.failure(NetworkUnavailableException(Exception("offline"))) }))

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `a 401 sets requiresReauth instead of a retriable Error`() = runBlocking {
        val viewModel = viewModel(FakeSalonRepository(getSalonResult = { Result.failure(BackendApiException(401, null)) }))

        assertTrue(viewModel.requiresReauth)
    }

    @Test
    fun `retry re-fetches from the backend`() = runBlocking {
        val salonRepo = FakeSalonRepository(getSalonResult = { Result.success(salon) })
        val viewModel = viewModel(salonRepo)
        assertEquals(1, salonRepo.getSalonCallCount)

        viewModel.retry()

        assertEquals(2, salonRepo.getSalonCallCount)
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

private class FakeSalonRepository(
    private val getSalonResult: suspend (String) -> Result<Salon> = { salonId ->
        Result.success(
            Salon(
                id = salonId,
                name = "سالن رویان",
                description = "زیبایی و سلامت",
                phone = "02100000000",
                email = null,
                address = "تهران",
                active = true,
            ),
        )
    },
    private val myOwnedSalonsResult: suspend () -> Result<List<Salon>> = { Result.success(emptyList()) },
) : SalonRepository {
    var lastRequestedSalonId: String? = null
        private set
    var getSalonCallCount = 0
        private set

    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> {
        getSalonCallCount++
        lastRequestedSalonId = salonId
        return getSalonResult(salonId)
    }

    override suspend fun myOwnedSalons(): Result<List<Salon>> = myOwnedSalonsResult()
}

private class FakeBookingRepository(private val result: Result<PagedResult<Booking>>) : BookingRepository {
    var lastRequestedSalonId: String? = null
        private set
    var lastRequestedPageSize: Int? = null
        private set
    var salonBookingsCallCount = 0
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
    override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by these tests")

    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> {
        salonBookingsCallCount++
        lastRequestedSalonId = salonId
        lastRequestedPageSize = size
        return result
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
