# ROJAN System2 Android Parallel Work Report v1

**Branch:** `feature/android-reception-app`. **Scope:** Phases A-D as specified. Not committed, not pushed — awaiting review.

**Note on the referenced approval document:** `ROJAN System 1 → System 2 Android Parallel Work Approval v1.0` was searched for on this machine (both repos, common locations) and does not exist as a file. Proceeding on the explicit, detailed scope given directly in this task's instructions, which was sufficient to act on independently.

**Restrictions honored, verified:** zero backend files touched (confirmed — all changes are in `ROJAN_DesignLab`); zero RBAC logic added or changed (no new permission/role check anywhere); zero API contract changes (no DTO field removed/renamed, only additive); no new global role (`UserRole`/`SalonMemberRole` untouched); no authentication *architecture* change (Customer's `SessionState`/Manager's `ManagerAuthState`/Reception's `ReceptionAuthState` sealed-interface shapes are all unchanged — every fix is a bug fix within the existing shape, not a redesign).

---

## 1. Changed files

**Phase A — modified (11 files):**

| File | Change |
|---|---|
| `data/remote/SafeApiCall.kt` | New `RequestTimeoutException`, `SocketTimeoutException` caught distinctly before the generic `IOException` branch |
| `presentation/common/ErrorMessages.kt` | `400` now maps to a distinct validation message (never the network message, never the raw backend text); `5xx` range gets its own message; timeout gets its own message |
| `presentation/auth/AuthViewModel.kt` | Phone normalization in `requestOtp` |
| `manager/presentation/auth/ManagerAuthViewModel.kt` | Phone normalization; `refreshIdentityContext` failure branch now resolves `activeSalonState` to `Error` instead of leaving it stuck at `Loading`; new `retryIdentityResolution()` |
| `reception/presentation/auth/ReceptionAuthViewModel.kt` | Phone normalization only (the stuck-at-Loading bug here was already fixed in an earlier phase) |
| `manager/navigation/ManagerDestinations.kt` | New `ACCESS_ERROR` route |
| `manager/navigation/ManagerRootGraph.kt` | `startDestination` resolution now includes the `Error` branch |
| `manager/navigation/ManagerNavGraph.kt` | `ACCESS_ERROR` composable registered; `OTP_AUTH`'s `onAuthenticated` now inspects the real, settled `activeSalonState` instead of always navigating to Dashboard |
| `manager/screens/auth/ManagerOtpAuthScreen.kt` | `onAuthenticated` waits for `activeSalonState` to leave `Loading` before firing |
| `manager/screens/auth/ManagerSalonSelectionScreen.kt` | Retry button added to its existing `Error` branch |

**Phase A — new (2 files):**

| File | Purpose |
|---|---|
| `domain/phone/PhoneNumberNormalizer.kt` | Shared normalizer, used by all three apps |
| `manager/screens/auth/ManagerAccessErrorScreen.kt` | Manager's counterpart to the already-existing `ReceptionAccessErrorScreen` |

**Phase B — new (3 files):**

| File | Tests |
|---|---|
| `test/.../domain/phone/PhoneNumberNormalizerTest.kt` | 6 |
| `test/.../presentation/common/ErrorMessagesTest.kt` | 7 |
| `test/.../manager/presentation/auth/ManagerAuthViewModelTest.kt` (extended, not new) | +4 (18 → 22) |

**Phase C — new (1 file):**

| File | Purpose |
|---|---|
| `manager/domain/repository/ManagerInviteRepository.kt` | Placeholder-only interface (issuing side) |

**Untouched, confirmed not part of this work:** `ROJAN_Customer_Git_Status_Report_v1.md` remains untracked — pre-existing, unrelated, excluded exactly as it was from the prior commit.

---

## 2. Apps affected

| App | Phone normalization | Error classification | Auth-state fix |
|---|---|---|---|
| Customer | ✅ Applied | ✅ (shared, automatic) | Reviewed — no equivalent bug found (no `activeSalonState`-style chain exists in Customer's flow at all; its `identityContext` failure path already correctly resolved to `Error`, never `Loading`-stuck) |
| Manager | ✅ Applied | ✅ (shared, automatic) | ✅ Fixed — identical stuck-at-`Loading` bug to the one already fixed for Reception in an earlier phase, found during this review and now fixed here |
| Reception | ✅ Applied | ✅ (shared, automatic) | Already fixed in an earlier phase; unaffected by this pass beyond phone normalization |

**Screens affected:**
- New: `ManagerAccessErrorScreen` (Manager).
- Modified: `ManagerOtpAuthScreen`, `ManagerSalonSelectionScreen` (retry button) — both Manager.
- No screen changes needed for Customer or Reception — Customer had nothing to fix, Reception's screens were already fixed.

---

## 3. Tests added

**17 new/added test cases, 0 new failures:**

- `PhoneNumberNormalizerTest` — 6 cases: local-format conversion, already-E.164 passthrough, whitespace trimming (both shapes), no-guessing passthrough for an unrecognized shape, blank input, a full realistic-length number.
- `ErrorMessagesTest` — 7 cases: `400` distinct from network message, `400` distinct from timeout message, `400` never leaks the raw backend text, timeout distinct from network, `5xx` distinct from both network and validation, every known status/failure kind maps to a non-blank message, same category is stable across different raw messages.
- `ManagerAuthViewModelTest` — 4 new cases mirroring `ReceptionAuthViewModelTest`'s existing bug-fix coverage exactly: failure resolves to `Error` not stuck `Loading`; `retryIdentityResolution` succeeding after a prior failure; `retryIdentityResolution` failing again stays at `Error`; `logout` resets `Error` back to `Loading`.

**Verification (fresh run, not reused from a prior report):**
```
./gradlew assembleCustomerDevDebug assembleManagerDevDebug assembleReceptionDevDebug testCustomerDevDebugUnitTest
```
All three flavors — **BUILD SUCCESSFUL**. Test suite: **133 tests, 131 passed, 2 failed** (the same pre-existing, network-dependent `BackendAuthFlowVerificationTest` cases present since before this branch started — unrelated to this work, unchanged in count). Per-suite: `AuthViewModelTest` (Customer) 15/15, `ManagerAuthViewModelTest` 22/22, `ReceptionAuthViewModelTest` 8/8 — all three login flows verified.

---

## 4. Screens affected

Covered in §2 — restated here for the requested structure: `ManagerAccessErrorScreen` (new), `ManagerOtpAuthScreen` (modified), `ManagerSalonSelectionScreen` (modified). No Customer or Reception screens changed.

---

## 5. Remaining backend dependencies

Unchanged by this phase — this was Android-only work:

- `SalonMembership` persistence + `GET /users/me/salon-access` still absent — Manager and Reception both still cannot reach real salon data with a non-owner account (Manager's fix here makes the *failure* honest and retryable, it doesn't remove the failure).
- `SalonInvite`/`InviteController` still absent — now has placeholder interfaces on **both** sides of the flow (`ReceptionInviteRepository` for accepting, `ManagerInviteRepository` for issuing, added this phase), still zero backend and zero UI on either side.
- The OTP-auto-registers-as-`CUSTOMER` gap (blocks a brand-new phone number from ever completing an invite) remains unaddressed by any System 1 decision — unchanged.

---

## Phase D — code quality findings (reviewed, not acted on)

Two duplication patterns found and documented, deliberately **not** refactored — extracting either would mean introducing a shared base class or helper touched by multiple ViewModels, which risks crossing into the "no authentication architecture change" restriction rather than a contained bug fix:

1. **`requestOtp`/`resendOtp`/`editPhoneNumber`** are structurally near-identical across `AuthViewModel`, `ManagerAuthViewModel`, and `ReceptionAuthViewModel` — pre-existing duplication, not introduced by this phase.
2. **`refreshIdentityContext`/`retryIdentityResolution`/`resolveActiveSalon`** are now near-identical between `ManagerAuthViewModel` and `ReceptionAuthViewModel` (Customer doesn't have this trio at all, having no salon-selection concept). This duplication was *increased* by this phase's fix (the same fix applied twice, once per class) — a deliberate trade-off: fixing the real bug in both places now, rather than delaying the fix to first build a shared abstraction neither app currently has.

Both are flagged here as a legitimate follow-up for a future, explicitly-scoped "extract shared auth base" task — not silently deferred.

**Architecture/repository/error-handling consistency, verified clean:** every new file's imports respect existing layering (domain files have zero Android/network imports); every failure branch across the whole app consistently routes through `userMessageFor` or an already-justified specific mapper (fresh grep, zero exceptions found); `ManagerInviteRepository` has zero wiring anywhere, matching `ReceptionInviteRepository`'s established placeholder pattern exactly; fresh mock/fake sweep across every file touched this phase — zero matches in production code.

---

**Not committed, not pushed — awaiting review.**
