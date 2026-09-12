# Customer Design Foundation — Migration

**Date:** 2026-09-09 · **Scope:** infrastructure only — no screen redesigned, no ViewModel / repository / navigation route / API contract touched. **Not committed.**
**Goal:** a single reusable Customer UI foundation based on the approved Quiet Luxury reference, replacing the booking-only naming.

---

## 1. What was created

New package: **`ai.rojan.designlab.screens.customer.components`** (`app/src/main/java/ai/rojan/designlab/screens/customer/components/`)

| File | Public API |
|---|---|
| `CustomerRefComponents.kt` | **Tokens:** `CustomerScreenMargin` 20dp · `CustomerCardRadius` 14dp · `CustomerButtonRadius` 12dp · `CustomerButtonHeight` 52dp · `CustomerTopBarHeight` 56dp · `CustomerAccent` `#E0A67A` · `CustomerOnAccent` `#1B1530` · `CustomerSurfaceFill` white 4.5% · `CustomerHairline` white 9% · `CustomerDivider` white 7% · `CustomerCardShape` · `CustomerSectionLabelStyle`. **Composables:** `CustomerSectionLabel` · `RefSurface` · `RefRowDivider` · `RefListRow` · `RefSelectableCell` · `RefPrimaryButton` (+ `internal CustomerSkeletonRows`). |
| `CustomerScaffold.kt` | **`CustomerScaffold(title, onBackClick, modifier, step? , totalSteps, bottomBar?, content)`** — flat 56dp top bar (outlined `ArrowBack` on the margin, RTL-centred `Body` title, 1px hairline; no orb), optional `CustomerStepIndicator`, full-width content slot, optional pinned bottom-bar. Owns its window insets; applies `HomeBackgroundTheme(applyContentInsets = false)`. |
| `CustomerStepIndicator.kt` | **`CustomerStepIndicator(step, modifier, totalSteps = 5)`** — 3dp rose-gold / hairline segment bar, TalkBack `stateDescription`. |
| `CustomerStates.kt` | **`CustomerLoadingState(modifier, count, rowHeight)`** — flat pulsing skeleton rows. **`CustomerEmptyState(title, modifier, body?, icon = Outlined.Inbox, actionLabel?, onAction?)`**. **`CustomerErrorState(message, modifier, title = "مشکلی پیش آمد", icon = Outlined.CloudOff, retryLabel = "تلاش مجدد", onRetry?)`**. Both centred, no card, outlined icon, `RefPrimaryButton` action, `liveRegion = Polite`. |
| `CustomerConfirmDialog.kt` | **`CustomerConfirmDialog(title, message, confirmLabel, onConfirm, onDismiss, dismissLabel = "انصراف")`** — flat opaque-navy card on the scrim, hairline border, no glass/glow; RTL button row (text dismiss + `RefPrimaryButton` confirm). Replacement for the raw Material3 `AlertDialog`. |

All values copied verbatim from the approved screen-local `Ref*` set (`SalonDetailsScreen.kt`). No global design-system token added, no shared component edited.

## 2. Booking-flow migration (the "replacing booking-only naming" part)

The 3 files in `ai.rojan.designlab.screens.bookingflow.components` were **rewritten as thin forwarders** to the new canonical implementation:

| File | Before | After |
|---|---|---|
| `BookingRefComponents.kt` | ~332 lines — the real implementation | ~135 lines — `val Booking* = Customer*` token aliases + `@Composable fun` forwarders (`BookingSectionLabel`, `RefSurface`, `RefRowDivider`, `RefListRow`, `RefSelectableCell`, `RefPrimaryButton`, `BookingLoadingRows` → `CustomerLoadingState`, `BookingCenteredState` → `CustomerEmptyState`) |
| `BookingScaffold.kt` | ~152 lines | ~30 lines — `BookingScaffold(...) = CustomerScaffold(...)` |
| `BookingStepIndicator.kt` | ~63 lines | ~20 lines — `BookingStepIndicator(...) = CustomerStepIndicator(...)` |

**The 5 already-redesigned booking screens** (`BookingSuccessScreen`, `BookingConfirmationScreen`, `BookingDateScreen`, `BookingTimeScreen`, `ServiceDetailsScreen`, `SpecialistSelectionScreen` — 6, counting the one in `screens/booking/`) **were NOT modified** — they still import `ai.rojan.designlab.screens.bookingflow.components.*` and now resolve through the forwarders to the same implementation. Output is byte-identical (verified on device).

The forwarders are labelled as a compatibility layer and can be deleted once the booking screens switch their imports to `customer.components` — a later, trivial change.

## 3. Files changed

**New (5):** `screens/customer/components/CustomerRefComponents.kt` · `CustomerScaffold.kt` · `CustomerStepIndicator.kt` · `CustomerStates.kt` · `CustomerConfirmDialog.kt`
**Rewritten as forwarders (3):** `screens/bookingflow/components/BookingRefComponents.kt` · `BookingScaffold.kt` · `BookingStepIndicator.kt`
**Screens touched:** none.
**ViewModels / repositories / navigation / routes / API / data models:** none.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14.

| Step | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 (one fix mid-way: a `/*` sequence inside a doc comment — `bookingflow/components/*` — opened a nested Kotlin block comment; reworded) |
| `:app:installCustomerDevDebug` | Installed on 1 device, **BUILD SUCCESSFUL**, exit 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — **zero findings** for any of the 5 new files or the 3 forwarders |

**On-device smoke (booking screens through the forwarders):**
- Service Details (`CustomerScaffold` step 2/5 + `RefSurface` meta strip + rose-gold price + `RefPrimaryButton`) — **pixel-identical** to the pre-migration screenshot.
- Booking Confirmation (`CustomerScaffold` step 5/5 + divided `RefSurface` summary + `RefRowDivider` + `BookingSectionLabel` + static radios + `RefPrimaryButton`) — **pixel-identical**.
- No crash; navigation, payment toggle, and edit-row behaviour unchanged.

## 5. What this unblocks

Per `CUSTOMER-VISUAL-DEBT-AUDIT.md` §D-3, the foundation is now in place for the screen migrations:

- Every remaining screen can adopt `CustomerScaffold` (flat top bar, no `GlassBackButton` orb) + `RefSurface` / `RefListRow` (no `HomeGlassSurface`) + `CustomerLoadingState` / `CustomerEmptyState` / `CustomerErrorState` (no glass `Rojan*State`) + `RefPrimaryButton` (no `PremiumButton` gradient pill).
- `AppointmentsScreen`'s cancel dialog can move to `CustomerConfirmDialog` (the one raw Material3 `AlertDialog` in the app).
- No visual change has shipped yet — the foundation is dormant until screens adopt it.

**Stop after foundation. No screen redesigned. No commit.**
