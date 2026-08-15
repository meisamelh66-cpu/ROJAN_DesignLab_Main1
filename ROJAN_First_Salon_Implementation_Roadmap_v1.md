# ROJAN First Salon Implementation Roadmap v1

**Status:** Planning artifact only. No source files were modified, no migrations created, no APIs changed, nothing committed or pushed in producing this document.
**Date:** 2026-08-15
**Authority:** System 1 decision response received (scope decisions below) — this document plans against those decisions. Per `CLAUDE.md`, planning/organizing this roadmap does not itself authorize starting any individual task; backend changes, RBAC/security changes, API contract changes, migrations, and any git commit/push still each require their own explicit go-ahead when work actually begins.
**Basis:** Synthesizes `ROJAN_Salon_Identity_Architecture_Report_v1.md`, `ROJAN_First_Salon_Readiness_Audit.md`, `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md`, `ROJAN_Pilot_Implementation_Task_Board_v1.md`, `ROJAN_QA_Remediation_Plan_v1.md`, and `ROJAN_Independent_Release_Readiness_Audit_v1.md` (all 2026-08-15, all current) against the newly-approved scope below. Facts restated from those documents are cited, not re-verified in this pass; this document's own contribution is sequencing and dependency mapping against the approved scope, not new source investigation.

---

## 1. Approved Scope

**Mandatory for first salon pilot:**
- Salon identity (basic identity fields)
- Location (address **and** structured latitude/longitude — new requirement, not previously in any Salon field)
- Services (categories, price, duration — already working backend-side)
- Reception operation (membership/RBAC-gated booking access)
- Specialist basic identity (name, bio, photo — not self-service access)
- Customer booking (the already-working authenticated browse-then-book path)
- Logo (single image)
- Cover image (single image)

**Deferred (explicitly, per this decision):**
- Advanced specialist panel (specialist self-service login/schedule/booking access)
- Full gallery (multi-photo, interior/team/before-after)
- Reviews
- Ratings
- Certifications
- AI layers

**Salon Identity v1 (the concrete field/feature list for Phase 1):** basic identity (name, description, phone, email, address), logo, cover, location (lat/long), services core (categories/price/duration — already built), people relationships (owner/manager/receptionist/specialist membership model).

**Media directive:** before the pilot, only *architecture design* for the full media system (the `salon_media` polymorphic model recommended in `ROJAN_Salon_Identity_Architecture_Report_v1.md` §4.2) is required — not built. The pilot's actual *implementation* is scoped down to exactly two fields: **Logo, Cover** — the minimum viable shape, not the general-purpose gallery/portfolio/service-image system.

**Location directive:** latitude and longitude are **required** fields for the pilot — elevated from "missing entirely" (per the architecture report §1.1/§3) to mandatory build scope.

**Trust Layer:** explicitly **post-pilot roadmap** (§6 below) — reviews, ratings, certifications are not pilot blockers and should not be scheduled against pilot-critical capacity.

**Scope gap flagged, not assumed:** QR/anonymous public-entry (`PublicSalonApi`'s slug-based path, per the architecture report §3 item 5 and the First-Salon audit §1/§6) is **not mentioned in either the mandatory or deferred list above**. This roadmap treats it as **out of pilot scope by default** (Customer booking = the already-working authenticated Path A only) but flags this as a scope point that should get an explicit yes/no rather than resolve by silence, since it's a multi-layer, currently-zero-built feature that would materially change Phase 4's shape if it were actually intended to be in scope.

---

## 2. First Salon User Journey

```
Owner → Salon → Reception → Specialist → Customer → Booking
```

Concretely, for the specific pilot salon, in the order a real rollout would happen:

1. **Owner** completes phone+OTP login (already working, all three flavors).
2. **Salon** is created — name, description, phone, email, address, latitude/longitude — then the owner uploads a **logo** and **cover image**. The salon's core service catalog (categories, services with price/duration) is entered.
3. **Reception** staff are invited (phone-number-based invite, per the already-approved RBAC decision in `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md`), accept the invite, and gain `SalonMembership.role = RECEPTIONIST` — scoped to `VIEW_CRM` + `MANAGE_BOOKINGS` per that same decision's permission table.
4. **Specialist** basic records are created by the owner/manager (display name, bio, photo) — this is already a real, working capability (`ManagerStaffEditScreen.kt` → `BackendSpecialistRepository` → `POST /api/v1/salons/{salonId}/specialists`), not new pilot work.
5. **Customer** discovers the salon via the authenticated browse path (`SalonListScreen`/`SalonDetailsScreen`), now showing a real logo/cover instead of a placeholder icon, sees services and specialists, and selects one.
6. **Booking** is created through the existing, already-functional booking engine (date → time → confirmation → success), with Reception now also able to view/manage that salon's bookings once step 3's backend work lands.

**What already works end-to-end today, unchanged by this roadmap:** steps 1, 4 (backend side), and 6 (Path A). **What this roadmap exists to build:** the salon-identity fields and media in step 2, the Reception/membership backend + Android write UI throughout steps 2–3, and the verification pass tying it all together (Phase 5).

---

## 3. Work Ownership

### System 1 (Backend/Database/API/Security-RBAC)

| Area | Responsibility |
|---|---|
| **Backend — Salon identity fields** | Add `logoUrl`, `coverImageUrl` (plain nullable string columns, same pattern as the existing `specialists.photo_url` precedent — not the full `salon_media` table, per the Media directive in §1) and `latitude`, `longitude` (nullable `DOUBLE PRECISION`) to the `salons` table + `Salon` domain aggregate + `SalonResponseDto`/create/update request DTOs. Extends the **already-working** `POST/PUT /api/v1/salons` endpoints — no new endpoint needed for the fields themselves. |
| **Backend — Media upload** | Object-storage decision (direct multipart-to-backend vs. signed-URL/direct-to-object-storage) — the actual prerequisite for anything else in this row, per `ROJAN_Salon_Identity_Architecture_Report_v1.md` §4.2. Then: an upload endpoint (or signed-URL-issuing endpoint) scoped to exactly logo + cover for the pilot — not the general `salon_media` table, which stays a design-only deliverable pre-pilot (§1). |
| **Backend — Media architecture design (design only, not built)** | Produce the `salon_media` polymorphic schema design (role enum: LOGO/COVER/GALLERY/PORTFOLIO_BEFORE/PORTFOLIO_AFTER/SERVICE, per the architecture report §4.2) as a written design artifact, so the pilot's minimal Logo/Cover implementation doesn't have to be thrown away when Gallery/Portfolio/Service-images are eventually built post-pilot — it should be built as a strict subset of the eventual shape, not a shape that later needs migrating. |
| **Database — Membership/RBAC schema** | `salon_memberships` table (`V__salon_membership_schema.sql`), per `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` §7 in full: `id, salon_id FK, user_id FK, role, active, created_at/updated_at`, unique `(salon_id, user_id)`, indexes on `user_id`/`salon_id`. |
| **Database — Migration sequencing** | Coordinate migration numbering between the Salon-identity-fields migration (§ above, additive to an existing table, no cross-dependency) and the `salon_memberships` migration (RBAC plan's own "V8 is next free" note) — both are independent, additive, and can land in either order or the same release; sequencing is a System 1 scheduling detail, not a technical dependency between them. |
| **API — Membership/RBAC surface** | `SalonMembershipController` (list/assign/revoke), `GET /users/me/salon-access`, `SalonInvite`/`InviteController` (issue + accept), broadened authorization on `SalonBookingController`/`BookingController`/`CustomerController`/`WorkingHoursController`/`SpecialistScheduleController` — the entire scope of `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` §5–10, already fully planned and awaiting this roadmap's Phase 2 to schedule it. |
| **API — Salon profile** | `PUT /api/v1/salons/{salonId}` and working-hours `PUT/DELETE` — **already exist and work today**, confirmed in `ROJAN_First_Salon_Readiness_Audit.md` §2. No new endpoint needed; only the new fields (§ above) extend the existing request/response DTO shapes. |
| **Security/RBAC — Decisions already made, need re-confirmation not re-decision** | `UserRole.MANAGER` shared across Owner/Manager/Reception accounts (no new global `RECEPTIONIST` role), invite-by-phone mechanism, and the `MANAGER`/`RECEPTIONIST` permission-by-role table — all already decided per `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md §1b/§1c/§2`. Per the Pilot Task Board §3.3, a **fresh, current** sign-off that this is still the intended design is recommended before Phase 2 starts, since the audit that found this decision flagged it can go stale as the role model evolves — this is a confirmation ask, not new design work. |
| **Security/RBAC — OTP auto-registration fix** | `VerifyOtpUseCase.kt` currently hardcodes `UserRole.CUSTOMER` for any new phone number, which would silently misclassify an invited staff member's first login. Needs a joint S1↔S2 fix design, coupled to the invite-accept flow (§5 below). |
| **Security/RBAC — Tenant isolation on new/broadened endpoints** | Every new membership-aware endpoint must preserve the existing cross-tenant-404 convention (`ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` §9/§10) — System 1's responsibility to build and test, System 2 cannot verify this from the client alone (`ROJAN_QA_Remediation_Plan_v1.md` §2.5). |

### System 2 (Android)

| Area | Responsibility |
|---|---|
| **Screens — Manager Settings (net new)** | Salon create/edit form (name, description, phone, email, address, latitude/longitude — manual numeric entry is sufficient for pilot scope; no map picker required unless separately requested), logo/cover upload UI (image picker + upload call + preview), working-hours edit screens (day-by-day interval editor calling the already-working `PUT`/`DELETE` endpoints). Today `manager/screens/settings/` is a `.gitkeep`-only stub — this is entirely new screen work, not a modification. |
| **Screens — Manager Membership** | Member list + invite-issue screen (`POST /salons/{id}/invites`), calling the new backend surface once it lands. Reuses the existing `ManagerInviteRepository` placeholder interface (already committed, unimplemented) as the integration point. |
| **Screens — Reception invite-accept** | Wire `ReceptionInviteRepository` (already a committed placeholder) to the real `GET /invites/{token}` preview + `POST /invites/{token}/accept` endpoints once built. |
| **Client integration — Salon rendering** | `SalonDetailsScreen`, `SalonListScreen`, `SearchScreen` already render `salon.logoUrl` via `RojanRemoteImage` — this becomes real (not the currently-null placeholder) the moment the backend field ships. Add `coverImageUrl` rendering (net new — no existing wiring, unlike `logoUrl`) and structured lat/long consumption (`SalonLocation` already has the fields, currently always null). |
| **Client integration — Contract correction** | Correct the two misleading code comments identified in `ROJAN_Salon_Identity_Architecture_Report_v1.md` §5.1 (`SalonRepository.kt`, `SalonLocation` docstring) once the real backend fields exist, so the comments describe verified reality rather than a previously-false "confirmed present on the wire" claim. |
| **Client integration — Membership DTO replacement** | Replace the superseded direct-assign-by-userId `SalonMembershipApi`/`SalonMembershipRepository`/`SalonMembershipDtos.kt` (already committed, built against a shape System 1 decided not to implement) with an invite-flow client matching the real backend contract, once §5 Phase 2 backend work lands — this is a replacement, not new-from-scratch client work. |
| **Client integration — Specialist basic identity** | Already built and working: `ManagerStaffScreen.kt`/`ManagerStaffEditScreen.kt` → `BackendSpecialistRepository` → real `POST/PUT /api/v1/salons/{salonId}/specialists` calls. No new Android work needed for Phase 3's mandatory scope (basic identity only) — confirmed by direct source inspection in this pass, not assumed. |
| **Client integration — Customer booking** | Already working end-to-end (Path A). No new Android work required for Phase 4's mandatory scope beyond consuming the new logo/cover/lat-long fields once they exist (covered above). |
| **Testing** | P0 test coverage per `ROJAN_QA_Remediation_Plan_v1.md` §4 (token refresh, booking lifecycle) plus new tests for every screen/repository built in Phases 1–2 of this roadmap, following the same coverage shape already established for auth ViewModels. |

---

## 4. Implementation Order

### Phase 0 — Foundation Validation

Purpose: close every open confirmation/decision that would otherwise block Phase 1–2 mid-stream. No new feature code in this phase.

1. **[S1↔S2]** Re-confirm the Reception `RECEPTION_GATE_ROLE = "MANAGER"` design and the `ROJAN_System1_Backend_Decision_v2.md` RBAC decisions are still current intent (Pilot Task Board §3.3) — fast, a sign-off not new work.
2. **[S1]** Confirm salon-scoped/tenant authorization is actually enforced server-side on every existing Manager endpoint today (Pilot Task Board §3.4) — cannot be verified from the Android client alone.
3. **[S1]** Object-storage decision for logo/cover upload (multipart vs. signed-URL) — prerequisite for all of Phase 1's media work.
4. **[Business/Ops]** Decide how the pilot's actual salon record gets created for pilot #1 — manual ops/DB action vs. waiting for the Phase 1 Android create-UI to exist. This determines whether Phase 1's create-screen is on the pilot's own critical path or can trail slightly behind a manually-provisioned first salon.
5. **[S1↔S2]** Explicit go/no-go on the QR/public-entry scope gap flagged in §1 — confirm Customer booking = Path A only for this pilot.
6. **[S1↔S2]** Confirm the OTP-auto-registers-as-`CUSTOMER` fix approach jointly, so it's ready to land alongside Phase 2's invite-accept flow rather than discovered as a blocker mid-Phase-2.

### Phase 1 — Salon Identity

7. **[S1]** Migration + domain/DTO changes: `Salon.logoUrl`, `Salon.coverImageUrl`, `Salon.latitude`, `Salon.longitude`.
8. **[S1]** Logo/cover upload endpoint (per Phase 0 item 3's storage decision).
9. **[S1]** Media architecture design document for the full `salon_media` system (design-only per §1's Media directive — no table built yet).
10. **[S2]** Manager Settings screens: salon create/edit (including lat/long fields), logo/cover upload UI.
11. **[S2]** Manager Settings: working-hours edit screens (backend already supports this — pure Android UI work, zero backend dependency, the single lowest-risk item in this entire roadmap per the First-Salon audit's own framing).
12. **[S2]** Customer-side rendering: `coverImageUrl` wiring (net new), lat/long consumption, comment corrections (§5.1 of the architecture report).

### Phase 2 — Reception Operation

13. **[S1]** `SalonMembership` domain + persistence (migration, JPA entity, repository adapter) — RBAC plan §5/§7.
14. **[S1]** `GET /users/me/salon-access` — RBAC plan §6/§8; this is the endpoint every login/cold-start already calls unconditionally on the Android side, so treat its correctness as load-bearing for basic app usability, not just a feature.
15. **[S1]** `SalonPermissionResolver` (fixed `MANAGER`/`RECEPTIONIST` permission mapping, RBAC plan §1c/§5).
16. **[S1]** `SalonInvite` domain + `InviteController` (issue + accept) — RBAC plan §6/§8, §4 step 7.
17. **[S1]** Broadened authorization, **one controller at a time with full test coverage each** (RBAC plan §4 step 6, §9, §10 — explicitly the highest-risk item in the whole backend plan; do not do this as one sweeping change): `SalonBookingController` → `BookingController` → `CustomerController` → `WorkingHoursController` → `SpecialistScheduleController`.
18. **[S1↔S2]** OTP auto-registration fix, landed alongside step 16 so an invited staff member's first login resolves correctly.
19. **[S2]** Manager Membership screens (member list, invite-issue) — new.
20. **[S2]** Reception invite-accept wiring — replaces existing placeholder.
21. **[S2]** Replace superseded `SalonMembershipApi`/`Repository`/DTOs with the real invite-flow client.
22. **[S2]** Re-run the Reception acceptance verification already documented in this repo (Pilot Task Board §4 item 7) — do not assume it still holds unchanged once the backend shape actually lands.

### Phase 3 — Specialist Basic Flow

23. **[S2/verification only]** Confirm `ManagerStaffScreen.kt`/`ManagerStaffEditScreen.kt` → `BackendSpecialistRepository` still functions correctly against the now-broadened authorization from Phase 2 step 17 (specifically `SpecialistScheduleController`, if specialist creation/editing itself is included in that broadening pass — confirm scope with System 1, since the RBAC plan explicitly listed `SpecialistController` write operations as **out of scope** for the initial broadening pass, meaning specialist CRUD may remain owner-only even after Phase 2 completes; verify this doesn't block Reception/Manager staff from managing specialists if that's an implicit pilot expectation).
24. No net-new backend or Android work required for mandatory scope — this phase is a verification/regression checkpoint, not a build phase, unless step 23's scope check surfaces a real gap.

### Phase 4 — Customer Booking Journey

25. **[S2/verification only]** Confirm the existing Path A booking flow renders the new logo/cover/location fields correctly once Phase 1 ships, and that no regression was introduced by the Phase 1/2 DTO changes (additive fields only — low risk, but verify).
26. No net-new booking-engine work required — this is confirmed already functional end-to-end.

### Phase 5 — Pilot Readiness Verification

27. **[S2]** P0 test coverage: token refresh (`TokenRepositoryImpl.kt`, `AuthInterceptor.kt`), booking lifecycle (`BookingRepositoryImpl.kt` + the Customer booking-flow ViewModel set) — per `ROJAN_QA_Remediation_Plan_v1.md` §4, still the largest coverage gap in the app and now directly exercised by this roadmap's Phase 2/4 work.
28. **[S2]** RQG pass on every new screen (Phase 1's Manager Settings, Phase 2's Membership screens): `assembleDebug`, design-token compliance, RTL layout, install + screenshot on device/emulator.
29. **[S1]** Confirmed staging/production backend deployment (`STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL`) — currently unset, per `ROJAN_QA_Remediation_Plan_v1.md` §2.4.
30. **[Ops]** Release signing keystore provisioned through a secure channel.
31. **[S1+S2]** Full end-to-end walkthrough against a real (or realistic staging) salon: Owner login → salon creation/identity completion → reception invite/accept → specialist record creation → customer discovery/booking → reception views the booking. This is the actual pilot-readiness gate — everything above this line is a prerequisite to being able to run this walkthrough meaningfully at all.

---

## 5. Dependencies

| # | Item | Owner | Required API | Required data model | Blocking dependencies |
|---|---|---|---|---|---|
| 1 | Salon logo/cover fields | S1 | Extend existing `POST/PUT /api/v1/salons` | `salons.logo_url`, `salons.cover_image_url` (new columns) | Phase 0 item 3 (storage decision) |
| 2 | Logo/cover upload endpoint | S1 | New upload/signed-URL endpoint | N/A (writes to item 1's columns) | Phase 0 item 3 |
| 3 | Salon lat/long fields | S1 | Extend existing `POST/PUT /api/v1/salons` | `salons.latitude`, `salons.longitude` (new columns) | None — independently buildable |
| 4 | `salon_media` design doc | S1 | N/A (design artifact only) | Design of future `salon_media` table (role enum) | None — should be informed by items 1-2's shape so the pilot fields are a strict subset |
| 5 | Manager Settings — salon create/edit UI | S2 | Items 1, 3 (existing `PUT /salons/{id}` otherwise already works) | Android `Salon` domain model extension | Items 1, 3 |
| 6 | Manager Settings — logo/cover upload UI | S2 | Item 2 | N/A | Item 2 |
| 7 | Manager Settings — working-hours edit UI | S2 | Already-existing `PUT/DELETE /salons/{salonId}/working-hours/{day}` | None new | **None** — buildable immediately, zero backend dependency |
| 8 | `SalonMembership` persistence | S1 | N/A (data layer) | New `salon_memberships` table | Phase 0 item 1 (re-confirmation) |
| 9 | `GET /users/me/salon-access` | S1 | New endpoint | Reads item 8 | Item 8 |
| 10 | `SalonPermissionResolver` | S1 | N/A (internal) | Reads item 8's `role` | Item 8 |
| 11 | `SalonInvite`/`InviteController` | S1 | New endpoints (`POST /salons/{id}/invites`, `GET /invites/{token}`, `POST /invites/{token}/accept`) | New `SalonInvite`/`salon_invites` model | Item 8, Phase 0 item 1 |
| 12 | Broadened authorization | S1 | Modifies existing endpoints (no new routes) | Reads items 8, 10 | Items 8, 10 — and must land one controller at a time (§4 Phase 2 step 17) |
| 13 | OTP auto-registration fix | S1↔S2 | Modifies `VerifyOtpUseCase` | None new | Item 11 (should land together) |
| 14 | Manager Membership screens | S2 | Items 9, 11 | Android `SalonMember`/invite domain types (partly already exist as placeholders) | Items 9, 11 |
| 15 | Reception invite-accept wiring | S2 | Item 11 | Already-committed `ReceptionInviteRepository` placeholder | Item 11 |
| 16 | Replace superseded membership DTOs | S2 | Item 11 | Removes `SalonMembershipDtos.kt`'s direct-assign shape | Item 11 |
| 17 | Specialist basic identity | **None — already working** | Already-existing `POST/PUT /api/v1/salons/{salonId}/specialists` | Already exists (`specialists` table) | None (verify only, per Phase 3 step 23's scope check against item 12) |
| 18 | Customer booking (Path A) | **None — already working** | Already-existing booking engine endpoints | Already exists | None (Phase 4 is verification-only) |
| 19 | P0 test coverage | S2 | N/A | N/A | None — independently schedulable, should not wait for Phases 1-4 |
| 20 | Staging/production deployment | S1 | N/A (infra) | N/A | None technically, but gates Phase 5's real-environment walkthrough |

---

## 6. Deferred Roadmap

Explicitly out of pilot scope per §1's approved decisions. Not restated in build detail here (see `ROJAN_Salon_Identity_Architecture_Report_v1.md` §7 Phase 2-3 for the fuller design sketch of several of these) — listed so they're tracked, not lost:

- **Full gallery** — the general-purpose `salon_media` table (GALLERY/PORTFOLIO_BEFORE/PORTFOLIO_AFTER/SERVICE roles), beyond the pilot's Logo/Cover-only implementation. The Phase 1 media design doc (§4 item 9) should anticipate this so the pilot build doesn't need to be redone.
- **Reviews** — no backend model exists anywhere; `MyReviewsScreen.kt` is already built as a Coming-Soon-gated entry point (per the architecture report §2.4), ready to be un-gated once a reviews backend exists. Natural anchor: tie a review to a completed booking, not an unverified freeform submission.
- **Ratings** — depends on Reviews (aggregate computation); `RojanRatingRow`/`RojanTokens.kt`'s star-rating color already exist as reusable UI primitives with nothing real feeding them yet.
- **Certifications** — no scaffolding on either side, lowest priority of the deferred set, no product urgency signal in any reviewed document.
- **Advanced specialist panel** — specialist self-service login/schedule/booking access. Explicitly flagged in the architecture report §2 (People row) and the First-Salon audit §5 as a genuinely **open design question**, not merely unbuilt: whether `SalonMemberRole` extends to specialists or a separate mechanism is needed has not been decided by System 1 at all (unlike the Manager/Reception RBAC decisions, which are made). This needs a design decision before any post-pilot build starts, not just implementation capacity.
- **AI layers** — no scope detail was given in this decision beyond the name; treat as fully undefined until a future scoping pass, not partially planned here.

---

## 7. Risks

### Architecture risks

- **Broadened-authorization regression risk (highest-severity item in this entire roadmap).** Every endpoint being broadened in Phase 2 step 17 currently has a trivially-correct, easily-audited owner-only check. A bug in the shared salon-access-check helper doesn't just fail a feature — it over-grants access to real salon/customer/booking data across every endpoint that adopts it. This is why the RBAC plan (and this roadmap, following it) insists on one controller at a time with full test coverage each, never a single sweeping change.
- **`salon_media` design/implementation drift.** If the pilot's Logo/Cover fields are built as ad hoc columns on `salons` (per §1's Media directive) without the Phase 1 design doc (§4 item 9) actively shaping them as a subset of the eventual polymorphic model, the post-pilot Gallery/Portfolio work risks needing a migration that moves Logo/Cover off `salons` and into the new table later — extra churn that a slightly-more-careful Phase 1 design pass avoids.
- **Manager's `ManagerRepositories` singleton bypass** (13 existing screens, per `ROJAN_Independent_Release_Readiness_Audit_v1.md` §2) is a pre-existing pattern this roadmap's new Manager Settings/Membership screens (Phase 1 item 10, Phase 2 item 19) could easily inherit by copying an existing screen as a template. Recommend the new screens follow Customer/Reception's ViewModel+Factory pattern instead, even though the existing Manager convention would be the path of least resistance — otherwise this roadmap adds to the technical debt `ROJAN_QA_Remediation_Plan_v1.md` §1.1 already flags for cleanup, rather than avoiding it.
- **Specialist-endpoint broadening scope ambiguity** (Phase 3 step 23) — the RBAC plan explicitly excluded `SpecialistController` writes from the initial broadening pass. If the pilot implicitly expects Reception or non-owner Manager staff to create/edit specialist records (a plausible real-world Reception task), this is a silent gap between the approved RBAC plan's scope and this roadmap's Phase 3 unless explicitly reconciled in Phase 0.

### Data consistency risks

- **The already-identified `logoUrl`/`latitude`/`longitude` comment drift** (`ROJAN_Salon_Identity_Architecture_Report_v1.md` §5.1) becomes *true* once Phase 1 ships — but until then, if any Phase 1 work is scoped or estimated by trusting the existing (currently false) Android comments rather than verifying against the real backend schema pre-change, effort could be misjudged. Correct the comments as part of Phase 1 (§3, Client integration row), not as an afterthought.
- **DTO field-name/casing mismatches** are the single highest-risk contract-drift point per the RBAC plan §8 — `SalonMembershipResponseDto`/`AssignMembershipRequestDto` field shapes must match Android's existing committed types exactly, since a naming mismatch fails silently at deserialization, not at compile time on either side. Recommend an explicit contract test (per RBAC plan §9) rather than relying on manual cross-checking.
- **Superseded `SalonMembershipDtos.kt` remaining in the codebase** until Phase 2 step 16 replaces it is a live risk if any code path is accidentally wired to it in the interim (nothing calls it against a real backend today, per the architecture report §5.3 — but that changes the moment Phase 2's real backend exists, if the replacement isn't sequenced correctly).
- **Migration-numbering coordination** (§3, Database row) — if the Salon-identity-fields migration and the `salon_memberships` migration are developed in parallel branches without coordination, a numbering collision or ordering assumption could cause a merge conflict or, worse, a silently-wrong migration order in a shared environment. Low technical risk (both are additive, independent), but a real process risk given two more-or-less-parallel workstreams in Phase 1/2.

### Release risks

- **No confirmed staging/production backend deployment** (§4 Phase 5 item 29) — this roadmap's entire Phase 5 walkthrough is unrunnable against a real environment until this exists; unchanged from `ROJAN_QA_Remediation_Plan_v1.md` §2.4/§3.
- **Test coverage gaps on exactly the paths this roadmap most exercises** — token refresh and booking lifecycle (Phase 5 item 27) currently have zero coverage; Phase 2's authorization broadening and Phase 1's new screens are precisely the kind of change that a silent regression in these untested paths would make hard to catch before it reaches a real pilot salon's data.
- **Two branches already diverged from `main`** (`feature/android-reception-app` 46 commits ahead, `feature/manager-backend-integration` 20 commits unpushed, per the release-readiness audit §6 item 5) — this roadmap's Phase 1/2 work will add further commits to an already-diverged branch; a merge/release-train decision is a prerequisite to any of this actually reaching a releasable state, independent of this roadmap's own progress.
- **R8/ProGuard disabled for release builds** and **no release signing keystore present in this environment** (release-readiness audit §6 items 3, 7) — both pre-existing, both still open, both gate Phase 5's "genuine production ship" framing even if the pilot itself ships via a less formal channel (e.g., a signed internal/staging build).
- **RBAC decision staleness** (Phase 0 item 1) — if this re-confirmation step is skipped and the underlying decision has quietly changed since `ROJAN_System1_Backend_Decision_v2.md` was written, Phase 2's entire backend build would be built against a stale premise. This is explicitly why Phase 0 exists as a gate rather than folding straight into Phase 1.

---

*This roadmap is a point-in-time planning artifact. No source code, configuration, database schema, or git history was modified in producing it, in either `ROJAN_DesignLab` or `ROJAN_Backend`.*

---

**STOP CONDITION MET — roadmap generated. No implementation performed. Waiting for roadmap approval before any implementation.**
