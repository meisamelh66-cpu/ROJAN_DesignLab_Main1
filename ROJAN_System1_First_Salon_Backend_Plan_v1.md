# ROJAN SalonMembership / RBAC Backend Implementation Plan v1

**Status:** Planning artifact only. No backend or Android code was written, modified, or proposed as a diff. This document is written for **System 1** review and approval before any implementation begins, per the System 1 / System 2 boundary in `CLAUDE.md` (RBAC/security changes and backend changes both require explicit confirmation).
**Date:** 2026-08-15
**Basis:** `ROJAN_Reception_Backend_Dependency_Checklist_v1.md` and `ROJAN_Reception_Implementation_Plan_v1.md` (System 2 / `ROJAN_DesignLab`) for the client-side contract already committed to; cross-checked in this pass against **actual current backend source** at `C:\AndroidProjects\ROJAN_Backend` — both `origin/main` (`8fe9df2`, a lean production snapshot) and the materially more advanced `origin/feature/auth-rate-limit-finalization` (`28e9842`), which is where this work would land. Every file path and code pattern below is taken from that source, not assumed.
**Not covered here:** the Reception client-side Android work — already built and verified, per the System 2 reports referenced above. This plan is backend-only.

---

## 1. Purpose & Scope

The Android client (Reception + Manager flavors) already has a complete, compiling RBAC *data contract* — role/permission types, an identity-context repository, and a membership API client — built against a shape the backend does not yet implement. This plan defines what System 1 needs to build so the backend catches up to that already-committed contract: `SalonMembership` persistence, the `/users/me/salon-access` aggregation endpoint, broadened authorization on existing salon-scoped endpoints, and the invite flow whose `SecurityConfig` matcher is already reserved but unimplemented.

**In scope:** domain model, application use cases, persistence, API surface, authorization changes, and the RBAC decisions blocking all of the above.
**Out of scope:** Android client changes (already done), the salon-onboarding/self-service-salon-creation gap and other findings from `ROJAN_First_Salon_Readiness_Audit.md` (separate concern, referenced only where directly coupled).

---

## 2. Current State (verified against backend source)

- **`UserRole`** (`domain/src/main/kotlin/ai/rojan/backend/domain/user/User.kt`): `CUSTOMER`, `MANAGER`, `SPECIALIST` only. **No `RECEPTIONIST` value exists** — confirms the Android-side checklist's "decision needed" item is still open.
- **No `SalonMembership`/`Membership`/`Invite` domain type, repository, controller, or persistence exists anywhere** on either branch inspected (`origin/main` or `origin/feature/auth-rate-limit-finalization`) — confirmed by an exhaustive filename search of both trees.
- **`SecurityConfig`** (`infrastructure/.../security/SecurityConfig.kt`, on `feature/auth-rate-limit-finalization` only — `main` doesn't have this yet either) already has one reserved matcher: `.requestMatchers(HttpMethod.GET, "/api/v1/invites/*").permitAll()`, with a code comment explicitly distinguishing it from `/api/v1/invites/{token}/accept` (which stays authenticated, `anyRequest().authenticated()` catches it). This is a real, deliberate reservation, not dead config — it's evidence the invite-based flow (vs. direct-assign-by-userId) was already the intended direction, at least provisionally.
- **Authorization pattern, verified consistent across every salon-scoped controller inspected** (`SalonBookingController.list`/`.createForCustomer` shown in full; same shape confirmed by the checklist for `BookingController`, `CustomerController`, `WorkingHoursController`, `SpecialistScheduleController`, `SalonController`/`ServiceCategoryController`/`ServiceController`/`SpecialistController`):
  ```kotlin
  val salon = salonRepository.findById(SalonId(salonId)) ?: throw SalonNotFoundException(...)
  if (salon.ownerId != callerId) throw SalonAccessDeniedException(salon.id.value.toString())
  ```
  Simple, easily-audited, and uniform — which is also exactly why broadening it everywhere is the highest-risk single change in this plan (§9-10).
- **`Salon` aggregate** (`domain/.../salon/Salon.kt`): `ownerId: UserId` is a single value, no membership/collaborator concept at the domain level at all today.
- **Migrations** run through `V7__add_booking_salon_start_time_index.sql` on the feature branch. **The next migration is `V8`.**
- **Existing precedent this plan reuses rather than reinvents:** `CustomerNotLinkedToAccountException` (409, "the assignee must already have a ROJAN account" — the exact constraint the Android client's own doc comments already assume for membership assignment); `GlobalExceptionHandler`'s `@RestControllerAdvice` + `errorCodeFor` `when`-branch pattern; `*UseCaseConfig.kt` (`@Configuration` classes wiring framework-free use cases as `@Bean`s, e.g. `CustomerUseCaseConfig.kt`); `ReceptionBookingFlowIntegrationTest.kt` (`bootstrap/src/test/.../*FlowIntegrationTest.kt`, embedded Postgres via `zonky`, real HTTP via `TestRestTemplate`, already covering the exact test shape §9 below calls for — happy path, access-denied, cross-salon tenant isolation, unauthenticated, OpenAPI doc coverage — for the sibling reception-booking feature).

---

## 3. Requirements Summary

Restated from `ROJAN_Reception_Backend_Dependency_Checklist_v1.md` (that document has the full, itemized detail — this section only summarizes what's load-bearing for the sections below):

- New endpoints: `GET /api/v1/users/me/salon-access`, `GET /api/v1/salons/{salonId}/members`, `PUT /api/v1/salons/{salonId}/members/{userId}`, `DELETE /api/v1/salons/{salonId}/members/{userId}`.
- Broadened authorization (owner-only → owner-or-permitted-member) on the controllers listed in §2.
- Exact DTO shapes and the `Permission` string vocabulary the Android client already ships and expects verbatim (`MANAGE_SALON`, `MANAGE_MEMBERSHIP`, `MANAGE_CATALOG`, `MANAGE_STAFF`, `MANAGE_SCHEDULE_ALL`, `MANAGE_SCHEDULE_OWN`, `VIEW_CRM`, `MANAGE_CRM`, `MANAGE_BOOKINGS`, `MANAGE_OWN_BOOKINGS`).
- **Unresolved RBAC decisions that block domain-layer work** (full list in the checklist §3; the two most load-bearing, restated because §5 depends on them): (a) global `UserRole.RECEPTIONIST` vs. membership-scoped-only `MANAGER` accounts, and (b) invite-by-phone vs. direct-assign-by-existing-userId — for which §2 above found real, if provisional, evidence the backend already leans invite-based.

---

## 4. Recommended Implementation Order

1. **Resolve the two blocking RBAC decisions** (§3) with System 1 sign-off — nothing in §5 can be built correctly without them, and building the domain model twice is expensive.
2. **Domain layer** (§5): `Permission`, `SalonMembership` aggregate, `SalonMembershipRepository` port.
3. **Persistence** (§7): `V8` migration, JPA entity, Spring Data repository, repository adapter.
4. **Application layer** (§6): use cases for assign/revoke/query membership, and the shared salon-access-check helper every broadened controller will call.
5. **API layer** (§8): new `SalonMembershipController`, `/users/me/salon-access` addition to `UserController`, new DTOs, `GlobalExceptionHandler` additions.
6. **Broaden authorization on existing controllers one at a time**, each behind its own test coverage (§9) before moving to the next — not as one large sweeping change, given §9-10's risk framing.
7. **Invite flow** (only after the direct-assign path in steps 2-6 is stable) — implement `POST /api/v1/invites/{token}/accept` and whatever issuing-side endpoint System 1 decides on, reusing the same `SalonMembership` write path underneath rather than a parallel one.
8. **Security review pass** (§10) before merge — treat this as a required gate, not a nice-to-have, given the "regression risk on broadened authorization" finding already flagged in the Android-side checklist.

---

## 5. Domain Layer Impact

New files, `domain/src/main/kotlin/ai/rojan/backend/domain/`:

- **`membership/Permission.kt`** — enum matching the client vocabulary exactly (§3): `MANAGE_SALON`, `MANAGE_MEMBERSHIP`, `MANAGE_CATALOG`, `MANAGE_STAFF`, `MANAGE_SCHEDULE_ALL`, `MANAGE_SCHEDULE_OWN`, `VIEW_CRM`, `MANAGE_CRM`, `MANAGE_BOOKINGS`, `MANAGE_OWN_BOOKINGS`. Client fails safe on an unrecognized value (grants nothing), so this enum's string names are effectively a frozen public contract once shipped — changing a name later is a breaking client change, not a refactor.
- **`membership/SalonMembership.kt`** — aggregate root, same construction discipline as `Salon`/`User` (private constructor, `create`/`reconstitute` companion factories only): `id: MembershipId`, `salonId: SalonId`, `userId: UserId`, `role: SalonRole`, `permissions: Set<Permission>` (only if §3's "per-row independently settable" decision is confirmed — otherwise `permissions` is derived from `role` alone and doesn't need to be stored), `active: Boolean`, `createdAt`/`updatedAt`. Behavior methods: `revoke()` (mirrors `User.deactivate()`'s soft-delete pattern — sets `active = false`, never a hard delete, matching the existing convention for `Salon`/`Service`/`Specialist`).
- **`membership/SalonRole.kt`** — enum, `MANAGER`, `RECEPTIONIST` (pending §3 decision (a); if the "membership-only, no new global role" direction is chosen instead, this enum still needs to exist as the *membership's* role even though `UserRole` itself doesn't grow a new value).
- **`membership/SalonMembershipRepository.kt`** — port interface: `findById`, `findBySalonIdAndUserId`, `findAllBySalonId`, `findAllByUserId` (backs the `/salon-access` aggregation), `save`.
- **`common/MembershipDomainExceptions.kt`** (new file, following the existing one-exception-per-class-per-file grouping convention seen in `SalonDomainExceptions.kt`/`CustomerDomainExceptions.kt`): `MembershipNotFoundException`, `DuplicateMembershipException` (unique `(salonId, userId)` violation), `MembershipAccessDeniedException` (mirrors `SalonAccessDeniedException`'s shape for the assign/revoke endpoints' own authorization, distinct from the general salon-access check below so the two failure modes stay independently testable).
- **Shared salon-access-check helper** — not a domain type by itself, but its *contract* is a domain-layer decision: `fun hasSalonAccess(salon: Salon, callerId: UserId, membership: SalonMembership?, required: Permission): Boolean` (or equivalent), so every controller in §8/§4-step-6 calls one function instead of six independently-hand-rolled `OR` conditions. Where this function *lives* (domain vs. application) is an open implementation choice — either is defensible; recommend colocating it with `SalonMembershipRepository` in `domain/membership/` since it's a pure business rule with no framework dependency, consistent with how `TimeSlotEngine.kt` already keeps pure booking-availability logic in `domain/booking/` rather than the API layer.

**No change needed to `UserRole`** unless §3 decision (a) resolves toward a new global role — flagged as conditional in the original checklist and unchanged here; do not add `RECEPTIONIST` to `UserRole` speculatively.

---

## 6. Application Layer Impact

New files, `application/src/main/kotlin/ai/rojan/backend/application/membership/`, following the existing `Create*UseCase`/`Update*UseCase`/`Deactivate*UseCase` naming and single-responsibility shape already used throughout `application/salon/` and `application/customer/`:

- **`AssignMembershipUseCase.kt`** — owner-only (enforced inside the use case, not just the controller, matching how `UpdateCustomerUseCase` etc. already re-check salon ownership rather than trusting the controller layer alone). Looks up the target user by whatever identifier the invite/direct-assign decision (§3) settles on; throws `CustomerNotFoundException`-equivalent-for-users (likely reuses `UserNotFoundException`, already exists) if the target account doesn't exist yet — directly mirroring the existing `CustomerNotLinkedToAccountException` precedent's "the assignee must already have an account" constraint, unless the invite flow supersedes this for its own path.
- **`RevokeMembershipUseCase.kt`** — owner-only; calls `SalonMembership.revoke()`, never a hard delete.
- **`GetSalonAccessUseCase.kt`** — backs `/users/me/salon-access`; aggregates three existing repositories (owned salons via `SalonRepository.findAllByOwnerId` if it exists or an equivalent query, memberships via the new `SalonMembershipRepository.findAllByUserId`, and specialist links via the existing `SpecialistRepository`) into the client's already-fixed `SalonAccessResponse` shape (§3) — this is a read-side aggregation use case, not a new write path.
- **`ListSalonMembersUseCase.kt`** — owner-only, backs `GET /api/v1/salons/{salonId}/members`.
- **Invite-side use cases** (only once §4-step-7 is reached): `CreateInviteUseCase`/`AcceptInviteUseCase` or equivalent — deliberately not designed in detail here since they depend on §3 decision (b) and on whatever token/expiry model System 1 chooses; flagged as a follow-up design task once the direct-assign path is proven, not blocking this plan's initial scope.
- **DI wiring**: new `MembershipUseCaseConfig.kt` in `api/src/main/kotlin/ai/rojan/backend/api/config/`, mirroring `CustomerUseCaseConfig.kt`/`SalonUseCaseConfig.kt` exactly (`@Configuration` class, one `@Bean` function per use case, constructor-injecting the repositories each needs).

---

## 7. Infrastructure Layer Impact

- **New migration:** `infrastructure/src/main/resources/db/migration/V8__salon_membership_schema.sql` — new `salon_memberships` table: `id UUID PK`, `salon_id UUID FK → salons`, `user_id UUID FK → users`, `role VARCHAR`, `active BOOLEAN DEFAULT true`, `created_at`/`updated_at`, **unique constraint on `(salon_id, user_id)`** (per checklist §4), plus a `permissions` column only if §3 decision confirms per-row settable permissions (otherwise omit — derive from `role` in code, avoid storing a derivable value). Indexes: on `user_id` (backs `/salon-access` lookups by caller) and `salon_id` (backs the members-list endpoint) — matching the existing precedent of `V7` adding a targeted composite index for a specific query pattern rather than indexing everything speculatively.
- **New JPA entity:** `infrastructure/.../persistence/membership/SalonMembershipJpaEntity.kt`, following the existing `SalonJpaEntity.kt`/`CustomerJpaEntity.kt` shape (plain `@Entity` mapping, no business logic).
- **New Spring Data repository:** `SalonMembershipSpringDataRepository.kt` (interface extending `JpaRepository`, query-derived methods for `findBySalonIdAndUserId`/`findAllBySalonId`/`findAllByUserId`).
- **New repository adapter:** `SalonMembershipRepositoryAdapter.kt` implementing the domain `SalonMembershipRepository` port — the standard hexagonal boundary already used for every other aggregate (`SalonRepositoryAdapter.kt`, `CustomerRepositoryAdapter.kt` are the direct precedents to mirror).
- **`SecurityConfig` change:** only the accept-side matcher needs anything, and it already has it implicitly — `POST /api/v1/invites/{token}/accept` is already covered by the existing `.anyRequest().authenticated()` fallthrough (confirmed: the code comment explicitly says the GET-only matcher is scoped to exclude `/accept`, meaning `/accept` is already authenticated by default, no new matcher needed there). No `SecurityConfig` change is required for the direct-assign membership endpoints either — `/api/v1/salons/**` already falls under `anyRequest().authenticated()`; authorization is enforced in-controller (§8), not at the security-filter level, consistent with every other salon-scoped endpoint today.
- **No infrastructure change needed for CORS/rate-limiting config** for the new endpoints beyond what's flagged in §10 (confirm `/salon-access` sits behind existing general rate limits, since it's called unconditionally on every login/cold-start).

---

## 8. API Planning

New/changed files, `api/src/main/kotlin/ai/rojan/backend/api/`:

- **`membership/SalonMembershipController.kt`** (new) — `@RestController`, `@RequestMapping("/api/v1/salons/{salonId}/members")`: `GET` (list, owner-only), `PUT /{userId}` (assign, owner-only, body `AssignMembershipRequestDto`), `DELETE /{userId}` (revoke, owner-only). Follows the exact `CurrentUserResolver` + ownerId-check + Swagger `@Operation`/`@ApiResponses` pattern shown in `SalonBookingController` (§2) — same shape, new resource.
- **`membership/MembershipDtos.kt`** (new) — `AssignMembershipRequestDto { role: SalonRole }`, `SalonMembershipResponseDto { id, salonId, userId, role, createdAt, updatedAt }` — field names and casing must match the Android client's `SalonMembershipDtos.kt` exactly (§3); this is the one place a naming mismatch fails silently at deserialization rather than at compile time on either side, so a contract test (§9) matters more here than typical DTO coverage.
- **`user/UserController.kt`** (existing, modified) — add `GET /users/me/salon-access`, backed by `GetSalonAccessUseCase` (§6). This is the endpoint every login/cold-start restore calls unconditionally on the Android side — treat its latency/availability as load-bearing for basic app usability, not just a nice-to-have feature.
- **`user/SalonAccessDtos.kt`** (new, in the `user` package alongside `UserController`) — `SalonAccessResponseDto { ownedSalons, memberships, specialistLinks }` matching the client's exact shape from §3.
- **Broadened controllers** (§4 step 6, one at a time): `SalonBookingController`, `BookingController`, `CustomerController`, `WorkingHoursController`, `SpecialistScheduleController`. Each gets its existing `if (salon.ownerId != callerId) throw SalonAccessDeniedException(...)` line replaced with a call to the shared salon-access-check (§5), parameterized with the specific `Permission` that endpoint requires (e.g. `SalonBookingController.createForCustomer` → `MANAGE_BOOKINGS`; `CustomerController` write methods → `MANAGE_CRM`). **`SalonController`/`ServiceCategoryController`/`ServiceController`/`SpecialistController` write operations are explicitly out of scope for this broadening pass** — the existing Reception implementation plan already scopes Reception to booking operations only and excludes staff/catalog/settings; confirm this stays excluded rather than assuming broadened access by default (checklist §1.2's own flagged open item).
- **`GlobalExceptionHandler.kt`** (existing, modified) — add the new exception types from §5 to the `handleNotFound`/`handleAccessDenied`/`handleConflict` `@ExceptionHandler` groups and their `errorCodeFor` `when` branches, exactly matching how `CustomerNotLinkedToAccountException` was added as precedent (one import, one addition to an existing handler group, one `when` branch — not a new handler method).

---

## 9. Test Strategy

**Unit tests** (`domain/src/test/`, `application/src/test/`, following existing per-module test placement):
- `SalonMembership` aggregate: creation invariants, `revoke()` idempotency (calling twice doesn't error, matching `Salon`'s deactivate precedent).
- Shared salon-access-check helper: every combination of (owner / active member with permission / active member without permission / inactive member / non-member) × (permission required / not required) — this is the single highest-value unit test surface in the whole plan, since every broadened controller depends on this one function being correct.
- Each new use case (`AssignMembershipUseCase`, `RevokeMembershipUseCase`, `GetSalonAccessUseCase`, `ListSalonMembersUseCase`): happy path, not-found, duplicate-membership, non-owner-caller rejection — mirroring the existing test shape in `application/customer/CreateCustomerUseCaseTest.kt`/`UpdateCustomerUseCaseTest.kt`.

**Integration tests** (`bootstrap/src/test/kotlin/ai/rojan/backend/bootstrap/`, new file: `SalonMembershipFlowIntegrationTest.kt`, directly modeled on the existing `ReceptionBookingFlowIntegrationTest.kt` — same embedded-Postgres-via-`zonky` + real-HTTP-via-`TestRestTemplate` shape, same coverage checklist that file already established for the sibling feature):
- Happy path: owner assigns a membership, the assignee's `/salon-access` reflects it, assignee can now call a permission-gated endpoint.
- Duplicate assignment (already-a-member) → 409.
- Non-owner attempts to assign/revoke → 403 (**authorization tests**, see below).
- Cross-salon isolation: a membership at salon A grants nothing at salon B (**tenant isolation tests**, see below).
- Unauthenticated → 401.
- Revoked membership's *next* request is rejected — the specific "no caching/staleness window" guarantee the checklist's §5 flags as needing a dedicated test rather than an architectural inference.
- OpenAPI doc coverage for every new endpoint (matching the existing convention of asserting `@Operation`/`@ApiResponses` presence, not just runtime behavior).

**Authorization tests** (folded into the integration suite above, called out separately since this is the highest-risk category per §10): for **every** endpoint broadened in §8 step "Broadened controllers," not just the new membership endpoints — each needs its own "still rejects a non-member," "still rejects a member without the specific required permission," and "still 401s unauthenticated" case, exactly as the original checklist's §5 already demands (*"every broadened endpoint needs equivalent... coverage, not just a happy-path test for the new membership case"*). Do not treat one well-tested endpoint as proof the pattern is safe everywhere it's applied — each application site is its own regression surface.

**Tenant isolation tests**: reuse the existing precedent already established in `ReceptionBookingFlowIntegrationTest` (cross-salon rejection returns 404, not 403 or a leaked resource — the OWASP API1 convention already applied elsewhere in this API, per the checklist's own note). Apply the identical assertion shape to every new/broadened membership-aware endpoint: a membership or resource belonging to a different salon must be indistinguishable from "doesn't exist."

---

## 10. Security Considerations

- **RBAC safety / least privilege:** `RECEPTIONIST` (or whatever the membership role ends up being, per §3 decision (a)) must not implicitly carry `MANAGE_STAFF`/`MANAGE_SALON`/`MANAGE_CATALOG`/`MANAGE_MEMBERSHIP` — only what §3 explicitly decides. The shared salon-access-check helper (§5/§9) should **fail closed** on any undecided/unmapped permission, not fail open — this is a direct requirement on that function's implementation, not just a policy statement.
- **Privilege escalation prevention:** only `MANAGE_MEMBERSHIP` holders (today: owner only) may call assign/revoke. Explicitly test that a member cannot escalate their own or another membership's role, including via a crafted `PUT .../members/{ownUserId}` call — the endpoint's authorization check must use the *caller's* permission to modify, never the *target* membership's current role, as the gate.
- **Tenant isolation:** every new/broadened endpoint must preserve the existing cross-tenant-resource-returns-404 convention (§9's tenant isolation tests are the enforcement mechanism for this, not just documentation of intent).
- **Invite security** (once §4 step 7 is reached): the reserved `GET /api/v1/invites/{token}` matcher is intentionally unauthenticated (confirmation-screen lookup) — the token itself must therefore be the only secret (sufficiently long/random, single-use or short-TTL, not a guessable sequential ID), since anyone with the token can view whatever that GET returns. `POST /api/v1/invites/{token}/accept` is already authenticated by the `anyRequest()` fallthrough (§7) — confirm the accept flow binds the invite to the *authenticated caller's* identity, not a userId supplied in the request body, so an authenticated attacker can't accept an invite meant for someone else's phone number by guessing/observing a token.
- **Authentication boundaries:** no client-side role or permission check is ever the actual security boundary — this repo's own established convention (per the Android-side checklist, already correctly followed everywhere else in the client) — server responses are the only source of truth. This plan's job is to make sure the server side actually enforces what the client already assumes it does; it does not change that principle.
- **No enumeration signal:** `/salon-access` and the membership endpoints must not leak whether a given phone number/email has an account at all, consistent with the existing OTP-request design's identical-response-regardless precedent.
- **Audit logging:** membership grants/revocations, and any booking/CRM action performed by a non-owner member "on the business's behalf," should extend the same structured audit-logging shape already applied to OTP request/verify attempts (actor id, action, target, outcome) — this is a new class of "acting for the business, not as its owner" action and should be traceable as such from day one, not retrofitted later.
- **Rate limiting:** confirm `/salon-access` (called unconditionally on every login/cold-start) sits behind whatever general request-rate protections apply — it is not currently covered by the OTP-specific limiters and needs its own confirmation, not an assumption that "some limiter somewhere" covers it.
- **Strict enum validation:** `AssignMembershipRequestDto.role` must reject unrecognized values with 400 (already the default behavior for Kotlin enum deserialization failures via the existing `HttpMessageNotReadableException` → `MALFORMED_REQUEST` handler in `GlobalExceptionHandler` — confirm this, don't assume it, since a silent-default-to-null-then-NPE failure mode would be worse than a clean 400).
- **Regression risk on broadened authorization** is the single highest-risk item in this entire plan (restated from the checklist because it governs §4's sequencing decision): every endpoint being broadened currently has a trivially-correct, easily-audited owner-only check. A bug in the shared access-check helper doesn't just fail a feature — it over-grants access to real salon/customer/booking data across every endpoint that adopts it. This is the reason §4 recommends broadening controllers one at a time with full test coverage each, not as one sweeping change.

---

## 11. Rollback Considerations

- **Migrations are additive and reversible in spirit, not just in mechanism:** `V8` only adds a new table (`salon_memberships`); it doesn't alter or drop any existing column/table. A rollback that simply stops calling the new code paths leaves the schema harmless and inert — no data-loss risk from reverting application code while leaving `V8` applied.
- **Soft-revoke, not hard-delete**, on `SalonMembership` (§5) means a mistaken revoke is trivially correctable (re-assign) without any data recovery — consistent with the existing deactivate-not-delete convention already used for `Salon`/`Service`/`Specialist`.
- **Per-controller broadening (§4 step 6) is independently revertible.** Because each controller's authorization change is a small, isolated diff (replace one `if` check with a call to the shared helper), any single controller can be reverted to its original owner-only check without touching the others or the new membership infrastructure underneath — this is a direct benefit of the "one at a time" sequencing recommended in §4/§10, not just a testing convenience.
- **The shared access-check helper is the one piece where a rollback isn't "revert one file"** — if a defect is found in it *after* multiple controllers have adopted it, the safe rollback is reverting all adopting controllers back to their owner-only checks simultaneously (or feature-flagging the helper's broadened branch off, falling back to owner-only), not attempting a partial fix under pressure. Worth deciding in advance whether a feature flag around "membership-based access is active" is worth building given this — recommended, low-cost given the helper is already a single call site per controller.
- **Invite flow (§4 step 7) is the easiest full rollback**: if issues surface post-launch, the accept endpoint can be disabled (return 503/feature-off) without affecting the direct-assign path at all, since it's designed to reuse the same underlying `SalonMembership` write path rather than a parallel one (§6) — the invite *mechanism* is separable from the membership *data model* it writes to.
- **Android client compatibility during rollback:** the client already treats an unrecognized/absent permission as "grants nothing" (fail-safe by construction, per §10) and an empty `salon-access` response as a legitimate state (a user with no memberships). Rolling back the backend to pre-membership behavior does not require a matching Android rollback — the client degrades gracefully to "no salon access" rather than crashing, though Reception/non-owner Manager use would simply stop working again until re-enabled, which is the expected, intentional rollback outcome.

---

*This plan is a point-in-time planning artifact. No source code, configuration, or database schema — in either `ROJAN_DesignLab` or `ROJAN_Backend` — was modified in producing it. All file paths and code patterns cited were read directly from `ROJAN_Backend`'s current source (`origin/main` and `origin/feature/auth-rate-limit-finalization`) as of this audit; nothing here was inferred solely from the Android client's contract. Implementation must not begin until System 1 has reviewed and approved this plan, per `CLAUDE.md`'s System 1/System 2 boundary.*

---

**STOP CONDITION MET — planning artifact generated. No implementation performed. Waiting for approval from System 1.**
