# Customer Home — Redesign Review

**Date:** 2026-09-09 · **Scope:** Customer **Home only** (`CustomerDashboardScreen`, route `RojanDestinations.CUSTOMER_HOME`, the bottom bar's **خانه** tab).
**Direction:** adopts the approved *quiet luxury / dark editorial / Persian-first* golden reference (`REFERENCE-SPEC-salon-detail.md`) — same `Ref*` tokens, flat surfaces, single rose-gold accent, outlined icons.
**Spec:** `docs/design-review/customer/REFERENCE-SPEC-customer-home.md` (written before coding).
**Not committed** — awaiting review.

---

## 1. Files changed

| File | Change |
|---|---|
| `app/src/main/java/ai/rojan/designlab/screens/customer/CustomerDashboardScreen.kt` | **rewritten** — presentation only. Public `CustomerDashboardScreen(...)` signature and all 8 `on*` callbacks byte-identical. All new visual code is screen-local `private` (`Ref*` tokens + `Home*` composables). |
| `docs/design-review/customer/REFERENCE-SPEC-customer-home.md` | new — the visual spec. |
| `docs/design-review/customer/HOME-REDESIGN-REVIEW.md` | new — this report. |
| `docs/design-review/customer/H0*.png`, `H01*.png`, `H02*.png`, `H07*.png` | new — device evidence. |

**Confirmed NOT changed:** no ViewModel, no repository, no domain model, no API/network, no navigation graph (`RojanNavGraph`), no auth, no backend contract, no `AndroidManifest`, no Gradle. No shared component, design-system token, colour, or icon file. `CustomerHomeScreen` (route `EXPLORE`) and every shared section it uses (`HomeHeader`, `HeroBookingCard`, `FeaturedSalons`, `AISearchBar`, `PromotionsSection`, `CustomerBottomBar`, …) are untouched — they still render exactly as before on that screen. The instrumentation test `HomeHeaderSemanticsTest` targets `HomeHeader` directly and is unaffected.

Only two `.kt` files are modified in the working tree: this one, and `SalonDetailsScreen.kt` from the prior (still-uncommitted) reference-screen task.

## 2. Visual changes

| Area | Before | After |
|---|---|---|
| **Header** | `HomeHeader` — glass app-bar chip, 22dp blurred glow wash, `RojanAmbientGlow` behind a filled-Person avatar, `KeyboardArrowDown`, a notifications bell wired to nothing on this route | flat greeting row: "سلام {نام} جان" (`SectionTitle`) + a 40dp outlined-Person avatar button → profile. 1px bottom hairline. No glass, no glow, no bell. |
| **Primary action** | `HeroBookingCard` — 360dp card, 2 gradient tint layers + a blurred glow twin, AI candy-pink salon photo w/ white border, gradient-pill `PremiumButton` at the bottom | one full-width **solid rose-gold** button, 52dp / 12dp radius — "رزرو نوبت" (same `onBookAppointmentClick`). The primary action is now the single loudest thing on the screen, and it is quiet. |
| **Search** | `HomeGlassSurface` (metallic border + corner sparkles) | flat `RefSurface` (4.5% white fill + 9% hairline, 14dp) + outlined search glyph. Same `onSearchClick`. |
| **Suggested salons** | `FeaturedSalons` → `HomeCard`: per-salon tinted glass, a 90dp band with a giant centred filled `Storefront`, name/address crammed under it | `HomeSalonCard`: flat `RefSurface`, 200dp — small outlined storefront tile, then salon name (`Body`), then a `Place` + address meta row. Content-first. Real `GET /api/v1/salons` via the same VM/factory `FeaturedSalons` uses. |
| **Section headers** | `RtlSectionHeader` — `Body`, full-weight white | `RefSectionLabel` — `Caption`/SemiBold, muted; 8dp to its content, 24dp to the next section. |
| **Placeholder sections** | `RecommendedSalons`, `TopSpecialists`, `FollowedSalons` — each a `RojanComingSoonState` "به‌زودی" card (no backend behind any of them) | **not composed on Home.** The shared composables stay in the tree for `CustomerHomeScreen`. Home now shows zero placeholders. |
| **Upcoming / recent** | always rendered their `RtlSectionHeader` even with 0 items | section (label + rows) renders **only when it has real data**; flat `RefSurface` rows (salon · specialist? · date · time). |
| **Bottom nav** | `CustomerBottomBar` — a 64dp filled purple Home disc protruding above the bar with a rotating metallic ring + `RojanAmbientGlow`; the other 4 icons ~20dp filled | screen-local `HomeBottomBar` — 5 equal-weight tabs, 24dp **outlined** icons on one baseline, flat bar, 1px top hairline, active = rose-gold + a 3dp dot. Same `CustomerHomeTab` enum, same `onTabSelected` wiring. Shared `CustomerBottomBar` left as-is for Explore. |
| **Palette on this screen** | pink / lavender / magenta / violet-glow / gold-metallic, ~5 gradient systems, AI photography | dark-navy ground + white-alpha surfaces + **one** accent (`#E0A67A`). No gradient, no glow, no sparkle, no photo. |

Section order after: greeting → search → **رزرو نوبت** → سالن‌های پیشنهادی → (نوبت‌های پیش‌رو) → (بازدیدهای اخیر).

## 3. Functionality preserved

- Every callback: `onProfileClick`, `onBookAppointmentClick`, `onBookingsClick`, `onFavoritesClick`, `onExploreClick`, `onSearchClick`, `onSalonClick` — all still invoked from the same places (avatar, CTA, search row, salon cards, booking rows, bottom-bar tabs).
- Real data only: greeting from `AuthViewModel.currentDisplayName` (same first-name derivation as `HomeHeader`); salons from `SalonListViewModel`; upcoming/recent from `BookingHistoryViewModel` (one `GET /bookings/mine` shared by both, as before). Nothing mocked, nothing fabricated.
- Navigation verified on device: salon card → Salon Detail (`H02`), bottom-bar **پروفایل** → Profile screen (`H07`). Bottom-bar wiring is the identical `when(tab)` block from the previous implementation.
- The three dropped sections removed **no** working feature — each had no endpoint and only drew a "coming soon" card. This directly resolves design-review finding **VIS-12** ("remove every placeholder section from the shipping build").
- Insets: `HomeGreetingRow` takes `statusBarsPadding`, `HomeBottomBar` takes `navigationBarsPadding` — same split the screen used before (`applyContentInsets = false`).

## 4. Before / After screenshots

| | Path |
|---|---|
| Before — authed Home, old dashboard (design-review capture) | `docs/design-review/customer/S04b_home_authed.png` |
| Before — old Home mid-scroll (hero promo + FeaturedSalons + placeholder + disc nav) | `docs/design-review/customer/R00h_home_top.png`, `R00j_salon_cards_visible.png` |
| **After — Home (final)** | `docs/design-review/customer/H00_home_after_final.png` |
| After — Home (first capture) | `docs/design-review/customer/H01_home_after_top.png`, `H01b_home_recheck.png` |
| After — Home → Salon Detail (nav works, consistent language) | `docs/design-review/customer/H02_home_to_salon_detail.png` |
| After — bottom-bar Profile tab navigates | `docs/design-review/customer/H07_tab_try_2150.png` |

## 5. Build result

**PASS.** `JAVA_HOME` = Temurin JDK 21.0.12.
- `./gradlew.bat :app:compileCustomerDevDebugKotlin` → **BUILD SUCCESSFUL**, exit 0.
- `./gradlew.bat :app:installCustomerDevDebug` → `Installed on 1 device.` **BUILD SUCCESSFUL in 57s**, exit 0. Package `ai.rojan.designlab`, debug-signed.

## 6. Lint result

**PASS.** `./gradlew.bat :app:lintCustomerDevDebug` → **BUILD SUCCESSFUL in 3m 31s**, exit 0. No errors. The SARIF report contains **zero findings** referencing `CustomerDashboardScreen.kt` (the rewrite introduced no new lint issues — not even an informational one).

## 7. Real-device result

**PASS.** Physical **Samsung Galaxy A72 (SM-A725F), Android 14**, logged in as the real account "گیتا", live backend `api.rojanai.ir`.

| Check | Result |
|---|---|
| Cold start → Home renders | PASS — `H00_home_after_final.png` |
| Greeting shows the real name | PASS — "سلام گیتا جان" |
| Search bar renders + is the flat surface | PASS |
| "رزرو نوبت" CTA renders (solid rose-gold) | PASS |
| "سالن‌های پیشنهادی" shows the 2 real salons | PASS — "ROJAN AI Pilot Salon" / "بانوصبا", real addresses |
| Placeholder ("به‌زودی") sections | PASS — none present |
| Upcoming / recent sections | PASS — correctly absent (this account has no bookings) |
| Bottom bar — flat, no protruding disc, outlined icons, Home active in rose-gold | PASS |
| Salon card tap → Salon Detail | PASS — `H02` |
| Bottom-bar tab → navigates (Profile) | PASS — `H07` |
| Crash / ANR | none observed |

## 8. Remaining visual issues

| # | Observation | Severity | Note |
|---|---|---|---|
| H-1 | With this account (2 salons, 0 bookings) Home has a large empty band below the salon rail. | low | Honest — Home shows only real content. The gap fills once the user has an upcoming/recent booking. Not worth adding filler. |
| H-2 | The salon skeleton is a static dimmed block (no shimmer). The salon-detail reference uses an `animateFloat` pulse. | low | Deliberate — avoids a second infinite transition on Home; the block only shows for the sub-second load. Can be aligned with the reference in the app-wide pass. |
| H-3 | The greeting `Text` sits centre-right of its slot rather than hard against the avatar. | cosmetic | The content-direction `Text` wrapper resolves alignment per string; acceptable, reads correctly RTL. |
| H-4 | The **session-restore loading screen** is still the full-bleed AI candy-pink photo (design-review **VIS-01**). | S1 — **out of scope** | Separate screen (`SplashScreen` / session restore), not Home. Flagged for its own pass. |
| H-5 | Profile, Appointments, Favorites, Explore, and the booking flow still use the old glass/sparkle/gradient language. | expected | Only Home was redesigned. The visual inconsistency between Home + Salon Detail (new) and the rest (old) is expected mid-rollout. Bottom-bar consistency specifically needs an app-wide step, since `CustomerBottomBar` is shared with `CustomerHomeScreen`. |

## 9. Final status

**PASS** — Customer Home redesigned to the approved golden reference, builds, installs, and renders correctly on the physical device with real backend data; navigation and all existing Home interactions preserved; scope held to the one screen. Not committed.

**Stop here** — Search, Booking, Profile, Account, and every other screen are explicitly out of scope for this task.
