# ROJAN Release Context

> Lightweight checkpoint file. Read this first on every resume. Do NOT
> re-audit the whole repo — continue from NEXT ACTION only.

## Repositories

- Android: `D:\AndroidProjects\ROJAN_DesignLab_Main1`
- Backend: `D:\AndroidProjects\ROJAN_Backend` (exists, not yet inspected this session)

## Git State (as of 2026-09-26)

- Current branch: `main`
- HEAD: `23ce03bdbcda20cc2c1015bdf091192aa9abb1d6`
- origin/main: `23ce03bdbcda20cc2c1015bdf091192aa9abb1d6`
- HEAD == origin/main — no local commits ahead. All in-progress work exists
  only as **untracked files** (nothing staged, nothing committed).
- Recent commits on main: build/lint/R8 fixes, customer session fix,
  slogan update, loading/profile fixes — no Manager-customers or
  SMS-OTP-autofill work has landed on main yet.

## Current Phase

Phase 0 — Checkpoint initialization. No release build/QA work has started
this session yet. This file did not exist before now; created per the
START/RESUME protocol.

## Untracked Work Found (not yet reviewed/staged/committed)

Large set of untracked files from prior session(s) — grouped:

- **Manager customers feature** (new, untracked):
  `app/src/main/java/ai/rojan/designlab/manager/presentation/customers/`
  (`ManagerCustomersViewModel(+Factory)`, `ManagerCustomerProfileViewModel(+Factory)`),
  plus DTOs `CustomerNoteResponseDto.kt`, `SalonCustomerResponseDto.kt`,
  plus UI components `ManagerBookingStepIndicator.kt`, `ManagerTextField.kt`.
  Matching tests exist at
  `app/src/test/java/ai/rojan/designlab/manager/presentation/customers/`
  (`ManagerCustomerProfileViewModelTest.kt`, `ManagerCustomersViewModelTest.kt`)
  — flagged by the user for review before inclusion, not yet reviewed.
- **Manager OTP autofill**: `manager/screens/auth/SmsOtpRetriever.kt` +
  test `manager/screens/auth/SmsOtpRetrieverTest.kt`. Code read this
  session — uses Google's `SmsRetriever` API (no READ_SMS/RECEIVE_SMS
  permission), looks production-intent (references real backend
  `RequestOtpUseCase.buildMessage` / `OtpPolicy.smsRetrieverAppHash`
  contract in its doc comments).
  **BLOCKER**: `com.google.android.gms.auth.api.phone.SmsRetriever` is
  used but **no `play-services-auth` (or any `com.google.android.gms`)
  dependency exists anywhere in the Gradle build files** (`app/build.gradle.kts`
  checked, no `.toml`/`.kts` match repo-wide). This will not compile as-is.
  Per instruction #9, do NOT wire `SmsOtpRetrieverTest.kt` into the real
  build until the dependency is added AND the feature compiles for real —
  currently neither is true.
- **Root-level report/planning docs** (untracked, not part of source):
  `MANAGER-AUTH-OTP-REQUIREMENTS.md`, `MANAGER-OTP-RECONCILIATION-PLAN.md`,
  `MANAGER-VISUAL-AUDIT.md`, `POST_ORIGIN_SOURCE_DIFF.txt`,
  `REAL_SOURCE_DIFF.txt` — likely prior-session working notes, not yet read.
- **`docs/architecture/*` (new files)** — a large parallel architecture-doc
  set (`ROJAN_7_WORKSTREAMS.md`, `ROJAN_DECISION_LOG.md`,
  `ROJAN_UNIFIED_ECOSYSTEM_ARCHITECTURE.md`, etc. + `workstreams/`
  subfolder). Not yet read/reconciled against `governance/` (Tier-0) or
  the existing Persian `docs/architecture/` set that CLAUDE.md says is
  superseded.
- Misc noise: stray file literally named
  `"-files --others --exclude-standard"` (likely an accidental shell
  redirection artifact, not real content), `.claude-flow/`,
  `.playwright-mcp/`, several `.claude/skills/*` directories.

## Scope Decision Findings (read-only, from MANAGER-AUTH-OTP-REQUIREMENTS.md + MANAGER-OTP-RECONCILIATION-PLAN.md)

- **In scope**: Manager email/password auth (already real, working, on `main`).
  Manager Customers list/profile/edit (already committed on `main` with real
  backend integration — `BackendCustomerRepository`, `CustomerRepository`,
  CRM insight providers; see git log `9fde757`, `9ac96e3`, `0d98eb4`, etc.
  Treated as frozen/shipped scope per CLAUDE.md).
- **Out of scope**: Real phone+OTP auth for Manager. Requirements doc: never
  built against `main`'s backend contract. Reconciliation doc (later,
  more informed): a complete real OTP implementation exists only on
  `feature/android-first-salon-pilot` (diverged 472 files from `main`);
  recommends a careful 4-phase/18-file manual port — not a quick wire-up.
  Nothing from that plan has been applied to `main`. `SmsOtpRetriever.kt`
  (untracked) is consequently orphaned — no OTP screen exists on `main` to
  attach it to, on top of its missing `play-services-auth` dependency
  (P0 blocker, already recorded above).
- **SmsRetriever/OTP autofill required for production?** No — dead code
  today, non-compiling as-is, no real OTP flow to attach to.
- **Manager Customers required for production?** Yes, and already shipped
  on `main`. The untracked `manager/presentation/customers/*ViewModel*`
  files are a separate, unreviewed, in-progress addition on top of the
  already-working feature — not required to ship what's already there.
- **Prerequisites if OTP is ever pursued**: backend must expose
  `/api/v1/auth/otp/request`, `/api/v1/auth/otp/verify`,
  `GET /users/me/salon-access` on the actual release-target backend
  (only verified previously against `release/v1.2.0-manager-dashboard-rbac-fix`,
  not re-confirmed against today's deployed backend). Separately flagged,
  unrelated to OTP: no backend path issues a `MANAGER`-role account today
  (`register()` hardcodes `CUSTOMER`) — a product/backend gap for System 1,
  not an Android fix.
- **Recommendation given to user**: exclude OTP + `SmsOtpRetriever` from
  this release entirely; focus on reviewing the two new Manager-Customers
  ViewModel test files (real vs. mock data), then get a clean
  `assembleDebug` baseline with only confirmed-real work included.
  **Awaiting user approval — no action taken yet.**

## Completed Work (this session)

- Confirmed no prior `ROJAN_RELEASE_CONTEXT.md` existed — created it now.
- Ran `git status`, branch, HEAD, origin/main — recorded above.
- Confirmed `ROJAN_Backend` repo path exists on disk (not yet inspected).
- Read `SmsOtpRetriever.kt` in full; confirmed missing `play-services-auth`
  dependency (see BLOCKER above).
- Confirmed existence of `ManagerCustomerProfileViewModelTest.kt` and
  `ManagerCustomersViewModelTest.kt` (flagged files) — not yet read/reviewed.
- Confirmed current version: `versionCode = 1`, `versionName = "1.0.0"`
  (`app/build.gradle.kts:93-94`).

## Blockers

- **P0**: `SmsOtpRetriever.kt` references `com.google.android.gms.auth.api.phone.SmsRetriever`
  with no corresponding Gradle dependency anywhere in the project — will
  fail to compile once wired into any build path that touches it.
- Untriaged: large untracked doc/code surface (see above) has not been
  reviewed for what's safe to stage vs. what's stale/experimental
  work-in-progress. Nothing should be staged/committed until reviewed.

## Next Action

1. Decide/confirm scope: is Manager-customers + OTP-autofill in scope for
   *this* release pass, or out of scope for now? (Needs a quick read of
   `MANAGER-AUTH-OTP-REQUIREMENTS.md` and `MANAGER-OTP-RECONCILIATION-PLAN.md`
   to determine intent before deciding.)
2. If in scope: resolve the `play-services-auth` dependency gap for
   `SmsOtpRetriever.kt` first (P0), then review
   `ManagerCustomerProfileViewModelTest.kt` / `ManagerCustomersViewModelTest.kt`
   for real vs. mock data before including.
3. Run a debug build (`assembleDebug`) with current untracked sources
   present to get a real baseline compile signal before going further.
4. Do NOT start a full repository audit — only expand into files
   directly relevant to the above.

## Important Decisions

- (none recorded yet this session)

## Files Intentionally Excluded

- (none excluded yet — pending review, see Untracked Work above)

## PHASE 1 — Customer ViewModel Test Review (read-only, no files modified)

Reviewed `ManagerCustomersViewModelTest.kt` / `ManagerCustomerProfileViewModelTest.kt`
and their production ViewModels/factories (`manager/presentation/customers/*`).

- **Target real backend types?** Partially. They're written against
  `domain.repository.SalonCustomerRepository` / `SalonCustomer` /
  `CurrentUserIdentityContextRepository` — real types already on `main`
  (also used by the already-shipped `ManagerBookingViewModel`). But they
  call `SalonCustomerRepository.getCustomer(...)`, `.getCustomerBookings(...)`,
  `.getCustomerNotes(...)` and import `domain.repository.CustomerNote` —
  **none of which exist on `main`'s actual `SalonCustomerRepository.kt`**,
  which declares only `searchCustomers(salonId, query)`. `CustomerNote`
  only exists under the *old* `manager.domain.customer` package (a
  different, incompatible type). **These ViewModels/tests will not compile
  against current `main` as committed** — they appear written against an
  extended version of the interface that either exists on another
  branch/WIP or was never finished.
- **Separately, a hard duplicate-class conflict**: the untracked
  `data/remote/dto/CustomerNoteResponseDto.kt` declares
  `CustomerNoteResponseDto` in package `ai.rojan.designlab.data.remote.dto`
  — but the already-shipped `CustomerDtos.kt` **already declares a class
  with the exact same name in the same package** (different fields:
  existing has `authorId`, new one doesn't). Two files, same FQN, same
  package → guaranteed Kotlin redeclaration compile error regardless of
  the interface-mismatch issue above. `SalonCustomerResponseDto.kt` has no
  such collision but is also unused by anything (`SalonCustomerRepositoryImpl`
  doesn't reference either new DTO at all).
- **Mocks/fakes implying false backend support?** No — the fakes correctly
  implement the real repository interfaces and don't fabricate data; not a
  concern here.
- **Meaningful coverage?** Yes, in intent — real regression scenarios
  (owner-vs-membership salon resolution, genuine-404 vs no-access
  disambiguation, phone-number-normalized search, debounce/race-condition
  handling). But the coverage is for code that doesn't compile today.
- **Duplicate/conflicting with existing tests?** No existing test exercises
  the shipped `BackendCustomerRepository`/`CustomerRepository` path this
  directly, so no duplication — but see production-code finding below.
- **Wired into any screen?** No. `ManagerCustomersViewModel`/
  `ManagerCustomerProfileViewModel` have zero consumers anywhere in
  `src/main` — not referenced by `ManagerCustomersListScreen.kt` or any
  nav graph. The shipped screens still read directly from
  `BackendCustomerRepository`/`CustomerRepository`/`ManagerCustomer`
  (a separate, older, still-in-use domain model). So today this is
  orphaned, non-compiling, unwired code — it does not "replace" the
  shipped implementation functionally, but it cannot be included as-is.
- **Recommendation**: EXCLUDE all of it (2 ViewModels, 2 factories, 2
  tests, 2 DTOs) from this release. It's incomplete/broken, not a
  reviewed-and-ready feature.

## PHASE 2 — Clean Baseline Build (origin/main only, isolated git worktree)

Built from a temporary `git worktree` checked out at `origin/main`
(23ce03b) — completely isolated from the working directory; no stash, no
stage, no delete, no Gradle/dependency/test/production-source changes;
worktree removed after. Working directory `git status` unaffected
throughout.

**RESULT: BUILD FAILS on `origin/main` itself — pre-existing, unrelated to
any untracked OTP/Customer work.**

`app:compileManagerDevDebugKotlin` (and by extension every flavor, since
this file lives in the shared `src/main` source set compiled by all
flavors) fails:
```
RojanNavGraph.kt:1078:29 No value passed for parameter 'onLoginClick'.
```
Root cause (confirmed via `git blame`): commit `64ee89e` ("feat(customer):
complete brand visual consistency", 2026-09-15) added a required
`onLoginClick: () -> Unit` parameter to `ProfileScreen(...)`
(`ProfileScreen.kt:152`, no default value). The `ProfileScreen(...)` call
site in `RojanNavGraph.kt:1065-1085` (Customer's Profile nav destination)
was never updated to pass it — most likely a gap in merge commit `642806e`
("merge: reconcile customer and manager release integration"). Reproduced
twice: once inside a 9-variant `assembleDebug` (where the fully-independent
external-dependency dex-merge tasks for Manager separately hit an
OOM — a build-environment memory issue, not a code issue — which is what
first surfaced), and once cleanly and deterministically via
`clean` + `assembleManagerDevDebug --no-configuration-cache --no-build-cache`.

**This means `origin/main` cannot produce a release build right now,
independent of any scope decision on OTP or Manager Customers.**

## Blockers (updated)

- **P0 — `origin/main` does not compile**: `RojanNavGraph.kt:1078` →
  `ProfileScreen(...)` missing required `onLoginClick` argument. Blocks
  every flavor's build (shared `src/main` source set). Root cause and
  exact fix location identified above; fix not yet applied (no code
  changes made this session per instructions).
- P0 (pre-existing, restated): `SmsOtpRetriever.kt` missing
  `play-services-auth` dependency — excluded from scope per approved
  checkpoint, not being fixed.
- P0 (new, this session): untracked `manager/presentation/customers/*`
  ViewModels/tests call `SalonCustomerRepository` methods and a
  `CustomerNote` type that don't exist on `main`, and the untracked
  `CustomerNoteResponseDto.kt` duplicate-declares a class that already
  exists in `CustomerDtos.kt` — excluded from scope per Phase 1 review
  above, not being fixed.

## PHASE 3 — Build-Blocker Fix (approved, applied)

**Change made** (production source, one file, one line):
`app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt`,
Customer Profile nav destination's `ProfileScreen(...)` call — added
```kotlin
onLoginClick = { navController.navigate(RojanDestinations.AUTH) },
```
immediately before the existing `onLogoutClick = { ... }` argument.
**Why**: this exact pattern (`onLoginClick = { navController.navigate(RojanDestinations.AUTH) }`)
already exists verbatim at two other call sites in the same file
(lines ~1021, ~1046) — reused, not invented. `ProfileScreen.kt` itself
was **not modified** — its existing `onLoginClick: () -> Unit` contract
(added by commit `64ee89e`) was simply satisfied at the one call site
that had been missed (likely during merge `642806e`).

**Verification**: built in a second isolated, disposable `git worktree`
(checked out at HEAD, same one-line fix applied there — kept
completely separate from the working directory's excluded untracked
OTP/Customer-ViewModel files so they can't contaminate the signal).
`clean` + `assembleManagerDevDebug --no-configuration-cache --no-build-cache`
(JAVA_HOME = Eclipse Temurin 21) → **BUILD SUCCESSFUL in 2m 5s, 39
actionable tasks: 39 executed.** Only output: one pre-existing, unrelated
compiler warning (`BackendApiContainer.kt:290`, redundant `Json`
instantiation) — not touched, not a blocker. Worktree removed after
(long-path cleanup needed `\\?\` prefix due to deep Kotlin-generated
classfile names, resolved). Working directory `git status` confirmed to
show only the one intended modified file — no other changes.

**Working directory state now**: `RojanNavGraph.kt` is `M` (modified,
tracked, matches the applied fix) — not staged, not committed, per
instructions. All previously-noted untracked files (OTP/SmsRetriever,
Manager-Customers ViewModels/tests/DTOs, docs) are untouched.

## PHASE 4 — Checkpoint Commit (approved, applied)

- **Commit hash**: `e5edaea1042597c21fe894a2f516551350274c97`
- **Commit message**: `fix(navigation): restore customer profile login callback`
- **Contents**: exactly one file —
  `app/src/main/java/ai/rojan/designlab/navigation/RojanNavGraph.kt`
  (1 insertion, the `onLoginClick` line from Phase 3). Verified via
  `git diff --cached` before commit; nothing else was staged. All
  previously-excluded untracked files remain untracked and untouched.
- **Build verification backing this commit**: see PHASE 3 — isolated
  worktree, `clean` + `assembleManagerDevDebug --no-configuration-cache
  --no-build-cache` → BUILD SUCCESSFUL.
- **Git state after commit**: `HEAD` = `e5edaea`; `origin/main` =
  `23ce03b`. Local branch `main` is **1 commit ahead** of `origin/main`
  (not pushed — not requested). Working tree otherwise clean (only the
  same untracked files as before remain).

## Current Phase

Phase 4 complete (checkpoint commit for the verified build fix). Not yet
pushed. Untracked OTP/Manager-Customers work still pending a disposition
decision; broader release Quality Gate (device test, full QA checklist,
versioning, signing) not yet started.

## PHASE 5 — Targeted Release Readiness Audit (read-only, complete)

Commands/build variants used (all in disposable, isolated `git worktree`s
at HEAD `e5edaea`, never the working directory; both removed after):
- `assembleManagerDevRelease assembleCustomerDevRelease --no-configuration-cache --no-build-cache` → BUILD SUCCESSFUL (R8/shrink validated, unsigned by design for non-production flavors)
- `testManagerDevDebugUnitTest testCustomerDevDebugUnitTest --continue` → 347 tests each, 4 failed each (2 distinct failing test classes, see below)
- `lintManagerDevDebug` → FAILED to even run (dependency resolution, see below)
- `curl https://api.rojanai.ir/actuator/health` → `200 {"status":"UP"}` (direct, live check)
- Backend repo (`D:\AndroidProjects\ROJAN_Backend`) audited read-only by an independent agent (source/git inspection only, no build/run there)

### A) Android Release
- **Variants**: `target` × `environment` flavor dimensions → `customer`/`manager`/`reception` × `dev`/`staging`/`production`. Production release variants: `customerProductionRelease`, `managerProductionRelease`, `receptionProductionRelease`.
- **Signing**: NOT configured in this environment — no `keystore.properties`, no `RELEASE_STORE_*`/`RELEASE_KEY_*` env vars, no keystore file. `build.gradle.kts`'s own `gradle.taskGraph.whenReady` guard (lines 291-323) correctly refuses to build any unsigned `*ProductionRelease` variant — by design, not a bug. Setup documented in `SIGNING-SETUP-REPORT.md`. **External secret, UNVERIFIED/blocking-for-actual-publish only.**
- **versionCode/versionName**: `1` / `"1.0.0"` — unchanged.
- **Release build validation**: `assembleManagerDevRelease` + `assembleCustomerDevRelease` (release build type, non-production flavor so no keystore needed) — **BUILD SUCCESSFUL**, including `minifyManagerDevReleaseWithR8`/`minifyCustomerDevReleaseWithR8` and `lintVital*` (release-gate lint subset) for both — all passed cleanly.
- **R8/ProGuard**: `isMinifyEnabled`/`isShrinkResources` = true for release. `proguard-rules.pro` carries narrowly-scoped `-keep` rules from a real, previously-diagnosed-and-fixed device crash (R8 over-merging Retrofit interfaces, 2026-09-14/15) — confirmed still effective, no regression, no new R8 risk found.
- **Full lint** (`lintManagerDevDebug`, `checkDependencies=true`): FAILED — could not resolve `androidx.compose.ui:ui-test-junit4:1.11.3`/`espresso-core:3.7.0`/`androidx.test.ext:junit:1.3.0` from Maven (this sandbox has no network for new dependency resolution, per known environment limitation). **UNVERIFIED — environment limitation, not a code defect.**
- **Unit tests**: 347 tests per flavor, 4 failed per flavor (2 distinct classes, same failures in both flavors since the test file is shared):
  - `TokenAuthenticatorTest` (2 tests, hermetic, no network) — **real, pre-existing test/prod drift**: asserts the *old* "clear session on any refresh failure" behavior; the actual `TokenAuthenticator.kt` (commit `bb37c28`, later than the test's last edit `25e14d7`) intentionally narrowed this to "only clear on a genuine 400/401/403 rejection" (an improvement). Tests are stale, not the production code. **P1** — not introduced by `e5edaea`.
  - `BackendAuthFlowVerificationTest` (2 tests) — **expected, non-blocking**: its own doc comment states it requires a real backend at `localhost:8080` and "is not wired into any CI/build gate." Correctly excluded from release gating already.
- **Instrumentation tests**: 9 files exist (`AuthScreenScreenshotTest`, `CustomerBottomBarSemanticsTest`, `HomeHeaderSemanticsTest`, `SearchScrollResetTest`, `ExampleInstrumentedTest`, `ManagerDashboardScreenshotTest`, `LazyListStableKeyTest`, `NavigateHomeAfterBookingTest`, `SharedAccessibilitySemanticsTest`) — **NOT RUN**, requires a real device/emulator (this project's own environment notes flag emulator unreliability here). **UNVERIFIED.**

### B) Production Data / Backend Config
- **Production API base URL**: `https://api.rojanai.ir/` — committed as a real default (`build.gradle.kts`), not a placeholder. **Verified live right now**: `GET /actuator/health` → `200 {"status":"UP"}`.
- `dev` reads a LAN IP from gitignored `local.properties` (expected); `staging` has no committed URL (requires `-P`, not release-relevant).
- **No fabricated/mock data found wired into production auth, customer, or booking paths.** Two disclosed, non-backend-by-design exceptions found and verified by reading the actual code:
  - `InMemoryBeautyProfileRepository` wired unconditionally for **all** variants including production (`BackendApiContainer.kt:248`) — its own doc comment: "No backend endpoint exists for a customer beauty profile anywhere in the ROJAN backend's API surface" — honest, session-only, not pretending to sync. **P1/product-awareness**, not a fake-data policy violation.
  - `DemoSessionProvider` — misleading name, but its actual code (read in full) shows it's now just a thin session-state holder; no mock auth/OTP logic remains; real auth goes through `BackendAuthRepository`. **P2, naming only.**

### C) Backend Release Contract (audited read-only, `D:\AndroidProjects\ROJAN_Backend`)

Backend's deployed branch is **`release/production-v24`** (HEAD `8e6764f`) — confirmed via `.github/workflows/backend-production-deploy.yml`, which hard-resets the production VPS to this exact branch. This branch and Android-adjacent-referenced `main`/`release/v1.2.0-*` are **disconnected git histories** — `release/production-v24` is the one that matters for what's actually live.

1. **Auth — FOUND, matches.** `/api/v1/auth/{register,login,refresh}` and `GET /api/v1/users/me` match Android's contract exactly. Confirmed both sides: Android's `register()` hardcodes `role=CUSTOMER` client-side, but the **backend has no such restriction** — `UserRole.MANAGER` is fully self-registerable today (only `PLATFORM_ADMIN`/`PLATFORM_REVIEWER` are blocked). The CUSTOMER-only limitation is a pure Android-side choice, not a backend gap. OTP endpoints (`/otp/request|resend|verify`) and `GET /users/me/salon-access` **already exist** on this branch, matching Android's documented (but currently unintegrated, out-of-scope) contract exactly — factual/informational only, per instructions.
2. **Manager Customers — PARTIAL, one confirmed real P0.** The **older, actually-shipped** `ManagerCustomerApi` model is a field-for-field, path-for-path exact match to the backend's real `CustomerController` (paginated list/get/notes/bookings/tags) — correct, no issue. The **newer, partially-ported** `SalonCustomerApi.kt` model is **broken**: it declares `GET /api/v1/salons/{salonId}/customers -> List<UserResponseDto>`, but the real backend endpoint at that exact path/method (`CustomerController.list`) returns a **paginated `PagedResponse<CustomerResponse>` envelope** with a completely different item shape (no `role` field at all; has `tags`/`lifetimeValue`/`status`/etc. instead). **Independently confirmed by me**: `SalonCustomerApi.kt` is committed on `main`/`e5edaea` (not one of the excluded untracked files), and it's wired into **real production DI** (`BackendApiContainer.kt:166-167`) and consumed by the **live, shipped** Manager Booking wizard's customer-search step (`ManagerBookingViewModel`). **This will fail at runtime against the real backend today — pre-existing on `main`, not introduced by `e5edaea`.**
3. **Manager salon access/identity — FOUND, matches exactly.** `GET /users/me/salon-access` response shape matches Android's `CurrentUserIdentityContext`/`OwnedSalonAccess`/`SalonMembershipAccess` field-for-field.
4. **Customer profile — PARTIAL.** `GET /users/me` and avatar/cover upload/delete all FOUND and match. A profile field-update endpoint (name/email/phone) does **NOT** exist server-side — but Android doesn't call one either, so currently moot, not a live gap.
5. **Booking/service flows — FOUND, matches well.** `BookingController`/`SalonBookingController` endpoints and DTOs match Android's `BookingResponseDto`/request DTOs field-for-field, including the already-correctly-handled absence of `service`/`specialist`/`customer` enrichment objects.
6. **Flyway**: no pending/incomplete migrations found for customers/salon_membership/bookings/users tables through `V41`. UNVERIFIED: actual `flyway_schema_history` state on the live production DB (no DB access).

## PHASE 5 — Consolidated Blockers

**P0:**
1. **`SalonCustomerApi.kt` contract mismatch** (Android: `app/src/main/java/ai/rojan/designlab/data/remote/SalonCustomerApi.kt`, committed) — expects a bare `List<UserResponseDto>` from `GET /api/v1/salons/{salonId}/customers`; real backend (`CustomerController.kt:102`, `release/production-v24`) returns a paginated `PagedResponse<CustomerResponse>` envelope with an unrelated item shape. Live, wired into production DI, consumed by the shipped Manager Booking wizard's "select existing customer" step. **Pre-existing on `main` — NOT introduced by `e5edaea`** (that commit only touched `RojanNavGraph.kt`). Will fail at runtime against the real backend.
2. **Production release signing not configured** — external secret/keystore requirement, by-design hard gate. Not a code defect; blocks only an actual signed production build. **UNVERIFIED pending external input.**

**P1:**
1. `TokenAuthenticatorTest.kt` — 2 tests assert stale pre-improvement behavior vs. current, intentionally-better `TokenAuthenticator.kt` logic. Pre-existing, not introduced by `e5edaea`.
2. `InMemoryBeautyProfileRepository` — Beauty DNA data is session-only in production (disclosed, no backend endpoint exists at all yet) — product-awareness item.
3. Backend's `main` branch is disconnected from the actually-deployed `release/production-v24` — backend-repo hygiene concern, flagged for whoever manages that repo.

**P2:**
1. `SalonCustomerApi.kt`'s doc comment references a nonexistent `SalonCustomerController` (it's `CustomerController`) — stale comment, same file as the P0 above.
2. `DemoSessionProvider`'s misleading class name — behavior is real, not mock; naming only.
3. `proguard-rules.pro`'s broad `-keep interface ai.rojan.designlab.data.remote.** { *; }` (from a proven historical fix) trades some shrinking scope for correctness — deliberate, not new.

**UNVERIFIED:**
1. Whether the production VPS container currently matches `release/production-v24` HEAD `8e6764f` right this moment (repo-only audit, no deploy-state probe).
2. Live production DB's actual Flyway `flyway_schema_history` state.
3. Full `lint` with `checkDependencies=true` (blocked by this sandbox's lack of network for new Maven artifact resolution).
4. Instrumentation/UI tests (9 files) — require a real device/emulator, not run.
5. Real production keystore secrets — external, absent in this environment.

## Current Phase

Phase 5 (targeted release readiness audit) complete — read-only, nothing modified, nothing staged/committed/pushed.

## PHASE 6 — P0 Fix: SalonCustomerApi Contract (approved, applied, NOT committed)

**Root cause** (confirmed against real backend source, `release/production-v24`,
`CustomerController.kt:102`, `CustomerDtos.kt:75-91`, `PagedResponse.kt`):
`SalonCustomerApi.search()` had **two independent contract bugs** against the
real, deployed `GET /api/v1/salons/{salonId}/customers`:
1. Wrong response shape — declared a bare `List<UserResponseDto>`; the real
   endpoint returns a paginated `PagedResponseDto<CustomerResponseDto>`
   envelope (`content/page/size/totalElements/totalPages`), and the item
   shape itself differs entirely (`CustomerResponseDto` has no `role` field;
   has `tags`/`lifetimeValue`/`status`/etc. that `UserResponseDto` doesn't).
   This would fail Retrofit/kotlinx.serialization deserialization outright.
2. Wrong query param name — sent `?query=`, but the backend reads the search
   term as `?search=` (`@RequestParam(required = false) search: String?`).
   Even had (1) not existed, the search term itself was silently never
   reaching the backend's filter.
   Also: the domain id this fed into `SalonCustomer.id` was a raw backend
   `User.id` (via `UserResponseDto`), not the actual CRM `Customer.id` — the
   type the Manager Booking flow's `customerId` is meant to carry downstream
   (per `ManagerBookingViewModel.confirm()`'s own doc comment). Fixing the
   response shape to `CustomerResponseDto` fixes this id-semantics bug too,
   not just the deserialization crash.

The already-shipped `ManagerCustomerApi.list()` (same file family, calling
the **exact same endpoint**) already had the correct contract — used as the
reference pattern for this fix, not invented.

**Exact fix** — two files, minimal:
- `data/remote/SalonCustomerApi.kt`: `search()` now declares
  `@Query("search") query: String?` (was `@Query("query")`), added
  `page: Int = 0, size: Int = 100` (matching `ManagerCustomerApi.list`'s
  own default — this caller needs one page's worth of matches, not a
  paged/scrolling UI, confirmed by reading its only caller), and returns
  `PagedResponseDto<CustomerResponseDto>` (reusing the existing, already-
  established `PagedResponseDto`/`CustomerResponseDto` types — nothing
  invented). Doc comment corrected: no `SalonCustomerController` exists
  (it's `CustomerController`); access is `Permission.VIEW_CRM`
  (owner-or-manager-membership), not owner-only.
- `data/repository/SalonCustomerRepositoryImpl.kt`: unwraps `.content`
  from the paged envelope and maps `CustomerResponseDto` (was
  `UserResponseDto`) → `SalonCustomer`. The domain `SalonCustomerRepository`
  interface, `SalonCustomer` domain type, and every existing caller
  (`ManagerBookingViewModel`, its Factory, `ManagerBookingCustomerScreen`)
  are **completely unchanged** — the fix is entirely internal to the
  network-layer implementation.

**Files changed** (verified via `git diff` — exactly these two, nothing
else):
- `app/src/main/java/ai/rojan/designlab/data/remote/SalonCustomerApi.kt`
- `app/src/main/java/ai/rojan/designlab/data/repository/SalonCustomerRepositoryImpl.kt`

**Verification** (isolated, disposable `git worktree` at HEAD `e5edaea`
with the fix copied in — never the working directory; removed after):
- `assembleManagerDevDebug --no-configuration-cache --no-build-cache` →
  **BUILD SUCCESSFUL** (54s).
- `testManagerDevDebugUnitTest --tests "*ManagerBookingViewModel*"` →
  **BUILD SUCCESSFUL** — `ManagerBookingViewModelTest` (7 tests) and
  `ManagerBookingViewModelSavedStateTest` (4 tests, uses
  `FakeSalonCustomerRepository`) both pass.
- Full `testManagerDevDebugUnitTest --continue` → **347 tests, 4 failed —
  identical to the pre-fix baseline** (2× `TokenAuthenticatorTest`, 2×
  `BackendAuthFlowVerificationTest`, both already-documented, pre-existing,
  unrelated to this change). **Zero new failures — no regression.**

**Not touched** (per instructions): OTP/`SmsOtpRetriever`, the excluded
untracked Manager-Customers ViewModels/tests/DTOs, release signing,
`TokenAuthenticatorTest.kt`, any backend file, any migration, `SalonCustomer`
domain type, `ManagerBookingViewModel`/its screen.

**Working directory state**: two files modified, not staged, not committed.
All previously-excluded untracked files remain untouched.

## Current Phase

Phase 6 (P0 SalonCustomerApi fix) implemented and verified, **not yet
committed** per instructions.

## PHASE 7 — Checkpoint Commit for the P0 Fix (approved, applied)

- **Commit hash**: `59fd2c19096e8c18739f1383cacbc2fcc874d691`
- **Commit message**: `fix(booking): align salon customer search with backend contract`
- **Contents**: exactly two files — `data/remote/SalonCustomerApi.kt` and
  `data/repository/SalonCustomerRepositoryImpl.kt` (23 insertions, 9
  deletions). Verified via `git diff --cached` before commit; nothing else
  was staged. `ROJAN_RELEASE_CONTEXT.md` deliberately left uncommitted, all
  previously-excluded untracked files untouched.
- **Build/test verification backing this commit**: see PHASE 6 — isolated
  worktree, `assembleManagerDevDebug` BUILD SUCCESSFUL, focused booking/
  customer-search tests 11/11 passed, full suite 347 tests with the same 4
  pre-existing failures as baseline (zero new failures).
- **Git state after commit**: `HEAD` = `59fd2c1`; `origin/main` = `23ce03b`.
  Local `main` is **2 commits ahead** of `origin/main` (`e5edaea`,
  `59fd2c1`) — not pushed. Working tree otherwise clean (only the
  long-standing untracked files remain).

## Current Phase

Phase 7 complete (checkpoint commit for the verified P0 fix). Not yet
pushed. Two commits now ahead of `origin/main`.

## PHASE 8 — Final Release Verification (read-only, complete)

All checks below ran in a disposable, isolated `git worktree` at HEAD
`59fd2c1` (never the working directory; removed after) unless noted as a
direct backend-repo read.

### 1) Production Signing
- Release variant: `*ProductionRelease` (customer/manager/reception).
- Confirmed unchanged from Phase 5: no `keystore.properties`, no
  `RELEASE_STORE_*`/`RELEASE_KEY_*` env vars, no keystore file. This is a
  pure **external secret gap** — the repository's own signing logic
  (`build.gradle.kts:27-47, 180-196, 291-323`) is correctly implemented and
  requires no code change; it just has nothing to read in this environment.
  No fake/dummy credentials created, no config touched.

### 2) Release Build
- Maximum safe validation without the missing secret: `bundleManagerDevRelease`
  (dev flavor, release build type — unsigned by design, no keystore needed)
  → **BUILD SUCCESSFUL in 2m 10s**, full pipeline including
  `minifyManagerDevReleaseWithR8`, `lintVitalManagerDevRelease`,
  `packageManagerDevReleaseBundle`, `signManagerDevReleaseBundle` (bundle
  tooling's own pass-through, not the production keystore),
  `bundleManagerDevRelease`. This is the actual **AAB** artifact type (not
  just an APK smoke test) — the highest-fidelity check possible here short
  of the real production keystore.

### 3) Test Health — the 4 known failures, precise root cause each
Reran with full stack traces (`--info`, isolated worktree):
- **`TokenAuthenticatorTest > a failed refresh clears the token pair...`**
  — `expected null, but was <OLD_ACCESS_TOKEN>`. Root cause unchanged from
  Phase 5: the test throws a plain `IOException` (transient); current
  `TokenAuthenticator.kt:107-119` only clears on a genuine
  `BackendApiException` with status 400/401/403 (commit `bb37c28`,
  intentional). **Stale test, current production behavior is the correct,
  documented one — no functional issue.**
- **`TokenAuthenticatorTest > when the single refresh fails, other
  concurrent callers bail...`** — `expected:<1> but was:<3>` refresh
  attempts. **Refined finding, more precise than Phase 5**: this is not
  merely a stale assertion — it's a real, narrow side-effect of the same
  `bb37c28` fix. The old behavior cleared the refresh token on *any*
  failure, which incidentally made callers 2/3 bail immediately (no
  refresh token left). The new, correct-on-purpose behavior preserves the
  refresh token across transient failures — but as a side effect, the
  concurrent-refresh guard's only signal ("did the token change") no
  longer distinguishes "someone already tried and failed" from "no one has
  tried yet," so **all N concurrently-401'd callers each independently
  retry their own refresh call during a transient outage**, instead of
  serializing to one. Not a correctness/security issue (no wrong session
  is ever established, all callers still fail together safely) — a real,
  if minor, inefficiency (redundant network calls under a transient blip
  with concurrent requests) that the test correctly caught but nobody
  reconciled after `bb37c28` landed. **P1, more specific than previously
  stated.**
- **`BackendAuthFlowVerificationTest` (both)** — stack traces reveal this
  environment has a **real, live backend actually reachable at
  `localhost:8080`** (contrary to the Phase 5 assumption of "no backend
  running"). Both failures trace to the same root cause:
  `BackendApiException: Too many registration attempts from: 172.18.0.1`
  — a real, IP-based anti-abuse rate limit on `/auth/register`, almost
  certainly exhausted by this session's own repeated test runs today
  (this same test class has now executed at least 4 times across separate
  isolated worktrees in this session). Confirmed **not a code defect**:
  the test's own doc comment already states it requires a live local
  backend and "is not wired into any CI/build gate" — a rate-limited local
  dev/test backend is exactly the kind of environmental flakiness that
  doc comment anticipates. **No production impact.**

### 4) Lint
- `lintManagerDevDebug --offline` → **FAILED**, and the `--offline` flag
  makes the cause unambiguous: `No cached version of
  androidx.compose.ui:ui-test-junit4:1.11.3 / espresso-core:3.7.0 /
  androidx.test.ext:junit:1.3.0 available for offline mode.` These three
  `androidTest` artifacts were simply never downloaded into this sandbox's
  Gradle cache. **Confirmed environment/network limitation, not a code or
  configuration defect. UNVERIFIED — needs an environment with network
  access to Maven, or these artifacts pre-cached.**

### 5) Backend Production Verification (direct read, `D:\AndroidProjects\ROJAN_Backend`)
- Re-checked directly: still on `release/production-v24`, HEAD still
  `8e6764f` — **unchanged since the Phase 5 audit**, no drift. (One
  unrelated uncommitted local diff to `scripts/deploy.sh` and some
  untracked screenshot files present in that repo's own working tree —
  not inspected further, out of scope, not touched.)
- All controller/contract/Flyway findings from Phase 5 (Auth, Manager
  Customers — now fixed client-side, Manager salon access, Customer
  profile, Booking/service flows, migrations V1-V41) stand unchanged;
  re-verified via a fresh `git status`/`git log` read rather than
  re-auditing file contents (no broad rescan, per instructions).

### 6) Production Data / Mock Check (dependency-wiring, not name-based)
- Re-swept `di/BackendApiContainer.kt` for `InMemory|Demo|Fake|Mock|Stub`
  wiring: **only one hit**, `InMemoryBeautyProfileRepository`
  (`BackendApiContainer.kt:248`) — same, already-disclosed finding as
  Phase 5 (no backend endpoint exists for this feature at all; honest,
  session-only, not fabricated). **No other mock/demo provider is wired
  into production DI for any shipped flow** (auth, manager customers,
  salon access, booking all confirmed wired to real
  `BackendAuthRepository`/`BackendCustomerRepository`/
  `SalonCustomerRepositoryImpl`(now fixed)/`SalonBookingApi`/`BookingApi`
  implementations backed by real Retrofit services).

## PHASE 8 — Final Classification

**P0 (blocks production release):**
1. Production release signing not configured — **external secret
   requirement**, not a code defect. Nothing more to do from the
   repository side; needs a real keystore + 4 credentials supplied via
   `keystore.properties` or CI env vars.

**P1 (should be resolved before release if it affects correctness):**
1. `TokenAuthenticatorTest`'s concurrency test reveals a real (if narrow)
   loss of refresh-call serialization under transient failures + concurrent
   401s — redundant network calls, not a correctness/security bug. Worth a
   product/engineering decision (re-add an explicit "attempt already
   failed this round" signal, or accept the trade-off and update the
   test) — not touched this phase per instructions.
2. `TokenAuthenticatorTest`'s other failure — stale assertion vs.
   intentionally-improved behavior; the test needs updating, not the code.

**P2 (non-blocking):**
1. `SalonCustomerApi.kt`'s now-corrected doc comment previously referenced
   a nonexistent `SalonCustomerController` — already fixed in commit
   `59fd2c1`.
2. `InMemoryBeautyProfileRepository` — disclosed, session-only Beauty DNA
   data in production; product-awareness item, not a defect.
3. Backend's `main` branch remains disconnected from the deployed
   `release/production-v24` — backend-repo hygiene, not an Android blocker.

**UNVERIFIED (external/environment/credential):**
1. Full `lintManagerDevDebug` — blocked by missing cached `androidTest`
   Maven artifacts in this sandbox (confirmed via `--offline`).
2. Whether the production VPS container currently matches backend HEAD
   `8e6764f`.
3. Live production DB's actual Flyway `flyway_schema_history` state.
4. Instrumentation/UI tests (9 files) — require a real device/emulator.
5. Real production keystore secrets.

## Are the two unpushed commits release-safe?

**Yes**, on the evidence gathered:
- `e5edaea` (nav fix) and `59fd2c1` (booking/customer-search contract fix)
  both build cleanly (`assembleManagerDevDebug`, `bundleManagerDevRelease`
  including full R8/shrink), introduce zero new test failures (the same 4
  pre-existing, independently-explained failures persist unchanged and
  are self-inflicted/stale, not caused by either commit), and touch only
  their exact intended files (verified via `git diff` at each checkpoint).
- Neither commit depends on or is blocked by the remaining P0 (signing is
  an orthogonal, external, always-required-at-publish-time step, not
  something either commit's correctness depends on).

## Current Phase

Phase 8 (final release verification) complete — read-only, nothing
modified, staged, committed, or pushed.

## PHASE 9 — Pushed to origin/main (approved, complete)

**Pre-push verification** (all passed):
1. Branch = `main`. 2. `HEAD` = `59fd2c19096e8c18739f1383cacbc2fcc874d691`.
3. Exactly two commits ahead of `origin/main` — `e5edaea`, `59fd2c1`, no
   others. 4. Confirmed via `git show --stat` on each: `e5edaea` touches
   only `RojanNavGraph.kt`; `59fd2c1` touches only `SalonCustomerApi.kt`
   and `SalonCustomerRepositoryImpl.kt` — neither contains OTP or
   Manager-Customer WIP. 5. `git diff --cached` empty — nothing staged.

**Push**: `git push origin main` → `23ce03b..59fd2c1  main -> main`.

**Post-push verification**:
- `git fetch origin main` + `git rev-parse origin/main` = `HEAD` =
  `59fd2c19096e8c18739f1383cacbc2fcc874d691` — **exact match**.
- `git status --porcelain -uno` — clean (no tracked-file changes).
- Push timestamp: 2026-09-26 14:39 (local).

**Pushed commit range**: `23ce03b..59fd2c1` (`e5edaea`, `59fd2c1`).

**Resulting state**: local `main` HEAD and `origin/main` both at `59fd2c1`.
No divergence. Untracked files (OTP/SmsOtpRetriever, Manager-Customers
WIP, docs, `ROJAN_RELEASE_CONTEXT.md` itself) remain untouched and
unpushed, as intended.

## Current Phase

Phase 9 complete — the two verified release fixes are now live on
`origin/main`. Remaining work is the external production-signing
prerequisite and the two P1 test items; no further code changes are
pending approval at this checkpoint.

## Remaining P0/P1/P2/UNVERIFIED (unchanged from Phase 8)

- **P0**: Production release signing not configured — external secret
  (keystore + `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/
  `RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD` via `keystore.properties` or
  CI env vars). Nothing further possible from the repository side.
- **P1**: (1) `TokenAuthenticatorTest`'s concurrency test — real, narrow
  loss of refresh-call serialization under transient failures + concurrent
  401s (redundant network calls, not a correctness/security issue) —
  needs a product/engineering decision. (2) `TokenAuthenticatorTest`'s
  other failure — stale assertion vs. intentionally-improved behavior;
  test needs updating, not the code.
- **P2**: `InMemoryBeautyProfileRepository` (disclosed, session-only
  Beauty DNA data) — product-awareness only. Backend's `main` branch
  disconnected from deployed `release/production-v24` — backend-repo
  hygiene, not an Android blocker.
- **UNVERIFIED**: full `lintManagerDevDebug` (missing cached `androidTest`
  Maven artifacts in this sandbox); whether the production VPS container
  matches backend HEAD `8e6764f`; live production DB Flyway state;
  instrumentation/UI tests (9 files, need a real device/emulator); real
  production keystore secrets.

## PHASE 10 — Final Production Preparation: Signing Audit (halted per instructions)

Re-verified exhaustively, no assumptions carried over from earlier phases:
- `keystore.properties` at repo root: **absent** (`Test-Path` / `test -f`
  both confirm).
- `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` /
  `RELEASE_KEY_PASSWORD`: checked in Bash's environment **and**
  PowerShell's Process/User/Machine scopes separately — **unset in every
  scope checked**. No value was printed at any point.
- No `.jks`/`.keystore` file exists anywhere in the repository tree.
- **Production release signing cannot be performed in this environment.**

Per the task's own instruction ("If signing credentials are missing: STOP
after reporting exactly what external input is required"), **Phases 2-6
of this production-preparation task were not attempted** — no signed build
was run, no artifact produced, no certificate to inspect. This is a
deliberate stop, not a failure: everything on the repository side is
already correct and unblocked (`build.gradle.kts`'s signing logic is
sound, verified working end-to-end for non-production flavors in Phase 8's
`bundleManagerDevRelease`); the only missing piece is a real, externally-
supplied keystore.

**Exact external input required, to unblock a real signed production build:**
1. A release-signing keystore file (`.jks` or `.p12`).
2. Four credentials: the keystore's store password, the key alias inside
   it, and that key's password (store/key password may be identical for a
   standard PKCS12 keystore).
3. Supplied via **either**:
   - a `keystore.properties` file at the repo root (copy
     `keystore.properties.sample`, fill in real values — gitignored,
     never commit it), for a local/workstation release build, **or**
   - the four environment variables `RELEASE_STORE_FILE`,
     `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
     for a CI build (never filed to disk).
4. Full procedure already documented in `SIGNING-SETUP-REPORT.md`
   (existing, tracked file — not modified this phase).

Once supplied, the real production build becomes:
`./gradlew bundleManagerProductionRelease` (and the Customer/Reception
equivalents as needed) — the exact same, already-verified pipeline that
just ran clean for `bundleManagerDevRelease` in Phase 8, gated only by the
`gradle.taskGraph.whenReady` signing check now finding real credentials
instead of none.

## Current Phase

Phase 10 halted at the signing-audit gate, exactly as instructed. No code,
config, secrets, or artifacts were touched, created, or committed this
phase.

## PHASE 11 — First Production Signing Keystore Created (approved, complete)

Confirmed before creation: `ai.rojan.designlab`/Manager/Reception apps
have never been published to Google Play — no prior upload key to
preserve. A brand-new keystore was generated per `SIGNING-SETUP-REPORT.md`'s
documented spec.

**Created**: `keystore/rojan-customer-upload.jks` — PKCS12, RSA 4096-bit,
`SHA384withRSA`, validity 10,950 days, alias `rojan-customer-upload`, DN
`CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR` (matches
the report's documented convention exactly). Store and key password:
freshly generated, 192-bit random, **never printed to any output, never
written anywhere except `keystore.properties`** (gitignored).

**Certificate fingerprints** (public, non-secret — safe to record and to
register with any third-party service that needs them later):
- SHA-1: `52:F8:D9:6C:5B:72:48:28:F6:C0:96:CC:97:5A:03:29:77:F8:4E:6C`
- SHA-256: `CA:9C:EA:27:17:82:2D:BB:87:C4:9A:4D:B5:8E:0E:71:31:9B:A9:1A:69:81:3A:F9:27:98:02:44:D7:4A:E3:89`

(Note: these differ from the earlier fingerprints documented in
`SIGNING-SETUP-REPORT.md` §3 — that key was never actually present on
disk in this environment when checked in Phase 10; this is a genuinely
new key generated now, not a recovery of the old one.)

**`keystore.properties`** created at repo root with the documented
variable names (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — real values, gitignored,
not committed.

**Verification performed** (no password ever printed, using keytool's
`-storepass:env` so no plaintext password appeared even as a process
argument):
- File exists: YES.
- Alias = `rojan-customer-upload`: confirmed via `keytool -list -v`.
- `.gitignore` coverage re-confirmed with both files now actually present:
  `keystore.properties` → `.gitignore:39`; `keystore/rojan-customer-upload.jks`
  → `.gitignore:33` (`*.jks`) — both correctly ignored.
- `git status --porcelain | grep -i keystore` → empty — neither file is
  tracked, staged, or even shown as untracked (correctly invisible to git).

**Ownership decision still open** (per the report's own §3, unchanged):
before the first Play Store upload, decide Play App Signing (Google holds
the app signing key, this `.jks` becomes the upload key — recommended) vs.
self-managed signing (losing this file = permanently unable to update the
app). Either way: **this keystore + its password must be backed up now,
outside the repo**, in a secret manager or encrypted offline copy — it
exists only in this local `keystore/` directory (gitignored) right now.

**No build was run this phase** (explicitly deferred, per instructions).

## Current Phase

Phase 11 complete — signing credentials now exist locally. Production
signing is theoretically unblocked (the file + `keystore.properties` are
both present and verified), but no signed build has been attempted yet.

## PHASE 12 — First Signed Production Build (approved, complete)

Keystore backed up externally per user confirmation before this phase.
Build ran in a disposable, isolated `git worktree` at HEAD `59fd2c1`
(only the two verified, committed fixes — none of the excluded untracked
OTP/Manager-Customers WIP), with `keystore.properties` and
`keystore/rojan-customer-upload.jks` copied in temporarily; worktree
(including its keystore copy) fully deleted after. The main repo's own
`keystore/`/`keystore.properties` were never modified, staged, or moved.

**Command**: `./gradlew bundleManagerProductionRelease
--no-configuration-cache --no-build-cache`

**Result**: **BUILD SUCCESSFUL in 3m 54s** (51 tasks). The signing gate
(`validateSigningManagerProductionRelease`, `signManagerProductionReleaseBundle`)
ran and passed — first time this gate has ever had real credentials to
consume.

**Artifact**:
- Path (inside the disposable worktree, now deleted —
  regenerate with the same command to reproduce):
  `app/build/outputs/bundle/managerProductionRelease/app-manager-production-release.aab`
- Size: 9,091,994 bytes (~8.67 MiB)
- versionCode: `1`, versionName: `"1.0.0"` — confirmed unchanged
  (`app/build.gradle.kts` shows zero diff throughout this entire phase,
  per `git status`).

**Signature verification** (`keytool -printcert -jarfile`, public
certificate data only, no password used or needed):
- Owner: `CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR`
- SHA-1: `52:F8:D9:6C:5B:72:48:28:F6:C0:96:CC:97:5A:03:29:77:F8:4E:6C`
- SHA-256: `CA:9C:EA:27:17:82:2D:BB:87:C4:9A:4D:B5:8E:0E:71:31:9B:A9:1A:69:81:3A:F9:27:98:02:44:D7:4A:E3:89`
- Signature algorithm: `SHA384withRSA`
- **Matches exactly** the certificate recorded in Phase 11 at keystore
  creation — confirms the AAB is signed with the intended, newly-created
  production key, not a stray/debug/other key.
- AAB file SHA-256 (artifact hash, distinct from the certificate hash):
  `01d28746a88a5e4649cc783ecb150a684357ab4e0c62f603843aafc8898365ea`

**Git status after build**: main repo clean — `git status --porcelain |
grep -i keystore` empty (neither `keystore.properties` nor the `.jks` is
tracked, staged, or untracked-visible); `app/build.gradle.kts` shows zero
diff. No secret value was printed at any point in this phase.

## Current Phase

Phase 12 complete. **This is the first successful signed production
build ROJAN has ever produced.** Only the Manager flavor was built, per
instructions — Customer/Reception production variants not yet attempted.

## PHASE 13 — Customer + Reception Production AABs (approved, complete)

Same pattern as Phase 12: disposable, isolated `git worktree` at HEAD
`59fd2c1`, `keystore.properties` + `keystore/rojan-customer-upload.jks`
copied in temporarily, worktree (and its keystore copy) fully deleted
after. Main repo never touched.

**Command**: `./gradlew bundleCustomerProductionRelease
bundleReceptionProductionRelease --no-configuration-cache --no-build-cache`

**Result**: **BUILD SUCCESSFUL in 3m 19s** (101 tasks) — both variants in
one invocation.

| | Customer | Reception |
|---|---|---|
| Build result | SUCCESS | SUCCESS |
| AAB path (in the now-deleted disposable worktree) | `app/build/outputs/bundle/customerProductionRelease/app-customer-production-release.aab` | `app/build/outputs/bundle/receptionProductionRelease/app-reception-production-release.aab` |
| AAB size | 7,914,932 bytes (~7.55 MiB) | 5,095,000 bytes (~4.86 MiB) |
| versionCode | `1` | `1` |
| versionName | `"1.0.0"` | `"1.0.0"` |
| Signed | YES | YES |
| Signer SHA-256 | `CA:9C:EA:27:17:82:2D:BB:87:C4:9A:4D:B5:8E:0E:71:31:9B:A9:1A:69:81:3A:F9:27:98:02:44:D7:4A:E3:89` | same — identical, as expected (same keystore) |
| AAB file SHA-256 | `48a0e1a2bce59afffb1790e716438d6be7d04492dc00272c5bc1dee9bf3a9690` | `9b71d3979f3c08aeaa649a5d4fa38ef42f277be78b5da0d6a0bc37389f4b3ccb` |

Both signer certificates match Manager's (Phase 12) and the original
keystore-creation record (Phase 11) exactly — all three flavors are
signed with the same, correct, newly-created production key.

**Verification**: no `keystore.properties`, `.jks`, password, or private
key material was printed at any point (only public certificate fields via
`keytool -printcert -jarfile`, no password needed to read a public
certificate). Main repo confirmed clean after: `git status --porcelain |
grep -i keystore` empty, `app/build.gradle.kts` shows zero diff. No
upload to any store attempted. No commit, no push.

## Current Phase

Phase 13 complete. **All three ROJAN production flavors (Manager,
Customer, Reception) now have a verified, correctly-signed production AAB
built from the same keystore**, none persisted outside their disposable
build worktrees.

## PHASE 14 — Final Release Gate (read-only, complete)

Rebuilt all three production AABs **and** their equivalent APKs (APKs
built solely for standard manifest-inspection tooling; AABs remain the
actual publishable artifact) in a disposable, isolated `git worktree` at
the approved commit `59fd2c1`, signing material copied in temporarily and
the worktree fully deleted after. No source, Gradle, dependency, backend,
migration, or version changes made; nothing staged/committed/pushed; no
store upload attempted; `TokenAuthenticatorTest`/OTP/Manager-Customers WIP
untouched.

**1) Artifact identity** — confirmed via `aapt2 dump badging` on each
compiled APK (authoritative binary manifest, not inferred):

| | Manager | Customer | Reception |
|---|---|---|---|
| applicationId | `ai.rojan.designlab.manager` | `ai.rojan.designlab` | `ai.rojan.designlab.reception` |
| versionCode | 1 | 1 | 1 |
| versionName | 1.0.0 | 1.0.0 | 1.0.0 |
| Build variant | managerProductionRelease | customerProductionRelease | receptionProductionRelease |
| AAB SHA-256 | `01d28746a88a5e4649cc783ecb150a684357ab4e0c62f603843aafc8898365ea` | `48a0e1a2bce59afffb1790e716438d6be7d04492dc00272c5bc1dee9bf3a9690` | `9b71d3979f3c08aeaa649a5d4fa38ef42f277be78b5da0d6a0bc37389f4b3ccb` |

**All three AAB SHA-256 values are byte-for-byte identical to the
independent Phase 12/13 builds** — the build is fully reproducible given
the same commit + keystore.

**2) Signing** — `keytool -printcert -jarfile` on each AAB: all three
signed, all three certificate SHA-256 = `CA:9C:EA:27:17:82:2D:BB:87:C4:9A:4D:B5:8E:0E:71:31:9B:A9:1A:69:81:3A:F9:27:98:02:44:D7:4A:E3:89`
— matches the Phase 11 keystore-creation record exactly, and is identical
across all three (same keystore, as intended).

**3) Production configuration** — verified by extracting `classes.dex`
from each compiled APK and searching the actual compiled bytecode
(not inferred from source): all three embed the literal string
`https://api.rojanai.ir/` (`BuildConfig.API_BASE_URL`). A parallel search
for any `localhost`/`192.168.*`/`staging` URL in the same compiled dex
found **none** in any of the three artifacts.

**4) Manifest** (`aapt2 dump xmltree`/`badging` on each APK):
- applicationId matches table above in every case.
- `android:debuggable`: absent in all three (release-correct default).
- Exported components: exactly one launcher activity each
  (`ManagerActivity`/`MainActivity`/`ReceptionActivity`, `exported=true`
  with `MAIN`/`LAUNCHER`, required for a launcher) plus the standard
  AndroidX `androidx.profileinstaller.ProfileInstallReceiver` (present in
  every modern Compose/AndroidX app, not project-specific, not a dev-only
  component). No other exported activities/services/receivers found.
- No deep links (`<data>`/scheme/host intent-filter) found in any of the
  three manifests.
- Permissions: `android.permission.INTERNET` + AGP's auto-generated
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` only, in all three — no
  unexpected or dangerous permission.

**5) Mock/demo reachability** — re-confirmed at the source level (the
authoritative check; a literal-class-name search in the release `dex` is
*not* meaningful here since R8 minification/obfuscation renames classes
in a release build, confirmed empirically — a name-based dex search
found nothing at all, including for classes known to exist, because
names are gone, not because the classes are absent). Source-level
dependency-wiring re-check of `di/BackendApiContainer.kt` (identical
across all flavors, no variant-specific override exists for this file):
only `InMemoryBeautyProfileRepository` (line 248) is non-backend, wired
unconditionally for the disclosed, no-backend-endpoint-exists Beauty DNA
feature (same finding as Phases 8/13, unchanged) — reachable from all
three ProductionRelease variants, honestly disclosed, not fabricated
data. No other mock/demo/fake/stub provider is wired anywhere in
production DI for any shipped flow.

**6) Versioning** — confirmed unchanged: `versionCode = 1`,
`versionName = "1.0.0"` (both from the artifact's own compiled manifest,
not just source). Not modified this phase.

**7) Release artifact archive** — created `RELEASE_ARTIFACT_MANIFEST.md`
at the repo root (untracked, not committed, no secrets) recording commit,
each AAB's path/size/SHA-256, version, certificate fingerprint, and build
timestamp/command for all three variants.

**8) Google Play readiness** (repository/build evidence only):
- **READY**: signed AAB, correct applicationId/version per variant,
  clean manifest (no debug flag, no dev-only components), R8/shrink
  pipeline verified, production backend URL confirmed embedded and live.
- **NOT VERIFIED**: no real-device install/functional smoke test of a
  release-signed build performed this session (environment note: reliable
  emulator not available here); Play Console-side requirements (store
  listing, privacy policy, content rating, target-API-level policy
  compliance, data-safety form) — entirely outside repository scope.
- **EXTERNAL INPUT REQUIRED**: a Google Play Console developer account,
  the Play App Signing vs. self-managed decision (Phase 11, still open),
  and the actual store listing/publication workflow — none of these can
  be verified or performed from this repository.

## PHASE 14 — Final Classification

**P0**: none remaining that block producing a correctly signed,
production-configured artifact — that objective is met for all three
variants.

**P1**:
1. No real-device functional verification of a release-signed build has
   been performed (build-time checks only).
2. `TokenAuthenticatorTest`'s two known items (stale assertion; narrow
   concurrency trade-off) — unchanged, undecided, untouched.

**P2**: `InMemoryBeautyProfileRepository` (disclosed, session-only Beauty
DNA data) — unchanged, product-awareness only.

**EXTERNAL**:
1. Google Play Console account/access, store listing, privacy policy,
   content rating, data-safety declarations.
2. The Play App Signing vs. self-managed-signing decision (Phase 11).
3. Real-device or Play Console-side pre-launch testing.
4. Backup confirmation of the keystore (user-stated as already done
   before this phase; not independently verifiable from the repository).

## PHASE 15 — Authoritative Android Production Artifact Record (checkpoint-only)

`RELEASE_ARTIFACT_MANIFEST.md` is now the **authoritative Android
Production Artifact Record**, confirmed by the user against the verified
Phase 14 output. Its exact values (Application IDs, variants,
versionCode/versionName, AAB paths, AAB SHA-256 hashes, AAB sizes,
production signing status) are unchanged from Phase 14 — this phase only
formalized the document's structure into three explicit tiers:

1. **VERIFIED FROM ARTIFACT** — everything confirmed directly from the
   built AAB/APK (binary manifest, compiled-dex string search, cert
   inspection): all three variants' identity/version/AAB hash/size/
   signing, plus the production-URL and no-mock/demo findings.
2. **PENDING REAL-DEVICE TEST** — no real-device/emulator functional
   smoke test performed this cycle; golden-path flows not exercised
   on-device against the signed artifact.
3. **EXTERNAL / GOOGLE PLAY REQUIREMENTS** — Play Console account/
   listing/policy items, the Play App Signing vs. self-managed decision,
   independent keystore-backup confirmation, Play Console pre-launch
   review.

No source code, Gradle/build configuration, AAB, or signing configuration
was modified. No staging, commit, push, or upload performed. Only
`ROJAN_RELEASE_CONTEXT.md` and `RELEASE_ARTIFACT_MANIFEST.md` were
touched.

## Current Phase

Phase 15 complete. `RELEASE_ARTIFACT_MANIFEST.md` is the authoritative,
tiered Android Production Artifact Record. All three production artifacts
remain verified signed, correctly configured, and reproducible (Phase
14). Remaining work is entirely external (Play Console account/listing/
policy items), a deliberate product decision (Play App Signing question),
or optional extra assurance (real-device smoke test) — nothing further is
achievable from the repository alone.

## PHASE 16 — Android App Update Client (Phase 2B, implemented, NOT committed)

Backend/Website checkpoints this phase builds against: `ROJAN_Backend`
`695e82c` (App Release Management foundation), `ROJAN_Web` `353f5a9` (Super
Admin release-management UI) — neither repo touched this phase.

**Update endpoint**: `GET /api/v1/public/app-releases/{applicationId}/latest?versionCode=N`
(no auth — `data/remote/AppReleaseApi.kt`, wired via `buildPlainRetrofit()`
in `BackendApiContainer.kt`, mirroring the existing `publicSalonRepository`
pattern exactly).

**Flavor identity handling**: `applicationId` and the caller's own
`versionCode` are resolved once, at DI-construction time, directly from
`BuildConfig.APPLICATION_ID`/`BuildConfig.VERSION_CODE` — never hardcoded,
never passed manually by a screen/ViewModel. Verified against the actual
compiled `BuildConfig.java` for all three production flavors:

| Flavor | APPLICATION_ID | VERSION_CODE | VERSION_NAME |
|---|---|---|---|
| Manager | `ai.rojan.designlab.manager` | 1 | 1.0.0 |
| Customer | `ai.rojan.designlab` | 1 | 1.0.0 |
| Reception | `ai.rojan.designlab.reception` | 1 | 1.0.0 |

**Update behavior**: the server's `updateAvailable`/`forceUpdate` flags are
trusted as-is (never recomputed client-side) —
`updateAvailable=false` → app continues normally (no dialog);
`updateAvailable=true, forceUpdate=false` → optional dialog
(«نسخه جدید ROJAN منتشر شد», «بعداً»/«به‌روزرسانی»);
`updateAvailable=true, forceUpdate=true` → mandatory dialog
(«به‌روزرسانی ضروری است», no dismiss control rendered, `DialogProperties`
disables back-press/tap-outside, and `AppUpdateViewModel.dismissOptionalUpdate()`
is a hard no-op while state is `Mandatory` — defense in depth beyond the UI
itself). «به‌روزرسانی» always opens the real, server-provided `downloadUrl`
via a plain `Intent.ACTION_VIEW` (mirroring the app's one existing precedent,
`SalonDetailsScreen`'s address-click handler) — never installs an APK, never
requests a permission, never hardcodes a URL.

**Failure behavior (fail open)**: any repository failure (offline, timeout,
non-2xx, malformed body — all already classified by the existing
`safeApiCall`) leaves `AppUpdateViewModel.state` at `Hidden`; a structurally
valid but practically unusable response (`updateAvailable=true` with a blank
`downloadUrl`) is treated identically. The check runs exactly once per
`AppUpdateViewModel` instance (from `init`), and that instance survives every
recomposition of its caller for the Activity's lifetime — no re-trigger, no
repeated dialog. `AppUpdateGate` always renders its wrapped content first;
the dialog only ever overlays on top, so existing splash/auth/session/
navigation logic in `RojanNavGraph`/`ManagerRootGraph`/`ReceptionRootGraph`
is completely untouched and never blocked.

**Files created**: `data/remote/AppReleaseApi.kt`,
`data/remote/dto/AppReleaseDtos.kt`, `domain/repository/AppUpdateRepository.kt`,
`data/repository/AppUpdateRepositoryImpl.kt`,
`presentation/update/{AppUpdateViewModel,AppUpdateViewModelFactory}.kt`,
`ui/components/update/{AppUpdateGate,AppUpdateDialog}.kt`, plus
`AppUpdateRepositoryImplTest.kt` (10 tests) and `AppUpdateViewModelTest.kt`
(12 tests).

**Files modified**: `di/BackendApiContainer.kt` (additive wiring only),
`MainActivity.kt`/`ManagerActivity.kt`/`ReceptionActivity.kt` (one
`AppUpdateGate { ... }` wrapper each around the existing, unmodified root
graph call).

**Test status**: 22/22 new tests pass. Full suite re-run: 369 tests per
flavor (347 pre-existing + 22 new), same 2 pre-existing, already-documented
`TokenAuthenticatorTest` failures as every prior phase — zero regressions.

**Build validation**: `bundleManagerProductionRelease`,
`bundleCustomerProductionRelease`, `bundleReceptionProductionRelease` — all
three **BUILD SUCCESSFUL** (run sequentially after an initial all-three-at-once
attempt hit a JVM OOM crash purely from this sandbox's memory limits, same
class of environment issue seen in earlier phases — not a code defect).
Existing production keystore/signing config used unchanged; no new keystore
created; versionCode/versionName untouched.

**Not done this phase** (correctly out of scope per instructions): no
commit, no push, no APK/AAB upload, no production release records seeded,
no website download files changed, no client-side checksum/SHA-256
verification implemented (the field is preserved on the domain model for a
future pass, per instructions not to invent one).

## Next Action

Awaiting direction: review the App Update Client implementation, decide on
a commit for this phase, and/or address standing open items from earlier
phases (production signing already resolved in Phases 11-14; Google Play
Console setup remains external/not started; the `TokenAuthenticatorTest`
P1 items remain open; the excluded OTP/Manager-Customers WIP disposition
remains undecided).

## Tests / Build Results

- **Baseline `assembleManagerDevDebug` against `origin/main` (isolated
  worktree, clean, no cache): FAILED** — `compileManagerDevDebugKotlin`,
  missing `onLoginClick` argument in `RojanNavGraph.kt:1078`. See PHASE 2
  above for full detail. Not yet fixed; working directory untouched.
- Untracked Manager-Customers ViewModel test files: not run (their
  production code doesn't compile against `main` — see PHASE 1 above).
