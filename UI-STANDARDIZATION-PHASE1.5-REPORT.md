# Customer UI Standardization — Phase 1.5

**Date:** 2026-09-10 · **Scope:** `FavoritesScreen`, `FollowedSalonsScreen`, `BeautyDnaScreen` ·
**Visual only.** **Not committed.**

Follows `CUSTOMER-UI-COMPONENT-STANDARDIZATION-AUDIT.md` (P1-4, P2-4) and
`UI-STANDARDIZATION-PHASE1-REPORT.md`.

---

## Result

| Screen | Before | After | Compile | Lint |
|---|---|---|---|---|
| `FavoritesScreen` | `HomeBackgroundTheme` + `GlassBackButton` + `HeroTitle` + `HomeGlassSurface`/`RtlListRow` cards + violet `HomeColors.Glow` heart + `rojanEnterAnimation` + `RojanLoadingState`/`RojanEmptyState`/`RojanErrorState` | `CustomerScaffold` + `RefSurface` + `RefListRow` (rose-gold outlined heart) + `CustomerLoadingState`/`CustomerEmptyState`/`CustomerErrorState` | ✅ | ✅ 0 findings |
| `FollowedSalonsScreen` | same set (bell instead of heart) | same target (rose-gold outlined bell) | ✅ | ✅ 0 findings |
| `BeautyDnaScreen` | `HomeBackgroundTheme` + `GlassBackButton` + `HeroTitle` + `RtlSectionHeader`s + per-option `HomeGlassSurface`/`RtlListRow` toggle rows | `CustomerScaffold` + `CustomerSectionLabel` section labels + one divided `RefSurface` per option group + `RefListRow` rows with a rose-gold check when selected | ✅ | ✅ 0 findings |

- `:app:compileCustomerDevDebugKotlin` → **BUILD SUCCESSFUL** (EXIT 0)
- `:app:lintCustomerDevDebug` → **BUILD SUCCESSFUL** — **0 findings** on the 3 changed files (lint total 94, unchanged from the pre-Phase-1.5 baseline)
- `:app:assembleCustomerDevDebug` → APK produced
- **A72 device test:** ✅ PASS — all 3 screens walked; `onSalonClick` + `updateHair` callbacks fire; **0 `FATAL EXCEPTION`** for the session

---

## What was replaced

| Removed | Replaced with |
|---|---|
| `HomeBackgroundTheme` + `GlassBackButton` orb + `RojanTypography.HeroTitle` shell | `CustomerScaffold(title, onBackClick)` |
| `HomeGlassSurface` cards + `RtlListRow` / `RtlSectionHeader` | `RefSurface` + `RefListRow` + `CustomerSectionLabel` |
| `HomeColors.Glow` (`#7C4DFF` violet) heart / bell tint | `CustomerAccent` (`#E0A67A` rose-gold) |
| filled `Icons.Filled.Favorite` / `Icons.Filled.NotificationsNone` / `Icons.Filled.Check` | outlined `Icons.Outlined.FavoriteBorder` / `NotificationsNone` / `Check` |
| `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` (glass) | `CustomerLoadingState` / `CustomerEmptyState` / `CustomerErrorState` |
| `rojanEnterAnimation` per-item stagger | none (reference screens don't animate list items) |

`PremiumCardShell` / `PremiumButton` were not present in any of these three screens.
`CustomerConfirmDialog` is not used — none of the three has a destructive confirm action wired
(Favorites / Followed have no remove-from-list action in their current form; Beauty DNA has no
delete). It stays available for a later phase.

Per-screen notes:

- **Favorites / Followed Salons** — each list row is now a `RefListRow` inside a per-item
  `RefSurface` card: the salon name is right-anchored, the address is the subtitle, a rose-gold
  outlined heart / bell sits on the RTL-right (the `leading` slot), and the "leads forward" chevron
  on the RTL-left. Tap → `onSalonClick` (unchanged).
- **Beauty DNA** — the three `RtlSectionHeader`s ("مو" / "پوست" / "ناخن") became `CustomerSectionLabel`s;
  each preference group is now a small caption label above one divided `RefSurface` of `RefListRow`s.
  A selected option shows a rose-gold `Check` in its `leading` slot (no chevron). Single-select still
  deselects on re-tap; multi-select still toggles — the exact `viewModel.updateHair` /
  `updateSkin` / `updateNails` payloads are unchanged. The "این اطلاعات فقط روی همین دستگاه ذخیره
  می‌شود." disclosure note is kept, as a caption under the top bar.

---

## Files changed

| File | Change |
|---|---|
| `screens/profile/FavoritesScreen.kt` | full visual rewrite |
| `screens/profile/FollowedSalonsScreen.kt` | full visual rewrite |
| `screens/profile/BeautyDnaScreen.kt` | full visual rewrite |

`git diff --stat`: **+256 / −244** across the three.

No shared component was touched (`CustomerScaffold` already gained `showBackButton` in Phase 1;
all three screens here use the default back arrow).

---

## Rules compliance

| Rule | Status |
|---|---|
| Visual only | ✅ — states map 1:1; option-toggle semantics identical |
| No ViewModel changes | ✅ — `FavoriteSalonsViewModel`, `FollowedSalonsViewModel`, `BeautyProfileViewModel` + factories byte-identical; every `viewModel(...)` block unchanged |
| No API changes | ✅ |
| No navigation changes | ✅ — `RojanNavGraph` untouched; public composable signatures **identical** (`FavoritesScreen(onBackClick, onSalonClick, …)`, `FollowedSalonsScreen(onBackClick, onSalonClick, …)`, `BeautyDnaScreen(customerId, beautyProfileRepository, onBackClick, …)`) |
| Preserve callbacks & routes | ✅ — `onBackClick`, `onSalonClick`, `viewModel::retry`, `updateHair`/`updateSkin`/`updateNails` all called exactly where they were |
| Compile + lint | ✅ both BUILD SUCCESSFUL; 0 lint findings on the 3 files |
| Test on A72 | ✅ PASS — all 3 walked; callbacks fire; 0 crashes (see below) |
| Report | this file |
| Commit | not done |

---

## Device test — A72 (SM-A725F, Android 14) — PASS

Fresh APK installed; tested logged-in as +989164987585. **0 `FATAL EXCEPTION`** for the session.

| Screen | How reached | Result |
|---|---|---|
| **Favorites** | Profile → "علاقه‌مندی‌ها" | ✅ Flat "علاقه‌مندی‌ها" top bar + RTL back arrow. One favourite ("ROJAN AI Pilot Salon" / "Pilot Test Address, Tehran") renders as a flat `RefSurface` card: name right-anchored, address subtitle, **rose-gold outlined heart** on the RTL-right, chevron on the RTL-left. `GET /customer/favorite-salons` + `getSalon` → 200. `01_favorites.png` |
| **Favorites → onSalonClick** | tap the card | ✅ navigates to Salon Details — callback preserved |
| **Followed Salons** | Profile → "سالن‌های دنبال‌شده" | ✅ same treatment, **rose-gold outlined bell** instead of the heart. `02_followed.png` |
| **Beauty DNA** | Profile → "بیوتی دی‌ان‌ای من" | ✅ "بیوتی دی‌ان‌ای" top bar + back arrow; disclosure caption under the bar; "مو" / "پوست" / "ناخن" as `CustomerSectionLabel`s; each group = a small label above one divided `RefSurface` of options. `03_beautydna.png` |
| **Beauty DNA → select** | tap "مشکی" (رنگ مو) | ✅ a **rose-gold `Check`** appears in the row's `leading` slot; `viewModel.updateHair(...)` fired (state → re-render). `04_beautydna_selected.png` |
| **RTL / dark** | all 3 | ✅ back arrow right, chevrons left, Persian right-anchored; flat dark surfaces, no glass / glow / violet anywhere |

Screenshots: `docs/device-verification/ui-phase1.5/`.

Minor visual note: in Beauty DNA the option text hugs the RTL-right edge with empty space on the
left (the standard `RefListRow` layout with `showChevron = false` and no `trailingValue`). It reads
cleanly and RTL-consistently; a denser option layout would be a UX change, out of scope.

---

## Follow-ups (not in this phase)

- Favorites / Followed Salons have no in-row "remove" affordance — a future phase could add a
  swipe-to-remove or trailing action, at which point `CustomerConfirmDialog` applies.
- The Beauty DNA option list is long (6 groups, ~28 rows) — a later pass could collapse groups or
  move to a stepper, but that is a UX change, out of a visual-only scope.

Nothing was committed.
