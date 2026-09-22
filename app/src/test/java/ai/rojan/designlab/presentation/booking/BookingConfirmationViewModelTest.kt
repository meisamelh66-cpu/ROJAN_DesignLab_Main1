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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Sprint 5B-8B — [BookingConfirmationViewModel] fires the real
 * `POST /api/v1/bookings` when the customer confirms. Its own doc records
 * a real prior bug (it swallowed failures and let the screen navigate to
 * "Success" regardless). None of its state transitions — duplicate-submit
 * guard, incomplete-data guard, success/failure — were covered.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingConfirmationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- fakes ------------------------------------------------

    private class FakeBookingRepository(
        var onCreate: suspend () -> Result<Booking> = { Result.success(booking("b-1")) },
    ) : BookingRepository {
        val createCalls = mutableListOf<CreateArgs>()

        data class CreateArgs(
            val salonId: String,
            val serviceId: String,
            val specialistId: String,
            val startTime: String,
            val idempotencyKey: String?,
        )

        override suspend fun createBooking(
            salonId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
            idempotencyKey: String?,
            customerId: String?,
        ): Result<Booking> {
            createCalls += CreateArgs(salonId, serviceId, specialistId, startTime, idempotencyKey)
            return onCreate()
        }

        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> = error("unused")
        override suspend fun getBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun completeBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> = error("unused")
        override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> = error("unused")
    }

    private class FakeSalonRepository(var salon: Salon?) : SalonRepository {
        var getSalonCalls = 0
        override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String) = error("unused")
        override suspend fun getSalon(salonId: String): Result<Salon> {
            getSalonCalls++
            return salon?.let { Result.success(it) } ?: Result.failure(IOException("no salon"))
        }
        override suspend fun myOwnedSalons(): Result<List<Salon>> = error("unused")
    }

    private class FakeSpecialistRepository(private val specialist: Specialist?) : SpecialistRepository {
        override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("unused")
        override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
            specialist?.let { Result.success(it) } ?: Result.failure(IOException("no specialist"))
    }

    private class FakeServiceCategoryRepository(private val categories: List<ServiceCategory>) : ServiceCategoryRepository {
        override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = Result.success(categories)
    }

    private class FakeServiceRepository(private val servicesByCategory: Map<String, List<Service>>) : ServiceRepository {
        override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> =
            Result.success(servicesByCategory[categoryId].orEmpty())
    }

    private fun viewModel(
        booking: FakeBookingRepository = FakeBookingRepository(),
        salon: Salon? = salon("salon-1"),
        specialist: Specialist? = specialist("spec-1"),
        categories: List<ServiceCategory> = listOf(category("cat-1")),
        servicesByCategory: Map<String, List<Service>> = mapOf("cat-1" to listOf(service("svc-1"))),
    ) = BookingConfirmationViewModel(
        bookingRepository = booking,
        salonRepository = FakeSalonRepository(salon),
        specialistRepository = FakeSpecialistRepository(specialist),
        serviceCategoryRepository = FakeServiceCategoryRepository(categories),
        serviceRepository = FakeServiceRepository(servicesByCategory),
    )

    // ---- confirmBooking: happy path -----------------------

    @Test
    fun `a successful confirm reports the backend booking id and clears isSubmitting`() = runTest(dispatcher) {
        val repo = FakeBookingRepository { Result.success(booking("backend-42")) }
        val vm = viewModel(booking = repo)
        var confirmedId: String? = null

        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") { confirmedId = it }
        advanceUntilIdle()

        assertEquals("backend-42", confirmedId)
        assertFalse(vm.isSubmitting)
        assertNull(vm.submitError)
        assertEquals(1, repo.createCalls.size)
        assertEquals("2026/09/01T10:00:00", repo.createCalls.single().startTime)
    }

    // ---- confirmBooking: failure recovery -----------------

    @Test
    fun `a failed confirm surfaces submitError, does NOT call onSuccess, and lets the user retry`() = runTest(dispatcher) {
        val repo = FakeBookingRepository { Result.failure(IOException("network down")) }
        val vm = viewModel(booking = repo)
        var onSuccessCalled = false

        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") { onSuccessCalled = true }
        advanceUntilIdle()

        assertFalse("must not claim success on a failed booking", onSuccessCalled)
        assertFalse(vm.isSubmitting)
        assertTrue(vm.submitError != null)

        // retry is possible after a failure
        repo.onCreate = { Result.success(booking("b-2")) }
        var retriedId: String? = null
        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") { retriedId = it }
        advanceUntilIdle()
        assertEquals("b-2", retriedId)
        assertEquals(2, repo.createCalls.size)
    }

    // ---- confirmBooking: duplicate submission -------------

    @Test
    fun `a second confirm while one is in flight is ignored`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Booking>>()
        val repo = FakeBookingRepository { gate.await() }
        val vm = viewModel(booking = repo)

        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") {}
        advanceUntilIdle()
        assertTrue("first submit is in flight", vm.isSubmitting)

        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") {} // must be ignored

        gate.complete(Result.success(booking("b-1")))
        advanceUntilIdle()

        assertEquals("exactly one POST /bookings for a double-tap", 1, repo.createCalls.size)
        assertFalse(vm.isSubmitting)
    }

    // ---- confirmBooking: incomplete data ----------------

    @Test
    fun `confirm with a missing field sets submitError and never calls the backend`() = runTest(dispatcher) {
        val repo = FakeBookingRepository()
        val vm = viewModel(booking = repo)
        var onSuccessCalled = false

        vm.confirmBooking("salon-1", "svc-1", specialistId = null, "2026/09/01", "10:00") { onSuccessCalled = true }
        advanceUntilIdle()

        assertTrue(vm.submitError != null)
        assertFalse(vm.isSubmitting)
        assertFalse(onSuccessCalled)
        assertTrue("no network call for incomplete data", repo.createCalls.isEmpty())
    }

    // ---- loadSummary ----------------------------------

    @Test
    fun `loadSummary resolves the salon, specialist and service from the backend`() = runTest(dispatcher) {
        val vm = viewModel(
            salon = salon("salon-1"),
            specialist = specialist("spec-1"),
            servicesByCategory = mapOf("cat-1" to listOf(service("svc-1"), service("svc-2"))),
        )

        vm.loadSummary("salon-1", "spec-1", "svc-2")
        advanceUntilIdle()

        assertEquals("salon-1", vm.summary.salon?.id)
        assertEquals("spec-1", vm.summary.specialist?.id)
        assertEquals("svc-2", vm.summary.service?.id)
        assertFalse(vm.isLoadingSummary)
    }

    @Test
    fun `loadSummary with a null salon id yields an empty summary and makes no calls`() = runTest(dispatcher) {
        val salonRepo = FakeSalonRepository(salon("salon-1"))
        val vm = BookingConfirmationViewModel(
            bookingRepository = FakeBookingRepository(),
            salonRepository = salonRepo,
            specialistRepository = FakeSpecialistRepository(specialist("spec-1")),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category("cat-1"))),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service("svc-1")))),
        )

        vm.loadSummary(salonId = null, specialistId = "spec-1", serviceId = "svc-1")
        advanceUntilIdle()

        assertNull(vm.summary.salon)
        assertNull(vm.summary.specialist)
        assertNull(vm.summary.service)
        assertEquals(0, salonRepo.getSalonCalls)
    }

    @Test
    fun `loadSummary does not re-fetch for the same salon-specialist-service key`() = runTest(dispatcher) {
        val salonRepo = FakeSalonRepository(salon("salon-1"))
        val vm = BookingConfirmationViewModel(
            bookingRepository = FakeBookingRepository(),
            salonRepository = salonRepo,
            specialistRepository = FakeSpecialistRepository(specialist("spec-1")),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category("cat-1"))),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service("svc-1")))),
        )

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()
        vm.loadSummary("salon-1", "spec-1", "svc-1") // identical key
        advanceUntilIdle()

        assertEquals("summary is fetched once per unique id triple", 1, salonRepo.getSalonCalls)
    }

    // ---- 5B-8D: loadSummary edge cases -------------------------

    @Test
    fun `loadSummary re-fetches when the id triple changes - edit-and-return refreshes the summary`() = runTest(dispatcher) {
        val salonRepo = FakeSalonRepository(salon("salon-1"))
        val vm = BookingConfirmationViewModel(
            bookingRepository = FakeBookingRepository(),
            salonRepository = salonRepo,
            specialistRepository = FakeSpecialistRepository(specialist("spec-1")),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category("cat-1"))),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service("svc-1"), service("svc-2")))),
        )

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()
        vm.loadSummary("salon-1", "spec-1", "svc-2") // customer edited the service
        advanceUntilIdle()

        assertEquals("a changed selection must trigger a fresh resolve", 2, salonRepo.getSalonCalls)
        assertEquals("svc-2", vm.summary.service?.id)
    }

    @Test
    fun `loadSummary with no specialist still resolves the salon and service`() = runTest(dispatcher) {
        val vm = viewModel(
            salon = salon("salon-1"),
            servicesByCategory = mapOf("cat-1" to listOf(service("svc-1"))),
        )

        vm.loadSummary("salon-1", specialistId = null, serviceId = "svc-1")
        advanceUntilIdle()

        assertEquals("salon-1", vm.summary.salon?.id)
        assertEquals("svc-1", vm.summary.service?.id)
        assertNull("auto-selected specialist path leaves it null", vm.summary.specialist)
    }

    @Test
    fun `isLoadingSummary is true while the resolve is in flight and false once it settles`() = runTest(dispatcher) {
        val vm = viewModel(salon = salon("salon-1"))

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        assertTrue("in flight", vm.isLoadingSummary)

        advanceUntilIdle()
        assertFalse("settled", vm.isLoadingSummary)
    }

    // ---- loadSummary: real failure must not be silently swallowed ----

    @Test
    fun `loadSummary surfaces summaryError when the salon fetch genuinely fails`() = runTest(dispatcher) {
        val vm = viewModel(salon = null)

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()

        assertTrue("a genuine backend failure must be surfaced, not silently swallowed", vm.summaryError != null)
        assertFalse(vm.isLoadingSummary)
    }

    @Test
    fun `loadSummary sets no summaryError on a fully successful resolve`() = runTest(dispatcher) {
        val vm = viewModel(salon = salon("salon-1"), specialist = specialist("spec-1"))

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()

        assertNull(vm.summaryError)
    }

    @Test
    fun `retryLoadSummary re-fetches after a failure and clears summaryError once it succeeds`() = runTest(dispatcher) {
        val salonRepo = FakeSalonRepository(salon = null)
        val vm = BookingConfirmationViewModel(
            bookingRepository = FakeBookingRepository(),
            salonRepository = salonRepo,
            specialistRepository = FakeSpecialistRepository(specialist("spec-1")),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category("cat-1"))),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service("svc-1")))),
        )

        vm.loadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()
        assertTrue(vm.summaryError != null)

        salonRepo.salon = salon("salon-1")
        vm.retryLoadSummary("salon-1", "spec-1", "svc-1")
        advanceUntilIdle()

        assertNull("a successful retry must clear the earlier summaryError", vm.summaryError)
        assertEquals("salon-1", vm.summary.salon?.id)
        assertEquals("retry must actually re-fetch, not be treated as the same cached key", 2, salonRepo.getSalonCalls)
    }

    @Test
    fun `a successful confirm after a failed one clears the stale submitError`() = runTest(dispatcher) {
        val repo = FakeBookingRepository { Result.failure(IOException("network down")) }
        val vm = viewModel(booking = repo)

        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") {}
        advanceUntilIdle()
        assertTrue(vm.submitError != null)

        repo.onCreate = { Result.success(booking("b-ok")) }
        vm.confirmBooking("salon-1", "svc-1", "spec-1", "2026/09/01", "10:00") {}
        advanceUntilIdle()

        assertNull("retry success must clear the earlier error message", vm.submitError)
    }

    private companion object {
        fun booking(id: String) = Booking(
            id = id, salonId = "salon-1", serviceId = "svc-1", specialistId = "spec-1",
            customerId = "c-1", startTime = "2026-09-01T10:00:00", endTime = "2026-09-01T10:30:00",
            status = BookingStatus.PENDING, notes = null,
        )

        fun salon(id: String) = Salon(id = id, name = "Salon $id", description = null, phone = "0", email = null, address = "addr")
        fun specialist(id: String) = Specialist(id = id, salonId = "salon-1", displayName = "Sp $id", bio = null, photoUrl = null)
        fun service(id: String) = Service(id = id, salonId = "salon-1", categoryId = "cat-1", name = "Svc $id", description = null, durationMinutes = 30, price = 0.0)
        fun category(id: String) = ServiceCategory(id = id, salonId = "salon-1", name = "Cat $id", description = null)
    }
}
