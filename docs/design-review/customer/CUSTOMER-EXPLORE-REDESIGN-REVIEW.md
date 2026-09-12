# Customer Explore / Salon-Discovery — Redesign Review

**Date:** 2026-09-09 · **Scope:** the Customer Explore / salon-discovery screen only.
**Reference:** the approved *quiet luxury / dark editorial / Persian-first* language — `REFERENCE-SPEC-salon-detail.md` (Golden Reference), `REFERENCE-SPEC-customer-home.md`, and the shared `CustomerBottomBar`.
**Not committed.**

---

## 1. Inspection (done before editing)

### 1.1 Which screen the "جستجو" tab uses

`RojanNavGraph` routes:
- **`RojanDestinations.EXPLORE` → `CustomerHomeScreen`** (`screens/customer/CustomerHomeScreen.kt`). This is the bottom bar's **جستجو** destination (from Home: `CustomerHomeTab.SEARCH → onExploreClick() → navigate(EXPLORE)`) **and** the guest landing screen (NavHost start for logged-out users). The nav-graph comment states it verbatim: *"the Dashboard's 'جستجو' tab destination."* Its `CustomerBottomBar` shows `bottomBarActiveTab = if (isLandingEntry) HOME else SEARCH`.
- `RojanDestinations.SEARCH → SearchScreen` (`screens/search/SearchScreen.kt`) is a **secondary** route inside `BOOKING_FLOW_GRAPH` — a live debounced salon-name search, reached from the search affordance on Explore/Home. It has its own `GlassBackButton`, no bottom bar.

**Confirmed target (user-selected): `CustomerHomeScreen.kt` (route EXPLORE)** — "the Customer bottom navigation discovery destination." `SearchScreen.kt` is explicitly out of scope.

### 1.2 Existing data flow

`CustomerHomeScreen` composed 13 sections. Real backend data came from:
- `FeaturedSalons` → `SalonListViewModel` (`GET /api/v1/salons`, page 0, ~20 salons) — the only genuine discovery data.
- `UpcomingBookings` / `RecentVisits` → `BookingHistoryViewModel` (`GET /api/v1/bookings/mine`) — activity, not discovery.

Everything else was placeholder / non-functional:
- `AISearchBar` — a glass button; its only job is `onClick` → navigate to `SearchScreen`.
- `SearchModeTabs` (سالن‌ها / خدمات) — toggled a local `searchMode` var that **nothing reads**; purely decorative.
- `PopularServices`, `TopSpecialists`, `PromotionsSection`, `NearbySalons`, `RecommendedSalons`, `FollowedSalons` — all render `RojanComingSoonState` ("به‌زودی"); no backend behind any of them.
- `HeroBookingCard` — 360dp glass card, AI salon photo, gradient pill; `onClick` → `MEMBER_SALONS_LIST`.

### 1.3 Visual language vs. the approved references

| | Explore (before) | Golden Reference / redesigned Home |
|---|---|---|
| Surfaces | `HomeGlassSurface` + metallic gold border + ✦ corner sparkles, 32dp radius | flat `RefSurface`, 4.5% fill + 9% hairline, 14dp |
| Header | `HomeHeader` — glass chip, blurred glow, `RojanAmbientGlow`, filled-Person avatar | flat title + hairline |
| Search | glass button with a blurred `HomeColors.Glow` wash behind it | flat `RefSurface` search entry |
| Cards | tinted-glass `HomeCard` (`salonAccentColorFor`), 90dp storefront band | flat card, storefront tile, name/desc/address, chevron |
| Accent | violet `#7C4DFF` (`HomeColors.Glow`) everywhere | rose-gold `#E0A67A`, controlled |
| Imagery | AI candy-pink salon photo in the hero | none |
| Buttons | magenta→pink gradient pill | solid rose-gold, 12dp (or none) |
| Bottom nav | shared `CustomerBottomBar` — **already redesigned flat** in the prior task | ✓ consistent |

### 1.4 Files that would be modified

`app/src/main/java/ai/rojan/designlab/screens/customer/CustomerHomeScreen.kt` — **only this file.**
The now-unused section composables (`AISearchBar`, `SearchModeTabs`, `PopularServices`, `PromotionsSection`, `NearbySalons`, plus the shared `HeroBookingCard` / `TopSpecialists` / `RecommendedSalons` / `FollowedSalons` / `FeaturedSalons` / `UpcomingBookings` / `RecentVisits`) are **not deleted** — just no longer composed here. `CustomerBottomBar` is used unchanged.

---

## 2. Files modified

| File | Change |
|---|---|
| `screens/customer/CustomerHomeScreen.kt` | **Rewritten.** Public `CustomerHomeScreen(...)` signature (11 params incl. `bottomBarActiveTab`) and the `RojanNavGraph` wiring are byte-identical. All new visual code is screen-local `private` (`Ref*` tokens + `Explore*` composables), mirroring the Golden Reference. |

**No other file touched.** Working tree `.kt`: this file + `CustomerBottomBar.kt` / `CustomerDashboardScreen.kt` / `SalonDetailsScreen.kt` from the earlier (still-uncommitted) tasks.

**Confirmed NOT changed:** ViewModels, repositories, domain, API/network, auth, navigation graph, routes, backend contracts, business logic, data models. `SalonListViewModel` is used exactly as `FeaturedSalons` used it (`SalonListViewModelFactory(container.salonRepository)`), no new params, no new calls.

## 3. Visual changes — Before → After

| Element | Before | After |
|---|---|---|
| **Composition** | 13 stacked sections (2 real, 11 placeholder/decorative/activity) | 4: header · search · "سالن‌ها" label · salon list (+ its states) |
| **Header** | `HomeHeader` glass chip: blurred glow wash, `RojanAmbientGlow` behind a filled-Person avatar, `KeyboardArrowDown`, a bell wired to nothing | flat `ExploreHeader`: "کشف سالن‌ها" (`SectionTitle`) + "سالن مناسب خود را پیدا کنید" (`Caption`, muted) + a 40dp outlined-Person avatar → profile · 1px bottom hairline |
| **Search** | `AISearchBar` — `HomeGlassSurface` (metallic border + sparkles) + a 16dp-blur `HomeColors.Glow` glow layer behind it | flat `ExploreSearchBar`: 14dp `RefSurface`, 4.5% fill + hairline, outlined `Search` glyph, "جستجوی سالن، خدمت یا متخصص…". Tap → `SearchScreen` (**behaviour unchanged**) |
| **Mode tabs** | `SearchModeTabs` gradient pills (سالن‌ها / خدمات) that controlled nothing | removed (dead UI) |
| **Discovery list** | `FeaturedSalons` horizontal rail of tinted-glass cards, buried at position 7 of 13, below 3 "به‌زودی" sections | a single **vertical** list of `ExploreSalonCard`, immediately below the search — the screen's primary content. Real `GET /api/v1/salons`, ~12 salons on device |
| **Salon card** | glass + gold metallic border + ✦ sparkles, 32dp radius, a 90dp band filled by a large centred filled `Storefront`, name/address crammed under it | flat 14dp `RefSurface` + 1px hairline · 48dp rounded-12 logo tile (real `logoUrl` when present, else outlined storefront glyph) · **`CardTitle` (20/SemiBold) salon name** · description line (`Caption`) · `Place` + address row (`Caption` muted) · trailing `KeyboardArrowLeft` chevron. No glow / sparkle / metallic / gradient. |
| **Hero CTA** | 360dp `HeroBookingCard`: 2 gradient tint layers + blurred glow twin, AI candy-pink salon photo w/ white border, magenta→pink gradient pill | removed — the salon cards are the discovery-and-booking path (tap → Salon Detail). `onBookAppointmentClick` no longer surfaced here. |
| **Placeholder sections** | `PopularServices`, `TopSpecialists`, `PromotionsSection`, `NearbySalons`, `RecommendedSalons`, `FollowedSalons` — 6 "به‌زودی" cards | all removed from the composition |
| **Activity sections** | `UpcomingBookings`, `RecentVisits` on the discovery screen | removed here (they live on Home, self-hiding — real data unaffected) |
| **Loading** | `RojanLoadingState` glass card (from `FeaturedSalons`) | 4 flat dimmed skeleton cards |
| **Empty / error** | `FeaturedSalons` rendered nothing on non-success; guests saw "به‌زودی" cards | screen-local `ExploreMessage` — centred outlined icon (`SearchOff` / `CloudOff`), near-white title, muted body, and a solid rose-gold "تلاش مجدد" → `viewModel.retry()` on error. Shared `RojanEmptyState` / `RojanErrorState` left untouched. |
| **Accent** | violet `#7C4DFF` on the search icon, glow washes, section headers | rose-gold `#E0A67A`, only on the retry button and (via the shared bar) the active tab |
| **Bottom nav** | shared `CustomerBottomBar` (already flat from the prior task) | unchanged; **جستجو active** via `bottomBarActiveTab` |
| **Page margins** | `LazyColumn` `horizontal = SpaceLG` (24dp) | `RefScreenMargin` (20dp) per-item, matching Home / Salon Detail |

## 4. Backend / data / navigation unchanged — confirmation

- **Data:** `SalonListViewModel` instantiated with the same factory + repository `FeaturedSalons` used. No new endpoint, no new VM param, no new call. `SalonListViewModel.init` already loads page 0; the screen just renders `state`. Real salons only — no mock, no fabricated rating/distance/reviews (backend has none).
- **Navigation:** `RojanNavGraph`'s `composable(EXPLORE) { CustomerHomeScreen(...) }` block is untouched — same 11 arguments passed. The in-screen `onTabSelected` `when(tab)` block and `activeTab = bottomBarActiveTab` are byte-identical to before.
- **Interactions preserved:** search affordance → `SearchScreen` (`onSearchClick`); salon card → Salon Detail (`onSalonClick`); profile avatar + bottom-nav tabs → their existing callbacks. `onBookAppointmentClick` / `onViewAllServicesClick` / `onSpecialistClick` remain as (now-unused) defaulted params so the nav graph needs no change.
- **No ViewModel / repository / domain / API / model file was edited.** No compile issue forced any such change.

## 5. Device / build / lint

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا", live backend `api.rojanai.ir`.

| Step | Result |
|---|---|
| `./gradlew.bat :app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `./gradlew.bat :app:installCustomerDevDebug` | `Installed on 1 device.` **BUILD SUCCESSFUL**, exit 0 |
| `./gradlew.bat :app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — **zero SARIF findings** referencing `CustomerHomeScreen.kt` |

## 6. Interaction verification (on device)

| Check | Result | Evidence |
|---|---|---|
| Explore renders correctly | PASS | `E01`, `E05` |
| Real salons appear (~12) | PASS | `E02` (scrolled: روژ, زیبا سرای حنا, هیرو, هیوا, ملک بانو…), `E05` |
| Salon list scrolls; bottom bar opaque, no bleed-through | PASS | `E02` |
| Search affordance still works → opens `SearchScreen` | PASS | `E06` (live "نتایج (12)" search) |
| Salon card clickable → opens Salon Detail | PASS | `E03` (بانوصبا) |
| "جستجو" is the active bottom-nav destination | PASS | `E01`/`E05`/`E02` — rose-gold + dot on the search tab |
| Other bottom-nav destinations route correctly | PASS | `E07` HOME→Home · `E08` BOOKINGS→Appointments · `E09` PROFILE→Profile · FAVORITES→Favorites (same `onFavoritesClick` wiring, verified in the prior bottom-nav task) |
| Filters / extra interactions | N/A | the only "filter" was the dead `SearchModeTabs`; nothing else exists on this screen |
| Crash / ANR | none |

## 7. Remaining visual issues

| # | Item | Severity | Note |
|---|---|---|---|
| EX-1 | `SearchScreen` (the live search reached from the Explore search bar) is still the old glass / gold-metallic-border / ✦-sparkle / orb-back design. | out of scope | User: *"SearchScreen.kt is a secondary search route and is not the target."* Its own redesign is a later task. |
| EX-2 | For an unauthenticated guest, `GET /api/v1/salons` returns 401 → the screen shows `ExploreMessage` ("مشکلی پیش آمد" + retry) instead of a login CTA. `CustomerHomeScreen` has no `onLoginRequired` param and adding one is a navigation change (out of scope). Behaviour is no worse than before (guests previously saw "به‌زودی" placeholder cards). | low | Flag for when guest discovery / login-gating is addressed. |
| EX-3 | No pagination — the screen renders `SalonListViewModel`'s page 0 (~20 salons), same as the old `FeaturedSalons`. `SalonListViewModel` supports `loadMore()`; wiring an infinite scroll here is a small follow-up, not a regression. | low | |
| EX-4 | `onBookAppointmentClick` / `onViewAllServicesClick` / `onSpecialistClick` are now unused params (kept for signature stability). The `MEMBER_SALONS_LIST` "browse to book" entry point is no longer on this screen — the salon cards go straight to Salon Detail instead. | judgment call | Flagged for your veto; easy to re-add a flat "رزرو نوبت" CTA (as on Home) if you want that entry point back. |
| EX-5 | `AISearchBar`, `SearchModeTabs`, `PopularServices`, `PromotionsSection`, `NearbySalons` are now orphaned (only `CustomerHomeScreen` referenced them). Left in place per the "modify only the Explore file" rule. | none | Dead-code cleanup is a later app-wide pass. |
| EX-6 | Screen-local `Ref*` tokens duplicated across the redesigned screens rather than promoted to the design system. | none | Same deferral as every prior redesign screen — promote in the app-wide phase. |

## 8. Final status

**PASS** — the Customer Explore / salon-discovery screen (route `EXPLORE`, the "جستجو" tab) is redesigned to the approved quiet-luxury reference: a clean Persian title, one flat search affordance, and a single content-first vertical list of **real** salons with calm loading/empty/error states — no dashboard, no placeholder content, no glass/glow/sparkle/metallic-border/gradient/AI-photo. All backend data, ViewModels, navigation, routes, and callbacks are unchanged and verified on the physical device. "جستجو" is the active bottom-nav destination and every other tab still routes correctly. Builds and lints clean. Not committed.

**Stop here.** No other Customer screen (Booking, Profile, Appointments, Favorites, Account, Settings, `SearchScreen`, …) was touched.
