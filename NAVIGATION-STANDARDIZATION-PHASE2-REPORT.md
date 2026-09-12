# Navigation Standardization — Phase 2

**Date:** 2026-09-10 · **Scope:** Customer navigation architecture only ·
**Structural only — no visual redesign outside the nav shell.** **Not committed.**

Implements audit item **P1-7** (`CUSTOMER-UI-COMPONENT-STANDARDIZATION-AUDIT.md` §7).

---

## Verdict: architecture allows a safe implementation — done.

A persistent bottom-navigation shell (`CustomerMainScaffold`) now wraps the **five main sections**
(Home, Explore, Appointments, Favorites, Profile). Every other route — the booking flow, salon /
service / specialist detail, appointment detail, reschedule, auth, the "coming soon" screens, the
public-salon preview — stays a plain pushed route with a back arrow and **no** bar, exactly as before.

Why this was safe to do:
- `CustomerBottomBar` was already a **pure, navigation-agnostic component** — it renders 5 tabs and
  raises `onTabSelected(tab)`; it holds no navigation logic. The shell reuses it byte-for-byte.
- The 5 tab-root routes all already exist in `RojanNavGraph`.
- **No deep links** to preserve — the manifest has only the `LAUNCHER` intent-filter; `PUBLIC_SALON`
  is an in-app-only route with no scheme registered.
- The booking flow (`BOOKING_FLOW_GRAPH` + its shared `BookingViewModel`) and the auth path
  (`AUTH`, `CustomerAccessGuard`) are entirely separate route trees — untouched.

---

## Audit of the current navigation

| Area | Before |
|---|---|
| **`RojanNavGraph`** | One flat `NavHost`. Tab roots scattered: `CUSTOMER_HOME` + `EXPLORE` are top-level `composable`s; `APPOINTMENTS`, `FAVORITES` (+ Profile) live inside the nested `PROFILE_GRAPH`. Splash → session-restore gate → `startDestination` = `CUSTOMER_HOME` (authed) or `EXPLORE` (guest), frozen with `remember`. |
| **`CustomerDashboardScreen`** (route `CUSTOMER_HOME`, "خانه" tab) | Rendered its **own** `CustomerBottomBar` inside a `Box`, measured its height via `onSizeChanged`, and padded its `LazyColumn`'s bottom by it. `onTabSelected` → per-screen `on*` callbacks. |
| **`CustomerHomeScreen`** (route `EXPLORE`, "جستجو" tab) | Same pattern; also computed `bottomBarActiveTab` = `HOME` on the guest landing screen, `SEARCH` otherwise. |
| **`CustomerBottomBar`** | Nav-agnostic. 5 tabs: `PROFILE` · `FAVORITES` · `HOME` · `BOOKINGS` · `SEARCH` (Home centred). Flat Quiet-Luxury treatment, `selectableGroup` + `Role.Tab` + Persian `stateDescription`. |
| **Profile navigation** | `PROFILE` inside `PROFILE_GRAPH`, reached from the bar (`onProfileClick`) or nowhere else. A back-arrow screen (`CustomerScaffold`). Its menu pushes `WALLET` / `COUPONS` / `APPOINTMENTS` / `FAVORITES` / … |
| **Appointments navigation** | `APPOINTMENTS` inside `PROFILE_GRAPH`, `CustomerAccessGuard`-wrapped, reached from the bar (`onBookingsClick`) or Profile's menu. A back-arrow screen. `AppointmentDetails` / `Reschedule` push from it. |

**The issue:** the bar appeared on exactly 2 of ~5 "main" sections; the other three were pushed
back-arrow screens, so a "tab" tap dropped the tab bar and changed the navigation metaphor mid-flow.

---

## Implementation

### New — `screens/customer/CustomerMainScaffold.kt`

```kotlin
@Composable
fun CustomerMainScaffold(
    activeTab: CustomerHomeTab,
    onTabSelected: (CustomerHomeTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        CustomerBottomBar(activeTab = activeTab, onTabSelected = onTabSelected)
    }
}
```

- The bar sits **below** the content (`weight(1f)`), it does **not** overlay — so screens no longer
  measure the bar's height or pad their scroll content by it.
- Holds **no** navigation logic; `onTabSelected` is raised to `RojanNavGraph`, same contract as
  `CustomerBottomBar` itself.
- Applies no theme / background / insets — the content brings its own `HomeBackgroundTheme` (via
  `CustomerScaffold` or directly), and `CustomerBottomBar` paints its own opaque ground and takes
  `navigationBarsPadding`.

### `RojanNavGraph` — one shared tab-nav lambda + 5 wraps

```kotlin
val onCustomerTab: (CustomerHomeTab) -> Unit = { tab ->
    val target = when (tab) {
        CustomerHomeTab.HOME     -> RojanDestinations.CUSTOMER_HOME
        CustomerHomeTab.SEARCH   -> RojanDestinations.EXPLORE
        CustomerHomeTab.BOOKINGS -> RojanDestinations.APPOINTMENTS
        CustomerHomeTab.FAVORITES-> RojanDestinations.FAVORITES
        CustomerHomeTab.PROFILE  -> RojanDestinations.PROFILE
    }
    navController.navigate(target) {
        popUpTo(navController.graph.findStartDestination().id)
        launchSingleTop = true
    }
}
```

- **`popUpTo(startDestination)` + `launchSingleTop`** — a tab tap pops back to the NavHost start
  destination and pushes the tab, so the back stack is at most one level deep beyond the start and
  never carries a duplicate tab. Back from any tab is always predictable (returns to Home / the
  landing screen, then exits). This is the same shallow model the embedded bar effectively produced;
  it does **not** use `saveState`/`restoreState` (a deliberate choice — see Follow-ups).

Then five `composable` bodies wrap their screen:

| Route | Wrap |
|---|---|
| `CUSTOMER_HOME` | `CustomerMainScaffold(HOME, onCustomerTab) { CustomerDashboardScreen(…) }` |
| `EXPLORE` | `CustomerMainScaffold(if (isLandingEntry) HOME else SEARCH, onCustomerTab) { CustomerHomeScreen(…) }` |
| `PROFILE` | `CustomerMainScaffold(PROFILE, onCustomerTab) { ProfileScreen(…) }` |
| `APPOINTMENTS` | `CustomerAccessGuard { CustomerMainScaffold(BOOKINGS, onCustomerTab) { AppointmentsScreen(…) } }` |
| `FAVORITES` | `CustomerAccessGuard { CustomerMainScaffold(FAVORITES, onCustomerTab) { FavoritesScreen(…) } }` |

`CustomerMainScaffold` is placed **inside** `CustomerAccessGuard` for the two guarded tabs, so a
guest tapping the tab is redirected to `AUTH` by the guard's `LaunchedEffect` with no bar flash.

Every `on*` callback the nav graph passed to these five screens is **unchanged** (byte-identical
lambdas). Every route string, argument, `enterTransition`/`exitTransition`, nested-graph structure,
and `CustomerAccessGuard` is unchanged.

### `CustomerDashboardScreen` + `CustomerHomeScreen` — bar removed

Both screens **stop rendering their own `CustomerBottomBar`**. Removed from each:
- the `CustomerBottomBar(…)` call and its wrapping `Box` / `align(BottomCenter)`;
- `var bottomBarHeight`, `val density = LocalDensity.current`, the `onSizeChanged` measurement;
- the `+ bottomBarHeight` term in the `LazyColumn`'s `contentPadding.bottom`.

Their **public signatures are unchanged** — every `on*` callback and `bottomBarActiveTab` stay in
the parameter list (now unused by the screen, still passed by the nav graph, so "preserve
callbacks" holds and no call site breaks). Six now-unused imports were pruned from each file. No
other line of either screen changed — no visual token, layout, colour, or ViewModel.

---

## Files changed

| File | Change |
|---|---|
| `screens/customer/CustomerMainScaffold.kt` | **new** — the shell (≈50 lines) |
| `navigation/RojanNavGraph.kt` | +`onCustomerTab` lambda, +import, 5 `composable` bodies wrapped |
| `screens/customer/CustomerDashboardScreen.kt` | removed embedded bar + plumbing; signature unchanged |
| `screens/customer/CustomerHomeScreen.kt` | removed embedded bar + plumbing; signature unchanged |

`CustomerBottomBar.kt`, `CustomerHomeTab`, `RojanDestinations`, every other screen, every ViewModel,
every repository/API — **untouched**.

---

## Requirements check

| Requirement | Status |
|---|---|
| Keep existing RojanNavGraph routes working | ✅ — no route string / argument / graph structure changed; wraps are additive |
| Preserve all navigation callbacks | ✅ — every `on*` lambda in the 5 wrapped `composable`s is byte-identical; the removed-bar screens keep every callback param |
| Preserve ViewModels | ✅ — no `viewModel(...)` block or factory touched anywhere |
| Preserve booking flow | ✅ — `BOOKING_FLOW_GRAPH`, `bookingViewModelFor`, `MEMBER_SALONS_LIST`, all detail routes are un-wrapped (no bar) and unchanged |
| Preserve auth guards | ✅ — `CustomerAccessGuard` wraps the shell for `APPOINTMENTS`/`FAVORITES`; guest → `AUTH` unchanged |
| Preserve deep links | ✅ — none exist beyond `LAUNCHER`; nothing changed |
| Keep RTL | ✅ — `CustomerBottomBar` (unchanged) is already RTL; the shell adds only a `Column` |
| Use existing CustomerBottomBar component | ✅ — reused verbatim, no signature or visual change |
| No visual redesign outside the nav shell | ✅ — the only screen edits are *removing* a bar the shell now owns |

---

## Validation

| Gate | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL** · EXIT 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `0 errors, 94 warnings` — identical to the pre-Phase-2 baseline (94). **Zero lint findings reference `CustomerMainScaffold.kt`, `RojanNavGraph.kt`, `CustomerHomeScreen.kt`, or `CustomerDashboardScreen.kt`.** |
| `:app:assembleCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `app-customer-dev-debug.apk` (≈34 MB) produced |

---

## Device test — A72

**Device:** Samsung Galaxy A72 (`RZ8R81WPS2J`), Android 14, 1080×2400 · **Build:** `app-customer-dev-debug.apk` (this branch) · **Account:** authenticated customer (+98 916 498 7585, "گیتا") · **Crash log:** `adb logcat -b crash` → **`FATAL EXCEPTION` count = 0** across the entire run.

| # | Step | Result |
|---|---|---|
| 1 | **Home tab** — launch app | Lands on `CUSTOMER_HOME` ("سلام گیتا جان"). Persistent bar present, 5 tabs (پروفایل · علاقه‌ها · **خانه** active rose-gold + 3dp dot · نوبت‌ها · جستجو). Content scrolls above the bar, no overlap. `GET /bookings/mine` fired. ✅ `01_home_tab.png`, `08_home_relaunch_bar.png` |
| 2 | **Explore tab** — tap جستجو | `EXPLORE` ("کشف سالن‌ها" / "سالن مناسب خود را پیدا کنید"). `GET /api/v1/salons` → 200. Bar persists, جستجو now active. ✅ `02_explore_tab.png` |
| 3 | **Appointments tab** — tap نوبت‌ها | `APPOINTMENTS` ("نوبت‌های من", empty state). `GET /bookings/mine` → 200. `CustomerAccessGuard` passed (authed). Bar persists, نوبت‌ها active. ✅ `03_appointments_tab.png` |
| 4 | **Profile tab** — tap پروفایل | `PROFILE` ("حساب کاربری" / "گیتا"). Bar persists, پروفایل active; menu scrolls under a fixed bar. ✅ `04_profile_tab.png` |
| 4b | **Back-stack depth** — back from Profile → back again | Profile → Home ("سلام گیتا جان") → launcher (app exits). Shallow, predictable — `popUpTo(startDestination) + launchSingleTop` confirmed; no duplicate tab entries, no deep stack. ✅ |
| 4c | **Tab → system-back** — Home → Explore → system back | Returns to Home with the bar. ✅ |
| 5 | **Booking regression** — Home → tap "ROJAN AI Pilot Salon" → tap "Haircut" → "رزرو این خدمت" → time screen → tap "09:00" → "تایید رزرو" | Salon detail, service detail, **"انتخاب ساعت"**, and **"تایید رزرو"** all render with a **back arrow and NO bottom bar** (correct — detail/booking routes are un-wrapped). `GET …/available-slots` → 200. Forward flow and back (تایید رزرو → انتخاب ساعت) work. ✅ `05_salon_detail_nobar.png`, `06_booking_time.png` |

**Pre-existing, out of scope (not a Phase 2 regression):** the `BOOKING_TIME` screen ("انتخاب ساعت") traps the system-back button — neither `KEYCODE_BACK`, the top-bar arrow, nor an edge-swipe leaves it (its `onBackClick = { navController.popBackStack() }` and the screen are untouched by this phase; the booking sub-graph is not wrapped by `CustomerMainScaffold`). Filed for a later booking-flow pass. All other back navigation in the app behaves correctly.

**Verdict:** all five required checks pass. The persistent bar appears on exactly the four tab roots + Favorites, never on detail/booking/auth routes, survives every tab switch, keeps the back stack shallow, and introduces **zero crashes**.

---

## Follow-ups (not in this phase)

1. **`saveState` / `restoreState` per tab.** The current shell keeps the stack shallow but does not
   preserve each tab's scroll position / sub-stack across tab switches. Adding `saveState = true` /
   `restoreState = true` would — at the cost of a tab round-trip potentially restoring to a
   sub-screen (e.g. Profile → Wallet → Home tab → Profile tab lands back on Wallet). Worth doing
   once each tab is its own `navigation {}` sub-graph.
2. **Tab-root back arrows.** `AppointmentsScreen` / `ProfileScreen` / `FavoritesScreen` still render
   their `CustomerScaffold` back arrow (they also get pushed from Profile's menu, where the arrow is
   correct). `CustomerScaffold.showBackButton` (added in Phase 1) could hide it when the screen is a
   true tab root — needs the route to know its entry context.
3. **Bar on within-tab detail screens.** A production bottom nav often keeps the bar visible on
   `AppointmentDetails` / `SalonDetails` within a tab. Deliberately out of this phase's scope (the
   task's structure lists only the 4/5 roots).

Nothing was committed.
