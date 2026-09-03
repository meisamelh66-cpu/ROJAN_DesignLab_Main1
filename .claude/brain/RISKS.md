# RISKS — Risk & Technical Debt Ledger

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: point-in-time risk ledger, seeded from the 2026-09-03 repository audit and the repo's own 2026-08-15 independent audit. IDs are stable across regenerations — re-verify status before assuming an item marked open is still open, or vice versa.

| ID | Title | Severity | First observed | Status |
|---|---|---|---|---|
| R-01 | Thin test coverage — 22 unit test files / 4 instrumented vs. 415 source files; no tests for booking lifecycle, token refresh, or most repository implementations | Critical | 2026-08-15 | open |
| R-02 | Manager `ManagerRepositories` singleton bypasses ViewModel layer in 13 screens (27 call sites) | High | 2026-08-15 | open — partially addressed, see ARCHITECTURE.md |
| R-03 | Reception's access gate has no distinct role — any `MANAGER`-role account passes; Manager and Reception aren't access-isolated | High | prior audit | open |
| R-04 | Two feature branches significantly diverged from `main`: `feature/android-reception-app`, `feature/manager-backend-integration` | High | prior audit | open |
| R-05 | R8/ProGuard disabled for release builds despite keep rules already scaffolded in `proguard-rules.pro` | Medium | prior audit | open |
| R-06 | `RojanNavGraph.kt` is 1,176 lines — largest file in the codebase by a wide margin (next is 736) | Medium | 2026-09-03 | open |
| R-07 | Repo hygiene: `ROJAN_AI_Production_Readiness_Report.md` self-flagged stale and unarchived; `screens/customer/theme/` self-flagged dead and undeleted; ~25 root-level historical reports unfiled into `docs/` | Medium | 2026-09-03 | open |
| R-08 | Staging build lacks the build-time API-URL gate that Production has | Medium | prior audit | open |

## Not tracked here as risk

The current uncommitted working-tree changes are functionally coherent (verified against real
API/repository signatures) — see [SESSION.md](SESSION.md) for what they are and why they're not
listed as a risk item themselves, only as context on R-01/R-02.

Source: 2026-09-03 repository audit + `ROJAN_Independent_Release_Readiness_Audit_v1.md`
(2026-08-15). Regenerate via a fresh audit pass rather than hand-editing status without checking.
