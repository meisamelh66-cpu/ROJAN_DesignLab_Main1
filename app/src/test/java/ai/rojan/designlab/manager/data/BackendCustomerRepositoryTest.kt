package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerNoteRequestDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
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

    private class FakeManagerCustomerApi(
        private val addNoteResult: suspend (String, String, CreateCustomerNoteRequestDto) -> CustomerNoteResponseDto = { _, _, _ -> error("not used by these tests") },
    ) : ManagerCustomerApi {
        override suspend fun list(salonId: String, page: Int, size: Int, status: String?, tag: String?, search: String?, sortDirection: String): PagedResponseDto<CustomerResponseDto> = error("not used by these tests")
        override suspend fun get(salonId: String, customerId: String): CustomerResponseDto = error("not used by these tests")
        override suspend fun notes(salonId: String, customerId: String): List<CustomerNoteResponseDto> = error("not used by these tests")
        override suspend fun bookings(salonId: String, customerId: String, page: Int, size: Int, status: String?, sortDirection: String): PagedResponseDto<BookingResponseDto> = error("not used by these tests")
        override suspend fun create(salonId: String, request: CreateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
        override suspend fun update(salonId: String, customerId: String, request: UpdateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
        override suspend fun addNote(salonId: String, customerId: String, request: CreateCustomerNoteRequestDto): CustomerNoteResponseDto =
            addNoteResult(salonId, customerId, request)
    }
}
