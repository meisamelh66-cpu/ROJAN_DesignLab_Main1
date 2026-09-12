package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.domain.repository.AuthenticatedUser

/**
 * The single DTO → domain mapping for the backend `UserResponse`. Shared by
 * [BackendAuthRepositoryImpl] (register / login / me / otp-verify) and
 * [UserProfileRepositoryImpl] (avatar / cover upload + delete, Phase 5B) so
 * both surfaces carry the same fields — notably `avatarUrl` / `coverUrl`.
 */
internal fun UserResponseDto.toAuthenticatedUser(): AuthenticatedUser = AuthenticatedUser(
    id = id,
    email = email,
    phoneNumber = phoneNumber,
    fullName = fullName,
    role = role.name,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
)
