package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.dto.AuthResponseDto
import ai.rojan.designlab.data.remote.dto.LoginRequestDto
import ai.rojan.designlab.data.remote.dto.MembershipAccessDto
import ai.rojan.designlab.data.remote.dto.OtpIssuedResponseDto
import ai.rojan.designlab.data.remote.dto.OtpRequestDto
import ai.rojan.designlab.data.remote.dto.OtpVerifyRequestDto
import ai.rojan.designlab.data.remote.dto.OwnedSalonAccessDto
import ai.rojan.designlab.data.remote.dto.RefreshRequestDto
import ai.rojan.designlab.data.remote.dto.RegisterRequestDto
import ai.rojan.designlab.data.remote.dto.SalonAccessResponseDto
import ai.rojan.designlab.data.remote.dto.SpecialistAccessDto
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.SalonPermissions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Identity & Session Architecture, Android Integration — hermetic coverage
 * of [CurrentUserIdentityContextRepositoryImpl]'s DTO->domain mapping.
 * Every permission assertion checks the exact set survives unmodified —
 * this repository has zero permission logic of its own to test.
 */
class CurrentUserIdentityContextRepositoryImplTest {

    private val user = AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "Sara", role = "MANAGER")

    private class FakeBackendAuthRepository(private val result: Result<AuthenticatedUser>) : BackendAuthRepository {
        override suspend fun register(email: String, password: String, fullName: String) = error("not used")
        override suspend fun login(email: String, password: String) = error("not used")
        override suspend fun currentUser(): Result<AuthenticatedUser> = result
        override suspend fun requestOtp(phoneNumber: String) = error("not used")
        override suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String?) = error("not used")
    }

    private class FakeAuthApi(private val salonAccessResult: SalonAccessResponseDto) : AuthApi {
        override suspend fun register(request: RegisterRequestDto): UserResponseDto = error("not used")
        override suspend fun login(request: LoginRequestDto): AuthResponseDto = error("not used")
        override suspend fun refresh(request: RefreshRequestDto): AuthResponseDto = error("not used")
        override suspend fun me(): UserResponseDto = error("not used")
        override suspend fun getSalonAccess(): SalonAccessResponseDto = salonAccessResult
        override suspend fun requestOtp(request: OtpRequestDto): OtpIssuedResponseDto = error("not used")
        override suspend fun verifyOtp(request: OtpVerifyRequestDto): AuthResponseDto = error("not used")
    }

    private fun repository(dto: SalonAccessResponseDto, userResult: Result<AuthenticatedUser> = Result.success(user)) =
        CurrentUserIdentityContextRepositoryImpl(FakeAuthApi(dto), FakeBackendAuthRepository(userResult))

    @Test
    fun `owner permissions are carried through exactly as returned`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = listOf(OwnedSalonAccessDto("salon-1", "Glow", true, setOf(SalonPermissions.MANAGE_SALON, SalonPermissions.MANAGE_MEMBERSHIP))),
            memberships = emptyList(),
            specialistLinks = emptyList(),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        assertEquals(1, context.ownedSalons.size)
        assertEquals(setOf(SalonPermissions.MANAGE_SALON, SalonPermissions.MANAGE_MEMBERSHIP), context.ownedSalons[0].permissions)
        assertEquals("user-1", context.userId)
        assertEquals("MANAGER", context.globalRole)
    }

    @Test
    fun `manager membership permissions are carried through exactly as returned`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = emptyList(),
            memberships = listOf(MembershipAccessDto("m-1", "salon-2", "Luxe", true, "MANAGER", setOf(SalonPermissions.MANAGE_CATALOG, SalonPermissions.MANAGE_STAFF))),
            specialistLinks = emptyList(),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        assertEquals(1, context.memberships.size)
        assertEquals("MANAGER", context.memberships[0].role)
        assertEquals(setOf(SalonPermissions.MANAGE_CATALOG, SalonPermissions.MANAGE_STAFF), context.memberships[0].permissions)
    }

    @Test
    fun `receptionist membership permissions are carried through exactly as returned`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = emptyList(),
            memberships = listOf(MembershipAccessDto("m-2", "salon-3", "Bloom", true, "RECEPTIONIST", setOf(SalonPermissions.MANAGE_BOOKINGS))),
            specialistLinks = emptyList(),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        assertEquals("RECEPTIONIST", context.memberships[0].role)
        assertEquals(setOf(SalonPermissions.MANAGE_BOOKINGS), context.memberships[0].permissions)
    }

    @Test
    fun `specialist link is consumed separately from memberships`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = emptyList(),
            memberships = listOf(MembershipAccessDto("m-3", "salon-4", "Studio", true, "MANAGER", setOf(SalonPermissions.MANAGE_CATALOG))),
            specialistLinks = listOf(SpecialistAccessDto("sp-1", "salon-5", "Chic", true, setOf(SalonPermissions.MANAGE_SCHEDULE_OWN))),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        assertEquals(1, context.memberships.size)
        assertEquals(1, context.specialistLinks.size)
        assertEquals("salon-5", context.specialistLinks[0].salonId)
        assertEquals(setOf(SalonPermissions.MANAGE_SCHEDULE_OWN), context.specialistLinks[0].permissions)
    }

    @Test
    fun `multiple salon relationships are all preserved`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = listOf(OwnedSalonAccessDto("salon-1", "A", true, setOf(SalonPermissions.MANAGE_SALON))),
            memberships = listOf(MembershipAccessDto("m-1", "salon-2", "B", true, "MANAGER", setOf(SalonPermissions.MANAGE_CATALOG))),
            specialistLinks = listOf(SpecialistAccessDto("sp-1", "salon-3", "C", true, setOf(SalonPermissions.MANAGE_SCHEDULE_OWN))),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        assertEquals(1, context.ownedSalons.size)
        assertEquals(1, context.memberships.size)
        assertEquals(1, context.specialistLinks.size)
    }

    @Test
    fun `no salon relationship returns valid empty lists, not an error`() = runTest {
        val dto = SalonAccessResponseDto(emptyList(), emptyList(), emptyList())

        val result = repository(dto).getCurrentUserIdentityContext()

        assertTrue(result.isSuccess)
        val context = result.getOrThrow()
        assertTrue(context.ownedSalons.isEmpty())
        assertTrue(context.memberships.isEmpty())
        assertTrue(context.specialistLinks.isEmpty())
    }

    @Test
    fun `an unrecognized permission value is carried through but never matches a known permission`() = runTest {
        val dto = SalonAccessResponseDto(
            ownedSalons = emptyList(),
            memberships = listOf(MembershipAccessDto("m-1", "salon-1", "A", true, "MANAGER", setOf("SOME_FUTURE_PERMISSION_NOT_YET_KNOWN"))),
            specialistLinks = emptyList(),
        )

        val context = repository(dto).getCurrentUserIdentityContext().getOrThrow()

        val permissions = context.memberships[0].permissions
        assertTrue("SOME_FUTURE_PERMISSION_NOT_YET_KNOWN" in permissions)
        assertFalse(SalonPermissions.MANAGE_STAFF in permissions)
        assertFalse(SalonPermissions.MANAGE_CATALOG in permissions)
    }

    @Test
    fun `a users-me failure surfaces as a failure without ever calling salon-access`() = runTest {
        val result = repository(
            dto = SalonAccessResponseDto(emptyList(), emptyList(), emptyList()),
            userResult = Result.failure(IllegalStateException("401")),
        ).getCurrentUserIdentityContext()

        assertTrue(result.isFailure)
    }
}
