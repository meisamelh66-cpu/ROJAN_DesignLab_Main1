# ARCHITECTURE_RULEBOOK_UPDATE_REPORT_v1

**Scope:** Documentation governance only. No Backend, Web, Mobile, Database, API, migration, or deployment code was touched — confirmed by `git status`, which shows changes confined to `docs/architecture/` plus this new report file.

**Status:** Draft changes written to the working tree, **uncommitted**. Awaiting Team 1 approval per the task's Safety Rule before any commit or push.

---

## 1. Current Rulebook State (before this update)

Location: `ROJAN_DesignLab_Main1/docs/architecture/` — the one governance doc tree in the ROJAN ecosystem whose structure and naming actually matches the taxonomy this task names (`PRODUCT_CONSTITUTION`, `DOMAIN_OWNERSHIP`, `FEATURE_PROCESS`, `TECHNICAL_STANDARDS`, `FUTURE_SCALE`, etc.). It is a lean, directive-style, mostly-Persian rule set — most files are 5–25 lines; only the three real ADRs (`ADR-002`, `ADR-003`, and its validation report) are prose-length. `ADR-001` itself is a 5-line stub that doesn't follow its own `ADR_TEMPLATE.md` (missing Context/Impact/Migration Plan sections).

The rulebook already covered, before this update:
- Backend as domain authority for Identity/Salon/Membership/Permission/Media/Booking/Customer Relationship (`ADR-001`, `DOMAIN_OWNERSHIP.md`, `ROJAN_PRODUCT_CONSTITUTION.md`)
- Per-application responsibility boundaries (`APPLICATION_RULES.md`)
- Permission always computed server-side (`PERMISSION_MATRIX.md`)
- Contract-first API discipline (`API_CONTRACT_RULES.md`)
- Media pipeline responsibilities, client vs. backend (`MEDIA_POLICY.md`, `ADR-002`)
- Pre-feature checklist (`FEATURE_PROCESS.md`)
- AI-agent obligations (`CLAUDE_RULES.md`)
- General technical principles including Observability (`TECHNICAL_STANDARDS.md`)
- A future-scale wishlist (`FUTURE_RULES.md`)

## 2. Files Reviewed

All 18 files under `docs/architecture/`, read in full:
```
00_START_HERE/README.md
01_PRODUCT_CONSTITUTION/ROJAN_PRODUCT_CONSTITUTION.md
02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md
03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md
04_USER_LIFECYCLE_SCENARIOS/USER_SCENARIOS.md
05_AUTHENTICATION_SECURITY/PERMISSION_MATRIX.md
06_API_CONTRACT_GOVERNANCE/API_CONTRACT_RULES.md
07_MEDIA_ARCHITECTURE/MEDIA_POLICY.md
08_DEVELOPMENT_GOVERNANCE/FEATURE_PROCESS.md
09_AI_AGENT_GOVERNANCE/CLAUDE_RULES.md
10_ARCHITECTURE_DECISIONS_ADR/ADR-001_BACKEND_SOURCE_OF_TRUTH.md
10_ARCHITECTURE_DECISIONS_ADR/ADR-002_MEDIA_UPLOAD_ARCHITECTURE.md
10_ARCHITECTURE_DECISIONS_ADR/ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md
10_ARCHITECTURE_DECISIONS_ADR/ADR-003_VALIDATION_REPORT.md
10_ARCHITECTURE_DECISIONS_ADR/ADR_TEMPLATE.md
11_TECHNICAL_STANDARDS/TECHNICAL_STANDARDS.md
12_FUTURE_SCALE/FUTURE_RULES.md
CLAUDE.md
```
Plus a scan of the ~30 root-level phase/report `.md` files in this repo for Phase 0–9 decision context (Reception Phase 0/1, System2 Authentication, First Salon, Salon Identity, RBAC, Customer CRM, Media Foundation).

## 3. Missing Rules (before this update)

| Rule | Status found |
|---|---|
| RULE 001 (Source of Truth) | Present, but **Calendar** and **Tenant Data** were not named as explicit domain rows — only implied via Salon/Booking |
| RULE 002 (Client Ownership) | "No independent business model in Client" existed; the specific "local storage may only be Cache/Projection/Temporary offline state" enumeration did **not** exist anywhere |
| RULE 003 (Calendar Authority) | Booking→Backend existed; **Desktop SQLite as Projection/Read-Model-only was never stated** |
| RULE 004 (Auth Data Separation) | **Completely absent** — no document separated Identity/Session/Cache/Client State, and nothing stated session expiration must not imply data loss |
| RULE 005 (Booking Reliability) | **Completely absent** — no idempotency/duplicate/conflict/retry/transaction-boundary rule existed anywhere |
| RULE 006 (Development Order) | Present but **in conflict** — see §6 |
| RULE 007 (Tenancy) | Implicit only (future goals mention "large organizations") — **no explicit statement** that today's tenant is Salon and Organization is future-only |
| RULE 008 (Cache Policy) | **Completely absent** as a named rule |
| RULE 009 (Release Governance) | **Completely absent** — no Owner/Approval/Rollback requirement existed for any component |
| RULE 010 (Scale Readiness) | Multi-tenant and Observability existed separately (`FUTURE_RULES.md`, `TECHNICAL_STANDARDS.md`); **Horizontal Scaling and Failure Recovery were never stated** |

## 4. Added Rules

- **RULE 008 (Cache Policy)** — new file `02_DOMAIN_ARCHITECTURE/CACHE_POLICY.md`
- **RULE 004 (Auth Data Separation)** — new file `05_AUTHENTICATION_SECURITY/AUTH_DATA_SEPARATION.md`
- **RULE 005 (Booking Reliability)** — new `10_ARCHITECTURE_DECISIONS_ADR/ADR-004_BOOKING_MUTATION_RELIABILITY.md`, following the existing ADR-002/ADR-003 structure (Status/Context/Decision/Impact/Migration Plan) rather than the thinner ADR-001 style
- **RULE 009 (Release Governance)** — new file `08_DEVELOPMENT_GOVERNANCE/RELEASE_GOVERNANCE.md`

## 5. Modified Documents

| File | Change | Rule closed |
|---|---|---|
| `02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md` | Added `Tenant Data` and `Calendar / Availability` rows; added a note that Calendar is part of the Booking domain, not independent | RULE 001 |
| `03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md` | Added Desktop SQLite = Projection/Read-Model-only note; added a new "Client Local Storage" section enumerating the only three permitted local-storage roles | RULE 002, RULE 003 |
| `00_START_HERE/README.md` | Corrected the canonical development sequence and added a resolution note (see §6) | RULE 006 |
| `01_PRODUCT_CONSTITUTION/ROJAN_PRODUCT_CONSTITUTION.md` | Added an explicit Tenancy section: today's tenant is Salon; Organization is Future Enterprise Architecture only | RULE 007 |
| `12_FUTURE_SCALE/FUTURE_RULES.md` | Added a "Scale Readiness" section: Horizontal Scaling and Failure Recovery stated explicitly; Observability cross-referenced rather than duplicated | RULE 010 |

Every new/modified file cross-references the others it depends on (e.g., `CACHE_POLICY.md` ↔ `APPLICATION_RULES.md`'s local-storage section ↔ `AUTH_DATA_SEPARATION.md`), so the rulebook stays internally navigable rather than becoming a flat pile of disconnected rules.

## 6. Architecture Conflicts Found

1. **RULE 006 sequence conflict (real, pre-existing, now resolved in docs).** `README.md` previously stated the canonical order as `Architecture → Domain → Contract → Implementation → Validation → Release`. The newly mandated RULE 006 is `Architecture → Contract → Implementation → Migration → Validation → Release` — a different 6th element (`Domain` vs. `Migration`) in a different position. These are not the same sequence and had been silently coexisting with `FEATURE_PROCESS.md`'s own, third variant (checklist including "Migration Impact" *before* Implementation, then "Implementation → Testing → Release" with no explicit Validation step). **Resolution applied:** `README.md` now states the mandated order exactly, with a note explaining Domain Ownership is folded into the Architecture phase (not a separate step) and reconciling the "Migration Impact assessed early / Migration executed late" distinction with `FEATURE_PROCESS.md`. `FEATURE_PROCESS.md` itself was left unmodified since its checklist-vs-sequence framing doesn't literally contradict the corrected README once the note is read — flagging this as worth a follow-up pass if Team 1 wants the two documents merged into one canonical statement rather than cross-referenced.

2. **ADR-001 doesn't follow the rulebook's own ADR template.** `ADR_TEMPLATE.md` requires Context/Decision/Reason/Impact/Migration Plan; `ADR-001` has only Decision + Reason (5 lines total), while `ADR-002`/`ADR-003` are fully fleshed out. Not fixed in this pass (out of the 10 mandated rules' scope, and rewriting another team's ADR without being asked risks altering intent) — flagged as a recommendation in §7 instead.

3. **A real, previously-discovered conflict outside this repo, worth flagging here since RULE 001 depends on it holding true ecosystem-wide:** earlier work this session found that `ROJAN_Web`'s own `docs/architecture.md` describes a *different*, stale "platform-core" backend (a modular-monolith Kotlin codebase under a `com.rojan.platform` package) that is **not** the actual production backend — the real one is a separate repository (`ROJAN_Backend`, package `ai.rojan.backend`), confirmed directly by reading its source and by live production inspection. `ROJAN_Web`'s own `ROJAN_Final_Completion_Report_v2.md` already documents this discrepancy. This rulebook (in `ROJAN_DesignLab_Main1`) correctly names "Backend" as sole authority without specifying *which* backend codebase — which is fine within this repo, but the ecosystem currently has two documents both claiming to describe "the" backend architecture, only one of which is real. This is outside this task's scope to fix (it lives in a different repository), but is exactly the kind of "architecture conflict" RULE 001 exists to prevent, and is flagged here for visibility.

## 7. Recommended Governance Improvements

- **Bring `ADR-001` up to the template standard** (Context/Impact/Migration Plan) — it's the foundational ADR for RULE 001 and is currently the thinnest document in the whole tree.
- **Reconcile `README.md`'s RULE 006 sequence with `FEATURE_PROCESS.md` directly**, rather than via a cross-reference note — one document should be the single source for the development sequence, with the other pointing to it rather than restating a variant.
- **Retire or explicitly mark `ROJAN_Web/docs/architecture.md` as historical/superseded**, since it currently documents a backend that isn't the one in production — this is a cross-repository action outside this task's scope, but worth a dedicated follow-up task.
- **Consider a lightweight "Rule Index"** (a single table mapping RULE 001–010 → file path) at `00_START_HERE/README.md`, since the rulebook has grown from ~13 files to 18 and a newcomer currently has to read every folder to find which rule lives where.
- **Version the rulebook itself** (e.g., a `CHANGELOG.md` under `docs/architecture/`) so future audits like this one can diff intent over time instead of re-deriving it from file contents alone.

---

## Files Changed (for commit review)

```
 M  docs/architecture/00_START_HERE/README.md
 M  docs/architecture/01_PRODUCT_CONSTITUTION/ROJAN_PRODUCT_CONSTITUTION.md
 M  docs/architecture/02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md
 M  docs/architecture/03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md
 M  docs/architecture/12_FUTURE_SCALE/FUTURE_RULES.md
??  docs/architecture/02_DOMAIN_ARCHITECTURE/CACHE_POLICY.md
??  docs/architecture/05_AUTHENTICATION_SECURITY/AUTH_DATA_SEPARATION.md
??  docs/architecture/08_DEVELOPMENT_GOVERNANCE/RELEASE_GOVERNANCE.md
??  docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/ADR-004_BOOKING_MUTATION_RELIABILITY.md
??  ARCHITECTURE_RULEBOOK_UPDATE_REPORT_v1.md   (this file)
```
9 architecture files touched (5 modified, 4 created) + this report. No code, no database, no API, no migration, no deployment file touched anywhere in the repository — confirmed by `git status` showing zero changes outside `docs/architecture/` and this report.

**No commit made. No push made. Awaiting Team 1 approval.**
