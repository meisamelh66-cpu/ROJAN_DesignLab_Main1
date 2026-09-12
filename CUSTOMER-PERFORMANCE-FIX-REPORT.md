# ROJAN Customer Android — Performance Fix Implementation

**Date:** 2026-09-10
**Scope:** Customer app only. Implements the two highest-impact findings (B.1/B.2 and B.3) from `CUSTOMER-PERFORMANCE-AUDIT.md`. No backend touched, no API contract changed, no unrelated UI modified, no DI/OkHttp/Coil/Compose-recomposition work done (deferred, per the audit's own recommendation).
**Status:** ✅ Complete. Compile + lint + assemble + relevant unit tests all green. Not committed.

---

## A. Exact root cause addressed

### Fix 1 — Splash / Session Restore
**Root cause:** `SplashScreen`'s exit was gated on an unconditional `delay(2600L)` (plus a 400ms fade), and — separately — `RojanNavGraph`'s call to `AuthViewModel.restoreSession(personId)` (the real session-validation network work: optional token refresh → `GET /auth/me` → `GET /users/me/salon-access`) lived *inside* the branch of code that only became reachable once `showSplash` had already flipped to `false`. The two were therefore strictly sequential — a returning, already-logged-in customer paid the full fixed splash time and *then* the network round-trip(s), rather than the network work running *during* the splash's already-mandatory wait.

### Fix 2 — Bottom-Tab Navigation State
**Root cause:** `onCustomerTab`'s `navController.navigate(target) { popUpTo(...); launchSingleTop = true }` had no `saveState`/`restoreState`. Navigation-Compose's default `viewModel()` scopes to the back-stack entry; without state preservation, `popUpTo` destroyed the previous tab's back-stack entry outright on every switch, taking its ViewModel with it. `SalonListViewModel` and `BookingHistoryViewModel` (both used on the Home/Dashboard tab) each fire a real network request from their `init {}` block — so every tab switch back to a previously-visited tab re-ran their startup fetch from scratch, not just on cold start.

---

## B. Files changed

| File | What changed |
|---|---|
| `app/src/main/java/ai/rojan/designlab/screens/splash/SplashScreen.kt` | `SplashScreen`'s signature changed from `(onSplashFinished, minDisplayMillis: Long = 2600L)` to `(ready: Boolean, onSplashFinished: () -> Unit = {})`. Removed the internal `delay(minDisplayMillis)`/`var ready by remember { mutableStateOf(false) }` gate entirely. The fade-out `LaunchedEffect` is now keyed on the caller-supplied `ready` parameter instead of an internally-timed one. No other change to this file — the 400ms fade-out, the logo/wordmark entrance choreography (`SplashLogo`/`SplashText`, their own cosmetic staggered `delay()`s), and `SplashScreenContent`'s visuals are all byte-identical to before. |
| `app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt` | Two independent, non-overlapping edits inside `RojanNavGraph()`: (1) the `authViewModel.restoreSession(personId)` `LaunchedEffect` was hoisted from inside the `SessionRestoreState.Restored` branch (reachable only post-splash) to directly after `restoreState` is collected (reachable immediately, overlapping the splash), and a new `readyToProceed` boolean now drives `SplashScreen(ready = readyToProceed, ...)`. (2) `onCustomerTab`'s `navigate(target) { ... }` call gained `popUpTo(...) { saveState = true }` and `this.restoreState = true` (explicitly qualified — see §F) alongside the existing `launchSingleTop = true`. No other `navigate()` call in this ~1300-line file was touched. |
| Only two files changed. No test file, ViewModel, repository, API, DTO, or backend file was modified. |

**Only the caller of `SplashScreen` (`RojanNavGraph.kt`) exists** — confirmed via grep before editing — so no other call site needed updating.

---

## C. Splash behavior — before / after

| | Before | After |
|---|---|---|
| **Exit condition** | `delay(2600L)` elapses → fade out (400ms) → `onSplashFinished()`. Fixed, unconditional. | `ready` (caller-supplied) becomes `true` → fade out (400ms, unchanged) → `onSplashFinished()`. Driven by real state, no internal timer. |
| **When session validation starts** | After `showSplash` becomes `false` — i.e., after the fixed ~3.0s has already elapsed. | The instant `restoreState` (a fast, local DataStore read) resolves to `Restored` — typically within milliseconds of process start, well inside the splash's own entrance-animation window. Network work now overlaps with the splash instead of following it. |
| **Guest (no persisted session)** | Flat ~3.0s wait regardless. | `readyToProceed` becomes `true` as soon as `restoreState` resolves (no `personId` → no network call needed) — splash exits almost immediately, no artificial wait. Matches the task's explicit "guest → no unnecessary waiting" requirement. |
| **Returning, logged-in customer** | ~3.0s splash + up to ~1–1.8s (estimated) of *additional*, fully sequential network latency afterward, behind a *second*, visually distinct loading screen (`RestoringSessionContent`). | Splash stays up for exactly as long as session validation actually takes (token refresh + `/auth/me` + `/users/me/salon-access`, all still awaited in the same order as before — see §F), with no separate loading-screen swap for the common case: the `SessionRestoreState.Loading` branch (still present, see §F) is unreachable once the splash gate has already required `Restored`. |
| **Failure path** (expired/revoked refresh token, network error, or the pre-existing 5s `SessionViewModel` DataStore-read timeout guard) | `restoreSession`'s `.onFailure {}` clears tokens/personId; `RojanNavGraph` proceeds to the guest `EXPLORE` start destination. Worst case ~5.0s total (2.6s splash + up to 2.4s more on `RestoringSessionContent` before the timeout guard fires). | Same `.onFailure {}` handling, completely untouched (`AuthViewModel.kt` was not modified). Worst-case total time is unchanged (~5.0s, still bounded by the same pre-existing 5s `SessionViewModel` timeout guard) — just now presented as one continuous splash instead of a two-screen swap. |
| **Screen swap / flicker** | Splash → (briefly) `RestoringSessionContent` → real screen: up to two distinct full-screen composable swaps. | Splash → real screen: exactly one transition (the existing 400ms fade), for the path every real user actually takes. `RestoringSessionContent` is still defined and still reachable in the `SessionRestoreState.Loading` branch, but that branch is now unreachable in practice once past the splash gate (kept as a defensive fallback, not deleted — see §F). |

---

## D. Navigation state behavior — before / after

| | Before | After |
|---|---|---|
| **Tab-switch back-stack entries** | `popUpTo(startDestination.id)` (no `saveState`) — every destination above the graph's start is popped and *discarded*, including its `ViewModelStore`. | `popUpTo(startDestination.id) { saveState = true }` — the popped destination's state (including its ViewModelStore) is *saved*, not discarded. |
| **Returning to a previously-visited tab** | `navigate(target) { launchSingleTop = true }` (no `restoreState`) — always constructs a fresh back-stack entry and a fresh ViewModel, even if that tab was visited moments ago. | `navigate(target) { launchSingleTop = true; this.restoreState = true }` — restores the previously-saved entry (and its ViewModel, and its already-loaded data) when one exists, instead of rebuilding from scratch. |
| **ViewModel `init {}` network calls** (`SalonListViewModel.load()`, `BookingHistoryViewModel.load()`, etc.) | Re-fired on every single tab switch back to a tab that had already been visited. | Fire once per genuinely-new ViewModel instance — i.e., once per tab per app session (until process death or the ViewModelStoreOwner is otherwise cleared), not once per tab switch. |
| **Scope of the change** | — | Applied **only** to `onCustomerTab`'s `navigate()` call — the five bottom-tab destinations (`CUSTOMER_HOME`, `EXPLORE`, `APPOINTMENTS`, `FAVORITES`, `PROFILE`). No other `navigate()`/`popUpTo()` call anywhere in `RojanNavGraph.kt` (booking flow, auth, salon/service/specialist detail, appointment detail, edit links, etc.) was touched. |
| **Booking flow, detail routes** | Bar-less, pushed routes, back-stack unaffected by tab switching. | Unchanged — none of these routes are reachable through `onCustomerTab`, and `saveState`/`restoreState` only affects the exact `navigate()` call they were added to. |

---

## E. Tests executed

```
:app:compileCustomerDevDebugKotlin   → BUILD SUCCESSFUL
:app:lintCustomerDevDebug            → BUILD SUCCESSFUL, 94 warnings (unchanged from session baseline,
                                        0 new findings in either changed file — the 2 pre-existing
                                        SplashScreen.kt warnings at lines 321/404, "State backed values
                                        should use the lambda overload of Modifier.offset", are inside
                                        SplashLogo/SplashText, which this fix did not touch)
:app:assembleCustomerDevDebug        → BUILD SUCCESSFUL

:app:testCustomerDevDebugUnitTest --tests AuthViewModelTest
                                      → 15/15 passed (restoreSession/onAuthenticated/
                                        refreshIdentityContext/logout behavior — all untouched by this
                                        change — independently re-verified)

:app:testCustomerDevDebugUnitTest (full Customer suite)
                                      → 307/309 passed. The 2 failures are the pre-existing
                                        `BackendAuthFlowVerificationTest` (requires a live
                                        `localhost:8080` backend, environmental, unrelated to this
                                        change — same 2 failures this exact suite has produced in every
                                        prior phase of this session, confirmed by memory)
```

One compile-time issue surfaced and was fixed during implementation (documented for transparency, not hidden): adding `restoreState = true` to the tab-switch `navigate { ... }` builder initially failed to compile — `RojanNavGraph`'s own outer `val restoreState by sessionViewModel.restoreState.collectAsStateWithLifecycle()` (pre-existing, same name) shadowed `NavOptionsBuilder.restoreState` inside the nested lambda, so the assignment resolved to the wrong (immutable, wrong-typed) property. Fixed by explicitly qualifying it as `this.restoreState = true` inside that lambda. No other symptom of this existed; it was caught by the compiler on the first build attempt.

---

## F. Known risks

- **Splash fade timing for a very fast guest path.** Per the task's explicit instruction ("must not wait for an arbitrary fixed timer... guest → no unnecessary waiting"), no minimum brand-floor was added. On a fast device with an already-warm DataStore, a guest could see the splash for well under 100ms before the 400ms fade begins — technically correct and exactly what was asked for, but worth knowing this is a deliberate choice, not an oversight, if a stakeholder later wants a minimum "branding moment" reintroduced. That would be a one-line addition (`max` the real readiness against a small floor) and was intentionally left out here since it wasn't asked for.
- **No new upper-bound timeout was added to the session-validation network call.** `authViewModel.restoreSession(personId)` still has no explicit timeout of its own — it's bounded only by OkHttp's default connect/read/write timeouts (10s each, unconfigured, unchanged — DI/OkHttp tuning is explicitly out of scope for this task). This is **not a new risk**: the exact same unbounded-by-this-code characteristic existed before this fix too (the old `isRestoringSession` gate awaited the identical suspend call). Worst-case splash duration on a hung connection is unchanged by this fix, not worsened.
- **`SessionRestoreState.Loading` branch is now effectively dead code** in the normal flow (kept, not deleted, as a defensive fallback in case `readyToProceed`'s invariant is ever violated by a future change). A future refactor could reasonably remove it once confident it's unreachable, but that's a separate, non-urgent cleanup, not done here to keep this change minimal.
- **Tab-nav state preservation is the well-documented, standard Navigation-Compose pattern**, but it does change back-stack/ViewModel-lifetime behavior across all 5 tabs simultaneously. Static verification (compile/lint/tests) cannot exercise the actual runtime back-stack transitions between tabs — **on-device verification of all 5 tabs, including edge cases (switching tabs mid-network-call, switching during an in-flight booking-adjacent navigation, guest→login mid-tab-switch) is still needed before this is considered fully verified**, consistent with this project's established "validate every screen on-device" convention. This task explicitly did not require or perform that device pass.
- **`CustomerHomeTab.HOME` always targets `CUSTOMER_HOME`**, even for a guest whose actual landing/start destination is `EXPLORE` — this is pre-existing behavior (the `target` resolution in `onCustomerTab` was not changed, only the `navOptions` were), unaffected by and unrelated to this fix, noted here only for completeness.

---

## G. Intentionally NOT changed

- **DI container (`BackendApiContainer`/`BackendApiContainerHolder`)** — eager construction, 3 separate `OkHttpClient`s, no HTTP cache: all explicitly out of scope per the task ("Do not optimize DI, OkHttp, Coil, or Compose yet"). Audit findings B.4/B.5, deferred per the audit's own recommendation.
- **Coil / image loading** — no `Application` class, no shared `ImageLoaderFactory`: untouched, out of scope.
- **Compose recomposition/Compose-level work** — untouched, out of scope; the audit's own spot-checks found this area largely already fine (stable list keys, already-`remember`ed `ImageRequest`s).
- **`AuthViewModel.kt` itself** — not modified. Token refresh, `GET /auth/me`, `GET /users/me/salon-access`, and `logout()` behavior are all byte-identical to before; only *when* `restoreSession()` is invoked changed (hoisted earlier in composition), never *what* it does or *how* it's called (same single call, same argument, same awaited-not-fire-and-forget contract).
- **`SessionViewModel.kt`** — not modified. The 5-second DataStore-read timeout guard, the `observePersonId()` flow, and `SessionRestoreState`'s shape are all unchanged.
- **Backend / API contracts** — nothing in `data/remote/`, no DTO, no endpoint path, no request/response shape touched anywhere.
- **Booking flow, nested `BOOKING_FLOW_GRAPH`, `AUTH` screen, salon/service/specialist detail routes, appointment detail/reschedule, edit links from Confirmation** — none of these `navigate()`/`popUpTo()` calls were touched; the `saveState`/`restoreState` addition is scoped exclusively to `onCustomerTab`.
- **`CustomerMainScaffold.kt`/`CustomerBottomBar.kt`** — inspected per the task's instruction, confirmed to hold zero navigation logic (`CustomerMainScaffold`'s own doc comment: "it holds NO navigation logic"), so neither needed any change.
- **Committing** — nothing was committed, per instructions.

---

## Summary

Both fixes are structural, minimal (two files, tightly scoped edits, one pre-existing composable's signature changed with exactly one caller updated), and directly address the two root causes identified in the prior audit without touching anything the task said not to. Compile, lint, assemble, and the relevant + full unit-test suites all confirm no regression — but per the audit's own risk assessment, both fixes (especially the tab-nav state-preservation change) still need a real on-device walk of all 5 tabs before they're considered fully verified for release; that pass was not performed here, consistent with the task's "no physical device required for this pass" instruction.
