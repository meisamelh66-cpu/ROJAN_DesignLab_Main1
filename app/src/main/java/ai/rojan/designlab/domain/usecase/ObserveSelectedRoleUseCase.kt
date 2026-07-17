package ai.rojan.designlab.domain.usecase

import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.repository.RoleRepository
import kotlinx.coroutines.flow.Flow

/**
 * Streams the persisted [Role] (`null` if none has been selected yet).
 *
 * Used on app launch to decide whether to restore a returning user
 * straight to their dashboard, or send a first-time user to Welcome.
 */
class ObserveSelectedRoleUseCase(
    private val repository: RoleRepository,
) {
    operator fun invoke(): Flow<Role?> = repository.observeSelectedRole()
}
