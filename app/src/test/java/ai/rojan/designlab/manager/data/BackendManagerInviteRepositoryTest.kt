package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerInviteApi
import ai.rojan.designlab.data.remote.dto.CreateSalonInviteRequestDto
import ai.rojan.designlab.data.remote.dto.SalonInviteResponseDto
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Gap P0-3-REC-INVITE-UI - proves [BackendManagerInviteRepository] calls the
 * real `POST /salons/{salonId}/invites` contract (role only, no phone
 * number - corrected from [ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository]'s
 * earlier, stale guess) and maps the real response back correctly.
 */
class BackendManagerInviteRepositoryTest {

    @Test
    fun `issueInvite sends the real salonId and role=RECEPTIONIST, nothing else`() = runBlocking {
        var capturedSalonId: String? = null
        var capturedRequest: CreateSalonInviteRequestDto? = null
        val api = FakeManagerInviteApi(
            createResult = { salonId, request ->
                capturedSalonId = salonId
                capturedRequest = request
                SalonInviteResponseDto(
                    id = "invite-1",
                    salonId = salonId,
                    role = request.role,
                    token = "real-token-value",
                    status = "CREATED",
                    expiresAt = "2026-11-19T10:00:00Z",
                )
            },
        )
        val repository = BackendManagerInviteRepository(api)

        repository.issueInvite("salon-1", SalonMemberRole.RECEPTIONIST)

        assertEquals("salon-1", capturedSalonId)
        assertEquals(CreateSalonInviteRequestDto(role = "RECEPTIONIST"), capturedRequest)
    }

    @Test
    fun `issueInvite maps a successful response into the domain SalonInvite`() = runBlocking {
        val api = FakeManagerInviteApi(
            createResult = { salonId, request ->
                SalonInviteResponseDto(
                    id = "invite-1",
                    salonId = salonId,
                    role = request.role,
                    token = "real-token-value",
                    status = "CREATED",
                    expiresAt = "2026-11-19T10:00:00Z",
                )
            },
        )
        val repository = BackendManagerInviteRepository(api)

        val result = repository.issueInvite("salon-1", SalonMemberRole.RECEPTIONIST)

        assertTrue(result.isSuccess)
        val invite = result.getOrNull()!!
        assertEquals("invite-1", invite.id)
        assertEquals("salon-1", invite.salonId)
        assertEquals(SalonMemberRole.RECEPTIONIST, invite.role)
        assertEquals("real-token-value", invite.token)
        assertEquals(SalonInviteStatus.CREATED, invite.status)
        assertEquals("2026-11-19T10:00:00Z", invite.expiresAt)
    }

    @Test
    fun `issueInvite returns a failed Result carrying the real status code on a 403 - caller is not the owner`() = runBlocking {
        val api = FakeManagerInviteApi(createResult = { _, _ -> throw httpException(403) })
        val repository = BackendManagerInviteRepository(api)

        val result = repository.issueInvite("salon-1", SalonMemberRole.RECEPTIONIST)

        assertTrue(result.isFailure)
        assertEquals(403, (result.exceptionOrNull() as ai.rojan.designlab.data.remote.BackendApiException).statusCode)
    }

    @Test
    fun `issueInvite returns a failed Result carrying the real status code on a 404 - salon not found`() = runBlocking {
        val api = FakeManagerInviteApi(createResult = { _, _ -> throw httpException(404) })
        val repository = BackendManagerInviteRepository(api)

        val result = repository.issueInvite("salon-1", SalonMemberRole.RECEPTIONIST)

        assertTrue(result.isFailure)
        assertEquals(404, (result.exceptionOrNull() as ai.rojan.designlab.data.remote.BackendApiException).statusCode)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}

/** Scoped to this test file only - [createResult] throwing by default means an unexpected call fails loudly rather than silently returning a fake success. */
private class FakeManagerInviteApi(
    private val createResult: suspend (salonId: String, request: CreateSalonInviteRequestDto) -> SalonInviteResponseDto = { _, _ -> error("not used by this test") },
) : ManagerInviteApi {
    override suspend fun create(salonId: String, request: CreateSalonInviteRequestDto): SalonInviteResponseDto =
        createResult(salonId, request)
}
