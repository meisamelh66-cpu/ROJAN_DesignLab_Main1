# TEAM2 RESULT — MANAGER BOOKING CREATION INTEGRITY FOLLOW-UP

Priority: P0 (regression fix)
Status: **PARTIALLY FIXED — STOPPED at a genuinely missing backend contract, per the task's own instruction.** The confirmed regression (a fake local "success" that silently vanishes from the real Calendar) is fully fixed. Real backend booking creation from Manager is **not implemented** because the backend has no contract for it — reported below, not worked around.
Repository modified: ANDROID (`ROJAN_DesignLab_Main1`)
Repository inspected, not modified: BACKEND (`ROJAN_Backend`)
Branch: `fix/team2-002-manager-data-persistence` (current HEAD `2a38aa3` before this change) — **not pushed**, per instruction.

---

## INSPECTION (before coding, as instructed)

**Manager booking wizard** (`manager/presentation/booking/ManagerBookingViewModel.kt`,
`manager/screens/booking/*.kt`, 7 screens): confirmed unchanged since the
original TEAM2-002 finding — `confirm()` built an
`ai.rojan.designlab.manager.domain.appointment.Appointment` locally and
wrote it via `AppointmentRepository.create(...)`
(`manager.data.InMemoryAppointmentRepository`, an in-memory list) — no
network call anywhere in the wizard. `ManagerBookingReviewScreen`'s
confirm button called `viewModel.confirm(); onConfirmed()`
**unconditionally** — the exact same "call the action, then always
proceed to success" shape TEAM2-001 fixed on the Customer side, never
fixed here.

**Service selection** (`ManagerBookingServiceScreen`) and **specialist
selection** (`ManagerBookingSpecialistScreen`): both read
`ManagerRepositories.services`/`.specialists` — Manager's own separate
in-memory catalog, with its own id scheme (`"s1"`, `"sp1"`, ...)
unrelated to real backend `Service`/`Specialist` ids. Not touched by this
follow-up (see Scope below).

**Customer selection / customer identity**
(`ManagerBookingCustomerScreen`): reads `ManagerRepositories.customers`
(`InMemoryCustomerRepository`) via `viewModel.searchCustomers(query)` — a
fixed, hardcoded seed list of demo customers. This is the crux of the
blocker found below.

**`BookingRepository`/`BookingApi`** (Android): `createBooking(salonId,
serviceId, specialistId, startTime, notes, idempotencyKey)` — confirmed
already real and working (TEAM2-001), but takes no `customerId` parameter
at all, for the reason confirmed next.

**Backend booking creation endpoint** (`BookingController.create`,
`api/src/main/kotlin/ai/rojan/backend/api/booking/BookingController.kt:96-127`),
re-read fresh:

```kotlin
fun create(
    @Valid @RequestBody request: CreateBookingRequest,
    @RequestHeader(IDEMPOTENCY_KEY_HEADER, required = false) idempotencyKey: String?,
    @AuthenticationPrincipal principal: UserDetails,
): ResponseEntity<BookingResponse> {
    val customerId = currentUserResolver.resolve(principal)
    ...
    createBookingUseCase.execute(CreateBookingCommand(..., customerId = customerId, ...))
```

`customerId` is **always** `currentUserResolver.resolve(principal)` — the
authenticated caller's own resolved user id. Confirmed via
`grep -n customerId` across the full booking-creation path
(`BookingController.kt`, `BookingUseCases.kt`): every single occurrence
traces back to this one line. There is no field on it for a different
customer.

**Request/response DTOs** (`BookingDtos.kt`, re-read fresh):
`CreateBookingRequest(salonId, serviceId, specialistId, startTime,
notes)` — no `customerId` field exists on the wire contract at all, not
just unused in the handler.

**Authentication requirements**: `POST /api/v1/bookings` requires a valid
Bearer token (any authenticated role — no role check in `create()` beyond
authentication itself); the *customer* on the resulting booking is always
that token's own account.

**Customer lookup, for completeness**: `UserController.kt` (full file, 29
lines) exposes exactly one endpoint, `GET /api/v1/users/me`. No
`GET /users/{id}`, no search, no salon-scoped customer listing exists
anywhere in the backend (`grep -rl "customers\|CustomerController\|searchUsers\|findByPhone"` across the entire backend source tree returns nothing).

## CONFIRMED MISSING BACKEND CONTRACT (per the task's explicit STOP instruction)

Two independent, both-required gaps — closing only one would still leave
this impossible:

1. **No "book on behalf of a customer" capability.** `POST
   /api/v1/bookings` structurally cannot create a booking attributed to
   anyone but the caller. There is no `customerId` field on
   `CreateBookingRequest`, no alternate code path in
   `CreateBookingUseCase`/`BookingController.create`, and no role check
   that would let a `MANAGER`/salon-owner caller specify one even if the
   field existed.
2. **No customer lookup/search/listing endpoint at all.** Even if gap #1
   were closed, a salon owner has no way to identify an existing
   customer by phone, name, or id to put in that field — `UserController`
   exposes only `/me`.

**This was not worked around.** Per the task's explicit instruction, no
new backend API was invented, and no customer identity was fabricated
(e.g., substituting the manager's own account id, or a synthetic id) to
force a call through. **Real backend booking creation from the Manager
app remains unimplemented, and cannot be implemented until backend work
closes both gaps above.**

## WHAT WAS FIXED

The regression as literally described — "a booking created through the
wizard can appear successful locally but disappear from the real
Calendar" — is fully fixed, by removing the false success rather than by
completing a booking the backend cannot yet accept:

- `ManagerBookingViewModel.confirm()` no longer constructs an
  `Appointment` or calls `AppointmentRepository.create(...)` under any
  circumstance. It returns `Boolean` (`false`, always, until the backend
  contract above exists) instead of `Appointment?`, and sets a new
  `ManagerBookingState.submitError` with a clear, honest, user-facing
  explanation ("ثبت نوبت به نام مشتری هنوز از طریق سرور پشتیبانی نمی‌شود.
  این قابلیت به‌زودی اضافه خواهد شد." — "Booking on behalf of a customer
  isn't supported by the server yet; this capability will be added
  soon.").
- `ManagerBookingReviewScreen`'s confirm button now calls `onConfirmed()`
  **only** when `viewModel.confirm()` returns `true` — never
  unconditionally. Since it currently always returns `false`, the
  wizard's Success screen (`ManagerBookingSuccessScreen`, whose copy
  literally claims "نوبت جدید در تقویم سالن ثبت و قابل مشاهده است" — "the
  new appointment is registered and visible in the salon calendar," which
  TEAM2-002 made demonstrably false) is now structurally unreachable via
  this path. The screen itself is left in place, unremoved — same
  "kept, unreachable, not ripped out" posture `AuthViewModel`'s own
  `submitFirstName`/`editPhoneNumber` already use in this codebase for a
  dormant-until-a-future-milestone flow.
- The review screen surfaces `submitError` via the existing
  `ManagerErrorState` component (introduced in TEAM2-002, reused as-is —
  no new UI) with **no retry action**: this is a structural
  backend-contract gap, not a transient failure, so offering "تلاش مجدد"
  would dishonestly imply retrying could succeed.

`AppointmentRepository`/`InMemoryAppointmentRepository` were **not**
removed: `ManagerBookingViewModel.availableTimes()` still legitimately
reads from it (to compute which of the fixed daily slots are already
taken) — a pre-existing, separate concern from booking *creation*, and
touching it was outside this follow-up's scope (see Remaining Risks).

## ACCEPTANCE CRITERIA — STATUS

| # | Criterion | Status |
|---|---|---|
| 1 | Send a real backend request | ❌ Not possible — no backend contract (see above) |
| 2 | Receive a valid persisted booking ID | ❌ Not possible (same reason) |
| 3 | Show backend-confirmed success only | ✅ No success is ever shown now — none is fabricated either |
| 4 | Appear in Manager Calendar after refresh | ❌ Not possible — nothing is created |
| 5 | Remain after app restart | N/A — nothing is created to persist or lose |
| 6 | Fail honestly on backend/network/validation error | ✅ Fails honestly, always, with a clear explanation |
| 7 | Never silently fall back to in-memory success | ✅ **This is the actual regression fix** — confirmed by test (see below) |

Criteria 1/2/4/5 describe a capability that does not exist yet on the
backend; criteria 3/6/7 — the actual integrity properties the reported
regression violated — are fully satisfied.

## FILES CHANGED

| File | Change |
|---|---|
| `manager/domain/booking/ManagerBookingState.kt` | Added `submitError: String? = null`. |
| `manager/presentation/booking/ManagerBookingViewModel.kt` | `confirm()` rewritten: no `Appointment` construction, no `AppointmentRepository.create` call, returns `Boolean` (documents the exact missing backend contract inline, with file/line-level detail, for future reference when the backend work lands). |
| `manager/screens/booking/ManagerBookingReviewScreen.kt` | Confirm button only calls `onConfirmed()` when `confirm()` returns `true`; renders `submitError` via `ManagerErrorState` (no retry action). |

One new test file (see Tests below). No backend file, and no other
Android file, was modified.

## ARCHITECTURE IMPACT

- **No UI redesign.** Same 7-screen wizard, same Review screen layout;
  the only visible change is an error card appearing where a tap on
  "تایید نهایی" used to silently proceed. `ManagerErrorState` is the
  exact component already introduced for this exact purpose in TEAM2-002
  — not new UI.
- **No architecture rewrite.** `ManagerBookingViewModel`'s dependency
  list, the wizard's navigation graph, and every other screen in it are
  unchanged. This is the narrowest change that removes the confirmed
  false-success path.
- **Deliberately did not touch service/specialist selection or
  date/time availability**, even though they're also on in-memory data —
  wiring them to real backend data would not change the outcome (the
  final step is blocked regardless of where the earlier selections came
  from) and would be speculative scope expansion the task didn't ask for
  once the hard blocker was confirmed. Left as later work if/when the
  customer-attribution gap is closed (see Remaining Risks).

## TESTS

**`app/src/test/java/ai/rojan/designlab/manager/presentation/booking/ManagerBookingViewModelTest.kt`** (new, 5 tests):

1. `confirm with a complete selection never creates a fake local appointment and reports false` — the direct regression test: proves `AppointmentRepository.create` is never called and `confirm()` never returns `true`.
2. `confirm with a complete selection leaves a real, non-blank submitError`.
3. `confirm never sets createdAppointmentId`.
4. `confirm with an incomplete selection returns false without touching the repository`.
5. `reset clears a previous submitError`.

Uses hand-written fakes for `CustomerRepository`/`ServiceRepository`/`SpecialistRepository`/`AppointmentRepository` (Manager's own in-memory-domain interfaces, distinct from the backend-facing ones other TEAM2 tests fake) — no mocking library, same convention as every prior TEAM2 test file.

## VALIDATION

| Check | Result |
|---|---|
| Compile Customer (`compileCustomerDebugKotlin`, `assembleCustomerDebug`) | ✅ |
| Compile Manager (`compileManagerDebugKotlin`, `assembleManagerDebug`) | ✅ |
| Relevant unit tests (`testCustomerDebugUnitTest`, `testManagerDebugUnitTest`) | ✅ 5/5 new tests pass on both flavors (54 tests total per flavor, up from 49). Same 2 pre-existing `BackendAuthFlowVerificationTest` failures remain (documented as needing a live backend, unrelated, unchanged). |
| `lintCustomerDebug` + `lintManagerDebug` | ⚠️ Same single pre-existing error on each flavor every prior TEAM2 task found (`ViewModelConstructorInComposable` in `AuthScreenScreenshotTest.kt:70`, untouched, out of scope), same 94/98 pre-existing warnings. Confirmed zero findings in any of the 4 files this task touched. |
| `git diff --check` | ✅ clean (no whitespace errors, no conflict markers) |

Backend not modified — no backend build/test run performed.

**Not pushed**, per instruction.

## REMAINING RISKS / RECOMMENDED NEXT STEPS

- **The actual fix for "Manager can create real bookings" is backend
  work**, not Android work: `POST /api/v1/bookings` needs an
  authorization-aware way for a salon-owner/staff caller to specify the
  customer (a new `customerId` field on `CreateBookingRequest`, gated to
  callers who own the target salon), **and** a scoped customer lookup/
  search endpoint (e.g. "customers who have booked with my salon" or a
  phone-number lookup restricted to existing accounts) for the Manager
  app to select one against. Both are real product/security design
  decisions (should a manager be able to create an account for a
  brand-new customer who has never used the app? look up any phone
  number? only past customers of their own salon?) outside this
  follow-up's Android-only scope.
- **The wizard is now an honest dead end**, not a working feature — a
  manager can walk all 7 screens and will always be told at the last
  step that it doesn't work yet. This is strictly better than the
  previous silent-failure regression, but it is not a complete feature.
  Recommend either hiding "نوبت جدید" from the Dashboard's Quick Actions
  until the backend contract lands, or keeping it visible with the
  current honest block — a product decision, not made unilaterally here
  since the task said not to redesign UI.
- **Service/specialist/date-time selection remain on in-memory data**
  (unchanged from TEAM2-002) — deliberately not rewired now (see
  Architecture Impact); worth doing together with the backend work above
  once it exists, so the whole wizard becomes real in one pass rather
  than partially real twice.
- **`AppointmentRepository`/`InMemoryAppointmentRepository` still exist**
  and are still read (not written) by `availableTimes()` — this method's
  own correctness (checking a frozen, never-updated seed list for
  conflicts) was already questionable before this change and is
  unaffected by it either way; not addressed here as it's a
  pre-existing, separate concern from booking creation.
