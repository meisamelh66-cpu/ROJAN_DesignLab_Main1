package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.InvalidResponseException
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
 * TEAM2-001 (Booking Transaction Integrity). [BookingConfirmationViewModel.confirmBooking]'s
 * `onResult` callback is the single gate the whole customer-facing
 * "Booking Success" chain depends on downstream
 * (`BookingConfirmationScreen` -> `RojanNavGraph` ->
 * `CustomerEcosystemViewModel.bookAppointment`, which grants the
 * loyalty/wallet reward and schedules reminders). Every failure-path test
 * below asserts `onResult` is never invoked — which is exactly what
 * prevents that reward chain from ever running for a booking that was not
 * actually confirmed by the backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingConfirmationViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val validBooking = Booking(
        id = "booking-123",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-10T10:00:00",
        endTime = "2026-09-10T10:30:00",
        status = BookingStatus.PENDING,
        notes = null,
    )

    private fun viewModel(bookingRepository: BookingRepository) = BookingConfirmationViewModel(
        bookingRepository = bookingRepository,
        salonRepository = FakeSalonRepository,
        specialistRepository = FakeSpecialistRepository,
        serviceCategoryRepository = FakeServiceCategoryRepository,
        serviceRepository = FakeServiceRepository,
    )

    /** Drives a full-selection [BookingConfirmationViewModel.confirmBooking] call and returns whatever (if anything) `onResult` received. */
    private fun confirm(viewModel: BookingConfirmationViewModel): String? {
        var result: String? = null
        viewModel.confirmBooking(
            salonId = "salon-1",
            serviceId = "service-1",
            specialistId = "specialist-1",
            dateKey = "2026-09-10",
            time = "10:00",
            onResult = { result = it },
        )
        return result
    }

    @Test
    fun `backend success with a persisted booking id calls onResult and clears submitError`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(Result.success(validBooking)))

        val backendBookingId = confirm(viewModel)

        assertEquals("booking-123", backendBookingId)
        assertNull(viewModel.submitError)
        assertFalse(viewModel.isSubmitting)
    }

    @Test
    fun `an HTTP failure sets submitError and never calls onResult`() = runBlocking {
        val viewModel = viewModel(
            FakeBookingRepository(Result.failure(BackendApiException(statusCode = 409, apiError = null))),
        )

        val backendBookingId = confirm(viewModel)

        assertNull(
            "onResult must not fire on failure - this is what would otherwise grant a reward for a booking that doesn't exist",
            backendBookingId,
        )
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
        assertFalse(viewModel.isSubmitting)
    }

    @Test
    fun `a connectivity failure sets submitError and never calls onResult`() = runBlocking {
        val viewModel = viewModel(
            FakeBookingRepository(Result.failure(NetworkUnavailableException(Exception("offline")))),
        )

        val backendBookingId = confirm(viewModel)

        assertNull(backendBookingId)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
    }

    @Test
    fun `an undecodable (invalid) response sets submitError and never calls onResult`() = runBlocking {
        val viewModel = viewModel(
            FakeBookingRepository(Result.failure(InvalidResponseException(Exception("malformed body")))),
        )

        val backendBookingId = confirm(viewModel)

        assertNull(backendBookingId)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
    }

    @Test
    fun `a response with a blank booking id is treated as a failure, not a success`() = runBlocking {
        val viewModel = viewModel(FakeBookingRepository(Result.success(validBooking.copy(id = ""))))

        val backendBookingId = confirm(viewModel)

        assertNull(
            "a blank id must never reach onResult - there would be nothing for a caller to record or later cancel",
            backendBookingId,
        )
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
        assertFalse(viewModel.isSubmitting)
    }

    @Test
    fun `missing required selection state fails without ever calling the repository or onResult`() = runBlocking {
        val repository = FakeBookingRepository(Result.success(validBooking))
        val viewModel = viewModel(repository)
        var result: String? = null

        viewModel.confirmBooking(
            salonId = null,
            serviceId = "service-1",
            specialistId = "specialist-1",
            dateKey = "2026-09-10",
            time = "10:00",
            onResult = { result = it },
        )

        assertNull(result)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())
        assertFalse("the network call must never fire when required selection state is missing", repository.createBookingCalled)
    }

    @Test
    fun `a retry after failure that now succeeds calls onResult and clears the earlier submitError`() = runBlocking {
        val repository = FakeBookingRepository(Result.failure(NetworkUnavailableException(Exception("offline"))))
        val viewModel = viewModel(repository)
        confirm(viewModel)
        assertTrue(viewModel.submitError.orEmpty().isNotBlank())

        repository.nextResult = Result.success(validBooking)
        val backendBookingId = confirm(viewModel)

        assertEquals("booking-123", backendBookingId)
        assertNull(viewModel.submitError)
    }
}

/** Every non-createBooking method is unused by [BookingConfirmationViewModel] and deliberately left unimplemented. */
private class FakeBookingRepository(initialResult: Result<Booking>) : BookingRepository {

    var nextResult: Result<Booking> = initialResult
    var createBookingCalled = false
        private set

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
    ): Result<Booking> {
        createBookingCalled = true
        return nextResult
    }

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        error("not used by BookingConfirmationViewModel")

    override suspend fun getBooking(bookingId: String): Result<Booking> =
        error("not used by BookingConfirmationViewModel")

    override suspend fun cancelBooking(bookingId: String): Result<Booking> =
        error("not used by BookingConfirmationViewModel")

    override suspend fun confirmBooking(bookingId: String): Result<Booking> =
        error("not used by BookingConfirmationViewModel")

    override suspend fun completeBooking(bookingId: String): Result<Booking> =
        error("not used by BookingConfirmationViewModel")

    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        error("not used by BookingConfirmationViewModel")
}

private object FakeSalonRepository : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by these tests")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by these tests")
}

private object FakeSpecialistRepository : SpecialistRepository {
    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used by these tests")
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
        error("not used by these tests")
}

private object FakeServiceCategoryRepository : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> =
        error("not used by these tests")
}

private object FakeServiceRepository : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> =
        error("not used by these tests")
}
