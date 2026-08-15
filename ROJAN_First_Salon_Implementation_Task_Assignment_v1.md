# ROJAN First Salon Implementation Task Assignment v1.0

**Role:** Team 2 — Architecture Validation & Implementation Planning
**Status:** Planning only. No source files modified, no migrations created, no APIs changed, nothing committed, pushed, merged, or deployed in producing this document.
**Date:** 2026-08-15
**Authority / Baseline:** This document is a task-assignment restructuring of three already-approved planning artifacts — `ROJAN_First_Salon_Implementation_Roadmap_v1.md`, `ROJAN_Salon_Identity_Architecture_Report_v1.md`, `ROJAN_QA_Remediation_Plan_v1.md` (all committed at `23e4629`). It does not re-derive new findings — it re-packages the already-approved roadmap into an assignable, per-task ownership map with explicit acceptance criteria, so each item can be picked up and tracked independently.

---

# 1. Project Objective

**Goal:** Complete one real, end-to-end salon workflow — the first genuine pilot salon operating in ROJAN AI, not a demo or partial slice of the product.

```
Owner
  ↓
Salon Identity
  ↓
Reception
  ↓
Specialist
  ↓
Customer
  ↓
Booking
```

Concretely: an owner logs in, creates and completes a salon's identity (name, location with coordinates, logo, cover, service catalog), invites and onboards reception staff under real RBAC, has specialists represented with basic identity, and a real customer discovers that salon, books a real appointment, and reception can see and manage that booking — all against the production backend, with zero mock/demo data standing in for any part of that chain.

Everything in this document exists to get that one workflow real, verified, and end-to-end — not to add unrelated features. Anything outside this workflow (full gallery, reviews/ratings/certifications, advanced specialist self-service, AI layers) is explicitly out of scope here, per the approved roadmap's deferred list.

---

# 2. Ownership Model

## System 1 — Backend Ownership

| Responsibility | Scope for this pilot |
|---|---|
| **Domain models** | `Salon` field extension (logoUrl, coverImageUrl, latitude, longitude); `SalonMembership` aggregate; `SalonRole`/`Permission` enums; `SalonInvite` aggregate. All new types follow the existing private-constructor + `create`/`reconstitute` factory discipline already used by `Salon`/`User`. |
| **Database schema** | Migration adding logo/cover/lat/long columns to `salons`; new `salon_memberships` table (unique `(salon_id, user_id)`, indexed on both FKs); new `salon_invites` table (or equivalent, per the invite design). All additive — no existing column altered or dropped. |
| **Backend APIs** | Extend existing `POST/PUT /api/v1/salons` request/response DTOs with the new identity fields; new logo/cover upload (or signed-URL) endpoint; new `SalonMembershipController` (list/assign/revoke); new `GET /users/me/salon-access`; new `InviteController` (issue/preview/accept). |
| **Authentication dependencies** | OTP auto-registration fix — `VerifyOtpUseCase` currently hardcodes `UserRole.CUSTOMER` for any new phone number, which must not misclassify an invited staff member's first login. |
| **RBAC implementation** | `SalonPermissionResolver` implementing the fixed `MANAGER`/`RECEPTIONIST` permission table already decided in `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md §1c`. |
| **Permission enforcement** | Broadened authorization (owner-only → owner-or-permitted-member) on `SalonBookingController`, `BookingController`, `CustomerController`, `WorkingHoursController`, `SpecialistScheduleController` — one controller at a time, each behind its own full test coverage, never as one sweeping change. |
| **Tenant isolation** | Every new/broadened endpoint must preserve the existing cross-salon-returns-404 convention already verified elsewhere in the codebase (`SpecialistController.findSpecialistOrThrow` is the confirmed-correct reference pattern). |
| **Media architecture foundation** | The `salon_media` polymorphic design (role enum: LOGO/COVER/GALLERY/PORTFOLIO_BEFORE/PORTFOLIO_AFTER/SERVICE) as a design artifact only for this pilot — the pilot's actual Logo/Cover columns should be built as a strict subset of that eventual shape, not a shape that later needs migrating. |
| **Production backend readiness** | Staging/production deployment (`STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL` currently unset); confirmation that salon/tenant-scoped authorization is actually enforced server-side today (cannot be verified from the Android client alone). |

## System 2 — Android Ownership

| Responsibility | Scope for this pilot |
|---|---|
| **Compose UI** | Manager Settings screens (salon create/edit including lat/long, logo/cover upload, working-hours editor); Manager Membership screens (member list, invite-issue); Reception invite-accept screen wiring. All through the existing design system (`RojanTokens`, `GlassSurface`, `RojanDimens` — no parallel visual system). |
| **Navigation** | Wire the new screens into `ManagerNavGraph`/`ReceptionNavGraph`; extend `ManagerDestinations`/`ReceptionDestinations` as needed — additive routes only. |
| **Client integration** | Consume the new `logoUrl`/`coverImageUrl`/`latitude`/`longitude` fields through the already-existing `RojanRemoteImage` rendering seam; correct the two misleading code comments in `SalonRepository.kt`/`SalonLocation` (identified in the architecture report §5.1) once the real backend fields exist. |
| **Repository integration** | Replace the superseded direct-assign-by-userId `SalonMembershipApi`/`SalonMembershipRepository`/`SalonMembershipDtos.kt` with an invite-flow client matching the real backend contract; wire the already-committed `ManagerInviteRepository`/`ReceptionInviteRepository` placeholder interfaces to real endpoints. |
| **Authentication flows** | No new authentication architecture — Phase 2's invite-accept flow reuses the existing phone+OTP session flow; only the post-login identity-resolution path (`activeSalonState`) gains a new membership-aware branch. |
| **Role-based screens** | Manager Settings/Membership screens gated to owner (and, once RBAC lands, `MANAGE_SALON`/`MANAGE_MEMBERSHIP` permission holders); Reception screens gated to `SalonMembership.role = RECEPTIONIST` once membership exists — server-enforced, client-side gating is convenience only (per §7). |
| **Error handling** | Every new repository call routes through the existing `SafeApiCall`/`ErrorMessages` pattern — no new error-handling mechanism introduced. |
| **Android testing** | Unit tests for every new ViewModel/repository built in Phases 1-2, plus closing the pre-existing P0 gap (token refresh, booking lifecycle) that this roadmap's work directly exercises. |

---

# 3. Execution Order

## Phase 0 — Foundation Validation

- **Objective:** Close every open decision/confirmation that would otherwise block Phase 1-2 mid-stream. No feature code in this phase.
- **Owner:** System 1 (confirmations), System 1↔System 2 (joint items), Business/Ops (one item).
- **Dependencies:** None — this is the entry point.
- **Deliverables:**
  1. Fresh, current confirmation that the `RECEPTION_GATE_ROLE = "MANAGER"` design and `ROJAN_System1_Backend_Decision_v2.md`'s RBAC decisions are still intended (not stale).
  2. Confirmation that salon-scoped/tenant authorization is actually enforced server-side today.
  3. Object-storage decision for logo/cover upload (multipart vs. signed-URL).
  4. Decision on pilot-salon provisioning method (manual ops vs. waiting for Phase 1's create-UI).
  5. Explicit go/no-go that Customer booking = authenticated Path A only (QR/public entry stays out of scope).
  6. Joint OTP-auto-registration fix approach agreed, ready to land alongside Phase 2.
- **Acceptance criteria:** All six items above have a written, dated answer from the responsible party — no item may be silently assumed. Phase 1 does not start until items 1 and 3 are answered; Phase 2 does not start until items 1 and 6 are answered.

## Phase 1 — Salon Identity

- **Objective:** A salon can be created and fully identified — name, description, contact, structured location, logo, cover, service catalog — through the Android Manager app, against real backend endpoints.
- **Owner:** System 1 (schema/API/storage), System 2 (screens/client integration).
- **Dependencies:** Phase 0 items 3, 4.
- **Deliverables:**
  1. `Salon.logoUrl`, `Salon.coverImageUrl`, `Salon.latitude`, `Salon.longitude` — migration + domain + DTO.
  2. Logo/cover upload endpoint.
  3. `salon_media` architecture design document (design only).
  4. Manager Settings: salon create/edit screen (incl. lat/long fields), logo/cover upload UI.
  5. Manager Settings: working-hours edit screens (zero backend dependency — buildable immediately).
  6. Customer-side rendering of `coverImageUrl` (net new) and lat/long consumption; comment corrections.
- **Acceptance criteria:**
  - `POST/PUT /api/v1/salons` returns and accepts all four new fields, verified via a real request/response, not just code inspection.
  - A logo and cover image, uploaded through the Manager app, render correctly on `SalonDetailsScreen`/`SalonListScreen` for a real salon.
  - Working-hours edits made in the Manager app persist and are reflected on `GET /working-hours` on a subsequent read.
  - The two misleading Android comments (architecture report §5.1) are corrected to match verified reality.
  - `assembleManagerDevDebug` and `assembleCustomerDevDebug` both succeed; RQG screenshot verification performed on the new screens.

## Phase 2 — Reception Operation

- **Objective:** A reception staff member can be invited, accept the invite, authenticate, and access that salon's bookings under real, server-enforced permissions.
- **Owner:** System 1 (membership/permission/invite backend, authorization broadening), System 2 (membership screens, invite-accept wiring, DTO replacement).
- **Dependencies:** Phase 0 items 1, 6.
- **Deliverables:**
  1. `SalonMembership` domain + persistence.
  2. `GET /users/me/salon-access`.
  3. `SalonPermissionResolver`.
  4. `SalonInvite` domain + `InviteController` (issue + preview + accept).
  5. Broadened authorization, one controller at a time with full test coverage each: `SalonBookingController` → `BookingController` → `CustomerController` → `WorkingHoursController` → `SpecialistScheduleController`.
  6. OTP auto-registration fix, landed alongside item 4.
  7. Manager Membership screens (member list, invite-issue).
  8. Reception invite-accept screen wiring.
  9. Superseded `SalonMembershipDtos.kt`/API/repository replaced with the real invite-flow client.
  10. Re-run of the existing Reception acceptance verification against the now-real backend.
- **Acceptance criteria:**
  - An owner can issue an invite from the Manager app; the invited phone number, on first OTP login, lands as a `RECEPTIONIST` member of the correct salon — not a `CUSTOMER` account.
  - A logged-in receptionist can view and manage that salon's bookings; a receptionist without a valid, active membership for a given salon receives a 403/404 on every attempt, verified per endpoint, not assumed from one happy-path test.
  - A membership at salon A grants zero access at salon B (tenant isolation, tested explicitly).
  - A revoked membership's *next* request is rejected — no caching/staleness window.
  - Every broadened controller has its own "non-member rejected," "member without required permission rejected," and "unauthenticated rejected" test — not just a happy-path test for the new membership case.

## Phase 3 — Specialist Basic Flow

- **Objective:** Verify specialist basic identity (display name, bio, photo) continues to function correctly under the new membership/authorization model — this phase is a regression checkpoint, not a build phase, unless a real gap is found.
- **Owner:** System 2 (verification), System 1 (scope confirmation).
- **Dependencies:** Phase 2 item 5 (authorization broadening) — specifically whether specialist CRUD is included in that broadening pass.
- **Deliverables:**
  1. Confirmation of whether `SpecialistController` write operations are included in Phase 2's broadening pass (the RBAC plan explicitly excluded them from the initial pass) — resolve this against whether Reception/non-owner Manager staff are expected to manage specialist records for this pilot.
  2. Regression pass on `ManagerStaffScreen.kt`/`ManagerStaffEditScreen.kt` → `BackendSpecialistRepository` against the Phase 2 backend changes.
- **Acceptance criteria:**
  - Specialist create/edit/list continues to function for the owner account with no regression.
  - The scope question in deliverable 1 has an explicit, written answer — not resolved by silence.
  - If deliverable 1 resolves toward Reception/staff needing specialist-management access, that becomes a tracked addition to Phase 2 rather than a silently-missed gap.

## Phase 4 — Customer Booking Journey

- **Objective:** Verify the already-functional authenticated browse-then-book path continues to work correctly and now displays real salon identity (logo/cover/location) once Phase 1 ships — this phase is a regression checkpoint, not a build phase.
- **Owner:** System 2 (verification).
- **Dependencies:** Phase 1 (logo/cover/lat-long fields).
- **Deliverables:**
  1. Regression verification of the full booking flow (date → time → confirmation → success) against the DTO changes introduced in Phases 1-2.
  2. Visual verification that a real salon's logo/cover/location render correctly across `SalonListScreen`, `SearchScreen`, `SalonDetailsScreen`.
- **Acceptance criteria:**
  - A real booking can be created, confirmed, and appears correctly in both the customer's and (once Phase 2 lands) reception's views.
  - No regression in booking success rate or error handling introduced by the additive DTO fields.
  - Salon identity (name/logo/cover/address) renders correctly with no fallback-icon path triggered for a salon that has real media.

## Phase 5 — Pilot Readiness Verification

- **Objective:** Confirm the entire Owner→Salon→Reception→Specialist→Customer→Booking chain works end-to-end against a real (or realistic staging) environment, with adequate test coverage on the highest-risk paths.
- **Owner:** System 2 (test coverage, RQG), System 1 (deployment), Ops (signing).
- **Dependencies:** Phases 1-4 complete.
- **Deliverables:**
  1. P0 test coverage: token refresh (`TokenRepositoryImpl.kt`, `AuthInterceptor.kt`), booking lifecycle (`BookingRepositoryImpl.kt` + the Customer booking-flow ViewModel set).
  2. RQG pass on every new screen from Phases 1-2: `assembleDebug`, design-token compliance, RTL layout, install + screenshot on device/emulator.
  3. Confirmed staging/production backend deployment.
  4. Release signing keystore provisioned.
  5. Full end-to-end walkthrough executed and recorded: Owner login → salon identity completion → reception invite/accept → specialist record verification → customer discovery/booking → reception views the booking.
- **Acceptance criteria:**
  - The Phase 5 item 5 walkthrough completes with zero manual workarounds, zero mock data, and zero client-side-only authorization decisions standing in for a real server check.
  - Token refresh and booking lifecycle have real, passing unit test coverage (currently zero).
  - The app is installed and screenshot-verified on a real device or emulator, not claimed without evidence.
  - A named person or role has signed off that the pilot salon can go live.

---

# 4. Dependency Mapping

| Task | Required backend capability | Required API contract | Required data model | Blocking dependency | Responsible team |
|---|---|---|---|---|---|
| Salon logo/cover fields | Extend `Salon` write/read path | `POST/PUT /api/v1/salons` (existing, extended) | `salons.logo_url`, `salons.cover_image_url` (new columns) | Phase 0 item 3 (storage decision) | S1 |
| Logo/cover upload | New upload capability | New upload/signed-URL endpoint | Writes to the columns above | Phase 0 item 3 | S1 |
| Salon lat/long fields | Extend `Salon` write/read path | `POST/PUT /api/v1/salons` (existing, extended) | `salons.latitude`, `salons.longitude` (new columns) | None — independently buildable | S1 |
| `salon_media` design doc | N/A — design artifact | N/A | Design of future `salon_media` table | Should be informed by the two rows above | S1 |
| Manager Settings — salon create/edit UI | Consumes the two rows above | `POST/PUT /api/v1/salons` | Android `Salon` domain extension | Salon logo/cover/lat-long fields (backend) | S2 |
| Manager Settings — logo/cover upload UI | Consumes upload endpoint | Upload endpoint | N/A | Logo/cover upload (backend) | S2 |
| Manager Settings — working-hours edit UI | Already exists | `PUT/DELETE /salons/{salonId}/working-hours/{day}` (already working) | None new | **None** | S2 |
| `SalonMembership` persistence | New membership data layer | N/A (internal) | New `salon_memberships` table | Phase 0 item 1 | S1 |
| `GET /users/me/salon-access` | Aggregation over memberships/ownership/specialist links | New endpoint | Reads `salon_memberships` | `SalonMembership` persistence | S1 |
| `SalonPermissionResolver` | Permission-by-role mapping | N/A (internal) | Reads `SalonMembership.role` | `SalonMembership` persistence | S1 |
| `SalonInvite`/`InviteController` | Invite issue/preview/accept | New endpoints (`POST /salons/{id}/invites`, `GET /invites/{token}`, `POST /invites/{token}/accept`) | New `SalonInvite`/`salon_invites` model | `SalonMembership` persistence, Phase 0 item 1 | S1 |
| Broadened authorization | Shared salon-access-check helper | Modifies existing endpoint behavior (no new routes) | Reads `SalonMembership`, `SalonPermissionResolver` | `SalonMembership`, `SalonPermissionResolver` — sequenced one controller at a time | S1 |
| OTP auto-registration fix | Modify `VerifyOtpUseCase` default-role logic | N/A | None new | Should land with `SalonInvite`/`InviteController` | S1↔S2 |
| Manager Membership screens | Consumes salon-access + invite endpoints | `GET /users/me/salon-access`, `POST /salons/{id}/invites` | Android `SalonMember`/invite domain types (placeholders exist) | `GET /users/me/salon-access`, `SalonInvite`/`InviteController` | S2 |
| Reception invite-accept wiring | Consumes invite endpoints | `GET /invites/{token}`, `POST /invites/{token}/accept` | Already-committed `ReceptionInviteRepository` placeholder | `SalonInvite`/`InviteController` | S2 |
| Replace superseded membership DTOs | Consumes real invite/membership contract | Real membership/invite endpoints | Removes `SalonMembershipDtos.kt`'s direct-assign shape | `SalonInvite`/`InviteController` | S2 |
| Specialist CRUD scope confirmation | Confirm whether `SpecialistController` writes are broadened | `POST/PUT /api/v1/salons/{salonId}/specialists` (already exists, owner-only today) | None new | Broadened authorization's defined scope | S1 (decision), S2 (verification) |
| Customer booking regression check | None — already functional | Already-existing booking engine endpoints | Already exists | Phase 1 fields shipping (verification trigger only) | S2 |
| P0 test coverage | N/A | N/A | N/A | None — independently schedulable, should not wait on Phases 1-4 | S2 |
| Staging/production deployment | Deployed, reachable backend environment | N/A (infra) | N/A | None technically, but gates Phase 5's real-environment walkthrough | S1 |

---

# 5. Acceptance Criteria

Global, cross-phase gates — every phase's deliverables must satisfy all of the following before being marked complete, in addition to each phase's own specific criteria in §3:

- **Backend API verified.** Every new/changed endpoint has been exercised with a real request against a running backend (integration test or manual verification) — not just confirmed to compile or match a DTO shape on paper.
- **RBAC verified.** Every permission-gated action has been tested both for the permitted case (succeeds) and the denied case (rejected) — a single happy-path test is not sufficient.
- **Tenant isolation tested.** Every salon-scoped resource returns 404 (not 403, not the resource) when requested under a different salon's ID than the one it belongs to.
- **Android flow connected to production backend.** No screen built in this roadmap ships wired to a mock, demo, or in-memory repository as its real data source — `DemoIdentityProvider`/`DemoSessionProvider`-style scaffolding stays disconnected, never wired into a release path.
- **No mock data.** No fabricated rating, review, gallery, or identity data is ever rendered — consistent with the existing, already-established discipline in `SalonDetailsScreen.kt`/`TopSpecialists.kt` (architecture report §2.4). If a field has no real backend data yet, the section is omitted or gated behind an explicit "Coming Soon" state, never faked.
- **No temporary storage replacing backend.** No new local-only persistence (Room, DataStore, SharedPreferences) is introduced as a substitute for a backend capability that should exist server-side — local storage remains scoped to genuine client-only concerns (session tokens, UI state), never business data.
- **No client-side authorization bypass.** Every role/permission check rendered in the UI is confirmed to also be enforced server-side; a client-side check that isn't backed by an equivalent server check is treated as a defect, not a convenience.

---

# 6. Testing Requirements

### Backend tests (System 1)

- **Unit tests:** `SalonMembership` aggregate invariants and `revoke()` idempotency; the shared salon-access-check helper across every combination of (owner / active member with permission / active member without permission / inactive member / non-member) × (permission required / not required) — the single highest-value unit test surface in Phase 2; each new use case's happy-path/not-found/duplicate/non-owner-rejection cases.
- **Integration tests:** New `SalonMembershipFlowIntegrationTest` (modeled on the existing `ReceptionBookingFlowIntegrationTest` — embedded Postgres, real HTTP): happy-path assign/invite/accept, duplicate assignment → 409, non-owner assign/revoke → 403, unauthenticated → 401, revoked membership's next request rejected, OpenAPI doc coverage for every new endpoint.
- **Authorization tests:** For **every** endpoint touched by Phase 2's broadening — not just the new membership endpoints — its own "rejects non-member," "rejects member without the specific required permission," and "401s unauthenticated" case.
- **Tenant isolation tests:** Cross-salon rejection returns 404, not 403 or a leaked resource, applied identically to every new/broadened membership-aware endpoint.

### Android tests (System 2)

- **ViewModel tests:** Every new ViewModel from Phases 1-2 (Manager Settings, Manager Membership, Reception invite-accept) — happy path, error path, loading state — matching the existing coverage shape already established for `ManagerAuthViewModel`/`ReceptionAuthViewModel`.
- **Repository tests:** Every new/changed repository implementation (Salon identity fields, membership, invite) — currently zero coverage exists on any `Backend*Repository` in Manager/Reception, and this roadmap adds more repositories to that same gap unless explicitly tested.
- **UI flow tests:** Interaction-driven Compose tests (`performClick`-style) for the salon create/edit flow and the invite-issue/accept flow — this category has zero existing coverage anywhere in the app today; start here rather than deferring again.
- **Authentication tests:** Regression coverage confirming the OTP auto-registration fix correctly resolves an invited phone number to the right membership rather than a default `CUSTOMER` account, and that the existing `activeSalonState` resolution chain (already fixed for the stuck-at-Loading bug in an earlier phase) handles the new membership-aware branch correctly.

### End-to-end

```
Owner → Salon → Reception → Specialist → Customer → Booking
```

One recorded, reproducible walkthrough (Phase 5 deliverable 5) exercising every stage of this chain against a real or realistic staging backend, performed after Phases 1-4 are individually verified — this is the actual pilot-readiness gate, not a formality performed once other checks are assumed passing.

---

# 7. Architecture Rules

Confirmed, carried forward from `CLAUDE.md` and the approved baseline documents — not renegotiated by this task assignment:

- **Clean Architecture preserved.** `domain/`, `manager/domain/`, `reception/domain/` remain free of `android.*`/`androidx.*` imports for every new type introduced by this roadmap (`SalonMember`, invite domain types, extended `Salon`). Verified today at 77 files with zero violations — this roadmap's new files must not become the first exception.
- **Backend is Single Source of Truth.** No Android-side fabrication of identity, RBAC, or availability data. Every field rendered in a new screen traces to a real backend response — the same discipline already demonstrated in `SalonDetailsScreen.kt`'s removal of fabricated rating/review/gallery data must extend to every new field this roadmap adds.
- **RBAC remains backend authority.** Client-side role/permission checks (Manager Settings/Membership screen gating, Reception access gates) are UX convenience only. The server's 401/403/404 responses are the actual security boundary in every case — no new client-side check introduced by this roadmap is treated as sufficient on its own.
- **Tenant isolation preserved.** Every new endpoint and every new Android call site respects salon-scoped isolation; a resource from one salon must never be reachable, visible, or editable through another salon's context, enforced server-side and verified per §6's tenant isolation tests.
- **No duplicate business logic.** RBAC/permission logic exists exactly once — server-side in `SalonPermissionResolver` — never re-implemented or approximated on the Android client. Identity-shaped domain types (`ActiveSalonContext`, `AvailableSalon`) remain shared, unforked, between Manager and Reception, consistent with the existing pattern.
- **No security decisions on client side.** Invite-token validation, membership-role assignment, and permission grants are decided exclusively by the backend. The Android client's role is to call the correct endpoint and render the correct response — never to locally decide whether an action is allowed beyond optimistic UI state that a server response can always override.

---

# 8. Implementation Approval Gate

**Status:**

**"Waiting for System 1 approval before implementation."**

---

*This document is a point-in-time planning artifact. No source code, configuration, database schema, or git history was modified in producing it. It does not authorize or begin any implementation, migration, API change, commit, push, merge, or deployment.*

---

**STOP CONDITION MET — task assignment document created. No implementation performed. Waiting for approval.**
