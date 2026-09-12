# ROJAN Phase A — Location (Latitude/Longitude) Contract Block Report v1

**Status:** Blocked. Android implementation held — no source files modified.
**Date:** 2026-08-15
**Role:** System 2 — Android Client Implementation
**Branch:** `feature/android-first-salon-pilot`, baseline `5e1bb14` (clean working tree at time of this report, aside from pre-existing untracked session `.md` reports)
**Scope:** Response to "ROJAN AI — Android Phase A Correction v1.0," which requested adding latitude/longitude support to the Owner Salon Identity flow on the stated premise that backend support is now approved and available.

---

## 1. Android Implementation Blocked

No Android code was modified in response to the correction request. Before touching `UpdateSalonRequestDto`, `ManagerSalonRepository`, `ManagerSalonSetupViewModel`, or `ManagerSalonSetupScreen`, the backend contract this task depends on was re-verified directly against source — and does not exist. Per this task's own "Backend as Single Source of Truth" rule and the "no fake data" discipline already established in Phase A (`ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md`), implementation was held rather than proceeding against an unverified premise.

## 2. Backend Dependency Missing

The correction request stated: *"The previous comments or assumptions stating that backend does not support latitude/longitude are no longer valid."* This does not hold against the current backend repository state:

- **Repository:** `ROJAN_Backend`, branch `main`, up to date with `origin/main`
- **History:** single commit (`8fe9df2` — "Initial backend production deployment"); no new commits since the original Phase A readiness audit
- **Working tree:** no uncommitted salon-related changes (only an unrelated `.claude/settings.local.json` diff and an untracked planning doc, neither touching `Salon`/`SalonDtos`/migrations)

No branch, unmerged PR, or in-progress work containing latitude/longitude support was found anywhere in this repository.

## 3. Verified Missing Contract Locations

Checked directly against source on disk, this session, not reused from a prior report:

| Location | File | Result |
|---|---|---|
| Request/response DTOs | `ROJAN_Backend/api/src/main/kotlin/ai/rojan/backend/api/salon/SalonDtos.kt` | `CreateSalonRequest`, `UpdateSalonRequest`, `SalonResponse` — exactly the same six fields as the original readiness audit (`name`, `description`, `phone`, `email`, `address` + audit fields). **No `latitude`/`longitude` field.** |
| Domain aggregate | `ROJAN_Backend/domain/src/main/kotlin/ai/rojan/backend/domain/salon/Salon.kt` | Zero matches for `latitude`/`longitude`/`lat`/`lng`/`geo`. |
| Database migrations | `ROJAN_Backend/infrastructure/src/main/resources/db/migration/` | Zero matches — no migration adds `latitude`/`longitude` columns to `salons`. |
| API documentation | `ROJAN_Backend/API_CONTRACT.md` | Zero matches. |

**Conclusion: latitude/longitude has no backend representation today, at any layer — identical to the finding in `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §1/§7-8.** Nothing has changed backend-side since that audit.

## 4. Why This Blocks Android Work

Adding `latitude`/`longitude` to the Android `UpdateSalonRequestDto` and wiring UI input fields to it, without a real backend field to persist them, would mean:

- An owner enters coordinates and taps save.
- The client reports success (the existing `name`/`description`/`phone`/`email`/`address` fields still save correctly).
- The coordinates are silently dropped — never persisted, never returned on the next read — a misleading UX outcome, not a real feature.

This directly conflicts with the correction request's own preserved-architecture rules ("Backend as Single Source of Truth") and the "strictly forbidden: create fake data / create local persistence" constraints already listed in that request.

## 5. Required System 1 Action

Before Android implementation can proceed:

1. Add `latitude: Double?` and `longitude: Double?` to `Salon` (domain aggregate).
2. Add a migration adding `latitude`/`longitude` columns to `salons` (additive, no existing column altered).
3. Add the same two fields to `CreateSalonRequest`, `UpdateSalonRequest`, and `SalonResponse` in `SalonDtos.kt`.
4. Confirm the field names/types Android should target (e.g. `Double?` vs. a `String`-typed coordinate pair) so the Android DTO can be written to match exactly, the same field-for-field verification discipline already applied to every other Phase A/B field.
5. Communicate the landing commit/branch back to System 2 so the next correction attempt re-verifies against real, present source — not a stated premise.

---

*This report is a point-in-time block record. No source code, configuration, database schema, or git history was modified in producing it. No commit or push was made.*

---

**STOP CONDITION MET — report created. Android implementation remains blocked. Phase B untouched. Waiting for System 1 to land the backend contract.**
