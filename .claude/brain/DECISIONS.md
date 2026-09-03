# DECISIONS — ADR Index

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: **index only**. Durable architecture decisions live in `docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/`. This file must never contain decision content — only pointers. It is not itself a decision log.

## Real ADRs (`docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/`)

| ADR | Title | Status | One-line impact |
|---|---|---|---|
| ADR-001 | Backend Source Of Truth | *(no Status field in file)* | Backend owns the core domains — prevents conflicting product-side models |
| ADR-002 | Media Upload Architecture | Accepted | Client-side compression + alignment with backend multipart limits for logo/cover/gallery uploads |
| ADR-003 | Environment Configuration Strategy | Accepted | `target` × `environment` build-flavor URL matrix; fail-loud on missing URL (companion: `ADR-003_VALIDATION_REPORT.md`) |
| ADR-004 | Booking Mutation Reliability | Accepted | Five required properties (incl. idempotency) for any booking mutation across concurrent clients |

Template for new ADRs: `docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/ADR_TEMPLATE.md`.

## Open governance item — not resolved by Brain

`docs/architecture/RULE_BOOK/ROJAN_RULE_BOOK_V2.md` (new, untracked as of this snapshot — see
SESSION.md) mints its own `ADR-0015` outside the real `ADR-00N` sequence above, and restates
topics already covered by the numbered `docs/architecture/01_.../08_...` folders (booking rules,
auth rules, UI rules). This is a standing inconsistency, flagged here so it isn't rediscovered
from scratch each session. **Brain does not resolve it** — it needs a decision to either merge
`ADR-0015` into the real sequence as a proper `ADR-005`, or explicitly mark `RULE_BOOK` as a
draft/proposal track, not a decision-numbered one.

## How this file is updated

When a session's work produces a real architecture decision, it becomes a new file in
`10_ARCHITECTURE_DECISIONS_ADR/` (using `ADR_TEMPLATE.md`) — never a new entry invented here
first. This file then gets one new index row pointing to it.
