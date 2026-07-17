package ai.rojan.designlab.domain.ai

/**
 * AI Recommendation infrastructure — interface + empty implementation,
 * exactly as instructed ("provider interface + empty implementation is
 * acceptable, fake AI is not"). No real AI/ML logic exists anywhere in
 * this codebase to build a genuine implementation on, and every other
 * "AI" label already present elsewhere in this app is static cosmetic
 * content, not live logic — [NoOpAiRecommendationProvider] deliberately
 * does not pretend otherwise.
 */
interface AiRecommendationProvider {
    fun recommendationsFor(context: RecommendationContext): List<AiRecommendation>
}

/** Returns nothing — this is the honest current state, not a placeholder pretending to work. */
class NoOpAiRecommendationProvider : AiRecommendationProvider {
    override fun recommendationsFor(context: RecommendationContext): List<AiRecommendation> = emptyList()
}
