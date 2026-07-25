package ai.rojan.designlab.domain.model

/**
 * The user roles ROJAN AI supports.
 *
 * Persisted via [ai.rojan.designlab.domain.repository.RoleRepository] and
 * used to route the user to the correct home/dashboard destination, both
 * right after selection and automatically on every later app launch.
 */
enum class Role {
    CUSTOMER,
    SALON_MANAGER,
    STYLIST,
}
