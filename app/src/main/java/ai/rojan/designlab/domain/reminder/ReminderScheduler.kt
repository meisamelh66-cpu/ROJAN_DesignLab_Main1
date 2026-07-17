package ai.rojan.designlab.domain.reminder

/**
 * Appointment System completion (V1.0 Module 6 - Reminder). The
 * abstraction boundary between "the app knows a reminder should exist"
 * (domain layer, real) and "the OS actually delivers it" (platform
 * layer, explicitly out of scope for this pass per the instruction:
 * "Do NOT implement AlarmManager/WorkManager/FCM/Android Notifications").
 *
 * [schedule]/[cancel] describe *intent* - the domain and ViewModel call
 * these exactly as they would against a real implementation, so wiring
 * in a real AlarmManager/WorkManager-backed [ReminderScheduler] later
 * requires zero changes above this interface.
 */
interface ReminderScheduler {
    fun schedule(preference: DemoReminderPreference, appointmentDateLabel: String, appointmentTime: String)
    fun cancel(appointmentId: String)
}

/**
 * Mock/No-Op implementation - the only one this pass provides, per
 * explicit instruction. Does not schedule anything real; exists purely
 * so [ReminderScheduler] has a working default and the rest of the
 * architecture (ViewModel, UI) can be built and genuinely exercised
 * end-to-end today, without waiting on real OS-level scheduling work.
 */
class NoOpReminderScheduler : ReminderScheduler {
    override fun schedule(preference: DemoReminderPreference, appointmentDateLabel: String, appointmentTime: String) {
        // Intentionally no-op - see class doc comment.
    }

    override fun cancel(appointmentId: String) {
        // Intentionally no-op - see class doc comment.
    }
}
