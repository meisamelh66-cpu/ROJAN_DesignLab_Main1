package ai.rojan.designlab.data.identity

import ai.rojan.designlab.domain.identity.PermissionCatalogProvider
import ai.rojan.designlab.domain.identity.PermissionCategory
import ai.rojan.designlab.domain.identity.PermissionDefinition
import ai.rojan.designlab.domain.identity.PermissionGroup
import ai.rojan.designlab.domain.identity.PermissionKey

/**
 * Infrastructure implementation — Identity Foundation Final
 * Stabilization (Patch 2/3). The single source of both
 * [PermissionGroup]s and [PermissionDefinition]s in this codebase — a
 * small, illustrative demo hierarchy (not exhaustive), matching the
 * RuleBook's own worked examples: Finance→Reports,
 * Staff→Scheduling, Services→Pricing.
 *
 * Version 1.0 Scope Lock: the earlier illustrative examples here
 * referenced Inventory and Marketplace permission groups — both
 * out-of-scope modules per the Scope Lock ("Remove... placeholders").
 * Replaced with in-scope examples (Staff scheduling, Service pricing)
 * since this catalog was never connected to any real UI anyway
 * (confirmed in the original Identity Foundation audit) - only the
 * illustrative labels needed to change, no functional removal was
 * possible because there was never a working feature here to remove.
 */
class DemoPermissionCatalogProvider : PermissionCatalogProvider {

    private val groups = listOf(
        PermissionGroup("group_001", "finance.reports", "گزارش‌های مالی", PermissionCategory.FINANCE),
        PermissionGroup("group_002", "staff.scheduling", "زمان‌بندی کارکنان", PermissionCategory.STAFF),
        PermissionGroup("group_003", "services.pricing", "قیمت‌گذاری خدمات", PermissionCategory.SERVICES),
        PermissionGroup("group_004", "staff.management", "مدیریت کارکنان", PermissionCategory.STAFF),
        PermissionGroup("group_005", "booking.management", "مدیریت نوبت‌ها", PermissionCategory.BOOKING),
        PermissionGroup("group_006", "permissions.administration", "مدیریت دسترسی‌ها", PermissionCategory.PERMISSIONS_ADMIN),
        PermissionGroup("group_007", "crm.reports", "گزارش‌های مشتریان", PermissionCategory.CRM),
    )

    private val definitions = listOf(
        // Finance → Reports
        PermissionDefinition("perm_001", PermissionKey("finance.report.view"), "مشاهده گزارش مالی", "group_001"),
        PermissionDefinition("perm_002", PermissionKey("finance.report.edit"), "ویرایش گزارش مالی", "group_001"),
        PermissionDefinition("perm_003", PermissionKey("finance.report.export"), "خروجی گزارش مالی", "group_001"),
        // Staff → Scheduling
        PermissionDefinition("perm_004", PermissionKey("staff.schedule.create"), "ثبت برنامه‌ی کاری", "group_002"),
        PermissionDefinition("perm_005", PermissionKey("staff.schedule.approve"), "تایید برنامه‌ی کاری", "group_002"),
        // Services → Pricing
        PermissionDefinition("perm_006", PermissionKey("service.pricing.edit"), "ویرایش قیمت خدمت", "group_003"),
        // Staff → Management
        PermissionDefinition("perm_007", PermissionKey("staff.manage"), "مدیریت کارکنان", "group_004"),
        // Booking → Management
        PermissionDefinition("perm_008", PermissionKey("booking.manage"), "مدیریت نوبت‌ها", "group_005"),
        // Permissions → Administration
        PermissionDefinition("perm_009", PermissionKey("permissions.manage"), "مدیریت دسترسی‌ها", "group_006"),
        // CRM → Reports
        PermissionDefinition("perm_010", PermissionKey("reports.view"), "مشاهده گزارش‌ها", "group_007"),
    )

    override fun allDefinitions(): List<PermissionDefinition> = definitions

    override fun definitionForKey(permissionKey: PermissionKey): PermissionDefinition? =
        definitions.find { it.permissionKey == permissionKey }

    override fun allGroups(): List<PermissionGroup> = groups

    override fun groupById(groupId: String): PermissionGroup? = groups.find { it.groupId == groupId }
}
