package ai.rojan.designlab.domain.customer.rules

import ai.rojan.designlab.data.demo.DemoAppointment

/**
 * What makes a salon/specialist "preferred" — simple frequency is one
 * reasonable definition, but a real system might weight by recency,
 * rating given, or spend. Extension point rather than assuming
 * frequency is the final answer.
 */
interface PreferredEntityRuleProvider {
    fun preferredSalonName(appointments: List<DemoAppointment>): String?
    fun preferredSpecialistName(appointments: List<DemoAppointment>): String?
}

/** TEMPORARY placeholder, pending BOOK 3 import. Most-frequent-by-count is the simplest definition, not a considered one. */
class PlaceholderPreferredEntityRuleProvider : PreferredEntityRuleProvider {
    override fun preferredSalonName(appointments: List<DemoAppointment>): String? =
        appointments.groupingBy { it.salonName }.eachCount().maxByOrNull { it.value }?.key

    override fun preferredSpecialistName(appointments: List<DemoAppointment>): String? =
        appointments.groupingBy { it.specialistName }.eachCount().maxByOrNull { it.value }?.key
}
