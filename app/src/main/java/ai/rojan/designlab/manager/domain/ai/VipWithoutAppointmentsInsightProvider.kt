package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.CustomerTag

/**
 * Manager CRM AI Foundation, Phase 8 Step 2 — the first rule to consume
 * [ManagerCrmInsightContext.appointments] (Phase 8 Step 1's context
 * expansion). One [ManagerCrmInsight] per customer whose
 * [ai.rojan.designlab.manager.domain.customer.ManagerCustomer.tag] is
 * [CustomerTag.VIP] **and** whose id doesn't appear as
 * [ai.rojan.designlab.manager.domain.appointment.Appointment.customerId]
 * anywhere in [ManagerCrmInsightContext.appointments].
 *
 * Deliberately an **existence check, not a date check** - it asks "does
 * any synced appointment reference this customer at all," never "how
 * long since their last one." That distinction is what keeps this rule
 * inside the same discipline every prior rule has held to: no threshold,
 * no date parsing, no claim about *when* or *how often* - see
 * [ai.rojan.designlab.manager.domain.ai.VipCustomerInsightProvider]'s own
 * doc comment for why a recency-based reading of "VIP attention" was
 * already evaluated and declined (Phase 7 Step 5's audit) as needing
 * either an invented threshold or a `suspend` provider contract this
 * interface still deliberately doesn't have.
 *
 * **Real, disclosed limitation this rule inherits, not introduces**:
 * [ManagerCrmInsightContext.appointments] is the same salon-wide bulk
 * list [ai.rojan.designlab.manager.data.BackendAppointmentRepository]
 * syncs with no verified sort order and a capped page size (disclosed
 * since Phase 7 Step 3). A VIP customer whose only appointment fell
 * outside that fetched window would incorrectly appear to have none.
 * This rule states only what the *currently synced* data shows, and its
 * [ManagerCrmInsight.reason] says exactly that - never "never visited" or
 * any claim beyond what's actually verifiable from what's in memory right
 * now.
 *
 * Independent of [VipCustomerInsightProvider] and
 * [InactiveCustomerInsightProvider] - neither is modified by this class,
 * and this provider is not wired into
 * [ai.rojan.designlab.di.BackendApiContainer]'s
 * [CompositeManagerCrmInsightProvider] yet; that's a separate activation
 * decision, not assumed here.
 */
class VipWithoutAppointmentsInsightProvider : ManagerCrmInsightProvider {

    override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> {
        val customerIdsWithAppointments = context.appointments.map { it.customerId }.toSet()

        return context.customers
            .filter { it.tag == CustomerTag.VIP && it.id !in customerIdsWithAppointments }
            .map { customer ->
                ManagerCrmInsight(
                    id = "vip-without-appointments-${customer.id}",
                    category = ManagerCrmInsightCategory.VIP_WITHOUT_APPOINTMENTS,
                    customerId = customer.id,
                    title = customer.name,
                    reason = "این مشتری VIP در داده‌های نوبت‌دهی همگام‌سازی‌شده، نوبتی ندارد.",
                )
            }
    }
}
