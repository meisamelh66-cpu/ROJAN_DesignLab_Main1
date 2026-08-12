package ai.rojan.designlab.manager.domain.ai

/**
 * Manager CRM AI Foundation, Phase 7 Step 5 — runs every [providers]
 * entry against the same [ManagerCrmInsightContext] and concatenates
 * their results, in order. Exists so each real rule
 * ([InactiveCustomerInsightProvider], [VipCustomerInsightProvider]) stays
 * its own small, independently-testable class with a single
 * classification to make - this is the only place that combines them,
 * and it adds no judgment of its own (no re-ranking, no de-duplication
 * beyond what each provider already guarantees by construction, no
 * filtering).
 */
class CompositeManagerCrmInsightProvider(
    private val providers: List<ManagerCrmInsightProvider>,
) : ManagerCrmInsightProvider {

    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> =
        providers.flatMap { it.insightsFor(context) }
}
