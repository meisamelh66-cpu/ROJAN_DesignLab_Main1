# ROJAN Independent Release Readiness Audit v1

**Auditor:** Independent QA, Security, Architecture & Release Audit (review-only — no code, config, or repository state was modified in the course of this audit)
**Date:** 2026-08-15
**Scope:** `C:\AndroidProjects\ROJAN_DesignLab` (System 2 / Android repo), branch `feature/android-reception-app` @ `1a3bdb0`
**Out of scope:** ROJAN_Backend (System 1) — no backend source is present in this repository. All backend-related findings below are inferred from the Android client's contracts and documentation only, and are explicitly marked as unverifiable from this vantage point.

---

## 1. Executive Summary

The Android codebase (three flavors on one `:app` module — `customer`, `manager`, `reception`, no `specialist` flavor exists yet) is in materially better shape than its own oldest report in the repo (`ROJAN_AI_Production_Readiness_Report.md`) suggests — that document is stale and contradicted by current code; it should be archived or deleted to stop misleading future readers. All three current flavors build successfully in debug (`assembleCustomerDevDebug`, `assembleManagerDevDebug`, `assembleReceptionDevDebug`), architecture layering is clean (no `android`/`androidx` imports leak into any `domain/` package), error handling is centralized and disciplined (zero swallowed exceptions found anywhere), and client-side security fundamentals (Keystore-encrypted token storage, gated release logging, narrow cleartext exceptions, no hardcoded secrets) are solid.

The two things most likely to matter at release time are **not** code-quality problems: (1) **backend dependencies the Reception app is client-ready for but the backend doesn't yet serve** (salon-access/membership persistence, the entire Invite feature, and non-owner authorization — meaning most Reception screens will 403 for their actual target users today), and (2) **near-total absence of test coverage on business-critical paths** — booking creation, token refresh, and every repository implementation across all three flavors. Neither is a "the app is broken" finding; both are "this has not been verified to work under real conditions" findings, which is a different and arguably more dangerous category going into a release.

A secondary, Android-only architectural inconsistency (Manager screens bypassing the ViewModel layer via a global mutable singleton) and a product/security ambiguity (Reception's access gate currently accepts any `MANAGER`-role account, with no distinct `RECEPTIONIST` role) are both worth explicit sign-off before wider rollout, even though neither is a broken-as-implemented defect.

No frozen design-baseline violations were found in the four most recent commits. No secrets, keystores, or credentials are tracked in git.

---

## 2. Architecture Findings

**Layering (Clean Architecture):** Clean. All three `domain/` trees (`domain/`, `manager/domain/`, `reception/domain/`, 77 files) have zero `android.*`/`androidx.*` imports. `domain/reminder/ReminderScheduler.kt` is a good example of the pattern done right — the domain layer declares only the interface and a no-op default, deferring any real `AlarmManager`/`WorkManager` use to the data layer.

**Repository pattern:** Followed consistently in Customer (15 interfaces / 15 impls) and mostly in Manager (6/6) and Reception (3 interfaces, 2 impls — the third, `ReceptionInviteRepository`, has no implementation because there is no backend for it yet, and is explicitly documented as typed-ahead scaffolding). Two deviations:
- `manager/data/BackendDashboardRepository.kt` has no domain-layer interface, unlike every sibling `Backend*Repository`.
- `ManagerInviteRepository` / `ReceptionInviteRepository` are unimplemented interfaces (intentional — see §6).

**Manager-flavor ViewModel bypass (the one substantive architecture finding):** 13 Manager screens/components (`ManagerDashboardScreen.kt`, `ManagerCalendarScreen.kt`, `ManagerCustomersListScreen.kt`, `ManagerCustomerProfileScreen.kt`, `ManagerCustomerEditScreen.kt`, `ManagerServicesScreen.kt`, `ManagerServiceEditScreen.kt`, `ManagerStaffScreen.kt`, `ManagerStaffEditScreen.kt`, `ManagerAppointmentDetailScreen.kt`, `ManagerBookingStartScreen.kt`, `TodayOverviewSection.kt`, `AIInsightCard.kt`, `SalonIdentityCard.kt`) read data directly from a global mutable singleton, `ManagerRepositories` (`manager/data/ManagerRepositories.kt`), instead of going through a ViewModel. `ManagerDashboardScreen.kt` goes further and calls `LocalContext.current` and triggers `ManagerRepositories.initialize(context)` — a network sync — directly inside a Composable `LaunchedEffect`. Reception's equivalent locator (`ReceptionRepositories.kt`) is, by contrast, only ever touched from `*ViewModelFactory` classes; Customer uses proper ViewModel+Factory+DI throughout. This is Manager-flavor-specific technical debt, not a repo-wide pattern, and is very likely a contributing cause of Manager's weak test coverage in this area (state that lives in a Composable-triggered singleton is much harder to unit test than a ViewModel).

**Networking layer:** Manual composition-root DI (`BackendApiContainer.kt` + `BackendApiContainerHolder.kt`, double-checked-locking singleton) — no Hilt/Koin/Dagger. `NetworkConfig.kt` correctly fails loudly (`check()`) if `BuildConfig.API_BASE_URL` is blank; `app/build.gradle.kts` additionally hard-gates any `ProductionRelease` assemble task at Gradle configuration time if `PRODUCTION_API_BASE_URL` isn't supplied (confirmed live — see §6). `HttpLoggingInterceptor` is `BASIC` in debug / `NONE` in release. No explicit OkHttp timeouts are set anywhere (implicit 10s defaults apply) — minor, not a defect. A small, contained architecture smell: 4 presentation-layer files (`ManagerBookingViewModel.kt`, `SalonRelationshipViewModel.kt`, `SalonListViewModel.kt`, `ReceptionBookingViewModel.kt`) import `data.remote` exception types directly for error-message mapping, rather than going through a domain-level sealed error type — scoped only to error-message mapping, not to data access itself.

**Module/flavor structure:** Matches CLAUDE.md's documented design exactly. `customer` has zero overrides; `manager`/`reception` each only set `applicationId` plus a thin flavor source set (`AndroidManifest.xml` + one `Activity.kt` + minor `res` overrides — both `Activity` classes are explicitly documented as containing no auth/session logic). No logic leakage into flavor source sets was found. **No `specialist` flavor exists anywhere in this codebase** — confirmed absent, not merely early-stage.

**Dependency versions:** Kotlin 2.2.10, AGP 9.3.1, Compose BOM 2026.06.00, Retrofit 2.11.0, OkHttp 4.12.0, Gradle 9.6.1 — internally consistent, no conflicting declarations, not stale for the stated project timeline.

---

## 3. Security Findings

**Auth flow:** All three flavors use phone + OTP against the real backend exclusively; OTP verification is entirely server-side, the client only submits phone+code and trusts the returned tokens. No client-side rate limiting exists (correctly deferred to backend, but unverifiable from the client alone). No hardcoded bypass/test credentials found anywhere. Note: `AuthApi`/`BackendAuthRepositoryImpl` still contain unused email+password `login`/`register` methods with zero call sites — dead code, not a live risk, but worth removing to reduce surface area. Likewise, `DemoIdentityProvider`/`DemoSessionProvider` are disconnected demo scaffolding (mock login/OTP methods removed, zero real callers) — harmless but should eventually be deleted.

**Token storage:** Strong. Access/refresh tokens are AES-256-GCM encrypted with a non-exportable Android-Keystore-backed key (`SecureTokenStore.kt`) before being written to disk; only IV-prefixed ciphertext ever reaches `SharedPreferences`. The one unencrypted item persisted (a person ID in DataStore) carries no secret material, so encrypting it would be unnecessary.

**Session/refresh handling:** A proper OkHttp `Authenticator` (`TokenAuthenticator.kt`) handles 401s via a non-intercepted refresh call, capped at 1 retry (no infinite-retry risk), and clears both tokens and the persisted person ID on a genuinely failed refresh.

**Logging:** `HttpLoggingInterceptor` is `BASIC` (method/URL/status/timing only, never headers or bodies) in debug and `NONE` in release, gated on `BuildConfig.DEBUG`. No stray `Log.d/v/i/w/e`, `println`, or `Timber` calls exist anywhere in `app/src/main`. **However, R8/ProGuard minification is explicitly disabled for release builds** (`optimization { enable = false }` in `app/build.gradle.kts`) — meaning the release APK ships fully unobfuscated and unshrunk, and the only thing preventing verbose logging in a shipped build is the `BuildConfig.DEBUG` flag being set correctly at build time, not a compiler-enforced strip. This is a real hardening gap worth closing before a genuine production release (both for log-safety-in-depth and for reverse-engineering resistance / APK size).

**RBAC / access gating — Reception role ambiguity:** The most notable product-security finding. `ReceptionAuthViewModel.kt` defines `RECEPTION_GATE_ROLE = "MANAGER"` — there is currently no distinct `RECEPTIONIST` role; **any account with the `MANAGER` role can authenticate into either the Manager app or the Reception app**, with the intended per-salon differentiation (owner/staff/reception) deferred entirely to server-side `SalonMembership.role`, which this client never checks. This is documented in the code as a confirmed System 1 (backend) decision, not an oversight — but it means the two apps are not access-isolated from each other today, which is worth an explicit, current confirmation from System 1 before Reception rolls out more broadly, since it's easy for this kind of "confirmed decision" to go stale as the role model evolves. Elsewhere, all other client-side role checks are correctly framed in code comments as UX convenience only, never as the actual security boundary — server responses (401/403/404/409) are treated as the source of truth throughout.

**Tenant/salon isolation:** `salonId` is passed as a client-supplied Retrofit path/query parameter on nearly every Manager endpoint, with DTO doc comments annotating expected owner-only authorization. This is standard REST practice and consistent client-side, but — since backend code isn't in this repo — **whether the backend actually enforces salon-scoped authorization on every one of these routes cannot be verified from here** and should not be assumed proven by this audit.

**Secrets / config hygiene:** No hardcoded API keys, secrets, or Bearer values found anywhere. `.gitignore` correctly excludes `local.properties`, `*.jks`/`*.keystore`, `keystore.properties`; none are tracked in git (confirmed via `git ls-files`). `network_security_config.xml` grants cleartext exceptions only to `10.0.2.2`/`localhost`/`127.0.0.1`, lives exclusively under `src/debug/`, and is confirmed never merged into release.

---

## 4. Code Quality Findings

**Error handling:** Genuinely strong. A single wrapper, `SafeApiCall.kt`, is used by essentially all repository network calls and converts every failure mode (`HttpException`, `SocketTimeoutException`, other `IOException`) into a typed `Result<T>`. A repo-wide grep for `catch (e: Exception)` / `catch (e: Throwable)` across `app/src/main` returned **zero matches outside `SafeApiCall.kt` itself** — no swallowed exceptions were found anywhere sampled.

**Duplication:** Low overall, with one deliberate, acknowledged exception: the Manager and Reception auth-flow screens/ViewModels (`ManagerAccessErrorScreen.kt`/`ReceptionAccessErrorScreen.kt`, `ManagerSalonSelectionScreen.kt`/`ReceptionSalonSelectionScreen.kt`, `ManagerOtpAuthScreen.kt`/`ReceptionOtpAuthScreen.kt`, and the two auth ViewModels) are near-line-for-line duplicates, mirrored intentionally per their own code comments but never consolidated into a shared base. The project's own internal reports separately flag two more instances of the same trade-off (duplicated OTP-flow logic across 3 ViewModels; duplicated `refreshIdentityContext`/`retryIdentityResolution`/`resolveActiveSalon` between Manager and Reception), explicitly deferred to a future scoped task rather than left unnoticed. A prior duplicate-DTO defect (`CreateSpecialistRequestDto`/`UpdateSpecialistRequestDto`) was already fixed (commit `19cb512`) and no similar pattern recurs elsewhere.

**Comments/markers:** Zero `TODO`/`FIXME`/`HACK`/`XXX` markers anywhere in `app/src`. Either genuinely clean, or work items are tracked entirely outside the code (in the many root-level report `.md` files) rather than inline — worth noting as a process observation, not a defect.

**Maintainability:** `navigation/RojanNavGraph.kt` at 1,154 lines is a clear outlier (more than double the next-largest file, `SalonDetailsScreen.kt` at 667 lines) and is a reasonable decomposition candidate independent of any design-system concern. Package structure mirrors cleanly across flavors (Reception is a consistent, narrower subset of Manager's screen areas, matching its intentionally reduced scope).

**Repository-doc hygiene:** The repo root carries ~25 historical report `.md` files spanning the project's whole recent history. Most recent/final reports per topic are internally consistent and verified accurate against current code (spot-checked). One exception: `ROJAN_AI_Production_Readiness_Report.md` is chronologically stale — it describes a "placeholder-only" Manager Dashboard and a broken build that no longer reflects reality at all — and should be archived or deleted so it can't mislead a future reader (human or AI) into acting on outdated claims.

---

## 5. Test Coverage Findings

This is the largest gap identified in the audit. Total: **23 test files** (19 unit under `app/src/test`, 4 instrumented under `app/src/androidTest`) against **359 source files** under `app/src/main/java`.

- Of 25 total `*ViewModel.kt` files, only **6** have a matching test.
- **Zero test coverage** on every repository implementation across all three flavors — including `TokenRepositoryImpl.kt` and `AuthInterceptor.kt` (the token-refresh/auth-header path), `BookingRepositoryImpl.kt` (booking creation/cancel/confirm/complete/reschedule), and every `Backend*Repository` in Manager and Reception.
- The Customer app's **entire booking-flow ViewModel set** is untested (`BookingViewModel`, `BookingConfirmationViewModel`, `BookingDateViewModel`, `BookingTimeViewModel`, `AppointmentDetailsViewModel`, `RescheduleViewModel`, `BookingHistoryViewModel`, plus `SalonDetailsViewModel`, `SpecialistProfileViewModel`, and others).
- `androidTest` coverage is **exclusively screenshot/visual-regression tests** (`AuthScreenScreenshotTest`, `CustomerThemeScreenshotTest`, `ManagerDashboardScreenshotTest`) — there are **zero interaction-driven Compose UI tests** (no `performClick`-style flows) anywhere, and **zero Reception instrumented tests** of any kind.
- What *is* tested is reasonably targeted: auth ViewModels (Customer/Manager/Reception), phone normalization, identity-context resolution, salon-relationship logic, CRM AI insight rule providers, error-message mapping, and a structural `ArchitectureRulesTest` lint.

Net effect: the parts of the app that move money/bookings and manage sessions — the parts where a silent regression would be most costly — are currently the least covered.

---

## 6. Release Blockers

Ordered roughly by what would actually stop a real production release, distinguishing Android-side readiness from backend dependency:

1. **Reception feature is backend-blocked, not Android-blocked.** `SalonMembership` persistence, `GET /users/me/salon-access`, and the entire Invite feature (`SalonInvite`/`InviteController`) do not exist on the backend. Authorization is currently owner-only, so **every non-owner Reception screen will return real 403s** for the app's actual target users today. The Android client is verified ready and waiting (clean architecture, no mocks, `assembleReceptionDevDebug` passes, 112/114 tests pass with the 2 failures being pre-existing and network-dependent) — this is squarely a System 1 dependency, explicitly documented as such in the project's own Reception Phase 1 Final Acceptance report.
2. **No confirmed staging or production backend deployment.** `STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL` are unset in this checkout. Production release is correctly hard-gated at Gradle configuration time and cannot be built without the URL. **Staging is not equivalently gated** — `assembleCustomerStagingRelease` builds successfully (unsigned, confirmed by this audit) even with a blank URL, deferring the failure to app runtime instead of build time. This is an inconsistency worth closing so a broken staging build can't be produced and handed out.
3. **No release signing keystore present in this environment.** Expected and correctly gitignored — release builds fall back to unsigned rather than hard-failing (verified working as documented) — but real signing material must be provisioned through a secure channel before an actual distributable release build exists.
4. **Test coverage gaps on critical paths** (booking lifecycle, token refresh/repository layer, all Manager/Reception backend repositories) mean regressions in these flows would not be caught automatically before release.
5. **Two branches sit well ahead of `main`, unmerged**: `feature/android-reception-app` (current) is 46 commits ahead of `origin/main`, 0 behind; `feature/manager-backend-integration` has 20 additional commits not yet pushed to its own remote. Nothing described in this audit has actually reached `main` — a merge/release-train decision is a prerequisite to any release, independent of code readiness.
6. **Reception ships with placeholder branding** — it inherits the generic Customer launcher icon (`@mipmap/ic_launcher`); no Reception-specific art exists yet. Already known and documented in-code as a placeholder, not an oversight, but real art is needed before a genuine Reception app launch.
7. **R8/ProGuard minification is disabled for all release builds** — not a hard blocker, but a hardening gap (obfuscation, shrinking, defense-in-depth for the debug-flag-gated logging) that should be closed before a real production ship.
8. **Environment/tooling documentation is out of date on this machine**: CLAUDE.md's documented Android SDK path and the `Pixel_4` AVD do not exist here; the actual working SDK path differs (per `local.properties`), and no AVD is present at all, so no live device/emulator verification (RQG step 5) was possible during this audit. This blocks visual/manual QA sign-off specifically, not the build itself.

**No `specialist` flavor exists in this repo at all** — it is not a near-term release candidate; there is nothing to evaluate for it.

---

## 7. Risk Levels

**High**
- Reception app is functionally blocked for its real target users (non-owner roles get 403s) pending System 1 backend work — Invite feature has no backend at all.
- Test coverage is effectively absent on booking creation/lifecycle, token refresh, and every repository implementation — the highest-value paths in the app.
- Two feature branches significantly diverged from `main` and from each other's remotes — a release-process risk independent of code quality.
- Reception's access gate currently accepts any `MANAGER`-role account with no distinct `RECEPTIONIST` role — needs explicit, current (not historical) confirmation from System 1 before wider rollout, since it means the two apps aren't access-isolated from each other today.

**Medium**
- Manager-flavor screens bypass the ViewModel layer via a global mutable singleton, including triggering network calls directly from Composables — architecture debt that also likely explains Manager's weak testability in that area.
- R8/ProGuard disabled for release builds — hardening/obfuscation gap.
- Staging release lacks the build-time hard-gate that Production has, allowing an unsigned, misconfigured staging build to be produced without immediate failure.
- Stale `ROJAN_AI_Production_Readiness_Report.md` in the repo contradicts current state and could mislead a future reader.
- `RojanNavGraph.kt` at 1,154 lines is a maintainability outlier.
- Deliberate, acknowledged duplication (Manager/Reception auth screens+ViewModels, identity-resolution logic) remains unconsolidated.
- Backend authorization enforcement for salon/tenant-scoped Manager endpoints cannot be verified from the Android client alone.

**Low**
- Dead demo-auth scaffolding and unused email/password auth methods (disconnected, zero call sites, no live risk).
- No explicit OkHttp timeouts configured (implicit defaults apply).
- Minor data-layer exception types leaking into presentation-layer error mapping (4 files, scoped to message formatting only).
- Reception's placeholder launcher icon (already known/documented, not an oversight).
- Local environment/tooling docs (SDK path, AVD) out of sync with this machine's actual state.

**Critical**
- None found within Android-side code as implemented. The closest candidate — Reception's backend dependencies — is correctly attributed to System 1 scope rather than an Android defect, but functionally blocks the feature from working for real users today and should be tracked with Critical urgency at the cross-team level even though it isn't an Android bug.

---

## 8. Recommendations

1. Treat the Reception backend dependencies (salon-access/membership persistence, Invite backend, non-owner authorization) as the top cross-team priority — the Android side has been verified ready and is currently idle waiting on System 1.
2. Get explicit, current confirmation from System 1 on the Reception `MANAGER`-role access-gate design before expanding Reception rollout — confirm it's still the intended model, not a historical decision that's since drifted.
3. Prioritize unit tests for the booking lifecycle, token refresh/`TokenRepositoryImpl`/`AuthInterceptor`, and the Manager/Reception backend repository implementations before relying on this app for a real release — these are the highest-value, currently-least-covered paths.
4. Add a build-time hard-gate for `StagingRelease` matching the one already in place for `ProductionRelease`, so a misconfigured staging build can't be produced silently.
5. Refactor the 13 Manager screens/components off the `ManagerRepositories` global singleton and onto the ViewModel+Factory pattern already used consistently by Customer and Reception — this will also make that code testable, closing part of the coverage gap.
6. Re-enable R8/ProGuard for release builds (with appropriate keep rules already scaffolded in `app/proguard-rules.pro`) before a genuine production ship.
7. Archive or delete `ROJAN_AI_Production_Readiness_Report.md` (and consider consolidating the ~25 root-level historical reports into a `docs/reports/` or similar location) to prevent stale claims from being read as current state.
8. Resolve the branch divergence — decide and execute a merge/release-train plan for `feature/android-reception-app` and `feature/manager-backend-integration` relative to `main`.
9. Update the local environment documentation (SDK path, AVD availability) to match actual machine state, and provision a working AVD (or confirm physical-device access) so future audits can complete live device/emulator verification (RQG step 5).
10. Schedule the two already-acknowledged duplication cleanups (Manager/Reception auth screens, identity-resolution logic) as an explicitly scoped follow-up task, as the project's own reports already recommend.

---

*This report is a point-in-time audit artifact. No source code, configuration, or git history was modified in producing it. All backend-side (System 1) claims are inferred from the Android client only and are marked as unverifiable where the underlying backend code could not be reviewed.*
