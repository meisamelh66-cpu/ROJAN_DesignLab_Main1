# Customer Bottom Navigation — Visual Consistency Pass

**Date:** 2026-09-09 · **Scope:** Customer bottom navigation only — its visual treatment, states, placement, spacing, iconography, and consistency between Customer Home and Explore. **No destination screen redesigned.**
**Reference:** the approved *quiet luxury / dark editorial / Persian-first* language (`REFERENCE-SPEC-salon-detail.md`, `REFERENCE-SPEC-customer-home.md`).
**Not committed.**

---

## 1. Inspection (done before editing)

### 1.1 All Customer bottom-navigation implementations

| # | Implementation | Where it rendered | Notes |
|---|---|---|---|
| A | `CustomerBottomBar` (shared, public, `screens/customer/CustomerBottomBar.kt`) | `CustomerHomeScreen` (route `EXPLORE`, the **جستجو** tab) | The old treatment: a 78%-width `HomeGlassSurface` pill (metallic border + corner sparkles) with a **protruding 64dp filled purple Home disc**, a rotating `HomeMetallicRing` (sweep-gradient gold, `tween` 5500ms), a `RojanAmbientGlow` halo, and 20dp filled glyphs for the other four tabs. |
| B | `HomeBottomBar` (screen-local `private`, in `CustomerDashboardScreen.kt`) | `CustomerDashboardScreen` (route `CUSTOMER_HOME`, the **خانه** tab) | Added in the *Customer Home redesign* task: a flat bar (5 equal `weight(1f)` slots, 24dp outlined icons, active = rose-gold + a 3dp dot, 1px top hairline). |

A repo-wide search (`BottomBar|CustomerHomeTab|NavigationBar` under `app/src/main`) confirms these are the **only two**. Every other Customer destination — Profile, Appointments, Favorites, Search, the booking flow — is a **pushed route with no bottom bar** (a `GlassBackButton` / orb instead). `CustomerScreenScaffold` is a back-button wrapper with no bottom bar. `RojanNavGraph` contains **no** bottom-nav UI — it only passes `bottomBarActiveTab` to `CustomerHomeScreen` and wires per-screen `on*` callbacks.

### 1.2 Why Home used a screen-local `HomeBottomBar`

Deliberate and temporary. The *Customer Home redesign* task was explicitly scoped to "**Customer Home screen only … do not modify other screens**." `CustomerBottomBar` is shared with `CustomerHomeScreen` (Explore), so editing it there would have broken that scope. `HomeBottomBar` was a screen-local copy of the target treatment, and that task's report flagged it: *"a shared bottom-bar redesign — `CustomerBottomBar` is used by another screen; changing it there is not 'Home only'"* and listed bottom-bar consistency as needing an app-wide step. **This task is that step.**

### 1.3 Can the shared `CustomerBottomBar` become the single visual implementation without changing navigation?

**Yes — safely.** The component holds **no navigation logic**: it renders 5 tabs and raises `onTabSelected(tab: CustomerHomeTab)`; the caller decides what each tab does.
- Public signature `CustomerBottomBar(modifier, activeTab, onTabSelected)` and the `CustomerHomeTab` enum are unchanged.
- Both call sites already pass exactly `(modifier, activeTab, onTabSelected)` with their own `when(tab)` block — untouched.
- `CustomerHomeScreen` measures the bar via `onSizeChanged` and feeds the height into its list's bottom `contentPadding` — this self-corrects to the new (flat, full-width) bar's height with no code change.
- Accessibility parity kept: one `selectableGroup`, each tab a ≥ 48dp `selectable` with `Role.Tab` and a Persian `stateDescription` (فعال / غیرفعال) — same as the old bar.

### 1.4 Files that would be modified

| File | Why |
|---|---|
| `app/src/main/java/ai/rojan/designlab/screens/customer/CustomerBottomBar.kt` | Replace the disc/glass/sparkle/metallic-ring treatment with the flat quiet-luxury treatment. Signature + enum unchanged. |
| `app/src/main/java/ai/rojan/designlab/screens/customer/CustomerDashboardScreen.kt` | Delete the screen-local `HomeBottomBar` + `HomeTab` + `homeTabs`; call the now-consistent shared `CustomerBottomBar`. Prune the 7 imports that only `HomeBottomBar` used. |

`CustomerHomeScreen.kt` needs **no edit** — it already calls `CustomerBottomBar` and inherits the new look.

---

## 2. Files modified

| File | Change | Reason |
|---|---|---|
| `screens/customer/CustomerBottomBar.kt` | **Rewritten body.** `enum CustomerHomeTab` and `@Composable fun CustomerBottomBar(modifier, activeTab, onTabSelected)` are byte-identical. Removed: `HomeMetallicRing`, `RojanAmbientGlow`, the protruding 64dp Home disc, the `HomeGlassSurface` pill, `borderStrokeWidth`/`glowSpread`, all metallic-border tokens, `rememberReducedMotion`, the infinite rotation. Added: a flat `Column` = 1px top hairline + a full-width opaque `Row` of 5 `weight(1f)` `selectable` slots, 24dp outlined icons, active = `#E0A67A` + 3dp dot. | One consistent Customer bottom-nav visual; kills the "casual-game" protruding disc (design-review **VIS-06**); removes glass/sparkle/glow/gradient from the nav (**VIS-02/03/09**). |
| `screens/customer/CustomerDashboardScreen.kt` | Removed `private HomeBottomBar` (~65 lines), `private data class HomeTab`, `private val homeTabs`. Call site `HomeBottomBar(…)` → `CustomerBottomBar(…)` (identical args). Removed 7 now-unused imports (`heightIn`, `navigationBarsPadding`, `selectable`, `selectableGroup`, `ImageVector`, `Icons.Outlined.Home`, `Icons.Outlined.FavoriteBorder`). Two doc/comment lines updated. | Eliminate the duplicate competing implementation; Home now uses the shared component. |

No other file touched. Working tree: 3 modified `.kt` — the two above + `SalonDetailsScreen.kt` from the earlier (still-uncommitted) reference task.

**NOT modified:** ViewModels, repository, domain, API/network, authentication, `RojanNavGraph`, routes, backend contracts, business logic. No functionality changed.

---

## 3. Visual changes

| Property | Before (`CustomerBottomBar`, on Explore) | After (both Home + Explore) |
|---|---|---|
| Container | 78%-width floating `HomeGlassSurface` pill, metallic gradient border, corner sparkles, `SoftElevation` shadow, `glowSpread` | full-width flat bar, opaque `NavyBase` fill, **1px top hairline** at 9% white, no shadow |
| Home tab | **64dp filled purple disc protruding ~20dp above the bar**, `HomeMetallicRing` (rotating gold sweep), `RojanAmbientGlow` halo, 24dp filled glyph | same 24dp **outlined** `Home` glyph as the other four, on the same baseline |
| Other 4 tabs | 20dp filled glyphs (`Icons.Filled.*`) | 24dp **outlined** glyphs (`Icons.Outlined.Person / FavoriteBorder / CalendarMonth / Search`) |
| Active state | `HomeColors.Glow` violet `#7C4DFF` tint | rose-gold `#E0A67A` tint **+ a 3dp filled dot** under the icon |
| Inactive state | `HomeColors.TextSecondary` (`#CBBEE0`) | `HomeColors.TextMuted` (`#9C8FB5`) — quieter, clearer active/inactive delta |
| Icon size consistency | mixed (disc 24dp, others 20dp) | **all 24dp, one baseline** |
| Touch targets | disc 64dp; others ~48dp overflow boxes | all 5 = `weight(1f)` × `heightIn(min = 48dp)`, equal |
| Placement | floats, doesn't touch screen edges; disc pokes into content | pinned to the bottom edge, full width, `navigationBarsPadding` inside the fill so it extends behind the system nav |
| Vertical alignment | disc baseline ≠ other icons' baseline | all icons vertically centered on one line |
| Motion | continuous ring rotation (unless reduced-motion) | none |
| RTL order | Profile → Search (Home centred) | **unchanged** — same order, Home stays centred |
| Accent usage | violet on active + gold on the ring (always) | rose-gold, **active state only** |

**Consistency achieved:** Home and Explore now render pixel-identical bottom navigation (same component, same tokens). The only difference is which tab is active (`HOME` vs `SEARCH`).

## 4. Navigation behaviour preserved

Verified on device — every tab still routes exactly as before (the `when(tab)` blocks in both screens are untouched):

| Tab | From Home (`CustomerDashboardScreen`) | From Explore (`CustomerHomeScreen`) |
|---|---|---|
| خانه (HOME) | no-op (already Home) | → `CUSTOMER_HOME` |
| جستجو (SEARCH) | → `EXPLORE` (`onExploreClick`) | → `SEARCH` / no-op per landing context |
| نوبت‌ها (BOOKINGS) | → `APPOINTMENTS` | → `APPOINTMENTS` |
| علاقه‌ها (FAVORITES) | → `FAVORITES` | → `FAVORITES` |
| پروفایل (PROFILE) | → `PROFILE` | → `PROFILE` |

Device walkthrough: Home → **جستجو** → Explore (`N02`), Explore scroll (bar stays opaque, content behind it — `N02b`), Explore → **خانه** → Home (`N03`), Home → **علاقه‌ها** → Favorites screen (`N04`), Home → **نوبت‌ها** → Appointments screen (`N05`), Home → **پروفایل** → Profile screen (`N06`). All correct; back button returns as before. No crash.

Active/inactive states verified: on Home the **Home** icon is rose-gold + dot, others muted (`N01`, `N03`); on Explore the **Search** icon is rose-gold + dot, others muted (`N02`, `N02b`).

## 5. Screenshots

`docs/design-review/customer/`

| File | Shows |
|---|---|
| `N01_home_bottomnav.png` | Home — new flat bar, **خانه** active (rose-gold + dot) |
| `N02_explore_bottomnav.png` | Explore — **same** flat bar, **جستجو** active. The protruding disc is gone. |
| `N02b_explore_scrolled.png` | Explore scrolled — bar is opaque, content passes cleanly behind it |
| `N03_back_to_home.png` | **خانه** tap from Explore returns to Home |
| `N04_favorites.png` | **علاقه‌ها** → Favorites screen (pushed route, no bottom bar — unchanged) |
| `N05_bookings.png` | **نوبت‌ها** → Appointments screen |
| `N06_profile.png` | **پروفایل** → Profile screen |

Before (old disc bar) for comparison: `R00j_salon_cards_visible.png`, `S04b_home_authed.png`, `S07b_salon_detail_full.png` (all from the earlier reviews).

## 6. Build result

**PASS.** `JAVA_HOME` = Temurin JDK 21.0.12.
- `./gradlew.bat :app:compileCustomerDevDebugKotlin` → **BUILD SUCCESSFUL**, exit 0.
- `./gradlew.bat :app:installCustomerDevDebug` → `Installed on 1 device.` **BUILD SUCCESSFUL**, exit 0.

## 7. Lint result

**PASS.** `./gradlew.bat :app:lintCustomerDevDebug` → **BUILD SUCCESSFUL**, exit 0, no errors. The SARIF report has **zero findings** referencing `CustomerBottomBar.kt` or `CustomerDashboardScreen.kt` (the rewrite + the removed screen-local bar introduced no lint issues; unused imports were pruned).

## 8. Physical-device result

**PASS.** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا", live backend `api.rojanai.ir`.

| Check | Result |
|---|---|
| Compile + install | PASS |
| Launch → Home renders, new bar | PASS (`N01`) |
| All 5 destinations reachable from the bar | PASS (`N02`–`N06`) |
| Active state = rose-gold + dot, on the correct tab per screen | PASS |
| Inactive state = muted, consistent | PASS |
| Bar identical on Home and Explore | PASS |
| No protruding disc / glow / sparkle / rotation | PASS |
| Opaque — content does not bleed through | PASS (`N02b`) |
| Navigation behaviour unchanged | PASS |
| Crash / ANR | none |

## 9. Remaining inconsistencies

| # | Item | Severity | Note |
|---|---|---|---|
| BN-1 | Bottom nav only exists on **Home** and **Explore**. Profile / Appointments / Favorites / Booking are pushed routes with a `GlassBackButton` orb and no bar. | by design (not this task) | Whether interior Customer screens should carry the tab bar is a navigation-architecture decision, explicitly out of scope ("do not modify navigation logic / routes"). Flagged only. |
| BN-2 | The `GlassBackButton` orb on those interior screens is still the old sparkle/glow/gold treatment. | out of scope | Belongs to the per-screen redesign passes (Profile/Appointments/etc.), not this bottom-nav task. |
| BN-3 | `Explore` (`CustomerHomeScreen`) content above the bar is still the old glass/sparkle/"به‌زودی"-placeholder design; its floating tab pills overlap the status bar. | out of scope | Only the bottom nav was unified. Explore's own redesign is a later task. |
| BN-4 | `HomeColors.Glow` / `RojanPremiumBorder*` / `HomeGlassSurface` tokens are now unused by the nav but still exist for other components. | none | Token cleanup is the app-wide design-system phase, after all screens adopt the reference. |
| BN-5 | Icon tokens are screen-local (`NavAccent`, `NavHairline`) rather than promoted design-system values. | none | Same deferral as the reference screens — promote in the app-wide phase. |

## 10. Final status

**PASS** — the Customer bottom navigation is now a single, shared, flat quiet-luxury implementation, identical on Home and Explore, with a clear rose-gold active state and muted inactive state, equal touch targets, consistent 24dp outlined icons on one baseline, correct RTL order, and no disc / glow / sparkle / gradient. All navigation callbacks and behaviour are unchanged and verified on the physical device. Builds and lints clean. Not committed.

**Stop here.** No destination screen (Profile, Explore, Search, Booking, Account, Settings, …) was redesigned in this task.
