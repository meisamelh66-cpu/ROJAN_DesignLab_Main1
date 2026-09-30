package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AvailabilityRepository
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
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Manager Booking Creation Integrity follow-up. These tests prove the
 * wizard's catalog/customer-search/slot loading all source real backend
 * data for the manager's own salon, and that [ManagerBookingViewModel.confirm]'s
 * `onSuccess` fires if and only if the real backend call genuinely
 * succeeds — the same "no fake success" contract TEAM2-001 established.
 *
 * **Master Integration Repair, Pass 4:** [confirm] now calls
 * [ai.rojan.designlab.manager.domain.repository.AppointmentRepository.createForCustomer]
 * (`POST /api/v1/salons/{salonId}/bookings`, the real owner/manager-authorized
 * counterpart) — not the customer self-service endpoint the wizard used
 * before, which always attributed the booking to the calling Manager, never
 * the customer actually selected. [ManagerBookingViewModel] does depend on
 * one `manager.data.ManagerRepositories`-managed repository now
 * ([AppointmentRepository], via a provider lambda) alongside its other,
 * `BackendApiContainer`-sourced dependencies - see
 * [ManagerBookingViewModel]'s own doc comment for the full rationale.
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
        salonRepository: SalonRepository = FakeSalonRepository(Result.success(salon)),
        salonCustomerRepository: SalonCustomerRepository = FakeSalonCustomerRepository(Result.success(listOf(customer))),
        appointmentRepository: AppointmentRepository = FakeAppointmentRepository(Result.success(sampleAppointment())),
        availabilityRepository: AvailabilityRepository = FakeAvailabilityRepository(Result.success(emptyList())),
        activeSalonId: String? = salon.id,
    ) = ManagerBookingViewModel(
        salonRepository = salonRepository,
        salonCustomerRepository = salonCustomerRepository,
        serviceCategoryRepository = FakeServiceCategoryRepository(Result.success(listOf(ServiceCategory("cat-1", "salon-1", "Category", null)))),
        serviceRepository = FakeServiceRepository(Result.success(listOf(service))),
        specialistRepository = FakeSpecialistRepository(Result.success(listOf(specialist))),
        availabilityRepository = availabilityRepository,
        appointmentRepositoryProvider = { appointmentRepository },
        activeSalonContextRepository = FakeActiveSalonContextRepository(activeSalonId),
    )

    private fun sampleAppointment() = Appointment(
        id = "booking-1",
        customerId = "customer-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        date = "2026-09-20",
        time = "10:00",
        status = AppointmentStatus.PENDING,
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
    fun `no active salon selected is Empty, not an error, and not a fake catalog`() = runBlocking {
        val viewModel = viewModel(activeSalonId = null)

        assertEquals(UiState.Empty, viewModel.catalogState)
    }

    /**
     * Master Integration Repair, Pass 3 (Staff/Receptionist access): the real bug this fixes - a
     * non-owner Manager/Receptionist has no owned salons at all, but does have a real, staff-
     * inclusive active salon id. Before this fix, this exact scenario silently produced an empty
     * catalog (via the owner-only `myOwnedSalons()`), blocking the whole booking wizard; now it
     * resolves correctly via [ActiveSalonContextRepository] + [SalonRepository.getSalon].
     */
    @Test
    fun `the catalog loads a real active salon even when it is not owned by the caller`() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.catalogState as UiState.Success
        assertEquals("salon-1", state.data.salonId)
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
        val repository = FakeAppointmentRepository(Result.success(sampleAppointment()))
        val viewModel = viewModel(appointmentRepository = repository)
        readySelection(viewModel)
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertTrue(succeeded)
        assertEquals("customer-1", repository.lastCustomerId)
        assertEquals("booking-1", viewModel.uiState.value.createdAppointmentId)
        assertEquals(null, viewModel.uiState.value.submitError)
    }

    /**
     * Master Integration Repair, Pass 4 (Customer Management audit): [confirm] previously called
     * the customer self-service `BookingRepository.createBooking`, which always attributes the
     * booking to the CALLER (the Manager), never the customer actually selected in the wizard -
     * regardless of which customer was picked. This test proves the real fix: the exact selected
     * `customerId` is what reaches the repository, via the real owner-authorized
     * [AppointmentRepository.createForCustomer] (`POST /api/v1/salons/{salonId}/bookings`).
     */
    @Test
    fun `confirm sends the exact selected customer, not the caller's own identity`() = runBlocking {
        val repository = FakeAppointmentRepository(Result.success(sampleAppointment()))
        val viewModel = viewModel(appointmentRepository = repository)
        viewModel.selectCustomer("a-completely-different-customer-id")
        viewModel.selectService(service.id)
        viewModel.selectSpecialist(specialist.id)
        viewModel.selectDate("2026-09-20")
        viewModel.selectTime("10:00")

        viewModel.confirm(onSuccess = {})

        assertEquals("a-completely-different-customer-id", repository.lastCustomerId)
    }

    @Test
    fun `a backend failure never calls onSuccess and leaves a real submitError, never a fake local booking`() = runBlocking {
        val repository = FakeAppointmentRepository(Result.failure(NetworkUnavailableException(Exception("offline"))))
        val viewModel = viewModel(appointmentRepository = repository)
        readySelection(viewModel)
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertFalse("onSuccess must never fire on a real backend failure", succeeded)
        assertTrue(viewModel.uiState.value.submitError.orEmpty().isNotBlank())
        assertEquals(null, viewModel.uiState.value.createdAppointmentId)
    }

    @Test
    fun `confirm with an incomplete selection never calls the repository or onSuccess`() = runBlocking {
        val repository = FakeAppointmentRepository(Result.success(sampleAppointment()))
        val viewModel = viewModel(appointmentRepository = repository)
        viewModel.selectCustomer(customer.id)
        // service/specialist/date/time left unselected.
        var succeeded = false

        viewModel.confirm(onSuccess = { succeeded = true })

        assertFalse(succeeded)
        assertFalse(repository.createForCustomerCalled)
    }
}

private class FakeSalonRepository(private val result: Result<Salon>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = result
    override suspend fun myOwnedSalons(): Result<List<Salon>> = error("not used by these tests - ManagerBookingViewModel must never call the owner-only endpoint (Master Integration Repair, Pass 3)")
}

private class FakeActiveSalonContextRepository(initialSalonId: String?) : ActiveSalonContextRepository {
    private val salonId = MutableStateFlow(initialSalonId)
    override suspend fun saveActiveSalonId(salonId: String) { this.salonId.value = salonId }
    override suspend fun clearActiveSalonId() { salonId.value = null }
    override fun observeActiveSalonId(): Flow<String?> = salonId
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

private class FakeAppointmentRepository(private val createForCustomerResult: Result<Appointment>) : AppointmentRepository {

    var createForCustomerCalled = false
        private set
    var lastCustomerId: String? = null
        private set

    override fun getAll(): List<Appointment> = error("not used by these tests")
    override fun getById(id: String): Appointment? = error("not used by these tests")
    override fun getByCustomerId(customerId: String): List<Appointment> = error("not used by these tests")
    override suspend fun create(appointment: Appointment): Result<Appointment> = error("not used by these tests - superseded by createForCustomer (Master Integration Repair, Pass 4)")
    override fun update(appointment: Appointment): Appointment? = error("not used by these tests")
    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = error("not used by these tests")
    override fun cancel(id: String): Appointment? = error("not used by these tests")

    override suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment> {
        createForCustomerCalled = true
        lastCustomerId = customerId
        return createForCustomerResult
    }

    override suspend fun confirm(id: String): Result<Appointment> = error("not used by these tests")
    override suspend fun complete(id: String): Result<Appointment> = error("not used by these tests")
}
