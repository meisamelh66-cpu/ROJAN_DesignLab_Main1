package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Sprint 5B-6 (5B6-1) — the Manager booking wizard's selections now
 * persist through [SavedStateHandle], so a process death mid-wizard no
 * longer returns the manager to an empty flow.
 */
class ManagerBookingViewModelSavedStateTest {

    private fun viewModel(handle: SavedStateHandle) = ManagerBookingViewModel(
        savedStateHandle = handle,
        customerRepository = StubCustomerRepo,
        serviceRepository = StubServiceRepo,
        specialistRepository = StubSpecialistRepo,
        appointmentRepository = StubAppointmentRepo,
    )

    @Test
    fun `selections are restored when the ViewModel is recreated from the same SavedStateHandle`() {
        val handle = SavedStateHandle()

        val first = viewModel(handle)
        first.selectCustomer("cust-1")
        first.selectService("svc-1")
        first.selectSpecialist("spec-1")
        first.selectDate("2026/09/01")
        first.selectTime("2026-09-01T10:00:00")

        // simulate process death: a brand-new instance from the restored handle
        val restored = viewModel(handle).uiState.value

        assertEquals("cust-1", restored.customerId)
        assertEquals("svc-1", restored.serviceId)
        assertEquals("spec-1", restored.specialistId)
        assertEquals("2026/09/01", restored.dateKey)
        assertEquals("2026-09-01T10:00:00", restored.time)
        assertEquals(true, restored.isReadyToConfirm)
        // transient fields are not persisted
        assertEquals(false, restored.isSubmitting)
        assertNull(restored.confirmError)
        assertNull(restored.createdAppointmentId)
    }

    @Test
    fun `picking a new date clears the previously chosen time in the persisted state`() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.selectDate("2026/09/01")
        vm.selectTime("2026-09-01T10:00:00")
        vm.selectDate("2026/09/02")

        assertNull(viewModel(handle).uiState.value.time)
        assertEquals("2026/09/02", viewModel(handle).uiState.value.dateKey)
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

    // ---- stubs (the selection methods touch no repository) ----------

    private object StubServiceRepo : ServiceRepository {
        override fun getAll() = emptyList<Service>()
        override fun getById(id: String): Service? = null
        override suspend fun create(service: Service) = error("unused")
        override suspend fun update(service: Service) = error("unused")
        override suspend fun delete(id: String) = error("unused")
        override fun getCategoryNames() = emptyList<String>()
    }

    private object StubSpecialistRepo : SpecialistRepository {
        override fun getAll() = emptyList<Specialist>()
        override fun getById(id: String): Specialist? = null
        override suspend fun create(specialist: Specialist) = error("unused")
        override suspend fun update(specialist: Specialist) = error("unused")
        override suspend fun delete(id: String) = error("unused")
    }

    private object StubCustomerRepo : CustomerRepository {
        override fun getAll() = emptyList<ManagerCustomer>()
        override fun getById(id: String): ManagerCustomer? = null
        override fun search(query: String) = emptyList<ManagerCustomer>()
        override suspend fun create(customer: ManagerCustomer) = error("unused")
        override suspend fun update(customer: ManagerCustomer) = error("unused")
        override fun getServiceHistory(customerId: String) = emptyList<CustomerServiceHistoryEntry>()
        override fun getNoteHistory(customerId: String) = emptyList<CustomerNote>()
        override suspend fun loadDetail(customerId: String) = error("unused")
    }

    private object StubAppointmentRepo : AppointmentRepository {
        override fun getAll() = emptyList<Appointment>()
        override fun getById(id: String): Appointment? = null
        override fun getByCustomerId(customerId: String) = emptyList<Appointment>()
        override suspend fun create(appointment: Appointment) = error("unused")
        override fun update(appointment: Appointment): Appointment? = null
        override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = null
        override fun cancel(id: String): Appointment? = null
        override suspend fun createForCustomer(
            customerId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
        ) = error("unused")
        override suspend fun confirm(id: String) = error("unused")
        override suspend fun complete(id: String) = error("unused")
    }
}
