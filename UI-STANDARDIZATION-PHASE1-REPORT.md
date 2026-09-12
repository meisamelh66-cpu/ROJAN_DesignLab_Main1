# Customer UI Standardization — Phase 1

**Date:** 2026-09-10 · **Scope:** `SearchScreen`, `SalonListScreen`, `PublicSalonScreen`,
`SpecialistProfileScreen` (+ one enabling param on `CustomerScaffold`) · **Visual only.**
**Not committed.**

Follows `CUSTOMER-UI-COMPONENT-STANDARDIZATION-AUDIT.md` (P1-1 … P1-5).

---

## Result

| Screen | Before | After | Compile | Lint |
|---|---|---|---|---|
| `SearchScreen` | `GlassBackButton` + `HeroTitle` + `HomeGlassSurface` bar/cards/skeleton + `RojanEmptyState`/`RojanErrorState` + violet `HomeColors.Glow` | `CustomerScaffold` + rose-gold `SearchField` + `RefSurface` rows + `CustomerLoadingState`/`CustomerEmptyState`/`CustomerErrorState` | ✅ | ✅ 0 findings |
| `SalonListScreen` | `GlassBackButton` + `HeroTitle` + violet `HomeTextField` + `HomeGlassSurface` chips + `PremiumCardShell`/`HomeGlassSurface` cards + `RojanEmptyState`/`RojanErrorState` + `HomeColors.Glow` | `CustomerScaffold` + rose-gold `SearchField` + quiet `FilterPill`s + `RefSurface` cards + `Customer*State` | ✅ | ✅ 0 findings |
| `PublicSalonScreen` | `GlassBackButton` + `HeroTitle` + `HomeGlassSurface` cards + `RtlInfoRow`/`RtlListRow`/`RtlSectionHeader` + violet price + `PremiumButton` + `RojanLoadingState`/`RojanErrorState` | `CustomerScaffold` (+ pinned `RefPrimaryButton` in `bottomBar`) + `RefSurface`/`RefListRow` + `CustomerSectionLabel` + `Customer*State` | ✅ | ✅ 0 findings |
| `SpecialistProfileScreen` | `GlassBackButton` + `HeroTitle` + `HomeGlassSurface` + `RtlListRow`/`RtlSectionHeader` + `RojanSoftLavender`/… avatar tint + `RojanLoadingState`/`RojanErrorState` | `CustomerScaffold` + `RefSurface`/`RefListRow` + `CustomerSectionLabel` + `Customer*State` | ✅ | ✅ 0 findings |

- `:app:compileCustomerDevDebugKotlin` → **BUILD SUCCESSFUL** (EXIT 0)
- `:app:lintCustomerDevDebug` → **BUILD SUCCESSFUL** (`abortOnError = true` → 0 errors; **0 new lint findings** attributable to the four changed files or `CustomerScaffold`)
- `:app:assembleCustomerDevDebug` → APK produced
- **A72 device test:** ✅ PASS (reconnected; see §5) — no crashes (`0 FATAL EXCEPTION` for the session)

---

## What was replaced

| Removed (all four screens) | Replaced with |
|---|---|
| `HomeBackgroundTheme` + `GlassBackButton` orb + `HeroTitle` shell | `CustomerScaffold(title, onBackClick)` — flat 56dp top bar, RTL back arrow, 1px hairline |
| `HomeGlassSurface` cards (metallic border / corner sparkle / glow) | `RefSurface` — flat 4.5%-white lift, 1px 9%-white hairline, 14dp radius |
| `PremiumButton` (magenta→pink gradient pill) | `RefPrimaryButton` — solid rose-gold `#E0A67A`, 12dp radius, 52dp |
| `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` (glass cards) | `CustomerLoadingState` (flat pulsing skeleton) / `CustomerEmptyState` / `CustomerErrorState` — centred, outlined icon, `RefPrimaryButton` action |
| `HomeColors.Glow` (`#7C4DFF` violet) as spinner / cursor / chip / follow-favourite tint | `CustomerAccent` (`#E0A67A` rose-gold) — the single accent |
| `RojanSoftLavender` / `RojanAquaMint` / `RojanBlushPink` / `RojanPearlPink` per-salon / per-avatar tile tints | flat `CustomerSurfaceFill` behind the logo / avatar |
| `RtlInfoRow` / `RtlListRow` / `RtlSectionHeader` (legacy RTL primitives) | `RefListRow` + `CustomerSectionLabel` |
| filled `Icons.Filled.*` (Search, Storefront, Favorite, NotificationsActive, KeyboardArrowLeft) | outlined `Icons.Outlined.*` / `Icons.AutoMirrored.Outlined.*`, one family |
| `HomeTextField` (violet focus-glow input, `SalonListScreen`) | screen-local `SearchField` — flat surface, rose-gold cursor |
| `rojanEnterAnimation` per-item stagger, `rojanPressedShadow`, `PremiumCardShell` | none (the redesigned reference screens do not animate list items) |
| `RojanTypography.HeroTitle` (32sp) page titles | `CustomerScaffold` `Body` top-bar title; `Display.copy(26sp)` for the salon / specialist name header |

Per-screen notes:

- **PublicSalonScreen** — the "ورود و رزرو نوبت" CTA moved from an inline list item to a **pinned
  `bottomBar`** (shown only in the `Success` state), same `onLoginClick`. Contact info (address /
  phone) became one divided `RefSurface` card. Specialists / services became `RefListRow`s in
  `RefSurface` cards under `CustomerSectionLabel`s. The old `salonAccentColorFor` logo tint and
  `RojanIconContainer` fallback are gone.
- **SearchScreen** — the "نتایج (N)" count is now shown above the list in the `Success` state only
  (previously it also rendered "نتایج (0)" over the empty / error states). Result rows are individual
  `RefSurface` cards.
- **SalonListScreen** — the two filter chips ("همه" / "نزدیک من") were **kept** (removing a control
  is its own visible change; "نزدیک من" is still the same documented no-op it always was) and
  restyled as quiet rose-gold-on-select pills.
- **SpecialistProfileScreen** — the screen-local `SpecialistProfileScaffoldState` helper (a
  `GlassBackButton` over a centred state card) was deleted; `CustomerScaffold` + `Customer*State`
  replace it for every non-`Success` state.

---

## Files changed

| File | Change |
|---|---|
| `screens/search/SearchScreen.kt` | full visual rewrite (see above) |
| `screens/booking/SalonListScreen.kt` | full visual rewrite |
| `screens/salon/PublicSalonScreen.kt` | full visual rewrite |
| `screens/specialist/SpecialistProfileScreen.kt` | full visual rewrite |
| `screens/customer/components/CustomerScaffold.kt` | **+1 param** `showBackButton: Boolean = true` (and the top bar renders a same-size spacer instead of the back arrow when `false`). Additive; all 15 existing callers use named arguments and are unaffected. Needed because `RojanNavGraph` calls `SalonListScreen(showBackButton = false, …)` for the Home-rooted `MEMBER_SALONS_LIST` entry, which must not gain a dead back arrow. |

`git diff --stat`: **+730 / −607** across the 4 screens; `CustomerScaffold.kt` **+8 / −4**.

---

## Rules compliance

| Rule | Status |
|---|---|
| Visual only | ✅ — no logic branch changed; states map 1:1 (`Loading`/`Empty`/`Error`/`Success` → same components' state renderers) |
| No ViewModel changes | ✅ — `SalonListViewModel`, `PublicSalonViewModel`, `SpecialistProfileViewModel` and their factories are byte-identical; every `viewModel(...)` block unchanged |
| No API changes | ✅ — no repository / `*Api` / DTO touched |
| No navigation changes | ✅ — `RojanNavGraph` not touched; every screen's public composable signature is **identical** (`SearchScreen`, `SalonListScreen` incl. `selectedServiceIds` / `showBackButton` / `onBusinessLoginClick`, `PublicSalonScreen`, `SpecialistProfileScreen`) |
| Preserve all callbacks | ✅ — `onBackClick`, `onSalonClick`/`onSalonSelected`, `onLoginRequired`, `onLoginClick`, `onServiceClick`, `onBusinessLoginClick` (guard + action preserved), `viewModel::retry`, `viewModel.load`/`loadMore`, the debounce `LaunchedEffect`, `listState.scrollToItem(0)`, and the `LifecycleResumeEffect` stale-401 retry are all called exactly where they were |
| Compile + lint | ✅ (see §Result) |
| Test on A72 | ✅ PASS — Search, Salon List, Specialist Profile walked on-device; booking regression clean; 0 crashes (see §5) |
| Report | this file |
| Commit | not done |

---

## 5. Device test — A72 (SM-A725F, Android 14) — PASS

The A72 reconnected. Fresh APK installed (`adb install -r`). Tested as a guest and then
(OTP-logged-in) as +989164987585. **0 `FATAL EXCEPTION`** across the whole session.

| Screen | How reached | Result |
|---|---|---|
| **Search** | Explore → tap the search bar | ✅ Flat 56dp "جستجو" top bar + RTL back arrow; flat search field, **rose-gold cursor** confirmed, outlined search icon, no violet. Results render as flat `RefSurface` rows (name right-anchored, chevron left, logo/storefront on right). Debounced query → `CustomerEmptyState` ("نتیجه‌ای یافت نشد" + `SearchOff` icon); clear → 5 results back. `01_search_empty.png`, `02_search_typed.png` |
| **Salon List** (`MEMBER_SALONS_LIST`) | Home → "رزرو نوبت" | ✅ "انتخاب سالن" top bar with **no back arrow** (`showBackButton = false` honoured — the enabling param works). Flat search field; **"همه" filter pill = solid rose-gold, "نزدیک من" = flat outline**; `RefSurface` salon cards; guest → `GET /api/v1/public/salons` 200. `04_salonlist_noback.png` |
| **Salon List** (`SALON_LIST`, with back arrow) | same composable, `showBackButton = true` (default) | ✅ verified by construction — identical `CustomerScaffold` default path; the back arrow renders on every other Phase-1 screen using the default (Search / Specialist Profile confirmed on-device) |
| **Specialist Profile** | Salon Detail (authed) → "Pilot Specialist" | ✅ "متخصص" top bar + back arrow; centred 88dp avatar on `CustomerSurfaceFill`+hairline; name in `Display` 26sp; bio in a flat `RefSurface`; "خدمات قابل رزرو" `CustomerSectionLabel` + one `RefListRow` ("Haircut" / "۳۰ دقیقه" / chevron). `GET .../specialists/{id}` + category/service fan-out all 200. `05_specialist_profile.png` |
| **Specialist Profile → onServiceClick** | tap the "Haircut" row | ✅ navigates to Service Details — the `RefListRow` `onClick` callback is preserved and functional |
| **Public Salon** | route `PUBLIC_SALON` | ⚠️ **not walked on-device** — the route exists but **nothing in the current app navigates to it** (`RojanDestinations.publicSalon(slug)` has no call site; the QR-scan / deep-link entry point is unbuilt). Verified by compile + lint (0 findings) and by component-equivalence: it uses the exact `CustomerScaffold` + `RefSurface` + `RefListRow` + `CustomerSectionLabel` + `RefRowDivider` + `Customer*State` primitives confirmed on-device on the other three screens, plus `bottomBar` (confirmed on `AppointmentDetailsScreen` in a prior pass). |
| **Booking regression** | Service Details → "رزرو این خدمت" | ✅ still flows to "انتخاب ساعت" (time selection) with live slots; no crash |
| **RTL / dark / states** | all tested screens | ✅ back arrow right, chevrons left, Persian right-anchored; flat dark surfaces, no glass/glow/sparkle anywhere; empty state flat and centred |

Screenshots: `docs/device-verification/ui-phase1/`.

Minor note: `SpecialistAvatar`'s built-in fallback-icon tint reads faintly maroon (it is an existing
image primitive, deliberately not touched this phase — not a "violet/magenta legacy color" in the
`HomeColors.Glow` sense).

---

## 6. Follow-ups (not in this phase)

- `SearchField` is now duplicated (near-identical) in `SearchScreen.kt` and `SalonListScreen.kt` —
  promote to a shared `CustomerTextField` (audit item **P1-6**).
- `SearchScreen` / `SalonListScreen` still import `androidx.lifecycle.compose.LifecycleResumeEffect`
  fully-qualified inline (carried over verbatim to avoid touching behaviour).
- `SalonListScreen.selectedServiceIds` is an unused parameter (kept — public signature; it was only
  ever referenced in a doc comment). Compiler emits an unused-parameter warning.

Nothing was committed.
