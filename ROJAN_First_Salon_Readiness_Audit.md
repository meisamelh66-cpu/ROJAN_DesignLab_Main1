# ROJAN First Salon Readiness Audit

**Auditor:** Review-only audit — no source code, configuration, migrations, APIs, UI, or repository state were modified in producing this report. Read-only `git fetch`/`git show`/file reads only.
**Date:** 2026-08-15
**Scope:** `C:\AndroidProjects\ROJAN_DesignLab` (System 2 / Android, branch `feature/android-reception-app` @ `1a3bdb0`, clean working tree). **This pass also cross-references actual backend source at `C:\AndroidProjects\ROJAN_Backend`** (`origin/main` @ `8fe9df2`, and the materially more advanced `origin/feature/auth-rate-limit-finalization` @ `28e9842`) — read-only, no backend files modified, consistent with `CLAUDE.md`'s System 1/System 2 boundary (backend changes require confirmation; backend *reads* for audit accuracy do not).
**Framing:** *if ROJAN onboarded one real salon today, what would actually work end-to-end, what would silently not work, and what would stop the pilot cold?* Traced against the fixed journey: Owner → OTP Login → Create Salon → Complete Salon Profile → Activate Salon → Generate QR → Reception Setup → Specialist Setup → Customer Booking.

**Material correction vs. this repository's own prior same-day audits:** `ROJAN_First_Salon_Readiness_Audit.md`'s earlier revision and `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` both inferred backend capability primarily from the Android client's contracts and treated several RBAC questions as "unresolved." Direct backend source inspection in this pass finds that framing **outdated in two important ways**, both detailed in §1 and §4 below:
1. Salon creation/update/deactivation and working-hours read/write **already exist server-side** — the gap is Android-side (no Manager screen/API call), not backend-side.
2. A backend decision document, `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md` (present in that repo, untracked, not yet reflected in `ROJAN_DesignLab`'s own RBAC plan), **already resolves** the RBAC open questions the RBAC plan still lists as blocking — role model, permission mapping, invite mechanism, all decided. Nothing from it is implemented yet, but the "waiting on decisions" framing is stale; the correct framing now is "waiting on implementation of already-made decisions."

---

## 1. Salon Lifecycle

**Salon entity** — `ROJAN_Backend/domain/.../salon/Salon.kt` (aggregate root, private-constructor + `create`/`reconstitute` factory discipline), backed by `salons` table (`V2__salon_management_schema.sql`):

```
id UUID, owner_id UUID FK→users, name, description, phone, email, address,
active BOOLEAN DEFAULT TRUE, created_at, updated_at
```

**Salon creation flow:**
- **Existing (backend):** `POST /api/v1/salons` (`SalonController.create` → `CreateSalonUseCase`), fully built, owner resolved from the bearer token, `active = true` from the moment of creation. `GET /mine` lists an owner's own salons.
- **Missing (Android):** no Manager screen and no Retrofit call exists anywhere in `ROJAN_DesignLab` for `POST /api/v1/salons` — confirmed by search across `data/remote/**` and `manager/screens/**`. `SalonApi.kt` (Android) is read-only: `browseSalons`/`getSalon` only. `manager/screens/settings/` is a `.gitkeep`-only stub. **This is a client-side gap, not a backend gap** — correcting this repo's own prior assessment that a salon "must be created directly by System 1 (backend/DB), out of band."

**Owner relationship:** `Salon.ownerId: UserId`, single value, enforced via FK + `idx_salons_owner_id`. Every salon-scoped controller applies the same owner-only check (`if (salon.ownerId != callerId) throw SalonAccessDeniedException(...)`) — simple, uniform, verified across every controller inspected.

**DRAFT → ACTIVE lifecycle:** **Does not exist.** `Salon.active` is a plain boolean, `true` from `create()`, flippable only to `false` via `deactivate()` (soft-delete, one-way in the domain model — no `reactivate()`). There is no `DRAFT`/`PENDING`/multi-state status at all, at either the domain or schema level, on either backend branch inspected. A created salon is immediately "active" and immediately appears in `GET /api/v1/salons` (`findAllActive`) — there is no "complete profile, then flip a switch to go live" step to audit, because the step doesn't exist. If the journey's "Activate Salon" stage is meant to be a real gate (e.g. don't list a salon publicly until the owner finishes onboarding), **that gate needs to be designed and built** — it is not a hidden/undocumented existing feature.

**Public visibility:** `GET /api/v1/salons` (paginated, filterable by name, active-only) is real and works for the *authenticated, ID-based* browse path the Customer app already uses. **Separately, and not the same thing:** the Android client also ships a complete, unauthenticated, **slug**-based public API contract for QR-code entry (`PublicSalonApi.kt`, targeting `/api/v1/public/salons/{slug}` + `/categories` + `/specialists` + `/available-slots`, explicitly documented in its own file header as "the ROJAN backend's unauthenticated QR-code customer journey"). **No backend controller matching this path exists anywhere** — confirmed by exhaustive search on both backend branches. The only public-facing controller that exists is `PublicWebsiteController` (`GET /api/v1/public/{tenantSlug}/website`), a different URL shape entirely, returning a **hardcoded stub** (`"name" to "ROJAN AI", "description" to "AI Beauty Platform", "status" to "ACTIVE"` — literally not reading from any salon data). A CORS integration test (`CorsConfigurationIntegrationTest.kt`) references `/api/v1/public/salons/nonexistent-slug`, evidence someone expected this route to exist, but no controller was ever built behind it. **This is the single most concrete, previously-undocumented finding in this audit: the entire QR-entry customer journey is non-functional against the real backend today, on both the "missing route" and "missing `slug` field on `Salon`" axes.**

**QR generation:** **Does not exist anywhere** — zero references to QR/`zxing`/barcode generation in either repository's source (only in this repo's own audit/report `.md` files, as prose). No client-side QR image generation, no server-side QR payload endpoint, and (per above) not even a working URL for a QR code to encode yet, since the slug-based public page it would need to point to isn't implemented.

**Report:**
- **Existing:** Salon entity + CRUD backend endpoints (create/update/deactivate/get/list/mine), owner relationship + authorization, active-salon listing.
- **Missing:** Android salon-creation/edit UI and API calls (client-only gap); any DRAFT/ACTIVE lifecycle state (doesn't exist at all, at either layer); the entire slug-based public/QR salon page backend; QR code generation (client or server); a `slug` field on `Salon` itself.
- **Blocking:** the public/QR path is a hard blocker for the "Generate QR" and downstream "Customer Booking via QR" journey stages — not a partial gap, a complete absence on the backend side paired with a fully-built-but-unreachable Android client contract.

---

## 2. Salon Data Readiness

| Field | Entity/table | API endpoint(s) | Current status |
|---|---|---|---|
| `name` | `Salon.name` / `salons.name` | `POST` `/salons`, `PUT` `/salons/{id}`, `GET` `/salons`, `/salons/{id}`, `/salons/mine` | **Working** (backend). No Android write UI. |
| `description` | `Salon.description` (nullable) | same as above | **Working** (backend). No Android write UI. |
| `phone` | `Salon.phone` | same as above | **Working** (backend). No Android write UI. |
| `address` | `Salon.address` (single free-text field) | same as above | **Working** (backend, plain string — see below). No Android write UI. |
| `email` | `Salon.email` (nullable) — not in the requested list but present | same as above | **Working** (backend). No Android write UI. |
| `latitude` / `longitude` | **No field exists anywhere.** Not on `Salon`, not on `Branch`, not in any migration, not in any DTO, on either backend branch. | none | **Missing entirely.** The Customer app's advertised "nearby" salon discovery (per this repo's own earlier capability notes) has no real geo-coordinate backing anywhere in the system to support genuine distance-based search — worth verifying client-side whether "nearby" is currently a real feature or a UI label with no working geo query behind it, since the data it would need doesn't exist server-side. |
| `working hours` | Separate domain: `WorkingHours`/`WorkingHoursIntervals` (`domain/.../schedule/`), tables `working_hours` + `working_hours_intervals` (per-`salon_id`, per-`day_of_week`, multiple time intervals/day) | `GET /salons/{salonId}/working-hours`, `GET .../{dayOfWeek}`, **`PUT .../{dayOfWeek}`** (full set/edit), `DELETE .../{dayOfWeek}` — all exist, backend-complete | **Backend: working, full CRUD.** **Android: read-only** — `WorkingHoursApi`/`WorkingHoursRepositoryImpl` only call the `GET` endpoints; no Manager screen calls `PUT`/`DELETE`. Correcting the prior audit's framing: this is purely a missing Manager-side write screen over an already-complete backend contract, not a backend gap. |
| `slug` | **No field exists anywhere** — not on `Salon`, not in any migration. The only place the string "slug" appears in the backend source is a path-variable name (`tenantSlug`) in the unrelated, stub `PublicWebsiteController`, and in a CORS test referencing an unimplemented route. | none real | **Missing entirely**, and load-bearing — the Android client's whole QR/public-entry contract (§1) is slug-addressed, so this isn't a cosmetic gap. |
| `activation status` | `Salon.active: Boolean` only | implicit in create/`deactivate()` (`DELETE /salons/{id}`) | **Working, but binary only** — no DRAFT/PENDING/multi-stage status (§1). |
| `owner relationship` | `Salon.ownerId: UserId` FK | enforced on every write/owner-scoped read | **Working**, uniformly enforced. |

---

## 3. Media / Images

- **Logo support:** **Missing.** No `logo`/`logoUrl`/image column exists on `Salon` or `salons` at all, at either the domain or schema level, on either backend branch.
- **Image upload:** **Missing**, client and server. No `@Multipart`/`MultipartBody` usage anywhere in Android's `app/src/main/java`. No multipart/file-upload controller anywhere in the backend — the only "upload"/"multipart"/"image" hits in the backend repo are unrelated Docker/nginx/deploy-script matches, not application code.
- **Storage strategy:** **Missing / undecided.** No object-storage integration (S3/GCS/Azure Blob/self-hosted) exists anywhere in either repo. `Specialist.photoUrl` (`specialists.photo_url VARCHAR(1000)`) is the **only** image-shaped field in the entire schema, and it is a plain string column — the API accepts whatever URL string is sent, it does not host or validate the image itself. There is nowhere in ROJAN's own stack today that would actually produce a hosted URL for that field.
- **Database references:** `specialists.photo_url` only. No equivalent column on `salons` or `services`/`service_categories`.
- **Retrieval APIs:** Trivial where the field exists (`GET` specialist endpoints already return `photoUrl` verbatim) — but retrieval isn't the gap; nothing produces a real, salon-owned image URL to retrieve in the first place. Android's actual working image rendering today is exclusively bundled app drawables wired per-item (`ASSET_READINESS_REGISTRY.md`, `ui/assets/RojanAssetNames.kt`), not per-salon uploaded content.
- **Required dependency:** an object-storage decision (System 1) — direct multipart-to-backend vs. signed-URL/direct-to-object-storage upload — is a prerequisite for any image-upload work on either side, salon logo included; today there isn't even a field to receive one on `Salon`.

---

## 4. Reception Readiness

```
Owner → Reception invitation → Authentication → Membership → Permissions → Booking access
```

**Authentication:** **Working, and correctly designed.** Reception's real OTP login flow (`ReceptionAuthViewModel.kt`) is complete and functional. Its role gate (`RECEPTION_GATE_ROLE = "MANAGER"`) is not a placeholder or bug — a doc comment in that exact file cites and confirms `ROJAN_System1_Backend_Decision_v2.md §1b`: System 1 has decided **not** to add a new global `UserRole.RECEPTIONIST`; Reception, Manager, and Owner accounts all share `UserRole.MANAGER` globally, with per-salon distinction resolved entirely through salon-scoped membership, never the global role. The gate was "correct as originally written."

**Reception invitation:** **Not built, but fully designed.** `ManagerInviteRepository`/`ReceptionInviteRepository` (Android) are unimplemented placeholder interfaces — zero concrete implementation. On the backend, `SalonInvite`/`InviteController` don't exist yet on any branch (confirmed by exhaustive search) — but `ROJAN_System1_Backend_Decision_v2.md §2` specifies the entire mechanism in implementation-ready detail: invite-by-phone-number (not direct-assign-by-userId — explicitly chosen to avoid needing a phone-number-lookup endpoint, which the codebase has an established anti-pattern stance against), a `SalonInvite` aggregate (`id, salonId, phoneNumber, role, status: PENDING/ACCEPTED/EXPIRED/REVOKED, issuedByUserId, expiresAt`), and a three-endpoint sequence (`POST /salons/{id}/invites`, `GET /invites/{token}` public preview, `POST /invites/{token}/accept` — binds to the *authenticated caller's own* phone number, `403` on mismatch). `SecurityConfig.kt` already reserves the route matchers for this, unimplemented behind them.

**Important, previously-unflagged consequence:** the decision document explicitly states Android's **already-committed** `SalonMembershipRepository`/`SalonMembershipApi`/`SalonMembershipDtos.kt` client code (direct-assign-by-userId shape, `PUT /salons/{salonId}/members/{userId}`) is **superseded** by the invite-based decision — that client code was built against a shape System 1 has now decided not to implement. It isn't broken today (nothing calls it against a live backend either way), but it represents Android-side work that will need to be replaced with an invite-flow client, not merely activated once a backend lands.

**Membership:** **Not built.** No `SalonMembership` domain type, repository, table, or migration exists on any backend branch — confirmed directly (this repo's own RBAC plan reached the same conclusion). `V8` is the next free migration number. Per the decision doc, this is item #1 in the approved dependency order — nothing else in this chain can be built correctly before it exists.

**Permissions:** **Not built, but fully decided.** `ROJAN_System1_Backend_Decision_v2.md §1c` fixes the permission-by-role mapping for v1 (no per-membership custom overrides yet):

| `SalonMemberRole` | Permissions |
|---|---|
| `MANAGER` | `MANAGE_SALON, MANAGE_MEMBERSHIP, MANAGE_CATALOG, MANAGE_STAFF, MANAGE_SCHEDULE_ALL, VIEW_CRM, MANAGE_CRM, MANAGE_BOOKINGS` |
| `RECEPTIONIST` | `VIEW_CRM, MANAGE_BOOKINGS` (explicitly not `MANAGE_CRM` — write access to CRM notes stays manager-only in v1; explicitly `MANAGE_BOOKINGS` not `MANAGE_OWN_BOOKINGS` — reception manages the whole salon's bookings, not a personal calendar) |

A `SalonPermissionResolver` to implement this table is specified but not yet built.

**Booking access:** **Blocked.** Every salon-scoped controller (`SalonBookingController`, `BookingController`, `CustomerController`, `WorkingHoursController`, `SpecialistScheduleController`) still applies the original owner-only check today. The decision doc's §4 dependency table sequences "authorization broadening" as item 6, depending on items 1 (membership) and 4 (permission resolver) — i.e. correctly scoped as the *last* step once membership+permissions exist, not something that can land independently. Until it lands, a receptionist — even with a hypothetically-working invite and membership — still cannot call a single booking-related endpoint.

**Net assessment:** Reception's non-owner access is still **fully blocked end-to-end today**, same bottom line as this repo's prior audits — but the *nature* of the blocker has changed materially: every open design/RBAC question is now resolved and documented (§1c's table, §2's invite sequence, §4's dependency order), and what remains is a scoped, sequenced implementation backlog rather than pending decisions. `ROJAN_DesignLab/ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` (same-day, this repo) still frames these as open questions for System 1 to decide — that framing is now stale relative to `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md` and should be reconciled before anyone starts implementing off the older plan.

---

## 5. Specialist Readiness

**Specialist creation:** **Working (backend).** `POST /api/v1/salons/{salonId}/specialists` (`SpecialistController.create` → `CreateSpecialistUseCase`), owner-only today (same broadening dependency as §4). `specialists` table: `id, salon_id FK, user_id FK (nullable), display_name, bio, photo_url, active, created_at, updated_at`.

**Mobile identity:** `Specialist.userId` is **optional** — a specialist can be a pure listing (no login) or linked to a real `User` account via `userId`. A separate, global `UserRole.SPECIALIST` value exists on `User` (`domain/user/User.kt`: `CUSTOMER, MANAGER, SPECIALIST`) for specialists who do log in.

**Membership:** Specialist-linked accounts are **not** part of the `SalonMembership`/`SalonMemberRole` model described in §4 at all — that model only defines `MANAGER`/`RECEPTIONIST` rows (§1c's table has no `SPECIALIST` row). A logged-in specialist's own access to, e.g., their own schedule/bookings is a **separate, unaddressed question** — not covered by `ROJAN_System1_Backend_Decision_v2.md`, and not verified in this pass beyond confirming the two role systems (global `UserRole.SPECIALIST` vs. salon-scoped `SalonMemberRole`) don't currently intersect. Worth a dedicated follow-up check before assuming specialist self-service access works once the membership work above lands — it may not automatically.

**Permissions:** As above — no specialist-specific permission set is defined in the current decision. The Android-side RBAC plan's vocabulary lists `MANAGE_SCHEDULE_OWN`/`MANAGE_OWN_BOOKINGS` as intended for specialist self-service, but neither appears in the backend's actual approved v1 mapping (§4) — these remain aspirational client-side, not backend-confirmed.

**Personal data access / tenant isolation — confirmed correct:** `SpecialistController`'s internal lookup, `findSpecialistOrThrow(salonId, specialistId)`, filters by **both** `SpecialistId` **and** `salon.id == SalonId(salonId)` before returning a result — a specialist ID from salon A requested under salon B's path returns `SpecialistNotFoundException` (404), not the record. This is the correct cross-tenant-isolation pattern, verified directly in source, not inferred. The same pattern is consistently used by every other salon-scoped controller inspected in this and the prior audit.

---

## 6. Customer Journey

```
QR entry → Public salon page → Services → Specialists → Booking creation
```

**Two structurally different paths exist in the Android client, with very different real status — this distinction matters and wasn't previously called out this precisely:**

**Path A — authenticated browse-then-book (Customer app, logged-in):** **Working end-to-end against the real backend.** `SalonApi.browseSalons`/`getSalon` (search/discovery), specialist selection, full date→time→confirmation→success booking flow, all calling real backend endpoints (`SalonController`, `SpecialistController`, `AvailabilityController`, `BookingController`). This is the path the prior same-day audit verified and is not re-litigated in depth here.

**Path B — QR entry (unauthenticated, slug-based):** **Non-functional against the real backend, at every step:**
- **QR entry:** no QR generation exists anywhere (§1) — there is no QR code to scan in the first place.
- **Public salon page:** `PublicSalonApi.getSalon(slug)` → `GET /api/v1/public/salons/{slug}` → **no matching backend controller** (§1). Would 404 (or worse, silently resolve to the unrelated `PublicWebsiteController` stub if routing overlaps in a way not verified here — worth an explicit integration-test check before assuming a clean 404).
- **Services:** `PublicSalonApi.getCategories`/`getServices` → same missing-controller problem, same `/public/salons/{slug}/...` path family.
- **Specialists:** `PublicSalonApi.getSpecialists` → same.
- **Booking creation (from this path):** `PublicSalonApi.getAvailableSlots` (same missing-controller problem) is as far as this contract goes client-side for the public path — actual booking *creation* from an anonymous QR entry isn't itself defined in `PublicSalonApi.kt`, meaning even a hypothetical fix to the missing-controller problem above would still need a decision on how an anonymous QR visitor converts into an authenticated booking (prompt for OTP login at that point? guest checkout? not specified anywhere in either repo).

**Report:**
- **Existing:** Path A (authenticated discovery → booking) is real and functional.
- **Missing:** Path B (QR/anonymous entry) is unimplemented at the backend from the very first call; the `slug` field it depends on doesn't exist on `Salon`; QR generation doesn't exist; the anonymous-to-authenticated conversion step for actually creating a booking from this path isn't designed anywhere yet.
- **Blocking:** if "Generate QR" / "Customer Booking via QR" is a required capability for the first salon's launch (rather than Path A alone being acceptable for a pilot), this is a full, multi-layer build — new `Salon.slug` field + migration, a real `PublicSalonController` matching the Android contract, QR generation (client or server), and a booking-creation decision for the anonymous-entry case.

---

## 7. Missing Capabilities Not Covered Above

Carried forward from this repo's prior same-day audit, still current, not re-verified against backend source in this pass (see `ROJAN_Independent_Release_Readiness_Audit_v1.md` / `ROJAN_QA_Remediation_Plan_v1.md` for full detail): no real payment integration beyond `PAY_AT_SALON` (`WALLET` is UI-only); no appointment reminders/push notifications (explicit `NoOpReminderScheduler`, by design); OTP verification **auto-registers a brand-new phone number as `CUSTOMER`** (confirmed directly at the source this pass: `VerifyOtpUseCase.kt:70`, `UserRole.CUSTOMER` hardcoded as the default for auto-registration) — meaning an invited staff member's first login would silently become a customer account unless resolved as part of the invite work in §4; the only signed release (`manager-v1.0.0`) predates Reception entirely and lacks a hardening fix (`63f2412`) the active branch also lacks.

---

## 8. Blocking Issues (Ordered)

1. **[System 2, low-effort]** No Android salon-creation/edit UI, despite the backend fully supporting it (§1, §2). The fastest, lowest-risk unblock in this entire audit — no backend or design work required, purely additive Manager screens over an existing, working contract.
2. **[System 1, implementation — not decisions]** `SalonMembership` + `/salon-access` + `SalonPermissionResolver` + `SalonInvite`/`InviteController` + authorization broadening (§4) don't exist yet, but every design question blocking them is now resolved per `ROJAN_System1_Backend_Decision_v2.md §4`'s approved, sequenced dependency order. This remains the single highest-leverage backend deliverable — it unblocks Reception, non-owner Manager staff, and (per §5's caveat) possibly informs specialist self-service too.
3. **[System 2, coupled to #2]** `SalonMembershipRepository`/`SalonMembershipApi` client code needs replacing with an invite-flow client once #2 lands — building against the current direct-assign client code would be building against a superseded shape.
4. **[System 1 + System 2, cross-team]** OTP auto-registers as `CUSTOMER` unconditionally — breaks invited-staff first login regardless of #2/#3's progress unless fixed alongside them.
5. **[System 1 + System 2, multi-layer]** The QR/anonymous customer-entry path (§6) is unimplemented at every layer — no `slug` field, no matching public controller, no QR generation, no anonymous-to-booking conversion design. Needs an explicit go/no-go for whether the first pilot salon requires this path or can launch on authenticated browse-then-book alone.
6. **[System 1, net-new]** No image/logo upload path anywhere (§3) — no storage strategy decided, no `Salon.logoUrl`-equivalent field exists yet.
7. **[Documentation/process]** `ROJAN_DesignLab/ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` (this repo) is now stale relative to `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md` — both exist, both are dated the same general period, and they disagree on whether the RBAC questions are open. Reconcile before either team implements off the older document.
8. **No DRAFT→ACTIVE lifecycle exists at all** (§1) — if a real pre-launch "complete your profile before going live" gate is a product requirement, it needs to be designed from scratch, not just wired up.
9. **Everything already listed in `ROJAN_Independent_Release_Readiness_Audit_v1.md`/`ROJAN_QA_Remediation_Plan_v1.md`** — not restated in full; still applies.

---

## 9. Dependencies

| Item | Depends on | Owner | Notes |
|---|---|---|---|
| Owner can create/edit their own salon from the app (§1, §2) | Nothing — backend already supports it | **[S2 only]** | Purely additive Manager UI + Retrofit calls over an already-complete contract |
| Owner can edit working hours from the app (§2) | Nothing — backend already supports full CRUD | **[S2 only]** | Same shape as above — `PUT`/`DELETE` calls already exist server-side |
| Reception/non-owner Manager staff access works (§4) | `SalonMembership` → `/salon-access` + `SalonPermissionResolver` → authorization broadening, per `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md §4`'s own approved order | **[S1]**, implementation only — no decisions pending | Highest-leverage backend deliverable; fully sequenced already |
| Invite flow works (§4) | `SalonMembership` (above) | **[S1]** for `SalonInvite`/`InviteController`; **[S2]** to replace the superseded direct-assign client with an invite-flow client | Two-sided, but no longer blocked by open questions |
| Invited staff don't silently become customers (§7) | OTP auto-registration default | **[S1↔S2]** | Coupled to the invite flow — must land together or invited staff's first login still breaks |
| QR/anonymous customer entry (§6) | `Salon.slug` field + migration, real `PublicSalonController`, QR generation, anonymous→booking design decision | **[S1]** for the field/controller/design decision; **[S1 or S2]** for QR generation depending on where it's produced | Multi-layer, currently zero built on any layer — needs an explicit scope decision, not assumed in-scope for a first pilot |
| Salon logo/photo upload (§3) | Object-storage decision + new upload endpoint + new `Salon` field | **[S1]** | No field, no storage, no endpoint exist yet — full net-new feature |
| Specialist self-service access (own schedule/bookings) (§5) | Clarify whether `SalonMemberRole` extends to specialists, or a separate mechanism is needed | **[S1]**, decision needed | Not addressed by the existing decision doc — genuinely open, unlike §4's items |
| DRAFT→ACTIVE lifecycle, if required (§1, §8) | Product decision on whether it's needed at all | **[Business]**, then **[S1+S2]** if yes | Currently doesn't exist; not a hidden feature waiting to be wired up |

---

## 10. Exact Files/Modules/APIs Involved

**Backend (`ROJAN_Backend`, read-only in this audit):**
- Salon: `domain/.../salon/Salon.kt`, `SalonRepository.kt`; `application/.../salon/CreateSalonUseCase.kt`/`UpdateSalonUseCase.kt`/`DeactivateSalonUseCase.kt`; `api/.../salon/SalonController.kt`, `SalonDtos.kt`; migration `V2__salon_management_schema.sql`.
- Working hours: `domain/.../schedule/WorkingHours.kt`, `WorkingHoursRepository.kt`; `api/.../schedule/WorkingHoursController.kt`; migration `V3__booking_engine_schema.sql`.
- Specialist: `domain/.../salon/Specialist.kt`, `SpecialistRepository.kt`; `application/.../salon/CreateSpecialistUseCase.kt`/`UpdateSpecialistUseCase.kt`; `api/.../salon/SpecialistController.kt`, `SpecialistDtos.kt`.
- Public/QR (missing): only `api/.../website/PublicWebsiteController.kt` exists (unrelated stub); no `PublicSalonController` anywhere; `bootstrap/src/test/.../CorsConfigurationIntegrationTest.kt` references the missing route.
- RBAC decision doc: `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md` (untracked, present in working tree) — **the single most load-bearing document for §4/§5 of this audit.**
- Auth/OTP: `application/.../auth/VerifyOtpUseCase.kt` (line 70: hardcoded `UserRole.CUSTOMER` on auto-register), `domain/.../user/User.kt` (`UserRole`: `CUSTOMER, MANAGER, SPECIALIST` — no `RECEPTIONIST`, by decision).
- Migrations present: `V1__init_schema.sql` … `V4__idempotency_keys.sql` (on `main`); up to `V7__add_booking_salon_start_time_index.sql` on `feature/auth-rate-limit-finalization`; `V8` is next-free, reserved for `SalonMembership`.

**Android (`ROJAN_DesignLab`):**
- Salon (read-only client): `data/remote/SalonApi.kt` (`browseSalons`, `getSalon` only — no create/update).
- Public/QR client (unreachable): `data/remote/PublicSalonApi.kt`, `domain/repository/PublicSalonRepository.kt`, `data/repository/PublicSalonRepositoryImpl.kt`.
- Working hours (read-only client): `data/remote/WorkingHoursApi.kt`, `data/repository/WorkingHoursRepositoryImpl.kt`, `domain/repository/WorkingHoursRepository.kt`.
- Membership (superseded shape, per decision doc): `data/remote/SalonMembershipApi.kt`.
- Reception auth/gate: `reception/presentation/auth/ReceptionAuthViewModel.kt` (contains the doc comment confirming the RBAC decision is closed).
- Invite placeholders: `manager/domain/repository/ManagerInviteRepository.kt`, `reception/domain/repository/ReceptionInviteRepository.kt`.
- Specialist client: `data/remote/SpecialistApi.kt`, `data/remote/ManagerSpecialistApi.kt`.
- Manager settings stub: `manager/screens/settings/.gitkeep`.
- Image/upload: no files exist (confirmed absent by search).
- Payment/reminders (carried forward, §7): `domain/booking/PaymentMethod.kt`, `screens/profile/WalletScreen.kt`, `domain/reminder/ReminderScheduler.kt`.

---

## 11. Recommended Implementation Order

**Phase 0 — Fastest, lowest-risk unblocks (System 2 only, backend already ready)**
1. Build the missing Manager "Settings" screens: salon create/edit, working-hours edit — both call already-complete backend endpoints. No System 1 work needed for this phase.

**Phase 1 — Backend membership/RBAC (System 1, implementation only — per `ROJAN_System1_Backend_Decision_v2.md §4`'s own approved order)**
2. `SalonMembership` domain + persistence (`V8` migration).
3. `GET /api/v1/users/me/salon-access` (unblocks Android's already-coded identity resolution).
4. `active: Boolean` on `UserResponse` (independent, ship anytime — decision §1e).
5. `SalonPermissionResolver` (§1c's fixed mapping).
6. `SalonInvite` domain + `InviteController`.
7. Authorization broadening across `SalonBookingController`/`BookingController`/`CustomerController`/`WorkingHoursController`/`SpecialistScheduleController` — one controller at a time, per the existing RBAC plan's own test-coverage sequencing.
8. `BookingResponse` enrichment (decision §3, independent, ship anytime).

**Phase 2 — Coupled Android work**
9. Replace the superseded `SalonMembershipApi`/`SalonMembershipRepository` client with an invite-flow client, once step 6 lands.
10. Fix OTP auto-registration defaulting to `CUSTOMER`, coordinated with step 6 so invited staff's first login resolves correctly.
11. Reconcile `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` against `ROJAN_System1_Backend_Decision_v2.md` explicitly — don't let two disagreeing planning documents both stay "current."

**Phase 3 — Explicit scope decisions before further build**
12. Decide whether QR/anonymous entry (§6) is required for the first pilot, or whether authenticated browse-then-book (already working) is acceptable at launch. If required: `Salon.slug` field + migration, real `PublicSalonController`, QR generation, anonymous→booking conversion design — all net-new.
13. Decide whether specialist self-service access (§5) is needed for the first pilot, and if so, design how it relates to (or stays separate from) the `SalonMemberRole` model.
14. Decide whether a real DRAFT→ACTIVE lifecycle gate is needed (§1/§8), or whether "active immediately on creation" is acceptable for a first pilot.
15. Payment (`WALLET` vs. `PAY_AT_SALON`-only) and reminders/notifications go/no-go — unchanged from prior audit, business decisions, not technical blockers.

**Phase 4 — Media**
16. Object-storage decision (System 1) for salon logo/photo upload, then build the field + endpoint + Android upload UI — lowest priority unless the pilot salon specifically needs owner-uploaded branding at launch.

**Phase 5 — Everything already sequenced in `ROJAN_QA_Remediation_Plan_v1.md`** once the above makes the app usable end-to-end.

---

*This report is a point-in-time audit artifact. No source code, configuration, database schema, or git history was modified in either `ROJAN_DesignLab` or `ROJAN_Backend` in producing it — reads only (`git fetch`, `git show`, file reads). Backend findings in this pass are grounded directly in `ROJAN_Backend` source, not inferred solely from the Android client, correcting several inference-based claims in this repo's own prior same-day audits.*

---

**STOP CONDITION MET — audit-only report generated. No implementation performed. Awaiting approval before any further action.**
