# ARCHITECTURE_CONSTITUTION_V2_FINAL_REVIEW

**Scope:** Read-only review of the draft Rulebook changes from `ARCHITECTURE_RULEBOOK_UPDATE_REPORT_v1.md`, plus a cross-repository consistency check against Backend, Web, and Desktop documentation. **No file was modified in this pass.** Mobile's rulebook changes remain exactly as previously drafted and uncommitted.

---

## 1. Cross-Repository Architecture Consistency Review

### Backend (`ROJAN_Backend`)

No `docs/architecture/` folder exists in this repo (confirmed — searched for `architecture*`/`ARCHITECTURE*` at repo root, zero results). Its architecture record lives instead as root-level audit/report files: `ROJAN_Architecture_Compliance_Report_v1.md`, `ROJAN_Backend_Architecture_Audit_v1.md`, `ROJAN_Backend_Architecture_Implementation_Report.md`, plus module-specific plans (Customer CRM, Owner App Integration, etc.).

Checked directly: `ROJAN_Architecture_Compliance_Report_v1.md` confirms `Salon.ownerId: UserId` is the sole ownership mechanism (`domain/salon/Salon.kt`) — "Owner" is a data relationship (`Salon.ownerId == User.id`), **not** an RBAC role. `UserRole` has exactly three values (`CUSTOMER`, `MANAGER`, `SPECIALIST`) — no `OWNER` role exists, and the report itself flags this as an open question from prior audits, not something resolved. **No contradiction with the Rulebook** — `DOMAIN_OWNERSHIP.md`/`PERMISSION_MATRIX.md` don't enumerate roles at all, so there's nothing to conflict with, but this is a real gap worth naming: the Rulebook currently has no stated position on whether "Owner" should ever become a formal role. Not one of the 10 mandated rules, so not added in this pass — flagged as a future candidate.

### Web (`ROJAN_Web`)

`docs/architecture.md` + `docs/adr/*.md` describe a **different, non-production backend** — a "modular monolith with star topology" under package `com.rojan.platform`, living in this repo's own `platform-core/` module tree. This is **not** the real production backend (`ROJAN_Backend`, package `ai.rojan.backend`, Spring Boot 3.3.5, live at `api.rojanai.ir`) — confirmed independently by reading both codebases' source directly, and by `ROJAN_Web`'s own `ROJAN_Final_Completion_Report_v2.md`, which documents discovering and correcting this exact discrepancy. **This is a real, standing contradiction**, carried over unresolved from the prior report — see §2.

### Desktop (`ROJAN_Desktop`)

Both files in `docs/architecture/` (`00-overview.md`, `01-desktop-shell.md`) carry an explicit, prominent banner: *"⚠ Superseded by the phase-gated SDLC... not an active or approved deliverable. See `docs/phases/phase-01-foundation.md` for what's actually in force right now."* So Desktop's own `docs/architecture/` is **self-declared non-authoritative** — the real current source is `docs/phases/`. Checked `phase-01-foundation.md`: it's scoped to repo/solution/tooling standards only (branching, versioning, coding standards), not domain ownership — no statement there conflicts with the Rulebook. `01-desktop-shell.md` explicitly states Desktop is architecturally independent of `ROJAN_DesignLab` at the *file/package/UI-layering* level ("no file, package, asset, or architectural decision is shared") — this is about composition-root/DI/MVVM structure, a different axis than domain data ownership, so it does **not** contradict the Rulebook's RULE 001/003 (Backend owns Booking/Calendar; Desktop SQLite is projection-only) — those are orthogonal concerns (UI-layer independence vs. data-authority ownership) and both can be simultaneously true.

**Naming collision, not a contradiction, worth flagging:** "Phase" means different, repo-local things across the ecosystem — Desktop has its own `Phase 01`/`Phase 02`/`Phase 05` sequence (repo foundation → enterprise architecture → shell), completely separate from Mobile's phase history (Reception Phase 0/1, System2, etc.) referenced by this task's "Phase 0 to Phase 9" framing. No document claims these are the same sequence, but a reader moving between repos could reasonably assume they line up. Not fixed in this pass (out of the 10 mandated rules' scope) — flagged in §5.

### Mobile (`ROJAN_DesignLab_Main1`)

The Rulebook itself — already reviewed in full in the prior pass. No new contradiction found on this second read.

---

## 2. Verification: No Contradictory Architecture Statements

**Result: one standing contradiction confirmed (carried over, not new), zero new contradictions introduced by this pass's draft changes.**

The one real contradiction is the Web/Backend one from §1: two documents (`ROJAN_Web/docs/architecture.md` and the actual `ROJAN_Backend` codebase) both implicitly claim to describe "the" backend, and only one is true. This predates and is independent of the Rulebook draft — it lives entirely in `ROJAN_Web`, outside this repo, and outside what this task's commit would touch. It cannot be fixed by anything in `ROJAN_DesignLab_Main1`; it needs a `ROJAN_Web`-side action (retire or clearly mark `docs/architecture.md` as historical). Already flagged in the prior report (§6.3); repeating here because the cross-repo check was explicitly requested this round.

Everything else checked — Desktop's independence claims, Backend's role-model silence, Mobile's own internal cross-references added in the last pass — is either consistent or simply non-overlapping (different axis, no shared claim to contradict).

---

## 3. Review of All New Rulebook Additions

| Addition | File | Verdict |
|---|---|---|
| Source of Truth (RULE 001) | `DOMAIN_OWNERSHIP.md` | Sound. Calendar/Tenant Data rows added; correctly cross-references the new Booking ADR rather than duplicating its content. |
| Client Ownership (RULE 002) | `APPLICATION_RULES.md` (new section) | Sound. The three-category enumeration (Cache/Projection/Temporary offline state) is exhaustive and mutually exclusive as written; correctly points to `CACHE_POLICY.md` instead of restating it. |
| Cache Policy (RULE 008) | `CACHE_POLICY.md` (new) | Sound. Explicitly forbids using Cache for Permission/Booking decisions — this closes a real risk (a stale cached permission or slot being trusted) that RULE 002 alone doesn't cover. |
| Auth Separation (RULE 004) | `AUTH_DATA_SEPARATION.md` (new) | Sound, and the strongest addition of the batch — it's the one rule with zero prior coverage anywhere in the ecosystem (not even implicitly). The "session expiration ≠ data loss" statement is unambiguous and testable. |
| Booking Reliability (RULE 005) | `ADR-004_BOOKING_MUTATION_RELIABILITY.md` (new) | Sound. Correctly written as an ADR (not a rule file) since it's a concrete technical decision with Context/Decision/Impact/Migration Plan — matches `ADR-002`'s structure, unlike the under-filled `ADR-001`. |
| Tenant Model (RULE 007) | `ROJAN_PRODUCT_CONSTITUTION.md` | Sound. Explicit "today = Salon, Organization = future" statement closes real ambiguity — the pre-existing "5-year goals" list mentioned "large organizations" as an aspiration without ever stating today's constraint, which is exactly the kind of gap that invites a Client or Backend feature to quietly assume multi-Salon tenancy prematurely. |
| Release Governance (RULE 009) | `RELEASE_GOVERNANCE.md` (new) | Sound, and directly informed by real incidents this engagement encountered (an unowned, un-reviewed deploy-script permission bug that silently blocked production deploys until manually diagnosed). The "deploy mechanism itself is a release risk" line is not generic boilerplate — it reflects an actual failure mode. |
| Scale Rules (RULE 010) | `FUTURE_RULES.md` | Sound. Correctly avoids duplicating Observability (already in `TECHNICAL_STANDARDS.md`) — cross-references instead of restating, keeping one source of truth per concept. |

Two additions outside the requested list of 8, reviewed for completeness since they were part of the same draft:
- **RULE 003 (Calendar Authority)** — `APPLICATION_RULES.md`'s Desktop section: sound, directly supports Desktop's own `01-desktop-shell.md` (which never claims SQLite is a booking authority — no conflict).
- **RULE 006 (Development Order)** — `README.md`: this is the one addition worth a harder second look. The resolution note is correct but verbose relative to the rest of the Rulebook's terse style, and it papers over rather than fully merges the three-way discrepancy with `FEATURE_PROCESS.md` (already flagged as a follow-up in the prior report, §7). Not a blocker — the note is accurate — but it's the one place where "add a rule" and "keep the Rulebook lean" are in mild tension.

---

## 4. Final Recommended Structure

No structural reorganization needed — the existing 00–12 numbered-folder scheme absorbed all 10 rules without requiring renumbering, which is itself a signal the original structure was well-chosen. Recommended structure going forward:

```
00_START_HERE/README.md                                   — entry point + RULE 006 (dev order)
01_PRODUCT_CONSTITUTION/ROJAN_PRODUCT_CONSTITUTION.md      — RULE 007 (tenancy)
02_DOMAIN_ARCHITECTURE/
  DOMAIN_OWNERSHIP.md                                      — RULE 001 (source of truth)
  CACHE_POLICY.md                                           — RULE 008 (new)
03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md           — RULE 002, RULE 003
04_USER_LIFECYCLE_SCENARIOS/USER_SCENARIOS.md              — unchanged
05_AUTHENTICATION_SECURITY/
  PERMISSION_MATRIX.md                                      — unchanged
  AUTH_DATA_SEPARATION.md                                   — RULE 004 (new)
06_API_CONTRACT_GOVERNANCE/API_CONTRACT_RULES.md           — unchanged
07_MEDIA_ARCHITECTURE/MEDIA_POLICY.md                       — unchanged
08_DEVELOPMENT_GOVERNANCE/
  FEATURE_PROCESS.md                                        — unchanged
  RELEASE_GOVERNANCE.md                                     — RULE 009 (new)
09_AI_AGENT_GOVERNANCE/CLAUDE_RULES.md                      — unchanged
10_ARCHITECTURE_DECISIONS_ADR/
  ADR-001..003 (unchanged)
  ADR-004_BOOKING_MUTATION_RELIABILITY.md                   — RULE 005 (new)
11_TECHNICAL_STANDARDS/TECHNICAL_STANDARDS.md               — unchanged
12_FUTURE_SCALE/FUTURE_RULES.md                             — RULE 010
```

One addition recommended for *next* iteration, not this one: a `RULE_INDEX.md` under `00_START_HERE/` mapping RULE 001–010 → file path in one table (already recommended in the prior report; repeating because the folder now genuinely has enough files — 19 — that this is no longer optional polish).

## 5. Commit Readiness

**Ready to commit**, with two carry-over caveats that are explicitly out of this commit's scope, not blockers to it:

1. The `ROJAN_Web` vs `ROJAN_Backend` documentation contradiction (§1/§2) needs a separate, `ROJAN_Web`-side task — cannot be resolved by anything in this repo or this commit.
2. `ADR-001`'s template-non-compliance and the `README.md`/`FEATURE_PROCESS.md` sequence duplication (§3) are documentation-quality follow-ups, not correctness defects — the current draft is accurate, just not maximally lean in one spot.

No new contradiction was introduced by the draft changes. No file outside `docs/architecture/` (plus the two report files) was touched. All 10 mandated rules are now present, cross-referenced, and internally consistent with each other and with every other repository's own architecture statements that were checked.

---

## Files Changed (unchanged from the prior report — nothing further modified this pass)

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
??  ARCHITECTURE_RULEBOOK_UPDATE_REPORT_v1.md
??  ARCHITECTURE_CONSTITUTION_V2_FINAL_REVIEW.md   (this file)
```

**No commit made. No push made. Awaiting Team 1 approval.**
