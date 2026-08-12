package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.CustomerTag

/**
 * Manager CRM AI Foundation, Phase 7 Step 3 — the first real
 * [ManagerCrmInsightProvider] implementation: one [ManagerCrmInsight] per
 * customer whose [ai.rojan.designlab.manager.domain.customer.ManagerCustomer.tag]
 * is already [CustomerTag.INACTIVE].
 *
 * Deliberately introduces **no new judgment** - the "who counts as
 * inactive" decision already exists, made by the real backend
 * (`NetworkCustomerStatus.INACTIVE`/`CHURNED`, both mapped to
 * [CustomerTag.INACTIVE] by
 * [ai.rojan.designlab.manager.data.BackendCustomerRepository.toDomain]).
 * This provider only surfaces that existing classification; it does not
 * read [ManagerCustomer.lastVisit], does not parse any appointment date,
 * and does not invent a day threshold - per the audit that preceded this
 * implementation (Phase 7 Step 3's own report), none of that data is
 * reliable enough on the client to support a date-based rule today.
 *
 * Each [ManagerCrmInsight.reason] states only that the customer is
 * inactive - never a duration or a cause, since neither is real data this
 * provider has access to. [CustomerTag.INACTIVE] itself already conflates
 * the backend's distinct `INACTIVE`/`CHURNED` statuses (a pre-existing,
 * disclosed lossy mapping - not something this provider can or should try
 * to undo).
 */
class InactiveCustomerInsightProvider : ManagerCrmInsightProvider {

    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> =
        context.customers
            .filter { it.tag == CustomerTag.INACTIVE }
            .map { customer ->
                ManagerCrmInsight(
                    id = "inactive-customer-${customer.id}",
                    category = ManagerCrmInsightCategory.INACTIVE_CUSTOMER,
                    title = customer.name,
                    reason = "این مشتری غیرفعال است.",
                )
            }
}
