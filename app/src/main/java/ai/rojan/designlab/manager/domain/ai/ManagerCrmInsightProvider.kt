package ai.rojan.designlab.manager.domain.ai

/**
 * One CRM insight a provider surfaces for a salon (Manager CRM AI
 * Foundation, Phase 7 Step 1) — same minimal shape as
 * [ai.rojan.designlab.domain.ai.AiRecommendation] (the Customer-side
 * equivalent this mirrors), not expanded with score/confidence/priority
 * fields no real implementation exists to populate yet.
 */
data class ManagerCrmInsight(
    val id: String,
    val title: String,
    val reason: String,
)

/**
 * What the provider is being asked to generate insights for — extension
 * point, minimal by design, same reasoning as
 * [ai.rojan.designlab.domain.ai.RecommendationContext]. Salon-scoped
 * rather than customer-scoped: Manager CRM insights operate at the salon
 * level (across its customers/appointments/services), not a single
 * customer.
 */
data class ManagerCrmInsightContext(
    val salonId: String,
)

/**
 * Manager CRM AI Foundation, Phase 7 Step 1 — interface + empty
 * implementation only, mirroring
 * [ai.rojan.designlab.domain.ai.AiRecommendationProvider]'s already-
 * established rule ("provider interface + empty implementation is
 * acceptable, fake AI is not"). No rule logic, scoring, or LLM call
 * exists anywhere in this codebase to build a genuine implementation on;
 * [NoOpManagerCrmInsightProvider] deliberately does not pretend
 * otherwise. Not wired into any screen or [ai.rojan.designlab.manager.data.ManagerRepositories]
 * this step — DI registration only (see [ai.rojan.designlab.di.BackendApiContainer]).
 */
interface ManagerCrmInsightProvider {
    fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight>
}

/** Returns nothing — this is the honest current state, not a placeholder pretending to work. */
class NoOpManagerCrmInsightProvider : ManagerCrmInsightProvider {
    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> = emptyList()
}
