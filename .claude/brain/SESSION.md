# SESSION — Volatile Working-Tree State

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: **generated, volatile state**. This file is stale the moment anyone commits, pushes, pulls, or edits a file. Regenerate it (`git status`, `git log -1`, `git rev-list --left-right --count HEAD...origin/<branch>`) before trusting it — do not treat it as durable fact the way STATE.md/ARCHITECTURE.md are.

## Branch state (as of 2026-09-03)

- Branch: `feature/android-first-salon-pilot`
- HEAD: `499ab45` — `docs(architecture): update constitution v2 rules and governance` (2026-08-22)
- Remote divergence: **2 ahead / 2 behind `origin/feature/android-first-salon-pilot`**
  — reconcile before treating this branch as a stable reference point for anything.

## Uncommitted changes (10 entries) and inferred intent

**Feature — "Customer Specialist → Services" integration** (complete, tested):
- M `data/remote/SpecialistApi.kt`, `domain/repository/SpecialistRepository.kt`,
  `data/repository/SpecialistRepositoryImpl.kt`, `presentation/specialist/SpecialistProfileViewModel.kt`,
  `screens/specialist/SpecialistProfileScreen.kt`
- ?? `app/src/test/.../presentation/specialist/SpecialistProfileViewModelTest.kt` — 5 tests
  (happy path, filtering, "empty = eligible for everything" rule, error, retry)

**Feature — Manager booking confirm/cancel/complete (a named "P0 pilot gap")** (functionally
complete, verified against real repository/API signatures; **no test coverage yet**):
- M `manager/screens/calendar/ManagerAppointmentDetailScreen.kt`
- ?? `manager/presentation/calendar/ManagerAppointmentDetailViewModel.kt`,
  `ManagerAppointmentDetailViewModelFactory.kt`

**Docs — new Rule Book** (purely additive, low risk):
- ?? `docs/architecture/RULE_BOOK/` (`README.md`, `CHANGELOG.md`, `ROJAN_RULE_BOOK_V2.md`) —
  see [DECISIONS.md](DECISIONS.md) for the numbering-collision this introduces

**Junk — not a real change:**
- ?? `how --stat --oneline 717dff4` — accidental redirect of `git show --stat --oneline` output
  into a file named after the tail of that command. Not source, not intentional.

## Baseline caveat

STATE.md / ARCHITECTURE.md / FEATURES.md in this Brain snapshot were generated **including**
the two uncommitted-but-complete feature slices above. Treat that content as provisional until
those changes are actually committed — it is a snapshot of this working tree, not of
`origin/main` or even `origin/feature/android-first-salon-pilot`.

## What this file does not do

It does not decide whether to commit, does not reconcile the branch divergence, and does not
get hand-edited to "catch up" — regenerate it from git instead.
