# ROJAN Documentation Sync Review v1

**Status:** Review only. No files were staged, committed, pushed, or modified in producing this document.
**Date:** 2026-08-15
**Scope:** The 11 untracked `.md` files currently sitting in the repository root of `C:\AndroidProjects\ROJAN_DesignLab` (per `git status`, branch `feature/android-reception-app`). The ~20 other `ROJAN_*` report/plan `.md` files already committed to this repo's history are out of scope — they're referenced only where relevant to a duplicate/staleness finding below.

---

## 1. Document List

| # | Document | Size | Last modified |
|---|---|---|---|
| 1 | `ROJAN_Customer_Git_Status_Report_v1.md` | 12.2 KB | 2026-08-12 23:28 |
| 2 | `ROJAN_First_Salon_Implementation_Roadmap_v1.md` | 30.8 KB | 2026-08-15 11:35 |
| 3 | `ROJAN_First_Salon_Readiness_Audit.md` | 34.2 KB | 2026-08-15 10:50 |
| 4 | `ROJAN_Git_Verification_Report_v1.md` | 5.3 KB | 2026-08-15 10:40 |
| 5 | `ROJAN_Independent_Release_Readiness_Audit_v1.md` | 24.1 KB | 2026-08-15 00:37 |
| 6 | `ROJAN_Pilot_Implementation_Task_Board_v1.md` | 11.7 KB | 2026-08-15 09:55 |
| 7 | `ROJAN_QA_Remediation_Plan_v1.md` | 16.3 KB | 2026-08-15 01:08 |
| 8 | `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` | 29.9 KB | 2026-08-15 08:36 |
| 9 | `ROJAN_Salon_Identity_Architecture_Report_v1.md` | 33.0 KB | 2026-08-15 11:23 |
| 10 | `ROJAN_System1_First_Salon_Backend_Plan_v1.md` | 29.9 KB | 2026-08-15 08:41 |
| 11 | `ROJAN_System2_Android_Parallel_Work_Report_v1.md` | 9.0 KB | 2026-08-13 06:19 |

---

## 2. Classification

The requested taxonomy is Audit / Decision / Plan / Report. One immediate structural finding: **no document in this untracked set is actually a Decision document.** The one real decision record this project relies on — `ROJAN_System1_Backend_Decision_v2.md` (RBAC role model, permission mapping, invite mechanism) — lives in the sibling `ROJAN_Backend` repository, not here. Every `ROJAN_DesignLab`-side document either investigates state (Audit), sequences future work (Plan), or reports completed/point-in-time state (Report), and several explicitly wait on a System 1 decision rather than recording one.

| Document | Classification | Basis |
|---|---|---|
| `ROJAN_First_Salon_Readiness_Audit.md` | **Audit** | Investigates current backend+Android state against the fixed pilot journey, cross-references live `ROJAN_Backend` source, produces findings/blocking-issues lists. Textbook audit shape. |
| `ROJAN_Independent_Release_Readiness_Audit_v1.md` | **Audit** | Independent architecture/security/quality/test-coverage review of the Android repo, risk-leveled findings. |
| `ROJAN_Salon_Identity_Architecture_Report_v1.md` | **Audit** *(filename says "Report," function is audit)* | Verifies a specific architectural principle ("salon = Beauty Identity Entity") against backend schema/domain source, produces graded findings (§2's field-by-field table) and one new, directly-verified defect (§5.1's DTO/comment drift). Despite the filename's "Report" suffix, its method and output shape are audit, not status-summary — flagged here as the clearest **naming/function mismatch** in this batch (see §3). |
| `ROJAN_Pilot_Implementation_Task_Board_v1.md` | **Plan** | Forward-sequenced, execution-ordered task list synthesizing three other documents; explicitly a planning artifact awaiting per-item approval. |
| `ROJAN_QA_Remediation_Plan_v1.md` | **Plan** | Ordered remediation backlog derived directly from the Independent Release Readiness Audit; owner-tagged, phased. |
| `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` | **Plan** | Full backend implementation plan (domain/application/infra/API/test/security layers) submitted for System 1 review — explicitly not yet approved. |
| `ROJAN_First_Salon_Implementation_Roadmap_v1.md` | **Plan** | Phase 0–5 implementation roadmap built against the newly-approved scope decision; the newest and most current plan in this set. |
| `ROJAN_Customer_Git_Status_Report_v1.md` | **Report** | Point-in-time status snapshot of the Customer flavor's git/build/config state. |
| `ROJAN_Git_Verification_Report_v1.md` | **Report** | Point-in-time git/repo-identity/branch/tracking-status snapshot. |
| `ROJAN_System2_Android_Parallel_Work_Report_v1.md` | **Report** | Completed-work summary (changed files, tests added, verification run) for a specific Android work phase — not yet committed or pushed. |

**Distribution:** 3 Audit, 4 Plan, 3 Report, 0 Decision (of 11 total).

---

## 3. Duplicates and Obsolete Documents

### 3.1 Exact duplicate — `ROJAN_System1_First_Salon_Backend_Plan_v1.md` ≡ `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md`

**Byte-for-byte identical content** (confirmed via `diff`: zero differing lines; identical size, 29,864 bytes; timestamps five minutes apart — 08:36 vs. 08:41 — consistent with one being saved as a second filename shortly after the first, not independently authored). This is the one unambiguous duplicate in the set.

**Recommendation:** keep exactly one of the two under Git. `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` is the better name to retain — it names the actual subsystem (`SalonMembership`/RBAC) rather than the broader, less specific "First Salon Backend Plan," and it's the name already cross-referenced by other documents in this set (`ROJAN_First_Salon_Readiness_Audit.md` §4/§8, `ROJAN_Pilot_Implementation_Task_Board_v1.md` §2.1/§3.1-3.2 both cite it by this exact filename). `ROJAN_System1_First_Salon_Backend_Plan_v1.md` should **not** be committed — carrying two identical files into shared history under different names would itself become a future documentation-hygiene defect, and per `CLAUDE.md`, deleting it requires separate explicit confirmation (not performed here — review only).

### 3.2 Partially obsolete — `ROJAN_Customer_Git_Status_Report_v1.md`

This document's own header states its branch context as **`feature/manager-backend-integration`** at commit `6b30597`. The repository's actual current branch, confirmed by both `git status` and the same-day `ROJAN_Git_Verification_Report_v1.md`, is **`feature/android-reception-app`** at `1a3bdb0` (46 commits ahead of `origin/main`). This document predates the branch switch (timestamp 2026-08-12 23:28, roughly two days older than every other file in this batch) and its git-status section (§1, §3, §4 of that document) is now **stale, not current**.

This is a **partial**, not total, obsolescence: the document's Customer-flavor architecture content (flavor config, source-set structure, screen inventory, auth flow, backend integration points — §2 of that document) describes structural facts about the Customer app that haven't changed and remain accurate. Only the *git/branch/commit-count* framing is out of date.

**Recommendation:** if committed, commit with an explicit note (in a future revision, not this review) that its git-status sections are point-in-time as of 2026-08-12 and superseded by `ROJAN_Git_Verification_Report_v1.md` for current branch state — don't let a reader assume its branch/commit-count claims are live.

### 3.3 Already-flagged staleness, carried forward — `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md`'s own premise

Not new to this review, but relevant to a documentation-hygiene pass: this plan frames the RBAC role-model and invite-mechanism questions as **open decisions for System 1 to make** (its own §3/§4 step 1). `ROJAN_First_Salon_Readiness_Audit.md` (same day, later timestamp) already found this framing outdated — `ROJAN_Backend/ROJAN_System1_Backend_Decision_v2.md` had, by the time of that audit, already resolved both questions. `ROJAN_Pilot_Implementation_Task_Board_v1.md` repeats the same now-stale "open decision" framing in its §3.1/§3.2. None of the three documents has been revised to reflect the resolution — the correction currently exists only as a callout *inside* the First-Salon audit, not as an update to the plan or task-board documents themselves.

**Recommendation:** this doesn't block committing either document (their implementation-detail content — domain/API/test-plan shapes — remains valid and useful), but a reader encountering `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` or `ROJAN_Pilot_Implementation_Task_Board_v1.md` cold should be pointed to the First-Salon audit's correction rather than taking either document's "open decision" framing at face value. Worth a small follow-up revision pass (out of scope for this review, which does not modify files).

### 3.4 Supersession risk, not yet actual — `ROJAN_Pilot_Implementation_Task_Board_v1.md` vs. `ROJAN_First_Salon_Implementation_Roadmap_v1.md`

These are not duplicates — the Task Board sequences a broader QA/hardening backlog (§1 Android-only fixes, §4 test priorities) that the Roadmap doesn't cover at all, and the Roadmap sequences pilot-scope work the Task Board predates (it was written before the System 1 scope-approval response that the Roadmap plans against — e.g., the Task Board has no concept of "Logo/Cover are mandatory, Trust Layer is deferred," because that scope decision didn't exist yet when it was written). But there is real content overlap on the shared subject matter (Reception/RBAC sequencing, salon-creation gap) where the two documents could drift out of sync if maintained independently going forward — the Task Board's §2/§3 (Blocked by System 1 / Requires API Confirmation) cover much of the same ground as the Roadmap's §4 Phase 0/2 and §5 dependency table, from before the scope decision landed.

**Recommendation:** not a duplicate to discard, but flag for the team that `ROJAN_First_Salon_Implementation_Roadmap_v1.md` is now the **more current, scope-approved** sequencing document for anything pilot-critical; the Task Board remains the authoritative source only for the QA/hardening items (§1, §4) that sit outside the Roadmap's scope entirely. Recommend a short cross-reference note in a future revision of the Task Board pointing to the Roadmap, rather than treating both as independently authoritative on overlapping ground.

### 3.5 No other duplicates found

The remaining six documents (`First_Salon_Readiness_Audit`, `Independent_Release_Readiness_Audit`, `QA_Remediation_Plan`, `Salon_Identity_Architecture_Report`, `Git_Verification_Report`, `System2_Android_Parallel_Work_Report`) each cover materially distinct scope, cite each other appropriately as sources rather than restating content, and show no meaningful text overlap beyond expected cross-references.

---

## 4. Recommendation: What Should Enter Shared Git History

**Commit as-is (8 of 11):**

| Document | Rationale |
|---|---|
| `ROJAN_First_Salon_Readiness_Audit.md` | Actively cited by 3 other documents in this set as the source of truth for pilot-path status; keeping it local-only breaks that referential chain for any other team member or session. |
| `ROJAN_Independent_Release_Readiness_Audit_v1.md` | Same reasoning — root source for `ROJAN_QA_Remediation_Plan_v1.md`'s entire structure. |
| `ROJAN_QA_Remediation_Plan_v1.md` | Live, owner-tagged backlog; only useful as a shared artifact if actually shared. |
| `ROJAN_SalonMembership_RBAC_Backend_Implementation_Plan_v1.md` | Submitted for System 1 review per its own header — cannot be reviewed by System 1 if it never leaves this local checkout. Commit **this filename**, not §3.1's duplicate. |
| `ROJAN_Pilot_Implementation_Task_Board_v1.md` | Working execution-order record; note its partial-supersession caveat (§3.4) in a future revision, not a reason to withhold it now. |
| `ROJAN_Salon_Identity_Architecture_Report_v1.md` | Contains the one genuinely new, directly-verified defect in this batch (§5.1's Android/backend DTO drift) — this needs to be visible to whoever next touches `Salon.logoUrl`/lat-long work, which requires it being in shared history, not a local file. |
| `ROJAN_First_Salon_Implementation_Roadmap_v1.md` | The newest, scope-approved planning document and the one this review's own git-status check confirmed is still local-only — per the prior turn's finding, this is the most urgent single item to get into shared history given it's the document Phase 0-5 execution will actually be run against. |
| `ROJAN_System2_Android_Parallel_Work_Report_v1.md` | Documents real, already-implemented (per its own content) Android fixes awaiting review — same "can't be reviewed if it's not shared" logic as the RBAC plan. |

**Commit with a caveat, not withheld (1 of 11):**

| Document | Rationale |
|---|---|
| `ROJAN_Customer_Git_Status_Report_v1.md` | Its Customer-architecture content (§2 of this review) remains genuinely useful as a shared record and isn't reproduced anywhere else in this batch. Commit it, but see §3.2 — a reader needs to know its branch/commit framing is a 2026-08-12 snapshot, not current, to avoid drawing a wrong conclusion about what branch the repo is on today. |

**Do not commit (1 of 11):**

| Document | Rationale |
|---|---|
| `ROJAN_System1_First_Salon_Backend_Plan_v1.md` | Exact duplicate (§3.1) of a file already recommended for commit under its better name. Committing both adds a maintenance burden (two files to keep in sync, or one silently drifting from the other) for zero benefit. |

**Neutral / no strong recommendation either way (1 of 11):**

| Document | Rationale |
|---|---|
| `ROJAN_Git_Verification_Report_v1.md` | Accurate and current as of this review, but a pure git-status snapshot has an inherently short shelf life (its branch/commit-count facts will be stale again within days). This repo already has an established convention of committing this *kind* of point-in-time report (per the ~20 already-committed historical reports this review didn't need to re-examine) — consistent with that convention, committing it is reasonable and low-risk, but its value as a *shared* artifact is lower than the audits/plans above, which drive actual ongoing decisions. Defer to the team's existing convention rather than treating this as a strong recommendation either way.

**Process observation, not a recommendation to act on now:** this is the fourth batch of same-day root-level report `.md` files this repo has accumulated (following the ~20 already-committed historical set `ROJAN_QA_Remediation_Plan_v1.md` §1.6 already flagged for consolidation into `docs/reports/`). That consolidation recommendation still stands and would apply equally to this batch once committed — noted here for completeness, not actioned, since this review's instructions are explicitly read-only.

---

*This review is a point-in-time documentation-hygiene artifact. No file was added, staged, committed, pushed, or modified in producing it.*
