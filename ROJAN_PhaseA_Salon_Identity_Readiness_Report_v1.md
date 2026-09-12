# ROJAN Phase A — Owner Salon Identity Readiness Report v1

**Status:** Audit only. No code written, no mock APIs created, no local fake storage created, no source files modified.
**Date:** 2026-08-15
**Branch:** `feature/android-first-salon-pilot` (created from `feature/android-reception-app` @ `a80e289`, no commits yet)
**Scope:** Verifies exactly what exists today — backend and Android — for the eight Phase A required fields (`name`, `description`, `phone`, `address`, `city`, `working hours`, `logo reference`, `cover image reference`), against the approved baseline (`ROJAN_First_Salon_Implementation_Roadmap_v1.md` Phase 1, `ROJAN_Salon_Identity_Architecture_Report_v1.md`, `ROJAN_First_Salon_Implementation_Task_Assignment_v1.md` Phase 1). All findings below are read directly from current source in this pass — `ROJAN_Backend` (`api/`, `domain/`, `infrastructure/src/main/resources/db/migration/`) and `ROJAN_DesignLab` (`app/src/main/java/`) — not inferred from prior reports, though they corroborate this pass's findings throughout.

---

## 1. Salon Entity Contract (Backend, Verified Directly)

**Domain aggregate:** `ROJAN_Backend/domain/.../salon/Salon.kt`. **Table:** `salons` (`V2__salon_management_schema.sql`). **Controller:** `SalonController.kt`. **Request/response DTOs:** `SalonDtos.kt`.

Confirmed field-by-field, reading `CreateSalonRequest`, `UpdateSalonRequest`, and `SalonResponse` directly (all three share the identical field set apart from response-only audit fields):

```kotlin
// CreateSalonRequest / UpdateSalonRequest (identical field set)
name: String            // @NotBlank, max 255
description: String?    // max 2000
phone: String            // @NotBlank, max 32
email: String?           // @Email, max 255
address: String          // @NotBlank, max 500 — single free-text field

// SalonResponse adds:
id: UUID, ownerId: UUID, active: Boolean, createdAt: Instant, updatedAt: Instant
```

**This is the complete, exhaustive field list — verified at the source, not summarized.** There is no `city`, `logoUrl`, `coverImageUrl`, `latitude`, or `longitude` field anywhere in the request DTOs, the response DTO, the domain aggregate, or the `salons` table schema. This confirms (at one layer deeper than prior same-day audits reached — the actual serialized wire DTOs, not just the domain aggregate/schema) that four of this phase's eight required fields have **zero backend representation today**.

---

## 2. Existing APIs (Backend, Verified Directly)

| Endpoint | Method | Auth | Status |
|---|---|---|---|
| `/api/v1/salons` | `POST` (create) | Authenticated, caller becomes `ownerId` | ✅ Working |
| `/api/v1/salons/{salonId}` | `PUT` (update) | Owner-only | ✅ Working |
| `/api/v1/salons/{salonId}` | `DELETE` (deactivate) | Owner-only | ✅ Working |
| `/api/v1/salons/mine` | `GET` (owner's own salons) | Authenticated | ✅ Working, **already consumed by Android** (§5) |
| `/api/v1/salons` | `GET` (browse active, paginated) | Public | ✅ Working |
| `/api/v1/salons/{salonId}` | `GET` (get by id) | Public | ✅ Working |
| `/api/v1/salons/{salonId}/working-hours` | `GET` (list) | Public | ✅ Working |
| `/api/v1/salons/{salonId}/working-hours/{dayOfWeek}` | `GET` / `PUT` (set) / `DELETE` (remove) | `GET` public, `PUT`/`DELETE` owner-only | ✅ Working, full CRUD, verified directly in `WorkingHoursController.kt` |

**Every API this phase needs for `name`/`description`/`phone`/`address`/`working hours` already exists and works.** Nothing in this row needs new backend endpoint work — only new Android call sites (§5) and, separately, new request/response *fields* for `city`/`logo`/`cover` (§1, §7).

---

## 3. DTOs — Including a Confirmed Contract Drift

**Backend → Android drift, confirmed at the exact DTO level in this pass:** Android's Customer-facing `data/remote/dto/SalonDtos.kt` (`SalonResponseDto`) declares:

```kotlin
val logoUrl: String? = null,
val latitude: Double? = null,
val longitude: Double? = null,
```

Backend's actual `SalonResponse` (§1, read directly this pass) has **none of these three fields**. This was previously flagged as a likely-false Android code comment in `ROJAN_Salon_Identity_Architecture_Report_v1.md` §5.1 based on a repo-wide `git grep`; this pass confirms it precisely at the exact DTO class the wire response deserializes into — `SalonResponse` in `SalonDtos.kt` has exactly the six fields listed in §1, full stop. Functionally harmless today (Kotlin's nullable defaults mean these fields silently resolve to `null`), but **this exact drift is what Phase A must resolve, not inherit** — building `city`/`logo`/`cover` on the Android side without first confirming the real backend field names risks repeating this exact mistake a third time.

**Working hours DTOs** (`WorkingHoursDtos.kt`, backend): `TimeIntervalDto { start: LocalTime, end: LocalTime }`, `SetWorkingHoursRequest { intervals: List<TimeIntervalDto> }`, `WorkingHoursResponse { id, salonId, dayOfWeek, intervals, createdAt, updatedAt }`. Android's `WorkingHoursResponseDto` (consumed by the existing `GET`-only client) already matches this shape for reads — a `SetWorkingHoursRequest`-equivalent write DTO does not exist on the Android side yet (§4).

---

## 4. Repositories (Android, Verified Directly)

| Repository | Flavor | Methods that exist | Methods that don't |
|---|---|---|---|
| `SalonRepository`/`SalonRepositoryImpl` | Customer | `browseSalons()`, `getSalon(id)` | No `create`, no `update` |
| `SalonApi` (Retrofit) | Customer | `GET /salons`, `GET /salons/{id}` | No `POST`, no `PUT` |
| `ManagerSalonApi` (Retrofit) | Manager | `GET /salons/mine` only | No `POST`, no `PUT`, no `DELETE` |
| `WorkingHoursRepository`/`WorkingHoursRepositoryImpl` | Shared | `getWorkingHours(salonId)` | No `setWorkingHours`, no `removeWorkingHours` |
| `WorkingHoursApi` (Retrofit) | Shared | `GET .../working-hours` | No `PUT`, no `DELETE` |

**Zero write capability exists anywhere in the Android codebase for any Salon Identity field**, confirmed directly — no `createSalon`/`updateSalon`/`setWorkingHours` call site exists in `app/src/main/java` (verified by direct search, not inferred). This matches every prior same-day audit's finding and is reconfirmed here at the exact repository/API-interface level.

**No Manager-side Salon repository/domain type exists to extend, either** — `ManagerSalonApi` is read-only and there is no `BackendSalonRepository` (Manager) analogous to the already-working `BackendSpecialistRepository`. Building Phase A's write path means creating this repository, not extending one.

---

## 5. Existing Compose Patterns (Android, Verified Directly)

- **`manager/screens/settings/` contains exactly one file: `.gitkeep`.** Confirmed empty — zero screens, zero ViewModels, zero placeholder code. This is the intended location for Phase A's new screens (per the already-approved roadmap), not a location requiring cleanup first.
- **`SalonIdentityCard.kt`** (`manager/components/`) is the *only* existing component that renders any salon-identity field today — and it's a **read-only dashboard summary**, not a form: renders `salonName`/`salonCategory` (= the backend's `description`, relabeled — the code comment explicitly notes *"the backend has no distinct 'category' field"*) and `isActive`, via a generic `Storefront` icon (`ManagerIconContainer`) — no image/logo rendering, no phone, no address, no working hours displayed. It reads from `ManagerRepositories.salon`, which loads via `GET /salons/mine` (§6). This card would need new fields (logo, once real) but is not itself a form and shouldn't be treated as one.
- **`ManagerStaffEditScreen.kt`** (`manager/screens/staff/`) is the closest real precedent for a create/edit form: one screen handling both create and edit via a sentinel ID parameter (`ManagerDestinations.NEW_SPECIALIST_ID`), `ManagerGlassSurface`/`ManagerPrimaryButton`/`OutlinedTextField` for the form fields, reading/writing through `ManagerRepositories` (the known global-singleton pattern, not ViewModel+Factory — already flagged as architecture debt in `ROJAN_QA_Remediation_Plan_v1.md` §1.1, and already flagged in `ROJAN_First_Salon_Implementation_Roadmap_v1.md`'s own risk section as a pattern the new Phase 1 screens should **not** copy). This is the pattern to reference for *form shape*, not the pattern to reference for *data-flow architecture*.
- **`RojanRemoteImage.kt`** (Coil-backed remote image rendering with icon fallback) already exists and is the correct, ready-to-use seam for logo/cover rendering the moment those fields are real — no new image-rendering mechanism needs to be built.

---

## 6. Authentication / Role Access (Verified Directly)

**Owner access to their own salon already works today, independent of the not-yet-built membership/RBAC system:**

- `GET /api/v1/salons/mine` is authenticated (any valid session), returns the caller's own salons by `ownerId`, and requires no `SalonMembership`/`SalonPermissionResolver`/`salon-access` machinery — none of which exists yet (confirmed, still true as of this pass).
- Android already wires this: `ManagerRepositories.initialize()` calls `GET /salons/mine` to resolve the active salon id (per code comments in `ManagerDashboardInsights.kt`, `SalonIdentityCard.kt`, `ManagerDashboardApi.kt` — all three independently reference this same call).
- Backend-side write authorization on `PUT`/`DELETE /salons/{id}` is owner-only (`salon.ownerId != callerId` → `SalonAccessDeniedException`), the same uniform pattern verified across every salon-scoped controller in prior audits.

**Practical consequence for Phase A: the salon-identity create/edit screen can be built and can function correctly for the owner today, with zero dependency on Phase 2's `SalonMembership`/RBAC work.** This is consistent with the roadmap's own Phase 1/Phase 2 split, now confirmed with the exact mechanism (`/salons/mine` + owner-only write checks) rather than assumed from that split alone.

**What is *not* yet true:** a non-owner Manager/Reception staff member cannot access or edit salon identity, because `SalonMembership` doesn't exist — this is out of scope for Phase A by design (owner-only is correct for this phase) and remains Phase 2's concern.

---

## 7. Required Fields Readiness Matrix

| Field | Backend field exists? | Backend API exists? | Android read? | Android write UI? | Blocking dependency |
|---|---|---|---|---|---|
| `name` | ✅ `Salon.name` | ✅ create/update | ✅ | ❌ | None — buildable now |
| `description` | ✅ `Salon.description` | ✅ create/update | ✅ | ❌ | None — buildable now |
| `phone` | ✅ `Salon.phone` | ✅ create/update | ✅ | ❌ | None — buildable now |
| `address` | ✅ `Salon.address` (single free-text field, no structured components) | ✅ create/update | ✅ | ❌ | None — buildable now |
| `city` | ❌ **Does not exist** — confirmed absent from domain, schema, and all three DTOs | ❌ | ❌ | ❌ | **Backend migration + domain + DTO work required (System 1)** |
| `working hours` | ✅ `WorkingHours`/`WorkingHoursIntervals`, separate domain | ✅ full CRUD (`GET`/`PUT`/`DELETE` per day) | ✅ (`GET` only) | ❌ | None — buildable now (Android write UI only) |
| `logo reference` | ❌ **Does not exist** — confirmed absent from domain, schema, and all three DTOs (Android's `SalonResponseDto.logoUrl` is dead/false, §3) | ❌ | N/A | ❌ | **Backend migration + domain + DTO + storage decision required (System 1)** |
| `cover image reference` | ❌ **Does not exist** — no Android-side placeholder either, unlike `logoUrl` | ❌ | N/A | ❌ | **Backend migration + domain + DTO + storage decision required (System 1)** |

**Five of eight fields are buildable on the Android side today with zero backend dependency** (`name`, `description`, `phone`, `address`, `working hours`) — the backend contract for all five already exists and works. **Three of eight fields (`city`, `logo reference`, `cover image reference`) cannot be built at all yet** — not partially, not with a workaround — because no backend field exists to write to or read from.

---

## 8. Missing Backend Dependencies (Explicit)

Per the task's instruction to report these rather than work around them:

1. **`city` field.** No column on `salons`, no field on `Salon` domain aggregate, no field on any request/response DTO. Requires a System 1 migration + domain + DTO change. (Note: `address` remains a single free-text field regardless — adding `city` as a separate structured field is additive, not a replacement of `address`.)
2. **`logoUrl` field.** Same absence, all three layers. Additionally requires the object-storage decision (multipart vs. signed-URL) already flagged as open in `ROJAN_First_Salon_Implementation_Roadmap_v1.md` Phase 0 — this decision blocks the upload *endpoint*, not just the field itself.
3. **`coverImageUrl` field.** Same absence, all three layers, same storage-decision dependency as #2.
4. **Logo/cover upload endpoint.** Does not exist in any form (no `@Multipart`/`MultipartBody` usage anywhere in Android; no multipart controller anywhere in the backend) — net-new, blocked on the same storage decision.
5. **Working-hours write endpoint on the Android side** is not a backend dependency (the backend endpoint already exists and works) — listed here only to be explicit that this is the one Phase A item whose blocker is purely "no Android code has been written yet," not a missing backend capability.

**No other Phase A field has an unmet backend dependency.** `name`/`description`/`phone`/`address` are fully backend-ready today.

---

## 9. Rules Compliance Confirmation

- **No code written.** This report contains no Kotlin/SQL/DTO source beyond quoting existing, already-committed field definitions verbatim for citation purposes.
- **No mock APIs created.** No fake endpoint, stub controller, or placeholder response was introduced anywhere.
- **No local fake storage created.** No Room table, DataStore entry, or in-memory repository was introduced as a substitute for the missing backend fields.
- **Missing backend dependencies reported**, not worked around — §8 is the actionable list for System 1.

---

## 10. Summary for Approval

**Buildable now, zero backend dependency:** Manager Settings screens for `name`/`description`/`phone`/`address` edit (extending the existing, working `PUT /salons/{id}`) and working-hours edit (extending the existing, working `PUT/DELETE .../working-hours/{day}`) — both fully gated on the already-working owner-only `/salons/mine` + `ownerId` check, no membership/RBAC dependency.

**Blocked on System 1, cannot start:** `city`, `logo reference`, `cover image reference` — all three need new backend fields (migration + domain + DTO) before any Android work is meaningful; logo/cover additionally need the object-storage decision before an upload endpoint can exist.

**One correction needed as part of this phase's Android work, not deferred:** the false `logoUrl`/`latitude`/`longitude` fields already sitting in `SalonResponseDto` (§3) should be reconciled once real fields land, so Phase A doesn't ship a second generation of the same drift.

---

*This report is a point-in-time readiness artifact. No source code, configuration, database schema, or git history was modified in producing it.*

---

**STOP CONDITION MET — readiness report generated. No implementation performed. Waiting for approval before any implementation.**
