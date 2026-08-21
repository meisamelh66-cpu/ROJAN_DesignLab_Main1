package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.dto.ApiErrorDto
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
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
 * RBAC compatibility fix — Manager Android Pilot: hermetic coverage of
 * [ManagerAppointmentDetailViewModel]'s Confirm/Complete actions, using an
 * in-memory fake [AppointmentRepository] — same fake-repository approach
 * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelTest]
 * already establishes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerAppointmentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pendingAppointment(id: String = "a1") = Appointment(
        id = id,
        customerId = "c1",
        serviceId = "s1",
        specialistId = "sp1",
        date = "2026/09/01",
        time = "10:00",
        status = AppointmentStatus.PENDING,
    )

    @Test
    fun `confirmBooking replaces the appointment with the real backend response on success`() = runTest {
        val appointment = pendingAppointment()
        val repository = FakeAppointmentRepository(initial = appointment)
        val viewModel = ManagerAppointmentDetailViewModel(repository, appointment.id)

        viewModel.confirmBooking()

        assertEquals(AppointmentStatus.CONFIRMED, viewModel.appointment.value?.status)
        assertEquals(false, viewModel.isSubmitting.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(1, repository.confirmCallCount)
    }

    @Test
    fun `completeBooking replaces the appointment with the real backend response on success`() = runTest {
        val appointment = pendingAppointment().copy(status = AppointmentStatus.CONFIRMED)
        val repository = FakeAppointmentRepository(initial = appointment)
        val viewModel = ManagerAppointmentDetailViewModel(repository, appointment.id)

        viewModel.completeBooking()

        assertEquals(AppointmentStatus.COMPLETED, viewModel.appointment.value?.status)
        assertEquals(1, repository.completeCallCount)
    }

    @Test
    fun `a real backend rejection surfaces a Persian error message and leaves the appointment unchanged`() = runTest {
        val appointment = pendingAppointment()
        val repository = FakeAppointmentRepository(
            initial = appointment,
            confirmResult = {
                Result.failure(
                    BackendApiException(
                        statusCode = 409,
                        apiError = ApiErrorDto(
                            timestamp = "2026-08-20T00:00:00Z",
                            status = 409,
                            error = "Conflict",
                            errorCode = "INVALID_BOOKING_STATE",
                            message = "Only a pending booking can be confirmed",
                            path = "/api/v1/bookings/${appointment.id}/confirm",
                        ),
                    ),
                )
            },
        )
        val viewModel = ManagerAppointmentDetailViewModel(repository, appointment.id)

        viewModel.confirmBooking()

        assertEquals(AppointmentStatus.PENDING, viewModel.appointment.value?.status)
        assertTrue(viewModel.errorMessage.value != null)
    }

    private class FakeAppointmentRepository(
        initial: Appointment,
        private val confirmResult: (() -> Result<Appointment>)? = null,
        private val completeResult: (() -> Result<Appointment>)? = null,
    ) : AppointmentRepository {
        private var stored = initial

        var confirmCallCount = 0
            private set
        var completeCallCount = 0
            private set

        override fun getAll(): List<Appointment> = listOf(stored)
        override fun getById(id: String): Appointment? = stored.takeIf { it.id == id }
        override fun getByCustomerId(customerId: String): List<Appointment> = emptyList()
        override suspend fun create(appointment: Appointment): Result<Appointment> = error("not used")
        override fun update(appointment: Appointment): Appointment? = null
        override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = null
        override fun cancel(id: String): Appointment? = null
        override suspend fun createForCustomer(
            customerId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
        ): Result<Appointment> = error("not used")

        override suspend fun confirm(id: String): Result<Appointment> {
            confirmCallCount++
            val result = confirmResult?.invoke() ?: Result.success(stored.copy(status = AppointmentStatus.CONFIRMED))
            result.getOrNull()?.let { stored = it }
            return result
        }

        override suspend fun complete(id: String): Result<Appointment> {
            completeCallCount++
            val result = completeResult?.invoke() ?: Result.success(stored.copy(status = AppointmentStatus.COMPLETED))
            result.getOrNull()?.let { stored = it }
            return result
        }
    }
}
