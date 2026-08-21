package ai.rojan.designlab.manager.domain.specialist

/**
 * Manager Domain Foundation Phase 1 — new model, no prior screen-local
 * equivalent existed (Calendar's specialist filter only ever used plain
 * name strings). [workingHours] is a plain description rather than a
 * structured schedule — deliberate simplification, a real scheduling
 * model is P2 (Specialists module) work, not this phase's.
 *
 * [photoUrl] (Media Sprint P0): a real, backend-persisted field
 * (`Specialist.photoUrl` on the backend domain entity) — not a placeholder
 * like [skills]/[workingHours]/[commissionRate] below. Set by uploading
 * through the existing Media Foundation (`ManagerMediaType.SPECIALIST_PHOTO`)
 * and writing the returned URL back via the existing specialist
 * create/update endpoints, which already carried this field on the wire
 * before any Android UI read or wrote it.
 */
data class Specialist(
    val id: String,
    val name: String,
    val skills: List<String>,
    val workingHours: String,
    val commissionRate: Double,
    val active: Boolean,
    val photoUrl: String? = null,
)
