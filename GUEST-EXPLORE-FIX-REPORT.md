# Guest Explore Fix — guest-aware salon discovery

**Date:** 2026-09-10 · **App:** `ai.rojan.designlab` (customer) · **Device:** Samsung Galaxy A72
(SM-A725F, Android 14) · **Backend:** live `https://api.rojanai.ir` · **Not committed.**

Follows `CUSTOMER-PRE-RELEASE-FINAL-AUDIT.md` §1.

---

## TL;DR

After logout the user landed on **Explore** and saw **"مشکلی پیش آمد" + "تلاش مجدد"**. Logout was
never broken: Explore's salon list came from the **authenticated** `GET /api/v1/salons`, which
answers **401** for a guest (and for a brand-new user on first launch), rendered as a generic error
wall with a dead-end retry.

**The fix is Android-only.** The backend already has the endpoint it needs — `GET /api/v1/public/salons`
(unauthenticated marketplace directory) **is built and deployed in production** by another team
(`PublicSalonDirectoryController`, on `origin/fix/web-otp-sms-origin-binding-production` /
`origin/feat/lbs-nearby-salons`). An earlier draft of this fix added a *second, conflicting*
`PublicSalonDirectoryController` — that has been **reverted**. The Android app now:

- routes salon browsing to `GET /api/v1/public/salons` whenever there is no access token, and to the
  authenticated `GET /api/v1/salons` (unchanged) when there is one;
- carries a **fully-nullable** directory DTO (only `id` + `name` guaranteed) — a strict DTO crashed
  on-device against the real, leaner response;
- keeps every booking/login guard exactly as-is; a defensive login CTA replaces the error wall for
  the rare case an *authenticated* browse still 401s.

**Device verification: all 3 tests PASS on the A72** (see §4). One real crash was found during
verification (strict DTO vs. lean response) and fixed.

---

## 1. Root cause

`ProfileScreen` logout → `AuthViewModel.logout()` (local token clear only — correct, no backend
call) → `navigate(EXPLORE)` → `CustomerHomeScreen` → `SalonListViewModel.init { load() }` →
`SalonApi` `@GET("api/v1/salons")` (authenticated) → no token → backend
`SecurityConfig: anyRequest().authenticated()` → **401** → `UiState.Error` →
`ExploreMessage(title = "مشکلی پیش آمد", actionLabel = "تلاش مجدد")`. Retry re-issues the same
tokenless call. Explore is the designated landing screen for unauthenticated users
(`RojanNavGraph.kt` `startDestination` → `EXPLORE` when `personId == null`), so **first launch hit
the same wall**.

---

## 2. Backend — no change (endpoint already exists & is deployed)

Production `GET https://api.rojanai.ir/api/v1/public/salons?page=0&size=20` returns **200**:

```json
{"content":[
  {"id":"c5d20a00-…","slug":"salon-c5d20a0051","name":"روژ","logoUrl":"https://…","coverUrl":"https://…"},
  {"id":"d3bde5e4-…","slug":"salon-d3bde5e4aa","name":"زیبا سرای حنا"},
  … ],"page":0,"size":20,"totalElements":5,"totalPages":1}
```

This is `PublicSalonDirectoryController` from `origin/fix/web-otp-sms-origin-binding-production`
(also on `origin/feat/lbs-nearby-salons`):

| Aspect | Deployed contract |
|---|---|
| Route | `GET /api/v1/public/salons` (permit-all `/api/v1/public/**`) |
| Query params | `page`, `size`, **`search`** (not `name`), `city`, `sortDirection`, `lat`, `lng`, `radiusKm` |
| Repo | `salonRepository.findAllPubliclyDiscoverable(...)` — 5 salons on prod, DRAFT excluded |
| Row DTO | `PublicSalonListResponse` = `{ id, slug, name, logoUrl?, coverUrl?, distanceKm? }` — `phone`/`address`/`description` deliberately kept behind the by-slug endpoint |

**An earlier draft of this fix that added its own `PublicSalonDirectoryController` + `PublicSalonSummaryResponse`
+ a bootstrap test has been reverted** — merging it would have produced two `@RequestMapping("/api/v1/public/salons")`
controllers (ambiguous mapping / duplicate bean). The P0-5 booking-filter backend changes
(`SalonSpringDataRepository` / `SalonRepositoryAdapter` / `SalonRepository` + tests) are untouched
and still stand.

> Note: the P0-5 fix (authenticated `GET /api/v1/salons` also excludes DRAFT) is **not yet deployed**,
> so on prod the *authenticated* list still shows the DRAFT pilot salon while the *public* list
> correctly does not. The two lists converge once P0-5 ships. This is visible in the screenshots
> (§4): the authenticated Explore shows "ROJAN AI Pilot Salon", the guest Explore does not.

---

## 3. Android changes (`ROJAN_DesignLab`)

| File | Change |
|---|---|
| **`data/remote/dto/PublicSalonDtos.kt`** | New `PublicSalonSummaryResponseDto` mirroring `PublicSalonListResponse`. **Only `id` and `name` are non-nullable** — every other field (`slug`, `description`, `phone`, `email`, `address`, `logoUrl`, `coverUrl`, `latitude`, `longitude`, `distanceKm`) is nullable/defaulted, so a lean row can never throw `MissingFieldException`. |
| **`data/remote/PublicSalonApi.kt`** | `browseSalons(page, size, search, sortDirection)` → `@GET("api/v1/public/salons")`. Filter param is **`search`** (matches the deployed contract, not `name`). Built on the existing no-token client. |
| **`domain/repository/PublicSalonRepository.kt`** | New `browseSalons(page, size, nameFilter, sortDirection): Result<PagedResult<Salon>>`. `nameFilter` maps to the backend's `search`. |
| **`data/repository/PublicSalonRepositoryImpl.kt`** | Implements `browseSalons`; maps `PublicSalonSummaryResponseDto -> Salon` with `phone = phone.orEmpty()`, `address = address.orEmpty()` (the directory row omits them; the booking flow re-fetches the full authenticated salon before it needs a phone). |
| **`presentation/salon/SalonListViewModel.kt`** | New optional ctor params `publicSalonRepository: PublicSalonRepository?` + `hasSession: () -> Boolean` (default `{ true }` — untouched call sites keep the authenticated path). New private `browseSalons(page, nameFilter)` picks **public** when `publicSalonRepository != null && !hasSession()`, else the existing authenticated `salonRepository.browseSalons` **unchanged**. `hasSession()` re-checked per call. `loadRelationshipState()` returns early for a guest (follow/favorite are per-account, auth-only). `isUnauthorized` / 401 path kept as a defensive net. |
| **`presentation/salon/SalonListViewModelFactory.kt`** | Threads the two new params. |
| **`screens/customer/CustomerHomeScreen.kt`** (Explore) | Factory wired with `publicSalonRepository = container.publicSalonRepository`, `hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true }`. New `onLoginClick` param. New error branch: on `isUnauthorized`, show `"برای مشاهده سالن‌ها وارد شوید"` + `"ورود"` CTA instead of `"مشکلی پیش آمد"`. `ExploreSalonCard`'s address row is now guarded with `isNotBlank()` (the public row has no address → no orphan map-pin). |
| **`screens/search/SearchScreen.kt`** | Same factory wiring — guest search works. Existing `onLoginRequired` path unchanged. |
| **`screens/booking/SalonListScreen.kt`** | Same factory wiring — a guest in the booking funnel browses/picks a salon; the login gate stays at `BOOKING_TIME`. |
| **`navigation/RojanNavGraph.kt`** | Passes `onLoginClick = { navController.navigate(RojanDestinations.AUTH) }` to `CustomerHomeScreen`. |
| **`app/src/test/.../SalonListViewModelTest.kt`** | +4 guest-routing tests. |

**Not changed:** `SalonApi`, `SalonRepository`/`SalonRepositoryImpl` (authenticated path byte-identical),
`AuthViewModel`, session/guard logic, the booking flow, `BookingViewModel`, navigation routes,
`BackendApiContainer` (both `publicSalonRepository` and `tokenRepository` were already exposed).

### Routing

```
SalonListViewModel.browseSalons(page, nameFilter):
    if publicSalonRepository != null AND !hasSession():   -> GET /api/v1/public/salons?search=…   (guest)
    else:                                                 -> GET /api/v1/salons?name=…             (authed, unchanged)
```

---

## 4. Device verification — Samsung A72, all PASS

Build: `:app:assembleCustomerDevDebug` (BUILD SUCCESSFUL), `adb install -r`. Evidence = OkHttp
request-line logcat (`BuildConfig.DEBUG` logging) + screenshots + crash buffer.

### TEST 1 — Logout flow → `logout_after.png` ✅

| Step | Result |
|---|---|
| Login (OTP, +989164987585, real SMS code) | `POST /auth/otp/verify` → 200, session stored |
| Profile → "خروج از حساب" → confirm dialog ("از حساب کاربری خود خارج می‌شوید؟") → "خروج" | — |
| After confirm | App **stays foreground** (`MainActivity` resumed), navigates to Explore ("کشف سالن‌ها") |
| Network | **`GET /api/v1/public/salons` → 200** — no `GET /api/v1/salons`, **no 401** |
| Screen | Salon list renders (روژ, زیبا سرای حنا, سالن زیبایی ملک بانو, ققنوس, میاه) — **no "مشکلی پیش آمد"** |
| Crash buffer | **0 FATAL** |
| Tokens after logout | cleared (`secure_token_preferences.xml` has 0 `access_token`) |

### TEST 2 — Fresh guest → `guest_explore.png` ✅

| Step | Result |
|---|---|
| `adb shell pm clear ai.rojan.designlab` → launch → (no login) | Lands on Explore as guest |
| Network | **`GET /api/v1/public/salons?page=0&size=20&sortDirection=ASC` → 200** |
| Screen | "کشف سالن‌ها" + 5 public salon cards — logo image (روژ) or storefront-icon fallback, name; no address line (public row omits it, card degrades cleanly) |
| Errors / crash | **none** — no "مشکلی پیش آمد", crash buffer clean |

### TEST 3 — Auth regression → `authenticated_explore.png` ✅

| Step | Result |
|---|---|
| Fresh relaunch, authenticated (token restored, `/users/me` → 200) | — |
| Explore | **`GET /api/v1/salons?page=0&size=20&sortDirection=ASC` → 200** (authenticated endpoint, **not** public) — shows the authed list incl. "ROJAN AI Pilot Salon" with full description + address |
| Salon → "Haircut" service → book | `GET .../categories`, `.../services`, `.../specialists`, `.../working-hours` all 200 |
| "انتخاب ساعت" (step 4/5) | `GET .../available-slots` → 200, full slot grid (09:00 … 17:30) — `t3_available_times.png` |
| Booking guard | authenticated → no login prompt (correct). Guest → **still routed to AUTH** ("ورود به روژان / برای ادامه، شماره موبایل خود را وارد کنید") when tapping a protected tab — `t1_login_gate.png` |
| Crash buffer | **0 FATAL** |

### Crash found & fixed during verification

First fixed build crashed on the **guest** path: `kotlinx.serialization.MissingFieldException:
Fields [phone, address] are required for type 'PublicSalonSummaryResponseDto'`. The initial DTO
declared `phone`/`address` non-nullable; the live `/api/v1/public/salons` row omits them.
`safeApiCall` catches `HttpException`/`IOException`/`SocketTimeoutException` but **not**
`SerializationException`, so it propagated and killed the process. **Fix:** made the DTO
fully-nullable (only `id`+`name` required). Re-verified — no crash. (Recommend separately: add a
`SerializationException` catch to `safeApiCall` so *any* future backend shape drift degrades to a
`Result.failure` instead of a crash — this is a latent risk across every repository, not just this one.)

### Screenshots (`docs/device-verification/guest-explore/`)

`logout_after.png`, `guest_explore.png`, `authenticated_explore.png` (the 3 requested) plus
`t1_login_gate.png`, `t1_confirm_dialog.png`, `t1_profile_scrolled.png`, `t3_salon_detail.png`,
`t3_service.png`, `t3_available_times.png`.

---

## 5. Automated verification

- `:app:compileCustomerDevDebugKotlin` — EXIT 0.
- **`SalonListViewModelTest` — 18/18 pass** (14 pre-existing + 4 new):
  `a guest browses the public directory, never the authenticated endpoint`;
  `an authenticated caller browses the authenticated endpoint, never the public one`;
  `a guest load leaves follow and favorite state empty`;
  `hasSession is re-checked per call - a guest who logs in then retries hits the authenticated endpoint`.
- `:app:lintCustomerDevDebug` — BUILD SUCCESSFUL (0 errors) *(from the pre-crash-fix build; the
  fix is a DTO nullability + one `takeIf` guard — re-run before commit).*
- `:app:testCustomerDevDebugUnitTest` whole task: only failures are the 2 pre-existing
  `BackendAuthFlowVerificationTest` cases (a manual trigger needing `localhost:8080`;
  `java.net.ConnectException`; not a build gate).
- Backend: P0-5 salon suites still green; this fix adds **no** backend code.

---

## 6. Known follow-ups (not blockers)

1. **Guest → login → return-to-Explore staleness.** If a user browses Explore as a guest, then logs
   in and navigates back to that same (still-composed) Explore, `SalonListViewModel` keeps the
   guest-fetched public list until a reload — follow/favorite indicators won't appear until then.
   Matching `SalonListScreen`/`SearchScreen`'s existing `LifecycleResumeEffect` retry-on-resume in
   `CustomerHomeScreen` would close this. Minor; the data is a correct subset.
2. **`safeApiCall` + `SerializationException`** — see §4. Cross-cutting hardening.
3. **P0-5 deploy** — until it ships, the authenticated `GET /api/v1/salons` still leaks DRAFT
   salons; the public list is already correct.

---

## 7. Commit note

**Not committed** (per task). Backend: nothing to commit for this fix (P0-5 changes are separate).
Android: the `ROJAN_DesignLab` tree carries extensive prior uncommitted work; this fix's hunks in
`RojanNavGraph.kt` / `CustomerHomeScreen.kt` / `SearchScreen.kt` / `SalonListScreen.kt` sit on top
of already-modified files, so a clean commit needs `git add -p` there plus whole-file adds of the
5 new/VM/DTO/repo/test files.
