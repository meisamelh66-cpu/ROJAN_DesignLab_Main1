package ai.rojan.designlab.manager.domain.customer

/**
 * Manager Domain Foundation Phase 1 — structured replacement for the
 * free-text Persian "tag" string the old screen-local `ManagerCustomer`
 * used. [displayLabel] maps back onto the exact strings the Customers
 * screens already render, so migrating call sites to it is a pure
 * type change with no visible diff.
 */
enum class CustomerTag {
    VIP,
    NEW,
    INACTIVE,
    REGULAR,
}

val CustomerTag.displayLabel: String
    get() = when (this) {
        CustomerTag.VIP -> "VIP"
        CustomerTag.NEW -> "مشتری جدید"
        CustomerTag.INACTIVE -> "غیرفعال"
        CustomerTag.REGULAR -> "مشتری وفادار"
    }
