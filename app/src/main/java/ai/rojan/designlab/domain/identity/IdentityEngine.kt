package ai.rojan.designlab.domain.identity

import ai.rojan.designlab.domain.identity.rules.PermissionRuleProvider
import ai.rojan.designlab.domain.identity.rules.PlaceholderPermissionRuleProvider

/**
 * Identity orchestration layer — same shape as every other Engine in
 * this codebase.
 *
 * Identity Foundation Final Stabilization (Patch 3): now depends
 * explicitly on [PermissionCatalogProvider] as well — "IdentityEngine
 * and PermissionRuleProvider must depend ONLY on PermissionCatalogProvider"
 * for anything permission-related, not a second, separate source.
 * [permissionCatalogProvider] has no default value, same reasoning as
 * [identityProvider]/[sessionProvider]: a Domain class must never
 * default to a concrete Infrastructure implementation — the composition
 * root supplies all three.
 *
 * "Who is current" is resolved through [sessionProvider] exclusively —
 * this class never decides that itself (Patch 2).
 */
class IdentityEngine(
    private val identityProvider: IdentityProvider,
    private val sessionProvider: SessionProvider,
    private val permissionCatalogProvider: PermissionCatalogProvider,
    private val permissionRuleProvider: PermissionRuleProvider = PlaceholderPermissionRuleProvider(permissionCatalogProvider),
) {
    fun currentPerson(): PersonIdentity? =
        sessionProvider.currentPersonId()?.let { identityProvider.personById(it) }

    fun currentSalon(): SalonIdentity? =
        sessionProvider.currentSalonId()?.let { identityProvider.salonById(it) }

    fun currentOrganization(): OrganizationIdentity? =
        sessionProvider.currentOrganizationId()?.let { identityProvider.organizationById(it) }

    fun personById(personId: String): PersonIdentity? = identityProvider.personById(personId)

    fun salonById(salonId: String): SalonIdentity? = identityProvider.salonById(salonId)

    fun rolesFor(personId: String, salonId: String): Set<PersonRole> =
        identityProvider.rolesFor(personId, salonId)

    fun hasPermission(personId: String, salonId: String, permissionKey: PermissionKey): Boolean =
        permissionRuleProvider.hasPermission(identityProvider.rolesFor(personId, salonId), permissionKey)

    /** Direct catalog access, per Patch 3's explicit requirement that IdentityEngine depend on the catalog too, not only transitively through the rule provider. */
    fun allPermissionDefinitions(): List<PermissionDefinition> = permissionCatalogProvider.allDefinitions()

    fun allPermissionGroups(): List<PermissionGroup> = permissionCatalogProvider.allGroups()
}
