# ROJAN DesignLab — Project Constraints

**This file is a consolidated index, not a new rule source.** Every rule below
already exists in `governance/` (Tier 0, vendored, `GOVERNANCE-v2.0.0`) or
`CLAUDE.md` (Tier 1). It exists so an agent or contributor can check the
quality bar in one place without re-reading twelve documents. It carries no
authority of its own.

**Authority order (unchanged):** `governance/*.md` **[NON-NEGOTIABLE]** tags >
`governance/*.md` (all other rules) > `CLAUDE.md` (Tier 1, may add, never
weaken) > Tier-2 frozen module baselines > this file. On any conflict between
this file and its sources, the source wins — treat the mismatch as a bug in
this file and report it, per `governance/11_AI_AGENT_GOVERNANCE.md` §11.1.2.
`governance/` is a vendored copy — **never edited here**; it upgrades only via
`vendor-governance.sh` against a newer tag (`governance/ADOPTED_VERSION`).

This file may be **strengthened** freely. It must never be edited to make an
existing rule easier to pass.

---

## 1. Non-negotiable / Tier-0 constraints

Source: `governance/` root documents, all `[NON-NEGOTIABLE]`-tagged. A lower
tier (this repo, this file) can never relax these — only an ecosystem-owner
exception can, and only via the dated block format in `governance/00_INDEX_AND_PRECEDENCE.md`.

- **One canonical backend.** `ROJAN_Backend` is the sole authority; no client
  integrates with a non-canonical backend. — `02` §2.1
- **Business authority is computed once, in the backend.** A client may never
  decide permission/conflict/duplicate/eligibility outcomes itself, not even
  "advisorily" or "for UX." — `02` §2.2
- **Contract-first, versioned, backward-compatible.** No DTO/endpoint/error
  shape change without review; breaking changes ship as a new `/api/vN/`
  prefix; shipped clients must stay parseable. — `03` §3.1, §3.3, §3.4, §3.5
- **Auth is backend-owned.** No client-side auth logic, no forged/bypassed
  token, tokens are opaque to the client. — `05` §5.1
- **Authorization is backend-owned.** Never computed, cached, or inferred
  client-side — not even to hide a button early. — `05` §5.2
- **Identity / Session / Cache / Client State are never conflated.** Session
  expiry never means data loss; logout invalidates only the session. — `05` §5.3
- **Client role is exactly:** collect input → submit with an
  `Idempotency-Key` for mutations → render what the backend returns. Never
  hold an authoritative business record, never build an independent domain
  model, never fabricate data on backend failure. — `06` §6.1, §6.2
- **No fake/mock/demo data in a production code path.** Fail loudly (error,
  empty state, refused start) instead. — `10` §10.1, §10.2, §10.4
- **Git authorization is separate from task approval.** `git commit`,
  `git push`, tag/branch creation on a shared remote, and merges to a
  protected branch always require explicit human authorization, regardless of
  what the task implies about scope. — `09` §9.3
- **Protected mainline.** No direct push or force-push to `main`/`master`;
  PR + review + green CI only. — `09` §9.4
- **Before writing code, an agent reads the architecture, reports any
  conflict, and STOPs** rather than resolving it unilaterally. — `11` §11.1

## 2. Build and release constraints

Source: `CLAUDE.md` "ROJAN Quality Gate (RQG)", `app/build.gradle.kts`,
`governance/07_RELEASE_POLICY.md`, `governance/06_CLIENT_RESPONSIBILITY_BOUNDARY.md` §6.4.

- `assembleDebug` must succeed before any UI task is considered complete.
  — `CLAUDE.md` RQG #1
- Every client runs a build + unit + architecture-test gate before merge and
  before release. — `06` §6.4
- A production release (`*ProductionRelease`) **fails the build** if the
  production API URL is malformed, or if release signing is not fully
  configured — enforced today by the `gradle.taskGraph.whenReady` guard in
  `app/build.gradle.kts`. This is a stricter, already-automated form of
  governance's "never release unsigned" invariant. — `07` §7.3
- `versionName` follows Semantic Versioning 2.0 and is the single source of
  truth; a `CHANGELOG.md` entry ships in the same change as the version bump,
  never retroactively. — `07` §7.3. The Android-specific mechanic —
  `versionCode` increases only, a published one is never reused — is this
  repo's own specialization of that rule. —
  `.claude/skills/rojan-release-manager/SKILL.md` Phase 4
- A component names an Owner, Approval Flow, and Rollback Responsibility
  before it releases; without a named Owner it does not release. — `07` §7.2
- At release time: full test suite has zero new failures vs. the last
  known-good baseline, build succeeds, architecture-test suite passes, and
  the change is verified on a real device/environment with real evidence — a
  verification that could not be performed is reported as **BLOCKED**, never
  silently skipped or claimed. — `07` §7.4
- Android Lint gate: `abortOnError = true`, `checkDependencies = true`, no
  baseline file — findings are triaged and fixed, not snapshotted or
  suppressed. — `app/build.gradle.kts` `lint {}` block
- When investigating uncommitted/ambiguous changes, diff against
  `git show HEAD:<path>` to establish the last-known-good baseline before
  proposing a fix. — `CLAUDE.md`

These gates are **operationalized**, not duplicated, by this repo's own
skills: `android-build-signing` (build/signing validation),
`rojan-release-manager` (full release lifecycle and release decision),
`android-qa-audit` (the device-level QA matrix behind RQG #5 and `07` §7.4.4),
`store-publish-manager` (distribution channels), `rojan-update-system`
(in-app update flow, see also `governance/08_UPDATE_POLICY.md`). Use those
skills to execute a release; this file only states the bar they must clear.

## 3. Architecture and System 1 / System 2 boundaries

Source: `CLAUDE.md` intro + "System 1 / System 2 boundary",
`governance/11_AI_AGENT_GOVERNANCE.md` §11.2, §11.4.

- Clean Architecture: `domain/` carries zero Android-framework imports;
  `data/`, `navigation/`, `screens/`, `ui/` are the other layers — enforced in
  this repo by `app/src/test/java/ai/rojan/designlab/architecture/ArchitectureRulesTest.kt`
  (a dependency-free source-scan guard; see also `02` §2.4 / `06` §6.4's
  "executable architecture-test suite" requirement).
- **System 1** (the coordinating backend team) owns: backend, security/RBAC,
  database schema, API contracts, cross-app architecture decisions.
  **System 2** (this repo) owns: Android implementation within scope already
  approved by System 1.
- A change that would cross into System 1's domain requires System 1
  confirmation **even if it is technically implementable entirely from
  Android-side files** (e.g. adding a role check, redefining what a DTO field
  means). — `CLAUDE.md`; `11` §11.2, §11.4
- Three product flavors (`customer`, `manager`, `reception`) on the single
  `:app` module is the frozen architecture ("ROJAN MANAGER FOUNDATION v1.0
  FROZEN"). No separate Gradle module, no shared-library extraction, no new
  flavor, no `applicationId` change without approval. — `CLAUDE.md`
- Reception app's approved scope (Phase 1) is fixed: OTP auth, salon
  selection, profile, dashboard, booking wizard, customer list. No Calendar
  screens, no Invite UI, until separately approved. — `CLAUDE.md`

## 4. UI / frozen Design Baseline constraints

Source: `CLAUDE.md` "Design Baseline v1.0 (Frozen — Customer Home / Manager
Dashboard)" and "Shared Premium Glass Design System" sections.

- Customer Home and Manager Dashboard's current visual language (background
  atmosphere, glass mechanic, three-tier shadow scale, color tokens, card
  elevation structure, typography tokens, entrance/press animation) is frozen
  at its current values. Current canvases are dark navy/deep-purple
  (Customer) and dark emerald/teal (Manager) per the 2026-09-02 supersession
  note — the glass mechanic, shadow scale, typography, spacing, and RTL rules
  remain authoritative regardless of that canvas-color change.
- **Shared Premium Glass Design System:** one rendering mechanic (glass,
  border, shadow, glow, typography, buttons, cards) across every app; only
  `RojanAppPalette` differs per app. Manager is the named master reference
  when a mechanic needs picking between divergent implementations.
- Spacing uses only `RojanDimens` tokens — never a raw `.dp` literal for
  layout spacing; stacked-card dashboard screens use the named rhythm tokens
  (`SpaceSectionToSection`, `SpaceCardToCard`, `SpaceTitleToContent`)
  specifically, not the raw scale token.
- Purely additive use of existing primitives at current values needs no
  approval. **Changing a frozen value, or introducing a parallel visual
  system, does.**
- RTL Persian-first experience must remain intact on every screen. —
  `CLAUDE.md` Development Rules; RQG #4

## 5. Testing and quality requirements

Source: `CLAUDE.md` RQG, `governance/06` §6.4, `governance/07` §7.4,
`.claude/skills/android-qa-audit/SKILL.md`.

- RQG is mandatory before any UI task is called done: `assembleDebug`
  succeeds; no new hardcoded colors/raw values outside token-definition
  files; design tokens/glass system used consistently; RTL intact; a real
  device/emulator screenshot is provided, or its absence is stated explicitly
  — never claimed without evidence. — `CLAUDE.md` RQG
- The architecture-test suite (`ArchitectureRulesTest.kt`) must pass; an
  architecture-boundary violation is a build error, not a review comment. —
  `06` §6.4
- At release time, the full test suite must show **zero new failures**
  against the last known-good baseline; pre-existing, documented exclusions
  are allowed, a new failure is not. — `07` §7.4.1
- `android-qa-audit`'s device-level matrix (install, auth/OTP, customer main
  flow, booking journey, profile, visual QA against the approved design
  language, performance QA, network QA, regression re-test of shared
  components) runs before a release decision — this complements, it does not
  replace, the governance build/test/architecture gate above.

## 6. Security and privacy constraints

Source: `governance/05_AUTHENTICATION_AND_AUTHORIZATION_RULES.md`,
`governance/10_NO_FAKE_DATA_POLICY.md`, `CLAUDE.md` "Confirmation Required".

- Auth/OTP/token issuance and verification happen only in the backend; no
  client-side auth logic, no "developer mode" bypass in a production build,
  tokens are opaque to the client. — `05` §5.1
- Authorization/RBAC/permission decisions are backend-only, never cached or
  derived client-side, even advisorily. — `05` §5.2; `CLAUDE.md`
- Identity/Session/Cache/Client-State distinctions must be preserved; a
  cached role or stored session is never proof of permission. — `05` §5.3
- No fake-data fallback on backend failure or unavailability — show an error
  or empty state with retry. — `10` §10.1–§10.4
- Changing RBAC, permissions, auth logic, or token handling always requires
  explicit confirmation. — `CLAUDE.md`; `11` §11.2
- Any new client-side data collection (e.g. a crash-reporting or analytics
  SDK) must be checked against this repo's existing privacy documentation
  (`PRIVACY-IMPLEMENTATION-REPORT.md`, `PRIVACY-POLICY-RELEASE-REPORT.md`)
  before it is added, and treated as a new-mechanic decision requiring
  approval (same precedent as adding osmdroid/Coil). — `CLAUDE.md` dependency
  comments in `app/build.gradle.kts`

## 7. Git / commit / push requirements

Source: `governance/09_GIT_DISCIPLINE.md`, `CLAUDE.md` "Confirmation
Required".

- Conventional Commits: `type(scope): summary`, imperative mood. — `09` §9.1
- One coherent change per commit; an out-of-scope fix found mid-task is
  surfaced and proposed as its own scoped change, never folded in silently.
  — `09` §9.2
- `git commit`, `git push`, tag creation, branch creation on a shared remote,
  and any merge to a protected branch **always** require explicit human
  authorization — regardless of what the current task's scope implies is
  pre-approved. — `09` §9.3; `CLAUDE.md`
- Protected mainline: no direct push, no force-push; PR + at least one review
  + green CI before merge. — `09` §9.4
- Branching default is trunk-based. This repo records a **logged exception**
  (top of `CLAUDE.md`, approved by the repository owner, 2026-08-30):
  `feature/android-first-salon-pilot` is the working trunk until merged into
  a deliberately frozen `origin/main`; review trigger is "once the pilot line
  lands on `main`." — `09` §9.5 exception mechanism
- Working-tree hygiene: never clean, revert, reformat, or "fix" unrelated
  dirty files as a side effect of another task. — `09` §9.7

## 8. Rules that require explicit approval

Source: `CLAUDE.md` "Confirmation Required", `governance/11` §11.2, §11.4.

- Backend changes (anything under `ROJAN_Backend`).
- API contract changes: DTO shapes, endpoint paths/methods, request/response
  fields, error conditions — from either side.
- RBAC/security changes: roles, permissions, auth logic, token handling.
- Database migrations or schema changes.
- Architecture changes: crossing module boundaries, merging/extracting
  shared layers across apps, changing how flavors relate to each other.
- Deleting files.
- Changing project structure: new modules, new flavors, build-config
  restructuring.
- Committing or pushing git — always, regardless of task scope.
- Changing a frozen Design Baseline value, or introducing a parallel visual
  system (§4 above).
- Adding a new cross-cutting library/rendering mechanic (precedent: osmdroid,
  Coil — each required explicit approval before being added).
- Crossing into System 1's domain, even when technically possible from
  Android-side files alone (§3 above).
- Removing an existing feature without approval.

## 9. Automatic actions already permitted

Source: `CLAUDE.md` "Automatic Actions", `governance/11` §11.3. No
confirmation needed for:

- Formatting; file organization within the existing package structure.
- Test creation and updates.
- Documentation updates: code comments, `CLAUDE.md`, and report docs
  explicitly requested by the task.
- Standard refactoring inside the current task's approved scope.
- Building the project; running tests and build verification.
- Inspecting code.
- Running emulator/device checks and capturing screenshots.
- Code cleanup: dead code, unused imports, lint fixes.
- Fixing a UI issue described in the task.

---

*Maintenance: this file should change only to stay aligned with
`governance/` and `CLAUDE.md` as those change — never to relax a rule those
sources still state. `governance/` itself is never edited in this repo.*
