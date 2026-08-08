package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.reminder.DemoReminderPreference
import ai.rojan.designlab.domain.reminder.InMemoryReminderRepository
import ai.rojan.designlab.domain.reminder.NoOpReminderScheduler
import ai.rojan.designlab.domain.reminder.ReminderRepository
import ai.rojan.designlab.domain.reminder.ReminderScheduler
import ai.rojan.designlab.domain.reminder.ReminderStatus
import ai.rojan.designlab.domain.reminder.ReminderTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Owns the appointment-reminder preference/notification-scheduling state
 * — a real on-device feature (see [ReminderRepository]/[ReminderScheduler]'s
 * own doc comments: genuinely mutable, just not backend-persisted; no
 * AlarmManager/WorkManager delivery yet, an explicitly separate boundary),
 * not fake business data. Extracted (Production Data Integrity Phase 1,
 * Task 7) from `CustomerEcosystemViewModel`, which bundled this real
 * feature together with a large amount of now-gated demo state (loyalty/
 * wallet/coupons/reviews/waitlist/beauty-timeline/favorites) — this is the
 * minimal, standalone slice that's still genuinely needed
 * ([ai.rojan.designlab.screens.profile.AppointmentsScreen]'s reminder
 * toggle), so screens that don't need reminders no longer have to depend
 * on the bigger ecosystem ViewModel just to get this one feature.
 */
class ReminderViewModel : ViewModel() {

    // No external wiring needed (no Context, no network) - a real no-arg
    // constructor, safely usable with the default viewModel() factory
    // (reflection-based, would fail on a Kotlin default-parameter
    // constructor without @JvmOverloads).
    private val reminderRepository: ReminderRepository = InMemoryReminderRepository()
    private val reminderScheduler: ReminderScheduler = NoOpReminderScheduler()

    var preferences by mutableStateOf<List<DemoReminderPreference>>(reminderRepository.getAllPreferences())
        private set

    fun reminderPreferenceFor(appointmentId: String): DemoReminderPreference? =
        preferences.find { it.appointmentId == appointmentId }

    fun setReminderPreference(
        appointmentId: String,
        enabled: Boolean,
        reminderTime: ReminderTime,
        appointmentDateLabel: String,
        appointmentTime: String,
    ) {
        val preference = DemoReminderPreference(
            appointmentId = appointmentId,
            enabled = enabled,
            reminderTime = reminderTime,
            status = if (enabled) ReminderStatus.SCHEDULED else ReminderStatus.CANCELLED,
        )
        reminderRepository.savePreference(preference)
        if (enabled) {
            reminderScheduler.schedule(preference, appointmentDateLabel, appointmentTime)
        } else {
            reminderScheduler.cancel(appointmentId)
        }
        preferences = reminderRepository.getAllPreferences()
    }
}
