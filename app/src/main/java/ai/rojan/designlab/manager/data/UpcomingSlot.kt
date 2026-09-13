package ai.rojan.designlab.manager.data

/**
 * A single row in [ai.rojan.designlab.manager.components.CalendarPreviewSection]
 * — resolved display names, not raw ids.
 *
 * Pre-release reconciliation note: the original computation this type
 * backed (`computeTodaysUpcomingSlots()`, reading the now-retired
 * `ManagerRepositories` in-memory singleton) was removed alongside
 * `ManagerDashboardStats.kt` when the Dashboard moved onto
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]'s
 * real backend wiring. `CalendarPreviewSection` itself was left
 * unchanged in that same pass (its own doc comment: "this task's scope
 * is data persistence for dashboard/calendar/status-update, not a
 * rewrite of every section") — it still takes `slots: List<UpcomingSlot>`
 * with a default `emptyList()`, so the type alone needs to keep existing
 * even though nothing currently computes real values for it. Wiring a
 * real `computeTodaysUpcomingSlots()`-equivalent over
 * [ai.rojan.designlab.domain.repository.BookingRepository.salonBookings]
 * is a real, separate follow-up, not something to invent here.
 */
data class UpcomingSlot(val time: String, val clientName: String, val service: String)
