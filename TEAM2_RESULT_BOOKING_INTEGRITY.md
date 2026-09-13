# TEAM2 RESULT — BOOKING TRANSACTION INTEGRITY (TEAM2-001)

Priority: P0
Status: FIXED
Repository modified: ANDROID (`ROJAN_DesignLab_Main1`)
Repository inspected, not modified: BACKEND (`ROJAN_Backend`)

---

## PROBLEM CONFIRMED

The customer booking flow could show "Booking Success" — and grant
loyalty/wallet rewards and schedule reminders — **without any confirmed
backend booking existing**. This was verified by direct source inspection
of the full chain, not inferred:

1. `BookingConfirmationViewModel.confirmBooking()` called the real
   `POST /api/v1/bookings` and took `.getOrNull()?.id` — collapsing every
   failure mode (network error, non-2xx response, malformed response body)
   into a plain `null`, with no distinction and no surfaced error.
2. `BookingConfirmationScreen.kt` invoked its `onConfirmClick` callback
   unconditionally with that (possibly-null) id.
3. `RojanNavGraph.kt`'s `onConfirmClick` handler unconditionally called
   `CustomerEcosystemViewModel.bookAppointment(...)` (the reward/wallet/
   reminder side effects) and unconditionally navigated to
   `RojanDestinations.BOOKING_SUCCESS` — regardless of whether a real
   booking id was ever obtained.

This is exactly the finding recorded as `TEAM2-001` in
`ROJAN_AI_ENGINEERING/INSPECTIONS/TEAM2/TEAM2-001_booking_success_ignores_backend_failure.md`.
Re-inspecting the current source confirmed the defect was still present
and unchanged.

A second, related defect was found and confirmed during this pass while
inspecting "Booking DTO mapping" / "Response contract" per the task's
inspection checklist: `safeApiCall` (the shared helper wrapping every
repository's network call) only caught `HttpException` and `IOException`.
`retrofit2:converter-kotlinx-serialization` throws
`kotlinx.serialization.SerializationException` — a plain `RuntimeException`,
neither of those — when a 2xx response body doesn't decode into the
expected DTO shape. That exception passed through `safeApiCall` uncaught,
which would have **crashed** the coroutine instead of producing a
`Result.failure` an error state could be built from. This directly blocked
the acceptance criterion "Invalid response → Error state" from being
achievable at all, and is fixed as part of this same change (see Files
Changed).

## ROOT CAUSE

`BookingConfirmationViewModel`'s original doc comment stated the design
intent explicitly: at the time it was written, Android auth was frozen and
this call was *expected* to 401 unconditionally, so gating the (working)
local demo success flow on a call known to be permanently dead would have
been a regression. The "try the real call, ignore any failure, proceed
locally either way" behavior was a deliberate trade-off under that
constraint.

Auth was wired for real in a later, separate milestone
(`di/BackendApiContainer.kt`'s `buildAuthenticatedRetrofit`, confirmed
live: it attaches a real bearer token via `AuthInterceptor` and transparently
refreshes via `TokenAuthenticator`). That milestone did not revisit this
call site. The "silently proceed regardless" behavior that was safe when
the call was known-dead became a real, user-facing integrity gap the
moment the call could genuinely succeed *or* genuinely fail for ordinary
reasons (a double-booking conflict, a stale salon/service/specialist id, a
transient network error).

## FILES CHANGED

All changes are scoped to the booking-confirmation transaction path only.

| File | Change |
|---|---|
| `app/src/main/java/ai/rojan/designlab/presentation/booking/BookingConfirmationViewModel.kt` | `confirmBooking`'s `onResult` callback now fires **only** on a confirmed backend booking with a non-blank id. Added `submitError: String?` state, surfaced on any failure (network, HTTP, invalid response, blank id, or missing required selection state). Updated class/method doc comments to state the new contract and remove the stale "auth frozen" reasoning. |
| `app/src/main/java/ai/rojan/designlab/data/remote/SafeApiCall.kt` | Added `InvalidResponseException` and a `catch (e: SerializationException)` branch in `safeApiCall`, so a response body that fails to decode becomes a `Result.failure` instead of an uncaught crash. Purely additive — no existing exception-handling path changed. |
| `app/src/main/java/ai/rojan/designlab/presentation/common/ErrorMessages.kt` | Added a `userMessageFor` branch for `InvalidResponseException` (reuses the existing Persian error-copy convention). |
| `app/src/main/java/ai/rojan/designlab/screens/bookingflow/BookingConfirmationScreen.kt` | `onConfirmClick`'s `backendBookingId` parameter is now non-null `String` (was nullable). Extracted the confirm action into one local lambda shared by the primary button and a new retry action. Added a `RojanErrorState` block (existing app-wide error-state component — no new UI pattern introduced) shown when `submitError` is non-null, with a "تلاش مجدد" (retry) action. |
| `app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt` | Updated the stale doc comment on the `onConfirmClick` handler to state that reaching it (and the `BOOKING_SUCCESS` navigation immediately below it) is now itself proof of a confirmed backend booking. No logic change needed here — this handler was already correctly structured; it was only ever reachable with an unverified id before this fix. |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Added `kotlinx-coroutines-test` as a test-only dependency (matching the existing `kotlinx-coroutines-android` version, 1.9.0) — required to drive `viewModelScope` synchronously from a plain JUnit test via `Dispatchers.setMain`. No previous ViewModel in this codebase had a unit test, so this dependency did not exist yet. |

Two new test files were added (see Tests Added below); no other files were
touched. `local.properties` was created locally to point at this machine's
Android SDK so the build could run — it is git-ignored and is not part of
this change set.

## ARCHITECTURE IMPACT

None beyond the transaction-integrity fix itself:

- No UI was redesigned — `RojanErrorState` is an existing, already-used
  component (see `BookingTimeScreen.kt`), applied here for the first time
  to this specific screen.
- No unrelated architecture was touched. Authentication, RBAC, the Manager
  module's data layer, and local caching are untouched, per the task's
  explicit instruction not to mix those into this fix.
- `safeApiCall`'s new `SerializationException` handling is shared by every
  repository, not booking-specific, but is a pure addition (a new `catch`
  branch) that changes behavior only for a case that previously crashed —
  it does not alter any currently-passing path for any other repository.
- The public contract of `BookingConfirmationScreen.onConfirmClick` changed
  from `(String?, BookingSummary) -> Unit` to `(String, BookingSummary) -> Unit`.
  The only call site (`RojanNavGraph.kt`) was updated; no other file
  references this composable's parameters (verified by a repo-wide search).

## TESTS ADDED

**`app/src/test/java/ai/rojan/designlab/presentation/booking/BookingConfirmationViewModelTest.kt`** (new, 7 tests) — covers all 5 required acceptance scenarios plus 2 extra edge cases, using a hand-written `FakeBookingRepository` (no mocking library is present in this project):

1. `backend success with a persisted booking id calls onResult and clears submitError` — **Backend booking success → Success screen** (onResult is the gate the screen's success navigation depends on).
2. `an HTTP failure sets submitError and never calls onResult` — **HTTP failure → Error state**.
3. `a connectivity failure sets submitError and never calls onResult` — additional network-layer failure case.
4. `an undecodable (invalid) response sets submitError and never calls onResult` — **Invalid response → Error state**.
5. `a response with a blank booking id is treated as a failure, not a success` — **Missing booking ID → Error state**.
6. `missing required selection state fails without ever calling the repository or onResult` — defensive guard-clause coverage.
7. `a retry after failure that now succeeds calls onResult and clears the earlier submitError` — proves the retry path works and doesn't leak stale error state.

Every failure-path test (`2`–`6`) asserts `onResult` is never invoked, which
is the direct, mechanical proof of **"Reward/wallet not executed on
failure"**: `CustomerEcosystemViewModel.bookAppointment` (the reward/wallet/
reminder grant) is only ever reachable downstream of `onResult` firing —
proving `onResult` didn't fire is proving the reward chain never ran.

**`app/src/test/java/ai/rojan/designlab/data/remote/SafeApiCallTest.kt`** (new, 3 tests) — covers the root-cause fix directly: a successful call passes through unchanged, a connectivity failure becomes `NetworkUnavailableException`, and a response-body decode failure becomes `InvalidResponseException` (`Result.failure`) instead of propagating as an uncaught crash.

## VALIDATION RESULTS

**Android:**

| Check | Result |
|---|---|
| `assembleCustomerDebug` + `assembleManagerDebug` (compile all flavors) | ✅ `BUILD SUCCESSFUL` (both APKs packaged) |
| `testCustomerDebugUnitTest` + `testManagerDebugUnitTest` | ✅ 10/10 new tests pass on both flavors. 2 pre-existing failures in `BackendAuthFlowVerificationTest`, which is documented in its own file header as requiring a live `ROJAN_Backend` server at `localhost:8080` and "not wired into any CI/build gate" — no such server was running; unrelated to this change and not newly broken by it. |
| `lintCustomerDebug` + `lintManagerDebug` | ⚠️ 1 pre-existing lint error on each flavor (`ViewModelConstructorInComposable` in `app/src/androidTest/.../AuthScreenScreenshotTest.kt:70`), plus 94/98 pre-existing warnings — confirmed by `git diff` that this file was not touched by this change, and by grepping the lint report that none of the 6 files this change modified appear in it anywhere. No lint baseline is configured in this project (`android.lint.baseline` is unset), so this pre-existing error was already failing a clean `lintCustomerDebug`/`lintManagerDebug` run before this change; it is an authentication-screen-test issue, out of scope per "do not mix authentication ... into this fix." |

**Backend:**

| Check | Result |
|---|---|
| `./gradlew build` (all modules: domain, application, api, infrastructure, bootstrap) | ✅ `BUILD SUCCESSFUL` — compiles and all existing tests pass (including `bootstrap:test`, which runs against an embedded Postgres instance). |

No backend file was modified — inspection of `BookingController`,
`BookingUseCases`, `BookingRepositoryAdapter` (JPA + Postgres
advisory-lock-based overlap checking), and the idempotency-key persistence
(`IdempotencyAdapter`, DB-backed via `V4__idempotency_keys.sql`) found the
backend's booking creation, persistence, and response contract already
sound: a created booking is genuinely persisted before the 201 response is
returned, and the returned `Booking.id` is always the real, saved entity's
id. This confirms the defect was entirely client-side.

## REMAINING RISKS

- **Pre-existing lint error is a real, unresolved defect** (`AuthViewModel`
  constructed directly in a composable in an androidTest screenshot test) —
  left untouched per this task's scope boundary, but it means neither
  `lintCustomerDebug` nor `lintManagerDebug` can pass cleanly today
  independent of this fix. Recommend a separate, scoped task once
  authentication work is in scope.
- **No instrumented/Compose UI test** was added to prove
  `BookingConfirmationScreen` actually renders `RojanErrorState` and that
  the retry button re-triggers `confirmBooking` end-to-end at the UI layer
  — coverage here is at the ViewModel level (where the actual
  success/failure gating decision is made) plus a manual compile/package
  verification that the screen still builds against the new non-null
  `onConfirmClick` signature. If a Compose UI test harness is added to this
  project later, this screen would be a good candidate to extend coverage
  to.
- **`TEAM2-004` (My Appointments never reads real backend bookings) is
  still open** and was explicitly out of scope for this task. Even with
  this fix, a customer has no in-app way to see a booking that succeeded
  on the backend, which was noted in the original inspection as making
  `TEAM2-001`-class regressions harder to catch via manual QA. Recommend
  prioritizing `TEAM2-004` next given the P1 ranking already assigned to
  it.
- **The idempotency key is regenerated fresh on every tap**, including a
  manual retry after a failure (unchanged behavior, not introduced by this
  fix) — this matches `BookingRepository.createBooking`'s own documented
  contract ("a fresh key ... per distinct user attempt"), so a retry is
  correctly treated as a new attempt rather than a duplicate of the failed
  one. No action needed, noted for completeness.
- This fix does not change the **Manager-flow** booking creation path
  (`ManagerBookingViewModel`, `TEAM2-002`) — that path does not call any
  backend API at all and is unaffected by anything here.

---

**Next priority (`TEAM2-004`, `TEAM2-003`, or `TEAM2-002`) requires
separate approval before starting, per the stop condition on this task.**
