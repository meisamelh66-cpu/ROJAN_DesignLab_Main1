# ROJAN Reception Phase 1 Readiness Report v1

**Scope:** Verification and mapping only, per instruction. No code written, no existing files modified — this report is the only file created in producing it.

---

## 1. Git state verification

| Check | Result |
|---|---|
| Current branch | `feature/android-reception-app` |
| Latest baseline commit | `6b30597` — "fix: enforce production API config and secure release logging" (2026-08-12), on `feature/manager-backend-integration`, 20 commits ahead of its own `origin` tracking branch, 42 ahead of `origin/main` (unchanged since Phase 0 — verified, not assumed) |
| Uncommitted changes | Still fully uncommitted, unchanged since the Phase 0 completion report: `app/build.gradle.kts` and `RojanAppPalette.kt` modified (additive); all `reception/*` sources, and the three prior `ROJAN_Reception_*.md` docs, untracked. Nothing has been committed to this branch at any point. |
| Backend baseline re-check | Re-fetched `ROJAN_Backend` — `origin/feature/auth-rate-limit-finalization` is still at `28e98421` (unchanged since the Backend Dependency Checklist). Re-confirmed: still no `InviteController`, still no `SalonMembershipController`, still no `/users/me/salon-access`. Every backend gap named in the prior two documents still holds exactly as stated — nothing to revise there. |

---

## 2. Review of completed documents

| Document | Status |
|---|---|
| `ROJAN_System2_PreImplementation_Audit_v1.md` | **Not found.** Searched this repo and the broader filesystem — this file does not exist anywhere on this machine. Flagging this explicitly rather than fabricating its contents or silently skipping it: if this document exists elsewhere (a different machine, a doc not yet saved here, or a different name), it has not been reviewed as part of this report. Everything below is grounded in the three documents that do exist, plus fresh verification against the current codebase. |
| `ROJAN_Reception_Implementation_Plan_v1.md` | Present, reviewed. Its §1 finding (client-side RBAC contract with no real backend behind it) and §6/§8 (screen list, phased order) are the basis for §4 below. |
| `ROJAN_Reception_Phase0_Completion_Report_v1.md` | Present, reviewed. Confirms Phase 0's actual delivered state (Splash/OTP/Salon-Selection/placeholder Dashboard, provisional `MANAGER`-role gate) — treated as ground truth for "what exists today" below, re-verified against the live source rather than taken on faith. |
| `ROJAN_Reception_Backend_Dependency_Checklist_v1.md` | Present, reviewed. Its endpoint/DTO/RBAC-decision findings are re-confirmed unchanged in §1 above and carried forward into §4's per-item blockers. |

---

## 3. Existing Android foundation — re-verified

| Layer | Verified state |
|---|---|
| **Compose architecture** | Clean Architecture (`domain/`, `data/`, `navigation/`, `screens/`, `ui/`) unchanged. Reception's Phase 0 additions follow the identical shape under `reception/`. |
| **Navigation** | `ReceptionDestinations`/`ReceptionRootGraph`/`ReceptionNavGraph` exist and build (Phase 0). Only 3 routes registered (`OTP_AUTH`, `SALON_SELECTION`, `DASHBOARD` placeholder) — no calendar/booking/customer routes yet, confirmed by re-reading `ReceptionNavGraph.kt`. |
| **Authentication layer** | `ReceptionAuthViewModel`/`ReceptionAuthViewModelFactory` exist, real OTP wiring (`requestOtp`/`verifyOtp`/`resendOtp`), gate still on the provisional `RECEPTION_GATE_ROLE = "MANAGER"` constant (unchanged, still flagged in its own doc comment). |
| **Networking layer** | `di/BackendApiContainer.kt` unchanged — Retrofit/OkHttp/kotlinx.serialization, one central container, `AuthInterceptor`/`TokenAuthenticator` handle refresh transparently. Reception's factory reuses it exactly as Manager's does. |
| **Repository pattern** | Two tiers confirmed: (1) `BackendApiContainer`-level singletons (`bookingRepository`, `salonMembershipRepository`, `currentUserIdentityContextRepository`, etc. — salon-agnostic, constructed once); (2) `ManagerRepositories` — a **per-active-salon** container (`AppointmentRepository`, `ServiceRepository`, `SpecialistRepository`, `CustomerRepository`) built fresh in `initialize()`. **New finding this phase, not previously documented:** `ManagerRepositories.initialize()` resolves its salon by calling `GET /api/v1/salons/mine` (owner-only — "salons owned by the caller," confirmed in `API_CONTRACT.md`) and failing with `IllegalStateException("Active salon ... not found in this account's salons")` if the persisted active salon isn't in that owned list. A receptionist's active salon comes from *membership*, not ownership, so **`ManagerRepositories` cannot be reused unmodified by Reception** even once every backend blocker in §4 below is resolved — this is a real, additional Android-side dependency the prior three documents didn't surface, because it only becomes visible when actually mapping Phase 1's data layer. |
| **Design system components** | `ReceptionScaffold` (on shared `WarmBackground`) and `ReceptionGlassSurface` (thin `PremiumGlassSurface` wrapper bound to `ReceptionPalette`) exist from Phase 0. No Reception-specific dashboard/calendar/card components exist yet — Manager's (`TodayOverviewSection`, `QuickActionsSection`, `ManagerCalendarScreen`'s day/week components) are all `ManagerPalette`/`ManagerColors`-bound and would need Reception-palette equivalents, not a straight reuse, when Phase 1 actually builds them. |

---

## 4. Reception Phase 1 mapping

For each required item: existing API, DTO, repository support, permission dependency — verified against current source, not inferred.

### 4.1 Authentication completion

| | Status |
|---|---|
| Existing API | `POST /auth/otp/request`, `/otp/verify`, `/otp/resend`, `GET /users/me` — all real, all already wired in `ReceptionAuthViewModel` (Phase 0). `GET /users/me/salon-access` — **does not exist** (re-confirmed §1). |
| DTO | Client-side request/response DTOs for OTP + `AuthResponse` already exist and are shared with Manager (`AuthApi.kt`) — no new DTO needed for the OTP leg itself. `SalonAccessResponse` shape needed for the identity-context leg is already defined client-side (`SalonAccessDtos.kt`) but has no backend counterpart to deserialize from. |
| Repository support | `BackendAuthRepository`, `TokenRepository`, `AuthSessionRepository`, `CurrentUserIdentityContextRepository` all exist and are already wired into `ReceptionAuthViewModel`. |
| Permission dependency | Blocked on the same two items as Phase 0: (1) plan §4 item 5's undecided global-role question — `RECEPTION_GATE_ROLE` stays a placeholder until answered; (2) `GET /users/me/salon-access` not existing means `refreshIdentityContext()` will fail for any account today, regardless of how the role gate is resolved. **"Completion" cannot proceed past Phase 0's current state until these land — this item is backend-blocked, not Android-blocked.** |

### 4.2 Invite acceptance flow

| | Status |
|---|---|
| Existing API | **None.** `SecurityConfig.kt` reserves a `permitAll` route pattern for `GET /api/v1/invites/{token}` and `POST /api/v1/invites/{token}/accept`, but re-confirmed (§1) that no `InviteController`, no invite domain type, and no invite persistence exist anywhere in the backend. |
| DTO | **None.** No invite-related request/response shape has been defined on either side — re-confirmed via a fresh search of the Android codebase (`grep` for "invite" across `app/src/main/java` returns only doc-comment prose already reviewed in the Backend Dependency Checklist, e.g. `SalonMembershipDtos.kt`'s note that "there is no invite-by-email flow yet" — zero actual invite types). |
| Repository support | **None.** No `InviteRepository` interface exists client-side, nothing in `BackendApiContainer`. |
| Permission dependency | Blocked on Backend Dependency Checklist §3's own open decision: "invite vs. direct-assign" was never resolved. This item cannot be scoped further, let alone built, until that decision is made and (if invites are chosen) the backend actually implements the dangling route pattern. **This is the least-ready of the four Phase 1 items — it currently has zero backend surface of any kind to build against, not just an authorization gap like the other three.** |

### 4.3 Real dashboard preparation

| | Status |
|---|---|
| Existing API | `GET /api/v1/salons/{salonId}/bookings` (salon-scoped booking list) is real but owner-only (Backend Dependency Checklist §1.2, unchanged). `GET /salons/mine` (used to resolve which salon to query today) is owner-only by definition and does not include a receptionist's membership salons (§3 finding above). |
| DTO | `BookingResponseDto`/`PagedResponseDto<BookingResponseDto>` already exist and are shared (`data/remote/dto/BookingDtos.kt`), reusable as-is once authorization permits calling them. |
| Repository support | `ManagerBookingApi.list()` already implements the call shape needed. However, per §3's finding, the salon-resolution step underneath it (`ManagerRepositories.initialize()` → `salons/mine`) is owner-scoped and would need to be adapted (or a parallel Reception repository container built) to resolve salon identity from membership instead. |
| Permission dependency | Blocked on Backend Dependency Checklist §1.2 (broadened authorization on the salon bookings list endpoint) **and** the newly-identified §3 gap (salon resolution can't come from `/salons/mine` for a non-owner). Two independent blockers, not one — resolving backend authorization alone is not sufficient to unblock this item. |

### 4.4 Booking management preparation

| | Status |
|---|---|
| Existing API | Mixed, verified precisely this phase: `POST /api/v1/salons/{salonId}/bookings` (create for customer) and `GET .../bookings` (list) are real, owner-only. `PATCH /api/v1/bookings/{bookingId}/cancel` and `PUT .../reschedule` (top-level, non-salon-scoped, "Customer or owner" per `API_CONTRACT.md`) are also real. **`PATCH /api/v1/bookings/{bookingId}/confirm` and `/complete` ("Owner only" per `API_CONTRACT.md`) exist backend-side but have zero client-side binding anywhere** — not in `BookingApi.kt` (Customer), not in `ManagerBookingApi.kt` (Manager). This is a gap neither prior document called out, since both focused on authorization rather than client-side API coverage. |
| DTO | `CreateBookingForCustomerRequestDto`, `RescheduleBookingRequestDto`, `BookingResponseDto` all exist. No DTO exists yet for confirm/complete (both are no-request-body `PATCH` calls per the contract, so the gap is purely the missing Retrofit method, not a missing type). |
| Repository support | `domain/repository/BookingRepository.kt` already implements `cancelBooking`/`rescheduleBooking` against the real top-level endpoints — its own doc comment on `rescheduleBooking` already states *"Its own customer or the salon owner/manager/receptionist may call this,"* ahead of what the backend actually enforces today (worth noting as a second instance of the client anticipating a role the backend doesn't distinguish yet, same pattern as the Implementation Plan's §1 finding). No repository method exists for confirm/complete. |
| Permission dependency | Blocked on Backend Dependency Checklist §1.2 (booking confirm/complete authorization is owner-only; cancel/reschedule's "Customer or owner" would additionally need "or salon member" per the checklist) **and** §3's undecided check-in-status question if that's still wanted. Additionally, unlike §4.1-4.3, this item has a **concrete, addressable Android-side gap independent of backend authorization**: confirm/complete need new Retrofit methods + repository methods added regardless of which way the backend RBAC decisions land, since they don't exist client-side at all today. |

---

## 5. Summary

| Phase 1 item | Backend-blocked | Android-side gap beyond Phase 0 |
|---|---|---|
| Authentication completion | Yes — `/salon-access` + role decision | None beyond what's already built |
| Invite acceptance flow | Yes — entire feature undecided/unbuilt | Entire feature unbuilt (zero DTO/API/repository) |
| Real dashboard preparation | Yes — booking-list authorization | Yes — `ManagerRepositories`' owner-only salon resolution needs adapting |
| Booking management preparation | Yes — confirm/complete/cancel/reschedule authorization | Yes — confirm/complete have no client-side binding at all yet |

Every Phase 1 item remains backend-blocked for its core authorization, consistent with the Implementation Plan and Backend Dependency Checklist. Two items (dashboard, booking management) additionally have Android-side preparatory work that does **not** depend on backend decisions and could be scoped independently if useful: adapting/duplicating the salon-resolution logic, and adding the missing confirm/complete Retrofit + repository bindings.

**No implementation performed. Waiting for approval before Phase 1 coding begins.**
