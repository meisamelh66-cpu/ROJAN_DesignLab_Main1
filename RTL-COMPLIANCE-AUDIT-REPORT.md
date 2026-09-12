# ROJAN Customer — RTL & Layout Direction Compliance Audit

**Date:** 2026-09-10
**Scope:** Customer app only (`ai.rojan.designlab`, `customer` flavor). Manager/Reception screens excluded per instructions.
**Goal:** 100% Persian RTL visual compliance — direction/alignment fixes only, no redesign, no backend changes.
**Status:** ✅ Complete. 5 files fixed (7 edit sites). `compileCustomerDevDebugKotlin` and `lintCustomerDevDebug` both green, lint warning count unchanged at baseline (94).

---

## 1. Architecture finding (read this before interpreting the fixes)

This app does **not** use ambient/global RTL (`LocalLayoutDirection` is never overridden, no `values-fa`, no locale-driven layout direction). This was a deliberate, previously-documented decision — see the doc comment in `ui/components/rtl/RtlLayoutKit.kt`:

> Forcing `LayoutDirection.Rtl` app-wide was tried and explicitly **rejected**: it mirrored salon/service/specialist card photo-vs-text composition and reversed back-button placement, breaking the frozen Design Baseline.

Instead, the app runs ambient **LTR** everywhere (confirmed empirically — `Alignment.End`/`Arrangement.End` resolve to the physical **right** edge, `Start` to physical **left**, consistently, across every screenshot collected in this session), and RTL correctness is achieved by **explicit composition**: code order + `Alignment.End`/`Arrangement.End` placed deliberately so the rendered result reads right-to-left like Persian expects.

Two dominant, correct row shapes are used throughout the app and were used as the reference template for every fix below:

- **List-row shape** (`RefListRow`, `DateCell`, `SearchResultRow`, `SalonCard`, `SalonHeaderRow`): chevron/status icon **first** in code (→ physical left) → `Column(horizontalAlignment = Alignment.End)` for title/subtitle (weighted, right-anchored) → leading visual (avatar/logo) **last** in code (→ physical right, flush edge).
- **Value+label shape** (`SummaryRow`, `PriceRow`, `SuccessRecapRow`, `DetailRow`): primary content `Modifier.weight(1f)` **first** in code, secondary fixed-width label/badge **last** in code.

Text alignment itself is **not** locale-driven — it's **content-based**. The shared `ai.rojan.designlab.ui.text.Text` (`RojanText.kt`) shadows Material3's `Text`, runs first-strong-character Unicode Bidi detection (`resolveTextDirectionality`) against explicit RTL Unicode block ranges (Hebrew/Arabic/Syriac/Arabic-Presentation-Forms), and sets `textDirection`/`textAlign` automatically unless the caller passes an explicit override. Every Customer `.kt` file uses this shared `Text` — grep confirmed **zero** raw `androidx.compose.material3.Text` bypasses in Customer scope, and zero hardcoded `TextAlign.Left`/`Alignment.Left`/`padding(left=/right=)`/`absolutePadding` calls anywhere in the app.

**Conclusion:** the text-alignment and back-arrow-mirroring layers of this app were already 100% compliant before this audit. The actual bugs found were all in the third category the task asked about — **row/card composition order** — where a handful of components (mostly recent additions) didn't follow the two conventions above.

---

## 2. Audit checklist results

| # | Area | Result |
|---|------|--------|
| 1 | Text alignment (no left-aligned Persian, no hardcoded Left) | ✅ Compliant app-wide — content-based bidi resolver, zero hardcoded overrides found |
| 2 | Icons (back arrows, leading/trailing, no LTR placement mistakes) | ✅ Compliant app-wide — 100% `Icons.AutoMirrored.*` usage for all back arrows/chevrons, zero non-mirroring variants found |
| 3 | Rows (avatar/icon/title/subtitle ordering, action icon side) | ⚠️ 5 bugs found and fixed (see §3) — all other rows already followed the list-row/value+label conventions |
| 4 | Buttons (text+icon arrangement) | ✅ Compliant — no button components deviate from RTL arrangement |
| 5 | Booking screens (Date/Time/Confirmation/Success) | ✅ Compliant — `DateCell`, `TimeChip`, `SalonHeaderRow`, `SummaryRow`, `PriceRow`, `PaymentRow`, `SuccessRecapRow` all already correct |
| 6 | Profile screens (Header/Menu rows/Settings) | ⚠️ Header had 3 bugs (this session's own Phase 5B regression) — fixed. Menu rows (`RefListRow`-based) and settings already compliant |
| 7 | Beauty DNA | ✅ Compliant — built entirely on `RefListRow` (`OptionRow`/`SingleSelectRows`/`MultiSelectRows`), no direction issues |

---

## 3. Bugs found and fixed

### 3.1 `screens/profile/ProfileScreen.kt` — `ProfileHeader` (Phase 5B regression)

This session's own recently-added profile header (avatar/cover upload UI) was composed with the avatar pinned to the **physical left** edge — a leftover from writing it LTR-first. Confirmed visually via the real device screenshot `docs/device-verification/profile-phase5b/02_profile_header.png`.

**Fix 1 — avatar position:** `Alignment.BottomStart` → `Alignment.BottomEnd`, `padding(start=)` → `padding(end=)`. The inward-facing edit badge on the avatar was flipped in tandem (`BottomEnd` → `BottomStart`) so it still faces the screen center rather than the outer edge.

**Fix 2 — name/verified-chip row:** `Modifier.weight(1f, fill = false)` → `Modifier.weight(1f)` (default `fill = true`). With `fill = false` the name text hugged its own intrinsic width instead of claiming the row's leading space, which broke the right-anchoring that `RojanText`'s auto RTL alignment depends on for a `weight`ed Text. Now matches the proven `SummaryRow`/`SuccessRecapRow` shape.

**Fix 3 — "remove photo" links row:** added `.fillMaxWidth()` + `horizontalArrangement = Arrangement.End`, and swapped the code order (cover-link first, avatar-link last) so the avatar-removal link now sits under the (now right-aligned) avatar, matching fix 1.

### 3.2 `screens/customer/CustomerHomeScreen.kt` — Explore tab

Confirmed visually via `docs/device-verification/nav-phase2/02_explore_tab.png`.

**Fix 4 — `ExploreHeader` title/subtitle column:** `Column(modifier = Modifier.weight(1f))` → added `horizontalAlignment = Alignment.End`. Title/subtitle text were rendering flush to the left inside their weighted column instead of right-anchored against the profile-icon.

**Fix 5 — `ExploreSalonCard`:** full reorder from `[logo] [Column] [chevron]` to `[chevron] [Column(End)] [logo]`, matching `SearchResultRow`/`SalonListScreen.SalonCard` exactly (this was the only card in the app with the leading-visual and chevron on the wrong sides). The inner address sub-row's icon-then-text micro-ordering was left untouched — it already matches the compliant `ContactRow`/`RefServiceRow` pattern.

### 3.3 `screens/customer/CustomerDashboardScreen.kt` — Home tab

**Fix 6 — `HomeSalonCard`** (200dp horizontal salon card): added `horizontalAlignment = Alignment.End` to the outer `Column` — icon, name, and address row were all left-anchored inside the card.

**Fix 7 — `HomeBookingRow`** (live component, rendered at the "your bookings" section): added `horizontalAlignment = Alignment.End` to the weighted `Column` holding salon name / specialist name / time row.

### 3.4 `screens/profile/AppointmentsScreen.kt` — appointment list card

Added `horizontalAlignment = Alignment.End` to the weighted `Column` (salon name / specialist name / time row) — identical bug and fix shape to `HomeBookingRow` above; `StatusPill` at the row's far right (already correct) is unaffected, matching `SummaryRow`'s label-flush-right convention.

### 3.5 `screens/profile/AppointmentDetailsScreen.kt` — header block

Added `horizontalAlignment = Alignment.End` to the header `Column` (salon name / service+specialist subtitle / time row / `StatusPill`) inside `AppointmentDetailsContent`. `DetailRow` used later in the same file was already compliant — no change needed there.

---

## 4. Confirmed already-compliant (no change needed)

Read/verified in full or via targeted grep, all correct as-is:

- `ui/text/RojanText.kt`, `ui/components/rtl/RtlLayoutKit.kt` — the shared RTL primitives themselves.
- `screens/customer/components/CustomerRefComponents.kt` (`RefListRow`), `screens/bookingflow/BookingDateScreen.kt` (`DateCell`), `BookingTimeScreen.kt` (`TimeChip`), `BookingConfirmationScreen.kt` (`SalonHeaderRow`/`SummaryRow`/`PriceRow`/`PaymentRow`), `BookingSuccessScreen.kt` (`SuccessRecapRow`), `screens/profile/BeautyDnaScreen.kt`, `screens/search/SearchScreen.kt` (`SearchResultRow`/`SearchField`), `screens/booking/SalonListScreen.kt` (`SalonCard`), `screens/profile/AppointmentDetailsScreen.kt` (`DetailRow`), `screens/customer/CustomerBottomBar.kt`, `screens/profile/FavoritesScreen.kt`, `screens/profile/RescheduleAppointmentScreen.kt`, `screens/customer/components/CustomerBookingStatus.kt` (`StatusPill`), `screens/profile/ProfileScreen.kt` (`LogoutRow`), `screens/salon/SalonDetailsScreen.kt` (`RefServiceRow`), `screens/profile/FollowedSalonsScreen.kt` (built on `RefListRow`), `screens/salon/PublicSalonScreen.kt` (`ContactRow`).
- App-wide: zero hardcoded `TextAlign.Left`/`Alignment.Left`/`Alignment.Right`/`padding(left=/right=)`/`absolutePadding` calls; zero non-`AutoMirrored` back-arrow or chevron icon usages.

**Excluded as dead code** (no external call sites in Customer scope, confirmed via grep — not part of the compiled/rendered app, so out of audit scope): `screens/customer/UpcomingBookings.kt` (the one remaining `Modifier.weight(1f)`-without-alignment hit in the codebase, left as-is since it's unreachable), `ui/components/hero/HeroBookingCard.kt`, `screens/dashboard/DashboardPlaceholder.kt`, `ui/components/ai/RojanAIInsightCard.kt`, `screens/customer/HomeHeader.kt` (explicitly listed in `CustomerHomeScreen.kt`'s own doc comment as "removed from the composition, not deleted").

---

## 5. Validation

```
:app:compileCustomerDevDebugKotlin  → BUILD SUCCESSFUL
:app:lintCustomerDevDebug           → BUILD SUCCESSFUL, 94 warnings (unchanged from session baseline, 0 new findings, 0 errors)
```

No `assemble`/instrumented run was required or performed per the task's "no device required" instruction; the two screenshots referenced above (`profile-phase5b/02_profile_header.png`, `nav-phase2/02_explore_tab.png`) were already-captured real A72 evidence from earlier phases in this session and were used to confirm bugs 1–5 visually rather than re-capturing new screenshots.

## 6. Scope discipline

Only direction/alignment properties were touched (`Alignment`, `Arrangement`, `horizontalAlignment`, code order of Row/Column children, one `weight(fill=)` flag, one missing `Arrangement` import). No spacing, sizing, color, typography, copy, or component redesign was made anywhere. No backend files were touched. Nothing was committed.
