package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Manager Booking Creation Integrity follow-up. [ManagerBookingViewModel]
 * is now constructed from nothing but backend-facing repository
 * interfaces — no `manager.data.ManagerRepositories` singleton appears
 * anywhere in its dependency list. These tests prove the wizard's catalog/
 * customer-search/slot loading all source real backend data for the
 * manager's own salon, and that [ManagerBookingViewModel.confirm]'s
 * `onSuccess` fires if and only if the real `POST /api/v1/bookings` call
 * (with the selected real `customerId`) genuinely succeeds — the same
 * "no fake success" contract TEAM2-001 established, finally reachable
 * here now that the backend contract for it exists.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerBookingViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val salon = Salon("salon-1", "Real Salon", null, "000", null, "addr")
    private val service = Service("service-1", "salon-1", "cat-1", "Real Service", null, 30, 200_000.0)
    private val specialist = Specialist("specialist-1", "salon-1", "Real Specialist", null, null)
    private val customer = SalonCustomer("customer-1", "customer@example.com", "Real Customer")

    private fun viewModel(
        salonRepository: SalonRepository = FakeSalonRepository(Result.success(listOf(salon))),
        salonCustomerRepository: SalonCustomerRepository = FakeSalonCustomerRepository(Result.success(listOf(customer))),
        bookingRepository: BookingRepository = FakeBookingRepository(Result.success(sampleBooking())),
        availabilityRepository: AvailabilityRepository = FakeAvailabilityRepository(Result.success(emptyList())),
    ) = ManagerBookingViewModel(
        salonRepository = salonRepository,
        salonCustomerRepository = salonCustomerRepository,
        serviceCategoryRepository = FakeServiceCategoryRepository(Result.success(listOf(ServiceCategory("cat-1", "salon-1", "Category", null)))),
        serviceRepository = FakeServiceRepository(Result.success(listOf(service))),
        specialistRepository = FakeSpecialistRepository(Result.success(listOf(specialist))),
        availabilityRepository = availabilityRepository,
        bookingRepository = bookingRepository,
    )

    private fun sampleBooking() = Booking(
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

    private fun readySelection(viewModel: ManagerBookingViewModel) {
        viewModel.selectCustomer(customer.id)
        viewModel.selectService(service.id)
        viewModel.selectSpecialist(specialist.id)
        viewModel.selectDate("2026-09-20")
        viewModel.selectTime("10:00")
    }

    @Test
    fun `the catalog loads the manager's own salon's real services and specialists`() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.catalogState as UiState.Success
        assertEquals("salon-1", state.data.salonId)
        assertEquals(listOf(service), state.data.services)
        assertEquals(listOf(specialist), state.data.specialists)
    }

    @Test
    fun `owning zero salons is Empty, not an error, and not a fake catalog`() = runBlocking {
        val viewModel = viewModel(salonRepository = FakeSalonRepository(Result.success(emptyList())))

        assertEquals(UiState.Empty, viewModel.catalogState)
    }

    @Test
    fun `customer search returns the salon's real customers`() = runBlocking {
        val viewModel = viewModel()

        viewModel.searchCustomers("real")

        val state = viewModel.customerSearchState as UiState.Success
        assertEquals(listOf(customer), state.data)
    }

    @Test
    fun `selecting a date loads real available slots for the selected specialist and service`() = runBlocking {
        val slots = listOf(TimeSlot("2026-09-20T10:00:00", "2026-09-20T10:30:00"))
        val viewModel = viewModel(availabilityRepository = FakeAvailabilityRepository(Result.success(slots)))
        viewModel.selectService(service.id)
        viewModel.selectSpecialist(specialist.id)

        viewModel.selectDate("2026-09-20")

        val state = viewModel.slotsState as UiState.Success
        assertEquals(1, state.data.size)
    }

    @Test
    fun `confirm sends the real selected customerId and calls onSuccess only after the backend genuinely succeeds`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(sampleBooking()))
        val viewModel = viewModel(bookingRepository = repository)
        readySelection(viewModel)
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertTrue(succeeded)
        assertEquals("customer-1", repository.lastCustomerId)
        assertEquals("booking-1", viewModel.uiState.value.createdAppointmentId)
        assertEquals(null, viewModel.uiState.value.submitError)
    }

    @Test
    fun `a backend failure never calls onSuccess and leaves a real submitError, never a fake local booking`() = runBlocking {
        val repository = FakeBookingRepository(Result.failure(NetworkUnavailableException(Exception("offline"))))
        val viewModel = viewModel(bookingRepository = repository)
        readySelection(viewModel)
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertFalse("onSuccess must never fire on a real backend failure", succeeded)
        assertTrue(viewModel.uiState.value.submitError.orEmpty().isNotBlank())
        assertEquals(null, viewModel.uiState.value.createdAppointmentId)
    }

    @Test
    fun `confirm with an incomplete selection never calls the repository or onSuccess`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(sampleBooking()))
        val viewModel = viewModel(bookingRepository = repository)
        viewModel.selectCustomer(customer.id)
        // service/specialist/date/time left unselected.
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertFalse(succeeded)
        assertFalse(repository.createBookingCalled)
    }
}

private class FakeSalonRepository(private val result: Result<List<Salon>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by these tests")
    override suspend fun myOwnedSalons(): Result<List<Salon>> = result
}

private class FakeSalonCustomerRepository(private val result: Result<List<SalonCustomer>>) : SalonCustomerRepository {
    override suspend fun searchCustomers(salonId: String, query: String?): Result<List<SalonCustomer>> = result
}

private class FakeServiceCategoryRepository(private val result: Result<List<ServiceCategory>>) : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = result
}

private class FakeServiceRepository(private val result: Result<List<Service>>) : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = result
}

private class FakeSpecialistRepository(private val result: Result<List<Specialist>>) : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = result
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
        error("not used by these tests")
}

private class FakeAvailabilityRepository(private val result: Result<List<TimeSlot>>) : AvailabilityRepository {
    override suspend fun getAvailableSlots(
        salonId: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int,
    ): Result<List<TimeSlot>> = result
}

private class FakeBookingRepository(private val createBookingResult: Result<Booking>) : BookingRepository {

    var createBookingCalled = false
        private set
    var lastCustomerId: String? = null
        private set

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
        customerId: String?,
    ): Result<Booking> {
        createBookingCalled = true
        lastCustomerId = customerId
        return createBookingResult
    }

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by these tests")

    override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used by these tests")
    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by these tests")

    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by these tests")
}
