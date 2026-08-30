# 07 — Release Policy

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 7.1 Release components

The ecosystem releases four component types independently:

| Component | Repository |
|---|---|
| Backend | `ROJAN_Backend` |
| Website | `ROJAN_Web` (`apps/website`) |
| Mobile | `ROJAN_DesignLab` (Customer / Manager / Reception) |
| Infra | VPS / Compose / reverse-proxy configuration |

*(Desktop joins this table as a fifth component when it enters release.)*

## 7.2 Every component names three roles before it releases **[NON-NEGOTIABLE]**

- **Owner** — the person or team that makes the final release decision.
- **Approval Flow** — who must approve before deploy.
- **Rollback Responsibility** — who executes rollback if the release is bad.

**Without a named Owner, a component does not release.**

## 7.3 Release invariants **[NON-NEGOTIABLE]**

Every release of any component carries:

- **Version** — Semantic Versioning 2.0 (`MAJOR.MINOR.PATCH[-PRERELEASE]`). The
  version is a single source of truth in the component's own repo, and any
  release-time tag must match it exactly.
- **Release notes** — a `CHANGELOG.md` entry in Keep a Changelog format
  (`Added / Changed / Deprecated / Removed / Fixed / Security`), written in the
  same change that bumps the version, never retroactively.
- **Release metadata** — at minimum: version, release date, and the minimum
  supported prior version it is compatible with. For distributed binaries
  (Android APK/AAB, Desktop package): also target architecture.
- **Integrity metadata where applicable** — for any downloadable binary, a
  published checksum (SHA-256). Signing is required before any Staging/Production
  distribution of a client binary; it is a known open gap for mobile and must be
  closed before that step.

## 7.4 Validation gate **[NON-NEGOTIABLE]**

A component does not release unless, at release time (not from stale evidence):

1. Its full test suite runs with **zero new failures** versus the last
   known-good baseline for that component. Pre-existing, understood,
   explicitly-excluded failures are allowed; a new one is not.
2. Its build succeeds with no errors.
3. Its architecture-test suite passes.
4. For a client: the change is verified on a real device or real environment,
   with real evidence — not an unsupported checkmark. If verification cannot be
   performed, that is stated explicitly; it is never claimed.
5. Every required check that could not be performed is marked **BLOCKED with a
   stated reason** — never silently skipped. **BLOCKED** (missing data or
   environment) and **FAIL** (a reproduced defect) are reported as the distinct
   outcomes they are.

## 7.5 Deploy mechanism **[NON-NEGOTIABLE]**

- The deploy mechanism (script, pipeline, CI/CD workflow) is itself tested before
  its first production use. **A failure of the deploy mechanism is a release risk
  in its own right.**
- Rollback must be executable **without a new architecture decision** — the
  procedure is defined in advance.
- Production releases follow staged validation where the infrastructure allows it
  (dev -> staging -> production, or an equivalent gated approval), and use a
  manual-approval gate for the production step where one is available.

## 7.6 Per-component mechanics (Tier-1, not Root)

Each repository owns *how* it releases — tag-triggered vs release-branch,
container vs APK vs ZIP, which CI provider. Root owns the invariants above. Where
a repository's mechanism diverges from a documented ecosystem norm (e.g. release
branches instead of trunk tags), it records a dated exception (Root 00).

## 7.7 Release is the last step, not a substitute for validation

`... -> Validation -> Release`. Release never stands in for the Feature Process
check, testing, or migration safety.
