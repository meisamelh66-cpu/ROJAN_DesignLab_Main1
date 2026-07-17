package ai.rojan.designlab.domain.reminder

/**
 * Appointment System completion (V1.0 Module 6 - Reminder). Repository
 * Pattern boundary for reminder preferences — mirrors the shape every
 * other repository interface in this codebase follows
 * ([ai.rojan.designlab.domain.identity.IdentityProvider], etc.):
 * Domain depends on this interface only, never on a concrete
 * implementation.
 */
interface ReminderRepository {
    fun getPreference(appointmentId: String): DemoReminderPreference?
    fun getAllPreferences(): List<DemoReminderPreference>
    fun savePreference(preference: DemoReminderPreference)
}

/**
 * In-memory implementation — same honesty as every other `Demo*`/
 * in-memory repository in this codebase (e.g.
 * [ai.rojan.designlab.data.identity.DemoIdentityProvider]): real,
 * working logic, genuinely mutable at runtime, just not backend-
 * persisted. A future real implementation (e.g. backed by a database or
 * server) implements this same interface — nothing above this class
 * needs to change.
 */
class InMemoryReminderRepository : ReminderRepository {
    private val preferences = mutableMapOf<String, DemoReminderPreference>()

    override fun getPreference(appointmentId: String): DemoReminderPreference? = preferences[appointmentId]

    override fun getAllPreferences(): List<DemoReminderPreference> = preferences.values.toList()

    override fun savePreference(preference: DemoReminderPreference) {
        preferences[preference.appointmentId] = preference
    }
}
