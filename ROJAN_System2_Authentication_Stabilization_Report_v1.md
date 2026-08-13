# ROJAN System2 Authentication Stabilization Report v1

**Branch:** `feature/android-reception-app`. **Scope:** exactly the 4 approved items (phone normalization, authentication error classification, authentication state handling improvements, tests) — no more. Not committed, not pushed.

**Pre-change diff review (as instructed):** the uncommitted working tree was reviewed before writing this report. It contains exactly the approved-scope files below, plus one file that does **not** belong to this scope: `manager/domain/repository/ManagerInviteRepository.kt` (60 lines) — a placeholder-only invite-repository interface from the separately-approved "Phase C: Integration Preparation" item of the earlier Android Parallel Work task. It has nothing to do with authentication, is not phone normalization/error handling/auth state/tests, and is excluded from every section below. It was not touched, added, or modified as part of this task — flagged here only so it isn't mistaken for part of this scope when the working tree is eventually reviewed for commit.

**Restrictions verified, not just followed:**
- **No refactor**: every change is additive (new exception type, new `when` branches, new file, new route) or a contained bug-fix edit within an existing method — no class was restructured, no interface signature changed shape, no inheritance introduced.
- **Manager and Reception auth layers not merged**: `ManagerAuthViewModel`/`ReceptionAuthViewModel` remain two independent classes; `ActiveSalonUiState` remains two independent, byte-identical copies (confirmed via diff in the prior architecture-mapping turn) — the fix was applied to both independently, not consolidated.
- **ActiveSalon logic not moved**: `resolveActiveSalon`/`refreshIdentityContext`/`retryIdentityResolution` still live as private/public methods inside each ViewModel exactly where they did before; nothing was extracted to `ActiveSalonContext.kt` or any new shared file.
- **No shared-architecture change**: `SafeApiCall.kt`/`ErrorMessages.kt` gained a new exception type and new `when` branches respectively — additive extensions of an existing shared component, not a restructuring of it.
- **No backend modification**: re-confirmed via `git status` in `ROJAN_Backend` — zero changes there from this task.
- **No API contract change**: no DTO field added/removed/renamed, no endpoint path changed. Verified below per item.

---

## Changed files

**Modified (11):**

| File | Change |
|---|---|
| `data/remote/SafeApiCall.kt` | New `RequestTimeoutException`; `SocketTimeoutException` caught before the generic `IOException` branch |
| `presentation/common/ErrorMessages.kt` | `userMessageFor()` — distinct `400`, `5xx`, and timeout messages |
| `presentation/auth/AuthViewModel.kt` | Phone normalization in `requestOtp` (Customer) |
| `manager/presentation/auth/ManagerAuthViewModel.kt` | Phone normalization; `refreshIdentityContext()` failure branch now resolves `activeSalonState` to `Error` instead of leaving it stuck at `Loading`; new `retryIdentityResolution()` |
| `reception/presentation/auth/ReceptionAuthViewModel.kt` | Phone normalization only (its state-handling fix predates this task) |
| `manager/navigation/ManagerDestinations.kt` | New `ACCESS_ERROR` route constant |
| `manager/navigation/ManagerRootGraph.kt` | `startDestination` resolution includes the `Error` branch |
| `manager/navigation/ManagerNavGraph.kt` | `ACCESS_ERROR` composable registered; `OTP_AUTH`'s `onAuthenticated` inspects the resolved `activeSalonState` instead of always navigating to Dashboard |
| `manager/screens/auth/ManagerOtpAuthScreen.kt` | `onAuthenticated` waits for `activeSalonState` to leave `Loading` before firing |
| `manager/screens/auth/ManagerSalonSelectionScreen.kt` | Retry button added to its existing `Error` branch |
| `test/.../manager/presentation/auth/ManagerAuthViewModelTest.kt` | +4 test cases |

**New (4):**

| File | Purpose |
|---|---|
| `domain/phone/PhoneNumberNormalizer.kt` | Shared normalizer |
| `manager/screens/auth/ManagerAccessErrorScreen.kt` | Makes Manager's now-reachable `Error` state navigable |
| `test/.../domain/phone/PhoneNumberNormalizerTest.kt` | 6 tests |
| `test/.../presentation/common/ErrorMessagesTest.kt` | 7 tests |

**API contract verification:** `ApiErrorDto`, `AuthResponseDto`, `OtpIssuedResponseDto`, `UserResponseDto` — all unchanged, re-checked directly. No Retrofit method signature added, removed, or altered.

---

## Apps affected

| App | Phone normalization | Error classification | Auth state handling |
|---|---|---|---|
| Customer | ✅ | ✅ (shared, automatic) | Reviewed in the prior architecture-mapping turn — no bug found, nothing changed |
| Manager | ✅ | ✅ (shared, automatic) | ✅ Fixed (stuck-at-`Loading` bug + new `ACCESS_ERROR` screen/route + OTP-race fix + retry button) |
| Reception | ✅ | ✅ (shared, automatic) | Unaffected — already fixed in an earlier phase |

---

## Tests added

**17 new/extended test cases:**
- `PhoneNumberNormalizerTest` — 6 (local-format conversion, E.164 passthrough, whitespace trimming both shapes, no-guess passthrough for an unrecognized shape, blank input, realistic-length number).
- `ErrorMessagesTest` — 7 (`400` distinct from network message, `400` distinct from timeout, `400` never leaks the raw backend text, timeout distinct from network, `5xx` distinct from both, every known case maps to a non-blank message, category stability across different raw messages).
- `ManagerAuthViewModelTest` — 4 (failure resolves to `Error` not stuck `Loading`; retry succeeding after failure; retry failing again stays at `Error`; logout clears an `Error` state back to `Loading`).

**Verification, fresh this session:**
```
./gradlew assembleCustomerDevDebug assembleManagerDevDebug assembleReceptionDevDebug testCustomerDevDebugUnitTest
```
All three flavors — **BUILD SUCCESSFUL**. Test suite — **133 total, 131 passed, 2 failed** (the same pre-existing, network-dependent `BackendAuthFlowVerificationTest` cases, unrelated to this work, unchanged in count and identity across every run this branch has had). Per-app login-flow suites: `AuthViewModelTest` 15/15, `ManagerAuthViewModelTest` 22/22, `ReceptionAuthViewModelTest` 8/8.

---

## Remaining risks

- **Duplicated, not shared, `ActiveSalonUiState` + resolution logic** (Manager/Reception) — explicitly accepted, not fixed, per this task's "do not merge auth layers / do not move ActiveSalon logic" restriction. Risk: a future fix to one copy not applied to the other. Flagged for a separate, explicitly-scoped decision — not actioned here.
- **`ManagerInviteRepository.kt` sitting in the same working tree** — not a risk to this task's correctness, but a commit-hygiene note: whoever stages this work for commit should confirm it's included deliberately (it belongs to a different, already-approved task) rather than accidentally swept in with `git add -A`.
- **The 2 pre-existing test failures** (`BackendAuthFlowVerificationTest`) will keep failing in any sandboxed/offline environment — not a regression, but worth the eventual owner knowing they're not silently ignorable signal in a real CI environment with network access.
- **No backend-side change accompanies this work** — Manager's fix makes a real `/salon-access` failure honest and retryable; it does not make salon data reachable. That remains blocked on the backend items already documented in prior reports (`ROJAN_System1_Backend_Decision_v2.md` §4), unchanged by this task.

---

**Not committed, not pushed.**
