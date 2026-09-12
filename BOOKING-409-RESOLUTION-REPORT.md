# Booking 409 — Root Cause & Resolution (Release Blocker P0-5)

**Date:** 2026-09-10 · **Backend repo:** `C:\AndroidProjects\ROJAN_Backend` · **Live API:**
`https://api.rojanai.ir` · **Deployed branch (identified):** `origin/release/v1.2.0-manager-dashboard-rbac-fix`.
**No Android code changed. No backend code committed.**

> `rojan-release-manager` is not a registered skill/agent here — work done directly.

---

## TL;DR

`POST /api/v1/bookings` for the **ROJAN AI Pilot Salon** returns **HTTP 409 `SALON_NOT_ACTIVE`**
because that salon's `onboardingStatus` is **`DRAFT`**, and the booking path calls
`Salon.requireActivated()` before it does anything else. The customer app only offered that salon
because its discovery endpoint `GET /api/v1/salons` filters on the `active` soft-delete flag but
**not** on `onboardingStatus` — unlike the website's public surface, which correctly hides DRAFT
salons. So a customer can browse and walk an entire booking flow that is guaranteed to fail at
submit.

**Two fixes, both required, neither bypasses a business rule:**
1. **Code:** `GET /api/v1/salons` must exclude DRAFT salons (`onboardingStatus == ACTIVE`), matching
   the public surface. → prevents *any* DRAFT salon from being bookable-in-appearance.
2. **Data/ops:** activate the ROJAN AI Pilot Salon (it meets the readiness rule). → the only
   fully-configured salon on prod becomes genuinely bookable.

---

## 1. Reproduction

Observed across **4 sessions** (this project's prior release-readiness + redesign work), always
against the ROJAN AI Pilot Salon:

| Attempt | Slot | Result |
|---|---|---|
| session 1 | 17:00 | `POST /api/v1/bookings` → **409** |
| session 2 | 13:15 | **409** |
| session 3 | 15:15 | **409** |
| session 4 (release-readiness) | 09:00 | **409** — logcat: `--> POST https://api.rojanai.ir/api/v1/bookings (189-byte body)` → `<-- 409 (202ms)` |

Characteristics: **deterministic**, **~200 ms** (far too fast for a slot/overlap DB scan),
**independent of the time slot**, **nothing created** (non-destructive — the customer's Appointments
list stayed empty). The Android client maps *any* 409 to the generic string
"این عملیات با وضعیت فعلی سازگار نیست." (`ErrorMessages.kt:30` — `409 -> …`), so the on-screen
message does not reveal the `errorCode`.

### The pilot salon is DRAFT — proven via the production API (no auth needed)

```
GET https://api.rojanai.ir/api/v1/public/salons?size=100     → 200
  content: روژ, زیبا سرای حنا, سالن زیبایی ملک بانو, ققنوس, میاه   (5 salons)
```

`GET /api/v1/public/salons` returns **only `active && onboardingStatus == ACTIVE`** salons. The
**ROJAN AI Pilot Salon (`165958d6-1762-4520-a93f-c92fc1ba6cf9`) is NOT in that list** → its
`onboardingStatus` is `DRAFT`.

---

## 2. Root cause — exact code path (deployed branch `origin/release/v1.2.0-manager-dashboard-rbac-fix`)

`api/.../booking/BookingController.kt` → `create()`:

```kotlin
// line 117 — runs BEFORE createBookingUseCase, before any slot/overlap logic
ensureCustomerAssociationUseCase.execute(EnsureCustomerAssociationCommand(SalonId(request.salonId), customerId))
// line 119
val booking = createBookingUseCase.execute( … )
```

`application/.../customer/EnsureCustomerAssociationUseCase.kt` → `execute()`:

```kotlin
val salon = salonRepository.findById(command.salonId)?.takeIf { it.active }
    ?: throw SalonNotFoundException(…)
salon.requireActivated()          // ← line 36
```

`domain/.../salon/Salon.kt` → `requireActivated()`:

```kotlin
fun requireActivated() {
    if (onboardingStatus != SalonOnboardingStatus.ACTIVE) {
        throw SalonNotActiveException(id.value.toString())   // DRAFT pilot salon hits this
    }
}
```

`api/.../common/GlobalExceptionHandler.kt`:

```kotlin
@ExceptionHandler( … SalonNotActiveException::class … )
fun handleConflict(…) = respond(HttpStatus.CONFLICT, errorCodeFor(ex), …)   // 409
…
is SalonNotActiveException -> "SALON_NOT_ACTIVE"
```

→ **409, `errorCode = "SALON_NOT_ACTIVE"`.** This is the only 409 on the create path that fires
before slot/overlap logic and fires identically for every slot — matching all four observations.

*(The overlap path was ruled out: `GetAvailableSlotsUseCase` and `BookingRepositoryAdapter.reserve`
use the identical `ACTIVE_STATUSES = [PENDING, CONFIRMED]` and the identical overlap predicate
`startTime < end AND endTime > start`, so a slot shown as available never 409s for `BOOKING_CONFLICT`.
The pilot specialist's `available-slots` returned the full 09:00–17:30 grid each time — i.e. it has
no bookings on that day.)*

### Why the customer could pick a DRAFT salon — the contract inconsistency

| Surface | Endpoint | Filter | DRAFT visible? |
|---|---|---|---|
| Website (public) | `GET /api/v1/public/salons`, `…/{slug}` | `active && onboardingStatus == ACTIVE` (`PublicSalonController:158`) | **No** |
| **Customer app** | `GET /api/v1/salons` (`SalonApi` → `SalonListViewModel`) | `active` only — `SalonRepositoryAdapter.findAllActive` → `jpaRepository.findByActiveTrue(pageable)` | **Yes** ← bug |

`GET /api/v1/salons` ("Browse active salons") is consumed **only by the Android Customer app**
(verified across `ROJAN_DesignLab`, `ROJAN_Web`, `ROJAN_Desktop` — the website uses `/api/v1/public/*`;
the Manager app uses `/api/v1/salons/mine` and `POST`). So a DRAFT salon leaking into it has exactly
one victim: the customer's booking flow.

### Deployment note

The deployed backend is **not** `origin/main` (which has no phone-OTP `User` model and could not
serve the customer app's login). It matches `origin/release/v1.2.0-manager-dashboard-rbac-fix`
(has `VerifyOtpUseCase` + the `ensureCustomerAssociation`/`requireActivated` booking path + the
summary public-salon list DTO). The local working checkout (`chore/adopt-ecosystem-governance-v2`,
`608426d`, **not pushed**) carries ~12 files of unrelated in-progress work by another team — the
fixes below are specified against the **release branch**, not applied to that tree.

---

## 3. The fix

### 3A. Code — stop `GET /api/v1/salons` returning DRAFT salons  *(the systemic cause)*

Bring the customer discovery surface in line with the public surface. ~3 files, no new concept —
`onboardingStatus == ACTIVE` is already the rule everywhere a customer-visible salon list is built.

**`infrastructure/.../persistence/salon/SalonSpringDataRepository.kt`** — add two derived queries:
```kotlin
fun findByActiveTrueAndOnboardingStatus(
    onboardingStatus: SalonOnboardingStatus, pageable: Pageable,
): Page<SalonJpaEntity>

fun findByActiveTrueAndOnboardingStatusAndNameContainingIgnoreCase(
    onboardingStatus: SalonOnboardingStatus, name: String, pageable: Pageable,
): Page<SalonJpaEntity>
```

**`infrastructure/.../persistence/salon/SalonRepositoryAdapter.kt`** — `findAllActive()`:
```kotlin
val page = if (nameFilter.isNullOrBlank()) {
    jpaRepository.findByActiveTrueAndOnboardingStatus(SalonOnboardingStatus.ACTIVE, pageable)
} else {
    jpaRepository.findByActiveTrueAndOnboardingStatusAndNameContainingIgnoreCase(
        SalonOnboardingStatus.ACTIVE, nameFilter, pageable,
    )
}
```
(+ `import ai.rojan.backend.domain.salon.SalonOnboardingStatus`)

**`domain/.../salon/SalonRepository.kt`** — tighten the doc:
```kotlin
/** Browses ACTIVE salons (active soft-delete flag AND onboardingStatus == ACTIVE), … */
```

**Optional hardening (separate call):** `GET /api/v1/salons/{salonId}` (`SalonController.get`)
could also `?.takeIf { it.onboardingStatus == SalonOnboardingStatus.ACTIVE } ?: throw SalonNotFoundException(…)`
to match `PublicSalonController`. Not strictly needed once the list is fixed (a customer never
obtains a DRAFT id), and it may serve an owner "view my draft" need — needs its own consumer check,
so it is **not** part of the minimal fix.

**Tests to add:** `SalonControllerIntegrationTest` / `SalonRepositoryAdapterTest` — a DRAFT salon
with `active = true` must **not** appear in `GET /api/v1/salons`; an ACTIVE one must.

**No business rule is bypassed:** `requireActivated()` stays exactly as-is; this change makes the
*discovery* endpoint honour the same "DRAFT ⇒ not customer-visible" rule the public surface already
enforces.

### 3B. Data/ops — activate the ROJAN AI Pilot Salon  *(makes the flow verifiable)*

Fix 3A alone hides the pilot salon and leaves **nothing bookable on prod** — every one of the 5
ACTIVE salons is missing services and/or specialists (verified: only "ققنوس" has both a specialist
and a category, and that category has **0 services**). The pilot salon is the only fully-configured
one.

It **meets `ActivateSalonUseCase`'s readiness rule** (≥1 active service — "Haircut"; ≥1 active
specialist — "Pilot Specialist"; ≥1 working-hours day — Monday 09:00–18:00), so:

```
PATCH /api/v1/salons/165958d6-1762-4520-a93f-c92fc1ba6cf9/activate
Authorization: Bearer <the pilot salon owner's token>       # caller needs MANAGE_SALON on that salon
```

After this, `onboardingStatus = ACTIVE` → the salon appears (correctly) in `GET /api/v1/salons`
**and** `GET /api/v1/public/salons`, and `requireActivated()` passes → bookings proceed to the real
availability/overlap check.

*(If the pilot salon is intentionally kept DRAFT, an alternative is to fully configure one of the
existing ACTIVE salons with a service + eligible specialist. Either way, at least one ACTIVE,
fully-bookable salon must exist before the customer app can complete a booking on prod.)*

---

## 4. Verification status

| Item | Status |
|---|---|
| 409 is deterministic / fast / slot-independent | ✅ observed, 4 sessions |
| Pilot salon `onboardingStatus == DRAFT` | ✅ proven — absent from `GET /api/v1/public/salons` (ACTIVE-only) |
| Code path → `requireActivated()` → 409 `SALON_NOT_ACTIVE` | ✅ traced end-to-end on the deployed release branch; it is the only pre-slot, every-slot 409 on the create path |
| `GET /api/v1/salons` leaks DRAFT (no `onboardingStatus` filter) | ✅ confirmed in code on the deployed branch **and** `origin/main`; sole consumer = Customer app |
| No ACTIVE salon on prod is fully bookable | ✅ probed all 5 public salons — all missing services/specialists |
| Literal `errorCode: "SALON_NOT_ACTIVE"` from a live 409 body | ⚠️ **not captured** — needs a customer bearer token; the A72 was offline for the whole session (OTP SMS unreadable) and prod requires auth for `POST /bookings`. To close: `curl -X POST …/api/v1/bookings -H "Authorization: Bearer <token>" -d '{pilot salon ids}'` and read `.errorCode`, **or** raise the Android OkHttp logging interceptor to `BODY` for one repro. |
| Fix 3A applied / built / tested | ❌ **not done** — the deployed branch (`origin/release/v1.2.0…`) ≠ the local working tree (WIP-heavy, unpushed `chore/…` branch), and there is no deploy path from here to verify against the live 409. Specified above, ready to apply on the release branch. |
| Fix 3B applied | ❌ **not done** — needs the pilot salon owner's credentials. |

---

## 5. Recommendation

1. Apply **3A** on `origin/release/v1.2.0-manager-dashboard-rbac-fix` (and forward-port to `main`
   once the OTP/CRM work merges there), with the DRAFT-exclusion test. Deploy.
2. Do **3B** — activate the pilot salon (or configure an ACTIVE salon to be bookable).
3. Confirm: cold customer flow → salon list shows only ACTIVE salons → book the pilot → **201
   Created**, Success screen, appointment appears in `GET /api/v1/bookings/mine`.
4. Then P0-5 is cleared. (Also resolves readiness finding **P1-1** — guest Explore error — indirectly:
   `GET /api/v1/salons` still needs auth, but that is a separate item.)

**Stopped after diagnosis + fix specification. Nothing committed. Android untouched.**
