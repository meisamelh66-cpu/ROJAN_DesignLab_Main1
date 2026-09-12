# ROJAN Customer App — UI Component Standardization Audit

**Date:** 2026-09-10 · **Scope:** Customer Android app only (`app/src/main/java/ai/rojan/designlab/screens/**`,
excl. `/manager/**`, `/reception/**`) · **Standard:** Quiet Luxury (golden reference
`docs/design-review/customer/REFERENCE-SPEC-salon-detail.md`) · **Audit only — no code changed, nothing committed.**

> The two named skills (`design-review`, `android-qa-audit`) are **not registered** in this
> environment — only `design-audit` is. Its methodology (read the reference spec first; walk every
> screen; apply the reduction filter; phase the findings) is applied below.

---

## 0. Score

### Standardization: **72 / 100**

| Dimension | Score | Note |
|---|---|---|
| Design-system *exists & is coherent* | 95% | `CustomerScaffold` + `CustomerRefComponents` (tokens + `RefSurface`/`RefListRow`/`RefSelectableCell`/`RefPrimaryButton`) + `CustomerStates` + `CustomerConfirmDialog` + `CustomerBottomBar` + `CustomerStepIndicator` + `CustomerBookingStatus` — all built, single accent (`#E0A67A`), 4 weights, one icon family. |
| Core journey *adoption* | 100% | Splash → Auth → Home → Explore → Salon Detail → Service → Specialist → Date → Time → Confirm → Success → Profile → Appointments → Appt Details → Reschedule — **15/15 standardized.** |
| Secondary-screen adoption | 0% | 7 real screens still on `GlassBackButton` + `HomeGlassSurface` + `Rojan*State`. |
| Placeholder-screen adoption | 0% | 7 "coming soon" screens still legacy (trivial to fix, or cut for v1). |
| Token / component *singularity* | 55% | 21 duplicated screen-local token constants; a deprecated `bookingflow/components` shim; 3 parallel state-view families; no shared text-input component. |
| Dead-code hygiene | 40% | ~15 legacy files (old Home sections + `CustomerScreenScaffold`) still in the tree, unreferenced. |

**Read:** the design system is finished and the entire booking journey (the release-critical path)
is fully on it. The remaining debt is (a) 7 secondary screens, (b) 7 placeholders, (c) system-level
consolidation + cleanup. None of it blocks the core flow; (a) is a visible regression next to the
redesigned screens.

---

## 1. Design-system component inventory

| Component | File | Status | Adopted by |
|---|---|---|---|
| `CustomerScaffold(title, onBackClick, step?, totalSteps, bottomBar?, content)` | `screens/customer/components/CustomerScaffold.kt` | ✅ canonical shell. Owns insets, applies `HomeBackgroundTheme`, flat 56dp top bar, optional step indicator + pinned bottom CTA | Auth, Appointments, Appt Details, Reschedule, Profile — and (via the shim) all 5 booking screens |
| `CustomerRefComponents` — tokens `CustomerAccent`/`CustomerOnAccent`/`CustomerScreenMargin`(20)/`CustomerCardRadius`(14)/`CustomerButtonRadius`(12)/`CustomerButtonHeight`(52)/`CustomerSurfaceFill`(W 4.5%)/`CustomerHairline`(W 9%)/`CustomerDivider`(W 7%)/`CustomerCardShape`; components `CustomerSectionLabel`, `RefSurface`, `RefRowDivider`, `RefListRow`, `RefSelectableCell`, `RefPrimaryButton`, `CustomerSkeletonRows` | `screens/customer/components/CustomerRefComponents.kt` | ✅ complete | Auth + 4 profile screens directly; Home/Explore/SalonDetail via **copied** local tokens (§4) |
| `CustomerStates` — `CustomerLoadingState`, `CustomerEmptyState`, `CustomerErrorState` | `screens/customer/components/CustomerStates.kt` | ✅ | **only** Appointments, Appt Details, Reschedule |
| `CustomerConfirmDialog(title, message, confirmLabel, onConfirm, onDismiss, dismissLabel)` | `screens/customer/components/CustomerConfirmDialog.kt` | ✅ RTL button order, no glass/glow | Appointments (cancel), Profile (logout). **No raw `AlertDialog` anywhere in Customer screens** ✅ |
| `CustomerBottomBar(modifier, activeTab, onTabSelected)` + `CustomerHomeTab` enum | `screens/customer/CustomerBottomBar.kt` | ✅ flattened to Quiet Luxury (disc/glow/sparkle/metallic-ring removed); `selectableGroup`, `Role.Tab`, Persian `stateDescription` | Home + Explore **only** (see §7) |
| `CustomerStepIndicator` | `screens/customer/components/CustomerStepIndicator.kt` | ✅ rose-gold segments | booking screens via scaffold `step` |
| `CustomerBookingStatus` — `BookingStatus.label()`, `.tint`, `StatusPill` | `screens/customer/components/CustomerBookingStatus.kt` | ✅ shared (de-duped from AppointmentsScreen) | Appointments, Appt Details |
| **Text input** | — | ❌ **missing** — no `CustomerTextField`. `AuthScreen` has a private `AuthField` (`BasicTextField`, Quiet-Luxury-styled); `SearchScreen` a separate private `BasicTextField`; `SalonListScreen` uses the violet-glow `HomeTextField` |
| `bookingflow/components/*` (`BookingScaffold`, `BookingRefComponents`, `BookingStepIndicator`) | `screens/bookingflow/components/` | ⚠️ **deprecated alias shim** — `BookingScaffold` = one-line `= CustomerScaffold(...)`; `BookingAccent = CustomerAccent`; `RefSurface` = `CustomerRefSurface`; its own KDoc says *"new code should call `CustomerScaffold` directly"* | ServiceDetails, SpecialistSelection, BookingDate, BookingTime, BookingConfirmation |

---

## 2. Screen-by-screen

Legend: **✅** on the standard · **◐** redesigned to Quiet Luxury but on screen-local primitives (sanctioned reference-impl pattern, not the shared components) · **❌** legacy chrome.

| # | Screen | Route | State | Legacy in use |
|---|---|---|---|---|
| 1 | Splash | `SPLASH` | ◐ | redesigned (monogram + rose-gold ring on dark ground); `HomeBackgroundTheme` + `painterResource` — fine |
| 2 | **Auth** | `AUTH` | ✅ | `CustomerScaffold` + shared tokens; input is a screen-local `AuthField` (P2) |
| 3 | **Home** (Dashboard) | `CUSTOMER_HOME` | ◐ | screen-local `Ref*` tokens (§4) + `CustomerBottomBar`; no legacy glass/premium/Rojan-state |
| 4 | **Explore** | `EXPLORE` | ◐ | screen-local `Ref*` tokens (§4) + `CustomerBottomBar`; `ExploreMessage` local state view |
| 5 | **Search** | `SEARCH` | ❌ | `GlassBackButton`, `HomeGlassSurface`×3, `RojanLoadingState`/`EmptyState`/`ErrorState`, private `BasicTextField`, `HomeColors.Glow` (violet) accents |
| 6 | **Salon List** | `MEMBER_SALONS_LIST` + `SALON_LIST` | ❌ | `GlassBackButton`×2, `HomeGlassSurface`×3, **`HomeTextField` (violet glow)**, `Rojan*State`×3, `HomeColors.Glow` tint on spinner/chips/icons (`:256/:279/:359/:362`) |
| 7 | **Public Salon** | `PUBLIC_SALON` | ❌ | `GlassBackButton`×2, `HomeGlassSurface`×3, **`PremiumButton`** (the only live one left), `Rojan*State`×3 |
| 8 | **Salon Details** | `SALON_DETAILS` | ◐ | the golden-reference implementation; screen-local `Ref*` primitives + tokens (§4) |
| 9 | **Service Details** | `SERVICE_DETAILS` | ✅ | `BookingScaffold` (→ `CustomerScaffold`) + `BookingRef*` |
| 10 | **Specialist Selection** | `SPECIALIST_SELECTION` | ✅ | `BookingScaffold` + `BookingRef*` |
| 11 | **Specialist Profile** | `SPECIALIST_PROFILE` | ❌ | `GlassBackButton`×3, `HomeGlassSurface`×2, `Rojan*State`×3 |
| 12 | **Booking Date** | `BOOKING_DATE` | ✅ | `BookingScaffold` + `RefSelectableCell` |
| 13 | **Booking Time** | `BOOKING_TIME` | ✅ | `BookingScaffold` + `RefSelectableCell` |
| 14 | **Booking Confirmation** | `BOOKING_CONFIRMATION` | ✅ | `BookingScaffold` + `RefSurface` + `RefPrimaryButton` |
| 15 | **Booking Success** | `BOOKING_SUCCESS` | ◐ | redesigned; `HomeBackgroundTheme` + `BookingRef*` + local |
| 16 | **Appointments** | `APPOINTMENTS` | ✅ | `CustomerScaffold` + `CustomerStates` + `CustomerConfirmDialog` + `StatusPill` |
| 17 | **Appointment Details** | `APPOINTMENT_DETAILS` | ✅ | `CustomerScaffold` + bottomBar rebook + `CustomerStates` + `StatusPill` |
| 18 | **Reschedule** | `RESCHEDULE_APPOINTMENT` | ✅ | `CustomerScaffold` + pinned bottomBar + `RefSelectableCell` + `CustomerStates` |
| 19 | **Profile** | `PROFILE` | ✅ | `CustomerScaffold` + `RefSurface`/`RefListRow` + `CustomerConfirmDialog` |
| 20 | **Favorites** | `FAVORITES` | ❌ | `GlassBackButton`×2, `HomeGlassSurface`, `Rojan*State`×3 — real data screen, **bottom-nav tab target** |
| 21 | **Followed Salons** | `FOLLOWED_SALONS` | ❌ | same set as Favorites — real data |
| 22 | **Beauty DNA** | `BEAUTY_DNA` | ❌ | `GlassBackButton`×2, `HomeGlassSurface` — 213-line real content screen |
| 23 | Beauty Timeline | `BEAUTY_TIMELINE` | ❌ placeholder | `GlassBackButton` + `RojanComingSoonState` + `RojanTypography.HeroTitle` |
| 24 | Wallet | `WALLET` | ❌ placeholder | same |
| 25 | Coupons | `COUPONS` | ❌ placeholder | same |
| 26 | Membership | `MEMBERSHIP` | ❌ placeholder | same |
| 27 | Loyalty | `LOYALTY` | ❌ placeholder | same |
| 28 | My Reviews | `MY_REVIEWS` | ❌ placeholder | same |
| 29 | Waitlist | `WAITLIST` | ❌ placeholder | same |

**Settings** — there is **no Settings screen**. Profile menu rows route straight to feature screens;
account controls (logout) live inline on Profile. Privacy-policy / notification / language / delete-account
entries do not exist (P1 for release — Play requires an in-app privacy link; see
`PRIVACY-POLICY-RELEASE-REPORT.md`).

---

## 3. Remaining legacy-component usage (live code only)

| Symbol | Live call sites | Files |
|---|---|---|
| `GlassBackButton` | 8 screens | Search, SalonList, PublicSalon, SpecialistProfile, Favorites, FollowedSalons, BeautyDna + all 7 placeholders (12 screens total, `GlassBackButton.kt:33` calls itself "the single highest-frequency clickable in the app") |
| `HomeGlassSurface` | 7 screens | Search×3, SalonList×3, PublicSalon×3, SpecialistProfile×2, Favorites×1, FollowedSalons×1, BeautyDna×1 |
| `PremiumGlassSurface` | 0 live | (only inside `HomeGlassSurface.kt` internals) |
| `PremiumButton` | **1** | PublicSalonScreen |
| `HomeTextField` (violet-glow input) | 1 | SalonListScreen |
| `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` | 7 screens | Search, SalonList, PublicSalon, SpecialistProfile, Favorites, FollowedSalons |
| `RojanComingSoonState` | 7 placeholders | Beauty Timeline, Wallet, Coupons, Membership, Loyalty, My Reviews, Waitlist |
| `RojanTypography.HeroTitle` / `ScreenTitle` / `SectionTitle` (legacy title sizes) | placeholders + legacy screens | — |
| `HomeColors.Glow` (`#7C4DFF`-family violet) as an **accent** | SalonListScreen (`:256`,`:279`,`:359`,`:362`) | spinner colour, selected filter-chip, favourite/follow icon tint |
| `PremiumBackground` / `HeroTitle` composable / sparkle overlays | **0 live** — only in KDoc comments describing what was removed | — |

**Sanctioned, not debt** (per the golden reference §2 "Kept"):
- `HomeBackgroundTheme` — the dark navy → deep-purple ground, incl. its ~20%-alpha purple radial
  glow and ~10%-alpha magenta echo (`HomeBackgroundTheme.kt:64-88`). The reference spec explicitly
  keeps this "untouched". Every screen (legacy and standardized) sits on it, so it is *consistent*.
- Rose-gold `#E0A67A` as the single accent.

---

## 4. Token & component duplication (P2)

1. **21 duplicated screen-local token constants.** `CustomerHomeScreen.kt:101-111`,
   `CustomerDashboardScreen.kt:103-114`, `SalonDetailsScreen.kt:106-120` each redeclare
   `private val RefScreenMargin = 20.dp` / `RefCardRadius = 14.dp` / `RefAccent` / `RefSurfaceFill` /
   `RefHairline` / `RefCardShape` / `RefOnAccent` — **byte-identical values** to
   `CustomerScreenMargin` / `CustomerCardRadius` / `CustomerAccent` / … in `CustomerRefComponents.kt`.
   No visual drift *today*; a token change would not propagate. → these 3 screens should
   `import` the `Customer*` tokens.
2. **`bookingflow/components/` alias shim** — `BookingScaffold` (= `CustomerScaffold`),
   `BookingRefComponents` (aliases), `BookingStepIndicator`. Collapse: repoint the 5 booking
   screens' imports to `screens/customer/components/*`, delete the shim (its own KDoc asks for this).
3. **3 state-view families**: `CustomerStates` (3 screens), the per-reference-screen
   `RefCenteredState`/`RefLoadingSkeleton`/`BookingCenteredState`/`ExploreMessage` (Home, Explore,
   SalonDetail, booking), and legacy `Rojan*State` (7 screens). Consolidate onto `CustomerStates`
   as the legacy screens migrate.
4. **2 dimension systems**: `ui/theme/Dimensions.kt` (`RojanDimens` — spacing scale ✅, but also
   legacy `ButtonHeight=64`, `HeroHeight=360`, `CardWidthStandard/Height`, `BackButtonSize`) vs the
   `Customer*` constants in `CustomerRefComponents`. Spacing tokens (`SpaceXS…SpaceXXL`) are shared
   and fine; the legacy size constants are only referenced by legacy screens.
5. **No shared `CustomerTextField`** — promote `AuthScreen.AuthField` into `customer/components/`
   and adopt it in Search + SalonList (killing `HomeTextField`).

---

## 5. Dead code — DELETE, don't migrate (P2 hygiene)

Verified **zero call sites** (grep `[^A-Za-z.]<name>\(` across `app/src/main`, excl. own file):

`screens/customer/CustomerScreenScaffold.kt`, `AISearchBar.kt`, `HomeHeader.kt`, `SearchModeTabs.kt`,
`UpcomingBookings.kt`, `RecentVisits.kt`, `FeaturedSalons.kt`, `FollowedSalons.kt` *(the section, not
the screen)*, `NearbySalons.kt`, `PopularServices.kt`, `PromotionsSection.kt`, `RecommendedSalons.kt`,
`TopSpecialists.kt`, `screens/customer/EmptyState.kt`, `screens/customer/LoadingState.kt`,
`screens/dashboard/DashboardPlaceholder.kt`.

All carry legacy `HomeGlassSurface` / `RojanComingSoonState` / glow — deleting them shrinks the
legacy-symbol footprint without any migration work. (~16 files.)

---

## 6. Layout standards

| Check | Finding | Verdict |
|---|---|---|
| **RTL** | `AndroidManifest supportsRtl="true"`; portrait-locked. `ai.rojan.designlab.ui.text.Text` (content-direction wrapper) imported in **44 / 46** screen files. `CustomerScaffold` centres the title, RTL back arrow (`AutoMirrored`). Device runs (this session + prior) show every screen laid out RTL-correct. | ✅ strong |
| **Spacing tokens** | `RojanDimens.SpaceXS…SpaceXXL` (4/8/16/24/32/48) used throughout; `CustomerScreenMargin` 20dp consistent. Reference spec's asymmetric section rhythm (label→content `SpaceSM`, content→next-label `SpaceXL`) followed on standardized screens. | ✅ (legacy screens use ad-hoc padding) |
| **Typography** | `RojanTypography`: 8 styles / **4 weights** (Bold/SemiBold/Medium/Regular) — meets the "4 real weights" rule. `includeFontPadding=false`, tuned line-heights for Persian. Overlap: `Display`(34) / `HeroTitle`(32) / `ScreenTitle`(30) — `HeroTitle`/`ScreenTitle`/`SectionTitle` are legacy, only on un-migrated screens. | ◐ consolidate on migration |
| **Margins** | 20dp screen margin consistent on standardized screens; legacy screens vary (`SpaceMD` 16 in placeholders). | ◐ |
| **Accessibility** | Interactive icons labelled (Persian: "بازگشت", "دنبال شده", "مورد علاقه", "جستجو"); 45/54 `Icon` calls are `contentDescription = null` — **correct** for decorative glyphs. Bottom bar: `selectableGroup` + `Role.Tab` + Persian `stateDescription`. `rojanPressable` gives press feedback + `Role.Button`. No TalkBack pass done on-device — recommend one before release. | ◐ good, verify on-device |
| **Touch targets** | `GlassBackButton` 48dp (bumped from 44, `:31`); `CustomerScaffold` back target `RojanDimens.MinTouchTarget` 48dp; bottom-bar tabs ≥48dp; `RefListRow` uses `heightIn(min = MinTouchTarget)`. | ✅ |
| **Dark theme** | App is **single-theme (dark editorial), always** — no `isSystemInDarkTheme()` branch, no light variant. `MaterialTheme.colorScheme` is nominally `lightColorScheme(...)` (`Theme.kt:43`) — a latent trap: any *raw* M3 component (`Button`, `TextField`, `AlertDialog`, default `CircularProgressIndicator` tint) would render light-on-dark. The app avoids raw M3 widgets almost everywhere (custom `Ref*`), but audit new code for this. | ◐ note the colorScheme trap |

---

## 7. Bottom-navigation model (P1 — UX consistency)

`CustomerBottomBar` renders on **Home + Explore only**. The bar's other three tabs —
**علاقه‌ها → `FAVORITES`**, **نوبت‌ها → `APPOINTMENTS`**, **پروفایل → `PROFILE`** — navigate to
**pushed routes with no bottom bar and a back arrow instead**. So a "tab" switch drops the tab bar
and changes the nav metaphor mid-flow. `CUSTOMER-BOTTOM-NAV-CONSISTENCY-REVIEW.md` unified the bar's
*visual* treatment but did not address this structural split. A proper `Scaffold`-with-persistent-`bottomBar`
over the 4–5 tab roots (Home, Explore, Favorites, Appointments, Profile) is the fix — larger than a
component swap; flag for a dedicated navigation task.

---

## 8. Performance / visual

| Item | Finding | Priority |
|---|---|---|
| **Coil `ImageRequest`** | `RojanRemoteImage` (`ui/components/image/RojanRemoteImage.kt:52`) — no `.size()` hint; Coil measures the target (works, but a 1080px logo decodes for a 48dp thumb on Salon-list / specialist cards). `crossfade(true)`, `ContentScale.Crop`, per-URL `failed` fallback. | P2 |
| **No custom `ImageLoader`** | No `Application` subclass → default Coil `ImageLoader`: no bounded memory/disk cache, its own separate OkHttp client. Prior device run: `TOTAL PSS ≈ 161 MB` after 70 min (Coil bitmap caches). | P1 (in `CUSTOMER-PRE-RELEASE-FINAL-AUDIT.md` §4) |
| **Recomposition** | `RojanNavGraph` is one ~1300-line composable; `authViewModel.sessionState` collected at the top → a mid-session re-emit (every OTP login) recomposes the whole `NavHost` subtree. `startDestination` correctly frozen with `remember{}`. | P1/P2 |
| **Screen-local `private val` tokens** | `Color.White.copy(alpha = …)` at file scope is computed once (top-level `val`) — fine. No per-frame allocation found. | — OK |
| **Animations** | Minimal & purposeful: skeleton alpha pulse (`CustomerRefComponents`, `SalonDetailsScreen`), OTP focus glow (`HomeTextField` — legacy), booking-success checkmark, nav page transitions (`RojanNavTransitions`, reduced-motion aware). **No gratuitous / infinite decorative animation** (the rotating metallic ring / ambient glow were removed). | ✅ |
| **Heavy images** | Salon logos only; no hero/cover imagery (backend has `logoUrl` only). Splash uses a local mipmap. | ✅ low risk |
| **Baseline Profile** | none — cold-start + first-scroll run un-AOT'd. | P1 (in PRE-RELEASE audit §4) |

---

## 9. Priority list & remaining debt

### P0 — none

The design system is built and the entire release-critical journey (Splash → booking → Success →
Profile → Appointments) is standardized. Nothing here blocks a release on *component* grounds.

### P1 — visible regression / release-adjacent

| # | Item | Files | Effort |
|---|---|---|---|
| P1-1 | Migrate **Search** to `CustomerScaffold` + `CustomerRefComponents` + `CustomerStates` + a shared text field | `screens/search/SearchScreen.kt` | ~0.5 d |
| P1-2 | Migrate **Salon List** (booking-funnel entry; last legacy step in an otherwise-standardized flow) — also kills the last `HomeTextField` + `HomeColors.Glow` accents | `screens/booking/SalonListScreen.kt` | ~0.5 d |
| P1-3 | Migrate **Specialist Profile** (inside the booking funnel) | `screens/specialist/SpecialistProfileScreen.kt` | ~0.5 d |
| P1-4 | Migrate **Favorites** + **Followed Salons** (bottom-nav tab targets; real data) | `screens/profile/FavoritesScreen.kt`, `screens/profile/FollowedSalonsScreen.kt` | ~0.5 d |
| P1-5 | Migrate **Public Salon** (shared-link first impression) — kills the last `PremiumButton` | `screens/salon/PublicSalonScreen.kt` | ~0.5 d |
| P1-6 | Add a shared **`CustomerTextField`** (promote `AuthScreen.AuthField`); adopt in Search + SalonList | new `screens/customer/components/CustomerTextField.kt` + the two screens | ~0.25 d |
| P1-7 | **Bottom-nav model** — persistent bar over the tab roots (Home/Explore/Favorites/Appointments/Profile) | `RojanNavGraph.kt` + a shell composable | ~1 d (nav task) |
| P1-8 | **Coming-soon screens — product decision:** cut the menu entries for v1 and delete the 7 screens, **or** one shared `ComingSoonScreen(title)` on `CustomerScaffold` + `CustomerEmptyState` routed for all | `screens/profile/{Wallet,Coupons,Membership,Loyalty,MyReviews,Waitlist,BeautyTimeline}Screen.kt`, `ProfileScreen.kt` menu lists | ~0.25 d |

### P2 — consolidation & hygiene

| # | Item | Files |
|---|---|---|
| P2-1 | De-duplicate the 21 screen-local `Ref*` token constants → import `Customer*` | `CustomerHomeScreen.kt`, `CustomerDashboardScreen.kt`, `SalonDetailsScreen.kt` |
| P2-2 | Collapse the `bookingflow/components/` alias shim; repoint 5 booking screens to `customer/components/*` | `screens/bookingflow/components/*` (delete), `BookingConfirmationScreen`, `BookingDateScreen`, `BookingTimeScreen`, `SpecialistSelectionScreen`, `ServiceDetailsScreen` |
| P2-3 | Delete ~16 dead legacy files (§5) | `CustomerScreenScaffold.kt`, `AISearchBar.kt`, `HomeHeader.kt`, `SearchModeTabs.kt`, `UpcomingBookings.kt`, `RecentVisits.kt`, `FeaturedSalons.kt`, `customer/FollowedSalons.kt`, `NearbySalons.kt`, `PopularServices.kt`, `PromotionsSection.kt`, `RecommendedSalons.kt`, `TopSpecialists.kt`, `customer/EmptyState.kt`, `customer/LoadingState.kt`, `dashboard/DashboardPlaceholder.kt` |
| P2-4 | Migrate **Beauty DNA** (real content, lower traffic) | `screens/profile/BeautyDnaScreen.kt` |
| P2-5 | Consolidate `CustomerStates` as the one state-view family; retire per-screen `RefCenteredState`/`ExploreMessage`/`Rojan*State` as screens migrate | — |
| P2-6 | Retire legacy typography (`HeroTitle`/`ScreenTitle`/`SectionTitle`) + legacy `RojanDimens` size constants once no screen references them | `ui/theme/Type.kt`, `ui/theme/Dimensions.kt` |
| P2-7 | Coil `.size()` hints on the known thumb slots; custom bounded `ImageLoader` via an `Application` subclass (also unblocks crash-reporting init — see PRE-RELEASE audit) | `RojanRemoteImage.kt`, new `RojanApplication.kt` |
| P2-8 | `MaterialTheme.colorScheme` is `lightColorScheme` on a permanently-dark app — either switch to `darkColorScheme` with the real tokens, or add a lint note; audit for raw M3 widgets | `ui/theme/Theme.kt` |

---

## 10. Recommended migration order

1. **P1-6** — `CustomerTextField` first (unblocks Search + SalonList cleanly).
2. **P1-2 Salon List** — completes the booking funnel (everything else in it is already standard).
3. **P1-3 Specialist Profile** — same funnel.
4. **P1-1 Search** — bottom-nav tab, high traffic.
5. **P1-4 Favorites + Followed Salons** — bottom-nav tabs.
6. **P1-5 Public Salon** — kills the last `PremiumButton`.
7. **P2-3 delete dead code** + **P2-2 collapse the shim** + **P2-1 de-dupe tokens** — one cleanup pass, now that the legacy symbols have few real users.
8. **P1-8 coming-soon decision** (product) → then **P2-4 Beauty DNA**.
9. **P1-7 bottom-nav model** — dedicated navigation task.
10. **P2-5/6/7/8** — final consolidation + the perf/theme items (fold P2-7's `Application` subclass in with crash-reporting from `CUSTOMER-PRE-RELEASE-FINAL-AUDIT.md`).

Per-screen validation for each migration (the pattern the 15 standardized screens followed):
`:app:compileCustomerDevDebugKotlin` → `:app:installCustomerDevDebug` → `:app:lintCustomerDevDebug`
→ A72 spot-check. Preserve every ViewModel, callback, route, and API call.

**Estimated total: ~5–6 engineering days** for all P1 + the P2 cleanup pass (excl. P1-7 nav task).

---

Nothing in this audit was implemented. No commits made.
