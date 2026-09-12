# ROJAN Customer Android — Performance Audit

**Date:** 2026-09-10
**Scope:** Customer app only (`ai.rojan.designlab`, customer flavor). Read-only — no code modified, no device connected, no backend touched, nothing committed.
**Method:** static source-code investigation (every claim below is traced to a specific file/line) + reasoned timing estimates where a real measurement would require a device. Every number is explicitly labeled **CERTAIN** (a fixed value read directly from code) or **ESTIMATED** (a plausible network-dependent range, not measured). Nothing here was "optimized blindly" — no code was changed.

---

## A. Startup timeline

Two distinct paths exist, and they differ substantially — this alone is a finding worth stating up front: **the app is fastest for a brand-new guest and slowest for a returning, already-authenticated customer** — usually the opposite of what you'd want to optimize for.

### Guest / first-time user (no persisted session)
| T+ | Event | Basis |
|---|---|---|
| 0ms | `MainActivity.onCreate()` → `setContent { RojanNavGraph() }` | `MainActivity.kt` |
| ~0–30ms | `AuthViewModelFactory`/`SessionViewModelFactory` construct → first call to `BackendApiContainerHolder.get()` → entire `BackendApiContainer` built synchronously on the main thread (3 `OkHttpClient`s, ~30 `Retrofit.create()` proxies, ~30 repository objects) | **ESTIMATED** — object/proxy construction only, no I/O; `di/BackendApiContainer.kt` |
| ~0–10ms | `SessionViewModel.init{}` launches a DataStore read off the calling coroutine | **ESTIMATED**, but DataStore reads are routinely sub-10ms; `SessionViewModel.kt:77-111` |
| 0–3000ms | `SplashScreen` shown — **fixed, unconditional** `delay(2600L)` + a 400ms fade-out | **CERTAIN** — `SplashScreen.kt:56,69,78-84` |
| ~3000ms | `showSplash=false`. `restoreState` is already `Restored(personId=null, ...)` (resolved minutes earlier, well inside the splash window) → `isRestoringSession` starts `false` (no `personId`) → real navigation renders immediately | `RojanNavGraph.kt:239` (`isRestoringSession = state.personId != null`) |
| ~3000ms | **First usable screen** (`EXPLORE`) | |

**Guest total: ~3.0s, dominated entirely by the fixed splash.**

### Returning, already-logged-in customer
| T+ | Event | Basis |
|---|---|---|
| 0–3000ms | Same as above — splash, `restoreState` resolves to `Restored(personId="<real id>", ...)` well before splash ends | **CERTAIN** for the splash floor |
| ~3000ms | `showSplash=false`. **Only now** does `LaunchedEffect(state)` fire `authViewModel.restoreSession(personId)`. The full-screen `RestoringSessionContent()` loading state is shown for the duration below | `RojanNavGraph.kt:239-252` |
| +X₁ | *(conditional)* Access token expired → `TokenAuthenticator` transparently refreshes it first (1 network round-trip) | **ESTIMATED**, ~150–800ms depending on connection; fires whenever the access token has outlived `JWT_ACCESS_TTL_MINUTES` (15min default) since last use — common on any cold start after the app's been backgrounded a while |
| +X₂ | `backendAuthRepository.currentUser()` → `GET /api/v1/auth/me`, awaited | **ESTIMATED**, ~150–500ms |
| +X₃ | On success, `onAuthenticated()` → `refreshIdentityContext()` → `GET /users/me/salon-access`, awaited **sequentially after X₂**, before `isRestoringSession` can become `false` | **ESTIMATED**, ~150–500ms; `AuthViewModel.kt:276-306,346-354` |
| ~3300–5000+ms | **First usable screen** (`CUSTOMER_HOME`, the Dashboard) — but its own ViewModels (see B.3) now fire 2 more network calls before their own content settles (non-blocking to screen render, since it shows immediately with skeleton loaders) | |

**Returning customer total: ~3.0s (certain floor) + up to ~1.0–1.8s of sequential, unoverlapped network work (estimated, network-dependent) = 3.3–5.0s+ before the customer can do anything at all**, vs. ~3.0s for a guest. The entire X₁+X₂+X₃ chain happens strictly *after* the splash's already-mandatory wait, when at least X₂'s personId is known within milliseconds of process start — it could have been running *during* the splash instead of after it.

---

## B. Top 5 performance bottlenecks, ranked by impact

### B.1 — Fixed 2.6s+0.4s splash, not gated on anything real (CERTAIN, highest impact, affects 100% of launches)
Every single cold start pays a flat, unconditional ~3.0 second tax, whether the app is ready in 50ms or 3 seconds. This is the single largest, most certain, most universal contributor to perceived slowness — it affects every user, every launch, deterministically.

### B.2 — Session-restore network chain runs *after* the splash instead of *during* it (CERTAIN mechanism, ESTIMATED magnitude, affects every returning logged-in customer)
The `personId` needed to start `restoreSession()` is known within milliseconds of process start (a local DataStore read), but the code structure (`if (showSplash) { ...; return }` placed *before* the `restoreState`/`LaunchedEffect` block) means the network work cannot even begin until the splash's fixed 3s has already elapsed. For your most valuable users — the ones who already signed up and logged in — this stacks up to ~1–1.8s of *additional*, fully serialized network latency on top of the splash, for functionally no reason: nothing about the splash depends on the network call's result, and nothing about the network call depends on the splash finishing.

### B.3 — No `saveState`/`restoreState` on bottom-tab navigation → screen ViewModels (and their startup API calls) are destroyed and recreated on every tab switch (CERTAIN, high-frequency, affects post-launch navigation)
```kotlin
navController.navigate(target) {
    popUpTo(navController.graph.findStartDestination().id)
    launchSingleTop = true
}
```
`RojanNavGraph.kt:306-309` — no `saveState = true` on the `popUpTo`, no `restoreState = true` on the `navigate`. Navigation-Compose's default `viewModel()` scopes to the back-stack entry; without state-saving, switching tabs *destroys* the previous destination's back-stack entry, and with it, its ViewModel. Both `SalonListViewModel` (`init { load() }`, `SalonListViewModel.kt:86-88`) and `BookingHistoryViewModel` (`init { load() }`, `BookingHistoryViewModel.kt:31-33`) fire a real network request the instant they're constructed. **Net effect: Home → Explore → Home again re-fetches the salon list and booking history from scratch every time**, even though nothing changed — a real, repeated, avoidable network+loading-spinner cost every time a user bounces between tabs, which for a bottom-nav app is extremely frequent.

### B.4 — Composition-root (`BackendApiContainer`) is eager, monolithic, and builds 3 independent OkHttp clients with zero caching (CERTAIN, moderate magnitude — tens of ms, not seconds — but a real structural cost paid on every cold start)
`di/BackendApiContainer.kt` constructs **all ~30 repositories/APIs** (manager-only ones included, regardless of whether the Customer app will ever touch them) synchronously, on the main thread, the first time any screen resolves `BackendApiContainerHolder.get()` — which happens essentially at launch, since `AuthViewModelFactory` (used at the very top of `RojanNavGraph()`) triggers it. Three separate `OkHttpClient` instances are built (`buildAuthenticatedRetrofit`'s main client, its internal `plainAuthApi` client used only for token refresh, and `buildPlainRetrofit()`'s client for the public salon API) — each with its own connection pool and dispatcher, no sharing, and **none of the three configures an HTTP disk cache** (`.cache(...)`) anywhere. Object/proxy construction itself is cheap (no I/O at construction time), so this is not a multi-second contributor — but it's real, unconditional main-thread work on every launch, and the missing cache means even cacheable, largely-static GETs (service categories, specialist lists) always hit the network.

### B.5 — Coil's default `ImageLoader` runs its own, separate OkHttp client — no shared connection pool with the app's API traffic (CERTAIN, low magnitude)
No custom `ImageLoader`/`ImageLoaderFactory` is configured anywhere (confirmed by grep — `RojanRemoteImage.kt` is the only Coil touchpoint in the app, and it uses the library's default singleton loader via `AsyncImage`). Coil's default loader builds its own internal `OkHttpClient`, independent of the three above. Since API responses and salon/specialist images are served from the same host (`api.rojanai.ir`, per the backend's nginx `/media/` alias), a 4th, unrelated connection pool means image requests can't reuse a TLS/TCP connection the API calls already warmed up (or vice versa) — a small, real, per-request latency cost, not a startup blocker. (Note: this customization path is also closed off by the fact that the app has no custom `Application` class at all — Coil's `ImageLoaderFactory` auto-discovery requires one.)

**What was checked and found *not* to be a problem, for completeness (per "do not optimize blindly," negative findings matter too):**
- `RojanRemoteImage`'s `ImageRequest` is already `remember`ed keyed on `(url, context)` (`RojanRemoteImage.kt:59-61`) — no per-recomposition request re-allocation; this was already fixed in an earlier phase.
- The real data `LazyRow`/`LazyColumn` lists on Home/Dashboard already use stable `key = { it.id }` (`CustomerDashboardScreen.kt:367`, `CustomerHomeScreen.kt:179`) — no missing-key recomposition/reload risk found.
- The `.filter{}` calls computing "upcoming"/"recent" bookings (`CustomerDashboardScreen.kt:149,152`) run over small, page-bounded lists (≤20 items) and only re-run when the underlying ViewModel state actually changes — not a measurable cost at this scale; `remember`/`derivedStateOf` here would be a no-op improvement, not a real fix.
- Search debouncing (`SearchScreen.kt`/`SalonListScreen.kt`, 350ms) is a deliberate, correctly-sized UX debounce, not an artificial startup delay.
- No `BackHandler`/`postDelayed`/`Handler.post` found anywhere in Customer scope.

---

## C. Evidence index

| Finding | File(s) | Line(s) |
|---|---|---|
| Fixed splash delay | `screens/splash/SplashScreen.kt` | 56, 69, 78–84 |
| Splash gates network restore, not the other way around | `navigation/RojanNavGraph.kt` | 200–252 |
| Session-restore chain: 3 sequential potential network calls | `presentation/auth/AuthViewModel.kt` | 276–306, 346–354 |
| DataStore-only, fast local resolve (not the bottleneck) | `presentation/session/SessionViewModel.kt` | 67–121 |
| No saveState/restoreState on tab nav | `navigation/RojanNavGraph.kt` | 298–310 |
| Eager `init { load() }` network calls | `presentation/salon/SalonListViewModel.kt` | 86–88 |
| | `presentation/booking/BookingHistoryViewModel.kt` | 31–33 |
| 3 independent OkHttpClients, no HTTP cache | `di/BackendApiContainer.kt` | 271–316 |
| Eager singleton DI container | `di/BackendApiContainerHolder.kt` | 17–27 |
| No custom `ImageLoader`/`Application` class | `ui/components/image/RojanRemoteImage.kt` (only Coil touchpoint); `AndroidManifest.xml` (no `android:name` on `<application>`) | — |
| `ImageRequest` already `remember`ed (not a bug) | `ui/components/image/RojanRemoteImage.kt` | 59–61 |
| Stable list keys already present (not a bug) | `screens/customer/CustomerDashboardScreen.kt` / `CustomerHomeScreen.kt` | 367 / 179 |
| Full `delay()`/`postDelayed` inventory | grep across `app/src/main/java/ai/rojan/designlab` | see §B.5 negative findings |

---

## D. Recommended fixes (not implemented — audit only)

1. **Gate splash exit on `max(brandFloor, restoreReady)` instead of a flat timer.** Start the session-restore network call (or at least the DataStore/personId resolution + the decision of *whether* a network call is even needed) concurrently with the splash animation, and let the splash's minimum brand-display time (kept short, e.g. 800ms–1.2s, enough for the logo/wordmark to register) and the restore's actual completion race — exit on whichever finishes last, not on a fixed 3s regardless. This directly closes both B.1 and B.2 at once, since they're the same structural issue (sequential-when-they-could-overlap).
2. **Make `refreshIdentityContext()`'s `/users/me/salon-access` call non-blocking for a plain customer.** `SessionViewModel` already independently resolves `personRoles` locally (no network) for `startDestination` routing — investigate whether a CUSTOMER-role user's first screen genuinely needs `/users/me/salon-access` resolved before rendering, or whether it can populate `_identityContext` in the background after navigation, the same way the Dashboard's own `SalonListViewModel`/`BookingHistoryViewModel` already render their shell immediately and fill in data asynchronously.
3. **Add `saveState = true` to the tab-switch `popUpTo` and `restoreState = true` to its `navigate` call.** This is the standard, well-documented Navigation-Compose bottom-nav pattern specifically designed to prevent exactly this class of bug — it preserves each tab's back-stack entry (and therefore its ViewModel and in-flight/cached data) across tab switches instead of destroying and recreating it every time.
4. **Add an HTTP disk cache to the authenticated `OkHttpClient`**, and consider consolidating the 3 `OkHttpClient` instances down to one shared client + `ConnectionPool` (the plain, unauthenticated ones can still omit the auth interceptor/authenticator while sharing the pool and cache).
5. **Consider a small `Application` class + a shared `ImageLoaderFactory`** that reuses the same `OkHttpClient`/`ConnectionPool` as Retrofit for Coil's image fetches — closes B.5. Lowest priority of the five; smallest, least certain payoff.

---

## E. Risk level of each fix

| # | Fix | Risk | Why |
|---|---|---|---|
| 1 | Splash gated on `max(brandFloor, restoreReady)` | **Medium** | Touches the one screen every user sees first, and a subtle bug here (e.g. a race that shows a flash of unauthenticated UI before restore completes) would be highly visible. Needs careful state-machine design and the device verification this audit was explicitly told not to do. Previously flagged (Phase 4 audit) as "not implemented — visible timing change" for exactly this reason. |
| 2 | Make salon-access refresh non-blocking for customers | **Medium** | Requires confirming no current screen actually depends on `_identityContext` being resolved synchronously before first render — needs a real usage audit of every reader of `identityContext`, not just this file, before it's provably safe. |
| 3 | `saveState`/`restoreState` on tab nav | **Low–Medium** | Well-trodden, standard Android pattern (this is literally what it's for), but changes back-stack/ViewModel-lifetime behavior across all 5 tabs — needs a full walk of every tab + back-stack edge case (deep tab switch during an in-flight network call, guest→login mid-tab, etc.) before shipping, per this project's own "validate every screen" convention. |
| 4 | HTTP cache + shared OkHttp client | **Low** | Purely additive (`.cache(...)`, sharing a `ConnectionPool`) — doesn't change request/response semantics, existing endpoints' `Cache-Control` headers (or lack thereof) determine actual cache behavior, so this is inert until/unless the backend also sends cache headers worth honoring. Safe, but its payoff is partly gated on backend behavior this audit didn't inspect. |
| 5 | Application class + shared Coil `ImageLoaderFactory` | **Low** | Additive, well-isolated, but introducing the app's first-ever `Application` class is a small architectural change worth a deliberate, separate decision rather than folding into a "quick win." |

---

## F. Quick wins vs. structural improvements

**Quick wins** (small, isolated, low-risk, don't change the startup state machine):
- Fix 4 (HTTP cache + shared `OkHttpClient`/`ConnectionPool`) — additive, mechanical, testable in isolation.
- Fix 5 (shared Coil `ImageLoader`) — additive, isolated, but see the `Application`-class caveat above.

**Structural improvements** (touch the startup/navigation state machine, need real device verification before shipping):
- Fix 1 (splash gating) — the highest-impact fix, and the riskiest; this is the one to spend real design + on-device verification time on.
- Fix 2 (non-blocking identity-context refresh) — needs a full call-site audit first.
- Fix 3 (tab-nav state preservation) — needs a full per-tab walk before shipping, per established project convention.

---

## G. Fixes that should happen before Release Candidate

- **Fix 1 (splash gating)** — the single largest, most certain, universally-experienced contributor to perceived slowness. This is a pre-release-quality issue, not a nice-to-have: every user currently waits a fixed ~3s no matter what, and returning customers wait meaningfully longer than that for no functional reason.
- **Fix 3 (tab-nav saveState/restoreState)** — repeated, avoidable network calls on every tab switch is a real, frequent, user-visible cost (not just a cold-start issue) that will show up as "the app feels slow" in day-to-day use, not just at launch.

Both require the on-device verification this audit was explicitly told not to perform — they should be implemented as their own, scoped, verified tasks, not bundled into this audit's findings.

## H. Optimizations that should be deferred

- **Fix 2 (non-blocking identity-context refresh)** — real but smaller magnitude than 1/3, and needs a broader call-site audit first; safe to defer past RC if time is tight, revisit once 1 and 3 are shipped and re-measured.
- **Fix 4 (HTTP caching)** — genuinely low-risk, but its actual payoff depends on backend `Cache-Control` behavior this audit didn't inspect (out of scope — "no backend changes"); worth doing, but not release-blocking.
- **Fix 5 (Coil/OkHttp connection-pool sharing)** — smallest, least certain payoff of the five; fine to defer indefinitely unless a future audit finds image-loading latency to be a bigger factor than this one did.
- **DI container eagerness (B.4's "constructs all 30 repositories regardless of screen")** — real, but tens-of-milliseconds scale, dwarfed by B.1–B.3; lazifying it (e.g., per-repository `by lazy {}`) is a legitimate structural cleanup but not a release-blocking performance fix.

---

## Summary

The dominant cause of perceived Customer-app slowness is **not** Compose recomposition, image loading, or DI overhead — those were checked and are either already fine or contribute only tens of milliseconds. It's **sequencing**: a fixed 3-second splash that doesn't overlap with the one thing it should (session restore), a session-restore chain that's itself unnecessarily sequential, and a tab-navigation pattern that repeats the same network calls every time a user switches tabs. All three are structural, all three are precisely evidenced in this document, and none were changed — per the task's instructions, this is an audit, not an implementation.
