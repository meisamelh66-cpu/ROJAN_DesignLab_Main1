package ai.rojan.designlab.domain.identity

/**
 * The single source of permission definitions — Identity Foundation
 * Final Stabilization (Patch 3): "No class should create permission
 * definitions directly" means everywhere in this codebase, a
 * [PermissionDefinition] or [PermissionGroup] instance originates here,
 * never constructed ad hoc elsewhere. [ai.rojan.designlab.domain.identity.IdentityEngine]
 * and [ai.rojan.designlab.domain.identity.rules.PermissionRuleProvider]
 * depend only on this interface for anything permission-related — never
 * on a concrete provider like
 * [ai.rojan.designlab.data.identity.DemoPermissionCatalogProvider]
 * directly. Future providers
 * (`BackendPermissionCatalogProvider`, `EnterprisePermissionCatalogProvider`)
 * implement this same interface without any Domain-layer change.
 */
interface PermissionCatalogProvider {
    fun allDefinitions(): List<PermissionDefinition>
    fun definitionForKey(permissionKey: PermissionKey): PermissionDefinition?

    fun allGroups(): List<PermissionGroup>
    fun groupById(groupId: String): PermissionGroup?
}
