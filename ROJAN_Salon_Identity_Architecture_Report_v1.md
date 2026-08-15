# ROJAN Salon Identity & Ecosystem Architecture Review v1

**Role:** Architecture Validation + Product Integrity Team (System 2) — review-only. No source code, configuration, migrations, or APIs were modified in producing this report, in either `ROJAN_DesignLab` or `ROJAN_Backend`. Reads only (`git log`/`git grep`/file reads), consistent with `CLAUDE.md`'s System 1/System 2 boundary (backend *reads* for audit accuracy don't require confirmation; backend *changes* do).
**Date:** 2026-08-15
**Scope:** `C:\AndroidProjects\ROJAN_DesignLab` (System 2, branch `feature/android-reception-app` @ `1a3bdb0`), cross-referenced directly against `C:\AndroidProjects\ROJAN_Backend` source (`origin/main` @ `8fe9df2`, `origin/feature/auth-rate-limit-finalization` @ `28e9842`).
**Relationship to prior same-day artifacts:** This report synthesizes and cross-checks `ROJAN_First_Salon_Readiness_Audit.md`, `ROJAN_Independent_Release_Readiness_Audit_v1.md`, `ROJAN_QA_Remediation_Plan_v1.md`, and `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` (all dated 2026-08-15, all still current) against the specific lens this mission asks for: **is a salon a Beauty Identity Entity in this system, or only a database record?** It does not re-derive facts those reports already established — it cites them — and adds one directly-verified finding those reports did not surface (§5.1).

---

## 0. Headline Answer

**Today, a salon in ROJAN AI is a database record with a name, phone, and address — not yet a Beauty Identity Entity.** The backend `Salon` aggregate (`ROJAN_Backend/domain/.../salon/Salon.kt`, table `salons`) carries exactly seven business fields: `name`, `description`, `phone`, `email`, `address`, `active`, plus the owner FK. Everything that would make a salon feel like a *brand* — logo, cover image, gallery, interior/team photos, before/after portfolio, service images, geo-coordinates, reviews/rating, certifications — **does not exist anywhere in the backend schema today**, confirmed directly against both backend branches' migrations (`V1`–`V7`) and domain source, not inferred from the Android client.

The good news, and it's real: the Android client is architecturally *ready* to receive this identity model the moment it exists — Clean Architecture layering is intact, a remote-image rendering path already exists (`RojanRemoteImage.kt`, Coil-backed), and — notably — the Customer app has already been through a deliberate pass that **removed fabricated identity data (rating, reviews, facilities, gallery) rather than faking it**, which is the correct instinct for a "beauty identity" product and worth explicitly preserving as a principle going forward (§2.4). The gap is almost entirely backend-side data modeling, not Android capability.

---

## 1. Current Architecture Status

### 1.1 What "Salon" actually is today (backend, verified against source)

```
salons (V2__salon_management_schema.sql)
├── id UUID
├── owner_id UUID FK → users
├── name VARCHAR(255)
├── description VARCHAR(2000), nullable
├── phone VARCHAR(32)
├── email VARCHAR(255), nullable
├── address VARCHAR(500)          -- single free-text field, no structured components
├── active BOOLEAN                -- binary only, no DRAFT/PENDING lifecycle
├── created_at / updated_at
```

Related tables that exist alongside it: `branches` (a salon can have multiple physical locations — name/address/phone/active, same flat shape, no geo either), `service_categories`, `services`, `specialists`, plus a separate `working_hours`/`working_hours_intervals` domain (`schedule/WorkingHours.kt`) and the booking engine (`V3__booking_engine_schema.sql`). This is a coherent **operational** data model — enough to run a booking calendar — but it was never extended into an **identity/brand** data model. There is no `salon_media`, `salon_gallery`, `reviews`, `certifications`, or equivalent table anywhere in the schema.

### 1.2 What the Android client believes exists

The Android client's own domain model has partially drifted ahead of this — see §5.1 for the specific, directly-verified discrepancy (`Salon.logoUrl`/`latitude`/`longitude` fields that the Android code's own comments claim are "confirmed present on the wire," but which do not exist in either backend branch's schema, domain, or DTOs). This isn't a large practical risk today (the fields are nullable and simply resolve to `null`), but it is exactly the kind of drift this review exists to catch before more is built on top of an assumption that was never actually verified against backend source.

### 1.3 Architectural readiness (Android side) to eventually host a real identity model

- **Layering:** Clean. `domain/`, `manager/domain/`, `reception/domain/` (77 files, per `ROJAN_Independent_Release_Readiness_Audit_v1.md` §2) have zero `android.*`/`androidx.*` imports.
- **Remote image rendering:** Already built — `RojanRemoteImage.kt` (Coil), with fallback to the existing tinted `RojanIconContainer`/`SpecialistAvatar` icon path when a URL is null/blank/fails. This is the correct seam for *any* future salon-media field (logo, cover, gallery items) — no new rendering mechanism would need to be invented, just new fields to feed it.
- **Discipline around not fabricating identity data:** see §2.4 — this is a product-integrity strength worth calling out explicitly, not just an absence of a bug.
- **Cross-flavor model reuse:** `domain/repository/ActiveSalonContext.kt` (`ActiveSalonContext`, `AvailableSalon`) is shared, unforked, between Manager and Reception's auth/identity-resolution flows (`manager/domain/auth/ManagerActiveSalonState.kt`, `reception/domain/auth/ActiveSalonUiState.kt` both reference the same types). This is the right pattern — one salon-identity-shaped model, palette/UI differs per app, matching the same "one mechanic, per-app expression" principle `CLAUDE.md`'s Shared Premium Glass Design System already establishes for the design layer.

---

## 2. Existing Capabilities — Section by Section (against the requested checklist)

| Section | Field | Backend status | Android status |
|---|---|---|---|
| **Basic identity** | name | ✅ Working, full CRUD backend | ⚠️ Read-only client (no create/edit UI — `ROJAN_First_Salon_Readiness_Audit.md` §1) |
| | description | ✅ Working | ⚠️ Read-only |
| | logo | ❌ No field, anywhere | ⚠️ Android DTO/domain *claims* the field exists (§5.1) — it doesn't, backend-side |
| | cover image | ❌ Doesn't exist | ❌ Doesn't exist |
| | brand style | ❌ No concept at all (no theme/color/style field on `Salon`) | N/A — brand expression today is entirely the *app's* design system, not per-salon data |
| **Location** | address | ✅ Working (single free-text field, no structured street/city/postal breakdown) | ⚠️ Read-only |
| | latitude/longitude | ❌ No field, anywhere (verified: zero hits for `latitude`/`longitude` across all of `ROJAN_Backend`, both branches, including docs) | ⚠️ Android DTO/domain *claims* the field exists (§5.1) — it doesn't |
| **Visual Experience** | gallery | ❌ No concept | ❌ Explicitly removed from `SalonDetailsScreen.kt` rather than faked (§2.4) |
| | interior photos | ❌ No concept | ❌ Same |
| | team photos | ❌ No concept | ⚠️ `Specialist.photoUrl` exists per-specialist (real, working field) — the closest thing to "team photos" that exists, but it's specialist-scoped, not a salon-level team gallery |
| | before/after works | ❌ No concept | ❌ Same |
| **Services** | categories | ✅ Working — `service_categories` table, full CRUD | ✅ Read path working (`GET .../categories`) |
| | images | ❌ No image column on `services` or `service_categories` (verified directly against `V2__salon_management_schema.sql`) | ❌ Doesn't exist |
| | price | ✅ Working (`services.price NUMERIC(10,2)`) | ✅ Working |
| | duration | ✅ Working (`services.duration_minutes`) | ✅ Working |
| **People** | owner | ✅ Working — `Salon.ownerId` FK, uniformly enforced authorization | ✅ Working |
| | manager | ⚠️ Modeled only as "the owner" today — no distinct non-owner manager membership exists yet | ⚠️ Client contract built, backend not there (§4) |
| | receptionist | ⚠️ Fully designed (`ROJAN_System1_Backend_Decision_v2.md`), zero built | ✅ Client fully built, blocked on backend |
| | specialists | ✅ Working — `specialists` table, real CRUD, tenant-isolated correctly (verified: `findSpecialistOrThrow` filters by both `specialistId` **and** `salonId`) | ✅ Working |
| **Trust** | reviews | ❌ No table, no domain type, anywhere | ❌ Explicitly removed from UI (`MyReviewsScreen.kt` → `RojanComingSoonState()`, code comment: *"No review DTO exists anywhere... this was entirely demo state"*) |
| | rating | ❌ Doesn't exist | ❌ Same — `RojanRatingRow`/`RojanTokens.kt`'s gold rating color exist as **reusable UI primitives**, but nothing currently feeds them real data |
| | certifications | ❌ No concept anywhere | ❌ No concept anywhere |
| **Operations** | working hours | ✅ Full CRUD backend (`WorkingHoursController`: GET/PUT/DELETE per day) | ⚠️ Read-only client (`WorkingHoursApi` only calls GET) |
| | booking rules | ✅ Real booking engine exists (`V3__booking_engine_schema.sql`, `BookingController`, `AvailabilityController`) — functional end-to-end for the authenticated browse-then-book path | ✅ Working (Path A per §6 of the First-Salon audit) |
| | availability | ✅ Working, same engine | ✅ Working |
| | payments | ⚠️ `PAY_AT_SALON` only is real; `WALLET` is UI-only, no backend payment integration | ⚠️ Same |

**Reading this table:** Services/Operations (the *transactional* half of a salon) are genuinely solid. Basic Identity/Location/Visual Experience/Trust (the *brand* half — the actual "Beauty Identity Entity" the mission's approved principle asks about) are, with the single exception of specialist photos, **entirely unbuilt at the backend**. This is not an Android gap to close — there is no client code that could make a `salon_gallery` table exist.

### 2.4 A finding worth stating explicitly: the Android client's discipline is a strength, not a gap

`SalonDetailsScreen.kt`'s own code comments (lines ~149–177) document a deliberate choice made during a past "Android <-> Backend Full Integration milestone": rating/review-count, a "phone-book-style facilities list," and a real photo gallery were **all removed from the screen rather than rendered empty or fabricated**, because the backend `Salon` has no data to back them. `TopSpecialists.kt` similarly documents removing a *fabricated* `rating`/`reviewCount` that didn't correspond to any real field. `SalonCardAccent.kt` states outright: *"it never fabricates business data (rating, ...)"*.

This is exactly the right posture for a product whose core premise is that a salon is a trustworthy identity — fabricated ratings or a fake gallery would actively undermine that premise the moment a real user noticed. **Recommendation: preserve this "never fabricate identity data, gate behind Coming Soon or omit the section" convention explicitly as a standing rule** for every future Visual Experience / Trust field this report recommends building — it's already the codebase's own established practice, just not yet written down anywhere outside scattered code comments.

---

## 3. Missing Foundations

Ordered by what blocks the most downstream capability:

1. **No unified media/image concept at any layer.** The *only* image-shaped field in the entire backend schema is `specialists.photo_url` (a plain `VARCHAR(1000)` — accepts any string, doesn't host or validate an image). No `Salon.logoUrl`, no cover image, no gallery, no service images, no object storage integration (S3/GCS/Azure Blob/self-hosted) anywhere in either repo. Detailed in §4.
2. **No geo-coordinate model.** `address` is one free-text `VARCHAR(500)`. There is no `latitude`/`longitude` on `Salon` or `Branch`, and — see §5.1 — the Android client currently believes there is, which is a documentation/contract-integrity risk more than a functional one today.
3. **No Trust layer at all.** Zero reviews/rating/certification modeling anywhere. This is a genuinely large product gap relative to the "Beauty Identity Entity" framing — trust signals are typically core to how a customer chooses a salon — and nothing in either repo's roadmap docs currently schedules it (it doesn't appear in the First-Salon audit's blocking-issues list at all, because that audit was scoped to the booking pilot path, not identity).
4. **No DRAFT → ACTIVE salon lifecycle.** A created salon is immediately `active = true` and immediately publicly listed. If "complete your identity profile, then go live" is meant to be a real product gate (which a Beauty Identity Entity framing would suggest it should be — a salon shouldn't be publicly discoverable with no logo, no photos, no services yet), that gate needs to be designed from scratch (confirmed absent at both domain and schema level, per `ROJAN_First_Salon_Readiness_Audit.md` §1).
5. **No `slug` field, no public/QR-entry backend.** Blocks the entire unauthenticated customer-entry path (`PublicSalonApi.kt` is fully built client-side and unreachable — no matching backend controller exists). Not this report's primary focus (covered exhaustively in the First-Salon audit §1/§6) but relevant here because a public-facing salon *identity* page is precisely where Basic Identity + Visual Experience + Trust data would actually be displayed to a prospective customer — the page that would consume this report's recommended media model doesn't exist yet either.
6. **No Manager write UI over the backend capability that does exist.** Salon create/edit and working-hours edit are fully built backend-side and have zero Android UI (`manager/screens/settings/` is a `.gitkeep`-only stub). This is the fastest, lowest-risk item in this entire report to close — no backend or design work required (§8, Phase 0).

---

## 4. Media Architecture Recommendation

### 4.1 Current state (verified, not inferred)

- **Storage strategy:** Undecided/nonexistent. No object-storage client, config, or bucket reference anywhere in either repo.
- **Database references:** `specialists.photo_url` only — a raw string column, not a managed asset reference.
- **API readiness:** No `@Multipart`/`MultipartBody` usage anywhere in Android's `app/src/main/java`. No multipart/file-upload controller anywhere in the backend (the only "upload"/"multipart" hits in the backend repo are unrelated Docker/nginx/deploy-script matches).
- **What Android renders today:** Exclusively bundled app drawables wired per-item (`ui/assets/RojanAssetNames.kt`, `ASSET_READINESS_REGISTRY.md`) for anything that isn't already a real remote URL (`Specialist.photoUrl`, and — once §5.1 is resolved — `Salon.logoUrl`). There is no per-salon *uploaded* content anywhere in the product today.

### 4.2 Recommended target architecture

The mission's proposed shape is correct and should be adopted as the target:

```
Salon
 |
 Media Assets  (new aggregate: salon_media table)
 |
 ├── Logo            (role = LOGO, cardinality 1)
 ├── Cover            (role = COVER, cardinality 1)
 ├── Gallery          (role = GALLERY, cardinality N)
 ├── Portfolio        (role = PORTFOLIO_BEFORE / PORTFOLIO_AFTER, cardinality N, optionally paired)
 └── Service Images   (role = SERVICE, FK → service_id, cardinality 1 per service)
```

Concretely, this implies:

- **One `salon_media` table**, not five parallel image columns bolted onto `salons`/`services` — `id, salon_id FK, role (enum: LOGO/COVER/GALLERY/PORTFOLIO_BEFORE/PORTFOLIO_AFTER/SERVICE), target_id (nullable FK, e.g. service_id when role=SERVICE), storage_url, display_order (for gallery/portfolio ordering), created_at`. This is the same pattern the domain model already uses elsewhere in the schema (one `working_hours` table with a `day_of_week` discriminator rather than seven day-specific columns) — consistent with existing backend conventions, not a new one.
- **A storage decision first, before any field/endpoint work** — direct multipart-to-backend vs. signed-URL/direct-to-object-storage upload. This is explicitly a System 1 decision (per `ROJAN_First_Salon_Readiness_Audit.md` §3, restated here because it's the actual prerequisite for everything else in this section) — nothing else in this recommendation can be built correctly before it's made.
- **Specialist photos should migrate onto the same mechanism eventually** (`role = SPECIALIST_PHOTO, target_id = specialist_id`), retiring the ad hoc `specialists.photo_url` string column — not urgent, but avoids ending up with two parallel "how do I attach a photo to something" patterns in the same schema.
- **Android-side, this is additive, not a redesign**: `RojanRemoteImage.kt` (Coil) is already the correct rendering seam; a `SalonMedia` domain type + `GET /salons/{id}/media` read path, and eventually an upload flow (Manager-only, owner-authenticated), would slot into the existing repository pattern cleanly. No new architecture is required on the Android side to consume this once it exists.

### 4.3 Sequencing relative to the rest of this report

Media is correctly the **lowest-priority** item relative to the active first-salon pilot's booking-path work (per `ROJAN_First_Salon_Readiness_Audit.md`'s own Phase 4 placement) — a pilot salon can operate a real booking calendar with zero photos. But it should not be forgotten entirely: at minimum, a `Salon.logoUrl` field (single image, no gallery complexity, no ordering/portfolio pairing) would close the single largest gap between "database record" and "recognizable brand" for the lowest implementation cost, and — per §5.1 — the Android client already behaves as if this field exists. Building the real field to match the client's existing (currently false) assumption may be the cheapest single win in this entire report.

---

## 5. Data Consistency Issues (Task 3)

### 5.1 New finding: `Salon.logoUrl`/`latitude`/`longitude` — Android believes the backend already has these; it doesn't

**This was directly verified in this pass, not inferred, and is not previously documented in this repo's other same-day reports.**

- Android's `domain/repository/SalonRepository.kt` (`Salon` data class) and `data/remote/dto/SalonDtos.kt` (`SalonResponseDto`) both declare `logoUrl: String?`, `latitude: Double?`, `longitude: Double?`, and are fully wired through `SalonRepositoryImpl.toDomain()`.
- The code comment directly above the domain `Salon` class states: *"`[logoUrl]`/`[latitude]`/`[longitude]` were added for Salon Discovery - the backend `Salon` entity already had them, this domain model just didn't carry them through yet."* A comment in `domain/repository/SalonLocation`'s docstring goes further: *"a real `Salon.latitude`/`longitude` pair now flows through from the backend (**confirmed present on the wire**...)"* [emphasis added].
- **Directly verified against actual backend source in this pass:** `git grep -i "logo\|latitude\|longitude"` across the entirety of `ROJAN_Backend` on both `origin/main` and `origin/feature/auth-rate-limit-finalization` returns **zero matches in any source file** (only incidental matches in unrelated report `.md` files and one SMS-provider file with an unrelated string). The `salons` table (`V2__salon_management_schema.sql`) has no `logo`/`latitude`/`longitude` column. `Salon.kt` (backend domain aggregate) has no such fields.

**Practical impact today: low.** These are nullable fields with `= null` defaults; since the backend never sends them, they simply deserialize to `null` and the app's own remote-image fallback path (`RojanRemoteImage`) already handles a null URL gracefully. Nothing is currently broken by this.

**Why it matters anyway:** this is a **documentation/contract-integrity defect**, not a functional one — an Android-side comment asserts a specific backend capability ("confirmed present on the wire") that a direct check of backend source shows is false. If a future implementer (human or AI) trusts that comment rather than re-verifying against `ROJAN_Backend` source — exactly the trap `CLAUDE.md`'s own guidance about diffing against `git show HEAD:<path>` baselines is meant to prevent — they could build a "nearby salons" feature or a logo-upload UI on the assumption that half the contract already exists server-side, when it doesn't. **Recommendation: correct these two comments** (`SalonRepository.kt`, `SalonLocation`'s docstring) to state accurately that these fields are speculative/future-shaped, not confirmed-present, and treat `Salon.logoUrl`/`latitude`/`longitude` as net-new backend work (folding into §4.2's media recommendation and a future geo-coordinate addition), not as "just needs Android wiring."

### 5.2 Confirmed consistent: Customer/Manager/Reception share one backend source of truth

No duplicated backend model or divergent API path was found across the three flavors:
- All three call the same `ROJAN_Backend` REST API (`API_CONTRACT.md`); no flavor has its own mock/demo backend baked into a release path (`DemoIdentityProvider`/`DemoSessionProvider` are disconnected scaffolding with zero real callers, per `ROJAN_Independent_Release_Readiness_Audit_v1.md` §3).
- Salon-identity-shaped domain types (`ActiveSalonContext`, `AvailableSalon`) are genuinely shared, unforked, between Manager and Reception's identity-resolution code — not reimplemented per flavor.
- The RBAC vocabulary (`Permission` string set: `MANAGE_SALON`, `MANAGE_MEMBERSHIP`, `MANAGE_CATALOG`, ..., per `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` §3) is specified once and intended to be shared verbatim between backend and every client — a real, if not-yet-implemented, single contract, not per-app divergence.

### 5.3 A second, smaller drift: superseded membership DTO shape still committed

`data/remote/dto/SalonMembershipDtos.kt` (Android, already committed) is built against a **direct-assign-by-userId** shape (`PUT /salons/{salonId}/members/{userId}`). `ROJAN_System1_Backend_Decision_v2.md` (backend repo) has since decided on an **invite-by-phone-number** mechanism instead — already flagged in `ROJAN_First_Salon_Readiness_Audit.md` §4/§8 item 3, restated here because it's squarely a Task-3 "duplicated/inconsistent model" finding: this is real, committed Android client code built against a contract shape System 1 has already decided not to implement. It isn't causing any live failure (nothing calls it against a real backend today), but it will need to be replaced, not merely activated, once the invite backend lands.

### 5.4 No Specialist app, no real Website — confirmed absent, not partial

The mission's Task 3 asks to verify consistency across "Customer App, Reception App, Manager App, Specialist App, Website." Of these, only three exist as real, buildable products today:

- **Specialist App: does not exist.** No `specialist` product flavor anywhere in `app/build.gradle.kts`'s `flavorDimensions("target")` — confirmed absent by both this pass and `ROJAN_Independent_Release_Readiness_Audit_v1.md` §2/§6 ("not merely early-stage"). A specialist who logs in today does so as a generic `UserRole.SPECIALIST` account with no dedicated app experience.
- **Website: does not exist as a real product**, only as a stub. `PublicWebsiteController` (`GET /api/v1/public/{tenantSlug}/website`) is the sole backend artifact under this name, and it returns a **hardcoded literal response** (`"name" to "ROJAN AI", "description" to "AI Beauty Platform", "status" to "ACTIVE"`) — confirmed not reading from any salon data, per `ROJAN_First_Salon_Readiness_Audit.md` §1. There is no consistency question to answer for it yet — it isn't consuming the backend's real salon data at all, hardcoded or otherwise per-salon.

**This means Task 3's "duplicated models across five surfaces" question is currently a three-surface question in practice** (Customer/Manager/Reception), and those three are, per §5.2, in good shape relative to each other. The cross-consistency risk that actually exists today is Android-vs-backend drift (§5.1, §5.3), not Android-vs-Android drift.

---

## 6. First Salon Pilot Risk Review (Task 4)

This section deliberately does not restate `ROJAN_First_Salon_Readiness_Audit.md` §1–§11 in full — that document is the authoritative, already-approved pilot-path audit and remains current as of this pass (spot-verified in §5.1 above, with no contradictions found beyond the one new drift noted there). It's summarized here strictly through the *identity/media* lens this report's mission adds on top of that audit's booking-path lens.

```
Owner → Salon → Reception → Specialist → Customer → Booking
```

- **Owner → Salon:** blocked by the same gap in both audits — no self-service salon creation exists anywhere (Android has no create UI despite a working backend endpoint; §3 item 6 above). Through this report's identity lens: even once that's fixed, a newly-created salon would have **name/phone/address and nothing else** — no logo, no photos, no hours filled in yet, immediately `active = true` and publicly listed (§3 item 4). A pilot salon's first public appearance today would be a bare record, not an identity.
- **Salon → Reception → Specialist:** unchanged from the First-Salon audit — blocked on `SalonMembership`/RBAC backend work, fully designed, not built (§4 of that audit, restated in `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md`). No identity-layer dependency here.
- **Specialist → Customer:** the one place a real image already flows end-to-end (`Specialist.photoUrl`) — the pilot's specialist-facing identity is in noticeably better shape than the salon's own.
- **Customer → Booking:** Path A (authenticated browse-then-book) works end-to-end against real data. What a customer sees along that path today, per §2.4, is honest but visually thin — a name, an address, working hours, services with price/duration, and (once `Salon.logoUrl` is real, §4.3) a logo — with no gallery, no reviews, no certifications, because none of that exists yet and the app correctly refuses to fake it.

**Net new risk this report adds to the existing pilot audit:** if the business intends the *first real pilot salon* to present a materially richer identity than "name + address + services list" — the mission's own "Beauty Identity Entity" framing suggests exactly that expectation — then **media (at minimum, a logo) and possibly a Trust-layer placeholder (even just "reviews coming soon") should be pulled into the pilot's scope decision**, not deferred to "after the pilot" by default. This is a product/business scope call, not a technical blocker — flagged here so it's made explicitly rather than falling out of scope by omission.

---

## 7. Priority Roadmap After First Salon Pilot

Ordered by leverage, building on top of (not replacing) `ROJAN_First_Salon_Readiness_Audit.md` §11's already-approved sequencing. Items already covered there are cross-referenced rather than repeated.

**Phase 0 — already scoped elsewhere, restated as prerequisite context:** salon self-service creation (Android UI over an existing backend endpoint), working-hours edit UI, `SalonMembership`/RBAC backend work — see the First-Salon audit's own Phase 0–2. This report doesn't change that sequencing.

**Phase 1 — Identity foundation (new, this report's primary recommendation)**
1. **Correct the two misleading Android code comments** identified in §5.1 — zero-risk, doc-only, immediately actionable, prevents future work being built on a false assumption.
2. **System 1 decision + build: `Salon.logoUrl` as a real backend field** (single image, no storage-strategy complexity beyond "one URL"). Lowest-cost way to close the largest gap between "record" and "brand." Requires the object-storage decision from `ROJAN_First_Salon_Readiness_Audit.md` §3/§9 as a prerequisite.
3. **System 1 decision + build: `Salon.latitude`/`longitude`.** Independently useful (unblocks genuine "nearby salon" discovery, currently either a non-functional UI label or entirely absent — worth an explicit Android-side check once this exists) and low-complexity relative to media.

**Phase 2 — Media system (per §4.2)**
4. Object-storage decision (System 1) — the actual prerequisite for everything below it.
5. `salon_media` table + `SalonMediaController` (logo/cover roles first, gallery/portfolio/service-image roles once the table exists — additive, not a redesign, per the schema shape in §4.2).
6. Android upload flow (Manager-only, owner-authenticated) + gallery/portfolio rendering, reusing the existing `RojanRemoteImage` seam.
7. Migrate `specialists.photo_url` onto the same `salon_media` mechanism (low priority, consistency cleanup, not urgent).

**Phase 3 — Trust layer (new, currently absent from every roadmap document in this repo)**
8. **Product/business decision first:** is a real reviews/rating system in scope for the first pilot cohort, or is "Coming Soon" (the current, correct, honest state) acceptable through the pilot? This is explicitly not a technical call — flagged here because no existing document currently owns this decision.
9. If yes: `reviews` table (customer_id, salon_id or booking_id, rating, comment, created_at — the natural anchor is a completed booking, to prevent unverified reviews), aggregate rating computation, `ReviewController`, Android `MyReviewsScreen.kt` un-gating (already has its entry point built, per §2.4 — this would be activating an existing placeholder, not new UI work).
10. Certifications: lowest priority in this entire report — no existing scaffolding on either side, no product urgency signal found in any reviewed document. Revisit only after §8/§9 land.

**Phase 4 — DRAFT → ACTIVE lifecycle (per §3 item 4, cross-referenced from the First-Salon audit's own §8/§11 item 14)**
11. Product decision: is "complete your identity profile (name, address, at least a logo, at least one service) before going publicly live" a real launch gate, or is "active immediately on creation" acceptable indefinitely? Answering this after Phase 1–2 exist (rather than before) means the gate can actually check for something meaningful (a logo, photos) rather than being designed against fields that don't exist yet.

**Everything else** (payments beyond `PAY_AT_SALON`, reminders/notifications, QR/public entry, Specialist self-service access, test coverage, R8/ProGuard, branch divergence) is already fully sequenced in the First-Salon audit and QA remediation plan and is out of this report's scope to re-litigate.

---

## 8. Summary of New Findings vs. Restated Findings

To make this report's incremental value explicit, separate from what it inherited from same-day sibling documents:

**New in this pass:**
- §5.1: `Salon.logoUrl`/`latitude`/`longitude` Android-backend contract drift, directly verified as false against both backend branches — not previously documented.
- §2.4: the "never fabricate identity data" convention, identified and named as a standing principle worth preserving explicitly, not just an absence of a bug.
- §3 item 3 / §7 Phase 3: the Trust layer (reviews/rating/certifications) being **entirely absent from every existing roadmap document** in this repo — the First-Salon audit is scoped to the booking path and never surfaces this gap at all.
- §5.4: explicit confirmation that "5 surfaces" in the mission brief is actually 3 real surfaces today (no Specialist app, no real Website) — narrows what Task 3's consistency question actually needs to cover.
- §6: the observation that a pilot salon's *first public appearance* would be visually bare even once the booking-path blockers clear, and that this is a scope decision no existing document currently owns.

**Restated/cross-verified from prior same-day audits** (spot-checked directly against source in this pass, not merely cited): the backend `Salon` schema's actual field list (§1.1), `specialists.photo_url` as the only real image field (§2, §4.1), the RBAC/membership backend gap (§2 People row, §6), the QR/public-entry backend gap (§3 item 5), the Manager Settings UI gap (§3 item 6), the superseded membership DTO shape (§5.3).

---

*This report is a point-in-time architecture-review artifact. No source code, configuration, database schema, or git history was modified in either `ROJAN_DesignLab` or `ROJAN_Backend` in producing it — reads only. It does not authorize or begin any implementation, migration, or commit.*

---

**STOP CONDITION MET — architecture review and recommendations only. No implementation performed. Awaiting approval before any further action.**
