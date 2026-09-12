# AppointmentDetailsScreen — Visual Debt Audit

**Date:** 2026-09-09 · **Type:** READ ONLY. No source modified. Awaiting approval before any redesign.
**Screen:** `screens/profile/AppointmentDetailsScreen.kt` · route `APPOINTMENT_DETAILS`
(`RojanDestinations.appointmentDetails(appointmentId)`) · title "جزئیات نوبت".
**Reached from:** `AppointmentsScreen` → tap a card (`onAppointmentClick(booking.id)`).

**Reference language (target):** dark navy · `CustomerScaffold` flat 56dp bar · rose-gold `#E0A67A`
accent · flat `RefSurface` (4.5% white fill + 1px 9% hairline) · 14dp card / 12dp button radius ·
outlined icons · minimal hierarchy · `CustomerLoadingState` / `CustomerErrorState` /
`CustomerEmptyState` · no glass / sparkle / glow / violet / gradient. Established by Salon Detail
(golden), Home, Explore, Auth, Profile, Appointments, and the whole booking flow.

---

## 1. What the screen does (unchanged by any redesign)

View-only detail for **one** booking, loaded from the real backend by
`AppointmentDetailsViewModel` (`GET /api/v1/bookings/{id}` + salon/specialist/service resolution).
Renders four blocks:

1. **Header card** — salon name, `serviceName • specialistName`, date-time.
2. **Invoice card ("رسید و فاکتور")** — service, price, status, tracking number (`booking.id`).
3. **"رزرو مجدد" button** — only when `serviceName != null`; calls
   `onRebookClick(booking.serviceId, booking.salonId)`.
4. **A trailing `RojanComingSoonState()`** — a "به‌زودی" placeholder card with no data behind it.

`state`: `Loading` / `Error(message)` + `retry()` / `Empty` / `Success(AppointmentDetailsData)`.

**Must stay untouched:** `AppointmentDetailsViewModel` / its factory / all 5 repositories, the
`appointmentId` arg, `onBackClick`, `onRebookClick(serviceId, salonId)` (both args, in that order),
`viewModel.state` / `viewModel.retry()`, the `serviceName != null` gate on rebook, and the public
`AppointmentDetailsScreen(...)` signature.

---

## 2. Visual debt — findings

| # | Sev | Element | Current | Problem |
|---|---|---|---|---|
| D-1 | **P0** | Back control | `GlassBackButton(onClick)` — the floating translucent **orb** with a 7-pass glow | The single most-flagged debt token app-wide; every approved screen replaced it with the `CustomerScaffold` flat 56dp top bar. |
| D-2 | **P0** | Screen shell | `HomeBackgroundTheme { Column(padding(SpaceMD)) { … } }` — ad-hoc, orb + bare `HeroTitle` "جزئیات نوبت" (32sp) floating above the list, no bar, no hairline, `SpaceMD` (16dp) margin | Not the app shell. Title as a 32sp hero is out of scale with the reference (screen titles now sit in the 56dp bar at `Body` weight, or as a 26sp `Display` sub-head). Margin should be `CustomerScreenMargin` (20dp). |
| D-3 | **P0** | Header card | `HomeGlassSurface(shape = RojanShapes.GlassCard)` — glass fill + `premiumMetallicBorder` + 4-point ✦ `drawSparkle` corners + multi-pass glow | Glass + sparkle + metallic border are all on the P0 removal list. Must be a flat `RefSurface`. |
| D-4 | **P0** | Invoice card | second `HomeGlassSurface(shape = RojanShapes.Small)` | Same as D-3. |
| D-5 | **P0** | Invoice header icon | `Icons.Filled.Receipt` tinted **`HomeColors.Glow`** (violet `#7C4DFF`) | Filled icon + violet. Reference uses outlined icons and rose-gold as the only accent. |
| D-6 | **P0** | "رزرو مجدد" button | `PremiumButton(text=…)` — magenta→pink **gradient pill**, 50dp, white top-sheen | Gradient button is on the P0 removal list. Must be a solid rose-gold `RefPrimaryButton`. |
| D-7 | **P1** | Loading state | `RojanLoadingState(message = "در حال بارگذاری...")` — glass `RojanStateCard` + filled icon | Replace with `CustomerLoadingState` (flat pulsing skeleton cards). |
| D-8 | **P1** | Error state | `RojanErrorState(description, actionLabel, onAction)` — glass card | Replace with `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })`. |
| D-9 | **P1** | Empty state | bare `Text("نوبت یافت نشد")` — no icon, no structure, left-floating under the hero title | Replace with `CustomerEmptyState(title = "نوبت یافت نشد", …, icon = Icons.Outlined.SearchOff)`. |
| D-10 | **P1** | Status value | `InvoiceRow("وضعیت", booking.status.name)` — renders the **raw enum**: `PENDING` / `CONFIRMED` / `COMPLETED` / `CANCELLED` in Latin caps | User-facing raw enum. `AppointmentsScreen` already has the Persian mapping ("در انتظار تایید" / "تایید شده" / "انجام شده" / "لغو شده") + a status tint. Detail screen should show the same Persian label, ideally as the same tinted pill. |
| D-11 | **P1** | Date-time | `booking.startTime.replace('T', ' ')` → `2026-09-15 14:30:00` (keeps the seconds) | The approved format (`HomeBookingRow`, and the new `AppointmentCard`) is `substringBefore('T') + "  ·  " + substringAfter('T').take(5)` → `2026-09-15  ·  14:30`. |
| D-12 | **P1** | "به‌زودی" card | trailing `RojanComingSoonState()` | A glass "coming soon" placeholder for nothing. The doc comment already records that reviews + photos sections were **deliberately removed** as fake — this leftover placeholder implies more is coming when nothing is. Recommend **removing it** (no data, no purpose). |
| D-13 | **P2** | Price format | `"${it.toInt()} تومان"` | Fine, but the reference emphasises price in rose-gold (`BookingConfirmationScreen.PriceRow`, `ServiceDetailsScreen.MetaCell`). Worth making the "مبلغ" row's value rose-gold for the one-glance "what did this cost". |
| D-14 | **P2** | Layout / RTL | invoice rows are `Row(SpaceBetween)` label-left / value-right; header texts are default-start | Reference rows are RTL-anchored (`RefListRow`: value + chevron left, label right). Adopt `RefListRow(showChevron = false, trailingValue = …)` or the confirmation screen's `SummaryRow` shape for consistency. |
| D-15 | **P2** | "رسید و فاکتور" section | an icon + label inside the card, above the rows | Reference pattern is a `CustomerSectionLabel("…")` *above* a `RefSurface`, not an in-card header row with an icon. |

**No sparkle literals, no `HomeColors.Magenta`, no `PremiumBackground`, no `HeroBookingCard`** on
this screen — the debt is entirely: the orb, two glass surfaces, one violet filled icon, one
gradient button, three glass state views, and two raw-data formatting misses.

---

## 3. Proposed before → after

### Shell
| Before | After |
|---|---|
| `HomeBackgroundTheme { Column(padding(SpaceMD)) { GlassBackButton(); Text("جزئیات نوبت", HeroTitle); when(state){…} } }` | `CustomerScaffold(title = "جزئیات نوبت", onBackClick = onBackClick, bottomBar = rebook?) { when(state){…} }` |

- Loading → `CustomerLoadingState(count = 3, rowHeight = 92, modifier = Modifier.padding(top = SpaceLG))`
- Error → `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })`
- Empty → `CustomerEmptyState(title = "نوبت یافت نشد", body = "این نوبت در دسترس نیست.", icon = Icons.Outlined.SearchOff)`
- Success → `AppointmentDetailsContent(...)`

### Success content (a `Column(verticalScroll)` + `PaddingValues(vertical = SpaceLG)`, cards on `CustomerScreenMargin`)

**Header** — `RefSurface { Column(padding SpaceMD) }`:
- salon name → `RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp)`, `TextPrimary`
- `Spacer(SpaceXS)`, `serviceName • specialistName` → `Body`, `TextSecondary`
- `Spacer(SpaceXS)`, a row: outlined `Icons.Outlined.Schedule` 14dp `TextMuted` + `Caption`
  `substringBefore('T') + "  ·  " + substringAfter('T').take(5)` `TextMuted`
- status → the **same tinted pill** as `AppointmentsScreen.StatusPill` (promote `StatusPill` +
  `BookingStatus.label()` + `.tint` from `AppointmentsScreen.kt` into `customer/components/`, or
  duplicate the tiny helper — recommend promoting so the list card and the detail agree).

**Invoice** — `CustomerSectionLabel("رسید و فاکتور")` + `Spacer(SpaceSM)` + `RefSurface { Column }`:
- `serviceName?.let { DetailRow("خدمت", it) }` + `RefRowDivider()`
- `servicePrice?.let { DetailRow("مبلغ", "${it.toInt()} تومان", valueColor = CustomerAccent) }` + `RefRowDivider()`
- `DetailRow("وضعیت", booking.status.label())` + `RefRowDivider()`
- `DetailRow("شماره پیگیری", booking.id, valueColor = TextMuted)`
- `DetailRow` = a flat RTL row (`RefListRow(showChevron = false, trailingValue = label)` with the
  value right-anchored, or the confirmation screen's `SummaryRow` without the edit chevron).
  The invoice `Receipt` icon + in-card header row is dropped (D-15).

**Rebook** — pinned in `CustomerScaffold`'s `bottomBar` slot when `data.serviceName != null`:
`RefPrimaryButton(label = "رزرو مجدد", onClick = { onRebookClick(booking.serviceId, booking.salonId) })`.
When `serviceName == null`, `bottomBar = null` (button simply absent, exactly as today).

**Remove** the trailing `RojanComingSoonState()` (D-12).

### Foundation to use
`CustomerScaffold` · `CustomerSectionLabel` · `RefSurface` · `RefRowDivider` · `RefPrimaryButton` ·
`CustomerLoadingState` / `CustomerErrorState` / `CustomerEmptyState` · tokens `CustomerAccent` /
`CustomerScreenMargin` / `CustomerCardShape` · `RojanTypography` / `RojanDimens` / `HomeColors`.

### Open question for approval
- **Promote `StatusPill` + `BookingStatus.label()` + `BookingStatus.tint`** from
  `AppointmentsScreen.kt` into `screens/customer/components/` (new small file, e.g.
  `CustomerBookingStatus.kt`) so both screens share one status display? This edits
  `AppointmentsScreen.kt` (swap its private helpers for the promoted ones — no visual change) in
  addition to the detail screen. If that's out of scope for a "detail screen only" task, the
  fallback is to duplicate the ~12-line helper locally.

---

## 4. Estimated change surface

One file: `screens/profile/AppointmentDetailsScreen.kt` (full rewrite of the composable bodies,
signature untouched). Optionally `AppointmentsScreen.kt` + one new `customer/components` file **iff**
the status-pill promotion is approved. Imports removed: `HomeBackgroundTheme`, `HomeGlassSurface`,
`GlassBackButton`, `PremiumButton`, `RojanComingSoonState`, `RojanErrorState`, `RojanLoadingState`,
`RojanShapes`, `Icons.Filled.Receipt`.

**No** ViewModel, repository, API, navigation, or callback change.

**Awaiting approval before writing code.**
