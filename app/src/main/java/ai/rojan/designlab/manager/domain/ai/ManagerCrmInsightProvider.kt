package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.ManagerCustomer

/**
 * What kind of real, already-classified fact a [ManagerCrmInsight]
 * restates (Phase 7 Step 5) — not a score or priority, just which rule
 * produced it, so a consumer that only cares about one kind (e.g.
 * [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s
 * inactive-customer count) can filter correctly once more than one rule
 * contributes to the same list (see [CompositeManagerCrmInsightProvider]).
 */
enum class ManagerCrmInsightCategory {
    INACTIVE_CUSTOMER,
    VIP_CUSTOMER,
}

/**
 * One CRM insight a provider surfaces for a salon (Manager CRM AI
 * Foundation, Phase 7 Step 1) — same minimal shape as
 * [ai.rojan.designlab.domain.ai.AiRecommendation] (the Customer-side
 * equivalent this mirrors), not expanded with score/confidence/priority
 * fields no real implementation exists to populate yet. [category]
 * (Phase 7 Step 5) is a classification tag, not a score.
 *
 * [customerId] (Phase 7 Step 6, Insight Customer Reference Foundation) is
 * the structured source-customer reference every current rule already
 * has in scope when building [id] - previously only reachable by parsing
 * [id]'s string convention (`"inactive-customer-{customerId}"`/
 * `"vip-customer-{customerId}"`), a fragile, implicit coupling this field
 * replaces with an explicit one. Model-only this step: nothing reads
 * [customerId] yet - no navigation, no UI decision made or assumed here.
 */
data class ManagerCrmInsight(
    val id: String,
    val category: ManagerCrmInsightCategory,
    val customerId: String,
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

/** Returns nothing — this is the honest current state for a salon this provider has no real rule for, not a placeholder pretending to work. Superseded as the DI-registered default by [CompositeManagerCrmInsightProvider] (Phase 7 Step 5, wrapping [InactiveCustomerInsightProvider]/[VipCustomerInsightProvider]) - kept for tests/future rules that genuinely have nothing to say yet. */
class NoOpManagerCrmInsightProvider : ManagerCrmInsightProvider {
    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> = emptyList()
}
