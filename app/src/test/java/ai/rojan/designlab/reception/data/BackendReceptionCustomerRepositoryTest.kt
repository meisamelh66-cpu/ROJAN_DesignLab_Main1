package ai.rojan.designlab.reception.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerNoteRequestDto
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerIdentityResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerNoteResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.LinkCustomerToUserRequestDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.UserLinkCandidateResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Gap P0-3-REC-IDENTITY-01 - proves [BackendReceptionCustomerRepository.listCustomers] calls the
 * real, already backend-tested `GET .../customers/identity` (`ManagerCustomerApi.searchIdentity`,
 * `Permission.VIEW_CUSTOMER_IDENTITY` - see `SalonPilotRbacIntegrationTest.kt:368-399` on the
 * backend), never the owner-only `GET .../customers` ([ManagerCustomerApi.list]) a real Reception
 * caller cannot use (403). [FakeManagerCustomerApi.list] below deliberately throws if invoked, so
 * any regression back to the old call site fails loudly, not silently.
 */
class BackendReceptionCustomerRepositoryTest {

    @Test
    fun `listCustomers calls the identity endpoint and maps every field, never the owner-only list endpoint`() = runBlocking {
        val api = FakeManagerCustomerApi(
            searchIdentityResult = {
                PagedResponseDto(
                    content = listOf(
                        CustomerIdentityResponseDto(
                            id = "cust-1",
                            salonId = "salon-1",
                            fullName = "Walk-in Wendy",
                            phoneNumber = "+989123456700",
                            email = null,
                            active = true,
                        ),
                    ),
                    page = 0,
                    size = 20,
                    totalElements = 1,
                    totalPages = 1,
                )
            },
        )
        val repository = BackendReceptionCustomerRepository(api)

        val result = repository.listCustomers("salon-1")

        assertTrue(result.isSuccess)
        val customer = result.getOrNull()!!.content.single()
        assertEquals("cust-1", customer.id)
        assertEquals("salon-1", customer.salonId)
        assertEquals("Walk-in Wendy", customer.fullName)
        assertEquals("+989123456700", customer.phoneNumber)
        assertEquals(null, customer.email)
        assertEquals(true, customer.active)
    }

    @Test
    fun `listCustomers passes the search query through to the identity endpoint`() = runBlocking {
        var capturedSearch: String? = "not-called"
        val api = FakeManagerCustomerApi(
            searchIdentityResult = { search ->
                capturedSearch = search
                PagedResponseDto(emptyList(), 0, 20, 0, 0)
            },
        )
        val repository = BackendReceptionCustomerRepository(api)

        repository.listCustomers("salon-1", search = "wendy")

        assertEquals("wendy", capturedSearch)
    }

    @Test
    fun `listCustomers returns an empty but successful PagedResult when the identity endpoint has no matches`() = runBlocking {
        val api = FakeManagerCustomerApi(
            searchIdentityResult = { PagedResponseDto(emptyList(), 0, 20, 0, 0) },
        )
        val repository = BackendReceptionCustomerRepository(api)

        val result = repository.listCustomers("salon-1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.content.isEmpty())
    }

    @Test
    fun `listCustomers returns a failed Result carrying the real status code on a 401`() = runBlocking {
        val api = FakeManagerCustomerApi(searchIdentityResult = { throw httpException(401) })
        val repository = BackendReceptionCustomerRepository(api)

        val result = repository.listCustomers("salon-1")

        assertTrue(result.isFailure)
        assertEquals(401, (result.exceptionOrNull() as ai.rojan.designlab.data.remote.BackendApiException).statusCode)
    }

    @Test
    fun `listCustomers returns a failed Result carrying the real status code on a 403`() = runBlocking {
        val api = FakeManagerCustomerApi(searchIdentityResult = { throw httpException(403) })
        val repository = BackendReceptionCustomerRepository(api)

        val result = repository.listCustomers("salon-1")

        assertTrue(result.isFailure)
        assertEquals(403, (result.exceptionOrNull() as ai.rojan.designlab.data.remote.BackendApiException).statusCode)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}

/** Scoped to this test file only, same idiom as [ai.rojan.designlab.manager.data.BackendCustomerRepositoryTest]'s own local fake - [list]/[get] throw unconditionally, so a regression back to the owner-only endpoints fails loudly instead of silently passing. */
private class FakeManagerCustomerApi(
    private val searchIdentityResult: suspend (search: String?) -> PagedResponseDto<CustomerIdentityResponseDto> = { error("searchIdentity not used by this test") },
) : ManagerCustomerApi {
    override suspend fun list(salonId: String, page: Int, size: Int, status: String?, tag: String?, search: String?, sortDirection: String): PagedResponseDto<CustomerResponseDto> =
        error("Reception must never call the owner-only list() endpoint")
    override suspend fun searchIdentity(salonId: String, page: Int, size: Int, search: String?): PagedResponseDto<CustomerIdentityResponseDto> =
        searchIdentityResult(search)
    override suspend fun get(salonId: String, customerId: String): CustomerResponseDto = error("not used by these tests")
    override suspend fun notes(salonId: String, customerId: String): List<CustomerNoteResponseDto> = error("not used by these tests")
    override suspend fun bookings(salonId: String, customerId: String, page: Int, size: Int, status: String?, sortDirection: String): PagedResponseDto<BookingResponseDto> = error("not used by these tests")
    override suspend fun create(salonId: String, request: CreateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
    override suspend fun update(salonId: String, customerId: String, request: UpdateCustomerRequestDto): CustomerResponseDto = error("not used by these tests")
    override suspend fun addNote(salonId: String, customerId: String, request: CreateCustomerNoteRequestDto): CustomerNoteResponseDto = error("not used by these tests")
    override suspend fun lookupLinkCandidate(salonId: String, customerId: String): UserLinkCandidateResponseDto = error("not used by these tests")
    override suspend fun link(salonId: String, customerId: String, request: LinkCustomerToUserRequestDto): CustomerResponseDto = error("not used by these tests")
}
