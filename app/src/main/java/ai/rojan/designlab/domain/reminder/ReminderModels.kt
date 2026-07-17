package ai.rojan.designlab.domain.reminder

/**
 * Appointment System completion (V1.0 Module 6 - Reminder, domain
 * first). Real business objects, not placeholders — [ReminderTime]
 * genuinely constrains how far ahead a reminder fires;
 * [DemoReminderPreference] genuinely tracks per-appointment opt-in.
 * What's deliberately NOT here is any real delivery mechanism (no
 * AlarmManager/WorkManager/FCM/Android notifications) - see
 * [ReminderScheduler] for that boundary.
 */
enum class ReminderTime(val hoursBefore: Int, val label: String) {
    H24(24, "۲۴ ساعت قبل"),
    H3(3, "۳ ساعت قبل"),
    H1(1, "۱ ساعت قبل"),
}

enum class ReminderStatus { SCHEDULED, SENT, CANCELLED }

data class DemoReminderPreference(
    val appointmentId: String,
    val enabled: Boolean,
    val reminderTime: ReminderTime,
    val status: ReminderStatus,
)
