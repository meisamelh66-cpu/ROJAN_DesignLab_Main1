package ai.rojan.designlab.domain.identity

/**
 * Identity Foundation Final Stabilization (Patch 1): strongly-typed
 * replacement for raw `String` permission identifiers throughout the
 * Identity layer. A `@JvmInline value class` has zero runtime overhead
 * over a plain String (compiles down to one at the bytecode level) but
 * makes it a compile error to accidentally pass an arbitrary string
 * (a display name, a role name, a typo) where a permission key is
 * expected — extensibility is unaffected: definitions are still fully
 * data-driven, this only changes the type of the identifier itself.
 */
@JvmInline
value class PermissionKey(val value: String)
