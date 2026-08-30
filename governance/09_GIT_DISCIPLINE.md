# 09 — Git Discipline

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 9.1 Commit messages **[NON-NEGOTIABLE]**

Conventional Commits across every repository: `type(scope): summary` — `feat`,
`fix`, `refactor`, `chore`, `docs`, `test`, `ci`, `perf`. Summary in imperative
mood.

## 9.2 Scoped commits **[NON-NEGOTIABLE]**

A commit does one coherent thing. Unrelated fixes are not folded into a feature
change silently. When a fix is discovered outside the current task's scope, it is
surfaced and proposed as its own scoped change — not absorbed.

## 9.3 Commit is not approval **[NON-NEGOTIABLE]**

- `git commit`, `git push`, tag creation, branch creation on a shared remote,
  and merges to a protected branch **always require explicit human
  authorization**, regardless of what a task description implies about scope
  being pre-approved.
- Task approval authorizes *work*. It does not authorize *publishing that work*.
  These are separate consents.
- An agent must never fabricate, assume, or report a git action or an approval
  that did not actually happen. Reported git state is verified against real
  state.

## 9.4 Protected mainline **[NON-NEGOTIABLE]**

- The mainline branch (`main` / `master`) is protected: no direct push, no
  force-push, merge only via reviewed PR.
- Every PR into the mainline must build and pass the architecture-test suite
  before merge. An architecture-test failure is a build error, not a review
  comment.
- At least one review and a green CI run are required before merge.

## 9.5 Branching model

- **Default: trunk-based** — short-lived branches (`feat/...`, `fix/...`,
  `chore/...`, `docs/...`) off the mainline, merged back quickly. The mainline
  stays releasable.
- A repository that runs a **release-train model** (long-lived `release/vX.Y.Z`
  branches, a deliberately frozen mainline) records a dated exception per Root
  00. This is a recognized, allowed divergence — but it is explicit, not drift.
- Phase/milestone branches, where used, merge as a single reviewed PR
  representing the whole reviewed unit.

## 9.6 Tags

A version tag (`vMAJOR.MINOR.PATCH[-PRERELEASE]`) is applied only to a branch
that represents a real, shippable state (the mainline, or a release branch under
a section 9.5 exception) — never to a work-in-progress feature branch.

## 9.7 Working-tree hygiene

An agent or contributor does not clean, revert, reformat, or "fix" unrelated
dirty files in the working tree as a side effect of another task. Pre-existing
uncommitted changes are left as found unless they are the task.
