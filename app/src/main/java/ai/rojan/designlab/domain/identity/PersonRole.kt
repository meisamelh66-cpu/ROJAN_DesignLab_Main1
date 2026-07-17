package ai.rojan.designlab.domain.identity

/**
 * WHAT positions a person currently holds — distinct from
 * [PersonIdentity] (who they are, permanent) and [Permission] (what
 * they're allowed to do, resolved separately by
 * [ai.rojan.designlab.domain.identity.rules.PermissionRuleProvider]).
 * A role can change; identity never does.
 */
enum class PersonRole {
    OWNER,
    GENERAL_MANAGER,
    RECEPTION,
    FINANCE,
    HR,
    SPECIALIST,
    CUSTOMER,
}

/**
 * "A person may have multiple roles" — this is what makes that
 * structurally real rather than just claimed: a person can appear in
 * multiple [PersonRoleAssignment] records, including more than one for
 * the same salon (e.g., Owner + Finance).
 */
data class PersonRoleAssignment(
    val personId: String,
    val salonId: String,
    val role: PersonRole,
)
