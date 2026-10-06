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
    // Data Integrity / API Hardening (Master Integration Repair, PASS 2/7):
    // real backend fields (`SpecialistResponseDto.bio`/`.photoUrl`), never
    // modeled on this domain type before. `update` on the real backend is a
    // full replace, not a merge (`UpdateSpecialistRequestDto`) — carrying
    // these through and always round-tripping the existing value on edit
    // prevents a name-only save from silently wiping a bio/photo any other
    // client ever sets for the same specialist.
    val bio: String? = null,
    val photoUrl: String? = null,
)
