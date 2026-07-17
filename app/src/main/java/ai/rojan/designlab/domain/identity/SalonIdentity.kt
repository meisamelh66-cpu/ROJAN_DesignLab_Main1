package ai.rojan.designlab.domain.identity

/**
 * A single salon (tenant) — permanent, device-independent. [id] follows
 * [IdentityIdFormat.salonId]'s contract shape. Belongs to an
 * [OrganizationIdentity], since one organization may operate several
 * salons under the Multi-Branch product vision.
 */
data class SalonIdentity(
    val id: String,
    val name: String,
    val organizationId: String,
)
