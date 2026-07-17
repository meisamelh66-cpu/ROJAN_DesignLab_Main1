package ai.rojan.designlab.domain.identity.rules

import ai.rojan.designlab.domain.identity.PermissionCatalogProvider
import ai.rojan.designlab.domain.identity.PermissionKey
import ai.rojan.designlab.domain.identity.PersonRole

/**
 * Resolves Permission from Role — a real business decision, so it's an
 * extension point. Identity Foundation Final Stabilization: [permissionKey]
 * is now strongly typed ([PermissionKey], Patch 1) instead of a raw
 * String.
 */
interface PermissionRuleProvider {
    fun hasPermission(roles: Set<PersonRole>, permissionKey: PermissionKey): Boolean
}

/**
 * TEMPORARY placeholder, pending real Enterprise permission policy —
 * same caveat as every other `Placeholder*` class in this codebase.
 *
 * Patch 3: depends only on [PermissionCatalogProvider] as the single
 * source of which permission keys genuinely exist — an unregistered key
 * (a typo, or one that was never added to the catalog) can never be
 * granted, checked defensively before any role logic runs. This class
 * still owns the *role→permission* mapping decision (that's its actual
 * job); it does not own *which permissions exist* (that's the
 * catalog's).
 *
 * [catalogProvider] has no default value deliberately — a Domain class
 * must never reference a concrete Infrastructure implementation (like
 * [ai.rojan.designlab.data.identity.DemoPermissionCatalogProvider]),
 * not even as a convenience default. The caller supplies it, same as
 * how [ai.rojan.designlab.domain.identity.IdentityEngine] is
 * constructed with its [ai.rojan.designlab.domain.identity.IdentityProvider]/
 * [ai.rojan.designlab.domain.identity.SessionProvider] — real dependency
 * injection at the composition root, not a Domain-side shortcut.
 */
class PlaceholderPermissionRuleProvider(
    private val catalogProvider: PermissionCatalogProvider,
) : PermissionRuleProvider {

    private val generalManagerExcluded = setOf(PermissionKey("permissions.manage"))

    private val financeKeys = setOf(
        PermissionKey("finance.report.view"),
        PermissionKey("finance.report.edit"),
        PermissionKey("finance.report.export"),
        PermissionKey("reports.view"),
    )

    override fun hasPermission(roles: Set<PersonRole>, permissionKey: PermissionKey): Boolean {
        // The catalog is the single source of truth for which keys exist at all.
        if (catalogProvider.definitionForKey(permissionKey) == null) return false

        return roles.any { role ->
            when (role) {
                PersonRole.OWNER -> true
                PersonRole.GENERAL_MANAGER -> permissionKey !in generalManagerExcluded
                PersonRole.FINANCE -> permissionKey in financeKeys
                PersonRole.RECEPTION, PersonRole.HR, PersonRole.SPECIALIST, PersonRole.CUSTOMER -> false
            }
        }
    }
}
