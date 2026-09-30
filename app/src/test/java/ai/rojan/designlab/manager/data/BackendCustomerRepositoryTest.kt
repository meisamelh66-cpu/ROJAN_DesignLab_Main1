package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerNoteRequestDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.LinkCustomerToUserRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkCustomerStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.UserLinkCandidateResponseDto
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Phase F4 — the first test coverage for [BackendCustomerRepository] (none existed before this
 * phase; see the Phase F3 audit's own finding on this gap). Scoped to [BackendCustomerRepository.createNote]
 * only - the operation Phase F4 adds - not a retroactive audit of this repository's other, pre-
 * existing methods (out of this phase's scope).
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
    }

    @Test
    fun `createNote returns a successful Result carrying the backend's note on a successful response`() = runBlocking {
        val api = FakeManagerCustomerApi(
            addNoteResult = { _, _, request ->
                CustomerNoteResponseDto(id = "note-1", authorId = "author-1", text = request.text, createdAt = "2026-01-01T10:00:00")
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        val result = repository.createNote("customer-1", "Prefers morning appointments")

        assertTrue(result.isSuccess)
        assertEquals("note-1", result.getOrNull()?.id)
        assertEquals("Prefers morning appointments", result.getOrNull()?.text)
    }

    @Test
    fun `createNote returns a failed Result when the backend call fails`() = runBlocking {
        val api = FakeManagerCustomerApi(
            addNoteResult = { _, _, _ -> throw IOException("network down") },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        val result = repository.createNote("customer-1", "text")

        assertTrue(result.isFailure)
    }

    @Test
    fun `createNote never mutates the notes cache itself - Backend stays the sole source of truth`() = runBlocking {
        val api = FakeManagerCustomerApi(
            addNoteResult = { _, _, request ->
                CustomerNoteResponseDto(id = "note-1", authorId = "author-1", text = request.text, createdAt = "2026-01-01T10:00:00")
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        repository.createNote("customer-1", "text")

        // getNoteHistory only ever reflects what loadDetail() populated - createNote alone must
        // never append to it (Phase F4's "no optimistic duplicate" requirement).
        assertEquals(emptyList<Any>(), repository.getNoteHistory("customer-1"))
    }

    // ---- CRM Customer -> User Account Linking, Phase 2 --------------------------------------

    private fun customerDto(id: String = "customer-1", userId: String? = null) = CustomerResponseDto(
        id = id,
        salonId = "salon-1",
        userId = userId,
        fullName = "Jane Doe",
        phoneNumber = "+989120000000",
        email = null,
        company = null,
        status = NetworkCustomerStatus.LEAD,
        lifetimeValue = 0.0,
        tags = emptyList(),
        active = true,
        createdAt = "2026-01-01T10:00:00",
        updatedAt = "2026-01-01T10:00:00",
    )

    @Test
    fun `lookupUserForLink calls the salon-scoped lookup endpoint with no phone parameter and maps the real candidate`() = runBlocking {
        var capturedSalonId: String? = null
        var capturedCustomerId: String? = null
        val api = FakeManagerCustomerApi(
            lookupLinkCandidateResult = { salonId, customerId ->
                capturedSalonId = salonId
                capturedCustomerId = customerId
                UserLinkCandidateResponseDto(userId = "user-1", fullName = "Jane Doe", phoneNumber = "+989120000000")
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        val result = repository.lookupUserForLink("customer-1")

        assertTrue(result.isSuccess)
        assertEquals("user-1", result.getOrNull()?.userId)
        assertEquals("Jane Doe", result.getOrNull()?.fullName)
        assertEquals("+989120000000", result.getOrNull()?.phoneNumber)
        assertEquals("salon-1", capturedSalonId)
        assertEquals("customer-1", capturedCustomerId)
    }

    @Test
    fun `lookupUserForLink returns a failed Result when the backend reports no match`() = runBlocking {
        val api = FakeManagerCustomerApi(
            lookupLinkCandidateResult = { _, _ -> throw IOException("404 not found") },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        val result = repository.lookupUserForLink("customer-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `linkToUser sends only the confirmed userId and updates the cache from the real backend response`() = runBlocking {
        var capturedRequest: LinkCustomerToUserRequestDto? = null
        val api = FakeManagerCustomerApi(
            listResult = { PagedResponseDto(content = listOf(customerDto(id = "customer-1", userId = null)), page = 0, size = 200, totalElements = 1, totalPages = 1) },
            linkResult = { _, _, request ->
                capturedRequest = request
                customerDto(id = "customer-1", userId = request.userId)
            },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")
        repository.sync() // populates the cache the same way a real profile view would have already loaded it

        val result = repository.linkToUser("customer-1", "user-1")

        assertTrue(result.isSuccess)
        assertEquals("user-1", result.getOrNull()?.userId)
        assertEquals("user-1", capturedRequest?.userId)
        // The cache now reflects the backend's own confirmed state - getById() proves this is not
        // a locally-fabricated userId, but the exact value the fake "backend" response carried.
        assertEquals("user-1", repository.getById("customer-1")?.userId)
    }

    @Test
    fun `linkToUser returns a failed Result and leaves the cache unchanged when the backend rejects the link`() = runBlocking {
        val api = FakeManagerCustomerApi(
            linkResult = { _, _, _ -> throw IOException("409 already linked") },
        )
        val repository = BackendCustomerRepository(api, serviceRepository, specialistRepository, salonId = "salon-1")

        val result = repository.linkToUser("customer-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals(null, repository.getById("customer-1"))
    }

    private class FakeManagerCustomerApi(
        private val addNoteResult: suspend (String, String, CreateCustomerNoteRequestDto) -> CustomerNoteResponseDto = { _, _, _ -> error("not used by these tests") },
        private val lookupLinkCandidateResult: suspend (String, String) -> UserLinkCandidateResponseDto = { _, _ -> error("not used by these tests") },
        private val linkResult: suspend (String, String, LinkCustomerToUserRequestDto) -> CustomerResponseDto = { _, _, _ -> error("not used by these tests") },
        private val listResult: () -> PagedResponseDto<CustomerResponseDto> = { error("not used by these tests") },
    ) : ManagerCustomerApi {
        override suspend fun list(salonId: String, page: Int, size: Int, status: String?, tag: String?, search: String?, sortDirection: String): PagedResponseDto<CustomerResponseDto> = listResult()
        override suspend fun get(salonId: String, customerId: String): CustomerResponseDto = error("not used by these tests")
        override suspend fun notes(salonId: String, customerId: String): List<CustomerNoteResponseDto> = error("not used by these tests")
        override suspend fun bookings(salonId: String, customerId: String, page: Int, size: Int, status: String?, sortDirection: String): PagedResponseDto<BookingResponseDto> = error("not used by these tests")
        override suspend fun create(salonId: String, request: CreateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
        override suspend fun update(salonId: String, customerId: String, request: UpdateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
        override suspend fun addNote(salonId: String, customerId: String, request: CreateCustomerNoteRequestDto): CustomerNoteResponseDto =
            addNoteResult(salonId, customerId, request)
        override suspend fun lookupLinkCandidate(salonId: String, customerId: String): UserLinkCandidateResponseDto =
            lookupLinkCandidateResult(salonId, customerId)
        override suspend fun link(salonId: String, customerId: String, request: LinkCustomerToUserRequestDto): CustomerResponseDto =
            linkResult(salonId, customerId, request)
    }
}
