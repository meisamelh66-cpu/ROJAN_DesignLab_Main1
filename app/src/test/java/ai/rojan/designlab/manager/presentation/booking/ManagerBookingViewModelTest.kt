package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEAM2 Booking Creation Integrity follow-up. [ManagerBookingViewModel.confirm]
 * used to write a fake local [Appointment] and report success
 * unconditionally — exactly the regression that let a manager-created
 * appointment look successful while never appearing in the now-real
 * Manager Calendar (TEAM2-002). These tests prove it can no longer do
 * that: [confirm] never calls [AppointmentRepository.create], never
 * returns `true`, and always leaves a real, visible
 * [ai.rojan.designlab.manager.domain.booking.ManagerBookingState.submitError]
 * behind instead.
 */
class ManagerBookingViewModelTest {

    private val customer = ManagerCustomer(
        id = "c1",
        name = "مشتری تست",
        phone = "09120000000",
        tag = CustomerTag.NEW,
        loyaltyScore = 0,
        notes = null,
        lastVisit = "—",
        totalVisits = 0,
    )
    private val service = Service(
        id = "s1",
        name = "خدمت تست",
        category = "عمومی",
        price = 100_000L,
        durationMinutes = 30,
        active = true,
    )
    private val specialist = Specialist(
        id = "sp1",
        name = "متخصص تست",
        skills = listOf("خدمت تست"),
        workingHours = "۹-۱۸",
        commissionRate = 0.0,
        active = true,
    )

    private fun viewModel(appointmentRepository: FakeAppointmentRepository = FakeAppointmentRepository()) =
        ManagerBookingViewModel(
            customerRepository = FakeCustomerRepository(listOf(customer)),
            serviceRepository = FakeServiceRepository(listOf(service)),
            specialistRepository = FakeSpecialistRepository(listOf(specialist)),
            appointmentRepository = appointmentRepository,
        )

    private fun readyViewModel(appointmentRepository: FakeAppointmentRepository = FakeAppointmentRepository()): ManagerBookingViewModel {
        val vm = viewModel(appointmentRepository)
        vm.selectCustomer(customer.id)
        vm.selectService(service.id)
        vm.selectSpecialist(specialist.id)
        vm.selectDate("2026-09-20")
        vm.selectTime("10:00")
        return vm
    }

    @Test
    fun `confirm with a complete selection never creates a fake local appointment and reports false`() {
        val appointmentRepository = FakeAppointmentRepository()
        val viewModel = readyViewModel(appointmentRepository)

        val succeeded = viewModel.confirm()

        assertFalse("confirm() must never report success - there is no backend contract to create a real booking on behalf of a customer yet", succeeded)
        assertFalse("confirm() must never write a fake local appointment - that is the exact regression this fixes", appointmentRepository.createCalled)
    }

    @Test
    fun `confirm with a complete selection leaves a real, non-blank submitError`() {
        val viewModel = readyViewModel()

        viewModel.confirm()

        val message = viewModel.uiState.value.submitError
        assertTrue(message?.isNotBlank() == true)
    }

    @Test
    fun `confirm never sets createdAppointmentId`() {
        val viewModel = readyViewModel()

        viewModel.confirm()

        assertNull(viewModel.uiState.value.createdAppointmentId)
    }

    @Test
    fun `confirm with an incomplete selection returns false without touching the repository`() {
        val appointmentRepository = FakeAppointmentRepository()
        val viewModel = viewModel(appointmentRepository)
        viewModel.selectCustomer(customer.id)
        // service/specialist/date/time left unselected - not ready.

        val succeeded = viewModel.confirm()

        assertFalse(succeeded)
        assertFalse(appointmentRepository.createCalled)
    }

    @Test
    fun `reset clears a previous submitError`() {
        val viewModel = readyViewModel()
        viewModel.confirm()
        assertTrue(viewModel.uiState.value.submitError != null)

        viewModel.reset()

        assertEquals(null, viewModel.uiState.value.submitError)
    }
}

private class FakeCustomerRepository(private val customers: List<ManagerCustomer>) : CustomerRepository {
    override fun getAll(): List<ManagerCustomer> = customers
    override fun getById(id: String): ManagerCustomer? = customers.find { it.id == id }
    override fun search(query: String): List<ManagerCustomer> = customers
    override fun create(customer: ManagerCustomer): ManagerCustomer = error("not used by these tests")
    override fun update(customer: ManagerCustomer): ManagerCustomer? = error("not used by these tests")
    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> = emptyList()
}

private class FakeServiceRepository(private val services: List<Service>) : ServiceRepository {
    override fun getAll(): List<Service> = services
    override fun getById(id: String): Service? = services.find { it.id == id }
    override fun create(service: Service): Service = error("not used by these tests")
    override fun update(service: Service): Service? = error("not used by these tests")
    override fun delete(id: String): Boolean = error("not used by these tests")
}

private class FakeSpecialistRepository(private val specialists: List<Specialist>) : SpecialistRepository {
    override fun getAll(): List<Specialist> = specialists
    override fun getById(id: String): Specialist? = specialists.find { it.id == id }
    override fun create(specialist: Specialist): Specialist = error("not used by these tests")
    override fun update(specialist: Specialist): Specialist? = error("not used by these tests")
}

private class FakeAppointmentRepository : AppointmentRepository {
    var createCalled = false
        private set

    override fun getAll(): List<Appointment> = emptyList()
    override fun getById(id: String): Appointment? = null
    override fun getByCustomerId(customerId: String): List<Appointment> = emptyList()

    override fun create(appointment: Appointment): Appointment {
        createCalled = true
        return appointment
    }

    override fun update(appointment: Appointment): Appointment? = error("not used by these tests")
    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = error("not used by these tests")
    override fun cancel(id: String): Appointment? = error("not used by these tests")
}
