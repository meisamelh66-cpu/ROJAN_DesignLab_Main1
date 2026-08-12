package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.ManagerCustomer

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
 * point, same reasoning as
 * [ai.rojan.designlab.domain.ai.RecommendationContext]. Salon-scoped
 * rather than customer-scoped: Manager CRM insights operate at the salon
 * level (across its customers/appointments/services), not a single
 * customer.
 *
 * [customers] (Phase 7 Step 3) is the salon's already-synced customer
 * list — [ai.rojan.designlab.manager.data.ManagerRepositories.initialize]
 * passes `customerRepo.getAll()` straight through, so a real provider
 * implementation is a pure function over already-fetched data and never
 * triggers a network call of its own (consistent with [insightsFor]
 * staying non-`suspend`).
 */
data class ManagerCrmInsightContext(
    val salonId: String,
    val customers: List<ManagerCustomer>,
)

/**
 * Manager CRM AI Foundation, Phase 7 Step 1 — provider contract. A real
 * implementation is a pure, synchronous function over
 * [ManagerCrmInsightContext] - no network call, no suspend, per the
 * already-established rule ("provider interface + empty implementation is
 * acceptable, fake AI is not" —
 * [ai.rojan.designlab.domain.ai.AiRecommendationProvider]'s doc comment).
 */
interface ManagerCrmInsightProvider {
    fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight>
}

/** Returns nothing — this is the honest current state for a salon this provider has no real rule for, not a placeholder pretending to work. Superseded as the DI-registered default by [InactiveCustomerInsightProvider] (Phase 7 Step 3) - kept for tests/future rules that genuinely have nothing to say yet. */
class NoOpManagerCrmInsightProvider : ManagerCrmInsightProvider {
    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> = emptyList()
}
