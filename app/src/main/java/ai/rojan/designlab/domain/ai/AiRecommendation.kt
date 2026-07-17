package ai.rojan.designlab.domain.ai

data class AiRecommendation(
    val id: String,
    val title: String,
    val reason: String,
)

/** What the provider is being asked to recommend for — extension point, minimal by design. */
data class RecommendationContext(
    val customerId: String,
)
