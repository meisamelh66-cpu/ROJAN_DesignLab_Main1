package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AuthApi
import ai.rojan.designlab.data.remote.dto.MembershipAccessDto
import ai.rojan.designlab.data.remote.dto.OwnedSalonAccessDto
import ai.rojan.designlab.data.remote.dto.SpecialistAccessDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OwnedSalonAccess
import ai.rojan.designlab.domain.repository.SalonMembershipAccess
import ai.rojan.designlab.domain.repository.SpecialistAccess

/**
 * Combines [BackendAuthRepository.currentUser] (`GET /users/me`, already
 * real) with `GET /users/me/salon-access` (new) into one
 * [CurrentUserIdentityContext] - two backend calls, one domain result. Pure
 * DTO->domain mapping only: [MembershipAccessDto.permissions]/
 * [OwnedSalonAccessDto.permissions]/[SpecialistAccessDto.permissions] are
 * carried through as received, never recomputed or filtered here.
 */
class CurrentUserIdentityContextRepositoryImpl(
    private val authApi: AuthApi,
    private val backendAuthRepository: BackendAuthRepository,
) : CurrentUserIdentityContextRepository {

    override suspend fun getCurrentUserIdentityContext(): Result<CurrentUserIdentityContext> {
        val user = backendAuthRepository.currentUser().getOrElse { return Result.failure(it) }

        return safeApiCall { authApi.getSalonAccess() }.map { dto ->
            CurrentUserIdentityContext(
                userId = user.id,
                phoneNumber = user.phoneNumber,
                email = user.email,
                fullName = user.fullName,
                globalRole = user.role,
                ownedSalons = dto.ownedSalons.map { it.toDomain() },
                memberships = dto.memberships.map { it.toDomain() },
                specialistLinks = dto.specialistLinks.map { it.toDomain() },
            )
        }
    }

    private fun OwnedSalonAccessDto.toDomain() = OwnedSalonAccess(
        salonId = salonId,
        salonName = salonName,
        active = active,
        permissions = permissions,
    )

    private fun MembershipAccessDto.toDomain() = SalonMembershipAccess(
        membershipId = membershipId,
        salonId = salonId,
        salonName = salonName,
        active = active,
        role = role,
        permissions = permissions,
    )

    private fun SpecialistAccessDto.toDomain() = SpecialistAccess(
        specialistId = specialistId,
        salonId = salonId,
        salonName = salonName,
        active = active,
        permissions = permissions,
    )
}
