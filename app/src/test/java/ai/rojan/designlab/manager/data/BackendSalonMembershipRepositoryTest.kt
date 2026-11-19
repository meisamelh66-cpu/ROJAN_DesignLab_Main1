package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.SalonMembershipApi
import ai.rojan.designlab.data.remote.dto.AssignMembershipRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkSalonRole
import ai.rojan.designlab.data.remote.dto.SalonMembershipResponseDto
import ai.rojan.designlab.data.remote.dto.SetMembershipActiveRequestDto
import ai.rojan.designlab.data.remote.dto.StaffLookupResponseDto
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Staff Access Phase 1 - [BackendSalonMembershipRepository] existed in Phase 2 (M4) as a real but
 * entirely unconsumed data layer with zero test coverage; this phase wires it up to a real screen
 * and adds the lookup/disable/re-enable methods the new architecture needs, so it earns real
 * coverage now.
 */
class BackendSalonMembershipRepositoryTest {

    @Test
    fun `listMembers maps the real response list including active state and identity`() = runBlocking {
        val api = FakeSalonMembershipApi(
            listResult = {
                listOf(
                    membershipDto(userId = "user-1", active = true),
                    membershipDto(userId = "user-2", active = false),
                )
            },
        )
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.listMembers("salon-1")

        assertTrue(result.isSuccess)
        val members = result.getOrNull()!!
        assertEquals(2, members.size)
        assertTrue(members[0].active)
        assertFalse(members[1].active)
        assertEquals("Staff Member", members[0].fullName)
    }

    @Test
    fun `lookupByPhone sends the real salonId and phone query param`() = runBlocking {
        var capturedSalonId: String? = null
        var capturedPhone: String? = null
        val api = FakeSalonMembershipApi(
            lookupResult = { salonId, phone ->
                capturedSalonId = salonId
                capturedPhone = phone
                StaffLookupResponseDto(userId = "user-9", fullName = "Candidate Person", phoneNumber = phone)
            },
        )
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.lookupByPhone("salon-1", "+989120000001")

        assertEquals("salon-1", capturedSalonId)
        assertEquals("+989120000001", capturedPhone)
        assertTrue(result.isSuccess)
        assertEquals("user-9", result.getOrNull()!!.userId)
    }

    @Test
    fun `lookupByPhone returns a failed Result with the real status code on a 404 - no account`() = runBlocking {
        val api = FakeSalonMembershipApi(lookupResult = { _, _ -> throw httpException(404) })
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.lookupByPhone("salon-1", "+989120000002")

        assertTrue(result.isFailure)
        assertEquals(404, (result.exceptionOrNull() as BackendApiException).statusCode)
    }

    @Test
    fun `assignRole sends the real role and maps the response`() = runBlocking {
        var capturedRequest: AssignMembershipRequestDto? = null
        val api = FakeSalonMembershipApi(
            assignResult = { _, userId, request ->
                capturedRequest = request
                membershipDto(userId = userId, role = request.role, active = true)
            },
        )
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.assignRole("salon-1", "user-1", SalonMemberRole.RECEPTIONIST)

        assertEquals(NetworkSalonRole.RECEPTIONIST, capturedRequest?.role)
        assertTrue(result.isSuccess)
        assertEquals(SalonMemberRole.RECEPTIONIST, result.getOrNull()!!.role)
    }

    @Test
    fun `setActive disabling sends active=false and maps the resulting state`() = runBlocking {
        var capturedRequest: SetMembershipActiveRequestDto? = null
        val api = FakeSalonMembershipApi(
            setActiveResult = { _, userId, request ->
                capturedRequest = request
                membershipDto(userId = userId, active = request.active)
            },
        )
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.setActive("salon-1", "user-1", active = false)

        assertFalse(capturedRequest!!.active)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.active)
    }

    @Test
    fun `setActive re-enabling sends active=true`() = runBlocking {
        var capturedRequest: SetMembershipActiveRequestDto? = null
        val api = FakeSalonMembershipApi(
            setActiveResult = { _, userId, request ->
                capturedRequest = request
                membershipDto(userId = userId, active = request.active)
            },
        )
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.setActive("salon-1", "user-1", active = true)

        assertTrue(capturedRequest!!.active)
        assertTrue(result.getOrNull()!!.active)
    }

    @Test
    fun `removeMember returns a failed Result with the real status code on a 403`() = runBlocking {
        val api = FakeSalonMembershipApi(removeResult = { _, _ -> throw httpException(403) })
        val repository = BackendSalonMembershipRepository(api)

        val result = repository.removeMember("salon-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals(403, (result.exceptionOrNull() as BackendApiException).statusCode)
    }

    private fun membershipDto(
        userId: String,
        role: NetworkSalonRole = NetworkSalonRole.RECEPTIONIST,
        active: Boolean,
    ) = SalonMembershipResponseDto(
        id = "membership-$userId",
        salonId = "salon-1",
        userId = userId,
        role = role,
        active = active,
        fullName = "Staff Member",
        phoneNumber = "+989120000000",
        createdAt = "2026-11-17T10:00:00Z",
        updatedAt = "2026-11-17T10:00:00Z",
    )

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}

/** Scoped to this test file only - every closure throws by default so an unexpected call fails loudly rather than silently returning a fake success. */
private class FakeSalonMembershipApi(
    private val listResult: suspend () -> List<SalonMembershipResponseDto> = { error("not used by this test") },
    private val lookupResult: suspend (salonId: String, phone: String) -> StaffLookupResponseDto = { _, _ -> error("not used by this test") },
    private val assignResult: suspend (salonId: String, userId: String, request: AssignMembershipRequestDto) -> SalonMembershipResponseDto = { _, _, _ -> error("not used by this test") },
    private val setActiveResult: suspend (salonId: String, userId: String, request: SetMembershipActiveRequestDto) -> SalonMembershipResponseDto = { _, _, _ -> error("not used by this test") },
    private val removeResult: suspend (salonId: String, userId: String) -> Unit = { _, _ -> error("not used by this test") },
) : SalonMembershipApi {
    override suspend fun list(salonId: String): List<SalonMembershipResponseDto> = listResult()
    override suspend fun lookup(salonId: String, phone: String): StaffLookupResponseDto = lookupResult(salonId, phone)
    override suspend fun assign(salonId: String, userId: String, request: AssignMembershipRequestDto): SalonMembershipResponseDto =
        assignResult(salonId, userId, request)
    override suspend fun setActive(salonId: String, userId: String, request: SetMembershipActiveRequestDto): SalonMembershipResponseDto =
        setActiveResult(salonId, userId, request)
    override suspend fun remove(salonId: String, userId: String) = removeResult(salonId, userId)
}
