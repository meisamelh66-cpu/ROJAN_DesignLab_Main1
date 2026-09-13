package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
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
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Sprint 5B-6 (5B6-1), preserved through the Manager Booking Creation
 * Integrity follow-up's real-backend rewrite of [ManagerBookingViewModel]:
 * the wizard's five selections still persist through [SavedStateHandle],
 * so a process death mid-wizard no longer returns the manager to an empty
 * flow — now proven against the real backend-repository constructor
 * shape rather than the retired `manager.data.ManagerRepositories`
 * in-memory one.
 *
 * The merged [ManagerBookingViewModel] also eagerly loads its catalog in
 * `init {}` (System2's real-backend contribution) — unlike either original
 * side alone, so a real `Dispatchers.Main` must be installed before every
 * construction below, same as [ManagerBookingViewModelTest]'s setup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerBookingViewModelSavedStateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle) = ManagerBookingViewModel(
        salonRepository = FakeSalonRepository,
        salonCustomerRepository = FakeSalonCustomerRepository,
        serviceCategoryRepository = FakeServiceCategoryRepository,
        serviceRepository = FakeServiceRepository,
        specialistRepository = FakeSpecialistRepository,
        availabilityRepository = FakeAvailabilityRepository,
        bookingRepository = FakeBookingRepository,
        savedStateHandle = handle,
    )

    @Test
    fun `selections are restored when the ViewModel is recreated from the same SavedStateHandle`() {
        val handle = SavedStateHandle()

        val first = viewModel(handle)
        first.selectCustomer("cust-1")
        first.selectService("svc-1")
        first.selectSpecialist("spec-1")
        first.selectDate("2026-09-01")
        first.selectTime("10:00")

        // simulate process death: a brand-new instance from the restored handle
        val restored = viewModel(handle).uiState.value

        assertEquals("cust-1", restored.customerId)
        assertEquals("svc-1", restored.serviceId)
        assertEquals("spec-1", restored.specialistId)
        assertEquals("2026-09-01", restored.dateKey)
        assertEquals("10:00", restored.time)
        assertEquals(true, restored.isReadyToConfirm)
        // transient fields are not persisted
        assertEquals(false, restored.isSubmitting)
        assertNull(restored.submitError)
        assertNull(restored.createdAppointmentId)
    }

    @Test
    fun `picking a new date clears the previously chosen time in the persisted state`() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.selectDate("2026-09-01")
        vm.selectTime("10:00")
        vm.selectDate("2026-09-02")

        assertNull(viewModel(handle).uiState.value.time)
        assertEquals("2026-09-02", viewModel(handle).uiState.value.dateKey)
    }

    @Test
    fun `reset clears the persisted selections`() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.selectCustomer("cust-1")
        vm.selectService("svc-1")
        vm.reset()

        val restored = viewModel(handle).uiState.value
        assertNull(restored.customerId)
        assertNull(restored.serviceId)
    }

    @Test
    fun `a fresh SavedStateHandle yields an empty wizard`() {
        val restored = viewModel(SavedStateHandle()).uiState.value
        assertNull(restored.customerId)
        assertNull(restored.serviceId)
        assertNull(restored.specialistId)
        assertNull(restored.dateKey)
        assertNull(restored.time)
    }

    // ---- fakes (the selection methods under test touch no repository) ----

    private object FakeSalonRepository : SalonRepository {
        override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
            error("not used by these tests")
        override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by these tests")
        override suspend fun myOwnedSalons(): Result<List<Salon>> = Result.success(emptyList())
    }

    private object FakeSalonCustomerRepository : SalonCustomerRepository {
        override suspend fun searchCustomers(salonId: String, query: String?): Result<List<SalonCustomer>> =
            Result.success(emptyList())
    }

    private object FakeServiceCategoryRepository : ServiceCategoryRepository {
        override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = Result.success(emptyList())
    }

    private object FakeServiceRepository : ServiceRepository {
        override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = Result.success(emptyList())
    }

    private object FakeSpecialistRepository : SpecialistRepository {
        override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = Result.success(emptyList())
        override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
            error("not used by these tests")
    }

    private object FakeAvailabilityRepository : AvailabilityRepository {
        override suspend fun getAvailableSlots(
            salonId: String,
            specialistId: String,
            serviceId: String,
            date: String,
            slotIntervalMinutes: Int,
        ): Result<List<TimeSlot>> = Result.success(emptyList())
    }

    private object FakeBookingRepository : BookingRepository {
        override suspend fun createBooking(
            salonId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
            idempotencyKey: String?,
            customerId: String?,
        ): Result<ai.rojan.designlab.domain.repository.Booking> = error("not used by these tests")
        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?) = error("not used by these tests")
        override suspend fun getBooking(bookingId: String) = error("not used by these tests")
        override suspend fun cancelBooking(bookingId: String) = error("not used by these tests")
        override suspend fun confirmBooking(bookingId: String) = error("not used by these tests")
        override suspend fun completeBooking(bookingId: String) = error("not used by these tests")
        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String) = error("not used by these tests")
        override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?) =
            error("not used by these tests")
    }
}
