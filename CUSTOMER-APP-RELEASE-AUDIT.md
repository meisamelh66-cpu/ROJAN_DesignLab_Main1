# ROJAN Customer App — Public MVP Release-Readiness Audit

**Date:** 2026-09-09 · **Type:** read-only audit — no code changed, no build run, no commit, no device action
**Repo:** `C:\AndroidProjects\ROJAN_DesignLab` · remote `github.com/meisamelh66-cpu/ROJAN_DesignLab_Main1`
**Branch inspected:** `chore/adopt-ecosystem-governance-v2` @ `a582eda` (2026-09-04)
**Target:** the `customer` product flavor of the single `:app` module (`ai.rojan.designlab`)
**Prior reports cross‑checked:** `ROJAN_Independent_Release_Readiness_Audit_v1.md` (2026‑08‑15), `ROJAN_MOBILE_RELEASE_AUTHORIZATION_v1.md` / `ROJAN_MOBILE_FINAL_EXECUTION_CHECKLIST_v1.md` (2026‑08‑26), `ROJAN_AI_Customer_Journey_Audit_PhaseA.md` (2026‑08‑12), `ROJAN_Customer_Git_Status_Report_v1.md`, `ROJAN_Android_Pilot_Session_Handoff_Report_v4.md`

> **Note on the prior reports:** all four named reports predate the September work. Several of their headline findings are now **resolved in code** (R8 disabled → enabled; the 4 Journey‑Audit P0s → fixed; booking‑flow ViewModels untested → test‑backfilled). This audit reflects the **2026‑09‑04 tree**, not those reports' snapshots. Where a prior finding still stands, it is marked "carried forward".

---

## A) Current completion percentage

### **≈ 65 / 100 to public MVP release**

| Area | Weight | Score | Notes |
|---|---|---:|---|
| Architecture & layering | 12% | 92 | Clean Architecture; `domain/` has zero `android.*`/`androidx.*`; manual composition‑root DI; `SafeApiCall` centralizes every network failure into a typed `Result`; zero swallowed exceptions; zero `TODO/FIXME` in `app/src`. |
| Data / API layer | 12% | 88 | 30+ Retrofit APIs + 20 DTO files (61 `@Serializable` DTOs); **15/15 customer repository interfaces implemented** against the real backend; `NetworkConfig` fails loudly on a blank base URL. Reminder feature is the one in‑memory store (`InMemoryReminderRepository`, `DemoReminderPreference`) — not persisted, not backend. |
| Authentication | 10% | 78 | Phone + OTP, **entirely server‑side**; AES‑256‑GCM token storage with a non‑exportable Keystore key (`SecureTokenStore`); OkHttp `Authenticator` refresh capped at 1 retry; session survives cold start. **Never run on a physical device for this flavor.** Dead email/password `login`/`register` methods still present (zero call sites). |
| Booking journey (code) | 14% | 74 | Full flow: Search → Service → **Specialist** (P0‑1 fixed: `BookingStepResolver` now gates on `specialistId`) → Date → Time → Confirmation → Success. `paymentMethod` now threaded into `BookingState` (P0‑2 fixed) — **verify the backend booking contract actually carries it**. Booking‑lifecycle ViewModels now have tests (`5B‑8B/8C/8D` backfill commits). **Never validated end‑to‑end on any surface.** |
| Discovery / salon / service / specialist screens | 8% | 80 | Salon list, salon profile, service details, specialist profile/selection, search — all wired to real repositories with Coil remote images. No device validation. |
| Customer Home | 6% | 62 | 11–section stacked feed. Journey Audit flagged missing section titles + dead taps (Featured/Top Specialists/Promotions were a documented scope decision; **Upcoming Bookings dead‑tap was not**). Partial fixes present; needs a focused pass + device check. |
| Profile / account | 8% | 55 | Logout menu item now present ("خروج از حساب"); hardcoded fake name ("رها احمدی", Journey Audit P0‑4) **removed**. But **≈ 7 of ~14 profile sub‑screens are ~45‑line stubs**: Wallet, Loyalty, Membership, Coupons, MyReviews, Waitlist, BeautyTimeline. Only Appointments, Favorites, FollowedSalons, Reschedule, BeautyDna (213 lines) are real. |
| Release / build config | 10% | 68 | R8 minify + resource shrink **ON** for release (Sprint 5A‑2); `proguard-rules.pro` minimal + reasoned; `ProductionRelease` hard‑gated at Gradle config time on a missing `PRODUCTION_API_BASE_URL`; signing config scaffolded (reads `keystore.properties` / `*.jks`, both gitignored & absent). **Gaps:** `versionCode = 1` / `versionName = "1.0.0"` never bumped; `StagingRelease` **not** gated (builds with blank URL); no `keystore.properties`/`.jks` present. |
| Store identity & publication | 8% | 15 | **`@string/app_name` = "ROJAN_DesignLab"** for the customer flavor (manager/reception override it, customer does not). Generic `@mipmap/ic_launcher`. `applicationId = ai.rojan.designlab` (a "designlab" package — permanent once published). No Play listing, no privacy policy, no data‑safety form, no signed AAB. |
| Crash / error handling | 6% | 45 | Error handling in‑app is strong (typed `Result`, centralized). **No crash reporting of any kind** — no Crashlytics/Sentry dependency, no custom `Application` class, no `Thread.setDefaultUncaughtExceptionHandler`. R8 keeps `SourceFile`/`LineNumberTable` + emits `mapping.txt`, so crashes *could* be deobfuscated — but nothing captures them. |
| Test coverage | 6% | 55 | 42 unit + 9 instrumented (was 23 total on 2026‑08‑15). Booking‑flow + auth + relationship + token‑authenticator + booking‑engine covered. **Repository implementations still largely untested**; `androidTest` is screenshot‑only (zero interaction‑driven Compose UI tests). |
| Device validation | (gate) | 5 | **Zero physical‑device validation for the customer flavor, ever, for any flow.** All on‑device evidence on record (Handoff v4) is Manager. This is the single biggest unknown. |

**What the remaining ~35% is:** (1) zero device validation, (2) no staging/production backend URL, (3) store‑identity items (name, icon, package decision, listing, privacy policy), (4) no crash reporting, (5) ~7 stub profile screens, (6) release signing material, (7) `origin/main` is **101 commits behind** this branch — nothing has reached a release train.

---

## B) Already production‑ready parts

These are code‑complete, audited clean, and (unless noted) need only device confirmation — not more building:

1. **Clean Architecture & layering.** `domain/` (77 files across flavors) has no Android imports; `ReminderScheduler` shows the interface‑in‑domain / impl‑in‑data pattern done right.
2. **Networking & error handling.** `SafeApiCall` wraps every repository call; a repo‑wide grep finds **no `catch (Exception/Throwable)` outside `SafeApiCall` itself**. `NetworkConfig.BASE_URL` `check()`s for a blank URL.
3. **Token security.** Access/refresh tokens AES‑256‑GCM encrypted with an Android‑Keystore‑backed, non‑exportable key; only IV‑prefixed ciphertext hits `SharedPreferences`. `TokenAuthenticator` does a non‑intercepted refresh, 1 retry max, clears everything on a failed refresh.
4. **Auth flow (code).** Phone + OTP, verification 100% server‑side; no client‑side bypass or test credentials anywhere; `isSubmitting` loading state now exists in `AuthViewModel`.
5. **Backend‑authority discipline.** No client‑side pricing/tax/payment computation; no local booking authority (create/confirm/complete/reschedule all call the backend); server responses (401/403/404/409) treated as the source of truth; client‑side role checks are commented as UX‑only.
6. **Release build hardening.** R8 minification + resource shrinking ON; `proguard-rules.pro` deliberately minimal with a documented rationale per library; `ProductionRelease` **cannot be built** without a real `PRODUCTION_API_BASE_URL` (Gradle `taskGraph.whenReady` guard).
7. **Config hygiene.** `.gitignore` excludes `local.properties`, `*.jks`/`*.keystore`, `keystore.properties`; none tracked in git. `network_security_config.xml` (cleartext exceptions for `10.0.2.2`/`localhost` only) lives under `src/debug/` and never merges into release.
8. **Manifest surface.** Single permission: `INTERNET`. Portrait‑locked (documented: no landscape‑ready screen exists). `role_preferences` DataStore excluded from cloud backup + device transfer (prevents stale‑role routing on reinstall).
9. **Booking‑engine domain model.** `BookingEngine` + rule groups (`BookingStepResolver`, `BookingValidationRules`, `BookingStateCompletion`, `BookingIntentResolver`, promotion/AI‑recommendation rules) — pure, unit‑tested (`BookingEngineTest`, `RollingBookingDatesTest`).
10. **Debug build works.** `build_output.log`: `assembleCustomerDevDebug` → `BUILD SUCCESSFUL`, `EXIT_CODE:0`. (Not re‑run in this audit; last recorded plus the 2026‑08‑15 audit's confirmation of all three flavors building.)

---

## C) Remaining blockers

### Hard blockers (cannot ship a public MVP without these)

| # | Blocker | Evidence |
|---|---|---|
| C‑1 | **Zero device validation of the customer app.** No install, launch, auth, API‑connection, session, or booking‑journey pass on record for the `customer` flavor. Carried forward from `ROJAN_MOBILE_RELEASE_AUTHORIZATION_v1.md` Phase 4 / `FINAL_EXECUTION_CHECKLIST` Blocker #3. | Handoff v4 is all Manager; no customer device report exists anywhere in the repo. |
| C‑2 | **No staging or production backend URL.** `STAGING_API_BASE_URL` / `PRODUCTION_API_BASE_URL` unset. A release build points at nothing. `ProductionRelease` is correctly un‑buildable without it; **`StagingRelease` is not gated** and will build a broken APK silently. | `app/build.gradle.kts` flavors; `NetworkConfig.kt`; Independent Audit §6.2. |
| C‑3 | **No release signing material.** No `keystore.properties`, no `.jks` in the environment → every release build is **unsigned**. Cannot upload an unsigned AAB to Play. | `app/build.gradle.kts` signingConfigs guarded by `keystorePropertiesFile.exists()`; Independent Audit §6.3. |
| C‑4 | **Customer app ships as "ROJAN_DesignLab".** `@string/app_name` has no `customer`‑flavor override; the launcher label and in‑app title are literally `ROJAN_DesignLab`. Generic `@mipmap/ic_launcher` (no customer branding). | `app/src/main/res/values/strings.xml`; `app/src/main/AndroidManifest.xml`; no `app/src/customer/` source set exists. |
| C‑5 | **`applicationId` decision not made.** `ai.rojan.designlab` is a lab identifier and is **permanent once published**. Ship under it, or migrate to a real ID (e.g. `ir.rojanai.customer`) before first publish. CLAUDE.md freezes it for the *Manager‑split* rationale, not as a *production* decision. | `defaultConfig.applicationId`; CLAUDE.md "Customer App (frozen)". |
| C‑6 | **No crash reporting.** No Crashlytics/Sentry, no `Application` class, no default uncaught‑exception handler. A public launch would be blind to production crashes. | `app/build.gradle.kts` dependencies; `AndroidManifest.xml` has no `android:name` on `<application>`. |
| C‑7 | **Play Console publication assets absent.** No store listing, screenshots, feature graphic, content rating, **privacy policy URL**, or Data‑Safety declaration. INTERNET‑only + auth + booking still requires a privacy policy and data‑safety form. | Not in repo; not applicable to code but blocks publish. |
| C‑8 | **Nothing has reached a release branch.** This branch is **101 commits ahead of `origin/main`, 0 behind**; `origin/main` is deliberately frozen. A merge/release‑train decision is a prerequisite. | `git rev-list --left-right --count origin/main...HEAD` → `0 101`. |

### Soft blockers (should fix before public, not strictly release‑gating)

| # | Item | Evidence |
|---|---|---|
| C‑9 | **≈ 7 profile sub‑screens are ~45‑line stubs** (Wallet, Loyalty, Membership, Coupons, MyReviews, Waitlist, BeautyTimeline). If they're reachable from the UI they'll read as broken; if they're not wired, dead nav entries. Decide: hide behind a flag, or finish, or cut for MVP. | File sizes + repo/VM reference counts. |
| C‑10 | **`versionCode = 1` / `versionName = "1.0.0"` never bumped.** Fine for a first upload, but there is no versioning discipline/CI bump. Every Play upload needs a unique, increasing `versionCode`. | `app/build.gradle.kts defaultConfig`. |
| C‑11 | **`StagingRelease` lacks the build‑time URL hard‑gate** `ProductionRelease` has. | Independent Audit §6.2, still open. |
| C‑12 | **Repository‑layer tests still thin.** Booking‑flow ViewModels are now covered, but most `*RepositoryImpl` / `AuthInterceptor` have no unit test. | `app/src/test` inventory vs `data/repository/*`. |
| C‑13 | **RTL not forced at the app root.** `LocalLayoutDirection` is never overridden; `supportsRtl="true"` only *permits* mirroring. Persian‑first is a top project rule; on an English system locale, `LazyRow` scroll direction etc. may flip. Needs device verification. | Journey Audit P1‑1; `MainActivity.kt` provides only `LocalRojanPalette`. |
| C‑14 | **`targetSdk = 37` / `compileSdk = 37`.** Confirm SDK 37 is a **stable** release (not preview) and that build‑tools/platform are installed on the build machine (env note: `sdkmanager` is offline here). | `app/build.gradle.kts`. |
| C‑15 | **Reminder feature is in‑memory only.** `InMemoryReminderRepository` (`mutableMapOf`), `DemoReminderPreference`, `ReminderScheduler` is a no‑op default. Reminders don't persist and don't schedule real notifications (and there's no `POST_NOTIFICATIONS` permission / `SCHEDULE_EXACT_ALARM`). Decide if reminders are in‑MVP. | `domain/reminder/*`. |
| C‑16 | **`allowBackup="true"`** with only `role_preferences` excluded. Encrypted token ciphertext could be backed up (harmless — key isn't), but session/active‑salon DataStore could restore stale. Consider `allowBackup="false"` or excluding all session files. | `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`. |
| C‑17 | **Legacy naming: `DemoIdentityProvider` / `DemoSessionProvider` still constructed** in `AuthViewModelFactory` / `SessionViewModelFactory`. Per the factory's own doc comment they now only supply the **session‑state machine** (real backend repos do the actual auth) — **not fake auth** — but the "Demo" name is misleading and invites a future misread. Rename. | `AuthViewModelFactory.kt` doc comment + body. |

---

## D) P0 / P1 / P2 priority list

### P0 — blocks a public MVP release (do all before shipping)

- **P0‑1 — First customer‑flavor device validation pass.** Install `assembleCustomerDevDebug` (or a signed staging build) on a real device; run install → launch → OTP auth → API connection (logcat evidence) → session persistence → **full booking journey end‑to‑end**. Report PASS / FAIL / BLOCKED per step with real evidence, per `ROJAN_MOBILE_RELEASE_AUTHORIZATION_v1.md` Phase 4. *(C‑1)*
- **P0‑2 — Provision & wire a real backend URL.** Stand up (or confirm) the production backend; set `PRODUCTION_API_BASE_URL` (and `STAGING_API_BASE_URL`) via CI‑injected Gradle property, never committed. *(C‑2)*
- **P0‑3 — Provision release signing.** Generate the upload keystore, store `keystore.properties` + `.jks` out of git, deliver via secure channel / CI secret. Verify `assembleCustomerProductionRelease` produces a **signed** AAB. *(C‑3)*
- **P0‑4 — Fix customer app identity.** Add a `customer`‑flavor `app_name` override ("روژان" / "ROJAN"), a real customer launcher icon (adaptive), and make + record the `applicationId` decision (keep `ai.rojan.designlab` or migrate now). *(C‑4, C‑5)*
- **P0‑5 — Add crash reporting.** Firebase Crashlytics (or Sentry), a custom `Application` class to init it, upload the R8 `mapping.txt` per release. *(C‑6)*
- **P0‑6 — Play Console prerequisites.** Privacy policy URL, Data‑Safety form, content rating, store listing + screenshots, internal‑testing track set up. *(C‑7)*
- **P0‑7 — Verify `paymentMethod` reaches the backend.** Confirm the booking create DTO (`BookingDtos.kt`) actually carries `paymentMethod`; if the backend contract has no such field, either remove the selector for MVP or get System 1 to add the field (cross‑boundary — needs confirmation). *(B‑item; Journey Audit P0‑2 is only half‑closed.)*
- **P0‑8 — Release‑train decision for `origin/main`.** Decide merge vs. reset; get this branch's 101 commits onto a releasable ref. *(C‑8)*

### P1 — important; fix before or immediately after first public build

- **P1‑1 — Decide the ~7 stub profile screens.** Finish, feature‑flag‑hide, or cut for MVP — no reachable stub screens in a public build. *(C‑9)*
- **P1‑2 — Gate `StagingRelease`** with the same build‑time URL check `ProductionRelease` has. *(C‑11)*
- **P1‑3 — Force RTL at the app root** (`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` in `MainActivity`), then verify on a non‑Persian system locale. *(C‑13)*
- **P1‑4 — `versionCode` / `versionName` strategy.** CI‑driven `versionCode` (e.g. build number), semantic `versionName`. *(C‑10)*
- **P1‑5 — Repository‑layer unit tests** for `BookingRepositoryImpl`, `AuthSessionRepositoryImpl`, `TokenRepositoryImpl`, `AuthInterceptor`, plus at least one interaction‑driven Compose UI test for the booking happy path. *(C‑12)*
- **P1‑6 — Reminder feature decision.** In‑MVP → build real persistence + `WorkManager`/`AlarmManager` scheduling + `POST_NOTIFICATIONS` permission + runtime request. Not in‑MVP → hide the reminder UI. *(C‑15)*
- **P1‑7 — Customer Home pass.** Wire the Upcoming‑Bookings card tap; add section titles; decide Featured/Top‑Specialists/Promotions (wire or hide). *(C‑10 in Journey Audit; Home score 62.)*
- **P1‑8 — Confirm SDK 37 is stable** + present in CI's SDK; pin build‑tools. *(C‑14)*
- **P1‑9 — Confirm the Reception `MANAGER`‑role gate is irrelevant to Customer** (it is — Customer has no role gate) and that a customer OTP that auto‑registers as `CUSTOMER` lands correctly. *(cross‑check from Independent Audit §3.)*

### P2 — polish / debt; not release‑gating

- **P2‑1** — Remove dead email/password `login`/`register` in `AuthApi` / `BackendAuthRepositoryImpl` (zero call sites).
- **P2‑2** — Rename `DemoIdentityProvider` / `DemoSessionProvider` → `SessionStateMachine`‑style names; update doc comments. *(C‑17)*
- **P2‑3** — `allowBackup` review: `false`, or exclude all session/active‑salon DataStore files. *(C‑16)*
- **P2‑4** — Decompose `navigation/RojanNavGraph.kt` (~1,150 lines, the file‑size outlier).
- **P2‑5** — Journey‑Audit P1/P2 UX finishing: loading spinners on every submit button (VMs now expose `isSubmitting` — wire them), 48dp touch targets via the app's own `MinTouchTarget` token, `RojanEmptyState` everywhere, "resend OTP" + expiry countdown, guarded‑route bounce messaging, numeral/unit formatting consistency, booking step‑indicator, Success‑screen recap.
- **P2‑6** — Add explicit OkHttp timeouts (implicit 10s today).
- **P2‑7** — Consolidate the acknowledged Manager/Reception auth‑screen duplication (does not affect Customer, but shares the auth ViewModel patterns).
- **P2‑8** — Move the ~30 root‑level report `.md` files into `docs/reports/`; archive `ROJAN_AI_Production_Readiness_Report.md` (stale, contradicts current state).

---

## E) Exact sprint plan to public release

Assumes 1–2 Android engineers + backend/infra support. Each sprint = ~1 week.

### Sprint 1 — "Make it real" (backend + identity + signing)
**Goal: a signed, correctly‑branded staging build that talks to a real backend.**
- P0‑2: provision production + staging backend URLs; wire via CI Gradle properties. Add the `StagingRelease` build‑time gate (P1‑2).
- P0‑3: generate upload keystore; wire `keystore.properties`/`.jks` through CI secrets; confirm `assembleCustomerStagingRelease` is **signed**.
- P0‑4: `customer` `app_name` override + adaptive launcher icon; **decide `applicationId`** (recommendation: migrate to `ir.rojanai.customer` now — it's free before first publish and impossible after) and, if migrating, do it this sprint while nothing is published.
- P0‑7: verify `paymentMethod` in `BookingDtos` create request; if absent, raise with System 1 or drop the selector for MVP.
- P1‑4: CI `versionCode` from build number.
- **Exit:** `assembleCustomerStagingRelease` → signed AAB, real staging URL baked in, correct name + icon, `tsc`‑equivalent green (`./gradlew :app:testCustomerStagingUnitTest`, `lintCustomerStagingRelease`, `assembleCustomerStagingRelease`).

### Sprint 2 — "Prove it" (first device validation + crash visibility)
**Goal: the customer booking journey passes end‑to‑end on a real device, and crashes are captured.**
- P0‑1: full customer‑flavor device pass — install → launch → OTP (real SMS, authorized test number) → API connection (logcat) → session persistence → **book a real appointment end‑to‑end** → see it in Appointments. Report per‑step PASS/FAIL/BLOCKED with evidence.
- P0‑5: add Crashlytics + `Application` class; verify a forced test crash appears deobfuscated (upload `mapping.txt`).
- P1‑3: force RTL at app root; re‑verify the device pass on an English system locale.
- Fix whatever the device pass surfaces (expect 2–4 real defects — booking‑journey has never run E2E).
- **Exit:** a green device‑validation report for every row of the Release‑Authorization Phase 4 table; Crashlytics receiving events.

### Sprint 3 — "Finish the edges" (stubs, home, tests)
**Goal: no visible stub or dead‑tap in the shipping build; core paths test‑covered.**
- P1‑1: resolve all ~7 profile stub screens (finish the 2–3 that are MVP; feature‑flag‑hide the rest).
- P1‑7: Customer Home pass (Upcoming‑Bookings tap, section titles, Featured/Promotions wire‑or‑hide).
- P1‑6: reminder decision — build or hide.
- P1‑5: repository‑layer unit tests + 1 interaction‑driven Compose UI test for the booking happy path.
- P2‑5: submit‑button loading states (wire the VMs' existing `isSubmitting`), touch‑target pass, empty‑state standardization, resend‑OTP.
- **Exit:** every reachable screen is real; `./gradlew testCustomerStagingUnitTest connectedCustomerStagingAndroidTest` green; a second device pass clean.

### Sprint 4 — "Ship it" (store + release train)
**Goal: internal‑testing track live on Play, then closed beta.**
- P0‑6: privacy policy URL, Data‑Safety form, content rating, store listing (icon, feature graphic, ≥ 4 screenshots per form factor, description), internal‑testing track.
- P0‑8: execute the `origin/main` release‑train decision; tag a release commit.
- Build `assembleCustomerProductionRelease` (signed, prod URL); upload AAB to the **internal‑testing** track; smoke‑test via Play install.
- Promote internal → **closed beta** (10–50 real users) for 1–2 weeks; watch Crashlytics + ANR rate; triage.
- **Exit:** closed beta stable (crash‑free sessions ≥ 99%, no P0 defects for 5 days) → promote to production / open testing.

### Rough timeline
**~4 sprints (≈ 4–5 weeks)** of Android work to a closed beta, **+ 1–2 weeks** beta soak before public production. Critical path is Sprint 1 (needs backend + infra) and Sprint 2 (needs a real device + authorized test SMS number).

---

## Appendix — key facts captured (2026‑09‑04 tree)

```
Module:            single :app, flavors  target{customer,manager,reception} × environment{dev,staging,production}
customer flavor:   zero overrides → applicationId ai.rojan.designlab, app_name "ROJAN_DesignLab", @mipmap/ic_launcher
versionCode/Name:  1 / "1.0.0"        minSdk 24   targetSdk 37   compileSdk 37
Permissions:       INTERNET only
Stack:             Kotlin 2.2.10, AGP 9.3.1, Compose BOM 2026.06.00, Retrofit 2.11.0, OkHttp 4.12.0,
                   kotlinx.serialization 1.11.0, Coil 2.7.0, DataStore, Navigation-Compose, ViewModel-Compose.
                   No Hilt/Dagger (manual DI). No Room (DataStore only). No Crashlytics/Sentry.
Customer src:      ~200 Kotlin files in src/main — MainActivity, navigation/ (RojanNavGraph ~1150 lines),
                   screens/{auth,customer,search,salon,service,specialist,booking,bookingflow,profile,splash,dashboard},
                   presentation/ (25+ ViewModels + Factories), domain/ (booking engine + rules, identity, beauty, reminder),
                   data/{remote (30+ APIs, 20 DTO files), repository (15 impls), local, identity}, di/, ui/ (~65-file design system)
Release:           R8 minify + shrinkResources ON; proguard-rules.pro minimal+reasoned; ProductionRelease
                   Gradle-gated on PRODUCTION_API_BASE_URL; signing scaffolded, keystore ABSENT; StagingRelease NOT gated
Tests:             42 unit + 9 instrumented (androidTest = screenshot-only). Booking flow + auth + relationship covered;
                   repo impls largely uncovered.
Git:               branch chore/adopt-ecosystem-governance-v2 @ a582eda; 101 ahead / 0 behind origin/main (frozen).
                   Working tree clean except .claude/settings.json, .gitignore.
Device validation: customer flavor — NONE, ever. All on-device evidence on record is Manager (Handoff v4).
Journey Audit PhaseA P0s (2026-08-12): P0-1 specialist step → FIXED; P0-2 payment method → threaded into BookingState
                   (backend contract unverified); P0-3 first-name back → verify on device; P0-4 hardcoded name → FIXED.
Build:             build_output.log shows assembleCustomerDevDebug BUILD SUCCESSFUL (not re-run in this audit).
```
