# ROJAN QA Remediation Plan v1

**Based on:** `ROJAN_Independent_Release_Readiness_Audit_v1.md`
**Date:** 2026-08-15
**Scope:** `C:\AndroidProjects\ROJAN_DesignLab` (System 2 / Android repo), branch `feature/android-reception-app` @ `1a3bdb0`
**Status:** Planning only — no code, config, or repository state changed in producing this document.

**Ownership key:** every item is tagged **[S2]** (Android — this repo, actionable now) or **[S1]** (Backend — requires System 1 work or sign-off, per CLAUDE.md's System 1/System 2 boundary). Items that need a *cross-team decision* rather than pure implementation are tagged **[S1↔S2]**.

---

## How This Plan Is Organized

1. **Android fixes owned by System 2** — things this repo can fix unilaterally, no backend or cross-team dependency.
2. **Backend dependencies for System 1** — things System 2 is blocked on; nothing to implement here until System 1 delivers.
3. **Release blockers** — the full gating list from the audit's §6, each tagged with owner, so it's clear which blockers System 2 can clear alone vs. which need System 1 or a cross-team decision.
4. **Test improvement priorities** — ordered remediation of the audit's §5 coverage gaps, all System 2, sequenced by business risk.

Each item references its source in the audit (§ section) for traceability.

---

## 1. Android Fixes Owned by System 2

Ordered by priority. All of these are actionable inside this repo without waiting on System 1.

### P0 — Architecture / Correctness

**1.1 Refactor Manager's `ManagerRepositories` singleton bypass onto ViewModel+Factory** *(audit §2, §7-Medium, Recommendation 5)*
- 13 screens/components read state directly from a global mutable singleton instead of a ViewModel; `ManagerDashboardScreen.kt` additionally triggers a network sync from inside a `LaunchedEffect`.
- Bring Manager in line with the pattern Customer and Reception already use consistently (`ReceptionRepositories` is only ever touched from `*ViewModelFactory` classes).
- This is both an architecture-consistency fix and a **test-coverage enabler** — see §4 below; sequence it before attempting to unit-test those 13 screens, since a Composable-triggered singleton is not unit-testable as-is.
- Files: `ManagerDashboardScreen.kt`, `ManagerCalendarScreen.kt`, `ManagerCustomersListScreen.kt`, `ManagerCustomerProfileScreen.kt`, `ManagerCustomerEditScreen.kt`, `ManagerServicesScreen.kt`, `ManagerServiceEditScreen.kt`, `ManagerStaffScreen.kt`, `ManagerStaffEditScreen.kt`, `ManagerAppointmentDetailScreen.kt`, `ManagerBookingStartScreen.kt`, `TodayOverviewSection.kt`, `AIInsightCard.kt`, `SalonIdentityCard.kt`.
- This is an **architecture change** per CLAUDE.md ("crossing module boundaries" is explicitly called out; this is narrower — presentation-layer pattern only — but touches 13 files broadly enough to warrant explicit confirmation before starting, per CLAUDE.md's "before major architectural changes, ask for confirmation").

**1.2 Add build-time hard-gate for `StagingRelease`** *(audit §6 item 2, Recommendation 4)*
- `assembleCustomerStagingRelease` currently builds successfully even with a blank `STAGING_API_BASE_URL`, deferring failure to runtime. `ProductionRelease` already has the correct Gradle-configuration-time `check()` gate.
- Mirror the existing Production gate onto Staging in `app/build.gradle.kts`. Small, contained, no design-system or architecture impact.

### P1 — Hardening / Maintainability

**1.3 Re-enable R8/ProGuard for release builds** *(audit §3, §6 item 7, §7-Medium, Recommendation 6)*
- `optimization { enable = false }` in `app/build.gradle.kts` ships release APKs fully unobfuscated and unshrunk; the only thing preventing verbose logging in a shipped build today is the `BuildConfig.DEBUG` flag, not a compiler-enforced strip.
- Keep rules are reportedly already scaffolded in `app/proguard-rules.pro` — verify they're current, re-enable, then rebuild all three release variants and confirm no runtime breakage (reflection-based libraries, Retrofit/Moshi models, etc. are the usual failure points).

**1.4 Decompose `RojanNavGraph.kt`** *(audit §4, §7-Medium)*
- 1,154 lines, more than double the next-largest file (`SalonDetailsScreen.kt` at 667). Reasonable decomposition candidate (e.g. per-flavor or per-feature nav graph files) independent of any design-system concern.
- Falls under CLAUDE.md's "standard refactoring inside the current task's approved scope" — proceed without separate sign-off once scheduled, but scope the split carefully since navigation graphs are easy to subtly break.

**1.5 Consolidate acknowledged Manager/Reception duplication** *(audit §4, §7-Medium, Recommendation 10)*
- Near-line-for-line duplicates: `ManagerAccessErrorScreen.kt`/`ReceptionAccessErrorScreen.kt`, `ManagerSalonSelectionScreen.kt`/`ReceptionSalonSelectionScreen.kt`, `ManagerOtpAuthScreen.kt`/`ReceptionOtpAuthScreen.kt`, the two auth ViewModels, plus duplicated `refreshIdentityContext`/`retryIdentityResolution`/`resolveActiveSalon` logic.
- The project's own prior reports already flag this and defer it — treat as a single scoped follow-up task, not folded into unrelated work.

**1.6 Archive/delete stale report doc; consolidate report sprawl** *(audit §4, §7-Medium, Recommendation 7)*
- `ROJAN_AI_Production_Readiness_Report.md` describes a "placeholder-only" Manager Dashboard and a broken build that no longer reflects reality — archive or delete so it can't mislead a future reader (human or AI).
- Consider consolidating the ~25 root-level historical report `.md` files into `docs/reports/` for hygiene. Low risk, doc-only — falls under CLAUDE.md's "documentation updates" automatic-action category.

### P2 — Low-Risk Cleanup

**1.7 Remove dead auth scaffolding** *(audit §3, §7-Low)*
- Unused email/password `login`/`register` methods in `AuthApi`/`BackendAuthRepositoryImpl` (zero call sites) and disconnected `DemoIdentityProvider`/`DemoSessionProvider` (zero real callers).
- No live risk today, but reduces attack surface and code-reading confusion. Per CLAUDE.md, deleting files requires confirmation even when clearly dead — flag explicitly before removing rather than folding into another change.

**1.8 Add explicit OkHttp timeouts** *(audit §2, §7-Low)*
- No explicit connect/read/write timeouts are set anywhere; implicit 10s OkHttp defaults apply. Not a defect, but worth setting explicitly and deliberately (especially for booking-mutation calls) rather than relying on library defaults.

**1.9 Move data-layer exception types out of presentation-layer error mapping** *(audit §2, §7-Low)*
- 4 files (`ManagerBookingViewModel.kt`, `SalonRelationshipViewModel.kt`, `SalonListViewModel.kt`, `ReceptionBookingViewModel.kt`) import `data.remote` exception types directly for error-message mapping. Scoped only to message formatting, not data access — introduce a domain-level sealed error type these can map through instead.

**1.10 Reception launcher icon / branding art** *(audit §6 item 6, §7-Low)*
- Reception currently inherits the generic Customer `@mipmap/ic_launcher`. Already documented in-code as a placeholder, not an oversight. Needs real art before a genuine Reception launch — an asset/design task, not urgent for internal testing.

### Process / Local Environment (not code, but System 2-actionable)

**1.11 Update local environment/tooling docs** *(audit §6 item 8, §7-Low, Recommendation 9)*
- CLAUDE.md's documented Android SDK path and `Pixel_4` AVD don't exist on this machine; actual SDK path differs per `local.properties`, and no AVD is present, blocking live device/emulator verification (RQG step 5).
- Update CLAUDE.md's environment notes to match actual machine state, and provision a working AVD or confirm physical-device access so future work can complete visual verification.

---

## 2. Backend Dependencies for System 1

Nothing here is actionable from this repo. Listed so System 2 work isn't silently blocked without visibility, and so these can be handed to System 1 as a tracked list.

**2.1 `SalonMembership` persistence + `GET /users/me/salon-access`** *(audit §6 item 1)*
- Does not exist on the backend today. This is the root cause of every non-owner Reception screen returning 403 for real target users. The Android client is verified ready and idle waiting on this (clean architecture, no mocks, `assembleReceptionDevDebug` passes).

**2.2 Invite feature backend (`SalonInvite`/`InviteController`)** *(audit §2, §6 item 1)*
- `ReceptionInviteRepository` has no implementation because there is no backend for it — explicitly documented as typed-ahead client scaffolding, not an Android gap.

**2.3 Non-owner authorization enforcement** *(audit §6 item 1)*
- Authorization is currently owner-only. Reception's entire premise (staff/reception roles accessing salon data) is blocked until this exists.

**2.4 Staging/production backend deployment** *(audit §6 item 2)*
- `STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL` are unset in this checkout. This is infra/deployment, not Android code — System 2 can only build the client-side gate correctly (see 1.2), not stand up the environments themselves.

**2.5 Confirm salon/tenant-scoped authorization is enforced on every Manager endpoint** *(audit §3, §7-Medium)*
- `salonId` is passed as a client-supplied path/query parameter on nearly every Manager endpoint. DTO comments annotate expected owner-only authorization, but whether the backend actually enforces this on every route **cannot be verified from the Android client** and should not be assumed proven by the audit. Request explicit confirmation or a backend-side audit from System 1.

---

## 3. Release Blockers

Full list from audit §6, each tagged with owner so it's clear what System 2 can clear independently vs. what's gated on System 1 or a cross-team decision. Ordered roughly by what would actually stop a real production release.

| # | Blocker | Owner | Notes |
|---|---------|-------|-------|
| 1 | Reception is backend-blocked — `SalonMembership`, Invite feature, non-owner auth don't exist server-side; non-owner Reception screens 403 today | **[S1]** | Android side verified ready; System 1's top priority per audit Recommendation 1 |
| 2 | Reception's `MANAGER`-role access gate (`RECEPTION_GATE_ROLE = "MANAGER"`, no distinct `RECEPTIONIST` role) needs explicit, *current* confirmation before wider rollout | **[S1↔S2]** | Documented in code as a confirmed System 1 decision, but the audit flags it can go stale as the role model evolves — needs re-confirmation, not re-implementation, unless System 1 says otherwise |
| 3 | No confirmed staging/production backend deployment | **[S1]** | Infra/deployment |
| 3a | Staging build lacks the build-time hard-gate Production has (unsigned/misconfigured staging build can be produced silently) | **[S2]** | = Fix 1.2 above; System 2 can close this independent of #3 |
| 4 | No release signing keystore present in this environment | **N/A (ops)** | Expected/correctly gitignored; real signing material needs provisioning through a secure channel — not a code fix, an ops/secrets-management action outside both systems' code scope |
| 5 | Test coverage gaps on booking lifecycle, token refresh, all Manager/Reception backend repositories | **[S2]** | = Test Improvement Priorities, §4 below |
| 6 | Two branches diverged from `main` and from each other: `feature/android-reception-app` (46 commits ahead of `origin/main`), `feature/manager-backend-integration` (20 commits not pushed to its own remote) | **[S2]** | Release-process decision — merge/release-train plan needed; nothing in this audit has reached `main` yet |
| 7 | Reception ships with placeholder branding (generic Customer launcher icon) | **[S2]** | = Fix 1.10 above; known placeholder, not urgent for internal testing |
| 8 | R8/ProGuard disabled for release builds | **[S2]** | = Fix 1.3 above; hardening gap, not a hard blocker |
| 9 | Local environment/tooling docs out of date (SDK path, AVD absent) — blocks live device/emulator RQG verification on this machine | **[S2]** | = Fix 1.11 above |

**Reading this table:** items 5, 6, 7, 8, 9, and 3a are fully within System 2's control and can be scheduled now. Items 1 and 3 require System 1 delivery. Item 2 requires a cross-team confirmation (likely fast — a sign-off, not new work). Item 4 sits outside both teams' code scope (secrets/ops).

**No `specialist` flavor exists in this repo** — not a release candidate for either system; nothing to plan for it here.

---

## 4. Test Improvement Priorities

All **[S2]**. Ordered by business risk per the audit's own framing: "the parts of the app that move money/bookings and manage sessions... are currently the least covered." Current baseline: 23 test files (19 unit, 4 instrumented) against 359 source files; only 6 of 25 `*ViewModel.kt` files have a matching test.

**P0 — Highest business risk, currently zero coverage**
1. **Token refresh / auth path**: `TokenRepositoryImpl.kt`, `AuthInterceptor.kt`. A silent regression here breaks every authenticated request across all three flavors.
2. **Booking lifecycle**: `BookingRepositoryImpl.kt` (create/cancel/confirm/complete/reschedule) plus the Customer booking-flow ViewModel set — `BookingViewModel`, `BookingConfirmationViewModel`, `BookingDateViewModel`, `BookingTimeViewModel`, `AppointmentDetailsViewModel`, `RescheduleViewModel`, `BookingHistoryViewModel`. This is the app's core revenue-generating path and is entirely untested today.

**P1 — High-value, currently zero or near-zero coverage**
3. **Manager/Reception backend repository implementations**: every `Backend*Repository` in both flavors has zero test coverage.
4. **Interaction-driven Compose UI tests**: `androidTest` today is exclusively screenshot/visual-regression (`AuthScreenScreenshotTest`, `CustomerThemeScreenshotTest`, `ManagerDashboardScreenshotTest`) — zero `performClick`-style flow tests exist anywhere. Start with the booking flow and auth flow, the two highest-risk interaction paths.

**P2 — Coverage completion**
5. **Remaining untested ViewModels**: 19 of 25 `*ViewModel.kt` files, including `SalonDetailsViewModel`, `SpecialistProfileViewModel`, and others not already covered above.
6. **Reception instrumented tests**: currently zero of any kind (screenshot or interaction).

**Sequencing dependency:** completing fix **1.1** (Manager ViewModel-bypass refactor) is a prerequisite for meaningfully unit-testing the 13 affected Manager screens/components — a Composable-triggered global singleton is not practically unit-testable as-is, per the audit's own observation. Sequence 1.1 before attempting P1 item 3 for Manager specifically.

**What's already reasonably covered** (no action needed): auth ViewModels (Customer/Manager/Reception), phone normalization, identity-context resolution, salon-relationship logic, CRM AI insight rule providers, error-message mapping, `ArchitectureRulesTest` structural lint.

---

## Suggested Sequencing Across All Four Tracks

This is a suggested order, not a schedule — actual sequencing is a scheduling decision for the user/team.

1. **Immediately, no dependencies**: 1.2 (staging gate), 1.6 (archive stale report), 1.11 (env docs) — all small, isolated, no design or architecture risk.
2. **Cross-team, low effort**: 2.5 confirmation request to System 1 (salon-scoped auth enforcement), blocker #2 re-confirmation (Reception role gate) — these just need someone to ask System 1, not implementation work.
3. **Before relying on this app for a real release**: P0 test coverage (§4.1–4.2) and 1.1 (Manager refactor, which unblocks P1 test item 3 for Manager) — the audit's own framing is that these are the highest-value gaps.
4. **Before a genuine production ship**: 1.3 (R8/ProGuard), release signing keystore provisioning (ops), branch-divergence/merge decision (blocker #6).
5. **Whenever convenient, low risk**: 1.4, 1.5, 1.7, 1.8, 1.9, 1.10.
6. **Ongoing, blocked on System 1**: §2 items — track but cannot start.

---

*This plan is a point-in-time planning artifact derived from `ROJAN_Independent_Release_Readiness_Audit_v1.md`. No source code, configuration, or git history was modified in producing it.*
