# ROJAN System2 Authentication Stabilization Plan v1

**Basis:** the architecture mapping delivered in the prior turn (shared vs. app-specific authentication components, verified fresh against source). **Plan only — no code, no refactor, no backend change performed in producing this document.**

**Status note, stated plainly rather than implied:** phone normalization, error-handling classification, and Manager's auth-state fix were already implemented as uncommitted working-tree changes in the prior "Android Parallel Work" session, before this plan was requested. This document does not pretend otherwise — it formally plans/documents that scope (per the structure requested) and, in §5, explicitly surfaces the one place that work made a duplication trade-off rather than a shared-layer one, for your decision rather than a silent default. Nothing new is proposed here beyond what's already described.

---

## 1. Phone normalization changes

**Files affected:**

| File | Role |
|---|---|
| `domain/phone/PhoneNumberNormalizer.kt` | New — the shared function |
| `presentation/auth/AuthViewModel.kt` (Customer) | Call site, inside `requestOtp` |
| `manager/presentation/auth/ManagerAuthViewModel.kt` | Call site, inside `requestOtp` |
| `reception/presentation/auth/ReceptionAuthViewModel.kt` | Call site, inside `requestOtp` |

**Shared vs. app-specific approach:** fully shared, deliberately — one pure function (`normalizeIranianPhoneNumber`) in the `domain` layer (no Android import, independently testable), called identically at each app's single OTP-request entry point. This is a correct case for full sharing, not a judgment call: the phone-number shape rules (`0912...` → `+98912...`, `+98...` passthrough) are a property of the backend's E.164 requirement, not of which app is asking — there is no legitimate reason for three divergent copies. No app-specific variant exists or is planned.

---

## 2. Error handling improvements

**Existing components reused:** `data/remote/SafeApiCall.kt` (`safeApiCall`, `BackendApiException`, `NetworkUnavailableException`) already existed as the single, shared classification layer every repository across all three apps already routed through. This work extends that existing layer rather than introducing a parallel one.

**Files affected:**

| File | Change |
|---|---|
| `data/remote/SafeApiCall.kt` | New `RequestTimeoutException`; `SocketTimeoutException` caught distinctly, before the generic `IOException` branch |
| `presentation/common/ErrorMessages.kt` | `userMessageFor()` — `400` gets its own distinct message (never the network-failure text, never the raw backend string — an existing, deliberate "don't leak backend internals" rule this plan respects, not overrides), `5xx` gets its own message, timeout gets its own message |

No new mapping function was created per app — every app's `onFailure { error -> _errorMessage.value = userMessageFor(error) }` call site is unchanged; the shared function underneath it just got more precise. This is the same "one shared layer, already in place, extended rather than duplicated" shape as §1.

---

## 3. Authentication state improvements

| Aspect | Customer | Manager | Reception |
|---|---|---|---|
| **Loading** | `sessionState`/`identityContext` start at their loading-equivalent value; unchanged | `ManagerAuthState.Checking` + `activeSalonState = Loading`; unchanged | `ReceptionAuthState.Checking` + `activeSalonState = Loading`; unchanged |
| **Error** | `identityContext` failure already correctly resolved to `UiState.Error` — reviewed, no bug found, nothing changed | **Fixed**: `refreshIdentityContext()`'s failure branch previously left `activeSalonState` stuck at `Loading` forever (identical to Reception's already-fixed bug); now resolves to `ActiveSalonUiState.Error` | Already fixed in an earlier phase; unaffected here |
| **Session restore** | `restoreSession(personId)` re-derives identity from the stored token via `BackendAuthRepository.currentUser()`; unchanged | `restoreSession()`, same shape; unchanged | Same shape; unchanged |
| **Logout** | `logout()` resets `identityContext` to `Loading`, clears tokens/session; unchanged | `clearSession()` resets both `identityContext` and `activeSalonState` to `Loading`; unchanged | Same; unchanged |
| **Expired session** | Handled by the shared `TokenAuthenticator.kt` (401 → refresh → retry once; failed refresh clears tokens + session) — one shared component, already in place, untouched by this work | Same shared component | Same shared component |

**New screens (Manager only):** `ManagerAccessErrorScreen.kt` — makes the now-reachable `Error` state navigable (message, retry, logout), mirroring the already-existing `ReceptionAccessErrorScreen.kt`.

**The duplication trade-off, stated explicitly here rather than only in §5:** the Manager fix was applied by duplicating the same edit shape into `ManagerAuthViewModel` (and creating a second, near-identical `ManagerAccessErrorScreen`) rather than extracting `ActiveSalonUiState` + its resolution orchestration (`resolveActiveSalon`/`refreshIdentityContext`/`retryIdentityResolution`) into the shared `ActiveSalonContext.kt` layer that already holds the pure parts (`availableSalons()`). That consolidation is possible and was identified during the architecture mapping — it is **not** part of this plan's scope, per the "no refactor" rule governing this document.

---

## 4. Tests to add/update

Already added, mirroring the structure above:

- `domain/phone/PhoneNumberNormalizerTest.kt` — 6 cases (both normalization rules, whitespace, no-guess passthrough, blank input, realistic-length number).
- `presentation/common/ErrorMessagesTest.kt` — 7 cases (each error category distinct from every other, no raw-message leakage, stability).
- `manager/presentation/auth/ManagerAuthViewModelTest.kt` — 4 new cases, mirroring `ReceptionAuthViewModelTest`'s existing bug-fix coverage exactly (failure resolves to `Error` not stuck `Loading`; retry succeeding; retry failing again; logout clearing an `Error` state).

**Not added, flagged for if/when §3's deferred consolidation happens:** a shared-layer version would need its tests written once against the shared component, replacing the current shape where `ManagerAuthViewModelTest` and `ReceptionAuthViewModelTest` each independently test near-identical salon-resolution behavior. Not proposed as work here — noted so it isn't rediscovered as a surprise later.

---

## 5. Risks

**Architecture changes:** none performed. The one architecture-relevant decision already made (§3) was to *avoid* an architecture change — fixing the bug via a duplicated, contained edit in each ViewModel rather than introducing a shared base/helper that both `ManagerAuthViewModel` and `ReceptionAuthViewModel` would depend on. Trade-off, stated plainly: this keeps the fix low-risk and reviewable in isolation, at the cost of two copies of `ActiveSalonUiState` and its resolution logic that can now drift out of sync if one is changed without the other — the same category of risk already surfaced and accepted for an earlier, unrelated duplication (the booking-response DTO mapper, since consolidated). Whether to consolidate now, later, or not at all is a decision for you, not something this plan resolves unilaterally.

**API contract impact:** none. Every change described in §1-§3 is client-side only:
- Phone normalization happens before a request leaves the device — the backend receives and validates E.164 exactly as it did before; no request/response shape changed.
- Error classification is local exception-handling logic — no DTO field added, removed, or renamed; `ApiErrorDto` is untouched.
- The auth-state fix changes only local `StateFlow` behavior inside `ManagerAuthViewModel`; no network call was added, removed, or changed in shape.

Re-confirmed via `git status` in `ROJAN_Backend`: no file there was touched by this work.

---

**Waiting for approval before any further code changes.**
