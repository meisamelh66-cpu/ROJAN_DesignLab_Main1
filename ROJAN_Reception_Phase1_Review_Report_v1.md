# ROJAN Reception Phase 1 Review Report v1

**Scope:** Review only, per instruction — no code modified in producing this report. Every finding below was re-verified directly against current source (not re-derived from the prior status report's own claims) — where the status report's claim held up, this says so; where re-checking surfaced something it missed, that's called out as new.

**Review inputs:**
- `ROJAN_System1_Backend_Decision_v2.md` (`ROJAN_Backend`) — reviewed in full.
- `ROJAN_System2_Reception_Phase1_Status_Report_v1.md` — reviewed in full.
- **`ROJAN_System1_Backend_Permission_Update_v1.0` — not found.** Searched this machine (both repos, common document locations) — no file by this name or a close variant exists anywhere. Flagging this explicitly rather than reviewing against fabricated content: everything below is checked against the two documents that do exist plus the actual current source, not against this third one. If it exists elsewhere, this review has not accounted for it.

---

## 1. Architecture compliance

**Compliant.** Clean Architecture layering held throughout the new code: `reception/domain/repository/*.kt` (pure Kotlin interfaces, zero Android/network imports), `reception/data/*.kt` (implementations), `reception/presentation/*.kt` (ViewModels), `reception/screens/*.kt` (Compose) — verified by re-reading each new file's import list, no layer violation found.

**Deliberate reuse, verified sound:** `BackendReceptionBookingRepository`/`BackendReceptionCustomerRepository` construct against the existing `ManagerBookingApi`/`ManagerCustomerApi` Retrofit interfaces rather than duplicating them. This is *not* a cross-flavor violation — re-confirmed both interfaces live under the shared `data.remote` package (not `manager.*`), so they were never flavor-restricted despite their names. Reusing them avoids two Retrofit interfaces hitting identical endpoints.

**One naming-hygiene note, not a defect:** `ManagerBookingApi`/`ManagerCustomerApi`'s names now read as misleading — a future maintainer skimming `reception/data/` will reasonably assume "Manager" means flavor-restricted and hesitate to reuse them, or worse, duplicate them unnecessarily. Not something this review fixes (renaming is a code change, out of scope), but worth flagging for whoever next touches those two files.

**`ReceptionRepositories` correctly avoids the bug flagged in `ROJAN_Reception_Phase1_Readiness_Report_v1.md` §3** — re-verified: `salonId` is a constructor parameter, never re-derived via `GET /salons/mine` (owner-only). Confirmed by reading the file directly, not assumed from the earlier report's own claim.

---

## 2. RBAC compliance

**Compliant — and compliant in the specific way that matters: no client-side RBAC logic exists at all.** No file anywhere in this phase's changes checks a permission string or role value to decide whether to *show* an action; every screen calls the real endpoint and lets the backend's `401`/`403` be the actual enforcement. This is the correct posture — RBAC decisions belong entirely to the backend per System 1's decision, and re-verified nothing added this session second-guesses that.

Specific checks:
- `RECEPTION_GATE_ROLE` constant: re-read, value unchanged (`"MANAGER"`), only its doc comment was edited (Phase 1 auth-completion work, prior to this session).
- `ReceptionCustomerRepository`'s interface surface was re-checked against the underlying `ManagerCustomerApi`: the underlying Retrofit interface exposes `create`/`update`/`notes`/`bookings` in addition to `list`/`get` — `BackendReceptionCustomerRepository` calls only `list`/`get`. Confirmed no path from any Reception screen reaches a `MANAGE_CRM`-level action, consistent with System 1 decision §1c fixing `RECEPTIONIST` at `VIEW_CRM`.
- Booking creation (`MANAGE_BOOKINGS`-scoped per §1c) is the only write action exposed, matching the decided permission exactly.

---

## 3. No mock data

**Compliant, re-verified with a fresh grep** across every file touched or added this session (not limited to `reception/` — also the four shared files: `BookingApi.kt`, `BookingDtos.kt`, `BookingRepositoryImpl.kt`, `domain/repository/BookingRepository.kt`). Zero matches for `Fake`/`Mock`/`Stub`/hardcoded sample data. Every repository call targets a real endpoint. Where a screen currently shows an error (Dashboard, Customers list, Booking wizard's customer/service-adjacent steps), that error is the real, live backend response — re-confirmed this is `403`/`404`-shaped, not a client-side simulated failure.

`ReceptionInviteRepository` remains interface-only — re-confirmed no `InviteApi`, no implementation, no DI registration exist anywhere.

---

## 4. Repository correctness

Two real findings from re-reading the implementation, neither present in the prior status report:

1. **`ReceptionBookingViewModel.loadServices()` silently drops a partial failure.** It fetches every category, then fetches that category's services in a loop, accumulating successes into one list. If category A's services load and category B's call fails, the current logic (`all.isEmpty()` is false, so the `else` branch fires) shows `UiState.Success` with only category A's services — the failure is discarded with no signal to the user. Not a crash, not fake data, but a receptionist could search for a service that silently failed to load and reasonably conclude it doesn't exist. Worth a follow-up fix (surface a partial-failure indicator, or fail the whole load) — not done here per "do not code."

2. **`BookingResponseDto.toDomain()` is duplicated**, byte-for-byte in shape, across `BookingRepositoryImpl.kt` and `BackendReceptionBookingRepository.kt` (two independent private extension functions mapping the identical DTO to the identical domain type). Both are currently correct and consistent with each other, but this is a real drift risk: the enrichment fields added this session (`service`/`specialist`/`customer`) had to be added to both copies by hand, and any future DTO change carries the same double-edit burden with no compiler enforcement that both stay in sync.

**Verified correct, not just claimed:** every property name `ReceptionRepositories.from()` reads off `BackendApiContainer` (`serviceCategoryRepository`, `serviceRepository`, `specialistRepository`, `availabilityRepository`, `bookingRepository`, `managerBookingApi`, `managerCustomerApi`) was re-checked directly against `BackendApiContainer.kt`'s actual declarations — all match exactly.

**Minor UX inconsistency, not a correctness bug:** the booking wizard's Review screen uses a custom error mapper (`bookingErrorMessage`) that gives a specific, reassuring message for a `403` ("this is a known limitation, not an app error"). Dashboard and the Customers list use the generic `userMessageFor`, whose `403` case is a plain "you don't have access to this section" — accurate, but less contextualized. Both are honest, real messages; only the tone differs between screens.

---

## 5. API integration readiness

Re-confirmed against `ROJAN_System1_Backend_Decision_v2.md` §4's dependency order:

| Backend item | Android readiness |
|---|---|
| §4.1 `SalonMembership` persistence | N/A (backend-only) |
| §4.2 `GET /users/me/salon-access` | Already called, ready — zero Android change needed on ship |
| §4.3 `active: Boolean` on `UserResponse` | Not yet consumed anywhere client-side (nothing reads it even speculatively) — smallest possible gap, additive whenever picked up |
| §4.4 `SalonPermissionResolver` | N/A directly — Android never inspects `permissions` client-side (§2 above), so this has no Android-side dependency at all beyond §4.2 already carrying the field |
| §4.5 `SalonInvite`/`InviteController` | `ReceptionInviteRepository` interface ready; zero implementation, zero UI — correct per this session's explicit scope limits |
| §4.6 Authorization broadening | Every affected repository (`ReceptionBookingRepository`, `ReceptionCustomerRepository`) already calls the real, correct endpoint shape — ships with zero Android change |
| §4.7 `BookingResponse` enrichment | DTO + domain fields ready, nullable, Dashboard already prefers them over raw ids when present — zero Android change on ship |

**One gap the status report didn't mention:** confirm/complete bindings exist in the repository layer (`BookingRepository.confirmBooking`/`completeBooking`) but are not called from any screen — re-confirmed by grep, zero call sites. This matches the status report's own §2 admission, just restated here under "readiness" rather than "pending" — the binding is ready, the UI action is not.

---

## 6. Remaining blockers

Unchanged from the status report, re-confirmed still accurate:

- No account can reach Dashboard/Customers/Booking-wizard-with-real-data today — blocked on §4.1-§4.2.
- Invite flow has zero UI and one still-open, System-1-unaddressed gap (`ROJAN_Reception_Phase1_Updated_Plan_v2.md` §3's OTP-auto-registers-as-CUSTOMER issue) — unchanged.
- Booking confirm/complete have no UI entry point yet (client-side only, no backend dependency) — unchanged from §5 above.

**New from this review, not blockers but follow-ups worth scheduling:**
- §4 finding 1 (partial-failure swallowing in `loadServices()`).
- §4 finding 2 (duplicated DTO mapping — a real drift risk, not yet a bug).
- The missing `ROJAN_System1_Backend_Permission_Update_v1.0` document — either produce it, or confirm `ROJAN_System1_Backend_Decision_v2.md` is the complete and only permission decision, so future reviews stop expecting a document that doesn't exist.

---

**No code modified, no commit, no push, in producing this report.**
