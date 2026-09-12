# ROJAN AI — TEAM 1 — MOBILE RELEASE AUTHORIZATION & DEPLOYMENT GATE v1

STATUS: AUTHORIZATION FRAMEWORK ONLY. No code was implemented, no build was produced, no device
action was taken, no git action (commit/push/merge) was performed.
ROLE: Team 1 — Architecture Authority + Technical Lead
REPO: `C:\AndroidProjects\ROJAN_DesignLab`, branch `feature/android-first-salon-pilot`, HEAD
`0afe738e62a0f47cce45f64e2c4e2d1e755fde28` — **69 commits ahead of `origin/main`, 0 behind**
(confirmed fresh this document). One uncommitted change exists in the working tree
(`BackendAppointmentRepository.kt`, the Booking Pagination Fix — implemented, tested, built, not
yet committed, per that project's own established "explicit approval for every git action"
discipline, unchanged and respected here).

**Grounding, stated once:** this document is not written from a blank slate. Two real, existing,
already-verified project artifacts inform every criterion below — `ROJAN_Independent_Release_Readiness_Audit_v1.md`
(2026-08-15, full architecture/security/test-coverage audit) and
`ROJAN_Android_Pilot_Session_Handoff_Report_v4.md` (2026-08-21, the most recent real, physical-
device validation session on record). Every claim below either cites one of these directly or is
marked as newly established by this document. **Reception, a real third flavor in this codebase,
is out of this document's scope** — this task names only Customer and Manager, and this document
does not expand that scope on its own initiative.

---

## PHASE 1 — Release Target Definition

### 1. ROJAN Customer App

**Purpose:** Customer booking experience.
**Delivery mechanism, confirmed real:** the `customer` Gradle product flavor of the single `:app`
module (`assembleCustomerDevDebug`/`assembleCustomerStagingRelease`/`assembleCustomerProductionRelease`)
— not a separate repository or module; one codebase, three installable APK flavors.

| Required flow | Verification status | Basis |
|---|---|---|
| Authentication (phone + OTP) | ✅ Code-verified, entirely server-side OTP against the real backend | Independent Audit §3 |
| Salon discovery | 🟡 Code exists; no physical-device validation on record for this flavor specifically | Independent Audit §2 (Customer: 15/15 repository interfaces implemented, cleanest architecture of the three flavors) |
| Salon profile | 🟡 Same status | Same |
| Services | 🟡 Same status | Same |
| Booking journey | 🔴 **Untested end-to-end, on any surface** — the Independent Audit's own §5 names the Customer app's *entire* booking-flow ViewModel set (`BookingViewModel`, `BookingConfirmationViewModel`, `BookingDateViewModel`, `BookingTimeViewModel`, `AppointmentDetailsViewModel`, `RescheduleViewModel`, `BookingHistoryViewModel`) as untested by unit test, and no physical-device booking-journey validation for Customer exists in any report found in this repository | Independent Audit §5, confirmed by this document's own file search finding no Customer-flavor device-validation report anywhere |
| Confirmation | 🔴 Same status as Booking journey — inherits its unverified state |

**Customer App is the less-proven of the two targets in this authorization**, despite having the
cleanest code architecture of the three flavors — a real, honest distinction this document does
not blur.

### 2. ROJAN Manager App

**Purpose:** Salon management experience.
**Delivery mechanism:** the `manager` product flavor (`assembleManagerDevDebug`/etc.).

| Required flow | Verification status | Basis |
|---|---|---|
| Authentication | ✅ **Physical-device-verified, real**: real OTP SMS to an authorized test number (`09164987585`), real multi-salon account, on a Samsung SM-A725F | Handoff v4 §2 |
| Dashboard | ✅ **Physical-device-verified, real**: correct salon identity display, confirmed against a live `GET /api/v1/salons/{salonId}` request/response in `logcat`, re-verified after a real bugfix (the Active Salon Fix, committed and pushed) | Handoff v4 §3 |
| Services | 🟡 Code exists (Independent Audit §2: 6/6 Manager repository interfaces implemented); no physical-device validation of the Services screen specifically found in any report | Independent Audit §2 |
| Specialists | 🟡 Same status — code exists, no device-validation report found for this specific screen | Same |
| Schedule/Shift access | 🟡 **Calendar screen loads and the specialist filter works, physical-device-verified** — but this is availability *viewing*, not schedule *authoring*; no report claims schedule-authoring was exercised on-device | Handoff v4 §4 ("Calendar screen loads. Specialist filter works") |
| Reports visualization | 🔴 Not mentioned in any report found — no basis to claim even code-level readiness beyond the general architecture finding in the Independent Audit | None found |

**A real, current, named blocker, restated from the most recent report rather than re-derived:**
Booking List/Detail/Confirm/Complete validation is **stuck** — a real backend page-size limit
(`size = 200` client request vs. a real, confirmed `MAX_SIZE = 100` backend constraint) was
producing a 400 on every attempt; the fix is implemented, tested, and built but **not committed**,
and whether real bookings even exist for the pilot salon to validate against remains unconfirmed
as of the most recent handoff. **This document does not authorize resolving that blocker itself**
— it is named here as the concrete reason Manager's own Booking-adjacent flows cannot yet be
marked verified, distinct from Dashboard/Auth, which are.

---

## PHASE 2 — Architecture Validation

**Backend Authority: PASS, for both apps, with one real, documented, non-Android caveat carried
forward honestly.**

- **No fake business authority found in either app** — confirmed by the Independent Audit's own
  repository-pattern review (Customer 15/15, Manager 6/6 real interface/implementation pairs, all
  calling the real backend via `Backend*Repository` classes) and its own explicit finding that
  **zero mocked/demo data paths remain live** (`DemoIdentityProvider`/`DemoSessionProvider` are
  confirmed disconnected scaffolding with zero real callers).
- **No local financial logic found** — neither flavor computes pricing, tax, or payment state
  client-side; not contradicted by anything in either source report.
- **No local booking authority found** — booking creation/confirm/complete/reschedule all call the
  real backend; the one real gap (Manager's page-size bug) is a client *request malformation*
  against a real backend constraint, not a local authority substitute for one.
- **No permission bypass found in Android-side code** — the Independent Audit's own explicit
  finding: "all other client-side role checks are correctly framed in code comments as UX
  convenience only, never as the actual security boundary — server responses (401/403/404/409)
  are treated as the source of truth throughout" (§3).
- **The one real, non-Android caveat, restated precisely, not smoothed over:** the Reception
  flavor's `MANAGER`-role access gate (`RECEPTION_GATE_ROLE = "MANAGER"`, no distinct
  `RECEPTIONIST` role) is a documented **System 1 (backend) decision**, not a Reception-app defect
  — irrelevant to Customer/Manager's own authorization boundaries and restated here only because
  it is the one place the Independent Audit found a security-adjacent ambiguity worth naming, even
  though this document's own two named apps are unaffected by it.

**Customer App: Backend consumption only — CONFIRMED**, per the Independent Audit's own
architecture finding (clean domain layering, zero `android.*`/`androidx.*` leakage, 15/15
real repository implementations).

**Manager App: Backend consumption only — CONFIRMED for data**, with one **architecture debt
item, not an authority violation**, carried forward honestly rather than omitted: 13 Manager
screens/components read from a global mutable singleton (`ManagerRepositories`) instead of going
through a ViewModel, and `ManagerDashboardScreen.kt` triggers a network sync directly from a
Composable `LaunchedEffect`. This is real, documented technical debt (Independent Audit §2/§7,
Medium severity) — it does not mean Manager fabricates or locally computes business data (it
still calls the real backend, just via an irregular access pattern), and this document does not
treat it as a Phase 2 failure, but it is named here so it is not silently forgotten before a real
release.

---

## PHASE 3 — Build Requirements

| Item | Definition |
|---|---|
| Build variant | `Dev` for the device-test gate itself (Phase 4) — matches every real validation performed to date (`assembleManagerDevDebug`, per Handoff v4). `StagingRelease`/`ProductionRelease` are **not** in scope for this first physical-installation authorization; a separate, later authorization is required before either is targeted, per the real, unresolved gap below. |
| Version code / version name | Not defined by this document — a build-tooling detail for whoever executes Phase 5's package, following whatever existing convention `app/build.gradle.kts` already establishes (not re-derived here; this document was not asked to, and does not, inspect that file's current values). |
| Signing policy | **Dev builds: unsigned/debug-signed is acceptable**, matching every real validation to date. **Release-variant signing is explicitly out of scope for this authorization** — the Independent Audit's own confirmed finding stands unresolved: no release signing keystore is present in this environment, and real signing material must be provisioned through a secure channel before any distributable release build exists (Independent Audit §6, item 3). This document does not authorize producing a signed release APK. |
| Environment configuration | Dev builds only, per the variant restriction above — `STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL` remain unset and unaddressed by this authorization, consistent with the Independent Audit's own finding that Production is correctly Gradle-hard-gated against a blank URL while Staging is not (a real, still-open inconsistency, restated as a risk in Phase 4/5 below, not resolved here). |

**Required artifacts, for this authorization's own scope:**
- `Customer` — `assembleCustomerDevDebug`
- `Manager` — `assembleManagerDevDebug`

No `StagingRelease`/`ProductionRelease` artifact is authorized by this document for either app.

---

## PHASE 4 — Device Test Gate

**Acceptance checklist, per app — restated as the binding bar, distinguishing what is already
proven from what this gate still requires:**

| Check | Customer App | Manager App |
|---|---|---|
| Installation | **Required, not yet performed for this flavor** | ✅ Already demonstrated (Samsung SM-A725F, Handoff v4) — re-confirm at execution time, don't assume it still holds without a fresh check |
| Launch | **Required, not yet performed** | ✅ Already demonstrated |
| Authentication | **Required, not yet performed** | ✅ Already demonstrated, real OTP, real device |
| API Connection | **Required, not yet performed** | ✅ Already demonstrated (live `GET /api/v1/salons/{salonId}` in `logcat`) |
| Session | **Required, not yet performed** | ✅ Already demonstrated (session preserved across reinstall in the Active Salon Fix verification) |
| Core User Flow | **Required — Booking journey, currently the least-verified path in either app (Phase 1)** | 🟡 **Partially demonstrated** (Dashboard, Calendar view, specialist filter) — **Booking Detail/Confirm/Complete remain the named, unresolved blocker (Phase 1)** and must be re-attempted and either passed or re-reported blocked, not assumed resolved by this document |

**PASS requires every row PASS for that app, independently** — Manager's real, prior partial
success does not exempt it from a fresh full pass; Customer's clean architecture does not exempt
it from needing its very first physical-device pass at all.

---

## PHASE 5 — Team 3 Execution Package

This document does not itself produce `ROJAN_MOBILE_RELEASE_EXECUTION_ORDER_v1.md` — it defines,
here, exactly what that separate execution package must contain when it is prepared, so its own
scope is not freely re-derived at that time:

```
ROJAN_MOBILE_RELEASE_EXECUTION_ORDER_v1.md must contain:

1. Approved scope
   - Exactly Phase 1's two apps, exactly the Dev build variant (Phase 3) — no Staging/Production
     build, no Reception flavor, no scope not named in this authorization.

2. Required APKs
   - assembleCustomerDevDebug, assembleManagerDevDebug — the two artifacts named in Phase 3,
     nothing else.

3. Validation checklist
   - Phase 4's own table, reproduced verbatim as the literal checklist to execute against —
     not re-invented, not loosened, not silently narrowed to only the rows already marked PASS
     for Manager.

4. Reporting format
   - Per app, per checklist row: PASS / FAIL / BLOCKED, with real evidence (device model, real
     account/phone number used, real logcat/network evidence for API Connection - matching
     the evidentiary bar Handoff v4 already set, not a lower one).
   - The Booking Pagination Fix's own current state (implemented/tested/built, not committed)
     must be explicitly addressed in that report - either committed (with explicit approval
     sought first, per this project's own standing git-action discipline) before Booking-flow
     validation is attempted, or the attempt explicitly reported as blocked on it, never silently
     worked around.
   - Any new defect found during device testing must be reported with the same rigor as the
     Independent Audit's own findings (a named root cause, not just a symptom) - not merely
     "booking failed."
```

---

## Known Limitations Carried Into This Authorization, Not Resolved By It

Restated once, plainly, so this document is not mistaken for a broader release readiness
declaration than it is:

1. **69 unmerged commits** sit on `feature/android-first-salon-pilot` relative to `origin/main` —
   this authorization concerns physical-device testing on this branch directly; it is not a merge
   or release-train decision, which remains separately required (Independent Audit §6, item 5).
2. **Test coverage on booking-critical paths remains thin** (Independent Audit §5) — physical-
   device validation in Phase 4 is a real, valuable check but does not substitute for the unit/
   integration coverage gap named there.
3. **R8/ProGuard is disabled for release builds** — irrelevant to this Dev-only authorization, but
   restated so it is not forgotten before any future Staging/Production authorization.
4. **Staging's build-time gate is weaker than Production's** — same treatment: irrelevant to this
   Dev-only scope, named so it isn't lost before it matters.
5. **Manager's ViewModel-bypass architecture debt** (Phase 2) — does not block this authorization,
   but is real, documented debt worth closing before Manager's scope grows further.

---

## STOP

Mobile Release Authorization complete. No code was implemented, no build was produced, no device
action was taken, no git action was performed. Waiting for Team 3 to execute Phase 5's package
against exactly the scope defined above.
