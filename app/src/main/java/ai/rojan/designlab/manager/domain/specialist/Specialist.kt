package ai.rojan.designlab.manager.domain.specialist

/**
 * Manager Domain Foundation Phase 1 — new model, no prior screen-local
 * equivalent existed (Calendar's specialist filter only ever used plain
 * name strings). [workingHours] is a plain description rather than a
 * structured schedule — deliberate simplification, a real scheduling
 * model is P2 (Specialists module) work, not this phase's.
 */
data class Specialist(
    val id: String,
    val name: String,
    val skills: List<String>,
    val workingHours: String,
    val commissionRate: Double,
    val active: Boolean,
)
