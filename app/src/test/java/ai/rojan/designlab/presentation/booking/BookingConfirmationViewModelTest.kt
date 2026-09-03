package ai.rojan.designlab.presentation.booking

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Reliability Sprint 01, P0-3: hermetic coverage of
 * [BookingConfirmationViewModel.confirmBooking] — success, failure, and the
 * existing `if (isSubmitting) return` double-submit guard, using a fake
 * [BookingRepository] whose `createBooking` suspends on a [CompletableDeferred]
 * so an in-flight request can be genuinely observed before it resolves
 * (same idiom [ManagerAppointmentDetailViewModelTest]/[ManagerDashboardViewModelTest]
 * already establish). The other 4 constructor dependencies aren't touched by
 * [BookingConfirmationViewModel.confirmBooking] (only `loadSummary` uses
 * them), so their fakes are unconditional `error("not used")` stubs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingConfirmationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBooking(id: String = "b1") = Booking(
        id = id,
        salonId = "s1",
        serviceId = "sv1",
        specialistId = "sp1",
        customerId = "c1",
        startTime = "2026-09-01T10:00:00",
        endTime = "2026-09-01T10:30:00",
        status = BookingStatus.PENDING,
        notes = null,
    )

    private fun viewModel(repository: FakeBookingRepository) = BookingConfirmationViewModel(
        bookingRepository = repository,
        salonRepository = FakeSalonRepository(),
        specialistRepository = FakeSpecialistRepository(),
        serviceCategoryRepository = FakeServiceCategoryRepository(),
        serviceRepository = FakeServiceRepository(),
    )

    @Test
    fun `confirmBooking success invokes createBooking once and returns the real backend booking id`() = runTest {
        val repository = FakeBookingRepository()
        val viewModel = viewModel(repository)
        var receivedId: String? = null

        repository.createCompletion.complete(Result.success(sampleBooking(id = "b1")))
        viewModel.confirmBooking(
            salonId = "s1",
            serviceId = "sv1",
            specialistId = "sp1",
            dateKey = "2026-09-01",
            time = "10:00",
            onSuccess = { receivedId = it },
        )

        assertEquals("b1", receivedId)
        assertEquals(false, viewModel.isSubmitting)
        assertEquals(null, viewModel.submitError)
        assertEquals(1, repository.createCallCount)
    }

    @Test
    fun `confirmBooking failure sets submitError and never invokes the success callback`() = runTest {
        val repository = FakeBookingRepository()
        val viewModel = viewModel(repository)
        var successCalled = false

        repository.createCompletion.complete(Result.failure(IllegalStateException("network down")))
        viewModel.confirmBooking(
            salonId = "s1",
            serviceId = "sv1",
            specialistId = "sp1",
            dateKey = "2026-09-01",
            time = "10:00",
            onSuccess = { successCalled = true },
        )

        assertTrue(!successCalled)
        assertEquals(false, viewModel.isSubmitting)
        assertTrue(viewModel.submitError?.isNotBlank() == true)
    }

    @Test
    fun `calling confirmBooking again while a request is in flight does not invoke createBooking twice`() = runTest {
        val repository = FakeBookingRepository()
        val viewModel = viewModel(repository)

        viewModel.confirmBooking(
            salonId = "s1", serviceId = "sv1", specialistId = "sp1", dateKey = "2026-09-01", time = "10:00",
            onSuccess = {},
        )

        assertEquals(true, viewModel.isSubmitting)
        assertEquals(1, repository.createCallCount)

        viewModel.confirmBooking(
            salonId = "s1", serviceId = "sv1", specialistId = "sp1", dateKey = "2026-09-01", time = "10:00",
            onSuccess = {},
        )

        assertEquals(1, repository.createCallCount)

        repository.createCompletion.complete(Result.success(sampleBooking()))

        assertEquals(false, viewModel.isSubmitting)
    }

    private class FakeBookingRepository : BookingRepository {
        val createCompletion = CompletableDeferred<Result<Booking>>()

        var createCallCount = 0
            private set

        override suspend fun createBooking(
            salonId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
            idempotencyKey: String?,
        ): Result<Booking> {
            createCallCount++
            return createCompletion.await()
        }

        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
            error("not used")

        override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> = error("not used")
    }

    private class FakeSalonRepository : SalonRepository {
        override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
            error("not used")
        override suspend fun getSalon(salonId: String): Result<Salon> = error("not used")
        override suspend fun getGallery(salonId: String): Result<List<String>> = error("not used")
    }

    private class FakeSpecialistRepository : SpecialistRepository {
        override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used")
        override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = error("not used")
        override suspend fun getPortfolio(salonId: String, specialistId: String): Result<List<String>> = error("not used")
        override suspend fun getAssignedServiceIds(salonId: String, specialistId: String): Result<List<String>> = error("not used")
    }

    private class FakeServiceCategoryRepository : ServiceCategoryRepository {
        override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = error("not used")
    }

    private class FakeServiceRepository : ServiceRepository {
        override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> = error("not used")
        override suspend fun getImages(salonId: String, serviceId: String): Result<List<String>> = error("not used")
    }
}
