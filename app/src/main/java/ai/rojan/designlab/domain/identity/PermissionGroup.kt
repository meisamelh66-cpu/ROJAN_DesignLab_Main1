package ai.rojan.designlab.domain.identity

/**
 * Identity Foundation Final Stabilization (Patch 2): the middle tier of
 * the Category → Group → Definition hierarchy. Groups organize related
 * permissions within a category (e.g. "Reports" and "Purchasing" are
 * groups inside the FINANCE and INVENTORY categories respectively) —
 * [PermissionDefinition] now references a group (via [groupId]), not a
 * category directly; the category is reached by following the group.
 */
data class PermissionGroup(
    val groupId: String,
    val groupKey: String,
    val groupName: String,
    val category: PermissionCategory,
)
