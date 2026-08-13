package ai.rojan.designlab.manager.domain.ai

/**
 * Manager CRM AI Foundation, Phase 7 Step 5 — runs every [providers]
 * entry against the same [ManagerCrmInsightContext] and concatenates
 * their results, in order. Exists so each real rule
 * ([InactiveCustomerInsightProvider], [VipCustomerInsightProvider],
 * [VipWithoutAppointmentsInsightProvider]) stays its own small,
 * independently-testable class with a single classification to make -
 * this is the only place that combines them, and it adds no judgment of
 * its own (no re-ranking, no de-duplication beyond what each provider
 * already guarantees by construction, no filtering).
 *
 * CRM Insight Engine Hardening, Phase 8 Step 5 — [insightsFor]'s
 * `flatMap` is the entire implementation, and that's a deliberate,
 * audited guarantee, not an oversight:
 * - A customer matching more than one provider's rule (e.g. a VIP
 *   customer with zero synced appointments, matching both
 *   [VipCustomerInsightProvider] and [VipWithoutAppointmentsInsightProvider])
 *   keeps every resulting [ManagerCrmInsight] - nothing here collapses
 *   multiple real, distinct facts about the same customer into one.
 * - Two insights are never merged or dropped by matching [ManagerCrmInsight.id]
 *   or [ManagerCrmInsight.customerId] - concatenation only, no
 *   `distinctBy`/`associateBy`/similar collapsing operation anywhere in
 *   this class.
 * - Combining order is deterministic: [providers] order, then each
 *   provider's own (already-deterministic) internal order - calling
 *   [insightsFor] twice with the same [providers]/[context] always
 *   produces the same list.
 *
 * **What this class deliberately does not have, and why**: no
 * scoring/confidence, no priority/ranking, no AI prediction, no
 * automatic action selection - none of the current providers compute
 * anything of that kind (each is a deterministic existence/classification
 * check - see their own doc comments), so adding fields or ordering logic
 * for any of these here would mean inventing a signal that doesn't exist,
 * exactly what every provider so far has deliberately avoided.
 *
 * **Real, currently-benign limitation**: [ManagerCrmInsight.id] uniqueness
 * across providers holds only by convention (each rule prefixes its ids
 * distinctly - `"inactive-customer-"`/`"vip-customer-"`/
 * `"vip-without-appointments-"`), not by anything this class enforces. A
 * future provider that reused an existing prefix could produce duplicate
 * ids; nothing here would catch or prevent that.
 */
class CompositeManagerCrmInsightProvider(
    private val providers: List<ManagerCrmInsightProvider>,
) : ManagerCrmInsightProvider {

    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> =
        providers.flatMap { it.insightsFor(context) }
}
