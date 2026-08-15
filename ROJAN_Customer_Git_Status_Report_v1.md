# ROJAN Customer App — Git Status Report v1

**Scope:** Customer flavor of `ROJAN_DesignLab` only. No files modified, no commits made, nothing pushed — this is a read-only audit.

---

## 1. Repository Status

| Item | Value |
|---|---|
| Path | `C:\AndroidProjects\ROJAN_DesignLab` |
| Remote (`origin`) | `https://github.com/meisamelh66-cpu/ROJAN_DesignLab_Main1.git` |
| Current branch | `feature/manager-backend-integration` |
| Latest commit | `6b30597` — "fix: enforce production API config and secure release logging" (Meisam Elhaee, 2026-08-12) |
| Ahead of `origin/feature/manager-backend-integration` | **20 commits**, not pushed |
| Uncommitted changes | 1 file modified: `app/build.gradle.kts` (doc-comment expansion + adds `-PDEV_API_BASE_URL` override for the `dev` flavor; not yet staged/committed) |

The 20 unpushed commits and the working-tree change are almost entirely **Manager-side** work (CRM insight rules, Manager customer/service/staff CRUD screens, Manager appointment detail, environment-config hardening). Nothing in that unpushed set touches Customer-only code paths.

---

## 2. Customer App Status

### Flavor configuration
Two independent flavor dimensions in `app/build.gradle.kts`:
- **`target`**: `customer` (zero overrides — inherits `defaultConfig` byte-for-byte) vs. `manager` (`applicationIdSuffix`-free but its own `applicationId`, own manifest, own source set `src/manager/`).
- **`environment`**: `dev` / `staging` / `production`, each supplying `BuildConfig.API_BASE_URL`.

So the Customer app is **not** a separate module or checkout — it's the `main` source set of this single-module app, with Manager added on top as an additive flavor + `src/manager/` overlay. There is no `app/src/customer/` directory, which is correct per the flavor design (customer = default/main).

### applicationId
`ai.rojan.designlab` (from `defaultConfig`, unmodified by the `customer` flavor). `minSdk 24`, `targetSdk 36`, `compileSdk 37`, `versionName 1.0.0` / `versionCode 1`.

### Source set structure
All Customer code lives in `app/src/main/java/ai/rojan/designlab/`:
- `screens/customer/` — dashboard/home widgets (bottom bar, AI search, featured/nearby/recommended salons, promotions, upcoming bookings, etc.)
- `screens/auth/`, `booking/`, `bookingflow/`, `salon/`, `search/`, `service/`, `specialist/`, `profile/` — the full booking + profile journey (appointments, wallet, coupons, membership, loyalty, favorites, reviews, waitlist, reschedule)
- `navigation/RojanNavGraph.kt` + `RojanDestinations.kt` — centralized route table, no magic strings
- `data/remote/` — Retrofit APIs and DTOs (auth, booking, availability, salon, service, specialist, working hours, public salon)
- `data/repository/`, `domain/` — clean layering; `domain/` has no Android imports
- `di/BackendApiContainer.kt` — single manual composition root, shared authenticated Retrofit client for both Customer and Manager

### Manifest
`app/src/main/AndroidManifest.xml` — single `MainActivity`, `INTERNET` permission, portrait-locked (documented reason: no screen has a landscape layout — confirmed by real-device testing, not theoretical). Standard launcher intent-filter. No customer-specific manifest exists or is needed since customer has no flavor overrides.

### Main screens
Splash → (new customer) unauthenticated Member Salons List / (returning) Customer Dashboard → Explore (search + full marketplace) → Salon Details → Specialist Selection → Booking Date/Time/Confirmation/Success, plus a Profile graph (Appointments, Reschedule, Waitlist, Favorites, Followed Salons, Wallet, Coupons, Membership, Loyalty, My Reviews, Beauty DNA/Timeline). Route table shows deliberate scope trims (e.g., category-first pickers and a separate payment step were both removed by design, not left unfinished).

### API configuration
`NetworkConfig.BASE_URL` reads `BuildConfig.API_BASE_URL`, which is set per `environment` flavor:
- `dev` → `http://10.0.2.2:8080/` (emulator-host alias), overridable via `-PDEV_API_BASE_URL` for physical-device testing (this is the uncommitted change currently in the working tree).
- `staging` / `production` → **no hardcoded value**; each must be supplied via `-PSTAGING_API_BASE_URL` / `-PPRODUCTION_API_BASE_URL` (or `gradle.properties`, gitignored). `NetworkConfig.BASE_URL` throws loudly if accessed while blank, and a Gradle `taskGraph.whenReady` check additionally hard-fails any `*ProductionRelease` build up front if `PRODUCTION_API_BASE_URL` isn't set — confirmed by direct test below (§4).

### Authentication flow
Phone → OTP → session, against real backend endpoints (`AuthApi.kt`):
- `POST /api/v1/auth/otp/request`
- `POST /api/v1/auth/otp/verify`
- `POST /api/v1/auth/refresh` (via `TokenAuthenticator`, transparent 401 retry)
- `GET /api/v1/users/me`, `GET /api/v1/users/me/salon-access`

`AuthScreen.kt` is navigation-agnostic (no direct `NavController` calls), driven by `AuthViewModel.sessionState`/`otpStep`. Replaces a prior email/password + Register-toggle screen entirely — this was a genuine migration, not an addition alongside the old flow. Tokens persisted via `SecureTokenStore`/`AuthSessionDataStore`; `AuthInterceptor` attaches bearer tokens; `TokenAuthenticator` handles refresh and clears session state on a hard refresh failure.

### Backend integration points
Retrofit interfaces bound to the real `ROJAN_Backend` API contract (per `AuthApi.kt`'s own reference to `ROJAN_Backend/API.md`): `AuthApi`, `SalonApi`, `PublicSalonApi` (unauthenticated QR-entry flow, deliberately on a separate plain client), `ServiceApi`, `ServiceCategoryApi`, `SpecialistApi`, `AvailabilityApi`, `BookingApi`, `WorkingHoursApi`, `SalonRelationshipApi`. All share one authenticated `Retrofit`/`OkHttpClient` instance from `BackendApiContainer`, except the intentionally-unauthenticated public-salon client. One confirmed gap: **no backend endpoint yet for the customer Beauty Profile** — `BeautyProfileRepository` is in-memory only, held in the same composition root purely so state survives navigation within a process lifetime, not backed by any API.

---

## 3. Git History Findings

### Recent Customer-related commits (most recent first)
- `2c93d05` — feat: complete customer authentication session and login journey
- `a70ad79` — Customer Journey Phases 1-4 checkpoint (Profile restructure, Beauty DNA foundation, Dashboard sections, Salon working hours)
- `92ebb60` — feat(android): integrate production backend for customer flow (Phase 1-6)
- `88f219a` — Add backend auth networking layer (register/login/refresh), verified end-to-end
- `edb97ba` — Fix three data-integrity bugs from demo/backend id mismatch
- `a636b37` — ROJAN Booking Journey P0 Complete
- `5a6f18d` / `c8ec9c8` / `ff82680` — UX Refactor Phases 1–3 (new/returning customer flows, role/identity reconciliation, real login)

Note: several recent commits with "customer" in the message (`fd9b278` "add customer phone contact action", `1792203` "add customer edit flow", `656eaa5` "show CRM insights on customer profile") are actually **Manager-side CRM features for managing customers** (`manager/screens/customers/ManagerCustomer*Screen.kt`), not Customer-app work — worth flagging since the naming is easy to misread.

### Missing / incomplete Customer features
- **No backend-integrated Beauty Profile** — screen(s) exist, repository is in-memory/demo only (confirmed above).
- **No automated instrumented/UI test coverage found** for the Customer booking or auth flows beyond unit tests — `androidTest` exists but wasn't exercised as part of this audit; unit tests are the only coverage confirmed running (§4).
- Two currently-failing unit tests (`BackendAuthFlowVerificationTest`) require a **live backend** on `localhost:8080` — they are integration-style tests misfiring as unit tests when no backend process is running locally, not a Customer-app code defect.
- Older audit docs already in this repo (`ROJAN_AI_Production_Readiness_Report.md`, `ROJAN_AI_Customer_Journey_Audit_PhaseA.md`) flag app-wide gaps (no back-navigation pattern beyond OS back, no `key =` on lazy list items, asset pipeline partially incomplete at time of writing) — these predate the current HEAD by many commits and were **not re-verified** in this pass; treat as historical, not current-state.

### Local vs. origin
Working branch is **20 commits ahead** of `origin/feature/manager-backend-integration`, all local/unpushed. No divergence (no commits on origin absent locally) — a straightforward fast-forward push would apply cleanly if/when the user chooses to push (not done here, per instructions).

---

## 4. Build Readiness

| Check | Result |
|---|---|
| `./gradlew tasks --all` (customer tasks resolve) | ✅ All expected `assembleCustomer*`, `bundleCustomer*`, `installCustomer*Debug`, `lintCustomer*`, `testCustomer*DebugUnitTest` tasks present for `Dev`/`Staging`/`Production` |
| `assembleCustomerDevDebug` | ✅ **BUILD SUCCESSFUL** (21s, 38 tasks) |
| `assembleCustomerProductionRelease` | 🔴 **Fails by design** — `PRODUCTION_API_BASE_URL` not set. This is an intentional Gradle-time guard (added in the latest, uncommitted-adjacent commit `6b30597`), not a broken build. |
| `testCustomerDevDebugUnitTest` | 🟡 104/106 passed; 2 failures are `ConnectException` from `BackendAuthFlowVerificationTest` trying to reach a live backend at `10.0.2.2:8080` — environmental (no backend running during this audit), not a code fault |
| Release signing | 🔴 `keystore.properties` **not present** in this checkout — release build type has no `signingConfig` applied, falls back to unsigned (by the build script's own documented design: "a release build still succeeds unsigned... rather than hard-failing"). Real signing material was never provisioned on this machine. |
| `local.properties` (SDK) | ✅ Present, valid `sdk.dir` |

### Release readiness blockers (Customer)
1. **No production backend URL configured** — `PRODUCTION_API_BASE_URL` must be supplied via `-P` flag or `gradle.properties`/CI secret before any `CustomerProductionRelease` artifact can be built. There is currently no confirmed deployed production backend to point at.
2. **No release keystore present** — `keystore.properties` + the referenced `.jks` are absent from this machine/checkout (correctly gitignored). A signed release AAB/APK cannot be produced here until real signing material is provisioned.
3. **Staging URL** has the same "must be supplied" gate as production, though it does not currently hard-fail the build the way production does.
4. Two failing unit tests depend on a live backend process; CI/release pipelines should either mock this dependency or ensure a reachable backend before running the full test suite, or these will intermittently fail release gating for reasons unrelated to app code.

---

## 5. Summary

### Completed
- Customer flavor config, applicationId, manifest, and full screen/navigation structure
- Real backend-integrated auth (phone/OTP), booking journey, salon/service/specialist discovery, profile sub-screens
- Dev-flavor debug build compiles and assembles cleanly
- Deliberate, documented production-safety gates (fail loudly rather than silently shipping a fake/blank backend URL)

### Missing / Incomplete
- Beauty Profile has no backend endpoint (in-memory placeholder only)
- No confirmed staging/production backend deployment yet
- No release signing material provisioned on this machine
- 2 unit tests require a live local backend to pass
- 20 local commits (mostly Manager-side) unpushed to origin

### Recommended Next Step
Before any Customer release build: (1) provision `keystore.properties` + the release `.jks` on the build machine/CI, (2) confirm and supply a real `PRODUCTION_API_BASE_URL`, (3) either stand up a reachable backend for CI test runs or separate `BackendAuthFlowVerificationTest` out of the unit-test task so it doesn't block release gating on an environmental dependency. Pushing the 20 local commits to `origin` is a safe fast-forward whenever the user chooses to do it, but was intentionally left undone here.
