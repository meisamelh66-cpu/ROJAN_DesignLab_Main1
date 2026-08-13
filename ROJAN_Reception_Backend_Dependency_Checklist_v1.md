# ROJAN Reception Backend Dependency Checklist v1

**Basis:** `ROJAN_Reception_Phase0_Completion_Report_v1.md` §4-5, and the underlying analysis in `ROJAN_Reception_Implementation_Plan_v1.md` §1-4 (this repo). Checklist only — no implementation performed or proposed as code. Every item below was verified against actual backend source (`ROJAN_Backend`, both local `main` and `origin/feature/auth-rate-limit-finalization`), not assumed.

**Framing:** the Android client (System 1) already has a complete, coherent RBAC *data contract* built and compiling — `SalonMemberRole`, `CurrentUserIdentityContext`, `SalonPermissions`, `SalonMembershipRepository`/`Api`/DTOs. Every checklist item below exists because the backend has to catch up to a contract the client already committed to, not because the shape is undecided. Where the client's assumed shape is the de facto spec, this checklist says so explicitly — the backend should match it, not redesign it, unless a decision item below says otherwise.

---

## 1. Required endpoints

### 1.1 New endpoints

- [ ] **`GET /api/v1/users/me/salon-access`** — any authenticated user. Returns the caller's `ownedSalons`/`memberships`/`specialistLinks`, each with an active flag and a `permissions` set. Already called by `CurrentUserIdentityContextRepositoryImpl` on the client; nothing works past login without this.
- [ ] **`GET /api/v1/salons/{salonId}/members`** — owner only (mirrors every other salon-scoped list). Returns current membership rows for the salon.
- [ ] **`PUT /api/v1/salons/{salonId}/members/{userId}`** — owner only. Assigns/updates a membership role for an existing account. Client's own doc comment already states the constraint: *"the assignee must already have a ROJAN account; there is no invite-by-email flow yet."*
- [ ] **`DELETE /api/v1/salons/{salonId}/members/{userId}`** — owner only. Revokes membership.

### 1.2 Existing endpoints requiring broadened authorization

Every one of these currently enforces `salon.ownerId == callerId` and nothing else. Each needs a second accepted condition: an active salon membership carrying the relevant permission (see §3 for which permission maps to which action).

- [ ] `SalonBookingController.list` (`GET /api/v1/salons/{salonId}/bookings`)
- [ ] `SalonBookingController.createForCustomer` (`POST /api/v1/salons/{salonId}/bookings`) — its own KDoc already says *"Reception/owner only"*; the check itself doesn't match the doc yet.
- [ ] `BookingController` confirm/cancel/complete/reschedule (`PATCH/PUT /api/v1/bookings/{id}/...`) — currently "Customer or owner"; needs "... or salon member with permission" added, not replaced.
- [ ] Every `CustomerController` method (list/get/timeline/bookings/notes/tags, create/update) — all currently owner-only.
- [ ] `WorkingHoursController`, `SpecialistScheduleController` — write operations currently owner-only; read stays "any authenticated user" (unchanged).
- [ ] `SalonController`/`ServiceCategoryController`/`ServiceController`/`SpecialistController` write operations — **decision needed on whether Reception ever needs these at all** (plan §5 already scopes Reception to "booking operations only" and explicitly excludes staff/catalog/settings — confirm this stays excluded rather than assuming broadened access by default).

### 1.3 Latent, unimplemented endpoint already referenced in security config

- [ ] **`GET /api/v1/invites/{token}`** and **`POST /api/v1/invites/{token}/accept`** — `SecurityConfig.kt` (on `origin/feature/auth-rate-limit-finalization`) already has a `permitAll` matcher reserved for these paths, but **no `InviteController` or invite domain type exists anywhere in the backend**. This is either a forgotten stub for a future invite-based membership flow (which would directly answer §3's invite-vs-direct-assign question below) or genuinely dead configuration. Needs a decision either way — leaving an authorized-but-unimplemented route pattern in security config unexplained is itself a hygiene gap, not just a missing feature.

---

## 2. Required DTO contracts

The client already committed to these exact shapes (`SalonAccessDtos.kt`, `SalonMembershipDtos.kt`) — listed here as the contract the backend must match, field names included, since a mismatch fails silently/throws at deserialization rather than at compile time.

- [ ] **`SalonAccessResponse`** (backing `/users/me/salon-access`):
  ```
  { ownedSalons: OwnedSalonAccess[], memberships: MembershipAccess[], specialistLinks: SpecialistAccess[] }
  OwnedSalonAccess:  { salonId, salonName, active, permissions: string[] }
  MembershipAccess:  { membershipId, salonId, salonName, active, role, permissions: string[] }
  SpecialistAccess:  { specialistId, salonId, salonName, active, permissions: string[] }
  ```
- [ ] **`AssignMembershipRequestDto`**: `{ role: "MANAGER" | "RECEPTIONIST" }`
- [ ] **`SalonMembershipResponseDto`**: `{ id, salonId, userId, role, createdAt, updatedAt }`
- [ ] **`Permission` value vocabulary** — backend must emit exactly these string values (client's `SalonPermissions` object, already shipping): `MANAGE_SALON`, `MANAGE_MEMBERSHIP`, `MANAGE_CATALOG`, `MANAGE_STAFF`, `MANAGE_SCHEDULE_ALL`, `MANAGE_SCHEDULE_OWN`, `VIEW_CRM`, `MANAGE_CRM`, `MANAGE_BOOKINGS`, `MANAGE_OWN_BOOKINGS`. Client-side, an unrecognized value silently grants nothing ("fails safe by construction") — so a typo'd or renamed backend value doesn't break the build, it just silently revokes access. Contract tests on this exact string set are worth having.
- [ ] **`SalonRole` (backend enum)** — `MANAGER`, `RECEPTIONIST`, matching client's `NetworkSalonRole` exactly (name and casing).

---

## 3. Required RBAC decisions

Product/architecture decisions this checklist cannot resolve on its own — each blocks a concrete downstream item above.

- [ ] **Global role vs. membership-only scoping.** Does a receptionist need a new global `UserRole.RECEPTIONIST` (parallel to `CUSTOMER/MANAGER/SPECIALIST`), or do reception staff hold `MANAGER`-role accounts scoped down purely by salon-membership permissions? This decides what the Android OTP-verify gate should actually check (currently a flagged placeholder reusing the `MANAGER` check) and whether `UserRole` needs a new enum value at all.
- [ ] **Role → permission mapping ownership.** Is `RECEPTIONIST` a fixed bundle of permissions decided server-side (every receptionist gets identical access), or is `permissions` on a membership row independently settable per assignment (an owner could grant one receptionist `VIEW_CRM` but not another)? The client's `MembershipAccess.permissions` being a per-row `Set<String>` implies the latter is at least representable — confirm whether it should be.
- [ ] **CRM write access for Reception.** Does `RECEPTIONIST` include `MANAGE_CRM` (add customer notes/tags) or only `VIEW_CRM` (read-only)? Directly blocks Phase 2 of the Android plan.
- [ ] **Booking check-in modeling.** Does Reception need a new `BookingStatus.CHECKED_IN` state, or does it operate entirely within the existing `PENDING → CONFIRMED → COMPLETED`/`CANCELLED` lifecycle? A new status is a domain change, not just an authorization change — affects §4 scope.
- [ ] **Membership creation flow.** Direct owner-assign-by-existing-userId only (current client assumption, §1.1), or an invite-by-phone/email flow (§1.3's dangling `/invites/*` config)? This changes whether §1.1's `PUT .../members/{userId}` is the whole feature or one piece of a larger flow.
- [ ] **Multi-salon membership.** Confirm whether one receptionist account is expected to hold `RECEPTIONIST` membership at more than one salon simultaneously (the data model — `List<MembershipAccess>` — already allows it; confirm it's an intended case, not just an unconstrained accident).
- [ ] **Immediate revocation semantics.** When an owner revokes a membership, does access end on that account's *next request* (re-resolved from DB every time, consistent with the existing "no `salonId` claim in the JWT" design) or is there any caching/staleness window to rule out explicitly? Should be a stated guarantee, not an assumption.

---

## 4. Required backend changes

Scoped as "what," not "how" — no implementation proposed.

- [ ] **Domain layer:** `Permission` enum, a `SalonMembership` aggregate (id, salonId, userId, role, active, timestamps), `SalonMembershipRepository` port — same shape as every existing domain module (`Customer`, `Booking`, etc.).
- [ ] **Persistence:** new migration for a `salon_memberships` table — `salonId`/`userId` foreign keys, unique constraint on `(salonId, userId)`, `active` flag (soft-revoke, matching the existing deactivate-not-delete convention used for `Salon`/`Service`/`Specialist`).
- [ ] **API layer:** `SalonMembershipController` (new), `/users/me/salon-access` addition to `UserController` (aggregates owned salons + memberships + specialist links — three existing repositories, one new response shape).
- [ ] **Authorization layer:** a reusable "salon access" check (ownerId match OR active membership with required permission) that every controller in §1.2 calls, rather than each controller reimplementing the OR-condition independently — the existing `CurrentUserResolver` pattern is the established place this kind of shared check already lives.
- [ ] **Exception handling:** new exception types for membership-specific failures (e.g., membership not found, duplicate membership), mapped through the existing `GlobalExceptionHandler` pattern (one handler block + one `errorCodeFor` branch, matching how `CustomerNotLinkedToAccountException` was added).
- [ ] **`UserRole` enum change** — only if §3's first decision requires it. Not committed here; conditional.
- [ ] **OpenAPI/Swagger documentation** for every new/changed endpoint, matching the existing `@Operation`/`@ApiResponses` annotation convention already used throughout (e.g. `SalonBookingController.createForCustomer`).
- [ ] **Resolve the dangling `/invites/*` security-config entry** (§1.3) one way or the other — implement it or remove the unused `permitAll` matcher.

---

## 5. Security considerations

- [ ] **Least privilege.** `RECEPTIONIST` membership must not implicitly carry `MANAGE_STAFF`/`MANAGE_SALON`/`MANAGE_CATALOG`/`MANAGE_MEMBERSHIP` — only whatever §3 explicitly decides. Fail-closed on any undecided permission, not fail-open.
- [ ] **Privilege escalation.** Only `MANAGE_MEMBERSHIP` holders (today: owner only) may call the assign/remove endpoints. A receptionist must never be able to grant themselves or anyone else a higher-privilege role, including indirectly (e.g. re-assigning their own membership row).
- [ ] **Tenant isolation.** New/broadened endpoints must preserve the existing "cross-tenant resource → 404, never 403" convention (OWASP API1 mitigation already applied everywhere else in this API) — a membership or booking belonging to a different salon must not be distinguishable from "doesn't exist."
- [ ] **No enumeration signal.** `/salon-access` and the membership endpoints must not leak whether a given phone number/email has an account at all, consistent with the existing OTP-request design's "identical response whether or not the phone is registered" precedent.
- [ ] **Audit logging.** Reception actions (booking created/confirmed/cancelled *by someone other than the account owner*, membership grants/revocations) are a new class of "acting on the business's behalf, not as its owner" action. Structured audit logging (actor id, action, target, outcome — the same shape already applied to OTP request/verify attempts) is the relevant precedent to extend, not a new logging system.
- [ ] **Revocation takes effect immediately.** Confirm explicitly (test, not assumption) that a revoked membership blocks the very next request — the "no `salonId` claim in the JWT, everything re-resolved per-request" design should already guarantee this, but it's a security-relevant guarantee worth a dedicated test rather than an inference from architecture.
- [ ] **Rate limiting.** `/salon-access` is called on every login/cold-start restore (client-side, unconditionally) — confirm it sits behind whatever general request-rate protections apply, distinct from the OTP-specific limits which don't cover this endpoint.
- [ ] **Strict enum validation.** `AssignMembershipRequestDto.role` must reject unrecognized values with `400`, never silently coerce or default.
- [ ] **Sensitive-field redaction parity.** The existing precedent (schedule overrides/leaves/blocks redact `reason` to non-owners, added specifically to close an OWASP API3 gap) should be reviewed against whatever `VIEW_CRM` ends up exposing to Reception — decide explicitly whether any customer fields need the same treatment for reception-level viewers, rather than assuming CRM data is uniformly fine to expose.
- [ ] **Regression risk on broadened authorization.** Every endpoint in §1.2 currently has a simple, easily-audited owner-only check; broadening it is the single highest-risk change in this checklist (a bug here over-grants access to salon data, not just fails a feature). Treat it with the same integration-test rigor already established for the one comparable precedent that exists (`ReceptionBookingFlowIntegrationTest`'s ownership/cross-salon/unauthenticated coverage) — every broadened endpoint needs equivalent "still rejects a non-member," "still rejects a different salon's member," and "still 401s unauthenticated" coverage, not just a happy-path test for the new membership case.

---

**No implementation, Android or backend, was performed or proposed as part of this checklist.**
