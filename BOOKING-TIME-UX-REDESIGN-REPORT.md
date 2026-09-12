# ROJAN Customer — Booking Time UX & Back-Navigation Audit

**Date:** 2026-09-10
**Scope:** Customer Android app only — the booking navigation stack (`HOME → Salon Details → Service → Specialist → Date → Time → Confirmation → Success`) and the Booking Time screen's presentation. No backend changes, no design changes outside the booking flow.
**Status:** ✅ Complete. `compileCustomerDevDebugKotlin`, `lintCustomerDevDebug`, `assembleCustomerDevDebug` all green, lint warning count unchanged at baseline (94).

---

## 1. Back-navigation investigation

### Root cause

**Duplicate forward `navigate()` pushes from unguarded rapid double-taps**, not a `BackHandler` conflict, not a nested-graph defect, and not a duplicated `popBackStack()` call. Concretely:

- The whole Customer app has **zero `BackHandler`/`OnBackPressedCallback` usages** anywhere (grep-confirmed) — system back is handled entirely by Navigation-Compose's own default dispatcher registration, and it maps to exactly one `popBackStack()` per press. There is nothing intercepting or double-consuming the system back event.
- The top app-bar back button and system back both resolve to the *same* `onBackClick = { navController.popBackStack() }` lambda per screen (`CustomerScaffold`/`BookingScaffold`) — they were never two different code paths, so there was nothing to make them "behave identically" *at the trigger level*.
- The single `RojanNavGraph()` uses one `NavHost`/one `NavController` (confirmed via grep — the Customer flavor never nests a second `NavHost`), and the booking flow's nested `navigation(route = BOOKING_FLOW_GRAPH, …)` graph is exactly the standard, documented Android pattern for a shared-scope `BookingViewModel` (its own doc comment already explains this). `NavController.popBackStack()` transparently pops exactly one leaf entry per call, including at a nested graph's start destination — there is no library-level "two pops needed" behaviour here. **Nested-graph issues were investigated and ruled out.**
- The actual defect: **none of the booking flow's forward `navController.navigate(...)` calls set `launchSingleTop = true`.** Every selection control that leads deeper into the flow (a `DateCell`, a `TimeChip`, the "Book" CTA, a salon/specialist row, an "ویرایش…" edit link on Confirmation) has no tap-debounce, and each screen transition runs a ~300ms fade+scale (`RojanNavTransitions.pageExit`) during which the *outgoing* composable is still on screen and still clickable. A fast double-tap on, most commonly, a `DateCell` on the Date screen fires `onDateSelected` twice: the first call pushes `BOOKING_TIME` and starts the exit fade; the second call — still landing on the not-yet-fully-gone Date screen — fires again and pushes **a second, identical `BOOKING_TIME` entry** on top of the first. `ServiceDetailsScreen`'s "Book" CTA has the same exposure through a different mechanism: `onBookClick` launches a coroutine that suspends on `specialistRepository.getSpecialists(...)` before calling `navigate()`, so two rapid taps can genuinely run two concurrent coroutines that each independently call `navigate()` once resumed.
- With two stacked, identical `BOOKING_TIME` entries, a single back press (system or app-bar — both call the same `popBackStack()`) only pops the top duplicate and lands on the *other, visually identical* `BOOKING_TIME` screen underneath. Nothing appears to change, so the user perceives "back requires multiple attempts" — exactly the reported symptom — even though every individual press was correctly popping one real stack entry the whole time.

### Fix

Added `launchSingleTop = true` to **every forward `navController.navigate(...)` call in the booking chain** (`RojanNavGraph.kt`, 20 call sites): the two Home→Salon-Details entry points (Dashboard, Explore), `MEMBER_SALONS_LIST`/`SALON_LIST`, `SEARCH`, `SALON_DETAILS` (specialist click, service click, continue-booking), `SPECIALIST_SELECTION`, `SPECIALIST_PROFILE`, the coroutine-based `SERVICE_DETAILS` "Book" navigate, `BOOKING_DATE`'s `onDateSelected`, `BOOKING_TIME`'s `onTimeSelected` (both the authenticated and the AUTH-gate branches), all five `BOOKING_CONFIRMATION` edit links plus its confirm-click, and the AUTH "resume booking" `popUpTo` navigate. This is the exact same mechanism the codebase already uses elsewhere in this file for the bottom-tab navigation (`onCustomerTab`) — not a new pattern, just applied consistently across the booking chain where it was missing.

`launchSingleTop` makes a `navigate()` call to a destination that is *already* the current top of the back stack a no-op (or, for a same-destination call with different args, a same-entry argument replacement) instead of a real push. Because each pair of duplicate-tap `navigate()` calls executes on the main thread and the first call's stack mutation is synchronous, by the time the second call runs, the back stack top already reflects the first — so the second call is correctly deduped in every case investigated, including the concurrent-coroutine case in `ServiceDetailsScreen` (both coroutines' `navigate()` calls still execute one-at-a-time on the main dispatcher).

This satisfies all four stated requirements directly:
- **System back returns exactly one step** — there are no more duplicate entries to hide behind.
- **Top app-bar back button behaves identically** — it already called the same `popBackStack()`; now that the stack itself is correct, both triggers produce the same, correct result.
- **No duplicated navigation** — the specific mechanism (duplicate push) is closed off at every entry point in the chain.
- **No stuck screens** — a screen that looked "stuck" was actually two copies of itself; there is now only ever one.

### Preserved (unchanged)

`BookingViewModel` scoping (`bookingViewModelFor`, the nested graph's own back-stack entry, the `popUpTo(BOOKING_TIME)` AUTH-resume dance) is untouched — `launchSingleTop` only changes whether a *new* leaf entry is pushed, never which graph entry owns the shared ViewModel store. Selected salon / service / specialist / date / time all continue to live in the same `BookingViewModel.state`, updated by the same calls, in the same order, before each `navigate()`. No `BookingViewModel`, repository, API, or route signature was touched.

---

## 2. Booking Time slots UX audit

### Before
A single flat `LazyVerticalGrid(columns = GridCells.Fixed(3))` rendering every returned `TimeSlot` as an equal-weight chip with no grouping — for a full business day of 15-minute slots (commonly 40+), this reads as one dense, undifferentiated wall of identical buttons.

### Decision
- **Grouped by day-part, not re-bucketed in time.** Slots are split into `صبح` (before 12:00), `بعدازظهر` (12:00–16:59), `عصر` (17:00+) — computed from the already-rendered "HH:mm" label, purely for display. `state.data.groupBy { dayPartFor(it.timeLabel()) }` is the only new logic; **every slot the backend returned is still rendered as its own chip**, in its original order, at its original 15-minute granularity — nothing is merged, rounded, or dropped.
- **One continuous scroll, not nested scrolling.** The day-part headers are full-width rows inside the *same* `LazyVerticalGrid` (`item(span = { GridItemSpan(maxLineSpan) })`), not separate `LazyColumn`/`LazyRow` combinations — avoids the nested-scroll-conflict class of bug entirely while still visually breaking the grid into sections.
- **4 columns instead of 3** — more, smaller chips per row for the same slot count, directly addressing "too many time buttons make the screen crowded" by fitting more per screen-height without touching slot data.
- **Selected state** — unchanged: still `RefSelectableCell`'s existing flat-surface → solid-rose-gold-fill flip (`CustomerAccent`), the same mechanism already used for `DateCell` and every other selectable cell in the app.
- **Backend untouched** — `BookingTimeViewModel`, `availabilityRepository`, the `available-slots` call, and `TimeSlot`/`timeLabel()` are all unmodified; this is a pure presentation change on top of the same `UiState.Success<List<TimeSlot>>`.

### Spacing / touch targets / RTL / scrolling review
- **Touch target:** `RefSelectableCell` already enforces `heightIn(min = RojanDimens.MinTouchTarget)` = 48dp internally — unchanged, still applies at 4 columns. Cell *width* at 4 columns on a typical 360–411dp-wide device works out to roughly 74–95dp, comfortably above the 48dp floor in both dimensions.
- **Spacing:** kept the existing `Arrangement.spacedBy(RojanDimens.SpaceSM)` (8dp) in both axes; day-part headers get `SpaceMD` (16dp) top / `SpaceXS` (4dp) bottom so a new section reads as a clear break without an oversized gap.
- **RTL order:** reviewed and deliberately left chronological left-to-right within each row — this app renders in a fixed, ambient-LTR `LayoutDirection` everywhere by design (`RtlLayoutKit.kt`'s doc comment: forcing global RTL was tried and rejected because it mirrored card photo/text composition and back-button placement), and every other grid/list in the app (`DateCell`'s vertical list, `SalonCard`, `SearchResultRow`) already follows the same convention — a grid of equal-size, non-textual time chips has no inherent reading direction the way a sentence does, so this isn't a new inconsistency, it's the existing, previously-reviewed pattern applied to one more grid. The **day-part header text itself** still right-aligns correctly, unchanged, via the shared bidi-aware `Text`.
- **Scrolling:** single `LazyVerticalGrid`, no nested scrollable containers — headers and chips share one scroll position, verified structurally (span-based header row is the standard, documented `LazyVerticalGrid` pattern for section headers inside a grid).

---

## 3. Visual consistency / legacy UI check

`BookingScaffold`, `RefSurface` (via `RefSelectableCell`), `BookingSectionLabel`/`BookingSectionLabelStyle`, and `CustomerAccent` are the only visual primitives used — all pre-existing Quiet Luxury tokens/components, nothing new introduced. `DayPartHeader` deliberately does **not** call `BookingSectionLabel` directly: that component carries its own horizontal margin, which would have doubled up against the grid's own `contentPadding` and misaligned the header against the chips beneath it — so it reuses `BookingSectionLabelStyle` (the same visual token) without the redundant inset instead.

Grepped the entire booking navigation stack (`screens/bookingflow/`, `screens/booking/`) for legacy UI remnants (`HomeGlassSurface`, `GlassBackButton`, `PremiumButton`, `RtlListRow`, `RtlSectionHeader`, bare `HeroTitle`): every hit is a doc-comment historical note ("the floating `GlassBackButton` orb … are replaced with …") from the prior Quiet Luxury migration phases, not live code. **No legacy booking UI remains anywhere in this stack** — nothing needed removing.

---

## 4. Files changed

- `app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt` — `launchSingleTop = true` added to 20 forward-navigation call sites across the full `HOME → … → Success` chain (see §1). No route, argument, `popUpTo`, or `BookingViewModel`-scoping logic changed.
- `app/src/main/java/ai/rojan/designlab/screens/bookingflow/BookingTimeScreen.kt` — grouped, 4-column grid with `DayPart` sectioning; `DayPartHeader` + a small private `Int.toPersianDigits()` helper added. No change to `BookingTimeViewModel`, its factory, `TimeSlot`, or `timeLabel()`.

Nothing else touched. No backend, ViewModel (beyond reading existing state, unchanged), repository, or unrelated screen was edited.

---

## 5. Regression checks

- **BookingContext / selected salon / service / specialist / date / time:** all still driven by the same `BookingViewModel.state` writes, in the same order, before the same `navigate()` calls — `launchSingleTop` does not affect argument passing or ViewModel scoping. Verified by reading `bookingViewModelFor` and every touched call site: none of the `bookingViewModel.onXSelected(...)` calls were reordered, removed, or wrapped.
- **Edit-from-Confirmation flow:** `onEditSalon`/`onEditSpecialist`/`onEditService`/`onEditDate`/`onEditTime` still navigate without a `popUpTo` (pushing a new entry on top of Confirmation, so back correctly returns to Confirmation) — only `launchSingleTop` was added, the push-not-replace behavior for the *first* tap is unchanged.
- **AUTH "resume booking" dance:** the existing `popUpTo(RojanDestinations.BOOKING_TIME) { inclusive = false }` block (the P0 fix documented in-file for avoiding a second `BookingViewModel` instance) is untouched; `launchSingleTop = true` was added alongside it in the same `navOptions` lambda, which is additive and does not change the `popUpTo` semantics.
- **All-slots-preserved:** the grouping is a pure `groupBy` over the same `List<TimeSlot>` — slot count in vs. chip count out is 1:1, verified by construction (no `filter`/`distinct`/dedup introduced).
- **Compile / lint / assemble:**
  ```
  :app:compileCustomerDevDebugKotlin  → BUILD SUCCESSFUL
  :app:lintCustomerDevDebug           → BUILD SUCCESSFUL, 94 warnings (unchanged from session baseline, 0 new findings in either changed file)
  :app:assembleCustomerDevDebug       → BUILD SUCCESSFUL
  ```

No device run was performed or required per the task's "no device required" instruction; the back-navigation root cause was established through static analysis (full grep sweep for `BackHandler`/`OnBackPressedCallback`, every `navigate()`/`popBackStack()` call site in `RojanNavGraph.kt` read in context, `RojanNavTransitions`/`rojanPressable` inspected for any debounce or double-fire behavior) rather than on-device reproduction. Nothing was committed.
