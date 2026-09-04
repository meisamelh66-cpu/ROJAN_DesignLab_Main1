package ai.rojan.designlab.reception.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Sprint 5B-6 (5B6-1) — the Reception booking wizard's selected
 * customer/service/specialist + date/time now persist through
 * [SavedStateHandle] (as flat primitive fields, since the domain models
 * are Android-free / not `Parcelable`), so a process death mid-wizard no
 * longer returns the receptionist to an empty flow.
 */
class ReceptionBookingViewModelSavedStateTest {

    private val salonId = "salon-1"

    private fun viewModel(handle: SavedStateHandle) = ReceptionBookingViewModel(
        savedStateHandle = handle,
        salonId = salonId,
        bookingRepository = StubBookingRepo,
        customerRepository = StubCustomerRepo,
        serviceRepository = StubServiceRepo,
        serviceCategoryRepository = StubServiceCategoryRepo,
        specialistRepository = StubSpecialistRepo,
        availabilityRepository = StubAvailabilityRepo,
    )

    private val customer = ReceptionCustomer(
        id = "cust-1", salonId = salonId, fullName = "زهرا احمدی",
        phoneNumber = "09120000000", email = "z@example.com", active = true,
    )
    private val service = Service(
        id = "svc-1", salonId = salonId, categoryId = "cat-1", name = "کوتاهی مو",
        description = "توضیح", durationMinutes = 45, price = 250_000.0,
    )
    private val specialist = Specialist(
        id = "spec-1", salonId = salonId, displayName = "الهام کریمی",
        bio = "بیو", photoUrl = "https://example.com/p.jpg",
    )

    @Test
    fun `full selection is restored when the ViewModel is recreated from the same SavedStateHandle`() {
        val handle = SavedStateHandle()

        val first = viewModel(handle)
        first.selectCustomer(customer)
        first.selectService(service)
        first.selectSpecialist(specialist)
        // date is set via loadAvailableTimes; call the state-mutating part directly through selectTime after
        first.selectTime("2026-09-01T10:00:00")

        val restored = viewModel(handle).uiState.value

        assertEquals(customer, restored.customer)
        assertEquals(service, restored.service)
        assertEquals(specialist, restored.specialist)
        assertEquals("2026-09-01T10:00:00", restored.time)
        // transient fields are not persisted
        assertEquals(false, restored.isSubmitting)
        assertNull(restored.confirmError)
        assertNull(restored.createdBookingId)
    }

    @Test
    fun `nullable object fields round-trip (no email or phone, null bio)`() {
        val handle = SavedStateHandle()
        val sparseCustomer = customer.copy(phoneNumber = null, email = null)
        val sparseSpecialist = specialist.copy(bio = null, photoUrl = null)
        val sparseService = service.copy(description = null)

        val first = viewModel(handle)
        first.selectCustomer(sparseCustomer)
        first.selectService(sparseService)
        first.selectSpecialist(sparseSpecialist)

        val restored = viewModel(handle).uiState.value
        assertEquals(sparseCustomer, restored.customer)
        assertEquals(sparseService, restored.service)
        assertEquals(sparseSpecialist, restored.specialist)
    }

    @Test
    fun `a fresh SavedStateHandle yields an empty wizard`() {
        val restored = viewModel(SavedStateHandle()).uiState.value
        assertNull(restored.customer)
        assertNull(restored.service)
        assertNull(restored.specialist)
        assertNull(restored.dateIso)
        assertNull(restored.time)
    }

    // ---- stubs (the selection methods touch no repository) ----------

    private object StubBookingRepo : ReceptionBookingRepository {
        override suspend fun listBookings(
            salonId: String,
            page: Int,
            size: Int,
            status: ai.rojan.designlab.domain.repository.BookingStatus?,
        ) = error("unused")
        override suspend fun createBookingForCustomer(
            salonId: String,
            customerId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
        ) = error("unused")
    }

    private object StubCustomerRepo : ReceptionCustomerRepository {
        override suspend fun listCustomers(salonId: String, search: String?, page: Int, size: Int) = error("unused")
        override suspend fun getCustomer(salonId: String, customerId: String) = error("unused")
    }

    private object StubServiceRepo : ServiceRepository {
        override suspend fun getServices(salonId: String, categoryId: String) = error("unused")
    }

    private object StubServiceCategoryRepo : ServiceCategoryRepository {
        override suspend fun getCategories(salonId: String) = error("unused")
    }

    private object StubSpecialistRepo : SpecialistRepository {
        override suspend fun getSpecialists(salonId: String) = error("unused")
        override suspend fun getSpecialist(salonId: String, specialistId: String) = error("unused")
    }

    private object StubAvailabilityRepo : AvailabilityRepository {
        override suspend fun getAvailableSlots(
            salonId: String,
            specialistId: String,
            serviceId: String,
            date: String,
            slotIntervalMinutes: Int,
        ) = error("unused")
    }
}
