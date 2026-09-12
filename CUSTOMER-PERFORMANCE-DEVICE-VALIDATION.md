# ROJAN Customer Android — Performance Fix Device Validation

**Date:** 2026-09-10
**Scope:** Customer app only. Device validation of the two fixes in `CUSTOMER-PERFORMANCE-FIX-REPORT.md`.
**Status:** 🛑 **STOPPED — no device or emulator available in this environment.** Per the task's own instruction ("Device test only if device available"), no on-device testing was attempted, none was fabricated, and no code was changed. This document is (1) the exact access check performed, and (2) the complete, ready-to-execute validation procedure for the moment a device is available — precise enough to run directly, with pass/fail criteria matching the task's checklist item-for-item.

---

## 1. Device availability check

```
$ adb devices -l
List of devices attached
(empty)

$ adb kill-server && adb start-server && adb devices -l
* daemon not running; starting now at tcp:5037
* daemon started successfully
List of devices attached
(empty)

$ adb get-state
error: no devices/emulators found

$ emulator -list-avds
(empty)

$ ls ~/.android/avd/*.ini
(no files)
```

- `adb` is present (`%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, not on `PATH` but resolvable directly) and the daemon starts cleanly — this isn't a tooling failure, there is simply no device or emulator attached to this machine right now.
- No physical device is connected (USB or wireless).
- No Android Virtual Device is configured at all — starting an emulator isn't a fallback option without first creating one, which is itself a real environment-setup step, not something to improvise mid-task.

**Nothing below this line was executed.** Every "PASS/FAIL" criterion is a specification of what to check, not a result.

## 2. What's ready the moment a device connects

- **A `customerDevDebug` APK already exists and reflects both fixes**, built during the prior implementation task's `assembleCustomerDevDebug` run (after both `SplashScreen.kt` and `RojanNavGraph.kt` were edited, not before): `app/build/outputs/apk/customerDev/debug/app-customer-dev-debug.apk`. Sanity-re-checked in this session (read-only) that the fixes are still present in source and weren't accidentally reverted since: `SplashScreen(ready: Boolean, ...)` and `RojanNavGraph.kt`'s `readyToProceed`/`saveState = true`/`this.restoreState = true` are all still in place.
- **`applicationId` for this build variant is `ai.rojan.designlab`** (the `customer` flavor has zero applicationId overrides — it inherits `defaultConfig` verbatim) — no `.debug`/`.dev` suffix.
- **`dev` flavor's `API_BASE_URL` is read from `local.properties`'s `DEV_API_BASE_URL`**, already configured locally — confirmed present (key exists), not re-printed here (may be a local-network address). The device used must be able to reach that address (same Wi-Fi/LAN as this machine, or the value must be a publicly-reachable host) — first blocker to check if a device connects but the app can't log in/restore a session.
- Install command, once a device is attached: `adb install -r app/build/outputs/apk/customerDev/debug/app-customer-dev-debug.apk`.

## 3. Full validation procedure — ready to execute

Every scenario below maps 1:1 to the task's own checklist. Logcat filters assume the app's existing debug-only `HttpLoggingInterceptor` (BASIC level, method+URL+response code only, no bodies/headers — `BackendApiContainer.kt`'s existing, unmodified logging setup) is the source of truth for "was a network call made," so **no code needs to change to observe duplicate requests** — this is exactly the pre-existing, standard debug-build behavior.

```bash
ADB=<path to adb>
PKG=ai.rojan.designlab
APK=app/build/outputs/apk/customerDev/debug/app-customer-dev-debug.apk

# Reusable during every scenario below:
$ADB logcat -c                                   # clear log buffer before each scenario
$ADB logcat -v time | grep -i "OkHttp\|okhttp"    # in a separate terminal, watch every request live
```

### 3.1 Cold launch — guest

```bash
$ADB shell pm clear $PKG        # wipes DataStore/tokens - guarantees a real guest cold start
$ADB logcat -c
$ADB shell am start -W -n $PKG/.MainActivity      # -W prints TotalTime (ms) to first frame drawn
$ADB shell screenrecord --time-limit 5 /sdcard/cold_launch_guest.mp4
$ADB pull /sdcard/cold_launch_guest.mp4
```
**Check:**
- [ ] `am start -W`'s reported `TotalTime` / `WaitTime` — should be materially lower than the old fixed ~3000ms floor for a guest (the fix removed the unconditional `delay(2600L)`; expect low hundreds of ms, network/device-dependent, not a fixed number to assert exactly).
- [ ] Play back `cold_launch_guest.mp4` frame-by-frame (or `adb shell screencap` a few times a few hundred ms apart) — confirm no blank/black frame between the splash and the Explore screen, and no visible "flash" of one screen before the other (the fix collapsed the old two-screen Splash→`RestoringSessionContent` swap into one transition — this is exactly the behavior to visually confirm).
- [ ] Logcat: zero `POST /api/v1/auth/*` or `GET /api/v1/auth/me` calls (a true guest has no `personId` to validate — `sessionValidationDone` should flip `true` with no network call at all).
- [ ] Lands on `EXPLORE` (guest start destination), not stuck on a loading screen.

### 3.2 Logged-in customer — kill and reopen

```bash
# First, actually log in through the app UI once (OTP flow) so a real session exists.
$ADB logcat -c
$ADB shell am force-stop $PKG
$ADB shell am start -W -n $PKG/.MainActivity
$ADB shell screenrecord --time-limit 6 /sdcard/cold_launch_returning.mp4
$ADB pull /sdcard/cold_launch_returning.mp4
```
**Check:**
- [ ] No AUTH/OTP screen shown — session restores silently, lands directly on `CUSTOMER_HOME` (the Dashboard).
- [ ] Logcat shows, in order: (optional) a token-refresh call, then exactly one `GET /api/v1/auth/me`, then exactly one `GET /api/v1/users/me/salon-access` — **not duplicated**, and this time overlapping with the splash's visible window rather than starting only after it (correlate the logcat timestamps against the screen recording's splash-to-real-screen transition point — the network calls should be *in progress during* the splash, not starting only once it's gone).
- [ ] Splash duration this run should visibly track real network latency (longer than the guest run in 3.1, proportional to however long those 1–3 calls actually took) — not a fixed value either way.
- [ ] No duplicate login screen at any point.
- [ ] Dashboard content (salon list, booking history) loads — confirms `CustomerDashboardScreen`'s own ViewModels aren't broken by the restructure.

### 3.3 Bottom navigation — repeated switching

For each pair below, tap the sequence, and after **each individual tap**, capture `logcat` output and a screenshot:
```bash
$ADB logcat -c
# tap Home -> Explore -> Home (via UI), then:
$ADB logcat -d | grep -i "okhttp\|GET /api/v1/salons\|GET /api/v1/public/salons"
```
Repeat for all four required pairs:
- Home → Explore → Home
- Home → Profile → Home
- Home → Appointments → Home
- Home → Favorites → Home

**Check, per pair:**
- [ ] **Scroll position preserved**: scroll down on the first tab before switching away; on return, the scroll offset should be exactly where it was left (this is the visible, human-verifiable proxy for "the ViewModel/back-stack entry — and Compose's own scroll state tied to it — were restored, not recreated").
- [ ] **No repeated loading spinner** on return to a previously-visited tab — content should already be there instantly (restored state), not a fresh skeleton/spinner.
- [ ] **No duplicate API request** in the logcat window between the *first* visit to a tab and every *subsequent* return to it in the same app session — e.g. `GET /api/v1/salons` (Home's `SalonListViewModel`) and the booking-history call should each appear **once**, not once per tab switch. (First-ever visit to a tab is expected to make its normal calls — the fix targets *repeated* calls on return, not the initial load.)
- [ ] Bottom bar's active-tab highlight updates correctly and instantly on every switch, no lag.

### 3.4 Network interruption mid-tab-switch

```bash
$ADB shell svc wifi disable && $ADB shell svc data disable   # or physically disable data on-device
# in the app: tap a tab that triggers a fresh network load (e.g. Appointments, if not yet visited this session)
# immediately switch away to another tab, then back
$ADB shell svc wifi enable && $ADB shell svc data enable
# switch back to the interrupted tab again
```
**Check:**
- [ ] Switching away from a tab mid-load does not crash the app (check `adb logcat | grep -i "FATAL\|AndroidRuntime"` throughout).
- [ ] Returning to the interrupted tab either shows its existing error/retry state (`UiState.Error`) or a loading state, never a permanently blank screen.
- [ ] Once connectivity returns, a manual retry (or the screen's own retry action) succeeds and populates real data — recovers correctly, no need to force-kill the app.

### 3.5 Booking flow regression

Walk: Home → a salon → a service → specialist selection (or a specific specialist) → date → time → confirmation.

**Check, at every one of those screens:**
- [ ] **No bottom tab bar visible** — `CustomerMainScaffold`/`CustomerBottomBar` should not appear on any booking-flow or detail screen (this fix never touched these routes or wrapped them in `CustomerMainScaffold` — regression would indicate something unrelated broke, worth flagging loudly if seen).
- [ ] Each screen's own back arrow / system back returns exactly one step (this was the subject of an *earlier*, separate fix this session — `launchSingleTop` on the booking chain's `navigate()` calls — not touched by today's two fixes, but worth reconfirming nothing regressed).
- [ ] Selected salon/service/specialist/date/time carry through correctly to Confirmation (the `BookingViewModel` state — unrelated to and unaffected by either of today's fixes, but the natural end-to-end check).
- [ ] Backing all the way out of the booking flow returns to Home/Explore with the bottom bar reappearing correctly.

## 4. Summary

| Scenario | Result |
|---|---|
| 1. Cold launch guest | **NOT RUN — no device** |
| 2. Logged-in customer restore | **NOT RUN — no device** |
| 3. Bottom navigation state preservation | **NOT RUN — no device** |
| 4. Network interruption recovery | **NOT RUN — no device** |
| 5. Booking flow regression | **NOT RUN — no device** |

**No pass/fail claim is made for any of the five scenarios.** The implementation itself was re-confirmed present and unmodified in source (read-only check, §2), and was already statically verified (compile/lint/assemble/unit tests) in the prior `CUSTOMER-PERFORMANCE-FIX-REPORT.md` — but per that report's own "Known risks" section, static verification is explicitly not a substitute for the device pass above, and this document does not claim otherwise.

**To unblock:** connect a physical Android device with USB debugging enabled (the project's established device is a Samsung A72, per prior session history) and re-run this task, or set one up via `adb devices` becoming non-empty — the procedure in §3 is complete and requires no further preparation once that's true.

**Nothing was modified, committed, or deployed in the course of this task.**
