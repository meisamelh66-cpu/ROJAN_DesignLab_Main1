package ai.rojan.designlab.domain.identity

/** The SaaS tenant owner — may own multiple [SalonIdentity] salons (Multi-Branch). */
data class OrganizationIdentity(
    val id: String,
    val name: String,
)
