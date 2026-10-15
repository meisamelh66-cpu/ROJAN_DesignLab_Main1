package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.NetworkCustomerStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * First created on `feature/android-first-salon-pilot` - no prior test file existed for
 * [BackendCustomerRepository] on this branch. Covers two independent fixes together since both
 * touch this one file:
 *
 * Issue-012 (`getByUserId`): a `Booking`/`Appointment.customerId` is a `UserId`, never a CRM
 * `Customer.id` - `getByUserId` must resolve on the linked-account field, and must never
 * accidentally match on the CRM id even when a test fixture makes the two strings equal by
 * coincidence.
 *
 * Issue-014 (status round-trip): `update()` must only resend `status` when the tag picker
 * genuinely changed it - an unrelated (e.g. name-only) edit must never silently flip the real
 * backend status under the lossy 6-to-4 tag mapping.
 */
class BackendCustomerRepositoryTest {

    private val serviceRepository = object : ServiceRepository {
        override fun getAll(): List<Service> = error("not used by these tests")
        override fun getById(id: String): Service? = error("not used by these tests")
        override suspend fun create(service: Service): Result<Service> = error("not used by these tests")
        override suspend fun update(service: Service): Result<Service?> = error("not used by these tests")
        override suspend fun delete(id: String): Result<Boolean> = error("not used by these tests")
        override fun getCategoryNames(): List<String> = error("not used by these tests")
    }

    private val specialistRepository = object : SpecialistRepository {
        override fun getAll(): List<Specialist> = error("not used by these tests")
        override fun getById(id: String): Specialist? = error("not used by these tests")
        override suspend fun create(specialist: Specialist): Result<Specialist> = error("not used by these tests")
        override suspend fun update(specialist: Specialist): Result<Specialist?> = error("not used by these tests")
        override suspend fun delete(id: String): Result<Boolean> = error("not used by these tests")
        override suspend fun getAssignedServiceIds(specialistId: String): Result<List<String>> = error("not used by these tests")
        override suspend fun assignService(specialistId: String, serviceId: String): Result<Unit> = error("not used by these tests")
        override suspend fun unassignService(specialistId: String, serviceId: String): Result<Unit> = error("not used by these tests")
    }

    private fun customerDto(
        id: String,
        userId: String?,
        status: NetworkCustomerStatus = NetworkCustomerStatus.ACTIVE,
        fullName: String = "Sara",
        phoneNumber: String? = "+989110000000",
    ) = CustomerResponseDto(
        id = id,
        salonId = "salon-1",
        userId = userId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = null,
        company = null,
        status = status,
        lifetimeValue = 0.0,
        tags = emptyList(),
        active = true,
        createdAt = "2026-01-01T10:00:00",
        updatedAt = "2026-01-01T10:00:00",
    )

    @Test
    fun `getByUserId resolves a customer by its linked userId, not by CRM id`() = runBlocking {
        val api = FakeManagerCustomerApi(
            listResult = {
                listOf(
                    customerDto(id = "customer-1", userId = "user-1"),
                    customerDto(id = "customer-2", userId = null, fullName = "Walk-in Reza"),
                )
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")
        repository.sync()

        assertEquals("customer-1", repository.getByUserId("user-1")?.id)
        assertEquals("Sara", repository.getByUserId("user-1")?.name)
    }

    @Test
    fun `getByUserId returns null for a CRM id passed by mistake, and for a walk-in customer with no linked account`() = runBlocking {
        val api = FakeManagerCustomerApi(
            listResult = {
                listOf(
                    customerDto(id = "customer-1", userId = "user-1"),
                    customerDto(id = "customer-2", userId = null, fullName = "Walk-in Reza"),
                )
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")
        repository.sync()

        // "customer-1" is a real CRM id in this cache, but it is not any customer's userId.
        assertNull(repository.getByUserId("customer-1"))
        assertNull(repository.getByUserId("customer-2"))
    }

    @Test
    fun `update never resends status for an unrelated (name-only) edit, for every backend status`() = runBlocking {
        for (backendStatus in NetworkCustomerStatus.entries) {
            var capturedStatus: NetworkCustomerStatus? = NetworkCustomerStatus.VIP // sentinel: must become null
            val api = FakeManagerCustomerApi(
                listResult = { listOf(customerDto(id = "customer-1", userId = "user-1", status = backendStatus, fullName = "Sara")) },
                updateResult = { _, _, request ->
                    capturedStatus = request.status
                    customerDto(id = "customer-1", userId = "user-1", status = backendStatus, fullName = request.fullName ?: "Sara")
                },
            )
            val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")
            repository.sync()
            val cached = repository.getById("customer-1")!!

            repository.update(cached.copy(name = "Sara Renamed"))

            assertEquals("status must stay unsent for backend status $backendStatus on a name-only edit", null, capturedStatus)
        }
    }

    @Test
    fun `update resends status only when the tag picker genuinely changed it`() = runBlocking {
        var capturedStatus: NetworkCustomerStatus? = null
        val api = FakeManagerCustomerApi(
            listResult = { listOf(customerDto(id = "customer-1", userId = "user-1", status = NetworkCustomerStatus.LEAD)) },
            updateResult = { _, _, request ->
                capturedStatus = request.status
                customerDto(id = "customer-1", userId = "user-1", status = request.status ?: NetworkCustomerStatus.LEAD)
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")
        repository.sync()
        val cached = repository.getById("customer-1")!!
        assertEquals(CustomerTag.NEW, cached.tag) // LEAD maps to NEW

        repository.update(cached.copy(tag = CustomerTag.VIP))

        assertEquals(NetworkCustomerStatus.VIP, capturedStatus)
    }

    private class FakeManagerCustomerApi(
        private val listResult: () -> List<CustomerResponseDto> = { error("not used by these tests") },
        private val updateResult: suspend (String, String, UpdateCustomerRequestDto) -> CustomerResponseDto = { _, _, _ -> error("not used by these tests") },
    ) : ManagerCustomerApi {
        override suspend fun list(salonId: String, page: Int, size: Int, status: String?, tag: String?, search: String?, sortDirection: String): PagedResponseDto<CustomerResponseDto> {
            val items = listResult()
            return PagedResponseDto(content = items, page = 0, size = 200, totalElements = items.size.toLong(), totalPages = 1)
        }
        override suspend fun get(salonId: String, customerId: String): CustomerResponseDto = error("not used by these tests")
        override suspend fun notes(salonId: String, customerId: String): List<CustomerNoteResponseDto> = error("not used by these tests")
        override suspend fun bookings(salonId: String, customerId: String, page: Int, size: Int, status: String?, sortDirection: String): PagedResponseDto<BookingResponseDto> = error("not used by these tests")
        override suspend fun create(salonId: String, request: CreateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
        override suspend fun update(salonId: String, customerId: String, request: UpdateCustomerRequestDto): CustomerResponseDto =
            updateResult(salonId, customerId, request)
    }
}
