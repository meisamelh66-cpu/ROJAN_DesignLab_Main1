package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.AvailabilityRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TEAM2-003 (Complete Booking API Contract). [RescheduleAppointmentViewModel]
 * is the one operation among confirm/complete/reschedule/cancel this task
 * wires into a real screen with the full Loading/Success/Error/Unauthorized
 * state machine (confirm/complete have no existing salon-owner-facing UI
 * to connect to — see TEAM2_RESULT_BOOKING_API_CONTRACT.md). These tests
 * cover that state machine end to end against fakes, and prove
 * [RescheduleAppointmentViewModel.confirmReschedule]'s `onSuccess` fires
 * only on a genuinely confirmed backend reschedule — same "no fake
 * success" contract TEAM2-001 established.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleAppointmentViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
        bookingRepository: BookingRepository,
        availabilityRepository: AvailabilityRepository = RescheduleFakeAvailabilityRepository(Result.success(emptyList())),
    ) = RescheduleAppointmentViewModel(
        bookingId = "booking-1",
        bookingRepository = bookingRepository,
        availabilityRepository = availabilityRepository,
        salonRepository = RescheduleFakeSalonRepository(Result.success(Salon("salon-1", "Real Salon", null, "000", null, "addr"))),
        specialistRepository = RescheduleFakeSpecialistRepository(
            Result.success(Specialist("specialist-1", "salon-1", "Real Specialist", null, null)),
        ),
        serviceCategoryRepository = RescheduleFakeServiceCategoryRepository(
            Result.success(listOf(ServiceCategory("cat-1", "salon-1", "Category", null))),
        ),
        serviceRepository = RescheduleFakeServiceRepository(
            Result.success(listOf(Service("service-1", "salon-1", "cat-1", "Real Service", null, 30, 200_000.0))),
        ),
    )

    @Test
    fun `the booking loads successfully with its real display names resolved`() = runBlocking {
        val viewModel = viewModel(RescheduleFakeBookingRepository(getBookingResult = Result.success(booking)))

        val state = viewModel.targetState as UiState.Success
        assertEquals("Real Salon", state.data.salonName)
        assertEquals("Real Service", state.data.serviceName)
        assertEquals("Real Specialist", state.data.specialistName)
    }

    @Test
    fun `a 401 loading the booking surfaces as Error - unauthorized handling`() = runBlocking {
        val viewModel = viewModel(
            RescheduleFakeBookingRepository(getBookingResult = Result.failure(BackendApiException(401, null))),
        )

        assertTrue(viewModel.targetState is UiState.Error)
    }

    @Test
    fun `selecting a date loads real available slots into Success`() = runBlocking {
        val slots = listOf(TimeSlot("2026-09-21T10:00:00", "2026-09-21T10:30:00"))
        val viewModel = viewModel(
            RescheduleFakeBookingRepository(getBookingResult = Result.success(booking)),
            availabilityRepository = RescheduleFakeAvailabilityRepository(Result.success(slots)),
        )

        viewModel.selectDate("2026-09-21")

        val state = viewModel.slotsState as UiState.Success
        assertEquals(1, state.data.size)
    }

    @Test
    fun `confirming a reschedule calls onSuccess only after the real backend call succeeds`() = runBlocking {
        val repository = RescheduleFakeBookingRepository(
            getBookingResult = Result.success(booking),
            rescheduleResult = Result.success(booking.copy(startTime = "2026-09-21T14:00:00")),
        )
        val viewModel = viewModel(repository)
        viewModel.selectDate("2026-09-21")
        viewModel.selectTime("14:00")
        var succeeded = false

        viewModel.confirmReschedule(onSuccess = { succeeded = true })

        assertTrue(succeeded)
        assertEquals("2026-09-21T14:00:00", repository.lastRescheduleNewStartTime)
        assertNull(viewModel.submitError)
        assertFalse(viewModel.isSubmitting)
    }

    @Test
    fun `a network failure rescheduling sets submitError and never calls onSuccess`() = runBlocking {
        val repository = RescheduleFakeBookingRepository(
            getBookingResult = Result.success(booking),
            rescheduleResult = Result.failure(NetworkUnavailableException(Exception("offline"))),
        )
        val viewModel = viewModel(repository)
        viewModel.selectDate("2026-09-21")
        viewModel.selectTime("14:00")
        var succeeded = false

        viewModel.confirmReschedule(onSuccess = { succeeded = true })

        assertFalse("onSuccess must not fire on failure - that is what would fake a backend update that never happened", succeeded)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
    }

    @Test
    fun `confirming without a selected date and time fails without calling the repository or onSuccess`() = runBlocking {
        val repository = RescheduleFakeBookingRepository(getBookingResult = Result.success(booking))
        val viewModel = viewModel(repository)
        var succeeded = false

        viewModel.confirmReschedule(onSuccess = { succeeded = true })

        assertFalse(succeeded)
        assertFalse(repository.rescheduleCalled)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
    }
}

private class RescheduleFakeBookingRepository(
    private val getBookingResult: Result<Booking>,
    // Not derived from getBookingResult - a default parameter expression
    // is evaluated eagerly at every call site that omits it, so
    // getBookingResult.getOrThrow() would throw immediately for the
    // getBookingResult-is-a-failure tests, before the fake is even built.
    private val rescheduleResult: Result<Booking> = Result.failure(IllegalStateException("rescheduleResult not configured for this test")),
) : BookingRepository {

    var rescheduleCalled = false
        private set
    var lastRescheduleNewStartTime: String? = null
        private set

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
    ): Result<Booking> = error("not used by RescheduleAppointmentViewModel")

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by RescheduleAppointmentViewModel")

    override suspend fun getBooking(bookingId: String): Result<Booking> = getBookingResult

    override suspend fun cancelBooking(bookingId: String): Result<Booking> =
        error("not used by RescheduleAppointmentViewModel")

    override suspend fun confirmBooking(bookingId: String): Result<Booking> =
        error("not used by RescheduleAppointmentViewModel")

    override suspend fun completeBooking(bookingId: String): Result<Booking> =
        error("not used by RescheduleAppointmentViewModel")

    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> {
        rescheduleCalled = true
        lastRescheduleNewStartTime = newStartTime
        return rescheduleResult
    }

    override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by RescheduleAppointmentViewModel")
}

private class RescheduleFakeAvailabilityRepository(private val result: Result<List<TimeSlot>>) : AvailabilityRepository {
    override suspend fun getAvailableSlots(
        salonId: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int,
    ): Result<List<TimeSlot>> = result
}

private class RescheduleFakeSalonRepository(private val result: Result<Salon>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = result
    override suspend fun myOwnedSalons(): Result<List<Salon>> = error("not used by these tests")
}

private class RescheduleFakeSpecialistRepository(private val result: Result<Specialist>) : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used by these tests")
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = result
}

private class RescheduleFakeServiceCategoryRepository(private val result: Result<List<ServiceCategory>>) : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = result
}

private class RescheduleFakeServiceRepository(private val result: Result<List<Service>>) : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = result
}
