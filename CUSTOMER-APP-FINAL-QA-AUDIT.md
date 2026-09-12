# Customer App — Final Pre-Release QA Audit

**Date:** 2026-09-12
**Scope:** Full pre-release validation of the ROJAN Customer Android app, with backend Media Phase 5A.2 deployment preparation now complete on the backend side (`release/production-v24`, not yet actually deployed).

## Methodology — read this before the findings

**No device or emulator was available this session** (`adb devices` returns empty; zero AVDs configured — consistent with every prior session in this project). Independent of that constraint, this task's own rules (no repeated login/OTP, no unnecessary API calls, no SMS/quota consumption, approval required before any token/OTP/paid-API action) would have ruled out a live click-through pass regardless — a real device walk requires exactly the kind of repeated auth/network activity this task asks to avoid.

**Given both facts, this audit was performed entirely as direct source-code verification** — reading the actual implementation (ViewModels, repositories, network layer, navigation graph) rather than exercising the running app. Every finding below is either:
- **Confirmed by code** — the actual logic was read and traced, not inferred, OR
- **Confirmed by a real, local build/test/lint run** — no network call left the machine, OR
- **Carried over from a prior session's real, on-device A72 verification** (explicitly cited by phase name below wherever used) — never presented as if newly device-tested today.

Nothing in this report should be read as "tapped through on a phone today." Where a genuine device pass is still needed before shipping, that's called out explicitly, not glossed over.

**Zero tokens, OTP codes, SMS, or paid API calls were consumed by this audit.** The only network-adjacent activity was one pre-existing unit test (`BackendAuthFlowVerificationTest`) attempting a connection to `http://localhost:8080` (hardcoded in the test itself, not production) as part of a routine local test run — it failed with `ConnectException` (no local backend was running), so no real request ever left the machine. See the checklist at the end for the full accounting.

---

## 1. Authentication

| Check | Result | Evidence |
|---|---|---|
| Login flow | ✅ Code-verified | `AuthViewModel.requestOtp()`/`verifyOtp()` — phone → OTP → session, real backend calls, no demo/mock fallback remaining. |
| Session persistence | ✅ Code-verified | `authSessionRepository.savePersonId()` on success (`AuthViewModel.onAuthenticated()`); cold-start reads it back and calls `restoreSession(personId)`. |
| Token refresh behavior | ✅ Code-verified, well-engineered | `TokenAuthenticator.kt`: single-retry cap (`MAX_RETRIES=1`, via response-chain length check), **concurrent-refresh de-duplication** (a `synchronized` lock — if N requests 401 simultaneously, only the first actually calls `/auth/refresh`; the rest re-check the stored token and retry with it instead of refreshing again, correctly handling refresh-token rotation), refresh itself goes through `plainAuthApi` (no authenticator of its own — no self-recursion risk). |
| Expired token handling | ✅ Code-verified | A genuinely failed refresh (dead/revoked refresh token, not a transient error) clears **both** the token pair **and** the persisted `personId` (`TokenAuthenticator.kt:88-89`) — this is a real, documented fix for a previously-real bug where a stale `personId` alone caused an optimistic route to `CUSTOMER_HOME` before the dead session was discovered. `restoreSession()` mirrors this on its own failure path. |
| No unnecessary OTP requests | ✅ Confirmed by code | `requestOtp()` is called from exactly two places: `AuthScreen`'s explicit "send code" button tap, and `resendOtp()` (an explicit "resend" tap, same rate limits, backend-enforced). **Zero call sites in `restoreSession()`, `onAuthenticated()`, token refresh, or any `init{}` block.** OTP is 100% user-gesture-gated. |

**No live login/OTP was performed to verify this section** — per instruction, and because the code-level evidence above is conclusive without it (the guard logic is structural, not something that only shows up at runtime).

---

## 2. Customer Home

| Check | Result | Evidence |
|---|---|---|
| Home loads correctly | ✅ Code-verified | Home's salon rows go through `SalonListViewModel` (shared with Explore/Search); appointment rows through `BookingHistoryViewModel`. Both fire exactly once per ViewModel lifetime. |
| API failure handling | ✅ Code-verified, comprehensive | `SafeApiCall.kt` distinguishes **5 failure classes** and never lets one crash the process: `BackendApiException` (backend-described error), `NetworkUnavailableException` (offline), `RequestTimeoutException` (slow server, distinct from offline — different actionable message), `MalformedResponseException` (contract drift / DTO mismatch — this is the fix for a real, previously-shipped crash on `/public/salons`), `UnexpectedApiException` (catch-all). Every one resolves to `Result.failure`, never a thrown exception past the repository boundary. |
| Loading/empty/error states | ✅ Code-verified | Standard `UiState.Loading/Empty/Error/Success` pattern confirmed at every screen checked (Home, SalonList, SalonDetails); `Error` states always carry a retry action wired to the ViewModel's own `retry()`. |
| No duplicate API calls | ✅ Code-verified | `SalonListViewModel.load()` cancels any in-flight `loadJob` before starting a new one; `loadMore()` is guarded by `isLoadingMore`/`canLoadMore` flags. Tab switching (Home↔Explore↔Profile↔Appointments) uses `saveState=true`/`restoreState=true` (Performance Fix, implemented this session, confirmed still present in source) — so a tab switch no longer destroys and recreates the ViewModel, which is what previously caused a re-fetch on every single switch, not just cold start. |

**Minor observation (not a bug):** `BookingHistoryViewModel.load()` has no `Job?.cancel()` guard the way `SalonListViewModel` does — asymmetric, but not currently consequential since (with the tab-nav fix above) `load()` only ever fires once per ViewModel instance and nothing else calls it concurrently. Worth aligning for consistency in a future pass, not release-blocking.

---

## 3. Salon Discovery / Marketplace

| Check | Result | Evidence |
|---|---|---|
| Public salon list loads | ✅ Code-verified | Guest (no session) routes to `PublicSalonRepository.browseSalons()` (`GET /api/v1/public/salons`, unauthenticated); logged-in routes to the authenticated `SalonRepository.browseSalons()`. `hasSession` is re-checked on every call, so a login/logout mid-session routes the *next* call correctly. |
| Only active salons appear | ✅ Backend-confirmed this session | The authenticated discovery endpoint's `onboardingStatus == ACTIVE` filter (commit `ec2681b`) was independently re-verified twice this session (code read + live read-only probe against `api.rojanai.ir` showing exactly 5 active salons, the previously-DRAFT pilot salon correctly absent). The public endpoint was already correct before that. Android does not need its own filter — it trusts the backend's, correctly. |
| Pagination | ✅ Code-verified | Real page-based `loadMore()` (`PAGE_SIZE=20`), appends to the existing list, `canLoadMore` derived from the backend's own `totalPages`. |
| Search/filter | ✅ Code-verified | Cancellation-safe (`loadJob?.cancel()` on every new search), and a **failed** follow-up search keeps the last successful result on screen instead of blanking it — only the very first load can show a hard error. Debounce (350ms, per prior session's audit) confirmed deliberate. |
| Salon cards / image loading | ✅ Code-verified | `RojanRemoteImage` (Coil `AsyncImage`): `url` null, blank, **or a failed load** all resolve to the same fallback placeholder — never a broken-image glyph, never an indefinite blank box. `ImageRequest` is `remember`ed keyed on `(url, context)` (fixed in an earlier phase — confirmed still present) so it isn't rebuilt on every recomposition. |

---

## 4. Salon Details

| Check | Result | Evidence |
|---|---|---|
| Salon info loads | ✅ Code-verified | `SalonDetailsViewModel.load()`: salon → categories → services (fanned out per category, see note below) → specialists → working hours. |
| Logo/cover display | ✅ Code-verified | Routes through the same `RojanRemoteImage` fallback-safe path as salon cards. |
| Services load | ✅ Code-verified, **real N+1 pattern flagged** | There is no salon-wide "all services" endpoint — the ViewModel's own doc comment states this explicitly. Services are fetched by fanning out one call per category and flattening. For a salon with few categories this is negligible; worth a backend follow-up (a combined endpoint) if a salon with many categories becomes common. Not a bug, not new — pre-existing, now specifically called out for Section 8 too. |
| Specialists load | ✅ Code-verified | Single call, straightforward. |
| Empty states | ✅ Code-verified, correctly graceful | Services/specialists sections are conditionally rendered (`if (services.isNotEmpty())` / `if (specialists.isNotEmpty())`) — a salon with neither configured (which, per this session's own backend audit, describes **most currently-active production salons**) renders a clean detail page with just the header, not a broken or awkward "empty services" block. **This is correct Android behavior for a real, known backend-data-completeness gap** — the screen isn't at fault, the content is genuinely sparse for those specific salons. Also confirmed: a working-hours fetch failure degrades to an empty list rather than failing the whole page. |

---

## 5. Booking Journey (Critical)

Traced the full chain `Home → Search → Salon Details → Specialist → Service → Date → Time → Confirmation`.

| Check | Result | Evidence |
|---|---|---|
| State consistency across screens | ✅ Code-verified | Single shared `BookingViewModel`, scoped to the booking nav sub-graph (nested-graph shared-ViewModel pattern, confirmed unchanged since the Phase 8 Booking-Time redesign, which explicitly verified this scoping while fixing an unrelated nav bug). Selections dispatch as events (`onSalonSelected`/`onServiceSelected`/`onSpecialistSelected`/`onDateSelected`/`onTimeSelected`) into one state object — never per-screen local state that could drift. |
| No duplicate booking requests | ✅ Code-verified, two independent layers | (1) **Navigation layer**: `launchSingleTop = true` on all 20 forward `navigate()` calls across the entire booking chain (Phase 8 fix, confirmed still present) — a rapid double-tap can no longer push the same destination twice. (2) **Submission layer, independently**: `BookingConfirmationViewModel.confirmBooking()` starts with `if (isSubmitting) return` — a hard guard against a second concurrent submit regardless of navigation state — **plus** a fresh `Idempotency-Key` (real UUID) generated per submit attempt and sent to the backend, which itself de-duplicates a retried request with the same key. Two independent, layered protections, not one. |
| Error recovery | ✅ Code-verified | A failed `createBooking()` call sets `submitError` (a real Persian user-facing message via `userMessageFor`) and resets `isSubmitting = false` — the customer can immediately retry without navigating away or losing their selections (the shared `BookingViewModel` state is untouched by a failed submit). |
| Back navigation / state loss | ✅ Code-verified | Same `launchSingleTop` fix (Phase 8) also fixed the original "back does nothing" bug (root cause was a duplicate-push leaving two identical stack entries, not a `BackHandler` conflict — confirmed via a full app-wide `BackHandler` grep finding zero instances). `BookingViewModel`'s state is untouched by back navigation since it's scoped to the whole sub-graph, not per-screen. |
| Loading states | ✅ Code-verified | `BookingConfirmationViewModel` exposes both `isLoadingSummary` (resolving salon/specialist/service for display) and `isSubmitting` (the actual booking POST) as separate flags — the confirm button can correctly disable during submission without blocking the summary from having already rendered. `loadSummary()` is also memoized (`if (key == loadedForKey) return`) so a recomposition with unchanged params doesn't re-fetch. |

**Booking creation was NOT tested live** — per instruction, this would consume a real backend write and (depending on the salon) potentially affect real data. This section's verification is entirely code-level, as detailed above. **Separately and importantly**: per the backend-side audit completed earlier this session, the live backend currently returns `409 SALON_NOT_ACTIVE` for essentially every real booking attempt (the one previously-known fully-configured salon is still in `DRAFT`, and none of the 5 currently-active salons have a confirmed-complete service/specialist configuration) — this is a **backend data/deployment issue, not an Android defect**, already fully documented in `BOOKING-PRODUCTION-READINESS-REPORT.md`/`FINAL-MIGRATION-RELEASE-GATE.md`. The Android code's own handling of that 409 (calm, inline, no crash) was itself already confirmed via real on-device testing in the RC-3 pass.

---

## 6. Customer Profile

| Check | Result | Evidence |
|---|---|---|
| Profile loading | ✅ Code-verified, notably efficient | `ProfileScreen` does **not** make its own network call — it reads `authViewModel.currentUser` as a already-populated `StateFlow` (`collectAsStateWithLifecycle()`), the same state object populated once during login/session-restore. Visiting Profile costs zero extra API calls. |
| User info display | ✅ Code-verified | Name, phone, email all sourced from the same cached `currentUser`. |
| Existing media behavior | ✅ Carried over from real A72 verification (Phase 5B) | Avatar/cover tap → system Photo Picker (zero runtime permission prompt), pick → client-side resize/compress → multipart upload. Confirmed on-device: 0 crashes, calm inline error handling. |
| No broken UI after backend Media changes | ✅ Code-verified fresh this session | See Section 7 — the Android contract was re-checked directly against the real, newly-implemented backend shape and found unchanged/compatible. |

---

## 7. New Media APIs Compatibility

This section was re-verified **fresh this session**, specifically because the backend side changed materially (Media System M0 + Phase 5A.2, now built on `release/production-v24`).

| Check | Result | Evidence |
|---|---|---|
| Endpoint paths match | ✅ **Exact match, confirmed by direct comparison** | Android's `UserMediaApi.kt`: `POST/DELETE api/v1/users/me/media/{avatar,cover}` — **byte-for-byte the same four routes** just implemented backend-side this session. Multipart shape matches too (`@Part file: MultipartBody.Part` ↔ backend's `@RequestParam file: MultipartFile`). |
| DTO compatibility | ✅ Confirmed safe both ways | `UserResponseDto.avatarUrl`/`coverUrl` are `String? = null` — nullable with a safe default. Whether the live backend has these fields present (once `V24` deploys) or entirely absent from the JSON (current live state, pre-deployment), the DTO resolves correctly either way — no crash, no missing-field exception (kotlinx.serialization uses the declared default for an absent field). |
| No regression if UI doesn't yet consume new fields | ✅ N/A — UI already fully consumes them | The Android UI (`ProfileScreen`, Phase 5B, already A72-verified) already renders `avatarUrl`/`coverUrl` and already calls all four media endpoints — this isn't a "not yet wired" gap, it's a complete, already-built, already-tested client waiting on a backend deploy. |
| No unnecessary new calls added | ✅ Confirmed | Nothing was changed on the Android side this session — this was a read-only compatibility check, not an implementation pass. Upload/delete success responses update the already-in-memory `currentUser` directly (`authViewModel::applyUpdatedUser`) rather than triggering a fresh `GET /users/me` — no extra round-trip either way. |

**Conclusion: zero Android changes needed for Phase 5A.2 to go live.** The moment the real backend is deployed with `V24`, this exact APK will work against it — this is a testable claim verified by direct route/DTO comparison this session, not an assumption carried forward.

---

## 8. Performance

| Check | Result | Evidence |
|---|---|---|
| Startup time | 🟡 Fix implemented, **not yet device-verified** | Splash screen's fixed `minDisplayMillis=2600L` block was replaced with a real `ready: Boolean` signal overlapped with session-restore (confirmed still present in source: `SplashScreen(ready: Boolean, ...)`). Guest path should now be near-instant; returning-user worst case is unchanged (~5s, same pre-existing timeout guard) but collapses from two visible screen transitions to one. **This specific claim needs a real on-device timing pass before shipping** — it was implemented and statically verified, never timed on a real device (no device has been available since the fix was written). |
| Screen transitions | ✅ Code-verified | `saveState`/`restoreState` on tab navigation (confirmed present) means switching tabs no longer destroys/recreates ViewModels — the visible "flash of loading" on every tab revisit that this fixed should be gone, but again, **not device-timed**. |
| Memory | ✅ Spot-checked, no red flags found | `BackendApiContainer` (the app's central DI point) is always constructed from `context.applicationContext`, never a raw Activity context, and doesn't retain the constructor parameter beyond building sub-repositories — no context-leak pattern found there. `SalonListViewModel`/`BookingViewModel` use only `viewModelScope` (auto-cancelled), no manually-created `CoroutineScope` requiring manual cleanup. This is a targeted spot-check of the most likely leak locations, not a full profiler pass — a real memory-profiling session on-device is the only way to be fully certain, and wasn't performed here (no device). |
| Network duplication | ✅ No new duplication found; one pre-existing pattern reconfirmed | Tab-switch duplication already fixed (see above). The salon-details "N+1 services-per-category" pattern (Section 4) is real but pre-existing and small-scale for realistic category counts — flagged, not blocking. |
| Image loading | ✅ Code-verified | Coil's default `ImageLoader`, no custom `ImageLoaderFactory` (would require adding an `Application` class first — a real, deliberately-deferred, low-priority item from the prior performance audit, unchanged). Every remote image call site has a safe fallback (Section 3/4). |

---

## 9. Release Checklist

### Tested screens (code-level verification this session)
Splash, Auth/OTP screen (logic only), Customer Home (dashboard + Explore), Search, Salon List, Salon Details, Booking flow (Specialist → Service → Date → Time → Confirmation), Profile (incl. avatar/cover), plus the shared infrastructure every screen depends on: `TokenAuthenticator`, `SafeApiCall`, `BackendApiContainer`, `RojanRemoteImage`, `RojanNavGraph`.

### Passed
Everything in Sections 1–7's tables above marked ✅ — authentication/session/token handling, error handling depth, no-duplicate-call guarding (nav-level and submission-level, independently), state consistency across the booking flow, Profile's zero-extra-call design, and full forward/backward compatibility with the newly-implemented backend media API (exact route + DTO match, confirmed by direct comparison).

### Failed / not fully verified
- **Startup-time and tab-transition performance fixes are implemented and code-correct, but not timed on a real device.** This is the one item in this whole audit that genuinely needs a device before final sign-off — not because the code is suspect, but because "does it *feel* faster" is not something static analysis can answer.
- **Memory profiling** was a targeted spot-check (DI container, ViewModel scopes), not a full on-device profiler session.
- **Booking creation itself** was not exercised end-to-end live, per instruction (would require the backend to have an activated, fully-configured salon, which it currently does not — see below).

### Bugs found
None new. One minor, non-blocking observation: `BookingHistoryViewModel.load()` lacks the same `Job?.cancel()` guard `SalonListViewModel.load()` has — asymmetric but not currently exploitable given the current call pattern (worth a consistency pass, not urgent).

### API issues
**None caused by Android.** The one real, live API issue in the whole system — `POST /api/v1/bookings` returning `409 SALON_NOT_ACTIVE` for essentially every real attempt — is a backend data/deployment state issue (no currently-active salon is confirmed fully bookable; the previously-identified complete salon remains in `DRAFT`), already fully documented in this session's own backend reports, and outside Android's control. Android's own handling of that 409 (calm, inline, retryable, no crash) is already correct and already device-verified from a prior phase.

### Token/SMS/paid-API-consuming actions performed
**None.** No OTP was requested. No login was performed. No booking was created. No paid/quota-limited call of any kind was made. The one test that *can* reach a network (`BackendAuthFlowVerificationTest`) targets `http://localhost:8080` (hardcoded in the test file itself, confirmed by reading it) — not production — and failed with `ConnectException` because no local backend was running, meaning the request never actually left the machine. This is the same pre-existing, environment-dependent failure documented in every prior phase this session (307/309 → confirmed again this session as 309 total, 2 failed, same two tests, same reason).

### Confirmation: no unnecessary external requests were made
Confirmed. This entire audit was performed via source-code reading plus local `./gradlew` compile/lint/test runs — no `curl`, no live API probe, no device interaction, no OTP/SMS trigger of any kind. The only two live, read-only probes against the real production API (`GET /actuator/health`, `GET /api/v1/public/salons`) happened in the **prior** backend-focused task, not this one, and are not repeated here.

---

## Final result

```
Compile:  BUILD SUCCESSFUL (customerDevDebug, fresh run this session)
Lint:     BUILD SUCCESSFUL, 94 warnings / 0 errors (exact baseline, no new findings)
Tests:    309 total, 307 passed, 2 failed (both pre-existing, localhost-only, zero real network reached)
```

**Verdict: no Android-side release blocker found.** The app's own code is release-quality across every section audited — this matches, and does not contradict, the RC-3 conclusion from earlier this session. The one still-open item before actual submission is the same one RC-3 already identified: **a real on-device timing/click-through pass**, specifically for the two performance fixes, whenever a device becomes available — everything else in this checklist is either already device-verified from a prior phase or conclusively answerable from code alone. Separately, and not Android's to fix: the backend booking-activation gap must close (a real, activated, fully-configured salon) before a customer can complete an actual booking against production, regardless of how clean the app itself is.
