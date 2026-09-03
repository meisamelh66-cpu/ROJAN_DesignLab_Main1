# ROJAN Project Brain — Index

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: routing map for this directory only

This directory is **descriptive project memory**, not a rulebook. It records what is
currently true about the code. It never states what an agent may or may not do —
that's root `CLAUDE.md`'s job — and it never contains architecture decisions —
those live in `docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/`.

**Load this file by default. Open the others only when their trigger matches your task.**

| File | Purpose | Load when | Last verified |
|---|---|---|---|
| [STATE.md](STATE.md) | Tech stack, versions, module/variant counts | Task touches build config, dependencies, or you need current stack facts | 2026-09-03 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Actual code layering per flavor + known deviations | Task touches `domain/`/`data/`/`presentation/` structure, or any `manager/` code | 2026-09-03 |
| [FEATURES.md](FEATURES.md) | Per-flavor feature completion status | Asked "is X done / blocked", or scoping new work | 2026-09-03 |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | Token/theme inventory + Glass System phase status | Task touches UI, theme, tokens, or visual components | 2026-09-03 |
| [RISKS.md](RISKS.md) | Numbered risk / technical-debt ledger | Assessing whether a change is safe, or asked about risk | 2026-09-03 |
| [DECISIONS.md](DECISIONS.md) | Index of real ADRs + open governance items | Proposing anything that affects architecture | 2026-09-03 |
| [SESSION.md](SESSION.md) | **Volatile** — branch/HEAD/divergence/uncommitted intent | Any git action, or start of any session | 2026-09-03 — regenerate, don't trust blindly |

## Where the actual rules live

- **Behavioral rules** (what an agent may do automatically, what needs confirmation, frozen
  design baselines, environment quirks): root `CLAUDE.md`. Brain never restates these.
- **Durable architecture governance + ADRs**: `docs/architecture/` (see its own
  `00_START_HERE/README.md`). Brain only indexes this, in `DECISIONS.md`.
- **Point-in-time historical audits**: the ~25 `ROJAN_*_v1.md` reports at repo root.
  Brain points at the relevant one instead of re-summarizing it.

## Known duplication in the existing docs (not Brain's to fix)

`docs/architecture/CLAUDE.md` and `docs/architecture/09_AI_AGENT_GOVERNANCE/CLAUDE_RULES.md`
are both short stubs pointing back at root `CLAUDE.md` — not a second rule set. Treat root
`CLAUDE.md` as the single behavioral authority.
