package ai.rojan.designlab.manager.domain.service

/**
 * Manager Domain Foundation Phase 1 — new model, no prior screen-local
 * equivalent existed (Calendar only ever stored a service *name* string
 * on its appointment rows). [price] is a raw Toman amount, not a
 * pre-formatted display string.
 */
data class Service(
    val id: String,
    val name: String,
    val category: String,
    val price: Long,
    val durationMinutes: Int,
    val active: Boolean,
)
