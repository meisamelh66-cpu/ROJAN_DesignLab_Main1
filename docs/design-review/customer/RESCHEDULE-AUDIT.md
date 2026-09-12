# RescheduleAppointmentScreen — Visual Debt Audit

**Date:** 2026-09-09 · **Type:** READ ONLY. No source modified. Awaiting approval before any redesign.
**Screen:** `screens/profile/RescheduleAppointmentScreen.kt` · route `RESCHEDULE_APPOINTMENT`
(`RojanDestinations.rescheduleAppointment(appointmentId)`) · title "تغییر زمان نوبت".
**Reached from:** `AppointmentsScreen` → an upcoming card's "تغییر زمان" action
(`onRescheduleClick(booking.id)`); wrapped in `CustomerAccessGuard`.

**Reference language (target):** dark navy · `CustomerScaffold` flat 56dp bar · rose-gold `#E0A67A`
accent · flat `RefSurface` / `RefSelectableCell` (4.5% fill + 1px 9% hairline; solid rose-gold when
selected) · 14dp card / 12dp button radius · outlined icons · minimal hierarchy · `CustomerStates` ·
no glass / sparkle / glow / violet / magenta / gradient. Established by Salon Detail (golden), Home,
Profile, Appointments, and the whole booking flow — **`BookingDateScreen` (step 3) and
`BookingTimeScreen` (step 4) are the direct precedent: this screen is their date-chips + time-grid,
merged onto one page.**

---

## 1. What the screen does (unchanged by any redesign)

Pick a new date + time for an existing booking and submit `PUT /bookings/{id}/reschedule`.
`RescheduleViewModel` loads the booking first (for salon/specialist/service ids), then drives the
same real `available-slots` calls the new-booking flow uses.

- **`viewModel.state`** (`RescheduleUiState`): `Loading` / `Error(message)` + `retry()` /
  `Ready(dates, selectedDate, slots: UiState<List<TimeSlot>>, selectedTime, isSubmitting, submitError)`.
- **`dates`** = `RollingBookingDates.next7Days()` → `List<Pair<isoKey, label>>` where `label` is
  already `"امروز"` / `"فردا"` / `"{weekday}، {persianDay} {persianMonth}"`.
- **Callbacks into the VM:** `viewModel::selectDate(isoKey)`, `viewModel::selectTime("HH:mm")`,
  `viewModel.confirm(onRescheduled)`, `viewModel.retry()`, and (for a slots error)
  `viewModel.selectDate(state.selectedDate)`.
- **`TimeSlot.timeLabel()`** = `start.substringAfter('T').take(5)` → bare `"HH:mm"` (the shape the
  callback + `confirm()`'s `"${date}T$time:00"` both expect).

**Must stay untouched:** `RescheduleViewModel` / its factory / `bookingRepository` /
`availabilityRepository`, the `appointmentId` arg, `onBackClick`, `onRescheduled`, every
`viewModel::…` reference, the `"HH:mm"` contract of `timeLabel()` / `selectTime`, and the public
`RescheduleAppointmentScreen(...)` signature.

---

## 2. Visual debt — findings

| # | Sev | Element | Current | Problem |
|---|---|---|---|---|
| R-1 | **P0** | Back control | `GlassBackButton(onClick)` — the floating translucent **orb** + glow | Most-flagged token app-wide; every approved screen uses the `CustomerScaffold` flat bar. |
| R-2 | **P0** | Screen shell | `HomeBackgroundTheme { Column(padding SpaceMD) { GlassBackButton() ; Text("تغییر زمان نوبت", HeroTitle 32sp) ; when(state) } }` | Ad-hoc. Not the app shell. 32sp hero title is out of scale (titles now live in the 56dp bar). Margin should be `CustomerScreenMargin` (20dp). |
| R-3 | **P0** | Date chips | `HomeGlassSurface(shape = Small)` per chip — glass + `premiumMetallicBorder` + ✦ `drawSparkle` corners + glow; selected = **violet `HomeColors.Glow`** text only, no fill | Glass + sparkle + metallic border are P0. No real selected *state* — just a text-colour swap in violet. Must be `RefSelectableCell` (flat → solid rose-gold on select). |
| R-4 | **P0** | Time cells | `HomeGlassSurface(shape = Small)` + raw `Modifier.clickable`; selected = **violet `HomeColors.Glow`** text + `HomeColors.Glow.copy(alpha = 0.12f)` wash; unselected wash references **`HomeColors.Primary`** (violet `#7C4DFF`, at alpha 0 — a dead reference to the violet token) | Glass + violet selected state + a leftover violet-token reference. `BookingTimeScreen` already solved this exact grid with `RefSelectableCell`. |
| R-5 | **P0** | Submit error | `Text(it, color = HomeColors.Magenta)` — **vivid magenta** | `HomeColors.Magenta` is explicitly on the removal list. Use `RojanErrorText` (the muted rose-red token), as every redesigned screen does. |
| R-6 | **P0** | Confirm button | `PremiumButton(text = "تایید زمان جدید", enabled, loading)` — magenta→pink **gradient pill**, white top-sheen, built-in spinner | Gradient button is P0. Replace with solid rose-gold `RefPrimaryButton`. |
| R-7 | **P1** | Loading (booking) | `RojanLoadingState(message = "در حال بارگذاری نوبت...")` — glass card + filled icon | → `CustomerLoadingState`. |
| R-8 | **P1** | Error (booking) | `RojanErrorState(description, actionLabel, onAction)` — glass card | → `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })`. |
| R-9 | **P1** | Loading (slots) | `RojanLoadingState(message = "در حال بررسی زمان‌های خالی...")` glass card | → `CustomerLoadingState(count = 4, rowHeight = 52)` (matches `BookingTimeScreen`). |
| R-10 | **P1** | Error (slots) | `RojanErrorState(...)` glass card | → `CustomerErrorState(message = slots.message, onRetry = onRetrySlots)`. |
| R-11 | **P1** | Empty (slots) | `RojanEmptyState(title = "زمانی برای این تاریخ موجود نیست")` glass card, no body, no action | → `CustomerEmptyState(title = "زمانی برای این تاریخ موجود نیست", body = "لطفاً تاریخ دیگری را انتخاب کنید.", icon = Icons.Outlined.EventBusy)` (matches `BookingTimeScreen`'s empty). |
| R-12 | **P1** | Section headings | `Text("انتخاب تاریخ" / "انتخاب ساعت", style = Body, color = TextPrimary)` — plain body text | → `CustomerSectionLabel(...)` (muted `Caption`/SemiBold), the pattern every redesigned list uses. |
| R-13 | **P1** | Confirm placement | `PremiumButton` sits inline at the bottom of a scrollable `Column`; can scroll out of reach | Pin it in `CustomerScaffold`'s `bottomBar` slot (like `ServiceDetailsScreen` / `BookingConfirmationScreen`). |
| R-14 | **P2** | Date chip content | one-line `label` (`"دوشنبه، ۱۴ سپتامبر"`) at `Caption` | `BookingDateScreen`'s `DateCell` splits weekday / date into a two-line hierarchy. Optional: split `label` on `"،"` for the same. Low priority — the chip rail is horizontal and compact. |
| R-15 | **P2** | Layout / RTL | `LazyRow` chips + grid are LTR-ordered; headings default-start | Reference cells are RTL-anchored. `RefSelectableCell` centres its content, so the grid is fine; the chip rail should read right-to-left (reverse layout or `reverseLayout = true` on the `LazyRow`). |
| R-16 | **P2** | `loading` affordance | `PremiumButton(loading = state.isSubmitting)` shows an inline spinner | `RefPrimaryButton` has no loading param. Convey `isSubmitting` via `enabled = … && !isSubmitting` (the button already dims to 40% when disabled) + optionally swap the label to `"در حال ثبت..."` while submitting. |

**No** `PremiumBackground`, `HeroBookingCard`, `AISearchBar`, or literal `✦` on this screen. The
debt is: the orb, N glass chips, N glass time cells (with a violet selected state + a dead violet
token ref), magenta error text, a gradient button, and four glass state views.

---

## 3. Proposed before → after

### Shell
```
val ready = viewModel.state as? RescheduleUiState.Ready
CustomerScaffold(
    title = "تغییر زمان نوبت",
    onBackClick = onBackClick,
    bottomBar = if (ready != null) {
        {
            ready.submitError?.let { Text(it, Caption, RojanErrorText) ; Spacer(SpaceSM) }
            RefPrimaryButton(
                label = if (ready.isSubmitting) "در حال ثبت..." else "تایید زمان جدید",
                onClick = { viewModel.confirm(onRescheduled) },
                enabled = ready.selectedTime != null && !ready.isSubmitting,
            )
        }
    } else null,
) {
    when (val state = viewModel.state) {
        Loading -> CustomerLoadingState(Modifier.padding(top = SpaceLG))
        Error   -> CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })
        Ready   -> RescheduleContent(state, viewModel::selectDate, viewModel::selectTime,
                                     onRetrySlots = { viewModel.selectDate(state.selectedDate) })
    }
}
```

### `RescheduleContent` (a `Column`, cards on `CustomerScreenMargin`)

**Date** — `CustomerSectionLabel("انتخاب تاریخ")` + `Spacer(SpaceSM)` + a
`LazyRow(contentPadding = horizontal CustomerScreenMargin, spacedBy SpaceSM, reverseLayout = true)`:
```
items(state.dates, key = { it.first }) { (key, label) ->
    RefSelectableCell(
        selected = key == state.selectedDate,
        onClick = { onDateSelected(key) },
    ) { contentColor ->
        Text(label, style = Caption, color = contentColor, textAlign = Center)   // or split on "،" for 2 lines (R-14)
    }
}
```

**Time** — `Spacer(SpaceLG)` + `CustomerSectionLabel("انتخاب ساعت")` + `Spacer(SpaceSM)` +
`when (state.slots)`:
- `Loading` → `CustomerLoadingState(count = 4, rowHeight = 52)`
- `Error` → `CustomerErrorState(message = slots.message, onRetry = onRetrySlots)`
- `Empty` → `CustomerEmptyState(title = "زمانی برای این تاریخ موجود نیست", body = "لطفاً تاریخ دیگری را انتخاب کنید.", icon = Icons.Outlined.EventBusy)`
- `Success` → `LazyVerticalGrid(GridCells.Fixed(3), contentPadding horizontal CustomerScreenMargin + bottom SpaceXL, spacedBy SpaceSM, Modifier.weight(1f))`:
```
items(slots.data, key = { it.start }) { slot ->
    val label = slot.timeLabel()
    RefSelectableCell(
        selected = label == state.selectedTime,
        onClick = { onTimeSelected(label) },
        modifier = Modifier.fillMaxWidth(),
    ) { c -> Text(label, style = Body, color = c, textAlign = Center) }
}
```
This is `BookingTimeScreen`'s `TimeChip` verbatim.

**Confirm + submit error** → moved to the scaffold `bottomBar` (R-13). The inline `PremiumButton`
and the inline magenta error `Text` are removed from the content.

---

## 4. Reusable components from the Customer foundation

| Component (`screens/customer/components/`) | Use here |
|---|---|
| `CustomerScaffold(title, onBackClick, bottomBar)` | shell + pinned confirm |
| `RefSelectableCell(selected, onClick, modifier, content)` | date chips **and** time cells — one primitive, flat → solid rose-gold |
| `CustomerSectionLabel(text)` | "انتخاب تاریخ" / "انتخاب ساعت" |
| `CustomerLoadingState(count, rowHeight)` | booking-load + slots-load skeletons |
| `CustomerErrorState(message, onRetry)` | booking-load error + slots error |
| `CustomerEmptyState(title, body, icon)` | no-slots state |
| `RefPrimaryButton(label, onClick, enabled)` | "تایید زمان جدید" (disabled while submitting; no gradient/spinner) |
| tokens `CustomerAccent` / `CustomerScreenMargin` / `CustomerCardShape` | spacing + accent |
| `RojanErrorText` (`ui/theme`) | submit-error line (replaces `HomeColors.Magenta`) |
| `RollingBookingDates` label (already in `state.dates`) | chip text — no `dateParts` re-derivation needed (unlike `BookingDateScreen`, the label is pre-built here) |

**Not in the foundation (screen-local, small):** `RescheduleContent` and — only if R-14 is taken —
a 2-line date-chip helper (split `label` on `"،"`). `BookingDateScreen`'s `dateParts` / `DateParts`
/ `toPersianDigits` are `private` to that file and **not needed** here.

**Note:** this screen sits in `screens/profile/`, so it should import from
`screens.customer.components` directly (as `AppointmentsScreen` / `AppointmentDetailsScreen` do),
**not** the `screens.bookingflow.components` forwarders.

---

## 5. Estimated change surface

One file: `screens/profile/RescheduleAppointmentScreen.kt` — full rewrite of the two composable
bodies; signature untouched. Imports removed: `HomeBackgroundTheme`, `HomeGlassSurface`,
`GlassBackButton`, `PremiumButton`, `RojanEmptyState` / `RojanErrorState` / `RojanLoadingState`,
`RojanShapes`, `clickable`, `background`, `HomeColors.Glow` / `.Primary` / `.Magenta` usages.

**No** ViewModel, repository, API, navigation, or callback change. `TimeSlot.timeLabel()` stays.

**Awaiting approval before writing code.**
