# ROJAN Customer App — Real-Device Smoke Test

**Date:** 2026-09-09 · **Type:** validation-only. No source, Gradle, `applicationId`, signing, or backend config was modified. No commit. The only write action was installing the existing **debug** variant on the connected device (explicitly authorized by the task).
**Repo:** `C:\AndroidProjects\ROJAN_DesignLab` · branch `chore/adopt-ecosystem-governance-v2` @ `a582eda`
**Tester method:** ADB + `am start` + `adb shell input` taps + `screencap` + `logcat` capture. 26 screenshots + full session logcat retained in the run scratchpad.

---

## Result summary

| Item | Verdict | Evidence |
|---|---|---|
| ADB available | **PASS** | `adb.exe` v1.0.41 (37.0.1) at `…\Android\Sdk\platform-tools` (not on PATH) |
| Physical device connected & authorized | **PASS** | `adb devices -l` → `RZ8R81WPS2J … device` (state `device`, not `unauthorized`/`offline`). `ro.kernel.qemu=0` → real device, **not an emulator** |
| Build (`installDebug`) | **PASS (task name corrected)** | `installDebug` is **not a valid task** (two flavor dimensions). Used the real customer debug task `:app:installCustomerDevDebug` → **BUILD SUCCESSFUL in 2m 4s**, exit 0 |
| Installation | **PASS** | `Installing APK 'app-customer-dev-debug.apk' … Installed on 1 device.` Package `ai.rojan.designlab`, versionCode 1, versionName 1.0.0, DEBUGGABLE |
| Launch | **PASS** | `am start -W … ai.rojan.designlab/.MainActivity` → `Status: ok`, `LaunchState: COLD`, `TotalTime: 2426ms`; `topResumedActivity = ai.rojan.designlab/.MainActivity` |
| App starts successfully | **PASS** | Splash shown then auto-advances to Customer Home; no crash |
| Smoke test | **PASS** (core paths) / **NOT TESTED** (booking creation) | see per-flow table below |
| Crash / runtime errors | **PASS (none)** | Zero `FATAL EXCEPTION`, zero `AndroidRuntime` crash, zero ANR, zero StrictMode/leak across the whole session. Benign warnings only |
| Backend / network | **PASS with a product blocker** | Backend `https://api.rojanai.ir` is **live and healthy** (`/actuator/health` → `{"status":"UP"}`). All authed calls returned **200**. **Blocker:** guest (unauthenticated) discovery calls all return **401** — no anonymous browsing |

---

## Device

| Property | Value |
|---|---|
| Manufacturer / model | **Samsung SM-A725F** (Galaxy A72) |
| Android version | **14** (API level **34**) |
| Security patch | 2025-03-01 |
| ABI | arm64-v8a |
| Network during test | **LTE mobile data** (`MOBILE[LTE] CONNECTED … IS_VALIDATED`), airplane mode off |
| SIM | dual-SIM; the active SIM's number **is the authorized test number `09164987585`** (a real ROJAN OTP SMS was received on-device during the test) |
| Users | primary user `0` ("Morche") + Samsung "Secure Folder" (user 150). Test ran on user 0 |

## ADB status

- **Available**, not on `PATH`; resolved at `C:\Users\ELHAEE\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- `adb devices -l` → one entry: `RZ8R81WPS2J   device   product:a72qnsxx model:SM_A725F device:a72q`.
- Authorized (`device`, not `unauthorized`). Real hardware, not an emulator.

## Build & installation

- **Customer `applicationId` (identified from `app/build.gradle.kts` before install):** `ai.rojan.designlab` — the `customer` flavor has zero overrides, so it inherits `defaultConfig.applicationId`. (`manager` = `.manager`, `reception` = `.reception`; `staging` env adds `.staging`.)
- `.\gradlew.bat installDebug` — **not runnable**: the module has `target{customer,manager,reception} × environment{dev,staging,production}`, so there is no bare `installDebug` task. The correct existing task is **`installCustomerDevDebug`** (the `dev` environment reads `DEV_API_BASE_URL` from `local.properties`, which on this machine = `https://api.rojanai.ir/`). This is a task selection, not a config change.
- `JAVA_HOME` set per-command to the Temurin JDK 21.0.12 already present on the machine.
- Result: **BUILD SUCCESSFUL in 2m 4s**, `installCustomerDevDebug` → `Installed on 1 device.` **Debug-signed** (no release keystore involved).

## Launch

`am start -W -n ai.rojan.designlab/.MainActivity` → `Status: ok` · `LaunchState: COLD` · `TotalTime: 2426ms` · `WaitTime: 2437ms`. The system splash showed, then the app rendered **Customer Home** (guest, no session). `MainActivity` became `topResumedActivity`. No launch crash.

---

## Smoke-test results (each row = actually exercised on the SM-A725F)

| # | Flow | Verdict | What was observed on-device |
|---|---|---|---|
| 1 | **App launch** | **PASS** | Cold start ~2.4s to first frame; process `31392` started; MainActivity resumed |
| 2 | **Splash / startup** | **PASS** | System splash → auto-advanced to Home (no manual dismiss); no artificial hang observed |
| 3 | **Guest Home render** | **PASS** | Dark navy/purple canvas, glassmorphism + gold borders, **RTL Persian** correct; header greeting "سلام کاربر جان" (generic, no name); search bar; "سالن‌ها / خدمات" tabs; "خدمات محبوب" section shows **"به‌زودی" (coming soon) placeholder cards**; bottom nav bar present |
| 4 | **First backend call (guest)** | **PASS (app) / expected 401** | On Home load: `GET https://api.rojanai.ir/api/v1/bookings/mine?page=0&size=50` → **401** (445 ms). App handled it gracefully — no crash, empty sections |
| 5 | **Navigation — tab toggle** | **PASS** | Tapping "سالن‌ها" selected the tab; triggered `GET /api/v1/salons?page=0&size=20&sortDirection=ASC` → **401** as guest (retried once) |
| 6 | **Navigation — bottom nav (Search)** | **PASS** | Routed to a "جستجو" screen with a search field and a clean **"برای جستجوی سالن‌ها وارد شوید"** (sign-in-required) error card with a gradient "ورود" CTA — a proper guarded-route state, not a silent bounce |
| 7 | **Navigation — bottom nav (Calendar / Favorites / Profile) as guest** | **PASS** | Each correctly **redirects a guest to the login screen** (`🌸 سلام` / phone-entry). No stuck/blank screen |
| 8 | **Login / auth entry — request OTP** | **PASS** | Phone-entry screen renders; typed `09164987585`; tapped "ارسال کد تایید" → `POST https://api.rojanai.ir/api/v1/auth/otp/request` (31-byte body) → **200** (930 ms). Number normalized to `+989164987585` on the next screen (`PhoneNumberNormalizer` works) |
| 9 | **Login / auth entry — real OTP received** | **PASS** | A real SMS arrived on the device: *"کد تایید ورود شما **8251** می‌باشد … روژان"* (sender `0998 200 4676`) |
| 10 | **Login / auth entry — verify OTP** | **PASS** | Entered `8251`; tapped "تایید و ورود" → `POST /api/v1/auth/otp/verify` (45-byte body) → **200** (240 ms). Immediately: `GET /users/me` → **200**, `GET /users/me/salon-access` → **200**, `GET /bookings/mine` → **200** — session token now attached and accepted |
| 11 | **Customer Home (authenticated)** | **PASS (render) / placeholder content** | Header now shows the **real account name — "سلام گیتا جان"** (confirms the old hardcoded "رها احمدی" is gone). "خدمات محبوب" and "فعالیت اخیر" sections still render **hardcoded "به‌زودی" placeholder cards** even with a valid session |
| 12 | **Customer booking entry point — salon list** | **PASS** | "سالن‌ها" tab (authenticated): `GET /api/v1/salons` → **200**; **two real salons render** — "ROJAN AI Pilot Salon" (📍 Pilot Test Address, Tehran) and "بانوصبا" (📍 Yazd…); a "رزرو نوبت زیبایی" (Book appointment) CTA card with a real photo |
| 13 | **Customer booking entry point — salon detail** | **PASS** | Tapping "ROJAN AI Pilot Salon" loaded, all **200**: `GET /salons/{id}`, `/salons/{id}/categories`, `/salons/{id}/categories/{cat}/services`, `/salons/{id}/specialists`, `/salons/{id}/working-hours`, `/customer/followed-salons`, `/customer/favorite-salons`. Screen renders real name, description, address, phone `+989114050112`, **working hours "دوشنبه 09:00–18:00"**, and a specialists section |
| 14 | **Appointments ("نوبت‌های من")** | **PASS** | Loads (authenticated `GET /bookings/mine` → 200), shows a clean empty state: "هنوز نوبتی ندارید" |
| 15 | **Session persistence** | **PASS** | `am force-stop` + cold relaunch (new pid 4560) → app came back **still authenticated**: `GET /users/me` → 200, `/salon-access` → 200, `/bookings/mine` → 200. Encrypted token store restored the session across a full process kill |
| 16 | **Booking creation / confirmation** | **NOT TESTED** | Deliberately did **not** submit a real appointment — that would write real data into the live pilot salon. Everything up to the booking form is verified; the create/confirm step needs a separate controlled test |

---

## Crash / runtime errors

**None.** Full-session logcat (both process lifetimes) contains:
- **0** `FATAL EXCEPTION`, **0** `AndroidRuntime` crash, **0** ANR, **0** StrictMode violation, **0** leaked window/resource, **0** `NullPointerException` / Kotlin exception from app code.

Benign warnings observed (not defects):
- `W/ziparchive … base.dm: No such file or directory` — no baseline profile / dex-metadata in a debug build. Normal.
- `W/rojan.designlab Accessing hidden method SystemProperties->addChangeCallback (unsupported, reflection, allowed)` — a bundled library greylist hit; allowed, common.
- `W/InputManager-JNI … Splash Screen … disposed without first being removed` / `W/BpBinder Linking to death … no threads listening` — Samsung/framework startup teardown warnings.

**Performance note:** first cold start produced 3 long ("Davey!") frames — `1075 ms`, `943 ms`, `742 ms` — i.e. ~1 s of dropped frames during initial render (also 1 × `768 ms` on relaunch). Visible first-paint jank, no sustained jank, not a crash.

---

## Backend / network blockers

| # | Finding | Impact |
|---|---|---|
| N-1 | **Backend is live** — `https://api.rojanai.ir` returns `{"status":"UP"}` on `/actuator/health`; every authenticated endpoint returned **200** on a real LTE connection. This contradicts the older Aug reports ("no confirmed staging/production backend"). | *Positive.* The `dev` flavor points at a real, healthy backend. |
| N-2 | **No anonymous browsing.** `GET /api/v1/salons`, salon search, and `GET /bookings/mine` all return **401** for a guest. A first-time user who hasn't logged in sees only "coming soon" placeholder cards and a sign-in wall on every real action. | **Release blocker (product/backend).** Either the backend must expose public salon discovery (System 1 decision), or the app's first-run must go straight to auth instead of a half-populated guest Home. |
| N-3 | **Redundant identity calls.** One OTP-verify triggered `GET /users/me` **4×** and `GET /users/me/salon-access` **2×** in ~1 s. | Minor efficiency / battery; not blocking. |
| N-4 | Home "خدمات محبوب" / "فعالیت اخیر" sections make **no** API call — they are hardcoded "به‌زودی" placeholders shown even to authenticated users. | Content gap; reads as an unfinished app. |

---

## Other on-device observations

- **App name on device = "ROJAN_DesignLab"** (the `customer` flavor has no `app_name` override) — visible in the launcher/recents. Generic `@mipmap/ic_launcher` icon.
- **OTP code field pre-fill:** after moving from phone-entry to the OTP screen, the "کد تایید" field appeared pre-populated with the phone number string. The tester's text-input method was non-standard, so this is logged as **an observation to reproduce cleanly**, not a confirmed defect.
- The guarded-route sign-in states (Search, Calendar, Favorites) are **clean and explicit** — an improvement over the Aug Customer Journey Audit's "silent bounce" finding.
- Real account name now shown in the header (Journey Audit P0-4 "hardcoded name" — **fixed on device**).

---

## Exact next blockers for Customer release

| P | Blocker | Status from this test |
|---|---|---|
| **P0** | **App identity** — ship name is "ROJAN_DesignLab"; generic launcher icon; `applicationId = ai.rojan.designlab` (permanent after publish, decision unmade). | Confirmed on device |
| **P0** | **No release signing keystore** — this was a debug-signed build. A signed AAB cannot be produced yet. | `keystore.properties` / `.jks` absent (unchanged) |
| **P0** | **No production backend URL wired for release builds** — only `dev` works (via git-ignored `local.properties`). `STAGING_API_BASE_URL` / `PRODUCTION_API_BASE_URL` unset; `ProductionRelease` is Gradle-gated and un-buildable. | Confirmed |
| **P0** | **Guest / first-run experience (N-2)** — the app cannot show real content without login, yet presents a placeholder guest Home. Decide: public discovery on the backend, or force-login first-run. | Confirmed on device |
| **P0** | **No crash reporting** (no Crashlytics/Sentry, no `Application` class). A public launch would be blind to field crashes. | From code audit; unchanged |
| **P1** | **Booking creation not yet validated E2E on device** — needs one controlled test appointment against the pilot salon (`POST` create → confirm → appears in "نوبت‌های من"). | Deliberately not done here |
| **P1** | **Home placeholder sections** ("خدمات محبوب", "فعالیت اخیر") — wire to real data or remove for MVP. | Confirmed on device |
| **P1** | **OTP code-field pre-fill** — reproduce cleanly and fix if confirmed. | Observed |
| **P2** | Redundant `/users/me` calls (N-3); first-frame jank (~1 s). | Observed |
| **P2** | `versionCode` still `1` / `versionName` `1.0.0` — no bump strategy; Play listing, privacy policy, data-safety form absent. | From audit |
| **P2** | `origin/main` is **101 commits behind** this branch — release-train decision required. | From `git` |

---

## Appendix — endpoints exercised on-device (all real, live backend)

```
POST /api/v1/auth/otp/request                                  200   (guest → OTP dispatched, real SMS)
POST /api/v1/auth/otp/verify                                   200   (OTP 8251 → tokens issued)
GET  /api/v1/users/me                                          200
GET  /api/v1/users/me/salon-access                             200   (was "not live" per Aug reports — now 200)
GET  /api/v1/bookings/mine?page=0&size=50                 401→200   (401 guest, 200 after auth)
GET  /api/v1/salons?page=0&size=20&sortDirection=ASC      401→200
GET  /api/v1/salons/{id}                                       200
GET  /api/v1/salons/{id}/categories                            200
GET  /api/v1/salons/{id}/categories/{catId}/services           200
GET  /api/v1/salons/{id}/specialists                           200
GET  /api/v1/salons/{id}/working-hours                         200
GET  /api/v1/customer/followed-salons?page=0&size=100          200
GET  /api/v1/customer/favorite-salons?page=0&size=100          200
```

Legend: **PASS** = exercised and worked on the physical device · **FAIL** = reproduced a defect · **BLOCKED** = could not complete due to missing data/access · **NOT TESTED** = deliberately not attempted.
