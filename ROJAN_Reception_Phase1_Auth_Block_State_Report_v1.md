# ROJAN Reception Phase 1 — Authentication Block-State Report v1

**Scope:** Resilient blocked-state UX only (Option 1, as selected), per `ROJAN_Reception_Phase1_Readiness_Report_v1.md` §4.1. Nothing else from Phase 1 (Invite acceptance, Dashboard, Booking management) was started.

**Constraints honored (verified, not just followed):**
- **No mock API** — no fake/stub HTTP layer was added. `ReceptionAuthViewModel` still calls the real `BackendAuthRepository`/`CurrentUserIdentityContextRepository` exclusively.
- **No fake salon-access data** — confirmed by grep: zero `Fake`/`Mock`/`Stub` references anywhere in `app/src/main/java/ai/rojan/designlab/reception/` (production code). The only fakes that exist are inside the new **test** file (`src/test/...`), which is standard JVM-unit-test practice, never shipped, and structurally identical to the fakes `ManagerAuthViewModelTest.kt` already uses for the same purpose.
- **No backend changes** — `ROJAN_Backend` has zero changes from this work (re-checked `git status` there: the only modification present, `.claude/settings.local.json`, predates this work and is unrelated).
- **No permission bypass** — the provisional `RECEPTION_GATE_ROLE = "MANAGER"` constant from Phase 0 is untouched. No new code grants access on any weaker condition than before.

---

## 1. Changed files

**Modified (existing Phase 0 files):**

| File | Change |
|---|---|
| `reception/presentation/auth/ReceptionAuthViewModel.kt` | Fixed the failure branch of `refreshIdentityContext()` to resolve `activeSalonState` to `Error` instead of leaving it stuck at `Loading`; added public `retryIdentityResolution()`. |
| `reception/navigation/ReceptionDestinations.kt` | Added `ACCESS_ERROR` and `PROFILE` route constants. |
| `reception/navigation/ReceptionRootGraph.kt` | `startDestination` resolution now routes to `ACCESS_ERROR` when `activeSalonState is Error` (cold-start restore path). |
| `reception/navigation/ReceptionNavGraph.kt` | Registered `ACCESS_ERROR`/`PROFILE` composables; `OTP_AUTH`'s `onAuthenticated` now inspects the real, settled `activeSalonState` instead of always navigating to `DASHBOARD`. |
| `reception/screens/auth/ReceptionOtpAuthScreen.kt` | `onAuthenticated` no longer fires the instant `authState` becomes `Authenticated` — now also waits for `activeSalonState` to leave `Loading`. |
| `reception/screens/auth/ReceptionSalonSelectionScreen.kt` | Added a retry button to the already-existing `ActiveSalonUiState.Error` notice branch. |
| `reception/screens/dashboard/ReceptionDashboardScreen.kt` | `onLogoutClick` replaced with `onProfileClick` — logout moved to the new Profile screen. |

**New files:**

| File | Lines | Purpose |
|---|---|---|
| `reception/screens/auth/ReceptionAccessErrorScreen.kt` | 112 | The resolved terminal screen for a salon-access failure — message, retry, logout. |
| `reception/screens/profile/ReceptionProfileScreen.kt` | 69 | Real name/phone + the real logout action. |
| `src/test/.../reception/presentation/auth/ReceptionAuthViewModelTest.kt` | 308 | Unit coverage for the fix (see §4). |

No other file, in this repo or `ROJAN_Backend`, was touched.

---

## 2. Behavior implemented

**Root cause found and fixed:** `refreshIdentityContext()`'s failure branch previously only set `identityContext` to `UiState.Error` — it never touched `activeSalonState`, which stayed at `Loading` forever. `ReceptionRootGraph`'s splash gate waits specifically for `activeSalonState` to leave `Loading`, so a real `/salon-access` failure produced an infinite splash screen with no error, no retry, and no way out short of force-quitting the app.

**Two paths reach this state, both now fixed:**

1. **Cold-start restore** — `ReceptionRootGraph`'s `startDestination` resolution now includes an `activeSalonState is Error -> ACCESS_ERROR` branch.
2. **Fresh OTP login** — previously, `ReceptionOtpAuthScreen` fired `onAuthenticated()` the instant `authState` became `Authenticated`, which happens synchronously, before the async salon-access call even starts. This unconditionally sent every fresh login to `DASHBOARD`, bypassing both `SALON_SELECTION` and any future `ACCESS_ERROR` state. The screen now waits for `activeSalonState` to leave `Loading` too, and `ReceptionNavGraph` inspects the real, settled value to route to `SALON_SELECTION`, `ACCESS_ERROR`, or `DASHBOARD` correctly.

**New user-facing behavior:**
- A salon-access failure (today: certain, since the endpoint doesn't exist — see §3) now lands on a real screen: an explanatory message, a "تلاش مجدد" (retry) button wired to `retryIdentityResolution()`, and a "خروج از حساب" (logout) button.
- Salon Selection's pre-existing (already-handled, just previously dead-ended) error branch gained the same retry affordance.
- Dashboard's inline logout was replaced by a "پروفایل" (profile) entry point; the new Profile screen shows the real authenticated name/phone and holds the actual logout action.

**Related, unfixed issue noted for the record:** `ManagerAuthViewModelTest.kt`'s own test (`a salon-access failure does not roll back an already-authenticated manager session`) only asserts `identityContext`, never `activeSalonState` — meaning Manager's `ManagerAuthViewModel` has the identical stuck-at-`Loading` bug, untested and unfixed. Out of scope here per "do not modify unrelated files" — flagged, not touched.

---

## 3. Remaining backend dependency

Unchanged from `ROJAN_Reception_Backend_Dependency_Checklist_v1.md` — this phase made the *failure path* resilient, it did not remove the failure itself:

- **`GET /api/v1/users/me/salon-access` still does not exist.** Every real login will deterministically reach the new `ACCESS_ERROR` screen today, not `DASHBOARD` — this is the correct, honest behavior, not a bug. Retry will keep failing until this endpoint ships.
- `SalonMembershipController` and a real backend `Permission` model remain unbuilt (checklist §1.1, §2) — even once `/salon-access` exists, there is still no way to grant a phone number `RECEPTIONIST` access.
- The provisional `RECEPTION_GATE_ROLE = "MANAGER"` role gate (checklist §3, plan §4 item 5) is unresolved and untouched by this phase.

**Net effect:** the app can now be run, logged into with a real `MANAGER`-role account, and will visibly and correctly stop at "دسترسی به سالن امکان‌پذیر نیست" with a working retry loop — instead of hanging. This is real, verifiable progress, not a workaround.

---

## 4. Tests

**New:** `ReceptionAuthViewModelTest.kt` — hermetic JVM unit tests (no Android framework, no real backend), same in-memory-fake pattern already established by `ManagerAuthViewModelTest.kt`. 8 tests, scoped to the behavior this phase actually changed:

| Test | Verifies |
|---|---|
| `a salon-access failure resolves activeSalonState to Error, not stuck at Loading` | The core bug fix. |
| `the Error message surfaced to activeSalonState matches the identity-context failure` | Both state flows carry the same message, not a divergent one. |
| `retryIdentityResolution re-attempts and can resolve to Active after a prior failure` | Retry actually works end-to-end (Error → Active). |
| `retryIdentityResolution that fails again stays at Error, still not stuck at Loading` | Retry failing again doesn't regress into the old stuck-`Loading` state. |
| `zero available salons still resolves to Error, unaffected by the failure-path fix` | The pre-existing "no salons" case wasn't broken by this change. |
| `fresh install with no persisted session lands on the OTP entry step, unauthenticated` | Baseline gate sanity, unchanged behavior. |
| `requesting then verifying an OTP saves the JWT and authenticates` | Baseline OTP flow, unchanged behavior. |
| `logout resets activeSalonState back to Loading, clearing a prior Error` | Logout correctly clears an `Error` state, not just a `Loading`/`Active` one. |

**Result:** `./gradlew testCustomerDevDebugUnitTest --tests "...ReceptionAuthViewModelTest"` — **8/8 passed, 0 failures, 0 errors.**

**Build verification:** `./gradlew assembleReceptionDevDebug` — **BUILD SUCCESSFUL**, no errors, no new warnings.

**Not covered:** no instrumented/on-device UI test of the new screens (`ReceptionAccessErrorScreen`, `ReceptionProfileScreen`) — verification is unit-test + compile-level only, consistent with this session's tooling (no emulator run performed).

---

**Stopping here per instruction — not continuing to Dashboard or Booking management.**
