package ai.rojan.designlab.domain.usecase

import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.repository.RoleRepository

/** Persists the user's chosen [Role]. One use case, one responsibility. */
class SaveSelectedRoleUseCase(
    private val repository: RoleRepository,
) {
    suspend operator fun invoke(role: Role) {
        repository.saveSelectedRole(role)
    }
}
