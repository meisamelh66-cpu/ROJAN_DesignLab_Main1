package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.CustomerTag

/**
 * Manager CRM AI Foundation, Phase 7 Step 5 — the second real
 * [ManagerCrmInsightProvider] implementation: one [ManagerCrmInsight] per
 * customer whose [ai.rojan.designlab.manager.domain.customer.ManagerCustomer.tag]
 * is already [CustomerTag.VIP]. Same shape and same discipline as
 * [InactiveCustomerInsightProvider] — no new judgment, no
 * [ai.rojan.designlab.manager.domain.customer.ManagerCustomer.lastVisit]
 * read, no appointment parsing, no invented threshold.
 *
 * `NetworkCustomerStatus.VIP` maps 1:1 to [CustomerTag.VIP] (see
 * [ai.rojan.designlab.manager.data.BackendCustomerRepository.toDomainTag]) -
 * no lossy merge the way `INACTIVE`/`CHURNED` both collapse into
 * [CustomerTag.INACTIVE], so this rule has no ambiguity to disclose there.
 *
 * Deliberately **not** "VIP customer who's gone quiet" - that reading
 * would need [ManagerCustomer.lastVisit] (only real per-customer, via
 * `loadDetail`, not in the bulk-synced list this provider reads) and a
 * day threshold this codebase has no source for. Per the audit that
 * preceded this implementation (Phase 7 Step 5's own report), that
 * richer rule isn't buildable today without either inventing a threshold
 * or a real architecture change (a `suspend` provider contract, or
 * N+1 detail fetches during sync) - a separate decision, not folded in
 * here. The [ManagerCrmInsight.reason] states only that the customer is
 * VIP - never a value, tenure, or recommendation.
 */
class VipCustomerInsightProvider : ManagerCrmInsightProvider {

    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> =
        context.customers
            .filter { it.tag == CustomerTag.VIP }
            .map { customer ->
                ManagerCrmInsight(
                    id = "vip-customer-${customer.id}",
                    category = ManagerCrmInsightCategory.VIP_CUSTOMER,
                    customerId = customer.id,
                    title = customer.name,
                    reason = "این مشتری VIP است.",
                )
            }
}
