# Changelog — ROJAN Ecosystem Governance

Format follows [Keep a Changelog](https://keepachangelog.com/). This set is
versioned as a whole:

- **MAJOR** — a rule removed, weakened, or a `[NON-NEGOTIABLE]` added or changed.
- **MINOR** — a rule added or clarified without weakening anything.

Each repository declares, in its Tier-1 entry file, which version it has adopted.

## [2.0] — DRAFT (materialized PASS G4)

### Added — first materialization of Root Governance

- `00_INDEX_AND_PRECEDENCE.md` — scope, tier model, precedence, conflict
  resolution, canonical development order, discovery procedure, version control.
- `01_PRODUCT_CONSTITUTION.md` — backend as source of truth; product consumer
  roles; domain ownership map (with V2 additions: Tenant data, Calendar/
  Availability inside Booking); tenancy; five-year direction; product values.
- `02_BACKEND_AUTHORITY_RULES.md` — canonical backend = `ROJAN_Backend`;
  business authority computed once in the backend; closed contract surface;
  clean architecture; change control; mutation reliability (5 properties,
  generalized from V2 ADR-004); observability.
- `03_API_CONTRACT_RULES.md` — contract-first; authoritative contract document;
  URI versioning; no contract change without review; backward compatibility;
  canonical cross-cutting conventions.
- `04_DATABASE_OWNERSHIP.md` — one database one owner; migration-owned schema;
  migration safety; no update destroys business data; client-side storage rules
  (from V2 CACHE_POLICY).
- `05_AUTHENTICATION_AND_AUTHORIZATION_RULES.md` — auth backend-owned;
  permission computed only in the backend; the four concepts never conflated
  (from V2 AUTH_DATA_SEPARATION); failure scenarios backend-managed.
- `06_CLIENT_RESPONSIBILITY_BOUNDARY.md` — client role stated positively; what a
  client must never do; what is entirely the client's own; shared client
  obligations.
- `07_RELEASE_POLICY.md` — release components; Owner/Approval/Rollback per
  component (from V2 RELEASE_GOVERNANCE); release invariants; validation gate;
  deploy mechanism; per-component mechanics local.
- `08_UPDATE_POLICY.md` — net-new. Operational continuity; data survives
  updates; compatibility before rollout; normal vs critical updates; controlled
  installation (forward-looking); download route stability.
- `09_GIT_DISCIPLINE.md` — Conventional Commits; scoped commits; commit is not
  approval; protected mainline; trunk-based default with a documented
  release-train exception mechanism; tags; working-tree hygiene.
- `10_NO_FAKE_DATA_POLICY.md` — net-new written rule for an existing practice.
  No silent fallback to fabricated data in production paths.
- `11_AI_AGENT_GOVERNANCE.md` — read/report/assess before coding; forbidden and
  allowed action lists; system-boundary awareness; honesty and scope;
  per-repository entry file requirement.
- `TRACEABILITY_MATRIX.md` — every rule mapped to its pre-existing source.
- `UNRESOLVED_CONFLICTS.md` — C-1 through C-12, carried forward, not resolved.

### Notes

- Content baseline is the V2 governance draft
  (`ROJAN_DesignLab @ origin/feature/architecture-constitution-v2`), promoted out
  of the Android repository and de-Androidified to ecosystem-level wording.
- Not yet adopted by any repository. Adoption is PASS G5.
