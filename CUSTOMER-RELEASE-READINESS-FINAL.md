# ROJAN Customer App — Release Readiness Validation (FINAL)

**Date:** 2026-09-10 · **Device:** Samsung Galaxy A72 (SM-A725F), Android 14 · **Backend:** live
`https://api.rojanai.ir/` · **Tester account:** "گیتا" (+989164987585)
**No source modified. No commit.**

---

## FINAL RECOMMENDATION: ⛔ NOT READY FOR RELEASE

Five **P0** blockers stand between the current state and a public release. The app is
**functionally healthy for browsing** (no crashes or ANRs across ~70 min of testing, RTL correct,
error handling calm) but **cannot be built as a signed production artifact**, **has no privacy
policy** (required by Google Play for an app that collects phone numbers), **ships under an internal
name** (`ROJAN_DesignLab`), and — most seriously — **the core booking flow cannot be completed**
against the live backend.

---

## 1. PASS / FAIL summary

| Area | Result | Notes |
|---|---|---|
| Clean build — `customerDevRelease` (assemble + R8 + shrink + lint) | ✅ PASS | BUILD SUCCESSFUL, 15m53s, `app-customer-dev-release-unsigned.apk` = **4.3 MB** |
| Clean build — `customerProductionRelease` | ⛔ **FAIL** | Hard build-time gate: `Missing required property: PRODUCTION_API_BASE_URL` |
| Release signing | ⛔ **FAIL** | APK is `…-unsigned.apk`. No `keystore.properties`, no keystore file present |
| `lintCustomerDevRelease` | ⚠️ PASS w/ warnings | 0 errors, **93 warnings** (all non-blocking — see §3) |
| Backend URL configuration | ⛔ **FAIL** | Only the `dev` flavor resolves a URL (from git-ignored `local.properties`); `staging`/`production` have none |
| App identity (name / icon / theme) | ⛔ **FAIL** | `app_name` = **`ROJAN_DesignLab`** (internal, has an underscore) |
| Privacy policy | ⛔ **FAIL** | None anywhere in the app or config; app collects phone numbers |
| Crash reporting | ⚠️ GAP | No Crashlytics / Sentry / any — zero production crash visibility |
| Cold launch + session restore | ✅ PASS | `REL01`/`REL02` — restores "گیتا", loads Home from live backend |
| Home | ✅ PASS | `REL02` — greeting, search, CTA, 2 real salons, bottom nav |
| Explore (authenticated) | ✅ PASS | `REL03` — real salon list, RTL correct |
| Explore (guest / logged-out) | ⛔ **FAIL** | `REL14`/`REL15` — **error wall** "برای این عملیات نیاز به ورود مجدد دارید"; salons never load for a guest. Retry does not recover. |
| Search | ✅ PASS (old UI) | `REL04` — 12 real results. Un-redesigned glass style (P2) |
| Salon Detail | ✅ PASS | `REL06` — services / specialists / hours from live backend |
| Service Detail | ✅ PASS | `REL07` |
| Specialist selection | ⚠️ N/T here | Auto-skipped for the pilot salon (1 specialist); verified in prior sessions via edit-from-confirmation |
| Date | ⚠️ N/T here | Auto-skipped for the pilot salon (1 available day); verified in prior sessions |
| Time | ✅ PASS | `REL08` — live availability grid, step 4/5 |
| Confirmation | ✅ PASS | `REL09` — summary, payment method, step 5/5 |
| **Booking completion → Success** | ⛔ **FAIL** | `REL10` — `POST /api/v1/bookings` → **HTTP 409** every attempt (4+ across sessions, multiple slots). Success screen unreachable. App shows the backend message calmly, no crash. |
| API failure handling | ✅ PASS | 409 / 401 / 429 all render as calm inline errors; no crash, form stays usable |
| Profile | ✅ PASS | `REL11` — redesigned, live data |
| Appointments | ✅ PASS (empty) | Empty state renders; populated state not testable ("گیتا" has 0 bookings — booking creation blocked) |
| Appointment Details | ⚠️ N/T | Unreachable (0 bookings, no deep-links). Redesign correct-by-construction |
| Reschedule | ⚠️ N/T | Same as above |
| Favorites | ✅ PASS (old UI) | `REL12` — shows "گیتا"'s favorited salon. Un-redesigned glass style (P2) |
| Logout | ✅ PASS | `REL13`/`REL14` — confirm dialog → clears session → routes to Explore |
| Login redirect (guarded tab) | ✅ PASS | `REL17` — guest taps نوبت‌ها → Auth screen |
| OTP request | ✅ PASS | `REL18` — real SMS delivered; `POST /auth/otp/request` |
| OTP request rate-limiting | ✅ PASS (enforced) / ⚠️ (copy) | `REL24` — burst requests → **HTTP 429**, but shown to user as generic "خطایی در ارتباط با سرور رخ داد." |
| OTP verify — happy path | ✅ PASS | Verified end-to-end in the **Auth Redesign task earlier today** on this exact build (phone → real SMS → correct code → verified → logged in → session restored) |
| OTP verify — re-test this session | ⚠️ CONCERN | `REL22`/`REL28` — fresh codes (5999, 8271) → **HTTP 401** after the rate-limiter was triggered. Consistent with a backend anti-abuse lockout on the number; the app surfaces only a generic error for this state |
| RTL correctness | ✅ PASS | Every screen tested — correct |
| Keyboard / IME | ✅ PASS | `imePadding()` keeps CTAs above the keyboard on Auth |
| Back handling | ✅ PASS | Back works from every screen; Auth back clears the code and returns to phone entry |
| Crashes | ✅ PASS | **Zero** `FATAL EXCEPTION` in ~70 min of heavy navigation |
| ANR | ✅ PASS | **Zero** `ANR in ai.rojan.designlab` |
| Broken navigation | ✅ PASS | No dead ends found (guest Explore error is a data/gating issue, not nav) |
| Empty states | ✅ PASS | Appointments / Favorites / booking-slots empties all render cleanly |
| Loading states | ✅ PASS | Session restore, salon lists, slot grids — all show proper loading UI |
| Permissions | ✅ PASS (minimal) | Only `INTERNET`. No runtime permission prompts |
| Memory / performance | ⚠️ NOTE | Snappy, no jank. `TOTAL PSS ≈ 161 MB` after 70 min — on the high side (Coil bitmap caches), no Activity leak (`Activities: 1`) |
| Appointment reminder feature | ⛔ **FAIL** | `NoOpReminderScheduler` — the "یادآوری نوبت" toggle **does nothing**: no AlarmManager, no notification, no WorkManager. A shipped promise the app never keeps. |
| Backup settings | ⚠️ MINOR | `allowBackup=true`; only `role_preferences` excluded. `auth_session_preferences` (logged-in person id) is **not** excluded. Tokens are Android-Keystore-encrypted so their backup is inert. |
| Play Store policy blockers | ⛔ **FAIL** | Privacy policy (P0), Data Safety form (needs auth), signed AAB (P0) |

---

## 2. Blockers by severity

### P0 — must fix before any public release

| # | Blocker | Exact file(s) responsible | Fix |
|---|---|---|---|
| P0-1 | **No production build path.** `assembleCustomerProductionRelease` hard-fails: `Missing required property: PRODUCTION_API_BASE_URL`. `assembleCustomerRelease` is ambiguous (dev/staging/production variants). | `app/build.gradle.kts` (`productFlavors { create("production") }`, `gradle.taskGraph.whenReady` guard); `app/src/main/java/ai/rojan/designlab/data/remote/NetworkConfig.kt` | Decide the release environment. If prod = `api.rojanai.ir`, build with `-PPRODUCTION_API_BASE_URL=https://api.rojanai.ir/` (CI-injected, never committed). Confirm `production` flavor has no `applicationIdSuffix` (it doesn't — good). |
| P0-2 | **APK is unsigned.** Output is `app-customer-dev-release-unsigned.apk`. `keystore.properties` and the keystore file are absent on this machine, so the `release` signing config resolves to nothing and `buildTypes.release.signingConfig` is never set. | `app/build.gradle.kts` (`signingConfigs.create("release")`, guarded by `keystorePropertiesFile.exists()`); missing `keystore.properties` + `keystore/*.jks` at repo root | Generate a release keystore (upload key for Play App Signing), place `keystore.properties` + `.jks` (both git-ignored) on the release machine / CI. Note the sample path references `rojan-manager-release.jks` — a Customer key may need creating. |
| P0-3 | **No privacy policy.** The app collects a **phone number** + creates an account + stores auth tokens + talks to a remote backend. Google Play requires a privacy policy URL and a completed Data Safety form for this. No policy URL exists in the app, manifest, strings, or store config. | (absent) — needs a hosted policy + a link in-app (Profile → "امکانات حساب" or a Settings entry) and in the Play Console listing | Publish a Persian privacy policy, add an in-app link, fill the Play Data Safety form. |
| P0-4 | **App name is an internal identifier.** `app_name` = `ROJAN_DesignLab` — this is what shows in the launcher, task switcher, and "App info". Underscore + "DesignLab" is not a consumer-facing name. | `app/src/main/res/values/strings.xml:2` (`<string name="app_name">ROJAN_DesignLab</string>`) — inherited by the `customer` flavor (Manager/Reception override it) | Set to the real consumer name (e.g. `روژان` or `ROJAN`). Consider also renaming `Theme.ROJAN_DesignLab` (cosmetic) and — separately — whether `applicationId = "ai.rojan.designlab"` (P1) is the intended permanent package. |
| P0-5 | **Core booking flow cannot be completed.** `POST /api/v1/bookings` returns **HTTP 409** ("این عملیات با وضعیت فعلی سازگار نیست") on every attempt — 4+ tries, multiple time slots, multiple sessions, on the only fully-configured salon (ROJAN AI Pilot Salon). The Booking Success screen has never been reachable on-device. | App side is behaving correctly (`app/src/main/java/ai/rojan/designlab/presentation/booking/BookingConfirmationViewModel.kt` sends a well-formed POST and surfaces the 409). **Backend / salon config** is rejecting the booking. | Root-cause the 409 with the backend team. Confirm a real customer can complete a booking against a **production** salon before release. A booking app that can't take a booking is not shippable. |

### P1 — strongly recommended before release

| # | Issue | File(s) | Notes |
|---|---|---|---|
| P1-1 | **Guest Explore is an error screen.** A logged-out user (every fresh install) opens the main "کشف سالن‌ها" tab and sees "مشکلی پیش آمد / برای این عملیات نیاز به ورود مجدد دارید" instead of a browsable salon list. `GET /api/v1/salons` appears to require auth. Poor first-run funnel. | `app/src/main/java/ai/rojan/designlab/screens/customer/CustomerHomeScreen.kt` + its `SalonListViewModel`; backend `salons` endpoint auth requirement | Either make salon discovery genuinely public (backend), or give guests an explicit "sign in to browse" call-to-action instead of an error card. |
| P1-2 | **Appointment reminder toggle is a no-op.** `NoOpReminderScheduler.schedule()` / `.cancel()` are empty bodies (by design, per a past instruction — never wired to a real implementation). Users who enable "یادآوری نوبت" get no reminder, ever. Models are named `DemoReminderPreference`. | `app/src/main/java/ai/rojan/designlab/domain/reminder/ReminderScheduler.kt` (`NoOpReminderScheduler`); `ReminderModels.kt` (`DemoReminderPreference`); `presentation/booking/ReminderViewModel.kt`; the toggle in `screens/profile/AppointmentsScreen.kt` | Wire a real AlarmManager/WorkManager scheduler + a notification channel + the `POST_NOTIFICATIONS` permission (Android 13+ runtime request — the A72 is Android 14), **or** hide the toggle for v1. |
| P1-3 | **No crash reporting.** Zero production crash/ANR visibility after launch. | `app/build.gradle.kts` dependencies (none), no `google-services.json` | Add Crashlytics or Sentry; upload the R8 `mapping.txt` (already retained — `proguard-rules.pro` keeps `SourceFile,LineNumberTable`). |
| P1-4 | **429 shown as a generic error.** OTP rate-limit (`HTTP 429`) surfaces as "خطایی در ارتباط با سرور رخ داد." — a user who requests a code twice quickly has no idea to wait. | `app/src/main/java/ai/rojan/designlab/presentation/common/` (the `userMessageFor` mapper) | Map 429 to a "درخواست‌های زیاد؛ کمی صبر کنید" message, ideally with the `Retry-After` window. |

### P2 — polish / post-launch

| # | Issue | File(s) |
|---|---|---|
| P2-1 | Un-redesigned screens still on the old glass UI (orb back button, ✦ sparkle cards, violet icons): **Search**, **Favorites**, Followed Salons, Wallet/Coupons/Membership/Loyalty, My Reviews, Beauty Timeline, Beauty DNA, Salon List, Specialist Profile, Public Salon. | see `CUSTOMER-VISUAL-DEBT-AUDIT.md` |
| P2-2 | `auth_session_preferences` DataStore (logged-in person id) is **not** excluded from Auto Backup / device transfer — a stale session id could restore onto a fresh install. Tokens can't be decrypted post-restore (Keystore key is device-bound), so it self-heals to logged-out, but it's inconsistent with the deliberate `role_preferences` exclusion. | `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`; `app/src/main/java/ai/rojan/designlab/data/local/AuthSessionDataStore.kt` |
| P2-3 | OTP "wrong / expired code" (`HTTP 401`) shows the same generic "برای این عملیات نیاز به ورود مجدد دارید" — not "کد اشتباه است". | `presentation/common/` `userMessageFor` |
| P2-4 | OTP code field enforces no max length (accepted a 5-digit paste). | `app/src/main/java/ai/rojan/designlab/screens/auth/AuthScreen.kt` (`AuthField`, code branch) |
| P2-5 | `targetSdk = 37` (Android 16+): `android:screenOrientation="portrait"` will be **ignored** on Android 16+ (lint `DiscouragedApi`). Every screen is a vertical Column with no landscape layout — they will render broken when the lock stops working. | `app/src/main/AndroidManifest.xml` (`.MainActivity`) |
| P2-6 | Launcher icon is the glossy 3D `ic_launcher` (design debt already flagged). Lint also reports 42× `IconLocation` / `IconDuplicates` / `IconXmlAndPng` — icons in wrong density folders / PNG+XML dupes. | `app/src/main/res/mipmap-*`, `CUSTOMER-VISUAL-DEBT-AUDIT.md` |
| P2-7 | `versionCode = 1`, `versionName = "1.0.0"` — fine for a first submission; just confirm the CI bumps `versionCode` per upload. | `app/build.gradle.kts` |
| P2-8 | 39× `UnusedResources` lint warnings — dead drawables/strings (shrinker removes them from the APK; source cleanup only). | various `res/` |
| P2-9 | Memory `TOTAL PSS ≈ 161 MB` after extended use — monitor; likely Coil's bitmap memory cache. Not a blocker. | Coil config in `di/BackendApiContainer.kt` / image call sites |

---

## 3. Clean build detail

```
:app:clean :app:assembleCustomerDevRelease :app:lintCustomerDevRelease
  → BUILD SUCCESSFUL in 15m 53s (53 tasks)
  → minifyCustomerDevReleaseWithR8            ✅  (R8 full mode, no keep-rule failures)
  → shrinkCustomerDevReleaseRes / convertShrunkResources ✅
  → lintVitalAnalyzeCustomerDevRelease        ✅  (0 fatal)
  → lintReportCustomerDevRelease              ✅  93 warnings / 0 errors
  → app-customer-dev-release-unsigned.apk     4,324,403 bytes

:app:assembleCustomerProductionRelease
  → BUILD FAILED — GradleException: "Missing required property: PRODUCTION_API_BASE_URL"
    (raised at task-graph-ready, before any compilation)
```

**Lint warning classes (0 errors):** `UnusedResources` ×39, `IconLocation` ×36, `IconDuplicates`
×5, `UseKtx` ×4, `UseOfNonLambdaOffsetOverload` ×2, `ModifierParameter` ×2, `DiscouragedApi` ×1
(portrait lock, see P2-5), `LockedOrientationActivity` ×1, `RedundantLabel` ×1, `IconXmlAndPng` ×1,
`UseTomlInstead` ×1. None gate a release.

## 4. Release checklist — verified values

| Item | Value | Verdict |
|---|---|---|
| `applicationId` | `ai.rojan.designlab` | ⚠️ P1 — "designlab" for a consumer app; confirm it's the intended permanent package before first publish (can't change after) |
| `namespace` | `ai.rojan.designlab` | — |
| App name (`app_name`) | `ROJAN_DesignLab` | ⛔ P0-4 |
| Theme name | `Theme.ROJAN_DesignLab` | P2 (not user-visible) |
| Launcher icon | `@mipmap/ic_launcher` (+ round) — glossy 3D | P2-6 |
| `versionCode` | `1` | ✅ (P2-7: ensure CI bumps) |
| `versionName` | `1.0.0` | ✅ |
| `minSdk` / `targetSdk` / `compileSdk` | 24 / 37 / 37 | ✅ (P2-5 caveat on orientation @ SDK 16+) |
| Signing | none — unsigned APK | ⛔ P0-2 |
| Backend URL (release) | not configured (`production`/`staging` empty; `dev` reads git-ignored `local.properties` = `https://api.rojanai.ir/` on this machine) | ⛔ P0-1 |
| Crash reporting | none | ⚠️ P1-3 |
| Privacy policy | none | ⛔ P0-3 |
| `allowBackup` | `true`, rules exclude only `role_preferences` | ⚠️ P2-2 |
| Permissions | `INTERNET` only | ✅ (P1-2: needs `POST_NOTIFICATIONS` if reminders are wired) |
| Data extraction rules | present (API 31+), excludes `role_preferences` | ⚠️ P2-2 |
| Cleartext traffic | default (blocked on API 28+); backend is HTTPS | ✅ |
| Obfuscation mapping | `SourceFile`/`LineNumberTable` retained in `proguard-rules.pro` | ✅ (but nowhere to upload it — P1-3) |
| Token storage | AES-256-GCM via Android Keystore, ciphertext-only in SharedPreferences | ✅ solid |

## 5. What was NOT verifiable this session

- **Booking completion / Booking Success screen** — backend 409 (P0-5).
- **Appointments (populated), Appointment Details, Reschedule** — "گیتا" has 0 bookings because
  booking creation is blocked; no deep-links. Their redesigns are correct-by-construction (all use
  foundation primitives verified on other screens this session).
- **Specialist selection / Date screens in the forward flow** — auto-skipped by the pilot salon's
  config (1 specialist, 1 open day); verified in earlier sessions via edit-from-confirmation.
- **A signed / production-configured artifact** — cannot be produced (P0-1, P0-2). All device
  testing used `customerDevDebug` (debuggable, un-minified) pointed at the live backend — **UI and
  API behaviour are representative; R8/signing/shrink behaviour of a true release build was only
  validated at build time (`customerDevRelease` assembled + linted clean).**

---

**Bottom line:** the app is stable and the redesigned surfaces look and behave well, but it is
**not releasable** until: a production build can be signed and built (P0-1, P0-2), a privacy policy
exists (P0-3), the app is renamed (P0-4), and — critically — a booking can actually be completed
end-to-end against a production salon (P0-5). Recommend re-running this checklist after those five
are closed.
