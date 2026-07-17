package ai.rojan.designlab.domain.identity

/**
 * Broad groupings permissions fall into — this is the one part of the
 * permission model that's genuinely bounded/slow-growing (unlike
 * individual permissions, which grow constantly per the RuleBook's own
 * reasoning), so it stays an enum while [PermissionDefinition] itself
 * doesn't.
 */
enum class PermissionCategory {
    FINANCE,
    STAFF,
    BOOKING,
    SERVICES,
    CRM,
    AI,
    PERMISSIONS_ADMIN,
}
