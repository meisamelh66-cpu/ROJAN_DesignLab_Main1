# TEAM2 RESULT — COMPLETE BOOKING API CONTRACT (TEAM2-003)

Priority: P1
Status: FIXED (contract complete for all 4 operations; UI wired for 2 of them — see Confirmed Gaps)
Repository modified: ANDROID (`ROJAN_DesignLab_Main1`)
Repository inspected, not modified: BACKEND (`ROJAN_Backend`)

---

## CONFIRMED GAPS

Re-verified from current source, not assumed from the earlier TEAM2
inspection:

**Backend** (`BookingController.kt`, `BookingUseCases.kt`,
`SecurityConfig.kt`) already fully implements all four target operations,
correctly and completely:

| Operation | Endpoint | Authorization | Confirmed via |
|---|---|---|---|
| Confirm | `PATCH /api/v1/bookings/{id}/confirm` | Salon owner only (403 otherwise); 409 if not PENDING | `ConfirmBookingUseCase`, `BookingController.confirm()` |
| Complete | `PATCH /api/v1/bookings/{id}/complete` | Salon owner only (403 otherwise) | `CompleteBookingUseCase`, `BookingController.complete()` |
| Reschedule | `PUT /api/v1/bookings/{id}/reschedule` | Customer or salon owner; 409 on a new overlap | `RescheduleBookingUseCase`, `BookingController.reschedule()`, request DTO `RescheduleBookingRequest(newStartTime: LocalDateTime)` |
| Cancel | `PATCH /api/v1/bookings/{id}/cancel` | Customer or salon owner; 409 if already CANCELLED/COMPLETED | `CancelBookingUseCase`, `BookingController.cancel()` |

No backend gap exists. **No backend file was modified** — per the task's
own instruction ("Do not add APIs if backend already provides them"),
confirmed there was nothing to add.

**Android**, before this change:

- `BookingApi`/`BookingRepository`/`BookingRepositoryImpl` implemented
  only `createBooking`, `myBookings`, `getBooking`, `cancelBooking` — no
  client for confirm, complete, or reschedule existed anywhere.
- `RescheduleAppointmentScreen.kt` existed as a UI flow, but was entirely
  local/demo: it looked its appointment up in
  `CustomerEcosystemViewModel.state.appointments` (a lookup that TEAM2-004
  made permanently unable to find any real booking, since real backend
  bookings were never added to that list) and computed available slots via
  the demo `BookingEngine`/`CatalogEngine`, not the real availability
  endpoint. Confirmed by full source read, not assumed.
- `AppointmentDetailsScreen.kt` was inspected as instructed and confirmed
  to have **no confirm/complete/reschedule/cancel action of its own** — it
  is a read-only details/invoice/review view (reached by tapping a card).
  It is therefore not in scope for "complete the booking lifecycle
  contract"; its pre-existing "appointment not found" limitation
  (disclosed in TEAM2-004) is unrelated to this task's four target
  operations and is not addressed here.
- `BookingConfirmationViewModel`/`AppointmentsViewModel` (from TEAM2-001/
  TEAM2-004) were re-checked and required no changes for this task beyond
  the mechanical fallout of `BookingRepository` gaining three new abstract
  methods (their test fakes needed the three new no-op overrides to keep
  compiling — see Files Changed).

## TARGET OPERATIONS: WHAT WAS ACTUALLY MISSING VS. WHAT WAS BUILT

| Operation | Client (API/DTO/Repository) | ViewModel + UI |
|---|---|---|
| Cancel | Already existed (TEAM2-001/004) | Already wired (`AppointmentsScreen`, TEAM2-004) |
| Reschedule | **Built this task** | **Built this task** — `RescheduleAppointmentScreen` fully rewired to real data |
| Confirm | **Built this task** | **Not wired to any screen — see below** |
| Complete | **Built this task** | **Not wired to any screen — see below** |

**Why confirm/complete have no UI wiring in this change:** both are
salon-owner-only operations per the backend's own authorization rules.
The only UI surface for salon-owner booking actions in this app is the
Manager module (`ManagerBookingViewModel`, `ManagerCalendarScreen`, etc.),
which — per TEAM2-002's still-open finding — has no backend integration
at all and is explicitly the next task, not this one (this task's own
stop condition: "Do not continue TEAM2-002"). There is no customer-facing
screen where confirming or completing a booking would ever make sense.
Building a new screen for either would be inventing UI this task
explicitly forbids ("Connect existing flows only. Do NOT redesign UI.").
So: the repository contract for confirm/complete is complete, tested, and
ready — the moment TEAM2-002 wires the Manager module to the backend, it
has a working `confirmBooking()`/`completeBooking()` to call immediately,
with no further Android networking work needed.

## FILES CHANGED

| File | Change |
|---|---|
| `app/src/main/java/ai/rojan/designlab/data/remote/dto/BookingDtos.kt` | Added `RescheduleBookingRequestDto(newStartTime: String)`, mirroring the backend's `RescheduleBookingRequest`. |
| `app/src/main/java/ai/rojan/designlab/data/remote/BookingApi.kt` | Added `confirmBooking`, `completeBooking` (`@PATCH`), `rescheduleBooking` (`@PUT` + body) Retrofit methods. |
| `app/src/main/java/ai/rojan/designlab/domain/repository/BookingRepository.kt` | Added `confirmBooking(bookingId)`, `completeBooking(bookingId)`, `rescheduleBooking(bookingId, newStartTime)` to the domain interface. |
| `app/src/main/java/ai/rojan/designlab/data/repository/BookingRepositoryImpl.kt` | Implemented the three new methods — same `safeApiCall { ... }.map { it.toDomain() }` pattern as every existing method here. |
| `app/src/main/java/ai/rojan/designlab/presentation/profile/RescheduleAppointmentViewModel.kt` (new) | Loads the real booking (`getBooking`), resolves its display names (same pattern as `BookingConfirmationViewModel`/`AppointmentsViewModel`), loads real available slots per selected date (`AvailabilityRepository` — the same endpoint `BookingTimeViewModel` uses), and submits the real reschedule. Exposes `UiState` for both the target-booking load and the slots load, plus `isSubmitting`/`submitError` for the confirm action — same shape TEAM2-001 established for booking confirmation. |
| `app/src/main/java/ai/rojan/designlab/presentation/profile/RescheduleAppointmentViewModelFactory.kt` (new) | Manual DI factory, mirrors every other one in this app. |
| `app/src/main/java/ai/rojan/designlab/screens/profile/RescheduleAppointmentScreen.kt` | Rewired to the new ViewModel. Same visual structure as before (back button, title, salon/service line, date `LazyRow`, time list, confirm button) — now rendering `Loading`/`Error`(+retry)/`Success` for the booking load and the slots load, and a `submitError`(+retry) block before the confirm button, matching `BookingConfirmationScreen`'s TEAM2-001 pattern. `onRescheduled()` fires only after the real `PUT /reschedule` call succeeds. No `ecosystemViewModel` dependency remains (dropped — nothing on this screen has a demo counterpart anymore). |
| `app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt` | Updated the `RescheduleAppointmentScreen` call site to drop the now-removed `ecosystemViewModel` argument. |
| `app/src/main/java/ai/rojan/designlab/screens/profile/AppointmentsScreen.kt` | Added a resume-triggered refresh (`DisposableEffect` + `LifecycleEventObserver` on `ON_RESUME`, calling `viewModel.load()`) — see Data Integrity below for why. |
| Two existing test files (`BookingConfirmationViewModelTest.kt`, `AppointmentsViewModelTest.kt`) | Their `FakeBookingRepository` test doubles needed three new no-op overrides (`error("not used by ...")`) to keep implementing the now-larger `BookingRepository` interface — no behavioral change to either test file. |

Three new test files were added (see Tests below).

## API MAPPING

| Backend endpoint | Retrofit method | Repository method | Request DTO | Response DTO |
|---|---|---|---|---|
| `PATCH /api/v1/bookings/{id}/confirm` | `BookingApi.confirmBooking` | `BookingRepository.confirmBooking` | — | `BookingResponseDto` |
| `PATCH /api/v1/bookings/{id}/complete` | `BookingApi.completeBooking` | `BookingRepository.completeBooking` | — | `BookingResponseDto` |
| `PUT /api/v1/bookings/{id}/reschedule` | `BookingApi.rescheduleBooking` | `BookingRepository.rescheduleBooking` | `RescheduleBookingRequestDto(newStartTime: String)` | `BookingResponseDto` |
| `PATCH /api/v1/bookings/{id}/cancel` | `BookingApi.cancelBooking` (pre-existing) | `BookingRepository.cancelBooking` (pre-existing) | — | `BookingResponseDto` |

**Error mapping**: no new code was needed here — `safeApiCall` (shared by
every repository method, including the three new ones) and
`userMessageFor` (401 → "برای این عملیات نیاز به ورود مجدد دارید.", 403 →
"اجازه دسترسی به این بخش را ندارید.", 409 → "این عملیات با وضعیت فعلی
سازگار نیست.", network → "اتصال اینترنت برقرار نیست...") already cover
every status code these three new endpoints can return. This is exactly
"do not duplicate backend logic" applied to error handling too: the
generic mapping was already correct and complete.

## ARCHITECTURE IMPACT

- **No UI redesign.** `RescheduleAppointmentScreen` keeps its exact
  original layout (back button → title → salon/service line → date row →
  time list → confirm button); only the data source and state handling
  changed, the same kind of swap TEAM2-001/004 already made elsewhere in
  this app.
- **`ecosystemViewModel` parameter removed from `RescheduleAppointmentScreen`** —
  it had no remaining use once the screen stopped reading
  `CustomerEcosystemViewModel.state.appointments`. This required one
  matching change in `RojanNavGraph.kt`'s call site (drop the argument).
- **A real, previously-undetected data-integrity gap was found and fixed
  while wiring this:** `AppointmentsScreen`'s own `AppointmentsViewModel`
  instance survives navigating to `RescheduleAppointmentScreen` and back
  (Navigation-Compose keeps a back-stack-scoped ViewModel alive as long as
  its entry isn't popped). Without an explicit refresh, a customer
  returning from a successful reschedule would see the *old* time in their
  list until some unrelated event happened to reload it — a real instance
  of "local state pretending the backend updated," exactly what this
  task's Data Integrity section rules out. Fixed with a resume-triggered
  reload in `AppointmentsScreen` (see Data Integrity).
- **Confirm/complete are contract-complete but UI-unwired**, by design —
  see Confirmed Gaps for the full reasoning. This is the one deliberate,
  disclosed scope boundary in this change.
- **`BookingRepository` gaining three new abstract methods** required a
  small, purely mechanical fix in two pre-existing test files' fakes (see
  Files Changed) — no test's actual behavior or assertions changed.

## DATA INTEGRITY

All four operations satisfy "HTTP success + valid response + updated
state refresh":

- **Reschedule**: `onRescheduled()` (which pops back to the appointments
  list) fires only from `RescheduleAppointmentViewModel.confirmReschedule`'s
  `onSuccess` — never on a missing selection, a failure, or an unconfirmed
  attempt. On return, `AppointmentsScreen`'s new `ON_RESUME` refresh
  re-fetches the real list, so the new time is what's actually displayed
  — not a locally-mutated stale value.
- **Cancel** (pre-existing, TEAM2-004): unchanged — already reloads from
  the backend after every cancel attempt regardless of outcome.
- **Confirm/complete**: the repository methods return a real
  `Result<Booking>` from the actual backend response — there is no local
  mutation path for either anywhere in this codebase to have introduced a
  fake-success risk in the first place, since nothing calls them yet.
- No branch anywhere in the new code manufactures a `Booking`/success
  state without it having come from an actual `safeApiCall`-wrapped
  network response.

## TESTS

**`app/src/test/java/ai/rojan/designlab/data/repository/BookingRepositoryImplTest.kt`** (new, 7 tests) — repository/DTO-mapping level, covering all four operations uniformly:

1. `confirmBooking success maps the response into a domain Booking` — **Confirm success**.
2. `confirmBooking failure (409 - booking not pending) becomes a real Result failure` — **Confirm failure**.
3. `completeBooking success maps the response into a domain Booking` — **Complete success**.
4. `rescheduleBooking success sends the new start time in the request and maps the response` — **Reschedule success** (repository level; see also the ViewModel-level test below).
5. `cancelBooking success maps the response into a domain Booking` — **Cancel success**.
6. `a 401 on any of the four operations becomes a real BackendApiException` — **401 unauthorized**.
7. `a network failure on any of the four operations becomes NetworkUnavailableException` — **Network error**.

Uses a hand-written `FakeBookingApi` (no mocking library in this project, same convention as every other TEAM2 test file) and constructs real `HttpException`s via `Response.error(...)` to exercise `safeApiCall`'s actual exception-mapping code, not a shortcut around it.

**`app/src/test/java/ai/rojan/designlab/presentation/profile/RescheduleAppointmentViewModelTest.kt`** (new, 6 tests) — the one operation with a full wired UI state machine:

1. Booking loads with real resolved display names.
2. A 401 loading the booking surfaces as `UiState.Error` — **401 unauthorized**, at the ViewModel layer this time.
3. Selecting a date loads real available slots into `Success`.
4. Confirming a reschedule calls `onSuccess` only after the real backend call succeeds, and sends the exact composed `newStartTime` — **Reschedule success**, end to end through the ViewModel.
5. A network failure sets `submitError` and never calls `onSuccess` — **Network error**, proving no fake success.
6. Confirming without a selected date/time fails without ever calling the repository.

Total new tests this task: **13**.

## VALIDATION

**Android:**

| Check | Result |
|---|---|
| Compile Customer (`assembleCustomerDebug`) | ✅ `BUILD SUCCESSFUL` |
| Compile Manager (`assembleManagerDebug`) | ✅ `BUILD SUCCESSFUL` |
| `testCustomerDebugUnitTest` + `testManagerDebugUnitTest` | ✅ 13/13 new tests pass on both flavors (33 tests total per flavor, up from 20 after TEAM2-004). Same 2 pre-existing `BackendAuthFlowVerificationTest` failures remain (documented as needing a live backend, not run here) — unrelated, unchanged. |
| `lintCustomerDebug` + `lintManagerDebug` | ⚠️ Same single pre-existing error on each flavor as TEAM2-001/004 found (`ViewModelConstructorInComposable` in `AuthScreenScreenshotTest.kt:70`, untouched, out of scope), same 94/98 pre-existing warnings. Confirmed by grepping every lint report: zero findings in any of the 8 files this task touched. |

**Backend:** Not modified — no backend build/test run needed per the task's own conditional instruction.

## REMAINING RISKS

- **Confirm/complete are unreachable from any UI today** (by design — see
  Confirmed Gaps). This is real, tested, ready capability sitting idle
  until TEAM2-002 wires the Manager module to the backend. Flagging
  explicitly so it isn't mistaken for "done and shipped to users" — it's
  "done and shipped to the codebase."
- **`AppointmentDetailsScreen`'s pre-existing "appointment not found"
  limitation (from TEAM2-004) is still open.** Confirmed again this task
  that it has no confirm/complete/reschedule/cancel action of its own, so
  it was correctly out of scope for "complete the booking lifecycle
  contract" — but it remains a real, disclosed gap for a future task to
  pick up (migrating its review/invoice/rebook flow to real backend data,
  which TEAM2-004's report already flagged as larger scope than a booking-
  lifecycle task).
- **The reschedule screen no longer auto-skips to the first day with real
  availability** the way the original demo version did (it now defaults
  to "today" only) — a disclosed simplification to keep this change
  scoped to the reschedule *contract*, not a re-implementation of
  `BookingDateViewModel`'s auto-skip refinement. The date row is still
  fully usable; the customer just taps a different day if today is full.
- **No instrumented/Compose UI test** was added for
  `RescheduleAppointmentScreen`'s rendering — coverage is at the
  ViewModel level (where confirm/fail/retry decisions are actually made),
  consistent with TEAM2-001/004's validation depth.
- **The `ON_RESUME`-triggered refresh in `AppointmentsScreen` fires an
  extra `myBookings()` call on the screen's very first appearance**
  (once from `init{}`, once from the resume observer firing on initial
  composition) — a minor, accepted inefficiency, not a correctness issue.

---

**Stopping here per the stop condition. Not continuing to TEAM2-002 —
waiting for review.**
