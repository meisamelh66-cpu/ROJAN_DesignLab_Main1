# TEAM2 — Manager Booking Creation Integrity, Follow-up 2 (Result)

Closes the two structural gaps confirmed in `TEAM2_RESULT_MANAGER_BOOKING_CREATION.md`:
no way to book on behalf of another customer, and no customer-lookup endpoint at all.
Both repos changed; **two separate local commits** (one per repo — they are independent
git repositories, so "one commit" is interpreted as one commit per repo). **Neither
branch pushed.**

- Backend repo: `C:\ROJAN\ROJAN_Backend`, branch `fix/team2-002-manager-booking-creation-integrity`
- Android repo: `C:\ROJAN\ROJAN_DesignLab_Main1`, branch `fix/team2-002-manager-data-persistence` (continues the prior commit `f6d18db` on the same branch, not pushed)

## 1. API / DTO changes

### New: `GET /api/v1/salons/{salonId}/customers?query=`
`api/src/main/kotlin/ai/rojan/backend/api/salon/SalonCustomerController.kt` (new file).

- Returns `List<UserResponse>` (existing DTO, no new response model).
- `query` is optional; when present, matches a case-insensitive substring of the
  customer's `fullName` or `email`.
- The candidate set is **derived from booking history for that salon**, not a
  global user search and not a separate "salon roster" entity — there is no
  such entity in the domain, and the task explicitly said not to invent one.
  A new repository query, `BookingRepository.findDistinctCustomerIdsBySalonId`,
  returns the distinct `customerId`s of that salon's bookings; the controller
  resolves each to a `User` and filters/sorts in memory.
- Only ever returns users with a real booking history at that salon — someone
  who has never booked there does not appear, by construction.

### Changed: `POST /api/v1/bookings`
`api/src/main/kotlin/ai/rojan/backend/api/booking/BookingDtos.kt`,
`api/src/main/kotlin/ai/rojan/backend/api/booking/BookingController.kt`.

- `CreateBookingRequest` gained one new optional field: `customerId: UUID? = null`.
  (`notes` also had to gain an explicit `= null` default — it previously had
  none — purely so the new trailing field could be added as a named optional
  parameter without breaking existing 5-arg call sites; this is a
  source-compatible, behavior-neutral change.)
- When `customerId` is omitted, behavior is byte-for-byte unchanged: the
  caller books for themselves. This is what the existing customer
  self-booking flow already does and continues to do (see acceptance #8 / test 4 below).
- When `customerId` is present, `BookingController.resolveBookingCustomerId`
  resolves it, subject to the authorization rules in §2, and the booking is
  created for that resolved customer — the idempotency fingerprint is
  computed from the **resolved** customer id, not the caller id, so two
  different managers (or a manager and the real customer) booking the same
  slot for the same customer collide correctly.
- No new endpoint, no new booking table/column — `Booking.customerId` already
  existed and already meant "who this booking is for"; this only changes who
  is allowed to set it away from `callerId`.

## 2. Authorization rules

Both new/changed surfaces are enforced **server-side**, unconditionally (not
just hidden client-side):

- `SalonCustomerController.search`: the caller must be the **owner** of the
  path's `salonId` (`salon.ownerId != callerId` → `403` via the existing
  `SalonAccessDeniedException` → existing `GlobalExceptionHandler` mapping).
  Unknown `salonId` → `404` (`SalonNotFoundException`), same as every other
  salon-scoped endpoint in this API.
- `BookingController.resolveBookingCustomerId`: a `customerId` in the request
  body is only honored if the caller is the **owner of the salon in the same
  request** (`request.salonId`). Any other caller supplying a `customerId`
  gets `403`, even if the id refers to a real customer. The resolved id must
  additionally exist and have `role == CUSTOMER`, or `404`
  (`UserNotFoundException`) — this stops a manager from silently creating a
  booking against a non-existent or non-customer account.
- No changes to authentication, role model, JWT/token handling, or the
  existing RBAC filter chain — explicitly out of scope and not touched.

## 3. Persistence behavior

Unchanged at the storage layer: booking persistence still goes through the
existing `BookingRepositoryAdapter.reserve()` path, which still uses the
existing Postgres advisory-lock slot-conflict guard and still returns a real,
DB-generated `bookingId`. The only change is **which `customerId` value** is
written into that row — resolved before the transactional reserve call, so a
concurrent conflict on the same slot is still detected exactly as before
regardless of who the booking is for.

The one new read path, `findDistinctCustomerIdsBySalonId`, is a plain
`SELECT DISTINCT customerId FROM bookings WHERE salon_id = :salonId` — no
new table, no new migration.

## 4. Android changes

All changes are in `C:\ROJAN\ROJAN_DesignLab_Main1`.

**New files**
- `domain/repository/SalonCustomerRepository.kt` — `SalonCustomer(id, email, fullName)` + `searchCustomers(salonId, query)`.
- `data/remote/SalonCustomerApi.kt` — Retrofit client for the new endpoint.
- `data/repository/SalonCustomerRepositoryImpl.kt` — implements the interface via `safeApiCall`, reusing the existing `UserResponseDto`.

**Wizard rewrite (`manager/presentation/booking/`, `manager/screens/booking/`)**
- `ManagerBookingViewModel` — full rewrite. Now constructed only from real
  backend-facing repositories (`SalonRepository`, `SalonCustomerRepository`,
  `ServiceCategoryRepository`, `ServiceRepository`, `SpecialistRepository`,
  `AvailabilityRepository`, `BookingRepository`); the old
  `manager.data.ManagerRepositories` in-memory singleton no longer appears
  anywhere in this flow. Exposes `catalogState` (real services/specialists
  for the manager's own salon), `customerSearchState` (real, salon-scoped
  customers via the new endpoint), `slotsState` (real available slots — the
  same `available-slots` endpoint the customer flow already uses).
  `confirm(onSuccess)` calls the real `POST /api/v1/bookings` with the
  selected real `customerId`, `serviceId`, `specialistId`, and slot;
  `onSuccess` fires **only** after that call genuinely succeeds — this is
  the same "no fake success" contract from TEAM2-001, now finally reachable
  here because the backend contract for it exists.
- `ManagerBookingViewModelFactory` / `ManagerNavGraph.managerBookingViewModelFor` — updated to wire the 7 real repositories from `BackendApiContainerHolder`.
- `ManagerBookingCustomerScreen` — real, debounced-by-recomposition search against `customerSearchState`, shows real `fullName`/`email`.
- `ManagerBookingServiceScreen` / `ManagerBookingSpecialistScreen` — list real services/specialists from `catalogState` instead of the old sample lists. Disclosed simplifications (data the real backend models don't have): no category-name grouping (categories are a separate resource), no specialist skill-based filtering (real `Specialist` carries no skills field) — every specialist at the salon is offered; the backend itself still rejects an invalid specialist/service pairing at submit time.
- `ManagerBookingDateTimeScreen` — real rolling 7-day dates (`RollingBookingDates`, same utility TEAM2-004 introduced) and real available slots from `slotsState`, replacing the old fixed grid computed against in-memory bookings.
- `ManagerBookingReviewScreen` — confirm button now calls `viewModel.confirm(onSuccess = onConfirmed)`; a failure renders the real `submitError` via `ManagerErrorState` with a **working retry** action (previously this state represented a permanent structural block; now it's a normal transient failure).

**Contract plumbing (backward-compatible, additive-only)**
- `data/remote/dto/BookingDtos.kt`: `CreateBookingRequestDto` gained optional `customerId: String? = null`.
- `domain/repository/BookingRepository.kt` (customer-facing) / `data/repository/BookingRepositoryImpl.kt`: `createBooking(...)` gained a trailing optional `customerId: String? = null`, passed through unchanged when absent — the existing customer self-booking call sites are untouched and still compile/behave identically.

**Nothing else was touched.** No RBAC changes, no token/auth changes, no unrelated screens, no design-system changes.

## 5. Tests

**Backend** — `bootstrap/src/test/kotlin/ai/rojan/backend/bootstrap/ManagerBookingCreationIntegrationTest.kt` (new), full Spring Boot integration test against an embedded Postgres, same conventions as the existing `BookingEngineFlowIntegrationTest`:
1. A manager can search and book only for a customer who has actually booked with their salon.
2. A manager **cannot** book on behalf of a customer for a salon they do not own → `403`.
3. Booking on behalf of a `customerId` that does not exist → `404`, not a fabricated booking.
4. Existing customer self-booking (no `customerId` in the request) still works unchanged — regression guard for acceptance #8.

Plus updates to `application/src/test/kotlin/ai/rojan/backend/application/booking/BookingTestFixtures.kt` (`InMemoryBookingRepository` now implements `findDistinctCustomerIdsBySalonId`) needed for the rest of the backend test suite to keep compiling.

**Android** — `manager/presentation/booking/ManagerBookingViewModelTest.kt` fully rewritten (old tests exercised the removed in-memory API and no longer applied): 7 tests covering catalog loading (incl. zero-salons → `Empty`, not an error), salon-scoped customer search, real slot loading on date selection, `confirm` sending the real selected `customerId` and firing `onSuccess` only on genuine backend success, a backend failure never firing `onSuccess` and never fabricating a `createdAppointmentId`, and an incomplete selection never calling the repository at all.

Five other pre-existing test files needed a one-line fake-interface update (`customerId: String?,` added to their `FakeBookingRepository.createBooking` override) purely to keep compiling against the new interface signature — no behavioral change to those tests: `ManagerCalendarViewModelTest.kt`, `ManagerDashboardViewModelTest.kt`, `BookingConfirmationViewModelTest.kt`, `AppointmentsViewModelTest.kt`, `RescheduleAppointmentViewModelTest.kt`.

## 6. Validation performed

- **Backend tests**: `./gradlew test` → BUILD SUCCESSFUL, including all 4 new `ManagerBookingCreationIntegrationTest` cases and no regressions in `BookingEngineFlowIntegrationTest` / `BookingConflictConcurrencyIntegrationTest`.
- **Android tests**: full unit test suite for both flavors — all pass except the 2 pre-existing, unrelated `BackendAuthFlowVerificationTest` failures already present before this change.
- **Compile/package both**: `compileManagerDebugKotlin compileCustomerDebugKotlin --rerun-tasks` → BUILD SUCCESSFUL; `assembleCustomerDebug assembleManagerDebug` → BUILD SUCCESSFUL (both APKs package).
- **Lint**: `lintCustomerDebug lintManagerDebug --rerun-tasks` → both flavors report the same single pre-existing, unrelated error (`AuthScreenScreenshotTest.kt:70`, `ViewModelConstructorInComposable`) and the same "94 warnings" baseline seen in every prior TEAM2 task. Grep-verified **zero** findings in every file this follow-up touched or added.
- **`git diff --check`**: clean (exit 0) on both repos. (Android reports pre-existing CRLF-normalization notices on save, not whitespace errors — these are not new and not flagged by `--check`.)
- **Inspect final diff**: reviewed `git diff --stat` and full diffs on both repos before committing; confirmed every changed/new file is one of the ones listed in §1/§4 above — no unrelated files.

## 7. Remaining blockers

None for the scope of this follow-up — all 8 acceptance criteria are met:

1. ✅ Manager/Receptionist can search customers belonging to their salon only (booking-history-derived, owner-enforced).
2. ✅ Selected `customerId` is sent to backend during booking creation.
3. ✅ Backend verifies customer (exists + `CUSTOMER` role), service/specialist (existing validation, now exercised through this path), and slot (existing advisory-lock reservation) validity.
4. ✅ Booking is transactionally persisted and returns a real `bookingId` (unchanged persistence path).
5. ✅ Manager Calendar shows the persisted booking (already wired to real data since TEAM2-002; this follow-up makes booking creation feed it real rows instead of none).
6. ✅ Booking survives app restart/reload (server-persisted, not in-memory — same guarantee TEAM2-002/004 established for reads).
7. ✅ Failure is shown honestly via `submitError`; no fake success, no local/demo fallback anywhere in the wizard.
8. ✅ Existing customer self-booking flow continues working — covered by a dedicated regression test on both ends of the contract.

Two pre-existing items noted for awareness, not introduced by this change and not touched:
- The 2 pre-existing `BackendAuthFlowVerificationTest` Android test failures.
- The 1 pre-existing `ViewModelConstructorInComposable` lint error in `AuthScreenScreenshotTest.kt`.
