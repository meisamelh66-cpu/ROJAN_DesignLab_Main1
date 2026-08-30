# 00 — Governance Index & Precedence

**Set:** ROJAN Ecosystem Governance v2.0
**Status:** MATERIALIZED (PASS G4) — not yet adopted by any repository.

Rules tagged **[NON-NEGOTIABLE]** may never be weakened by a lower tier (project
or module), under any framing, without a documented ecosystem-owner exception.
All other rules may be *strengthened* freely by a lower tier and *weakened* only
via a dated, attributed exception block.

---

## Purpose

This collection is the official governance reference for the **ROJAN ecosystem**
— all products that consume the ROJAN backend: the Website, the Android
applications (Customer, Manager, Reception), the Desktop application, and every
future product (Specialist, Accountant, Inventory, AI services). Its purpose is
to prevent architecture conflict, parallel models, incompatible contracts,
undocumented decisions, and unsafe releases across repositories that are
developed and deployed independently.

## Scope and reach

The ROJAN ecosystem is **four physically independent Git repositories**, not a
monorepo:

| Repository | Role for governance |
|---|---|
| `ROJAN_Backend` | The canonical backend. Sole authority for every business domain. |
| `ROJAN_DesignLab` | Android client (Customer / Manager / Reception flavors). |
| `ROJAN_Desktop` | Desktop client. |
| `ROJAN_Web` | The Website. Its `platform-core/` and `modules/` subtree is a **dormant, non-canonical** backend rebuild — advisory design input only, not authority, and no client may build against it. |

Root Governance applies to all four. Each repository carries a **vendored copy**
of this set plus a local entry file (`CLAUDE.md` / `AGENTS.md`) pointing to it.
Independence of repositories does not exempt a repository from Root Governance.

## Tier model

```
TIER 0   ROOT GOVERNANCE            these 11 documents — ecosystem-wide, mandatory
   |
TIER 1   PROJECT RULES              one repo's own CLAUDE.md / CONTRIBUTING.md / docs/
   |                                 (technology-specific; may add constraints)
   |
TIER 2   MODULE RULES               narrow local constraints (frozen design baselines,
                                     per-feature ADRs, per-phase docs)
```

## Precedence

Two axes:

1. **"May a lower tier weaken this?"** — Root **[NON-NEGOTIABLE]** > Root >
   Project > Module. A lower tier can never relax a higher-tier rule tagged
   NON-NEGOTIABLE.
2. **"How, concretely, is this done in this codebase?"** — Module > Project >
   Root (most specific wins on *mechanics only*, never on the *intent* of a Root
   rule).

## Conflict resolution

- A lower-tier rule that contradicts a Root **[NON-NEGOTIABLE]** rule: the
  lower-tier rule loses, and the contradiction must be **stopped and escalated
  to the governance owner** before any code is written against it.
- A lower-tier rule that contradicts a non-NON-NEGOTIABLE Root rule: permitted
  **only** with an explicit exception block in the Project/Module doc:

  ```
  > EXCEPTION to Root 09 section 3 (trunk-based).
  > Approved by: <name/role>   Date: <YYYY-MM-DD>
  > Reason: <why this repo needs a different mechanism>
  > Review trigger: <the condition under which this exception is revisited>
  ```
- Between two Root rules: the interpretation with the stronger production-safety,
  security, or data-safety guarantee prevails unless one is demonstrably
  obsolete.

## May a child strengthen a parent rule?

**Yes, always, without approval.** Example: an app-level release gate stricter
than Root 07 is encouraged.

## May a child weaken a parent rule?

**Never for [NON-NEGOTIABLE].** For other rules, only via the exception block
above.

## Development order (canonical)

```
Architecture  ->  Contract  ->  Implementation  ->  Migration check  ->  Validation  ->  Release
```

Every feature runs the **Feature Process check** before implementation:

1. Domain Owner
2. Data Ownership
3. API Impact
4. Permission Impact
5. Migration Impact
6. Failure Scenarios

*(This reconciles the two divergent orderings in the predecessor rulebook — the
`README` sequence and the `FEATURE_PROCESS` checklist — into one statement.
Migration-impact assessment is step 5 of the check **and** a named stage of the
development order.)*

## How an agent or contributor discovers applicable rules

1. Read this index.
2. Read all eleven Root documents.
3. Read the repository's own Tier-1 entry file and `docs/`.
4. Read any Tier-2 module doc covering the files being touched.
5. On any conflict between tiers: **stop and report** (see Root 11).

## Version and change control

- This set is versioned as a whole: `ROJAN Ecosystem Governance vX.Y`.
- **MAJOR** bump = a rule removed, weakened, or a NON-NEGOTIABLE added/changed.
- **MINOR** bump = a rule added or clarified without weakening anything.
- Every change ships with an entry in this set's own `CHANGELOG.md`.
- Each repository declares, in its Tier-1 entry file, which Governance version it
  has adopted.

## Document set

| # | Document |
|---|---|
| 00 | Governance Index & Precedence (this file) |
| 01 | Product Constitution |
| 02 | Backend Authority Rules |
| 03 | API Contract Rules |
| 04 | Database Ownership |
| 05 | Authentication & Authorization Rules |
| 06 | Client Responsibility Boundary |
| 07 | Release Policy |
| 08 | Update Policy |
| 09 | Git Discipline |
| 10 | No Fake Data Policy |
| 11 | AI Agent Governance |
| — | `TRACEABILITY_MATRIX.md` |
| — | `UNRESOLVED_CONFLICTS.md` |
