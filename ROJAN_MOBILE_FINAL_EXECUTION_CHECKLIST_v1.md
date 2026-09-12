# ROJAN AI — TEAM 1 — MOBILE RELEASE FINAL READINESS REVIEW & EXECUTION CHECKLIST v1

STATUS: FINAL PRE-EXECUTION REVIEW. No code was implemented, no build was produced, no device
action was taken, no git action was performed.
ROLE: Team 1 — Architecture Authority
BASED ON: `ROJAN_MOBILE_RELEASE_AUTHORIZATION_v1.md` (the standing authorization this document
does not re-litigate), `ROJAN_Independent_Release_Readiness_Audit_v1.md` (2026-08-15),
`ROJAN_Android_Pilot_Session_Handoff_Report_v4.md` (2026-08-21).

**Fresh state check, this document:** `feature/android-first-salon-pilot` @
`0afe738e62a0f47cce45f64e2c4e2d1e755fde28`, unchanged since the prior authorization — same one
uncommitted file (`BackendAppointmentRepository.kt`, the Booking Pagination Fix), same 69 commits
ahead of `origin/main`, 0 behind. **No Team 3 execution has occurred since the prior document —
this review confirms the same baseline, not a new one, and does not invent progress that hasn't
happened.**

---

## PHASE 1 — Mobile Release Risk Review

### Customer Flavor

| Area | Readiness | Risk |
|---|---|---|
| Authentication readiness | 🟢 Low risk — real, server-side-only OTP flow, no client-side auth logic, no hardcoded bypass (Independent Audit §3); architecturally identical to Manager's already-device-proven auth flow, but **never itself run on a physical device** | The code path is proven safe by audit; it is not yet proven *working* by a real install |
| Booking flow readiness | 🔴 High risk — the entire booking-flow ViewModel set is unit-untested (Independent Audit §5) and has never been exercised on a physical device by any report on record | This is the single least-proven surface of either app in this entire review |
| Backend connectivity | 🟢 Low risk — 15/15 real repository interfaces implemented, `NetworkConfig.kt` fails loudly on a blank base URL (Independent Audit §2) | Connectivity code is sound; only real-device exercise is missing |
| Device validation requirements | Full first-pass required — Installation, Launch, Auth, API Connection, Session, and Core (Booking) Flow all remain unperformed for this flavor, per the prior Authorization's own Phase 4 table |

### Manager Flavor

| Area | Readiness | Risk |
|---|---|---|
| Login readiness | 🟢 Proven — real OTP, real device, real multi-salon account (Handoff v4 §2) | None beyond re-confirming it still holds at execution time, per standard practice, not because of any reason to doubt it |
| Dashboard readiness | 🟢 Proven — correct salon identity, live-verified against a real `GET /api/v1/salons/{salonId}` response (Handoff v4 §3) | None |
| Booking dependency risk | 🔴 High risk — the real, named, still-open blocker: a client page-size request (`size=200`) exceeds a confirmed real backend limit (`MAX_SIZE=100`), producing a 400 on every booking-list attempt to date; the fix exists in the working tree, tested and built, **but is not committed** | Booking Detail/Confirm/Complete cannot be exercised until this is resolved — carried forward unchanged from the prior Authorization, not newly discovered here |
| Schedule/Shift compatibility | 🟡 Partial — Calendar screen loads and the specialist filter works, both physical-device-verified (Handoff v4 §4); this is availability *viewing* only — no report claims schedule/shift *authoring* was ever exercised on Manager, and the prior Authorization already flagged this distinction | Do not treat "Calendar loads" as equivalent to "Schedule/Shift is validated" — they are different claims |

---

## PHASE 2 — Blocker Register

| # | Blocker | Category | Severity | Resolution requirement |
|---|---|---|---|---|
| 1 | Manager's Booking Pagination Fix is implemented, tested, and built, but **not committed** | Uncommitted required fix | **P0** | Must be committed (with explicit approval sought first, per this project's own standing git-action discipline, unchanged by this document) before any Booking-flow validation attempt on Manager — validation against the current, unfixed committed state would simply reproduce the known 400 |
| 2 | Whether real bookings exist at all for the pilot salon remains **unconfirmed** — every prior check failed with the 400 before ever reaching a genuine populated-vs-empty answer | Missing validation | **P0** | Cannot be resolved by this document; the very first re-check after Blocker 1 is fixed is what answers this — if no booking exists, report BLOCKED again rather than fabricating one without explicit approval (restated from Handoff v4 §5, still binding) |
| 3 | Customer flavor has **zero physical-device validation on record**, for any flow, ever | Missing validation | **P0** | A full first-pass device install and walkthrough is required — this is not a re-check of known-good state, it is the first check of any kind |
| 4 | Customer's entire booking-flow ViewModel set is unit-untested | Missing validation | **P1** | Device-level validation (Phase 3) is a real, valuable check but does not substitute for this gap — restated from the Independent Audit, not newly elevated here |
| 5 | Manager's 13-screen `ManagerRepositories` global-singleton architecture debt | Build configuration risk (indirect — affects testability, not correctness) | **P2** | Does not block this device-validation pass; real, documented, worth closing before Manager's scope grows further (Independent Audit §7) |
| 6 | R8/ProGuard disabled for release builds | Build configuration risk | **P2** | Irrelevant to the Dev-only builds this checklist authorizes; must be resolved before any future Staging/Production authorization, not before this one |
| 7 | `StagingRelease` lacks the build-time URL hard-gate `ProductionRelease` already has | Build configuration risk | **P2** | Same treatment as #6 — out of this Dev-only checklist's own scope, named so it isn't lost |
| 8 | No release signing keystore present in this environment | Build configuration risk | **P2** for this checklist (Dev builds are unsigned/debug-signed by design); would be **P0** the moment any Staging/Production build is ever authorized | Provision through a secure channel before that future authorization, not before this one |
| 9 | 69 commits sit unmerged on this branch relative to `origin/main` | Backend constraint — *not applicable, mis-classified if placed there*; correctly a release-process risk | **P1** | Not a prerequisite for device-testing this branch directly (this checklist's own scope), but a real, separate decision required before any of this work reaches a release train — restated, not resolved, here |

**No new blocker was found by this review** — every item above is carried forward from the two
source reports and the prior Authorization, re-classified into this document's own P0/P1/P2
structure, not freshly invented, and re-confirmed still accurate against the fresh state check
performed at the top of this document.

---

## PHASE 3 — Team 3 Execution Package

### Required artifacts

| Artifact | Requirement |
|---|---|
| Customer APK | `assembleCustomerDevDebug` — matches the prior Authorization's own Phase 3 scope exactly; no Staging/Production build is in scope |
| Manager APK | `assembleManagerDevDebug`, built **only after Blocker #1 is resolved** (committed) — a build against the current, uncommitted-fix-absent tree would not reflect the state this checklist intends to validate |
| Version information | Whatever `app/build.gradle.kts`'s existing versionCode/versionName convention already produces for a Dev build — not redefined by this document, consistent with the prior Authorization's own Phase 3 treatment |
| Test evidence | `testCustomerDevDebugUnitTest` and `testManagerDevDebugUnitTest` results, both run fresh at execution time — not the Independent Audit's now 11-day-old numbers, and not Handoff v4's own point-in-time 160/158/2 result, both cited as *prior* evidence, neither as *current* proof |
| Installation report | Per app, per Phase 4 device-test-gate row (from the prior Authorization), with real evidence — device model, real account/phone number used, real logcat/network evidence for API Connection — matching the evidentiary bar Handoff v4 already set |

### PASS / FAIL criteria

**PASS, for a given app, requires all of the following — no partial credit:**
1. Fresh unit test run for that flavor: 0 new failures relative to the most recent known-good
   baseline for that flavor (Manager: 158/160, 2 pre-existing network-dependent failures already
   understood and excluded, per Handoff v4 — a *third* failure or more would fail this criterion;
   Customer: no prior baseline exists, so this is the first-ever run establishing one, and any
   failure must be triaged, not silently waived by comparison to a nonexistent prior number).
2. `assemble<Flavor>DevDebug` — BUILD SUCCESSFUL, no errors.
3. Every row of the Device Test Gate table (prior Authorization, Phase 4) — PASS, with real,
   citable evidence per row, not an unsupported checkmark.
4. For Manager specifically: Blocker #1 committed and reflected in the tested/built artifact
   before Booking-flow rows are attempted; if Blocker #2 resolves to "no bookings exist," the
   Booking Detail/Confirm/Complete rows are reported **BLOCKED**, not **FAIL** — a real, structural
   distinction this checklist preserves from Handoff v4's own established language, since
   "no test data available" is not the same finding as "the feature is broken."
5. For Customer specifically: since this is a first-ever pass, **BLOCKED is an acceptable, honest
   outcome for any row this checklist's own P0 Blocker #3 anticipated might not be immediately
   achievable** (e.g., if no real salon/service/specialist data is reachable from a fresh Customer
   session) — but **FAIL** (a real, reproduced defect) and **BLOCKED** (missing data/environment)
   must be reported as the distinct things they are, never conflated.

**FAIL, for a given app, is any of:** a build failure, a new unit test regression not already
understood and excluded, a Device Test Gate row that reproduces a real defect (not merely lacks
data), or any row silently skipped rather than explicitly marked BLOCKED with a stated reason.

**Overall release-candidate status is not "PASS" or "FAIL" as a single verdict** — per this
document's own Phase 1/2 findings, Customer and Manager are at genuinely different readiness
tiers today, and this checklist deliberately reports each app's own result independently rather
than forcing one combined verdict that would obscure that real difference.

---

## STOP

Final Readiness Review and Execution Checklist complete. No code was implemented, no build was
produced, no device action was taken, no git action was performed. Waiting for Team 3 to execute
against exactly this checklist.
