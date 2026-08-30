# 11 — AI Agent Governance

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

*Binding on any AI agent (Claude or otherwise) operating in any ROJAN
repository. Human contributors follow the same intent.*

---

## 11.1 Before writing code, an agent must **[NON-NEGOTIABLE]**

1. **Read the architecture** — this Root set, the repository's Tier-1 rules, and
   any Tier-2 module doc covering the files in scope.
2. **Report any conflict** it finds — between tiers, between a request and a
   rule, or between two rules — and **STOP**. It does not resolve an
   architecture conflict on its own or "work around" it.
3. **Assess impact** — run the Feature Process check (Domain Owner, Data
   Ownership, API Impact, Permission Impact, Migration Impact, Failure
   Scenarios).

## 11.2 Forbidden without explicit approval **[NON-NEGOTIABLE]**

- Building a parallel architecture, a second implementation of an existing
  mechanic, or an independent business/domain model in a client.
- Changing an API contract (DTO shape, endpoint path/method, request/response
  field, error condition) — from either side.
- Changing RBAC, permissions, auth logic, or token handling.
- Changing the database schema or adding a migration.
- Crossing into the canonical backend's domain from a client, even when it could
  be done entirely from client-side files (e.g. adding a role check, or
  redefining what a DTO field means).
- Removing or weakening an existing rule at any tier.
- Deleting files.
- `git commit`, `git push`, tag/branch creation on a shared remote, or a merge
  to a protected branch (Root 09 section 9.3).
- Restructuring a project (new modules, new flavors, build-config
  restructuring).

## 11.3 Proceed without asking

- Formatting; file organization within the existing structure; test creation and
  updates; documentation updates explicitly requested by the task; standard
  refactoring inside the current approved scope; building the project; running
  tests and build verification; inspecting code; running emulator/device checks
  and capturing screenshots; dead-code and unused-import cleanup; fixing a UI
  issue described in the task.

## 11.4 System boundary awareness

ROJAN is developed by coordinating teams with distinct control domains —
backend/security/schema/contract/cross-app architecture on one side, per-client
implementation within already-approved scope on the other. A change that would
cross into another team's control domain requires that team's confirmation,
**even if it is technically implementable from files the agent can already
edit.**

## 11.5 Honesty and scope

- An agent reports outcomes faithfully: a failed test is reported with its
  output; a skipped step is stated; "done" is claimed only when done and
  verified.
- An agent does not fabricate user approval, git state, verification it did not
  perform, or progress that did not happen.
- When a fix is found mid-task that is outside the current request's scope, the
  agent surfaces it and proposes a minimal scoped change — it does not fold it
  in silently.
- An agent stays within the requested scope: it does not expand a task, spawn
  more sub-agents than the work warrants, or add unrequested features.

## 11.6 Repository entry file

Every repository carries a Tier-1 `CLAUDE.md` (and/or `AGENTS.md`) at its root
that: (a) states repository-local rules, and (b) points to the vendored Root
Governance copy with the instruction *"read before implementing; on any
conflict, STOP and report."*
