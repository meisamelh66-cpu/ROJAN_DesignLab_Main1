# ROJAN Customer Android — RC-3 Final Release Candidate Audit

**Date:** 2026-09-11
**Scope:** Customer app only. Final verification pass before Play Store submission — synthesizes and re-verifies the 8 prior audits/fixes performed earlier in this session (RTL, Beauty DNA, Booking back-nav, performance, RC-1 security, RC-2 artifact, RC-2.1 size cleanup) plus fresh checks specific to this pass.
**Method:** No device or emulator is available in this environment (re-checked: `adb devices -l` returns empty). Every UI-flow item below was therefore verified by **tracing the actual implementation code** (navigation graph, ViewModels, screen composables) rather than live interaction, cross-referenced against this session's own real A72 device-verification history where that history exists. Release-stability items (compile/lint/assemble/tests) were run for real, this session, against the current code. Nothing was modified, refactored, optimized, or committed.

---

## Executive Summary

**The Customer Android app's own code is in release-quality shape.** Across 8 prior audits this session it has been RTL-audited, performance-audited-and-fixed (splash/session-restore overlap, tab-navigation state preservation), security-audited-and-fixed (backup exclusion), and artifact-audited-and-cleaned (APK/AAB size). This pass re-verified every one of those fixes is still in place (none reverted), re-ran the release build/lint/test suite fresh, and traced every flow in this task's checklist through the actual source.

**However, this app is a booking app, and a real booking cannot currently be completed end-to-end against the live production backend** — `POST /api/v1/bookings` returns `409` for the pilot salon (a known, previously-documented backend defect, not an Android code issue). That is unambiguously release-blocking for the app's core purpose, regardless of how clean the Android code itself is. A second, independent blocker — Play Store's privacy policy / Data Safety declaration — is also still incomplete. Both are **outside Android code scope** (backend + Play Console respectively), which is why neither shows up as a code-level finding anywhere in this report, but both must be resolved before submission.

---

## Release Status: **BLOCKED**

Not because of an Android code defect — none was found in this pass. Blocked because of two cross-cutting, non-Android-code items:
1. **Booking cannot complete on the live backend (409)** — a core-journey blocker.
2. **Privacy policy / Play Console Data Safety form incomplete** — a hard store-submission requirement.

See "Remaining actions before Play Store upload" for the precise, minimal list.

---

## 1. Fresh Guest Journey — **PASS** (code-traced)

| Check | Result | Evidence |
|---|---|---|
| Fresh install launch | Verified via code: `MainActivity.onCreate()` → `RojanNavGraph()`, no eager work beyond composition | `MainActivity.kt` |
| Splash behavior | **PASS** — confirmed the performance fix is still in place: `SplashScreen(ready: Boolean, ...)`, no internal `delay(2600L)` remains; splash exits on real readiness, not a fixed timer | `screens/splash/SplashScreen.kt:70-71` (re-checked this session) |
| Explore opens without login | `startDestination = ... ?: if (personId != null) CUSTOMER_HOME else EXPLORE` — a guest (`personId == null`) lands on `EXPLORE` directly | `navigation/RojanNavGraph.kt:300-307` (re-checked this session) |
| Public salon list loads (guest) | `SalonListViewModel`'s `hasSession`/`publicSalonRepository` guest-fallback logic confirmed intact — this exact path was previously A72 device-verified (`GUEST-EXPLORE-FIX-REPORT.md`, this session) | `presentation/salon/SalonListViewModel.kt:56-146` (re-checked this session) |
| Search works | Same `SalonListViewModel`-backed guest/authed branching; `SearchScreen` shares the same repository pattern | code trace |
| Salon details open | `SALON_DETAILS` route has no `CustomerAccessGuard` — reachable by a guest directly from Explore/Search | `navigation/RojanNavGraph.kt` |
| Login gate appears only where required | `CustomerAccessGuard` wraps exactly 6 routes (Appointments, Favorites, Waitlist, Reschedule, AppointmentDetails, and one more protected route) — none of them plain browsing; `onLoginRequired` is an on-demand callback (follow/favorite tap, booking action), never a blanket block | `navigation/RojanNavGraph.kt:1087-1212` (re-checked this session) |

---

## 2. Authenticated Customer Journey — **PASS** (code-traced + prior device history)

| Check | Result | Evidence |
|---|---|---|
| OTP login flow | Implementation unchanged this session; previously real-OTP device-verified in earlier phases (memory: `ROJAN_Real_OTP_Login_Test_Report_v1` lineage, and this session's own A72 runs) | `screens/auth/AuthScreen.kt`, `presentation/auth/AuthViewModel.kt` |
| Session restore after app restart | **PASS** — this is the exact mechanism the performance fix targeted: `restoreSession(personId)` now starts the moment local restore resolves, validated against the real backend (`GET /auth/me`, transparent token refresh) before any Customer UI renders | `AuthViewModel.kt:328-354`, `navigation/RojanNavGraph.kt:195-247` (re-checked this session) |
| Home dashboard / Explore / Profile / Appointments / Favorites | All five wrapped in `CustomerMainScaffold` with the tab-nav state-preservation fix (`saveState`/`restoreState`) confirmed still present | `navigation/RojanNavGraph.kt` (re-checked this session — `saveState = true` / `this.restoreState = true` present) |
| Logout | `authViewModel.logout()` (confirmed in the RC-1 security audit to fully clear tokens, personId, and in-memory state) then navigates to `EXPLORE` with `popUpTo(startDestination){inclusive=true}` | `navigation/RojanNavGraph.kt:1068-1074` (re-checked this session) |
| After logout, app returns to guest state correctly | The `popUpTo(...){inclusive=true}` clears the *entire* back stack including the start destination, landing fresh on `EXPLORE` — the correct guest entry point, not a stale authenticated screen | same, code trace |

---

## 3. Profile Personalization — **PASS (Android side) / ENVIRONMENT BLOCKER (backend)**

| Check | Result | Evidence |
|---|---|---|
| Profile header rendering | Cover band + 96dp avatar + verified-chip layout confirmed unchanged since the RTL audit fixes | `screens/profile/ProfileScreen.kt` |
| Avatar/cover picker opens | Wired to the Android 13+ Photo Picker (`ImageOnlyPickerRequest`, no runtime permission) — unchanged, previously A72-verified in Phase 5B | `ui/media/ImageDownscale.kt` |
| Upload request handling | `decodeResizeAndCompress` → `ProfileMediaViewModel` → `UserProfileRepository` → `POST /users/me/media/{avatar,cover}` — code path unchanged | `presentation/profile/ProfileMediaViewModel.kt` (re-checked this session) |
| Error handling when backend unavailable | Confirmed intact: `.onFailure { error -> ... errorMessage = userMessageFor(error) }` — a real HTTP failure becomes an inline Persian error message, never a crash; previously A72-verified producing a clean 404 message (not a crash) when Phase 5A wasn't deployed | `ProfileMediaViewModel.kt:77-78` (re-checked this session) |
| Guest user has no edit controls | `editable = !isGuest`, confirmed gating every avatar/cover tap handler | `ProfileScreen.kt:206,362,384,411,424` (re-checked this session) |

**Environment blocker (report only, not modified):** the backend's Phase 5A (user avatar/cover media endpoints) is confirmed, from this session's own backend audits, **not currently deployable as-is against the real production database** — its schema was built against an assumption that a separate, already-shipped production-lineage migration has since superseded (see `PHASE-5A-PRODUCTION-DEPLOYMENT-AUDIT.md` / `PRODUCTION-BACKEND-STATE-AUDIT.md`, both this session). This means a live upload attempt will currently 404, exactly as already observed and handled gracefully on-device. **No Android code change is implicated or warranted** — the Android side does exactly the right thing when this endpoint isn't there.

---

## 4. Beauty DNA — **PASS**

| Check | Result | Evidence |
|---|---|---|
| DNA title | `CustomerScaffold(title = "بیوتی DNA", ...)` — confirmed the Latin "DNA" naming is in place, not the old transliteration | `screens/profile/BeautyDnaScreen.kt:101` (re-checked this session) |
| Accordion behavior | `expandedSection: DnaSection?` — single shared state, confirmed only one section can be expanded (مو/پوست/ناخن), each toggle correctly closes the others | `BeautyDnaScreen.kt:99,123-176` (re-checked this session) |
| Selection persistence | Unchanged `viewModel.updateHair`/`updateSkin`/`updateNails` calls — same payloads as before the redesign | code trace |
| RTL layout | Header follows the audited list-row convention (chevron-first/`Alignment.End`-anchored); this exact screen was covered in the RTL compliance audit and found compliant | `BeautyDnaScreen.kt` (RTL audit, this session) |
| No old Persian "DNA" naming remains in visible UI | Confirmed via fresh grep this session: zero hits for "دی‌ان‌ای"/"دی ان ای" anywhere in `ProfileScreen.kt` or `BeautyDnaScreen.kt`'s actual UI strings (the one remaining hit anywhere in the app is a doc-comment describing the *old* name, and a default-parameter string in a confirmed-dead, zero-call-site component) | grep, this session |

---

## 5. Booking Full Journey — **PASS (Android UI) / BLOCKED (backend, core-journey)**

| Check | Result | Evidence |
|---|---|---|
| Home → Salon → Service → Specialist → Date → Time → Confirmation (navigation/UI) | Route chain confirmed intact, all screens correctly bar-less (not wrapped in `CustomerMainScaffold` — only the 5 tab destinations are) | `navigation/RojanNavGraph.kt` |
| Back navigation | Confirmed correct — the root cause fixed earlier this session (duplicate-push from unguarded double-taps) is still fixed: 30 `launchSingleTop` occurrences remain across the file | grep, this session (re-verified count consistent with the booking back-nav fix) |
| Duplicate navigation prevention | Same fix — `launchSingleTop = true` present on every forward call in the chain | code trace, this session |
| Time slot selection | Day-part-grouped `LazyVerticalGrid` (`DayPart.MORNING/AFTERNOON/EVENING`) confirmed still in place, 4-column grid, `RefSelectableCell`'s 48dp touch target unchanged | `screens/bookingflow/BookingTimeScreen.kt` (re-checked this session) |
| Loading/error states | `UiState.Loading/Error/Empty/Success` branches unchanged across `BookingDateScreen`/`BookingTimeScreen`/`BookingConfirmationScreen` | code trace |
| No crash | No new code was introduced this pass; nothing in the fixes applied earlier this session touches exception handling in the booking chain | — |
| **Reaching Success on the real backend** | **BLOCKED** — `POST /api/v1/bookings` returns `409` for the ROJAN AI Pilot Salon on the live backend, a previously-documented, still-open backend defect (`pilot-salon-booking-409` in project memory, tracked as P0-5 in the release-readiness audit). The Android confirmation screen correctly submits the request and correctly surfaces the backend's real error — this is not an Android bug, but it does mean **no customer can currently complete a real booking on production**, which is release-blocking for the app's core function regardless of code quality. |

---

## 6. Navigation & UI Consistency — **PASS**

| Check | Result | Evidence |
|---|---|---|
| Bottom navigation persistence | Confirmed: `saveState = true` on `popUpTo`, `this.restoreState = true` on `navigate` (the explicit-`this.` qualifier from the shadowing bug fixed earlier this session is still correctly in place) | `navigation/RojanNavGraph.kt` (re-checked this session) |
| RTL correctness | Full-app RTL compliance audit performed and fixed earlier this session (text alignment, icon mirroring, row/card ordering across booking, profile, home, explore); re-confirmed no legacy left-aligned/hardcoded-direction code was reintroduced by any later fix this session | RTL audit, cross-checked against later diffs |
| No legacy theme components visible | Fresh grep this session across the whole Customer scope for `HomeGlassSurface`/`GlassBackButton`/`PremiumButton`/`RtlListRow`/`RtlSectionHeader` — every hit is either (a) a doc-comment describing a *replaced* pattern, (b) the component's own definition file (not a usage), or (c) inside code already confirmed dead (zero call sites) in an earlier phase this session. No live legacy-styled screen found. | grep, this session |
| No broken back buttons | Covered by the booking back-nav fix (§5) plus `CustomerScaffold`'s single, shared back-button implementation used everywhere — no per-screen divergent back handling found anywhere in Customer scope | code trace |

---

## 7. Release Stability

```
:app:compileCustomerProductionReleaseKotlin   → BUILD SUCCESSFUL
:app:lintCustomerProductionRelease            → BUILD SUCCESSFUL, 98 warnings, 0 errors
                                                 (this is the production-release variant's own lint
                                                 run, not directly comparable to the 94-warning
                                                 customerDevDebug baseline tracked elsewhere this
                                                 session — different variant, some lint checks behave
                                                 differently under release/minified config. No error
                                                 was raised, and `abortOnError = true` is configured,
                                                 so a genuine error would have failed this build. Full
                                                 triage of the individual warning categories is a
                                                 non-blocking improvement, out of this audit's
                                                 explicit focus per its own instructions.)
:app:assembleCustomerProductionRelease        → BUILD SUCCESSFUL (all tasks UP-TO-DATE — confirms
                                                 the RC-2.1-cleaned artifact is still current and
                                                 valid, nothing regressed it)
```

**Full Customer unit test suite** (`:app:testCustomerDevDebugUnitTest`, run fresh this session): **307/309 passed.** The 2 failures are the same pre-existing `BackendAuthFlowVerificationTest` cases that require a live `localhost:8080` backend and have failed identically in every single phase of this session — confirmed environmental, not a regression from anything done in this pass.

**Crash logs:** **Environment blocker** — no device or emulator is available in this session (`adb devices -l` empty, re-checked at the start of this task); no live crash-log capture was possible. No crash was *predicted* by any code path traced in this audit.

**Release artifact integrity:** re-verified this session — `app-customer-production-release.apk` (2,526,759 bytes, matching the RC-2.1 post-cleanup size exactly) signature-verifies successfully (`apksigner verify` → `Verifies`).

---

## Passed items (summary)
Guest journey (splash, Explore, public browse, search, salon details, login-gate placement); authenticated journey (OTP, session restore, all 5 tabs, logout, post-logout guest state); Profile personalization Android-side behavior (picker, upload wiring, error handling, guest-gating); Beauty DNA (title, accordion, RTL, naming); Booking flow UI/navigation (back-nav, duplicate-prevention, slot grouping, states); bottom-nav state preservation; RTL correctness app-wide; no legacy components; compile/lint/assemble for the production release variant; 307/309 unit tests.

## Failed items
None found in Android code this pass.

## Environment blockers
1. **Backend booking 409** (`pilot-salon-booking-409`) — blocks real end-to-end booking completion. Backend-side, not Android.
2. **Backend Phase 5A media endpoints** not deployable as-is — blocks live avatar/cover upload only (gracefully handled, not a crash). Backend-side, not Android.
3. **No device/emulator available** — blocks live crash-log capture and any on-device interaction verification for this pass. Every UI-flow finding above is code-traced, not click-tested.
4. **Play Console privacy policy / Data Safety form** incomplete — store-listing side, not Android code.

## Remaining actions before Play Store upload
1. Resolve the backend booking-409 defect (backend team/task — outside this app's codebase).
2. Complete the Play Console privacy policy URL entry + Data Safety form (store-listing task).
3. Either deploy a corrected Phase 5A backend, or accept avatar/cover upload as a known-degraded feature for the first release (it already fails safely — a product decision, not a code defect).
4. Once a device becomes available, perform the live click-through pass this report's method could not: physically walk all 7 checklist sections on-device before final sign-off, per this project's established "device-verify before calling anything done" convention.

**Nothing was modified, refactored, optimized, or committed in the course of this audit.**
