package ai.rojan.designlab.domain.identity

/**
 * The bottom tier of the Category → Group → Definition hierarchy
 * (Identity Foundation Final Stabilization, Patch 2). [permissionKey] is
 * now strongly typed ([PermissionKey], Patch 1) rather than a raw
 * String. [groupId] references a [PermissionGroup] — category is
 * reached by following the group, not stored redundantly here.
 *
 * Every instance of this class must come from
 * [PermissionCatalogProvider] (Patch 3) — no other class constructs one
 * directly.
 */
data class PermissionDefinition(
    val permissionId: String,
    val permissionKey: PermissionKey,
    val permissionName: String,
    val groupId: String,
    val description: String? = null,
)
