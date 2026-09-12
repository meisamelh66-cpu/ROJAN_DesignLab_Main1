# Appointment Details Screen — Redesign Review

**Date:** 2026-09-09 · **Scope:** `screens/profile/AppointmentDetailsScreen.kt` (route
`APPOINTMENT_DETAILS`, "جزئیات نوبت") + a no-visual-change dedup of the booking-status helpers.
**Not committed.**

---

## 1. Files changed (3)

### a. `screens/customer/components/CustomerBookingStatus.kt` — **NEW** (shared foundation)

Moved verbatim out of `AppointmentsScreen.kt`:
- `fun BookingStatus.label()` — Persian label ("در انتظار تایید" / "تایید شده" / "انجام شده" / "لغو شده"), now **public**.
- `val BookingStatus.tint` `@Composable` — CONFIRMED → `CustomerAccent` (rose-gold), PENDING → `TextMuted`, COMPLETED → `TextSecondary`, CANCELLED → `RojanErrorText`. Now **public**.
- `@Composable fun StatusPill(status, modifier = Modifier)` — flat pill: label on a 14%-opacity wash of its own tint, 6dp corners. Byte-identical body; gained a `modifier` param (defaulted, so existing call sites are unaffected).

### b. `screens/profile/AppointmentsScreen.kt` — **dedup only, output identical**

- Deleted the local `private fun BookingStatus.label()`, `private val BookingStatus.tint`, and `private fun StatusPill(...)`.
- Added `import ai.rojan.designlab.screens.customer.components.StatusPill`.
- The one call site — `StatusPill(booking.status)` — now resolves to the shared component. Same pixels: the moved code is character-for-character the same.
- No other change. Nothing else in the file was touched.

### c. `screens/profile/AppointmentDetailsScreen.kt` — **redesigned, visual only**

Public signature (`appointmentId`, `onBackClick`, `onRebookClick(serviceId, salonId)`, `viewModel`)
byte-identical.

**Behaviour preserved verbatim:**
- `AppointmentDetailsViewModel` + its factory + all 5 repositories — untouched; `viewModel.state`
  and `viewModel.retry()` read/called in the same places.
- The rebook button still appears **only** when `data.serviceName != null` and still calls
  `onRebookClick(data.booking.serviceId, data.booking.salonId)` — same two args, same order.
- The reviews / photos sections stay removed (no backend counterpart), as before.

No ViewModel, repository, API, model, navigation route, or callback is touched.

## 2. Before → After (screen c)

| | Before | After |
|---|---|---|
| Shell | `HomeBackgroundTheme { Column(padding SpaceMD) { GlassBackButton() ; Text("جزئیات نوبت", HeroTitle 32sp) ; when(state) } }` | `CustomerScaffold(title = "جزئیات نوبت", onBackClick, bottomBar = rebook?)` — flat 56dp bar, outlined back, hairline |
| Header card | `HomeGlassSurface(shape = GlassCard)` — glass + metallic border + ✦ sparkle corners + glow | flat `RefSurface`; salon name in `Display` 26sp; `serviceName • specialistName` in `Body`/`TextSecondary`; outlined `Schedule` 14dp + `Caption` time; then `StatusPill` |
| Date-time | `startTime.replace('T', ' ')` → `2026-09-15 14:30:00` | `substringBefore('T') + "  ·  " + substringAfter('T').take(5)` → `2026-09-15  ·  14:30` (matches `HomeBookingRow` / the list card) |
| Status | `InvoiceRow("وضعیت", booking.status.name)` → raw enum `PENDING` | `DetailRow("وضعیت", booking.status.label())` → "تایید شده" etc. + the tinted `StatusPill` in the header |
| Invoice card | second `HomeGlassSurface(shape = Small)` with an in-card `Icons.Filled.Receipt` (violet `HomeColors.Glow`) + label row | `CustomerSectionLabel("رسید و فاکتور")` above a flat `RefSurface` of divided `DetailRow`s — no icon, no violet |
| Price | `InvoiceRow("مبلغ", "… تومان")` plain | `DetailRow("مبلغ", "… تومان", valueColor = CustomerAccent)` — rose-gold emphasis |
| Tracking no. | `InvoiceRow("شماره پیگیری", booking.id)` | `DetailRow("شماره پیگیری", booking.id, valueColor = TextMuted)` |
| Rebook CTA | `PremiumButton("رزرو مجدد")` — magenta→pink gradient pill, inline in the list | solid rose-gold `RefPrimaryButton("رزرو مجدد")` pinned in the scaffold `bottomBar`; absent (`bottomBar = null`) when `serviceName == null` — same gate |
| Loading | `RojanLoadingState(message=…)` glass card | `CustomerLoadingState(count = 3, rowHeight = 92)` flat skeletons |
| Error | `RojanErrorState(...)` glass card | `CustomerErrorState(message = state.message, onRetry = { viewModel.retry() })` |
| Empty | bare `Text("نوبت یافت نشد")` | `CustomerEmptyState(title = "نوبت یافت نشد", body = "این نوبت در دسترس نیست.", icon = Icons.Outlined.SearchOff)` |
| Trailing placeholder | `RojanComingSoonState()` — glass "به‌زودی" card for nothing | **removed** |

Removed imports: `HomeBackgroundTheme`, `HomeGlassSurface`, `GlassBackButton`, `PremiumButton`,
`RojanComingSoonState`, `RojanErrorState`, `RojanLoadingState`, `RojanShapes`, `Icons.Filled.Receipt`,
`Arrangement`, `LazyColumn`.

## 3. Foundation usage

`CustomerScaffold` · `CustomerSectionLabel` · `RefSurface` · `RefRowDivider` · `RefPrimaryButton` ·
`CustomerLoadingState` / `CustomerErrorState` / `CustomerEmptyState` · shared `StatusPill` /
`BookingStatus.label()` · tokens `CustomerAccent` / `CustomerScreenMargin` · `RojanTypography` /
`RojanDimens` / `HomeColors`.

**Screen-local:** `AppointmentDetailsContent`, `DetailRow` — thin composition over the foundation,
no new tokens.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | **Installed on 1 device**, exit 0 (device dropped ADB after packaging; reconnected, re-ran clean) |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `AppointmentDetailsScreen.kt` and `CustomerBookingStatus.kt` |
| Samsung A72 verification | **PARTIAL** — see §5 |

## 5. Device verification

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

### Dedup regression check — PASS
`docs/design-review/customer/AD_appts_no_regression.png` — `AppointmentsScreen` after the
`StatusPill` dedup still renders its empty state (`CustomerEmptyState`, "هنوز نوبتی ندارید")
identically. The screen builds and displays with no change; the moved code is byte-identical.

### AppointmentDetailsScreen — could NOT reach a populated screen on device
The detail screen is only reachable by tapping an appointment card in `AppointmentsScreen`, and
**"گیتا" has zero bookings** on the live backend. To create one I walked the full booking flow
(Home → رزرو نوبت → ROJAN AI Pilot Salon → Haircut → auto-skipped specialist/date → time 15:15 →
Confirmation → پرداخت در محل → تایید نهایی رزرو):

`docs/design-review/customer/AD_booking_409.png` — the backend rejected it with
**"این عملیات با وضعیت فعلی سازگار نیست."** (HTTP 409). Nothing was created. This is the **same
backend behaviour observed in the two prior sessions** (the pilot salon is Monday-only and its
slots are taken) — not a code issue, and unrelated to this redesign. There are no navigation
deep-links, so the route can't be reached another way.

### Correct-by-construction (Success path) + verified-by-equivalence (states + shell)
Every visual primitive the redesigned screen uses was device-verified this session or in the
immediately prior tasks:
- `CustomerScaffold` (flat bar, `bottomBar` CTA slot) — Auth, Profile, Appointments, ServiceDetails
- `CustomerLoadingState` / `CustomerErrorState` / `CustomerEmptyState` — **verified on the
  Appointments screen this session** (identical call shapes: skeleton rows, `CloudOff` + retry,
  `SearchOff` empty)
- `RefSurface` / `RefRowDivider` / `CustomerSectionLabel` — Salon Detail, Profile, Appointments
- `RefPrimaryButton` pinned in `bottomBar` — ServiceDetails ("رزرو این خدمت"), Confirmation, Auth
- `StatusPill` — the shared component; its wash/label/tint is a trivial `Box`+`Text`
- `DetailRow` — new, a `Row` + two `Text`

The rebook gate (`data.serviceName != null` → button present, else `bottomBar = null`) and the
`onRebookClick(booking.serviceId, booking.salonId)` call are unchanged from the pre-redesign code.

**Stop after this screen. No commit.**
