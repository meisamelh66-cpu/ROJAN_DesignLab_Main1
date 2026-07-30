package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.booking.ManagerBookingState
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager Booking Journey Phase 2 — owns the in-progress
 * [ManagerBookingState] for the whole 7-screen wizard and every piece of
 * business logic the wizard needs (available-time computation,
 * appointment creation). Screens read [uiState] and call these methods;
 * none of them touch a repository or compute anything themselves, per
 * "no business logic inside Composables."
 *
 * Scoped to the wizard's own back-stack lifetime (shared across all 7
 * screens via `navController.getBackStackEntry(ManagerDestinations.CREATE_APPOINTMENT)`,
 * see [ai.rojan.designlab.manager.navigation.ManagerNavGraph]) — not an
 * app-lifetime singleton, so it's naturally cleared when the wizard
 * completes or is abandoned.
 */
class ManagerBookingViewModel(
    private val customerRepository: CustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerBookingState())
    val uiState: StateFlow<ManagerBookingState> = _uiState.asStateFlow()

    fun reset() {
        _uiState.value = ManagerBookingState()
    }

    fun selectCustomer(customerId: String) {
        _uiState.value = _uiState.value.copy(customerId = customerId)
    }

    fun selectService(serviceId: String) {
        _uiState.value = _uiState.value.copy(serviceId = serviceId)
    }

    fun selectSpecialist(specialistId: String) {
        _uiState.value = _uiState.value.copy(specialistId = specialistId)
    }

    fun selectDate(dateKey: String) {
        // Changing the date invalidates a previously chosen time — the
        // slot may not even exist/be free on the new date.
        _uiState.value = _uiState.value.copy(dateKey = dateKey, time = null)
    }

    fun selectTime(time: String) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun searchCustomers(query: String) = customerRepository.search(query)

    fun activeServices() = serviceRepository.getAll().filter { it.active }

    fun activeSpecialists() = specialistRepository.getAll().filter { it.active }

    /**
     * Specialists whose declared skills cover [serviceName], falling
     * back to the full active roster if none match — a genuine filter,
     * but one that never dead-ends the wizard with an empty list.
     */
    fun specialistsFor(serviceName: String?): List<Specialist> {
        val active = activeSpecialists()
        if (serviceName == null) return active
        val matching = active.filter { specialist -> specialist.skills.any { it == serviceName } }
        return matching.ifEmpty { active }
    }

    fun customerById(id: String?) = id?.let { customerRepository.getById(it) }
    fun serviceById(id: String?) = id?.let { serviceRepository.getById(it) }
    fun specialistById(id: String?) = id?.let { specialistRepository.getById(it) }

    /** Free slots for [specialistId] on [dateKey] — the fixed grid minus that specialist's already-booked, non-cancelled times that day. */
    fun availableTimes(specialistId: String, dateKey: String): List<String> {
        val taken = appointmentRepository.getAll()
            .filter {
                it.specialistId == specialistId &&
                    it.date == dateKey &&
                    it.status != AppointmentStatus.CANCELLED
            }
            .map { it.time }
            .toSet()
        return managerBookingTimeSlots.filterNot { it in taken }
    }

    fun confirm(): Appointment? {
        val state = _uiState.value
        val customerId = state.customerId ?: return null
        val serviceId = state.serviceId ?: return null
        val specialistId = state.specialistId ?: return null
        val dateKey = state.dateKey ?: return null
        val time = state.time ?: return null

        val appointment = Appointment(
            id = "apt-${System.currentTimeMillis()}",
            customerId = customerId,
            serviceId = serviceId,
            specialistId = specialistId,
            date = dateKey,
            time = time,
            status = AppointmentStatus.PENDING,
        )
        appointmentRepository.create(appointment)
        _uiState.value = state.copy(createdAppointmentId = appointment.id)
        return appointment
    }
}
