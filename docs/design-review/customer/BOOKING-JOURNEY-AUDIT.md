# Customer Booking Journey — Visual Audit

**Date:** 2026-09-09 · **Status:** audit only — **no code changed.** Awaiting approval before implementation.
**Reference:** the approved quiet-luxury language — Salon Detail (Golden Reference), redesigned Home, redesigned Explore, the flat shared `CustomerBottomBar`.
**Method:** source inspection of every screen in the flow + the shared components they use. (The design review of 2026-09-09 marked all 6 interior booking screens **NOT TESTED** on device — this audit is the first visual pass over them.)

---

## 1. Screens in the flow

Traced from `RojanNavGraph.kt` (`BOOKING_FLOW_GRAPH`), entry = Salon Detail (already redesigned).

| # | Step | Screen / file | Route | Backing ViewModel(s) | Real data |
|---|---|---|---|---|---|
| 0 | (entry) | `SalonDetailsScreen.kt` — **already redesigned** | `SALON_DETAILS` | `SalonDetailsViewModel`, `SalonRelationshipViewModel` | salon, services, specialists, hours |
| 1 | Specialist selection | `screens/booking/SpecialistSelectionScreen.kt` | `SPECIALIST_SELECTION` | `SpecialistSelectionViewModel` → `GET /salons/{id}/specialists` | specialists (name, bio, photoUrl) |
| 1b | Specialist profile (optional detour — tap a specialist on Salon Detail) | `screens/specialist/SpecialistProfileScreen.kt` | `SPECIALIST_PROFILE` | `SpecialistProfileViewModel` → specialist + category/service fan-out | specialist + bookable services |
| 2 | Service selection (single-service confirm; the *list* is on Salon Detail) | `screens/service/ServiceDetailsScreen.kt` | `SERVICE_DETAILS` | `ServiceDetailsViewModel` → category fan-out | service (name, duration, price, description) |
| 3 | Date | `screens/bookingflow/BookingDateScreen.kt` | `BOOKING_DATE` | `BookingDateViewModel` → `available-slots` (for auto-skip) | 7-day rolling calendar + availability check |
| 4 | Time | `screens/bookingflow/BookingTimeScreen.kt` | `BOOKING_TIME` | `BookingTimeViewModel` → `available-slots` | real time slots for the chosen date |
| 5 | Confirmation (+ payment method, inline) | `screens/bookingflow/BookingConfirmationScreen.kt` | `BOOKING_CONFIRMATION` | `BookingConfirmationViewModel` → `POST /api/v1/bookings` | salon/specialist/service summary, price, payment method |
| 6 | Success | `screens/bookingflow/BookingSuccessScreen.kt` | `BOOKING_SUCCESS` | — (pure presentation) | — |

**Adjacent, NOT in this pass:** `SalonListScreen.kt` (`MEMBER_SALONS_LIST`, the "browse to book" list from the Home CTA) and `SearchScreen.kt` (`SEARCH`) — flagged separately; not part of the "from Salon Detail" journey the task names.

Every screen wraps `HomeBackgroundTheme` (dark navy — keep) and shares: `GlassBackButton`, `HomeGlassSurface`, `HomeColors.Glow` (violet `#7C4DFF`), `PremiumButton` (gradient pill), `RojanShapes.Small` (16dp) / `.GlassCard` (32dp), `rojanEnterAnimation` staggered entrance, and the shared `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` glass state cards.

---

## 2. Visual problems

### 2.1 System-level (every screen)

| # | Problem | Where | Why it fails the reference |
|---|---|---|---|
| B-1 | **`GlassBackButton` orb** — a 48dp circular `GlassSurface` (glass + metallic border + sparkle) floating top-left, off the content grid. | all 7 | "No orb buttons." The Golden Reference replaced it with a flat 56dp top bar + outlined `ArrowBack` on the margin. |
| B-2 | **`HomeGlassSurface` is the only container** — every row, card, chip, and panel is translucent glass + a metallic gold/rose gradient border + ✦ corner sparkles. | all 7 | "No sparkle, no glow, no metallic borders." Reference surface = flat `RefSurface` (4.5% white fill + 1px 9% hairline). |
| B-3 | **Violet `HomeColors.Glow` `#7C4DFF` as the accent** — calendar icons, price text, success circle, checkmarks, selected hints. | all 7 | The approved accent is rose-gold `#E0A67A`, used sparingly (active step / selected item / price / primary CTA only). |
| B-4 | **`PremiumButton` gradient pill** — magenta→pink `RojanGradients.PremiumButton` fill, `Pill` (50dp) shape. | Service Details, Confirmation, Success | "No gradients, no giant pills, no gradient CTA pills." Reference CTA = solid rose-gold, 12dp radius, 52dp. |
| B-5 | **No top bar / no title hierarchy** — the screen title is a bare `HeroTitle` (32/Bold) floating under the orb, with inconsistent spacing above it (`padding(vertical=SpaceMD)` vs `Spacer(SpaceMD)` vs `Spacer(SpaceLG)`). | all 7 | Reference has a real 56dp bar with a `Body`-weight centred title + hairline. |
| B-6 | **No step / progress context** — a 6-step flow with zero "where am I" signal. Each screen reads as an isolated island. | all | A premium mobile booking flow shows progress. |
| B-7 | **Staggered list entrance** — `rojanEnterAnimation(delayMillis = index * 60)` on every list (specialists, dates, services, and the time grid). Confirmation's own code comment says per-row stagger "would read as noisy here, not premium" — yet the sibling screens all do it. | 1, 1b, 2, 3 | Reads playful. Reference uses at most one calm screen fade. |
| B-8 | **Filled Material icons** — `Icons.Filled.CalendarMonth / ContentCut / Storefront / AccessTime / CheckCircle / RadioButtonUnchecked`. | all | Reference is one outlined family. |
| B-9 | **Inconsistent scaffolding** — Specialist/Date/Time: `Column + padding(SpaceMD) + orb + title + when{}`. Service/SpecialistProfile: `LazyColumn` with the orb as item 0. Confirmation: `verticalScroll` `Column` + pinned bottom button. No shared skeleton. | all | Reference implies one shared booking shell. |
| B-10 | **Shared state cards** — `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` are glass cards, placed inconsistently (centred in a scaffold on some, inline under the title on others). | all | Reference states = calm centred stack, no card (`RefCenteredState` pattern), or flat skeleton rows. |

### 2.2 Per-screen

**1 · Specialist Selection**
- B-11 Specialist avatar sits on a `accentFor(id).copy(alpha = 0.5f)` **coloured circle tint** (lavender/mint/pink/pearl by hash) — decorative, meaningless, and full-chroma.
- B-12 Row = avatar + name (`Body`) + bio (`Caption`) with **no trailing affordance** (no chevron), so the row doesn't read as "tap to choose."
- B-13 Title `HeroTitle` with `padding(vertical = SpaceMD)` — cramped against the orb.

**1b · Specialist Profile**
- B-14 96dp avatar on a `accentFor()` colour-tint circle; name `HeroTitle` directly under it, no breathing room.
- B-15 Bio is wrapped in its own `HomeGlassSurface` card — a paragraph of text does not need a glowing container.
- B-16 Service rows are individual glass cards (`RtlListRow` inside `HomeGlassSurface`), each with the metallic border — "every piece of information is a separate glowing card."

**2 · Service Details**
- B-17 **140dp decorative colour band** (`accentFor(service.id).copy(alpha=0.5f)`, 32dp `GlassCard` radius) containing a **giant 48dp filled `ContentCut` scissors** — pure decoration, no information, childish.
- B-18 Service **name and price are both `HeroTitle` (32sp)** — name in white, price in violet — competing, no hierarchy.
- B-19 Description in a `HomeGlassSurface` card.
- B-20 CTA "رزرو این خدمت" = `PremiumButton` gradient pill, not pinned (sits in a `Box(padding)` after the list).

**3 · Booking Date**
- B-21 Date list = full-width `HomeGlassSurface` rows, each just **"violet calendar icon + label"** — no day-of-week emphasis, no "امروز/فردا", no "this week" grouping, no availability hint. Flat and characterless for a date picker.
- B-22 `UiState.Empty` renders **nothing** (`is UiState.Empty -> Unit`) — a silent dead screen if the backend returns empty.
- B-23 No selected state (the row navigates immediately, but there's no visual "this is the soonest available" when auto-skip fires).

**4 · Booking Time**
- B-24 3-col `LazyVerticalGrid` of `HomeGlassSurface` chips — uses raw `Modifier.clickable` (**no `rojanPressable`**, inconsistent press feedback), `RojanShapes.Small` (16dp), centred `Body`, **no selected state**.
- B-25 No context line — the screen says "انتخاب ساعت" but not *which date* the user picked on the previous screen.
- B-26 Chips are uniform glass with the metallic border — a grid of ~15 glowing pills reads as a game level-select.

**5 · Booking Confirmation**
- B-27 Summary in one 32dp `GlassCard`; a **salon chip with a `accentFor()` colour tile + filled `Storefront`** at the top duplicates the "سالن" row right below it.
- B-28 Price row ("مبلغ قابل پرداخت") value in **violet `HomeColors.Glow`**; should be the rose-gold emphasis and visually distinct (larger), not just tinted.
- B-29 Payment rows = separate `HomeGlassSurface` cards with `RadioButtonUnchecked` + an **animated `RojanSuccessCheckmark` "beat"** on select — a celebratory animation on a radio button is over-styled.
- B-30 `submitError` shows in `HomeColors.Magenta` — inconsistent with a calm error treatment.
- B-31 CTA "تایید نهایی رزرو" = gradient pill; disabled state is `PremiumButton`'s own (not a clean 40%-alpha rose-gold).

**6 · Booking Success**
- B-32 **96dp `HomeColors.Glow`-alpha circle** + a filled `CheckCircle` (XLarge) in violet — the "reward screen in a mobile game" pattern the design review called out.
- B-33 `HeroTitle` headline + gradient-pill CTA, centred — loud where it should be calm and assured.
- B-34 No booking detail recap (date/time/salon) and no "مشاهده نوبت‌های من" secondary action — the flow just dead-ends at "بازگشت به خانه".

---

## 3. Recommended reference layout

### 3.1 One shared booking shell

Introduce **one new file, booking-only**: `screens/bookingflow/BookingScaffold.kt` (a `private`-primitive-style shared composable, provably used only by the 7 booking screens — cannot affect any approved screen). It provides:

```
BookingScaffold(
    title: String,                       // step title, Body weight, centred
    onBackClick: () -> Unit,
    step: Int? = null, totalSteps: Int = 5,   // null on Success / detours
    bottomBar: @Composable (() -> Unit)? = null,   // the pinned CTA slot
    content: @Composable ColumnScope.() -> Unit,
)
```

- **Top bar (56dp)** — identical to Salon Detail's `RefTopBar`: outlined `Icons.AutoMirrored.Outlined.ArrowBack` on the content margin (RTL-right), centred `title` (`RojanTypography.Body`, 1 line, ellipsis), 1px bottom hairline (`White @ 9%`). No orb, no glass.
- **Step progress** (when `step != null`) — a single 3dp-tall hairline row directly under the bar, split into `totalSteps` segments with a 4dp gap: segments `≤ step` are rose-gold `#E0A67A`, the rest `White @ 9%`. No numbers, no labels — quiet and editorial. Steps: `متخصص · خدمت · تاریخ · ساعت · تایید` (Success and the profile detour pass `step = null`).
- 20dp screen margin (`RefScreenMargin`), `SpaceLG` between blocks, `SpaceXXL` bottom padding; content scrolls, `bottomBar` pinned.

### 3.2 Shared primitives (screen-local `private`, mirroring Salon Detail — **no shared-component edits**)

| Token / component | Value |
|---|---|
| `RefSurfaceFill` | `White @ 4.5%` |
| `RefHairline` / `RefDivider` | `White @ 9%` / `White @ 7%` |
| `RefAccent` | `RojanPremiumBorderRoseGold` `#E0A67A` |
| `RefOnAccent` | `#1B1530` |
| card radius | **14dp** · button radius **12dp** / height **52dp** |
| `RefSurface` | flat: `clip(14dp) + background(fill) + border(1dp, hairline)` — no glass, glow, sparkle, metallic |
| `RefSectionLabel` | `Caption`/SemiBold, `TextMuted` |
| `RefPrimaryButton` | solid `RefAccent`, 12dp, 52dp, `Button` label in `RefOnAccent`; disabled = 40% alpha |
| `RefListRow` | flush inside one `RefSurface`, 1px dividers, ≥ 56dp, `rojanPressable`, trailing `KeyboardArrowLeft` chevron |
| `RefSelectableCell` | dates/times/payment: unselected = `RefSurface`; selected = `RefAccent` fill + `RefOnAccent` text (or a 3dp leading accent bar + a small `Check` for rows) |
| `RefLoadingSkeleton` | flat dimmed bars/rows matching the layout — no glass state card |
| `RefCenteredState` | calm centred stack (outlined 36–40dp icon `TextMuted` · `CardTitle` title · `Caption` body · optional `RefPrimaryButton`) |
| icons | `Icons.Outlined.*` only |
| motion | none per-row; at most one calm screen fade. Success gets **one** checkmark draw-in (the single sanctioned "moment of delight"). |

### 3.3 Per-screen

| Screen | Layout |
|---|---|
| **1 · Specialist Selection** | Shell "انتخاب متخصص", step 1/5. One `RefSurface` with a divided list of specialist rows: 48dp circular avatar (initials on `RefSurfaceFill`, **no colour tint**), name (`Body`), bio (`Caption` muted, 1 line), trailing chevron. Loading → 4 skeleton rows. Empty → `RefCenteredState` "متخصصی یافت نشد". |
| **1b · Specialist Profile** | Shell with the specialist's name, `step = null`. Centred 72dp avatar (initials, no tint) → name (`Display.copy(26)`) → bio (`Body`, ≤3 lines, **no card**). `RefSectionLabel` "خدمات قابل رزرو" → one `RefSurface` divided list (service name `Body` · "۳۰ دقیقه" `Caption` muted · chevron). Tap → Service Details. |
| **2 · Service Details** | Shell with the service name, `step = 2/5`. **No colour band, no scissors icon.** Header: service name (`Display.copy(26)`) → meta row (`Schedule` 15dp + "۳۰ دقیقه" `Caption` muted · `·` · price in `RefAccent`, `Caption`/SemiBold). Description in a flat `RefSurface` (`Body`). `bottomBar` = `RefPrimaryButton` "رزرو این خدمت". |
| **3 · Booking Date** | Shell "انتخاب تاریخ", step 3/5. A tight **vertical list of day cells** (`RefSurface`, 14dp): right = weekday (`Body`) with "امروز"/"فردا" swapped in for the first two; left = the Jalali/Gregorian date (`Caption` muted). Today's row: primary-white text + a 3dp leading `RefAccent` bar. Tap → select. Keep `RollingBookingDates.next7Days()` + `autoSelectedDate` auto-skip verbatim. Empty/error → `RefCenteredState` (fixes the current silent `Unit`). |
| **4 · Booking Time** | Shell "انتخاب ساعت" + a sub-line "`<selected date label>`" (`Caption` muted, under the bar). 3-col grid of flat time chips (`RefSurface`, 14dp, **`rojanPressable`**), centred `Body`. Selected chip → `RefAccent` fill + `RefOnAccent`. Empty → `RefCenteredState` "زمانی برای این تاریخ موجود نیست" + a "انتخاب تاریخ دیگر" button → `onBackClick`. |
| **5 · Booking Confirmation** | Shell "تایید رزرو", step 5/5. One `RefSurface` summary card: a compact salon line (24dp initials tile + name), then 5 divided rows — سالن / متخصص / خدمت / تاریخ / ساعت — each `Body` label (muted) + value (primary) + edit chevron. Then a visually distinct **"مبلغ قابل پرداخت"** block: larger value in `RefAccent`, no chevron, a hairline above it. `RefSectionLabel` "روش پرداخت" → 2 `RefSurface` rows with a real radio affordance (outlined `RadioButtonUnchecked` → filled `RadioButtonChecked` in `RefAccent`), **no animated beat**. `bottomBar`: `submitError` as a `Caption` (`TextMuted`, not magenta) above `RefPrimaryButton` "تایید نهایی رزرو" (disabled = 40% alpha). Every ViewModel call, edit route, and `isReadyForConfirmation()` gate unchanged. |
| **6 · Booking Success** | Dark ground, no top bar. Centred: a **56dp outlined `CheckCircle` in `RefAccent`** (no 96dp glow disc) with a one-shot draw-in, then "رزرو شما با موفقیت ثبت شد" (`Display.copy(26)` or `ScreenTitle`), then "پیامک تایید به شماره شما ارسال خواهد شد" (`Body`, `TextSecondary`). Optional: a compact recap line (salon · date · ساعت) in `Caption` muted. Bottom: `RefPrimaryButton` "بازگشت به خانه" → `onDoneClick`. *(Adding "مشاهده نوبت‌های من" would need a new nav callback → out of scope; flagged.)* |

### 3.4 Business logic — untouched

Every `ViewModel`, factory, `on*` callback, route, and nav-graph block stays byte-identical: `SpecialistSelectionViewModel` / `SpecialistProfileViewModel` / `ServiceDetailsViewModel` / `BookingDateViewModel` / `BookingTimeViewModel` / `BookingConfirmationViewModel`, `BookingViewModel` state events (`onSpecialistSelected`, `onServiceSelected`, `onDateSelected`, `onTimeSelected`, `onPaymentMethodSelected`), `confirmBooking(...)`, `isReadyForConfirmation()`, `nextStep()` / `routeForBookingStep(...)`, `RollingBookingDates`, the `available-slots` calls, the auth gate on `BOOKING_TIME`, auto-skip-specialist / auto-skip-date, and edit-from-confirmation routing. **Visual layer only.**

### 3.5 Shared components NOT modified

`GlassBackButton`, `PremiumButton`, `HomeGlassSurface`, `GlassSurface`, `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState`, `RtlListRow`, `RtlSectionHeader`, `SpecialistAvatar`, `RojanSuccessCheckmark`, `RojanIconContainer` — all shared with non-booking screens. Each booking screen gets screen-local `private` `Ref*` primitives instead, exactly as Salon Detail / Home / Explore did.

**One item needs explicit approval:** the new `BookingScaffold.kt` shared file (§3.1). It would be the first *new* shared surface in this redesign effort. It is booking-only and cannot affect an approved screen, but if you'd rather each screen inline its own top bar (as Salon Detail does), say so and I'll do that instead — at the cost of ~7× duplication of the 56dp-bar + step-progress code.

---

## 4. Proposed implementation order (after approval)

1. `BookingScaffold.kt` (if approved) + the shared `Ref*` primitives pattern.
2. Success (simplest, highest "childish" signal) → 3. Confirmation → 4. Date → 5. Time → 6. Service Details → 7. Specialist Selection → 8. Specialist Profile.
3. One combined compile + device walkthrough of the whole journey (Salon Detail → … → Success) + lint.
4. `docs/design-review/customer/BOOKING-JOURNEY-REDESIGN-REVIEW.md`.

**No code will be written until this audit is approved.**
