# Customer Appointments Screen — Redesign Review

**Date:** 2026-09-09 · **Scope:** `screens/profile/AppointmentsScreen.kt` (route `APPOINTMENTS`, "نوبت‌های من") only. No other screen touched.
**Not committed.**

---

## 1. File changed (1)

`screens/profile/AppointmentsScreen.kt` — rewritten, visual only. Public signature
(`onBackClick`, `onAppointmentClick`, `onRescheduleClick`, `onWaitlistClick`, `viewModel`,
`reminderViewModel`) is byte-identical.

**Behaviour preserved verbatim:**
- The list is still `BookingHistoryViewModel` over `GET /api/v1/bookings/my`; `viewModel.state` /
  `viewModel.retry()` are read/called in the same places.
- **Cancel** still runs `coroutineScope.launch { bookingRepository.cancelBooking(item.booking.id) }`
  then `viewModel.retry()` — identical.
- **Reschedule** still calls `onRescheduleClick(item.booking.id)`.
- **Appointment tap** still calls `onAppointmentClick(item.booking.id)`.
- The **reminder toggle** still drives `reminderViewModel.reminderPreferenceFor(id)` /
  `.setReminderPreference(...)` with the same 5 args.
- Upcoming = `PENDING`/`CONFIRMED`, past = `COMPLETED`/`CANCELLED` — same partition.

No ViewModel, repository, API, booking data model, navigation route, or cancel/reschedule logic
is touched.

## 2. Before → After

| | Before | After |
|---|---|---|
| Shell | `GlassBackButton` orb + bare `HeroTitle` in a `HomeBackgroundTheme` `Column` | `CustomerScaffold` (title "نوبت‌های من", flat 56dp bar, outlined back, hairline) |
| List | `LazyColumn`, section headers as plain `Body` text | `LazyColumn`, `CustomerSectionLabel("پیش‌رو")` / `("گذشته")`, `SpaceMD` gap between the two groups |
| Card | glass `PremiumCardShell` + `rojanEnterAnimation` stagger; a **48dp `salonAccentColorFor(salonId)` colour tile** with filled `Icons.Filled.Storefront` | flat `RefSurface`; a 40dp `CustomerSurfaceFill` tile with **outlined `Icons.Outlined.CalendarMonth`** in rose-gold; same salon / specialist / time column (byte-identical to the approved `HomeBookingRow`), outlined `Schedule` icon on the time line |
| Status | bare `Caption` text, `HomeColors.Gold` (or `TextSecondary` if cancelled) | a **tinted pill** — `Box(clip 6dp, bg = tint×14%)` + `Caption` in the tint. CONFIRMED → rose-gold `CustomerAccent`; PENDING → `TextMuted`; COMPLETED → `TextSecondary`; CANCELLED → `RojanErrorText` |
| Actions | "تغییر زمان" in **violet `HomeColors.Glow`**, "لغو نوبت" in `RojanErrorText`, both plain `clickable` `Caption` | a `RefRowDivider` then a row: "تغییر زمان" in rose-gold, "لغو نوبت" in `RojanErrorText`, both `Caption`/SemiBold with a `rojanPressable` + `Role.Button` |
| Cancel confirm | raw Material **`AlertDialog`** (light surface, LTR buttons) | **`CustomerConfirmDialog`** — flat navy card, hairline, RTL buttons (انصراف / لغو نوبت). Same `onConfirm` → the unchanged cancel lambda |
| Reminder toggle | `Switch` (default violet-ish M3 colours), plain label | `RefRowDivider` then a row: outlined `NotificationsNone` + "یادآوری نوبت", `Switch` tinted rose-gold (`checkedTrackColor = CustomerAccent`, `checkedThumbColor = CustomerOnAccent`) |
| Loading | glass `RojanLoadingState(message=…)` | `CustomerLoadingState(count = 5, rowHeight = 96)` — flat pulsing skeleton cards |
| Error | glass `RojanErrorState` card | `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })` |
| Empty | glass `RojanEmptyState` card | `CustomerEmptyState(title = "هنوز نوبتی ندارید", body = …, icon = Icons.Outlined.EventBusy)` |
| Motion | per-card enter stagger | none |

Removed imports: `PremiumCardShell`, `GlassBackButton`, `RojanEmptyState`/`RojanErrorState`/`RojanLoadingState`,
`HomeBackgroundTheme`, `AlertDialog`, `TextButton`, `Icons.Filled.Storefront`, `salonAccentColorFor`,
`RojanShapes`, `clickable`, `itemsIndexed`.

## 3. Foundation usage

`CustomerScaffold` · `CustomerSectionLabel` · `RefSurface` · `RefRowDivider` · `CustomerLoadingState` ·
`CustomerErrorState` · `CustomerEmptyState` · `CustomerConfirmDialog` · tokens `CustomerAccent` /
`CustomerOnAccent` / `CustomerScreenMargin` / `CustomerButtonRadius` / `CustomerSurfaceFill` /
`CustomerHairline` · `rojanPressable`.

**Screen-local:** `AppointmentCard`, `StatusPill`, `CardAction`, `BookingStatus.label()` /
`BookingStatus.tint` — composition helpers over the foundation, no new tokens. The card body
deliberately mirrors the already-approved `HomeBookingRow` (`CustomerDashboardScreen`) so the two
places that render a booking look identical.

**Date formatting:** `startTime.substringBefore('T') + "  ·  " + startTime.substringAfter('T').take(5)`
— copied verbatim from `HomeBookingRow`; pure display, no date maths, `RollingBookingDates` untouched.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | **Installed on 1 device**, exit 0 (device dropped ADB after packaging; reconnected, re-ran clean) |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `AppointmentsScreen.kt` |
| Samsung A72 verification | **PASS** (see §5) |

## 5. Device verification

Reached via Profile → "نوبت‌های من" (route `APPOINTMENTS`), signed in as "گیتا".

| Screenshot | State | What it shows |
|---|---|---|
| `docs/design-review/customer/AP_after_empty.png` | **Empty** (گیتا has no bookings) | flat "نوبت‌های من" top bar + hairline; `CustomerEmptyState` — outlined `EventBusy`, "هنوز نوبتی ندارید" + "برای رزرو نوبت جدید…" centred. No glass card, no glow. |
| `docs/design-review/customer/AP_after_error.png` | **Error** (airplane mode on) | `CustomerErrorState` — outlined `CloudOff`, "مشکلی پیش آمد", "اتصال اینترنت برقرار نیست…", solid rose-gold "تلاش مجدد". |
| `docs/design-review/customer/AP_after_loading.png` | **Loading** (caught after "تلاش مجدد", network restored) | `CustomerLoadingState` — 5 flat pulsing skeleton cards, 14dp radius + hairline, on the screen margin. |

**"تلاش مجدد" → reload → back to the empty state** with the network restored — `viewModel.retry()`
works, no regression, no crash.

**Populated list not device-verifiable this session:** "گیتا" has zero bookings on the live
backend and creating a real one against the pilot salon is out of scope (and was rejected 409 in
earlier sessions). The populated **card**, **status pill**, **action row**, and **cancel dialog**
are correct-by-construction:
- the card body is the same `RefSurface` + 40dp-tile + salon/specialist/time layout as
  `HomeBookingRow`, which **was** device-verified in the Home redesign (with real bookings);
- `RefRowDivider` / `RefSurface` are verified across Salon Detail and the booking flow;
- `CustomerConfirmDialog` was device-verified in the Profile task (same component, different copy);
- `StatusPill` is a trivial tinted `Box` + `Text`.

## 6. Out of scope (flagged for follow-up)

`AppointmentDetailsScreen.kt` and `RescheduleAppointmentScreen.kt` still use `GlassBackButton` /
`HomeGlassSurface` / `PremiumButton` / `RojanComingSoonState` / `RojanLoadingState` etc. They are
separate routes (`APPOINTMENT_DETAILS`, `RESCHEDULE_APPOINTMENT`), not part of "the Appointments
screen", and are listed in `CUSTOMER-VISUAL-DEBT-AUDIT.md` as their own items. Not touched here.

**Stop after Appointments. No commit.**
