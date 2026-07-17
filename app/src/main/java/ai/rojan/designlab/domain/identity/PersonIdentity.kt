package ai.rojan.designlab.domain.identity

/**
 * WHO a person is — permanent, device-independent, backend-owned in the
 * eventual real system. [id] follows [IdentityIdFormat.personId]'s
 * contract shape. This class carries no Role or Permission information
 * at all — per "Permission must never be inferred directly from
 * Identity," those are separate concepts ([PersonRole], [ai.rojan.designlab.domain.identity.PermissionDefinition]),
 * deliberately not fields here.
 */
data class PersonIdentity(
    val id: String,
    val displayName: String,
    val verifiedMobile: String,
)
