# Traceability Matrix — ROJAN Ecosystem Governance v2.0

**Status:** MATERIALIZED (PASS G4)

Every Root rule traces to at least one pre-existing source. Nothing in this set
is invented. Source abbreviations:

- **V1** = `ROJAN_DesignLab/docs/architecture/` on `feature/android-first-salon-pilot`
- **V2** = `ROJAN_DesignLab @ origin/feature/architecture-constitution-v2` (governance draft)
- **Meta-Scale** = `C:\AndroidProjects\ROJAN_META_SCALE_ARCHITECTURE_DIRECTION_v1.md`
- **G1 / G2 / G3** = the corresponding governance-pass reports

| Root doc / rule | Primary source(s) | Type |
|---|---|---|
| **00** Development order (reconciled) | V1 `00_START_HERE/README.md`; V1 `08_DEVELOPMENT_GOVERNANCE/FEATURE_PROCESS.md`; prior context report's flagged mismatch | Reconciliation of two V1 statements |
| **00** Tier / precedence / exception model | G1 section K; V1 `docs/architecture/CLAUDE.md` ("if conflict: STOP and report"); `ROJAN_DesignLab/CLAUDE.md` (Automatic vs Confirmation-Required) | New synthesis over existing intent |
| **00** Scope = 4 independent repos | G1 section B; `ROJAN_Backend/README.md`, `ROJAN_Desktop/README.md` (independence); Meta-Scale (adoption gap) | Discovery finding |
| **01** 1.1 Backend is source of truth | V1 `01_PRODUCT_CONSTITUTION` + `ADR-001_BACKEND_SOURCE_OF_TRUTH`; Meta-Scale section 1 | V1, unchanged in intent |
| **01** 1.2 Product consumer roles | V1 `03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md` + `04_USER_LIFECYCLE_SCENARIOS` | V1, de-Androidified |
| **01** 1.3 Domain ownership map (+ Tenant, + Calendar note) | V2 `02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md` additions (Tenant Data, Calendar/Availability rows) | V2 draft |
| **01** 1.4 Tenancy (Salon; Org as 2nd dimension) | V2 `ROJAN_PRODUCT_CONSTITUTION`; Meta-Scale section 5; `ROJAN_Web/docs/adr/0002`, `0019` | V2 + Meta-Scale |
| **01** 1.5 Five-year direction; AI optional | V1+V2 `12_FUTURE_SCALE/FUTURE_RULES.md`; `ROJAN_Web/docs/adr/0012` | V1/V2 |
| **01** 1.6 Persian-first, premium material | `ROJAN_DesignLab/CLAUDE.md` (RTL Persian-first; Shared Premium Glass) | Existing, elevated to value |
| **02** 2.1 Canonical backend = ROJAN_Backend | PASS G2 decision; `ROJAN_Backend/DEPLOYMENT.md` (live); G1 section F conflict 1 | G2 |
| **02** 2.2 Business authority computed once | Meta-Scale "Business Authority ownership"; V2 `ADR-004` generalized | Meta-Scale + V2 |
| **02** 2.3 Closed contract surface | `ROJAN_Backend/README.md` ("REST/JSON purely"); V1 `06` | Existing |
| **02** 2.4 Clean architecture enforced | `ROJAN_Backend/README.md`; `architecture-tests` (Web); `ArchitectureRulesTest.kt` (Android); V1 `11` | Existing x4 |
| **02** 2.5 Change control / frozen baseline | `ROJAN_Backend/README.md` ("same convention as ROJAN_DesignLab's frozen baselines"); `ROJAN_DesignLab/CLAUDE.md` | Existing (explicit cross-ref) |
| **02** 2.6 Mutation reliability (5 properties) | V2 `10_.../ADR-004_BOOKING_MUTATION_RELIABILITY.md`; Meta-Scale section 4; `ROJAN_Backend/API_CONTRACT.md` (idempotency) | V2 draft, generalized |
| **02** 2.7 Observability | V1 `11` ("Observability"); Meta-Scale section 7; `ROJAN_Backend` Prometheus endpoint | V1 + Meta-Scale |
| **03** 3.1 Contract-first | V1 `06_API_CONTRACT_GOVERNANCE/API_CONTRACT_RULES.md` | V1 |
| **03** 3.2 Authoritative contract doc | `ROJAN_Backend/API_CONTRACT.md`; `ROJAN_Web` completion report (mirrors it); `ROJAN_Desktop` contract files (mirror it) | Existing |
| **03** 3.3 URI versioning, breaking -> new prefix | `ROJAN_Backend/API_CONTRACT.md`; `ROJAN_Web/docs/adr/0016` | Existing (identical x2) |
| **03** 3.4 No contract change without review | V1 `06`; `ROJAN_DesignLab/CLAUDE.md`; auto-memory ("verify backend contracts") | Existing x3 |
| **03** 3.5 Backward compatibility | `ROJAN_Backend/API_CONTRACT.md` (error-shape decision); V1 `11` | Existing |
| **03** 3.6 Error shape / 404-not-403 / pagination max / idempotency / media | `ROJAN_Backend/API_CONTRACT.md`; V1 `07_MEDIA_ARCHITECTURE/MEDIA_POLICY.md` | Existing |
| **04** 4.1 One database, one owner | V1 `02` + `ADR-001`; Meta-Scale section 2 | V1 + Meta-Scale |
| **04** 4.2 Migration-owned schema, validate mode | `ROJAN_Backend/DEPLOYMENT.md` (Flyway, `ddl-auto: validate`); `ROJAN_Web/docs/adr/0008` | Existing x2 |
| **04** 4.3 Migration safety / rollback = code only | `ROJAN_Backend/DEPLOYMENT.md` section 8; V1 `08` (Migration Impact) | Existing |
| **04** 4.4 No update destroys data | `ROJAN_Backend/DEPLOYMENT.md` (backup before deploy); G1 section I gap | Existing practice -> written rule |
| **04** 4.5 Client projection / cache rules | V2 `02_.../CACHE_POLICY.md`; Meta-Scale section 2; `ROJAN_DATA_ACCESS_AND_PROJECTION_STRATEGY_v1.md` | V2 draft |
| **05** 5.1 Auth backend-owned, no bypass | V1 `05_.../PERMISSION_MATRIX.md`; `ROJAN_Independent_Release_Readiness_Audit_v1.md` section 3 (no client-side auth) | V1 + audit |
| **05** 5.2 Permission computed only in backend | V1 `05` ("Client is not allowed to compute Permission"); `ROJAN_Desktop` commit "remove client booking conflict authority" | V1 (+ remediation in flight) |
| **05** 5.3 Four concepts never conflated | V2 `05_.../AUTH_DATA_SEPARATION.md` | V2 draft |
| **05** 5.4 Failure scenarios backend-managed | V1 `04_USER_LIFECYCLE_SCENARIOS/USER_SCENARIOS.md` ("Failure Scenarios") | V1 |
| **06** 6.1 Client role stated positively | Meta-Scale "Client responsibility boundaries"; V1 `03` | Meta-Scale + V1 |
| **06** 6.2 What a client must never do | V1 `03` + `09_CLAUDE_RULES`; Meta-Scale section 1 | Existing |
| **06** 6.3 Client owns its own stack | `ROJAN_Backend/README.md` + `ROJAN_Desktop/README.md` (independence); G1 section H | Discovery synthesis |
| **06** 6.4 Shared client obligations | `ROJAN_DesignLab` RQG; `ROJAN_Web/CONTRIBUTING.md`; `ROJAN_Desktop/docs/standards` | Existing x3 |
| **07** 7.1-7.2 Components + Owner/Approval/Rollback | V2 `08_.../RELEASE_GOVERNANCE.md` | V2 draft |
| **07** 7.3 Release invariants (version/notes/metadata/integrity) | `ROJAN_Web/apps/website/lib/downloads/release-registry.ts`; `ROJAN_Desktop/docs/standards/versioning.md`; `CHANGELOG.md` x2 (Keep a Changelog); G1 section I | Existing fragments -> consolidated |
| **07** 7.4 Validation gate (PASS/FAIL/BLOCKED) | `ROJAN_MOBILE_FINAL_EXECUTION_CHECKLIST_v1.md`; `ROJAN_Desktop/docs/gates` (binary PASS/BLOCKED); `ROJAN_Web/CONTRIBUTING.md` (4 gradle checks) | Existing x3 |
| **07** 7.5 Deploy mechanism tested first | V2 `08_.../RELEASE_GOVERNANCE.md`; `ROJAN_Desktop/phase-09` ("first real run is the integration test") | V2 + existing |
| **07** 7.6 Per-component mechanics local | `ROJAN_Desktop/release.yml`; `ROJAN_Web/production-release.md`; `ROJAN_Backend` release branches; G1 section F conflict 5 | Discovery |
| **08** 8.1 Operational continuity | `ROJAN_Backend/DEPLOYMENT.md` (graceful shutdown); `ROJAN_Web/production-release.md` (health-gated) | Existing -> generalized |
| **08** 8.2 Data independent of binary; preserved | `ROJAN_Backend/DEPLOYMENT.md` (bind-mounted volumes); `ROJAN_Android_Pilot_Session_Handoff_Report_v4.md` (reinstall preserves session); V2 `AUTH_DATA_SEPARATION.md` | Existing fragments |
| **08** 8.3 Compatibility before rollout | V1 `11` (Backward Compatibility); `release-registry.ts` (`minimumSupportedVersion`) | Existing |
| **08** 8.4 Normal vs critical updates | G1 section I "MISSING" — net-new, modeled on standard practice | New (flagged as gap in G1) |
| **08** 8.5 Controlled installation | G1 section I "MISSING"; `ROJAN_Desktop/phase-09` (ZIP-only, "revisit") | New (forward-looking) |
| **08** 8.6 Download route stability | `ROJAN_Web/apps/website/.../route.ts` + `release-registry.ts` (dual-gate) | Existing |
| **09** 9.1 Conventional Commits | `ROJAN_Web/CONTRIBUTING.md`; actual history in all 4 repos; G1 section F conflict 4 | Existing (majority practice) |
| **09** 9.2 Scoped commits | auto-memory `feedback_android_phase_execution_policy.md`; `ROJAN_DesignLab/CLAUDE.md` ("surface out-of-scope fixes") | Existing |
| **09** 9.3 Commit is not approval; no fabrication | `ROJAN_DesignLab/CLAUDE.md` ("always ask before commit/push"); auto-memory `feedback_subagent_scope_and_trust.md` | Existing |
| **09** 9.4 Protected mainline + arch-test gate | `ROJAN_Desktop/docs/standards/branch-strategy.md`; `ROJAN_Web/CONTRIBUTING.md` | Existing x2 |
| **09** 9.5 Trunk-based default + release-train exception | `ROJAN_Desktop/branch-strategy.md` + `ROJAN_Web/CONTRIBUTING.md` (trunk); `ROJAN_Backend` `release/vX.Y.Z` (train); G1 section F conflict 3 | Existing + conflict resolution |
| **09** 9.6 Tags only on shippable branches | `ROJAN_Desktop/docs/standards/versioning.md` section 6; `branch-strategy.md` section 4 | Existing |
| **09** 9.7 Working-tree hygiene | `ROJAN_DesignLab/CLAUDE.md` ("Do not modify unrelated files"); every prior pass's "do not touch dirty files" | Existing |
| **10** 10.1-10.5 No fake data | `ROJAN_DesignLab/CLAUDE.md` (Reception "zero mock data"); `ROJAN_Backend` `NetworkConfig` fail-loud; `ROJAN_Web` completion report ("won't invent data"); `ROJAN_Desktop` `FakeOrganizationRepository` flagged; auto-memory; G1 section E item 10 ("never written as a rule") | Existing practice x4 -> written rule |
| **11** 11.1-11.3 Agent read/report/assess; forbidden/allowed lists | V1 `09_AI_AGENT_GOVERNANCE/CLAUDE_RULES.md` + `docs/architecture/CLAUDE.md`; `ROJAN_DesignLab/CLAUDE.md` (Automatic Actions / Confirmation Required) | V1 + existing |
| **11** 11.4 System boundary | `ROJAN_DesignLab/CLAUDE.md` ("System 1 / System 2 boundary") | Existing, de-Androidified |
| **11** 11.5 Honesty and scope | auto-memory (both files); Claude Code base guidance; every prior pass | Existing |
| **11** 11.6 Repository entry file | G1 section K recommendation; `ROJAN_Web/apps/website/CLAUDE.md` (stub exists) | New (G1 recommendation) |
