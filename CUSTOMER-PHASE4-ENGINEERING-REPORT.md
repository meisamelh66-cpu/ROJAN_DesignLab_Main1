# Customer App — Phase 4: Final Engineering Cleanup & Performance Pass

**Date:** 2026-09-10 · **Scope:** Customer Android app only · **Not committed.**

No feature, API-contract, navigation, or ViewModel-business-logic change. No UI redesign — the
only visual-adjacent work is *removing* three duplicate copies of one field and consolidating
token *values* that were already identical.

---

## Summary

| Part | Done |
|---|---|
| **P1.1** Shared `CustomerTextField` / `CustomerSearchField` | ✅ extracted from 3 hand-rolled copies |
| **P1.2** Token consolidation | ✅ 10–12 duplicate `Ref*` token literals per screen → import aliases of the `Customer*` single source, across 3 screens |
| **P1.3** Dead code | ✅ 4 zero-reference files deleted; unused imports pruned from the 6 touched screens |
| **P2.4** `safeApiCall` hardening | ✅ `SerializationException` / malformed body / any unforeseen exception now return a `Result.failure` — no process crash; `CancellationException` correctly re-thrown; +7 unit tests |
| **P2.5** Coil | ✅ `ImageRequest` now `remember`ed (no per-recomposition realloc); sizing already optimal — documented why no `.size()` |
| **P3** Startup audit | ✅ audited; recommendations below; **no unsafe change implemented** |
| **P4** Build gates + A72 | compile ✅ · lint ✅ (94 = baseline) · assemble ✅ · unit tests ✅ (309 run, only the 2 live-backend manual tests fail — network, pre-existing) · A72 <see Device test> |

---

## P1.1 — Shared text field

**New — `screens/customer/components/CustomerTextField.kt`** (~135 lines)

Three byte-identical hand-rolled fields existed:

| Copy | Shape |
|---|---|
| `SearchScreen.SearchField` | flat `CustomerSurfaceFill` box + 1px `CustomerHairline` + `CustomerCardShape`, rose-gold cursor, leading search glyph, placeholder-while-empty |
| `SalonListScreen.SearchField` | identical — only the placeholder string differed (`"نام سالن را جستجو کنید…"` vs `"جستجوی سالن…"`) |
| `AuthScreen.AuthField` | same box, plus a `Caption` label above, `keyboardType` + `enabled` params |

Now one composable:

```kotlin
@Composable
fun CustomerTextField(
    value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,          // caption above (AuthScreen)
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,  // search glyph
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
)

@Composable
fun CustomerSearchField(value, onValueChange, placeholder, modifier) =
    CustomerTextField(..., leadingIcon = Icons.Outlined.Search)
```

**Behaviour preserved exactly** — verified line-by-line before extraction:
- the `withDirectionFor(value.ifEmpty { placeholder ?: label ?: "" })` RTL heuristic (was
  `placeholder ?: label` in AuthField; the search copies forced RTL with a hardcoded Persian
  sample — the Persian placeholder now does the same job, same result);
- `singleLine`, `cursorBrush = SolidColor(CustomerAccent)`, the `value.isEmpty() && placeholder != null`
  placeholder gate, the 48dp (`RojanDimens.MinTouchTarget`) min height, `KeyboardOptions(keyboardType)`;
- focus / IME behaviour is `BasicTextField`'s, unchanged.

**Call sites unchanged.** The private `SearchField` in each screen and the private `AuthField`
became one-line delegates, so every call site (`SearchField(value, onValueChange, modifier)` ×2,
`AuthField(...)` ×3) is byte-identical. No screen's structure moved.

Removed as now-unused from the 3 screens: `BasicTextField`, `SolidColor`, `withDirectionFor`,
`Icons.Outlined.Search`, `border`, `heightIn`, `KeyboardOptions`, `TextOverflow` imports (only the
ones the compiler confirmed dead).

---

## P1.2 — Token consolidation

`CustomerHomeScreen`, `CustomerDashboardScreen`, and `SalonDetailsScreen` each declared a private
block of `Ref*` token `val`s whose **values were literal duplicates** of the `Customer*` design
tokens in `screens/customer/components/CustomerRefComponents.kt`:

```kotlin
private val RefScreenMargin = 20.dp                        // == CustomerScreenMargin
private val RefAccent = RojanPremiumBorderRoseGold         // == CustomerAccent
private val RefSurfaceFill = Color.White.copy(alpha = .045f) // == CustomerSurfaceFill
… 10–12 of them per file
```

Each duplicated `val` is replaced with an **import alias of the real token**, so the value lives in
exactly one place and the local name (and therefore every call site) is untouched:

```kotlin
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin as RefScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerAccent      as RefAccent
…
```

| File | Aliased (value now single-sourced) | Kept local (no `Customer*` equivalent) |
|---|---|---|
| `CustomerHomeScreen` | ScreenMargin, CardRadius, CardShape, Accent, OnAccent, SurfaceFill, Hairline, SectionLabelStyle | `RefTileRadius` (12dp), `RefTileFill` (White 5%) |
| `CustomerDashboardScreen` | + ButtonRadius, ButtonHeight | `RefTileFill` |
| `SalonDetailsScreen` | + TopBarHeight, Divider | `RefLogoSize`, `RefAvatarSize`, `RefSalonNameStyle` (26sp), `RefPriceStyle` |

`Customer*` in `CustomerRefComponents.kt` is now the **single source of truth** for all shared
token values. Because the values were already identical, **there is zero visual change** — this is
purely removing duplicate declarations. `RojanPremiumBorderRoseGold` / `FontWeight` imports pruned
where they became unused.

Not touched: the local `Ref*` *composables* (`RefSurface`, `RefPrimaryButton`, `RefSectionLabel`,
`RefContactRow`, …) in Dashboard / SalonDetails — those are screen-local UI, not tokens, and out of
scope.

---

## P1.3 — Dead code

Deleted (each had **zero references** anywhere in `app/src`):

| File | Why dead |
|---|---|
| `screens/customer/EmptyState.kt` | `@Composable fun EmptyState() {}` — empty stub |
| `screens/customer/LoadingState.kt` | `@Composable fun LoadingState() {}` — empty stub (its own successor `RojanLoadingState` already documented it as "an empty, unused stub") |
| `screens/customer/CustomerScreenScaffold.kt` | superseded by `CustomerScaffold` in Phase 1; never adopted, 0 call sites |
| `screens/bookingflow/components/BookingStepIndicator.kt` | deprecated alias (`= CustomerStepIndicator(...)`); its own doc says "new code should call `CustomerStepIndicator` directly"; 0 call sites |

Updated the `RojanLoadingState.kt` doc comment that referenced the removed `LoadingState.kt`.

**Kept (has references, or removal is a judgement call, not cleanup):**
- `components/Badge.kt` — a real, styled component referenced only by its own `@Preview`s. No
  production call site, but it is deliberate design-system inventory, not an accident — flagged,
  not deleted.
- The `bookingflow/components/` alias shim (`BookingScaffold`, `BookingAccent`, `BookingScreenMargin`,
  …) — still used by 6 booking screens. Migrating them onto `Customer*` directly is a booking-flow
  refactor, out of this phase's "no navigation / no risk to booking" scope.
- `screens/customer/` section stubs (`NearbySalons`, `PopularServices`, `PromotionsSection`,
  `RecommendedSalons`, `TopSpecialists`, `FollowedSalons`, `RecentVisits`, `UpcomingBookings`,
  `HomeHeader`, `AISearchBar`, `FeaturedSalons`) — all still referenced.

Unused imports pruned from `SearchScreen`, `SalonListScreen`, `AuthScreen`, `CustomerHomeScreen`,
`CustomerDashboardScreen`, `SalonDetailsScreen` as a side effect of P1.1/P1.2.

---

## P2.4 — `safeApiCall` runtime stability

**The bug:** `kotlinx.serialization.SerializationException` is a `RuntimeException`, not an
`IOException`. `safeApiCall` only caught `HttpException`, `SocketTimeoutException`, and
`IOException`, so when a 2xx response body no longer matched the DTO (a renamed/removed field, a
type change, malformed JSON), the exception **propagated out of the repository and crashed the
process** — the exact failure seen when `/api/v1/public/salons` shipped a leaner shape than the
Android DTO declared.

**`data/remote/SafeApiCall.kt` now:**

```kotlin
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e                                              // structured concurrency — never swallow
} catch (e: HttpException) {
    Result.failure(BackendApiException(e.code(), <decoded ApiError>))
} catch (e: SocketTimeoutException) {
    Result.failure(RequestTimeoutException(e))
} catch (e: SerializationException) {                    // ← covers JsonDecodingException + MissingFieldException
    Result.failure(MalformedResponseException(e))
} catch (e: IOException) {
    Result.failure(NetworkUnavailableException(e))
} catch (e: Exception) {                                 // ← nothing else can escape
    Result.failure(UnexpectedApiException(e))
}
```

Two new exception types, both `IOException` subtypes (so the many callers that already branch on
`IOException` treat them gracefully):

- **`MalformedResponseException`** — contract-drift / malformed body.
- **`UnexpectedApiException`** — any other unforeseen `Exception`.

**UI mapping** — `presentation/common/ErrorMessages.userMessageFor` gets an explicit branch:

```kotlin
is MalformedResponseException -> "پاسخ سرور قابل پردازش نبود. لطفاً بعداً دوباره تلاش کنید."
```

`UnexpectedApiException` falls to the existing `else -> "خطایی غیرمنتظره رخ آمد."` — every
`UiState.Error(userMessageFor(it))` call site (salon list/detail, search, booking date/time/confirm,
appointments, favorites, …) now renders a calm, retryable error state instead of the app dying.

**`CancellationException` is re-thrown** — a coroutine cancelled mid-call (screen left, ViewModel
cleared) must propagate as cancellation, not be reported as a failed request. Previously it fell
into `catch (IOException)` only if it happened to be one; now it is explicit and first.

**New — `app/src/test/.../data/remote/SafeApiCallTest.kt`** (7 tests, all green):
success pass-through · `MissingFieldException` → `MalformedResponseException` (not thrown) · raw
malformed JSON → same · `IllegalStateException` → `UnexpectedApiException` · plain `IOException` →
`NetworkUnavailableException` (unchanged) · `CancellationException` re-thrown by identity · bare
`SerializationException` covered.

---

## P2.5 — Image performance (Coil)

All remote-image loading in the app goes through one file: `ui/components/image/RojanRemoteImage.kt`
(`AsyncImage`, used for salon logos and specialist avatars).

**Change:** the `ImageRequest` is now `remember(url, context)`-ed instead of
`ImageRequest.Builder(context).data(url).crossfade(true).build()` **on every recomposition** — so a
scroll, a follow/favourite toggle, or any parent state change reuses the same request instance
instead of allocating a fresh builder + request each frame.

**Not changed, and why:**
- **Size constraints** — `AsyncImage` derives the decode size from the composable's measured
  bounds automatically, and the `Box(modifier)` is always bounded by the caller (72dp logo,
  64–88dp avatar). Adding an explicit `.size(...)` (e.g. `Size.ORIGINAL`) would *defeat* the
  downsampling and load full-resolution bitmaps. Left alone deliberately — documented in-file.
- **`crossfade(true)`**, `ContentScale.Crop`, the `onError → fallback` path, `clip(shape)`,
  the null/blank-url → fallback branch — all unchanged. No image rendering differs.
- Coil's memory + disk cache config is the library default; no eviction/size issue observed. No
  custom `ImageLoader` introduced.

---

## P3 — Startup performance audit

**What was reviewed:** `MainActivity`, `SplashScreen`, `RojanNavGraph`'s splash / session-restore
gate, `BackendApiContainer` / `BackendApiContainerHolder`, `applyRojanEdgeToEdge`, the manifest.

**The startup path is already lean:**
- **No custom `Application` class** — zero work at process start.
- **No `androidx.startup` initializers, no WorkManager, no eager DI.**
- `BackendApiContainerHolder` is a lazy, thread-safe (`@Volatile` + double-checked `synchronized`)
  singleton built from `applicationContext` (no leak). Retrofit `.create()` is a lazy dynamic
  proxy; the two `OkHttpClient`s cost ~1–3ms each and are built on first access (first composition).
- `MainActivity.onCreate` does `applyRojanEdgeToEdge()` + `setContent { RojanNavGraph() }` — minimal.
- Session restore (`SessionViewModel.restoreState` → `GET /api/v1/auth/me` with token refresh)
  runs **concurrently with the splash** and correctly blocks routing on the *validated* result
  (the "validate before route" fix) — this is correct, not a perf bug, and already has
  `RequestTimeoutException` handling for the offline-cold-start case.

**Findings (recommendations — NOT implemented, each is a visible behaviour change needing a product/design call):**

| # | Finding | Recommendation | Why not done here |
|---|---|---|---|
| P3-1 | `SplashScreen(minDisplayMillis = 2600L)` blocks app entry for a **fixed 2.6s + 0.4s fade ≈ 3s**, regardless of how fast session-restore resolves (often < 500ms on a warm start). | Gate splash dismissal on `max(brandFloor, restoreReady)` with `brandFloor ≈ 1000–1500ms`. A returning user with a valid cached session would enter in ~1.2s instead of ~3s; the logo animation (fade-in at 0/200/450ms) still completes. | Changes a visible timing / brand moment — out of "no redesign". |
| P3-2 | No `androidx.core:core-splashscreen`. The first ~200–400ms (process → first Compose frame) shows the plain window background before the Compose `SplashScreen` mounts. | Add `installSplashScreen()` + a `windowSplashScreenBackground` theme attr so the brand paints from frame 0 and hands off to the Compose splash seamlessly. | New dependency + theme change; additive but deserves its own change. |
| P3-3 | The two `OkHttpClient`s + ~25 Retrofit interfaces build synchronously on first composition (just after the splash, main thread). | If ever a measured jank source: warm `BackendApiContainerHolder.get(appContext)` on `Dispatchers.Default` from a `LaunchedEffect` during the splash. | Not a measured problem; poor risk/reward. |

---

## P4 — Production quality gates

| Gate | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL** · EXIT 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `0 errors, 94 warnings` — **identical to the pre-Phase-4 baseline**. Zero new findings; the only lint lines touching a changed file are 2 pre-existing `UseKtx` warnings in `SalonDetailsScreen.kt` (lines 285/365, `Uri.parse` for phone/map links — untouched by this phase). |
| `:app:assembleCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `app-customer-dev-debug.apk` produced |
| `:app:testCustomerDevDebugUnitTest` | 309 tests run, **307 pass + 7 new `SafeApiCallTest` green**. The 2 failures are `BackendAuthFlowVerificationTest` — a manual connectivity trigger that *"Requires ROJAN_Backend's Spring Boot app running locally at localhost:8080 … not wired into any CI/build gate"* (its own doc). Fails with `java.net.ConnectException` — environmental, not touched by this phase. |

### Files changed

| File | Change |
|---|---|
| `screens/customer/components/CustomerTextField.kt` | **new** — shared field |
| `data/remote/SafeApiCall.kt` | +`MalformedResponseException`, +`UnexpectedApiException`, +`SerializationException`/`Exception`/`CancellationException` handling |
| `presentation/common/ErrorMessages.kt` | +`MalformedResponseException` branch |
| `ui/components/image/RojanRemoteImage.kt` | `remember` the `ImageRequest` |
| `screens/search/SearchScreen.kt`, `screens/booking/SalonListScreen.kt`, `screens/auth/AuthScreen.kt` | field → shared component delegate; unused imports pruned |
| `screens/customer/CustomerHomeScreen.kt`, `screens/customer/CustomerDashboardScreen.kt`, `screens/salon/SalonDetailsScreen.kt` | duplicate token `val`s → import aliases of `Customer*`; unused imports pruned |
| `ui/components/state/RojanLoadingState.kt` | doc comment updated for the removed stub |
| `screens/customer/{EmptyState,LoadingState,CustomerScreenScaffold}.kt`, `screens/bookingflow/components/BookingStepIndicator.kt` | **deleted** (4 zero-reference files) |
| `app/src/test/.../data/remote/SafeApiCallTest.kt` | **new** — 7 tests |

No ViewModel business logic, no repository interface, no API/DTO contract, no navigation route or
graph, no `strings.xml` key, no colour/spacing/typography token *value* changed.

---

## Device test — A72

**Device:** Samsung Galaxy A72 (`RZ8R81WPS2J`), Android 14, 1080×2400 · **Build:** this branch's
`app-customer-dev-debug.apk` · **Crash log:** `adb logcat -b crash` → **`FATAL EXCEPTION` count = 0**
across the entire run; no `SerializationException` / `MalformedResponseException` /
`UnexpectedApiException` in logcat either.

| # | Step | Result |
|---|---|---|
| 1 | **Cold launch** | Splash → Home (`سلام گیتا جان`), cached session restored. Bottom bar, `GET /bookings/mine` 200. ✅ `01_cold_launch_home.png` |
| 2 | **Explore** | `کشف سالن‌ها` — `GET /api/v1/salons` 200 (authed). The flat search field (now `CustomerSearchField`) renders with its placeholder. ✅ `02_explore.png` |
| 3 | **Search field** (the `CustomerTextField` extraction) | Opened `SearchScreen` — placeholder `"نام سالن را جستجو کنید…"`, `نتایج (12)`. Focus + keyboard input updates the value, RTL correct, debounced search fires, empty-state shows for a no-match query. ✅ `03_search_field.png` |
| 4 | **Booking flow** | Explore → `ROJAN AI Pilot Salon` (SalonDetailsScreen — the most token-consolidated screen — renders identically: 72dp logo, contact card, services/specialists) → `Haircut` service (`25 تومان` / `30 دقیقه`) → `رزرو این خدمت` → **`انتخاب ساعت`** with slots, `…/available-slots` 200. No regression. ✅ `04_salon_detail.png`, `05_booking_time.png` |
| 5 | **Profile** | `پروفایل` tab → `حساب کاربری` / `گیتا` / `+989164987585`, flat rows, rose-gold accents. ✅ `06_profile.png` |
| 6 | **Logout** | Profile → `خروج از حساب` → **`CustomerConfirmDialog`** (`از حساب کاربری خود خارج می‌شوید؟` · `خروج` / `انصراف`) → confirm → lands on **Explore as guest**: `GET /api/v1/public/salons` 200, salon list renders, **no `"مشکلی پیش آمد"` error**, bottom bar present. ✅ `07_logout_tap.png`, `07_logout_result.png`, `08_guest_explore.png` |
| 7 | **Login** | Guest → Profile → `نوبت‌های من` (a `CustomerAccessGuard` route) → **`AuthScreen`** (`ورود به روژان`). The phone field (`CustomerTextField` via `AuthField`) shows the caption label, flat box, **numeric keyboard** (`KeyboardType.Phone` preserved), **rose-gold cursor**, digits LTR inside the RTL layout. Entered `09164987585` → `ارسال کد تایید` → `POST /auth/otp/request` 200 → code step → OTP `3222` (read from SMS) → `تایید و ورود` → `POST /auth/otp/verify` 200 → `GET /bookings/mine` 200, resumed authenticated at `نوبت‌های من`. ✅ `09_auth_phone.png`, `10_logged_in.png` |
| 8 | **Home (post-login)** | `خانه` tab → `سلام گیتا جان`, authed dashboard, `GET /api/v1/salons` 200. ✅ `11_home_authed.png` |

**Every part of Phase 4 that a device can exercise is verified:** the shared `CustomerTextField` /
`CustomerSearchField` (search field + auth phone/code fields — focus, RTL, numeric keyboard,
rose-gold cursor, placeholder, debounce all intact); the token-consolidated screens
(`CustomerHomeScreen`, `CustomerDashboardScreen`, `SalonDetailsScreen`) render pixel-identically;
the deleted dead files caused no missing-screen; every network call succeeds and the hardened
`safeApiCall` path is exercised (public + authed salon lists, OTP, bookings) with **zero crashes**.

---

## Follow-ups (not in this phase)

1. **P3-1 / P3-2 splash** — the single highest-impact startup win; needs a product call on the
   brand-display floor.
2. **Booking `Booking*` alias shim → `Customer*`** — finish what P1.2 started, once a booking-flow
   pass is scheduled.
3. **`components/Badge.kt`** — adopt it (recommended salon cards?) or delete it.
4. **`SerializationException` at the converter boundary** — consider a Retrofit `Converter.Factory`
   wrapper so a malformed body is a typed failure even for code paths that ever bypass
   `safeApiCall`.

Nothing was committed.
