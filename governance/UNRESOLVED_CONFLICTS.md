# Unresolved Conflicts — ROJAN Ecosystem Governance v2.0

**Status:** MATERIALIZED (PASS G4)

Documented, not resolved. These do not block the governance set from existing.
Each names its blocker and its target pass. Items marked **"before G5 adoption"**
gate propagation of this set into the product repositories.

| ID | Conflict | Status entering G5 | Owner / target |
|---|---|---|---|
| **C-1** | Fate of `ROJAN_Web/platform-core` (freeze / archive / revive-later). Owns designed-but-unbuilt domains (website-builder, commerce, finance, communication). | Interim: NON-CANONICAL / DORMANT (Root 01 section 1.3, Root 02 section 2.1). Needs a product-owner decision. | Ecosystem owner -> G6 |
| **C-2** | Fate of `ROJAN_Desktop/Rojan.Server` (C# backend, stale 2026-07-26). | Superseded. Recommend archive/delete after confirming no live references. | G6 |
| **C-3** | Which backend the production website actually calls. Frontend report says `ROJAN_Backend`; `apps/website/lib/api/client.ts` references a `platform-core` ADR and a `/api/v1/public/{tenantSlug}/...` surface not verified present in `ROJAN_Backend`. | **Must be verified against the live environment before G5 touches `ROJAN_Web`.** If the site calls a deployed `platform-core`, the G2 canonical-backend decision is re-examined. | Verify -> before G5 adoption |
| **C-4** | Error-response format: canonical backend's custom JSON (adopted, Root 03 section 3.6) vs `platform-core`'s RFC 7807 mandate. | Deferred. Canonical = custom JSON. Re-open only if platform-core domains are folded in. | Deferred |
| **C-5** | Does `ROJAN_DesignLab` also merge `feature/architecture-constitution-v2` into its own line, or is the V2 content only lifted to Root? | Content decision made (adopt V2). Branch mechanics open. | G5 input |
| **C-6** | Cross-product governance scope buy-in. V1 `docs/architecture/01` claims authority over all products; `ROJAN_Backend` / `ROJAN_Desktop` READMEs disclaim shared architecture. | Needs each repo owner's explicit sign-off before G5 adds the vendored copy + entry file. | Ecosystem owner -> before G5 adoption |
| **C-7** | Branch-model divergence (`ROJAN_Backend` release-train; `ROJAN_DesignLab` 69 commits ahead of a frozen `origin/main`). | Root 09 section 9.5 provides the exception mechanism. Each repo records a dated exception in G5 rather than realigning. | G5 |
| **C-8** | JWT algorithm: canonical HS256 (`ROJAN_Backend`) vs RS256 (`platform-core`). | Canonical = HS256 now. RS256 logged as a future security-hardening candidate. | Deferred |
| **C-9** | ~60 orphaned ecosystem `.md` files in `C:\AndroidProjects\` (version-controlled by nothing), including the V2-companion update report and the Meta-Scale direction doc. | Triage — move into this governance home's `archive/`, or a repo, or delete-with-approval. | G5 |
| **C-10** | Governance-set language policy (English-only / Persian-only / bilingual). Predecessor rulebook is Persian-first; this set is English. | Open. Decide before the set is adopted. | Before G5 adoption |
| **C-11** | Governance home + replication mechanism. G1 option A+C (shared repo + vendored copy + CI drift-check) vs. submodule. | **Partially settled in G4:** the set now lives at `C:\AndroidProjects\ROJAN_Governance\` as a standalone folder, not yet a git repository. Still open: git-init + first commit; whether replication into product repos is a vendored copy with a CI drift-check or a submodule. | G5 first step |
| **C-12** | Pre-existing fake-data fallback paths (e.g. Desktop `FakeOrganizationRepository`). | Root 10 section 10.5: remediation items, removed/gated before their component next releases. Not grandfathered. | Per-repo, tracked from G5 |

## Gating summary

**Before PASS G5 can propagate this set into the product repositories:**

- C-3 verified against the live environment.
- C-6 scope sign-off obtained from each repository owner.
- C-10 language policy decided.
- C-11 replication mechanism confirmed (and this folder git-initialized).
