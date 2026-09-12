# MANAGER DASHBOARD ACTIVE SALON FIX REPORT v1

**Scope:** Android only — no backend/API contract changes, no test data created.
**Date:** 2026-08-21
**Basis:** `ROJAN_Active_Salon_Context_Root_Cause_Report_v1.md`

---

## Files Changed (5)

| File | Change |
|---|---|
| `app/src/main/java/ai/rojan/designlab/manager/data/ManagerRepositories.kt` | `salon` is now a `StateFlow<ManagerSalonSummary?>` (backed by `_salon: MutableStateFlow`) instead of a plain `var`. Added `clearActiveSalon()` to reset `salon`/`salonId` to null. `updateSalon()` and `initialize()` write through `_salon.value` instead of direct assignment. |
| `app/src/main/java/ai/rojan/designlab/manager/screens/dashboard/ManagerDashboardScreen.kt` | Collects `ManagerRepositories.salon` via `collectAsStateWithLifecycle()` at the top of the composable instead of reading the singleton once inside the `item {}` block. Null branch now renders `SalonIdentityCard(isLoading = true)` instead of the old no-arg placeholder call. |
| `app/src/main/java/ai/rojan/designlab/manager/components/SalonIdentityCard.kt` | Removed the hardcoded `سالن رویان`/`آرایش و زیبایی بانوان` preview defaults. Added `isLoading: Boolean = false`, which renders "در حال بارگذاری..." and suppresses the category/active-status rows instead of falling back to fabricated-looking real data. |
| `app/src/main/java/ai/rojan/designlab/manager/presentation/auth/ManagerAuthViewModel.kt` | `clearSession()` (invoked by `logout()`) now also calls `ManagerRepositories.clearActiveSalon()`, so a stale salon snapshot can never survive a logout into the next session. |
| `app/src/main/java/ai/rojan/designlab/manager/presentation/settings/ManagerSalonMediaViewModel.kt` | Updated the one other call site reading `ManagerRepositories.salon` (`load()`) to `.value`, since it's now a `StateFlow`, not a plain property. |

---

## Before / After State Flow

**Before:**
```
ManagerDashboardScreen composes
  → item{} reads ManagerRepositories.salon (plain var, still null — LaunchedEffect hasn't finished)
  → renders SalonIdentityCard() with NO args
  → resolves to hardcoded defaults "سالن رویان" / "آرایش و زیبایی بانوان" / active
LaunchedEffect(Unit) runs ManagerRepositories.initialize(context)
  → sets ManagerRepositories.salon = <real salon>, bumps refreshKey
  → item{} block never re-runs (doesn't read refreshKey or any observable) — stale placeholder stays on screen forever, indistinguishable from real data
Logout → ManagerRepositories untouched → next login can still show the previous salon's stale in-memory snapshot if the item{} block ever does recompose later
```

**After:**
```
ManagerDashboardScreen composes
  → val salon by ManagerRepositories.salon.collectAsStateWithLifecycle()  (real StateFlow subscription)
  → currentSalon == null → SalonIdentityCard(isLoading = true) → "در حال بارگذاری..." (explicit, honest loading state)
LaunchedEffect(Unit) runs ManagerRepositories.initialize(context)
  → sets _salon.value = <real salon>
  → StateFlow emits → collectAsStateWithLifecycle recomposes the identity card automatically
  → card now shows the real, current active salon's name/description/active-status
Logout → ManagerAuthViewModel.clearSession() → ManagerRepositories.clearActiveSalon() → _salon.value = null, salonId = null
  → next Dashboard entry starts from a genuine loading state, never a leftover salon from the previous session
```

---

## Tests

`./gradlew testManagerDevDebugUnitTest` — **160 tests run, 158 passed, 2 failed.**

The 2 failures are both in `BackendAuthFlowVerificationTest` (`raw login call returns a well-formed token pair`, `register, login, an authenticated call, and refresh all succeed against the live backend`) — pre-existing, network-dependent tests that require a live backend at `localhost:8080`, unrelated to this change and already documented as a known baseline failure in prior sessions this branch. No test newly broken by this fix; no test exists yet that directly covers `SalonIdentityCard`/`ManagerDashboardScreen`/`ManagerRepositories.salon`, so none needed updating for the signature change beyond the one non-test call site (`ManagerSalonMediaViewModel`) already listed above.

## Build Result

`./gradlew assembleManagerDevDebug` — **BUILD SUCCESSFUL**, exit code 0, no warnings/errors surfaced.

---

*Android only — no backend files touched, no API contract changed, no test/fake data created. Nothing committed, nothing pushed.*
