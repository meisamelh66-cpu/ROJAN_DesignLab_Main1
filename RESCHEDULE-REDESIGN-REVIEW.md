# Reschedule Appointment Screen — Redesign Review

**Date:** 2026-09-09 · **Scope:** `screens/profile/RescheduleAppointmentScreen.kt` (route
`RESCHEDULE_APPOINTMENT`, "تغییر زمان رزرو") only. No other file touched.
**Not committed.**

---

## 1. File changed (1)

`screens/profile/RescheduleAppointmentScreen.kt` — rewritten, visual only. Public signature
(`appointmentId`, `onBackClick`, `onRescheduled`, `viewModel`) byte-identical.

**Behaviour preserved verbatim:**
- `RescheduleViewModel` + its factory + `bookingRepository` + `availabilityRepository` — untouched.
- `viewModel::selectDate`, `viewModel::selectTime`, `viewModel.confirm(onRescheduled)`,
  `viewModel.retry()`, and the slots-retry `viewModel.selectDate(state.selectedDate)` — all called
  in the same places, same args.
- `private fun TimeSlot.timeLabel()` = `start.substringAfter('T').take(5)` — unchanged; the grid
  still passes the bare `"HH:mm"` that `selectTime` and `confirm()`'s `"${date}T$time:00"` expect.
- `RescheduleUiState` (`Loading` / `Error` / `Ready`) and its `slots: UiState<List<TimeSlot>>`
  sub-state — read, never changed.

No ViewModel, repository, API contract, booking state, navigation route, callback, or model touched.

## 2. Before → After

| | Before | After |
|---|---|---|
| Shell | `HomeBackgroundTheme { Column(padding SpaceMD) { GlassBackButton() ; Text("تغییر زمان نوبت", HeroTitle 32sp) ; when(state) } }` | `CustomerScaffold(title = "تغییر زمان رزرو", onBackClick, bottomBar = confirm?)` — flat 56dp bar, outlined back, hairline |
| Date chips | `HomeGlassSurface(shape = Small)` per chip — glass + ✦ corners + metallic border + glow; selected = **violet `HomeColors.Glow`** text only | rail of `RefSelectableCell` chips (fixed 112dp) with a **weekday / date** two-line hierarchy (`label` split on "،"); selected = **solid rose-gold fill**, unselected = flat surface; `reverseLayout = true` so today sits on the right (RTL) |
| Time cells | `HomeGlassSurface` + raw `Modifier.clickable`; selected = violet `HomeColors.Glow` text + `HomeColors.Glow.copy(alpha=0.12f)` wash; unselected wash referenced **`HomeColors.Primary`** (dead violet token) | `RefSelectableCell` grid — identical to `BookingTimeScreen.TimeChip`: flat → solid rose-gold selected, `Body` label, centred |
| Section headings | `Text("انتخاب تاریخ" / "انتخاب ساعت", Body, TextPrimary)` | `CustomerSectionLabel("انتخاب تاریخ" / "انتخاب ساعت")` — muted `Caption`/SemiBold |
| Slots loading | `RojanLoadingState(message = …)` glass card | `CustomerLoadingState(count = 4, rowHeight = 52)` |
| Slots error | `RojanErrorState(...)` glass card | `CustomerErrorState(message = slots.message, onRetry = onRetrySlots)` |
| Slots empty | `RojanEmptyState(title = …)` glass card, no body/action | `CustomerEmptyState(title = "زمانی برای این تاریخ موجود نیست", body = "لطفاً تاریخ دیگری را انتخاب کنید.", icon = Icons.Outlined.EventBusy)` |
| Booking loading | `RojanLoadingState(message = …)` glass card | `CustomerLoadingState(count = 5, rowHeight = 56)` |
| Booking error | `RojanErrorState(...)` glass card | `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })` |
| Submit error | `Text(it, color = HomeColors.Magenta)` — **vivid magenta** | calm `Caption` in `HomeColors.TextSecondary`, sits just above the pinned CTA |
| Confirm CTA | `PremiumButton("تایید زمان جدید", enabled, loading)` — magenta→pink gradient pill + inline spinner, scrolls with the content | solid rose-gold `RefPrimaryButton("تایید زمان جدید")` **pinned in `CustomerScaffold`'s `bottomBar`**; `enabled = selectedTime != null && !isSubmitting` |
| `isSubmitting` | gradient button's built-in spinner | the CTA disables (40% alpha) while submitting — same guard against a double-submit; `RefPrimaryButton` has no spinner param, so the label stays "تایید زمان جدید" and the disabled state is the feedback |

Removed imports: `HomeBackgroundTheme`, `HomeGlassSurface`, `GlassBackButton`, `PremiumButton`,
`RojanEmptyState` / `RojanErrorState` / `RojanLoadingState`, `RojanShapes`, `clickable`,
`background`, `rojanPressable`, `HomeColors.Glow` / `.Primary` / `.Magenta`.

## 3. Foundation usage

`CustomerScaffold` (with `bottomBar`) · `CustomerSectionLabel` · `RefSelectableCell` (date chips
**and** time grid) · `CustomerLoadingState` / `CustomerErrorState` / `CustomerEmptyState` ·
`RefPrimaryButton` · tokens `CustomerScreenMargin` · `RojanTypography` / `RojanDimens` /
`HomeColors`. Imported from `screens.customer.components` directly (this screen is in
`screens/profile/`), not the `bookingflow.components` forwarders.

**Screen-local:** `RescheduleContent` (`ColumnScope` extension), `DateChip` — thin composition over
the foundation, no new tokens. `BookingDateScreen`'s `dateParts` helper is **not** reused — the
date `label` is already pre-built by `RollingBookingDates.next7Days()` in `state.dates`.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | **Installed on 1 device**, exit 0 (device dropped ADB after packaging; reconnected, re-ran clean) |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `RescheduleAppointmentScreen.kt` |
| Samsung A72 verification | **PARTIAL** — see §5 |

## 5. Device verification

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

### Could NOT reach the Reschedule screen on device
It is only reachable from an **upcoming** appointment card in `AppointmentsScreen` → "تغییر زمان",
and **"گیتا" has zero bookings** on the live backend. Attempted (4th time this project) to create
one via the full booking flow — Home → رزرو نوبت → ROJAN AI Pilot Salon → Haircut → time 09:00 →
Confirmation → پرداخت در محل → تایید نهایی رزرو:

`docs/design-review/customer/RS_booking_409.png` — backend rejected it again with
**"این عملیات با وضعیت فعلی سازگار نیست."** (HTTP 409), on a fresh 09:00 slot. This is a backend
state-machine rejection (not slot contention — every slot 09:00–17:30 was still listed), the same
behaviour as the two prior sessions and the AppointmentDetails task. Nothing was created. No
navigation deep-links exist to reach the route otherwise.

### On-device evidence for the core visual
`docs/design-review/customer/RS_bookingtime_grid_ondevice.png` — captured mid-flow: the redesigned
**`BookingTimeScreen`** rendering its `RefSelectableCell` time grid on the A72. **This screen's time
picker reuses that exact primitive and cell shape** (`RefSelectableCell` + centred `Body` label,
flat → solid rose-gold on select), so the time grid is effectively device-verified.

### Verified-by-equivalence (states + shell) + correct-by-construction (Ready layout)
Every primitive the redesigned screen uses is already device-verified this session or in the
immediately prior tasks:
- `CustomerScaffold` (flat bar + `bottomBar` slot) — Auth, Profile, Appointments; the pinned-CTA
  `bottomBar` — ServiceDetails ("رزرو این خدمت", seen at `RS02`→`RS03`), BookingConfirmation
- `RefSelectableCell` — **on device this session** (BookingTime grid, `RS_bookingtime_grid_ondevice.png`)
- `CustomerLoadingState` / `CustomerErrorState` / `CustomerEmptyState` — verified on the Appointments
  screen this session
- `CustomerSectionLabel` — Profile, BookingTime
- `RefPrimaryButton` — ServiceDetails, Confirmation, Auth

The only genuinely-new composition is the **horizontal date-chip rail** (`RefSelectableCell` chips
in a `reverseLayout` `LazyRow`) — the same primitive, a new arrangement.

The `isSubmitting` guard (`enabled = selectedTime != null && !isSubmitting`) and every
`viewModel::…` call are unchanged from the pre-redesign code.

**Stop after this screen. No commit.**
