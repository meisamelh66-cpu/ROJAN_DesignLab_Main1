# ARCHITECTURE — Actual Code Layering

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: architecture as observed in code, not aspirational. Intended rules: `docs/architecture/03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md`. Behavioral constraints (frozen baselines, do-not-touch lists): root `CLAUDE.md`.

## Pattern

Clean Architecture + MVVM. `domain/` has zero Android imports — mechanically enforced by
`ArchitectureRulesTest.kt`. `data/` implements domain repository interfaces. `presentation/` +
`screens/` pair a ViewModel with Compose UI. `navigation/` is Compose Navigation, one
`*NavGraph.kt` / `*Destinations` object per flavor (deliberately not shared).

## Per-flavor structure

Single `:app` module. Under `app/src/main/java/ai/rojan/designlab/`, shared code
(`data/`, `domain/`, `navigation/`, `presentation/`, `screens/`, `ui/`) sits alongside two
parallel flavor-scoped trees mirroring the same shape:

- `manager/` — own `data/`, `domain/`, `navigation/`, `presentation/`, `screens/`, `viewmodel/`
- `reception/` — same shape, smaller

## Use-case layer

Named and scaffolded (`domain/usecase/relationship/`) but barely used in practice — only
1 file matches `*UseCase*`. Most business logic lives directly in ViewModels/Repositories.

## Known standing deviation — Manager singleton bypass

13 Manager screens (27 call sites) read the global mutable `ManagerRepositories` singleton
directly from Composables — including triggering network calls from inside UI code — instead
of going through a ViewModel, the pattern Customer and Reception both follow correctly.

- First flagged 2026-08-15 in the repo's own `ROJAN_Independent_Release_Readiness_Audit_v1.md`.
- Still present at HEAD as of this snapshot (2026-09-03).
- **Partial exception observed in the current uncommitted work**: `ManagerAppointmentDetailScreen`
  now routes its confirm/cancel/complete actions through a real
  `ManagerAppointmentDetailViewModel` — but the same screen still reads customer/service/specialist
  display data directly from `ManagerRepositories`. A partial migration, not a full fix. See
  [RISKS.md](RISKS.md) R-02 and [SESSION.md](SESSION.md) for the uncommitted state this refers to.

This is tracked as ongoing debt, not re-litigated here each session — don't rediscover it from
scratch; check whether it's still true before relying on it.
