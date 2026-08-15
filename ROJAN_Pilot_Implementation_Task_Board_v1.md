# ROJAN Pilot Implementation Task Board v1

**Status:** Planning artifact only. No code, configuration, or repository state changed in producing this document. **STOP CONDITION: no implementation begins from this board without separate, explicit approval per task/category.**
**Date:** 2026-08-15
**Scope:** `C:\AndroidProjects\ROJAN_DesignLab` (System 2 / Android repo), branch `feature/android-reception-app` @ `1a3bdb0`.
**Basis:** Synthesizes `ROJAN_First_Salon_Readiness_Audit.md`, `ROJAN_Independent_Release_Readiness_Audit_v1.md`, `ROJAN_QA_Remediation_Plan_v1.md`, and `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` into one execution-ordered board for "get one real pilot salon operating end-to-end."
**Authority note:** "Authority Scope approved" is recorded as given for producing this board. Per CLAUDE.md, this approval covers *planning/organizing* the task list — it does not itself authorize starting any individual task. Categories 2 and 3 remain gated on System 1 regardless; Category 1 items still individually require the CLAUDE.md confirmations they're flagged with below (architecture changes, deletions) before coding starts.

---

## 1. Allowed Now by System 2

Pure Android-side, no new backend endpoint, no RBAC/API contract change, no cross-team decision needed. Still subject to normal CLAUDE.md automatic-action / confirmation rules noted per item.

| # | Task | Source | Confirmation needed before coding? |
|---|---|---|---|
| 1.1 | Add build-time hard-gate for `StagingRelease` (mirror existing `ProductionRelease` Gradle `check()`) | QA Plan 1.2 | No — small, contained, no design/architecture impact |
| 1.2 | Re-enable R8/ProGuard for release builds; verify existing `proguard-rules.pro` keep rules; rebuild all 3 release variants, confirm no runtime breakage | QA Plan 1.3 | No — hardening, reversible |
| 1.3 | Decompose `RojanNavGraph.kt` (1,154 lines) into per-flavor/per-feature nav graph files | QA Plan 1.4 | No — "standard refactoring inside approved scope" per CLAUDE.md, but scope carefully (nav graphs break easily) |
| 1.4 | Refactor 13 Manager screens off the `ManagerRepositories` global singleton onto ViewModel+Factory (also a test-coverage enabler — see §4) | QA Plan 1.1 | **Yes** — CLAUDE.md requires confirmation before major architectural changes; touches 13 files broadly even though scoped to presentation-layer pattern only |
| 1.5 | Consolidate acknowledged Manager/Reception auth-screen + identity-resolution duplication | QA Plan 1.5 | Recommend confirming as a single scoped task (not folded into other work) |
| 1.6 | Archive/delete stale `ROJAN_AI_Production_Readiness_Report.md`; consider consolidating ~25 root-level report docs into `docs/reports/` | QA Plan 1.6 | Doc-only, automatic action — but deletion of the stale file itself should be flagged per CLAUDE.md's delete-file rule |
| 1.7 | Remove dead auth scaffolding: unused email/password `login`/`register` (zero call sites), `DemoIdentityProvider`/`DemoSessionProvider` | QA Plan 1.7 | **Yes** — file deletion always requires confirmation per CLAUDE.md, even for confirmed-dead code |
| 1.8 | Add explicit OkHttp connect/read/write timeouts (currently implicit 10s defaults) | QA Plan 1.8 | No |
| 1.9 | Move `data.remote` exception types out of presentation-layer error mapping (4 ViewModels) into a domain-level sealed error type | QA Plan 1.9 | No — standard refactor |
| 1.10 | Reception launcher icon / real branding art (currently inherits generic Customer icon) | QA Plan 1.10 | No — asset/design task |
| 1.11 | Update local environment/tooling docs (CLAUDE.md's SDK path, AVD) to match actual machine state | QA Plan 1.11 | No — docs |
| 1.12 | Cherry-pick `63f2412` (release-signing hard-gate) onto `feature/android-reception-app`, or explicitly decide to keep releasing Manager-only from the existing signed line | First-Salon Audit §6 Phase 0.2 | Release-branch strategy is a **Business** call, not System 1 — flag for owner decision, not silent action |

---

## 2. Blocked by System 1

Nothing here is actionable in this repo. Tracked so System 2 capacity isn't silently spent elsewhere while these sit idle.

| # | Task | Source | Notes |
|---|---|---|---|
| 2.1 | `SalonMembership` persistence + `GET /users/me/salon-access` aggregation endpoint | First-Salon Audit §3.8, RBAC Plan §5-8 | Root cause of every non-owner Reception/Manager screen 403ing today. Highest-leverage single backend deliverable — full plan already written and awaiting System 1 sign-off in `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` |
| 2.2 | Broadened authorization (owner-only → owner-or-permitted-member) on `SalonBookingController`, `BookingController`, `CustomerController`, `WorkingHoursController`, `SpecialistScheduleController` | RBAC Plan §8, §10 | Sequenced one controller at a time behind full test coverage each — highest-risk item in the whole backend plan (over-broad access bug would leak real salon/customer/booking data) |
| 2.3 | `SalonInvite`/`InviteController` backend (issue + accept) | First-Salon Audit §3.9, RBAC Plan §4 step 7 | `ManagerInviteRepository`/`ReceptionInviteRepository` exist client-side as placeholder-only interfaces, zero backend, zero implementation |
| 2.4 | Non-owner authorization enforcement generally (currently owner-only across nearly every salon-scoped endpoint) | First-Salon Audit §3.8 | Same root cause as 2.1/2.2 |
| 2.5 | Staging/production backend deployment (`STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL` unset) | QA Plan §2.4 | Infra/deployment, not Android code |
| 2.6 | Salon record + owner-account creation (no self-service salon onboarding exists anywhere) | First-Salon Audit §3.1 | Single most consequential first-salon blocker — everything else assumes a salon already exists. Fastest unblock is a manual System 1/ops action for one pilot salon, not a build item |

---

## 3. Requires API Confirmation

Cross-team decisions or explicit confirmations needed from System 1 *before* System 2 can safely start building — not pure backend build items, and not free for Android to just build against an assumed contract.

| # | Item | Source | What's being asked |
|---|---|---|---|
| 3.1 | RBAC decision (a): global `UserRole.RECEPTIONIST` vs. membership-scoped-only role | RBAC Plan §3, §4 step 1 | Blocks the `SalonMembership`/`SalonRole` domain model — building it twice is expensive. System 1 sign-off required before RBAC Plan §5 (domain layer) starts |
| 3.2 | RBAC decision (b): invite-by-phone vs. direct-assign-by-existing-userId | RBAC Plan §3, §4 step 1 | Backend's own `SecurityConfig` already has a reserved (unauthenticated GET) matcher suggesting invite-based is the provisional direction — needs explicit confirmation, not inference |
| 3.3 | Re-confirm Reception's `RECEPTION_GATE_ROLE = "MANAGER"` access-gate design is still current intent | QA Plan §3 blocker #2 | Documented in code as a confirmed historical System 1 decision; audit flags it can go stale as the role model evolves — needs a fresh sign-off, likely fast, not new work |
| 3.4 | Confirm salon/tenant-scoped authorization is actually enforced server-side on every Manager endpoint today | QA Plan §2.5 | `salonId` is a client-supplied path/query param; cannot be verified from the Android client alone |
| 3.5 | Salon profile update endpoint shape (`PUT`/`PATCH /api/v1/salons/{salonId}`) — confirm it exists or needs building, and its exact DTO shape | First-Salon Audit §3.2, §7 | No client call exists yet; needs a confirmed contract before the Manager Settings screen can be built |
| 3.6 | Working-hours write endpoint — confirm it exists (currently client only has read-side `WorkingHoursApi`) | First-Salon Audit §3.4 | Needed before a Manager hours-editing screen can be built |
| 3.7 | Specialist schedule/availability-override endpoint(s) — confirm shape, or that none exists yet | First-Salon Audit §3.5 | No Manager schedule-editing screen exists; nothing to build against without a confirmed contract |
| 3.8 | Image/photo upload path decision: direct multipart vs. signed-URL/object storage | First-Salon Audit §3.3, §6 Phase 1 item 4 | System 1 decision required before any Android upload-flow work starts |
| 3.9 | OTP-auto-registers-as-`CUSTOMER` fix approach — coupled with the invite decision (3.2) | First-Salon Audit §3.9, §5 | An invited staff member's first login currently silently becomes a customer account; needs a joint S1↔S2 fix design, not a unilateral Android fix |

---

## 4. Testing Tasks

All **System 2 / Android-side**, ordered by business risk per the existing QA plan. (Backend-side RBAC test strategy is covered separately in `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` §9 — System 1's own responsibility, not restated here.)

**P0 — highest business risk, currently zero coverage**
1. Token refresh / auth path: `TokenRepositoryImpl.kt`, `AuthInterceptor.kt` — a silent regression breaks every authenticated request across all three flavors.
2. Booking lifecycle: `BookingRepositoryImpl.kt` (create/cancel/confirm/complete/reschedule) + the entire untested Customer booking-flow ViewModel set (`BookingViewModel`, `BookingConfirmationViewModel`, `BookingDateViewModel`, `BookingTimeViewModel`, `AppointmentDetailsViewModel`, `RescheduleViewModel`, `BookingHistoryViewModel`).

**P1 — high-value, currently zero/near-zero coverage**
3. Every `Backend*Repository` in Manager and Reception — zero test coverage today.
4. Interaction-driven Compose UI tests (`performClick`-style) — none exist anywhere today; start with booking flow and auth flow. Note: depends on 1.4 (Manager ViewModel refactor) for the Manager-side screens to even be testable.

**P2 — coverage completion**
5. Remaining untested ViewModels (19 of 25 `*ViewModel.kt` files).
6. Reception instrumented tests — currently zero of any kind (screenshot or interaction).

**Cross-team-gated regression task**
7. Re-run the Reception acceptance verification already documented in this repo once §2.1/§2.2 (SalonMembership + broadened authorization) land — do not assume it still holds unchanged from before the backend work.

---

## Suggested Sequencing

1. **Now, no dependencies:** everything in §1 (1.1–1.11), each still passing its own listed confirmation gate.
2. **Send today, low effort:** §3.3 and §3.4 — these are confirmation asks, not build items, likely fast turnaround from System 1.
3. **Before Android work on Settings/hours/schedule/photos can start:** §3.5–3.8 need answers first — these directly gate Phase 1 of the First-Salon audit's recommended order.
4. **System 1's critical path (parallel to the above):** §2.1/§2.2, gated on §3.1/§3.2 being resolved first per RBAC Plan §4 step 1.
5. **Before relying on this app for a real release:** §4 P0 test coverage, and §1.4 (Manager refactor, which unblocks §4 P1 item 3 for Manager).
6. **Business/process decisions, tracked but not part of this board's four categories:** salon-creation-for-pilot-#1 (manual ops vs. admin endpoint), payment path (pay-in-person only vs. wallet/prepayment), reminders/notifications go/no-go, release-branch strategy (§1.12). None of these are S1/S2 build items — they're explicit go/no-go calls for the business, referenced here only so they aren't lost.

---

*This board is a point-in-time planning artifact. No source code, configuration, or git history was modified in producing it.*

---

**STOP CONDITION MET — task board generated. No implementation performed. Awaiting approval before any coding begins.**
