package ai.rojan.designlab.domain.beauty

/**
 * Beauty DNA persistence - foundation only. No backend endpoint exists for
 * a customer beauty profile anywhere in the ROJAN backend's API surface, so
 * [InMemoryBeautyProfileRepository] is a session-scoped, process-memory-only
 * implementation, not a placeholder pretending to sync - same "provider
 * interface + honest non-backend implementation, not fake business logic"
 * reasoning already established for AI recommendations (see
 * `domain/ai/AiRecommendationProvider.kt`). Swapping in a real backend-
 * backed implementation later needs a new implementation of this interface,
 * not a UI change.
 */
interface BeautyProfileRepository {
    fun get(customerId: String): CustomerBeautyProfile
    fun save(profile: CustomerBeautyProfile)
}

/** Not persisted beyond process lifetime - cleared on app restart, same as any other in-memory-only state in this app. */
class InMemoryBeautyProfileRepository : BeautyProfileRepository {
    private val profiles = mutableMapOf<String, CustomerBeautyProfile>()

    override fun get(customerId: String): CustomerBeautyProfile =
        profiles[customerId] ?: CustomerBeautyProfile(customerId = customerId)

    override fun save(profile: CustomerBeautyProfile) {
        profiles[profile.customerId] = profile
    }
}
