# Booking Discovery Leak — Fix Applied & Verified (Release Blocker P0-5)

**Date:** 2026-09-10 · **Backend repo:** `C:\AndroidProjects\ROJAN_Backend` ·
**Working branch:** `chore/adopt-ecosystem-governance-v2` (HEAD `608426d`) ·
**Deployed branch (target for cherry-pick):** `origin/release/v1.2.0-manager-dashboard-rbac-fix`
**No Android code changed. Not committed.**

> `rojan-release-manager` / `android-build-signing` are not registered skills/agents here — work done directly.

---

## TL;DR

The code fix from `BOOKING-409-RESOLUTION-REPORT.md` (fix #1) is now **applied and green**:
`GET /api/v1/salons` — the endpoint the customer app's salon discovery consumes — now returns
**only salons with `onboardingStatus == ACTIVE`**, matching the rule the public website surface
(`PublicSalonController`) already enforces. DRAFT salons can no longer be browsed into a booking
flow that `Salon.requireActivated()` then rejects with 409 `SALON_NOT_ACTIVE`.

- **Booking's `requireActivated()` rule is unchanged.** No business rule was bypassed or relaxed.
- The owner can still see and manage their own DRAFT salon via `GET /api/v1/salons/{id}` and
  `GET /api/v1/salons/mine` — those endpoints are not filtered.
- All affected integration + unit tests pass (see §4).

**Still required for a bookable pilot (ops, not code):** activate the ROJAN AI Pilot Salon
(`165958d6-1762-4520-a93f-c92fc1ba6cf9`). It meets the activation readiness rule; it needs an owner
token. Until then, `GET /api/v1/salons` on prod will legitimately return **zero** salons, because
the 5 currently-ACTIVE public salons have no services/specialists and the pilot is still DRAFT.

---

## 1. Change set (7 files — all under `C:\AndroidProjects\ROJAN_Backend`)

| # | File | Layer | Change |
|---|---|---|---|
| 1 | `infrastructure/.../persistence/salon/SalonSpringDataRepository.kt` | infra | Replaced `findByActiveTrue(...)` / `findByActiveTrueAndNameContainingIgnoreCase(...)` with `findByActiveTrueAndOnboardingStatus(status, pageable)` and `findByActiveTrueAndOnboardingStatusAndNameContainingIgnoreCase(status, name, pageable)`. |
| 2 | `infrastructure/.../persistence/salon/SalonRepositoryAdapter.kt` | infra | `findAllActive(...)` now passes `SalonOnboardingStatus.ACTIVE` to the new derived queries; added a comment explaining the rule and the 409 it prevents. |
| 3 | `domain/.../salon/SalonRepository.kt` | domain | Expanded the `findAllActive` KDoc to state the contract: `active` **and** `onboardingStatus == ACTIVE`; DRAFT excluded. |
| 4 | `application/src/test/.../salon/SalonTestFixtures.kt` | app test | `InMemorySalonRepository.findAllActive` fake now filters `it.active && it.onboardingStatus == ACTIVE`, mirroring the real adapter. |
| 5 | `bootstrap/src/test/.../SalonManagementFlowIntegrationTest.kt` | bootstrap test | `full salon hierarchy…` test: a brand-new (DRAFT) salon is now asserted **absent** from `GET /api/v1/salons`; added a block asserting the owner can still fetch it via `GET /api/v1/salons/{id}`. |
| 6 | `bootstrap/src/test/.../ApiHardeningIntegrationTest.kt` | bootstrap test | `browsing salons is paginated…` test: added `fullyActivateSalon(...)` helper (category → service → specialist → activate) and fully activates each of the 3 fixture salons so the pagination/filter/sort assertions still exercise real data. |
| 7 | `bootstrap/src/test/.../SalonActivationFlowIntegrationTest.kt` | bootstrap test | **New test:** `a DRAFT salon is excluded from the customer directory GET api v1 salons, and appears once activated` — GETs the directory as a customer while DRAFT (asserts absent), activates, GETs again (asserts present). |

`git diff --stat` for these 7 files: **+128 / −13**.

> The working tree also contains ~12 unrelated files modified by another team (booking-engine WIP:
> `BookingUseCases.kt`, `PublicSalonController.kt`, `GlobalExceptionHandler.kt`,
> `BookingEngineFlowIntegrationTest.kt`, `MediaFlowIntegrationTest.kt`, `SalonPilotRbacIntegrationTest.kt`,
> `BookingDomainExceptions.kt`, etc.) plus ~90 `ROJAN_*_v1.md` planning docs. **None of those were
> touched by this fix.** A commit must add only the 7 files above explicitly.

---

## 2. Why this is the right layer

`GET /api/v1/salons` → `SalonController` → `BrowseSalonsUseCase` → `SalonRepository.findAllActive`
→ `SalonRepositoryAdapter` → `SalonSpringDataRepository`. The leak was entirely in the adapter/
Spring-Data query: it filtered the `active` soft-delete flag but not `onboardingStatus`. Fixing it
at the repository query:

- keeps the rule in one place, identical to the public surface's rule;
- does not touch the booking path, the `requireActivated()` domain invariant, controllers, DTOs,
  or navigation;
- the derived-query name change is safe — grep confirmed `SalonRepositoryAdapter` was the only
  caller of the two removed methods.

---

## 3. `requireActivated()` — untouched (verified)

```
git diff -- domain/src/main/kotlin/ai/rojan/backend/domain/salon/Salon.kt
  (no output — file unchanged)
git diff -- application/src/main/kotlin/ai/rojan/backend/application/customer/EnsureCustomerAssociationUseCase.kt
  (no output — file unchanged)
```

`Salon.requireActivated()` still throws `SalonNotActiveException` for any non-ACTIVE salon, and
`EnsureCustomerAssociationUseCase` / reception booking still call it first. A booking attempt against
a DRAFT salon still fails 409 — that is the intended defence in depth. The fix only removes the way a
customer could *reach* that attempt through the UI.

---

## 4. Verification

### Compile
`./gradlew :infrastructure:compileKotlin :application:compileTestKotlin :domain:compileKotlin` → **EXIT 0**

### Integration tests (`:bootstrap:test`, embedded Zonky Postgres + real HTTP) → **BUILD SUCCESSFUL**

| Suite | tests | failures | errors |
|---|---|---|---|
| `SalonActivationFlowIntegrationTest` | 8 | 0 | 0 |
| `SalonManagementFlowIntegrationTest` | 5 | 0 | 0 |
| `ApiHardeningIntegrationTest` | 7 | 0 | 0 |
| `BookingEngineFlowIntegrationTest` | 4 | 0 | 0 |
| `ReceptionBookingFlowIntegrationTest` | 6 | 0 | 0 |

Key cases:

- **GET salons behaviour — DRAFT excluded:** `a DRAFT salon is excluded from the customer directory…`
  (new) — directory GET as a customer returns the salon **only after** `POST /activate`. ✔
- **GET salons behaviour — still paginated/filterable/sortable** for ACTIVE salons:
  `browsing salons is paginated, name-filterable, and sortable` (now with fully-activated fixtures). ✔
- **GET salons behaviour — owner still sees own DRAFT** via `GET /api/v1/salons/{id}`
  (`full salon hierarchy…`). ✔
- **POST booking against an ACTIVE salon → still 201:**
  `an activated salon accepts self-service booking and customer association normally` — unchanged by
  this fix, still `HttpStatus.CREATED`. ✔
- **POST booking against a DRAFT salon → still rejected:**
  `self-service booking against a DRAFT salon is rejected…` and
  `reception booking creation against a DRAFT salon is rejected…` — both still pass. ✔

### Application-layer unit tests (`:application:test`, salon + booking + customer) → **BUILD SUCCESSFUL**

23 suites, **0 failures / 0 errors** — including `EnsureCustomerAssociationUseCaseTest` (5),
`BookingUseCasesTest` (22), `SalonUseCasesTest` (6), `ActivateSalonUseCaseTest` (5).

---

## 5. Live production state (unchanged — informational)

| Endpoint | Returns today |
|---|---|
| `GET /api/v1/public/salons?size=100` | 5 salons (روژ, زیبا سرای حنا, سالن زیبایی ملک بانو, ققنوس, میاه) — none have services/specialists |
| `GET /api/v1/salons` (after this fix is deployed) | Will return the **same** 5 (all already ACTIVE) — the pilot salon stays hidden while DRAFT |
| ROJAN AI Pilot Salon `165958d6-…` | `onboardingStatus == DRAFT` — must be activated by its owner |

So deploying the code fix **stops the leak** but does not by itself make anything bookable end-to-end
on prod. Fix #2 from `BOOKING-409-RESOLUTION-REPORT.md` (activate the pilot salon) is still an open
ops action requiring the owner's token.

---

## 6. Deployment / commit notes

- The fix belongs on the deployed branch `origin/release/v1.2.0-manager-dashboard-rbac-fix`. On this
  local checkout it sits on top of `chore/adopt-ecosystem-governance-v2` alongside another team's
  uncommitted WIP.
- A commit must stage **only** these 7 files explicitly:
  ```
  git add \
    domain/src/main/kotlin/ai/rojan/backend/domain/salon/SalonRepository.kt \
    infrastructure/src/main/kotlin/ai/rojan/backend/infrastructure/persistence/salon/SalonRepositoryAdapter.kt \
    infrastructure/src/main/kotlin/ai/rojan/backend/infrastructure/persistence/salon/SalonSpringDataRepository.kt \
    application/src/test/kotlin/ai/rojan/backend/application/salon/SalonTestFixtures.kt \
    bootstrap/src/test/kotlin/ai/rojan/backend/bootstrap/SalonManagementFlowIntegrationTest.kt \
    bootstrap/src/test/kotlin/ai/rojan/backend/bootstrap/ApiHardeningIntegrationTest.kt \
    bootstrap/src/test/kotlin/ai/rojan/backend/bootstrap/SalonActivationFlowIntegrationTest.kt
  ```
- **Not committed** pending your go-ahead (per the task: "do not commit until verification" —
  verification is now complete).

---

## 7. Status

| Item | State |
|---|---|
| Code fix applied (`GET /api/v1/salons` filters `onboardingStatus == ACTIVE`) | ✅ Done |
| `requireActivated()` booking rule unchanged | ✅ Verified (no diff) |
| GET salons behaviour tested (DRAFT excluded / ACTIVE included / owner sees own / pagination) | ✅ Pass |
| POST booking against ACTIVE salon tested (→ 201) | ✅ Pass (existing test, unaffected) |
| Android code | ✅ Untouched |
| Commit | ⏸ Held for approval |
| Activate ROJAN AI Pilot Salon on prod | ⛔ Open — ops action, needs owner token |
