package ai.rojan.designlab.domain.beauty

/**
 * Beauty DNA foundation - Phase 2 scope is explicitly "create the domain
 * model only... do not implement AI recommendations yet." Fields are plain
 * strings/lists, not enums: the real backend has no beauty-profile concept
 * at all yet to define an authoritative value set against, and guessing a
 * fixed enum now risks not matching whatever contract a future backend
 * integration actually needs.
 */

data class HairProfile(
    val hairType: String? = null,
    val hairColor: String? = null,
    val treatmentHistory: List<String> = emptyList(),
)

data class SkinProfile(
    val skinType: String? = null,
    val concerns: List<String> = emptyList(),
)

data class NailProfile(
    val stylePreference: String? = null,
)

data class CustomerBeautyProfile(
    val customerId: String,
    val hair: HairProfile = HairProfile(),
    val skin: SkinProfile = SkinProfile(),
    val nails: NailProfile = NailProfile(),
)
