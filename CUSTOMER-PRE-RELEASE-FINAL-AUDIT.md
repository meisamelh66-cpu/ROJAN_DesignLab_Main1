# ROJAN Customer App — Pre-Release Final Issue Audit

**Date:** 2026-09-10 · **App:** `ai.rojan.designlab` (customer flavor), versionName `1.0.0`, versionCode `1`
**Scope:** Customer Android app (`C:\AndroidProjects\ROJAN_DesignLab`) + the two backing repos
(`ROJAN_Backend`, `ROJAN_Web`) where a customer-facing fix must land there.
**Audit only — no code modified, nothing committed.**

Cross-references the earlier `CUSTOMER-RELEASE-READINESS-FINAL.md` (the 5 original P0s) and
`BOOKING-FILTER-FIX-REPORT.md` (P0-5 fix applied, not yet deployed).

---

## 0. Executive summary

| # | Issue | Verdict | Priority | Blast radius |
|---|---|---|---|---|
| 1 | Logout "failure" (`مشکلی پیش آمد، تلاش مجدد`) | **Root-caused** — not a logout bug; Explore/discovery requires auth | **P0** | Every guest + every logout; also first-launch |
| 2 | Profile avatar | **Not supported anywhere** — no model field, no endpoint, no user-scoped storage | **P2** | Cosmetic; net-new feature |
| 3 | Old visual theme still live | **4 live screens + ~13 dead files** still on glass/premium | **P1** | Search, salon picker, specialist profile, public deep-link + account sub-screens |
| 4 | Performance | **No baseline profile, no crash telemetry, 4 OkHttp clients, ~161 MB PSS** | **P1** | Cold-start jank, zero production crash visibility |
| 5 | Update system | **Does not exist** | **P1** | Cannot push a fix to installed users, cannot force-upgrade off a broken build |
| 6 | Website APK download | **Infrastructure exists; Customer entry is empty** (`fileName: null`, `available: false`) | **P1** | The only distribution channel until store listings exist |

**Recommended order:** 1 → 5 → 4 (crash reporting only) → 3 → 6 → 4 (rest) → 2.
Issue 1 is a hard release blocker. Issues 5 + 6 + crash-reporting must be done *before* the APK
reaches a single external user, because without them a bad build is unrecoverable and invisible.

---

## 1. Logout failure — `مشکلی پیش آمد، تلاش مجدد`

### 1.1 What actually happens

Logout **succeeds**. The error the user sees belongs to the screen logout lands them on.

**Trace:**

1. `ProfileScreen.kt:208` — confirm dialog → `onLogoutClick()`.
2. `RojanNavGraph.kt:904-910` — `authViewModel.logout()` then `navigate(EXPLORE)` with
   `popUpTo(startDestination){inclusive=true}`.
3. `AuthViewModel.logout()` (`AuthViewModel.kt:295-310`) — clears `sessionProvider`, `tokenRepository.clearTokens()`,
   resets state, fire-and-forget `authSessionRepository.clearPersonId()`. **Purely local. No backend call.** Correct.
4. `EXPLORE` route (`RojanNavGraph.kt:806-855`) composes `CustomerHomeScreen`.
5. `CustomerHomeScreen.kt:136-138` creates `SalonListViewModel`; its `init { load() }`
   (`SalonListViewModel.kt:74`) immediately calls `salonRepository.browseSalons(...)`.
6. `SalonRepositoryImpl` → `SalonApi.kt:12` → `@GET("api/v1/salons")` — the **authenticated**
   endpoint. `AuthInterceptor.kt:14-15` attaches no header (no token). `TokenAuthenticator.kt:83`
   has no refresh token → returns `null`.
7. Backend `SecurityConfig.kt:61` — `anyRequest().authenticated()`; `/api/v1/salons` is **not** in
   `ALWAYS_PUBLIC_ENDPOINTS` → **HTTP 401**.
8. `SalonListViewModel.kt:96-101` → `state = UiState.Error(userMessageFor(error))` and
   `isUnauthorized = true`.
9. `CustomerHomeScreen.kt:187-195` renders `ExploreMessage(title = "مشکلی پیش آمد", body = <401 message>,
   actionLabel = "تلاش مجدد", onAction = ::retry)`. **Retry re-issues the same tokenless call → 401 again.**

### 1.2 Root cause

**The Explore / salon-discovery screen calls an authentication-only endpoint (`GET /api/v1/salons`)
but is the designated landing screen for unauthenticated users** (`RojanNavGraph.kt:286-292`:
"an unauthenticated (first-time or logged-out) customer now lands on EXPLORE").

Two compounding defects:

- **A (primary, cross-cutting):** there is no *public* salon-list endpoint. `PublicSalonController`
  only exposes `/api/v1/public/salons/{slug}` (by slug) — no list. So a guest literally cannot be
  shown salons. This also breaks **first launch** for a brand-new user, not just logout.
- **B (screen-level):** `CustomerHomeScreen` ignores `SalonListViewModel.isUnauthorized`.
  `SalonListScreen` (the booking-funnel picker) *does* handle it and shows a "log in" affordance
  (`SalonListViewModel.kt:24-28` doc comment). Explore was never given the same treatment.

Note: `CUSTOMER-RELEASE-READINESS-FINAL.md` already logged this as `REL14/REL15` ("guest Explore
error wall"). This audit adds the exact mechanism and confirms the string the user quoted is the
`title` ("مشکلی پیش آمد") + retry button of that same error state, with the 401 message as the body.

### 1.3 Secondary finding — no server-side logout

There is no `POST /api/v1/auth/logout` (`AuthApi.kt` has none; backend `api/auth/` has none). The
refresh token stays valid server-side until natural expiry after logout. Low risk (tokens are
Android-Keystore-encrypted, cleared locally) but a stolen pre-logout refresh token cannot be
revoked. **P2.**

### 1.4 Fix plan

**Preferred — fix A at the backend (unblocks guest browsing properly):**

1. Add a public list endpoint: `GET /api/v1/public/salons` (paged, name filter, sort) in
   `PublicSalonController` or a sibling, returning the same `active && onboardingStatus == ACTIVE`
   set the P0-5 fix already enforces for `GET /api/v1/salons`. Reuse `SalonRepository.findAllActive`.
   Add it to `ALWAYS_PUBLIC_ENDPOINTS` (it is already covered by `/api/v1/public/**`).
   *Check whether the deployed branch `origin/release/v1.2.0-…` already has this — the P0-5 session
   observed `GET /api/v1/public/salons?size=100` returning 200 on prod, which this working tree's
   `PublicSalonController` cannot serve. Confirm before building anything new.*
2. Android: point customer discovery (`SalonListViewModel` / a new `PublicSalonListRepository`) at
   the public endpoint when there is no session, or unconditionally (the authed and public sets are
   now identical post-P0-5). `PublicSalonRepositoryImpl` + `buildPlainRetrofit()` already exist as
   the no-token seam.
3. Keep every authed-only action (favourite, follow, book) gated exactly as today.

**Minimum — fix B only (Android-only, ships without a backend deploy):**

- `CustomerHomeScreen`: when `salonListViewModel.isUnauthorized`, render a calm "برای مشاهده و رزرو
  وارد شوید" cell with a login CTA (route to `AUTH`) instead of `ExploreMessage(title="مشکلی پیش آمد")`.
  Mirror `SalonListScreen`'s existing handling. This removes the "logout looks broken" perception
  but still leaves guests with an empty discovery screen.

**Estimated files:**
- Backend: `PublicSalonController.kt`, `SecurityConfig.kt` (verify), 1 new DTO, 1 integration test. (~4)
- Android (preferred): `SalonApi`/new `PublicSalonListApi`, `SalonRepositoryImpl`/new impl,
  `SalonListViewModel` or a factory switch, `CustomerHomeScreen.kt`, `BackendApiContainer.kt`. (~6)
- Android (minimum): `CustomerHomeScreen.kt` only. (1)

---

## 2. Profile avatar

### 2.1 Current state

| Layer | Finding | Evidence |
|---|---|---|
| Android UI | `ProfileScreen.kt:220-256` `IdentityHeader` draws an initial-letter circle (first char of name) or an outlined `Person` icon. No image path. The KDoc (`ProfileScreen.kt:95-99`) explicitly says photo is "intentionally not shown — no such field exists on the backend `User` model and no update-profile endpoint exists yet." | source |
| Android model | `AuthenticatedUser` (`BackendAuthRepository.kt:12-18`) = `id, email?, phoneNumber?, fullName, role`. No avatar. `UserResponseDto` (`AuthDtos.kt:51-58`) — same 5 fields. | source |
| Backend model | `domain/user/User.kt` — `id, email?, passwordHash?, phoneNumber?, fullName, role, active, createdAt, updatedAt`. **No avatar / photo / image field.** DB migration `V5__mobile_authentication.sql` is the latest user-touching one. | source |
| Backend API | `UserController.kt` exposes only `GET /me` and `GET /me/salon-access`. **No `PATCH /me`, no avatar upload.** `User.rename()` exists on the domain object but is not wired to any endpoint — a customer cannot even change their name post-signup. | source |
| Storage | Media infra exists (`MediaController`, `UploadMediaUseCase`, `MediaStoragePort`, `MediaAsset`) but is **100% salon-scoped**: route `/api/v1/salons/{salonId}/media`, `MediaOwnerType` enum has only `SALON`, gated on `Permission.MANAGE_SALON`. Backing store is `LocalDiskMediaStorageAdapter` (local filesystem, `MediaProperties`) — **not** S3/CDN. | source |

**Conclusion:** avatar support does not exist at any layer. This is a **net-new feature**, not a bug.
It is **P2** — the initial-letter circle is a legitimate, common pattern (matches the Quiet Luxury
redesign) and nothing about the release depends on real photos.

### 2.2 Implementation plan (when prioritised)

**Decision required first:** local disk vs. object storage. `LocalDiskMediaStorageAdapter` does not
survive a container redeploy and does not scale horizontally. Avatars (like salon media) should move
to S3-compatible object storage + CDN before *either* is real at volume. This is a shared
infrastructure decision, not avatar-specific.

**Backend (hexagonal — mirrors the salon-media vertical):**

1. Domain: add `avatarUrl: String?` (nullable) to `User`; a `changeAvatar(url)` / `clearAvatar()`
   method with the `updatedAt` bump. Flyway migration `V<next>__user_avatar.sql` adding
   `users.avatar_url VARCHAR NULL`.
2. Media: extend `MediaOwnerType` with `USER`; allow `UploadMediaUseCase` to accept a user-owned
   asset (owner = caller's `UserId`, tenant check replaced by "owner == caller").
3. API: `UserController` — `POST /api/v1/users/me/avatar` (multipart, self only),
   `DELETE /api/v1/users/me/avatar`. Add `avatarUrl` to `UserResponse`. Consider also finally wiring
   `PATCH /api/v1/users/me` for `fullName` while here (same screen, same gap).
4. Storage: implement `S3MediaStorageAdapter` (or keep `LocalDisk` for a pilot and accept the
   redeploy-wipe risk — document it).
5. Tests: use-case unit tests + a `UserAvatarFlowIntegrationTest` (upload → `GET /me` shows URL →
   delete → gone; cross-user upload rejected).

**Android:**

6. Add `avatarUrl` to `UserResponseDto` + `AuthenticatedUser` (both already tolerate new nullable
   fields).
7. `ProfileScreen.IdentityHeader` — render `RojanRemoteImage(url = currentUser?.avatarUrl, fallback = { <current initial circle> })`. The fallback path already exists.
8. New "ویرایش پروفایل" screen or a tap target on the header: image picker (`PickVisualMedia`
   contract — no storage permission needed on any supported SDK), client-side downscale/crop to
   ~512², `POST /users/me/avatar` via a new `UserProfileRepository`, refresh `currentUser`.
9. `AndroidManifest` — no new permission (Photo Picker is permissionless). Add Coil `size(256)` hint
   for the avatar request.

**Estimated modules:** backend `domain/user`, `application/media` + `application/user`, `api/user`,
`infrastructure/persistence` + new storage adapter, 1 migration, ~4 test files. Android: `dto`,
`domain/repository`, new repository + API, `ProfileScreen.kt`, 1 new edit screen, DI container. **~18 files.**

---

## 3. Old visual theme — remaining screens

Method: import-level scan for `GlassBackButton`, `PremiumButton`, `HomeGlassSurface`,
`PremiumBackground`, `RojanLoadingState/EmptyState/ErrorState/ComingSoonState`, `HeroTitle`,
`HomeBackgroundTheme` across `app/src/main/java/ai/rojan/designlab/screens/**` (excluding
`/manager/**`, `/reception/**`). Doc-comment mentions of the old names in already-migrated screens
were excluded.

### 3.1 Migrated (no action)

`AuthScreen`, `ProfileScreen`, `AppointmentsScreen`, `AppointmentDetailsScreen`,
`RescheduleAppointmentScreen` → `CustomerScaffold` foundation (`screens/customer/components/`).
`BookingConfirmationScreen`, `BookingDateScreen`, `BookingTimeScreen`, `SpecialistSelectionScreen`,
`ServiceDetailsScreen`, `BookingSuccessScreen` → `BookingScaffold` foundation
(`screens/bookingflow/components/`). `CustomerHomeScreen`, `CustomerDashboardScreen`,
`SalonDetailsScreen` → screen-local Quiet-Luxury `Ref*` tokens on `HomeBackgroundTheme`.

> **Foundation fragmentation (tech-debt, P2):** there are now **three** parallel Quiet-Luxury
> implementations — `screens/customer/components/` (`CustomerScaffold`, `RefSurface`, `RefListRow`,
> `RefPrimaryButton`…), `screens/bookingflow/components/` (`BookingScaffold`, `RefSurface`,
> `RefPrimaryButton`, `RefSelectableCell`…), and per-screen private copies. `RefSurface` /
> `RefPrimaryButton` are defined 3×. Consolidate into one `screens/customer/components/` module
> after the migration below, or the debt compounds.

### 3.2 Live legacy screens — MIGRATE (priority order)

| # | Screen | Route | Legacy in use | Why this priority |
|---|---|---|---|---|
| 1 | **`SearchScreen.kt`** | `SEARCH` (bottom-bar tab + booking graph) | `GlassBackButton`, `HomeGlassSurface`×4, `RojanEmptyState`, `RojanErrorState`, `HomeBackgroundTheme` | Bottom-bar destination, hit on every session; visible glass/premium regression next to the redesigned Home |
| 2 | **`SalonListScreen.kt`** | `MEMBER_SALONS_LIST` + `SALON_LIST` | `GlassBackButton`×2, `HomeGlassSurface`×4, `RojanEmptyState`, `RojanErrorState`, `HomeBackgroundTheme` | The salon picker — **first step of the core booking funnel**; everything after it is already redesigned, so it stands out |
| 3 | **`SpecialistProfileScreen.kt`** | `SPECIALIST_PROFILE` (booking graph) | `GlassBackButton`×3, `HomeGlassSurface`×3, `RojanLoadingState`, `RojanErrorState`, `HomeBackgroundTheme` | Inside the booking funnel; reachable from salon detail + specialist selection |
| 4 | **`PublicSalonScreen.kt`** | `PUBLIC_SALON` (deep-link landing, unauthenticated) | `GlassBackButton`, `HomeGlassSurface`×4, `PremiumButton`, `RojanLoadingState`, `RojanErrorState`, `HomeBackgroundTheme` | The **first thing a shared-link visitor sees**; also the only `PremiumButton` left in a live customer screen |
| 5 | **`FavoritesScreen.kt`** | `FAVORITES` (bottom-bar tab + profile graph) | `GlassBackButton`, `HomeGlassSurface`×2, `RojanLoadingState`+`EmptyState`+`ErrorState`, `HomeBackgroundTheme` | Bottom-bar destination, real data, glass cards |
| 6 | **`FollowedSalonsScreen.kt`** | `FOLLOWED_SALONS` (profile graph) | same set as Favorites | Real data, reached from Profile |
| 7 | **`BeautyDnaScreen.kt`** | `BEAUTY_DNA` (profile graph) | `GlassBackButton`, `HomeGlassSurface`×2, `HomeBackgroundTheme` | Real-ish content screen from Profile |

### 3.3 Live legacy screens — "coming soon" placeholders (batch-migrate or cut)

All eight are `RojanComingSoonState` + `GlassBackButton` + `HomeBackgroundTheme`, reached from
`ProfileScreen`'s menu groups. Each is ~15 lines. **Product question:** should a v1 public release
surface these at all? "کیف پول / امتیازات وفاداری / عضویت / کدهای تخفیف" as empty "به‌زودی" screens
reads as an unfinished app.

`WalletScreen`, `CouponsScreen`, `MembershipScreen`, `LoyaltyScreen`, `MyReviewsScreen`,
`BeautyTimelineScreen`, `WaitlistScreen` (+ `BeautyDnaScreen` if it's also placeholder).

**Recommendation:** either (a) remove the menu entries from `ProfileScreen.kt`'s `activityItems` /
`salonItems` / `accountItems` for v1 and delete the screens, or (b) one shared
`ComingSoonScreen(title)` on `CustomerScaffold` + `CustomerEmptyState`, routed for all of them.

### 3.4 DEAD code — DELETE, don't migrate (P2 cleanup)

Not referenced by any `composable{}` or any live composable (verified: zero call sites):

- `screens/customer/CustomerScreenScaffold.kt` (superseded by `CustomerScaffold`)
- `screens/customer/` sections: `FeaturedSalons`, `PopularServices`, `TopSpecialists`,
  `PromotionsSection`, `NearbySalons`, `RecommendedSalons`, `FollowedSalons`, `UpcomingBookings`,
  `RecentVisits`, `HomeHeader`, `AISearchBar`, `SearchModeTabs`
- Their legacy deps (`HomeGlassSurface`, `RojanComingSoonState`, `HeroBookingCard`) can then be
  assessed for deletion too.

`CustomerDashboardScreen` (route `CUSTOMER_HOME`, the authenticated "home" tab) **is** live and
**is** already redesigned — leave it.

### 3.5 Migration approach

For each screen in 3.2: swap `HomeBackgroundTheme`+`GlassBackButton` shell → `CustomerScaffold(title, onBackClick)`;
`HomeGlassSurface` → `RefSurface`; `PremiumButton` → `RefPrimaryButton`;
`RojanLoadingState/EmptyState/ErrorState` → `CustomerLoadingState/EmptyState/ErrorState`; filled
violet icons → outlined neutral; single rose-gold accent. Preserve every ViewModel, callback, route,
and API call (the pattern the 5 already-migrated screens followed). Validate per screen:
`:app:compileCustomerDevDebugKotlin`, `:app:installCustomerDevDebug`, `:app:lintCustomerDevDebug`,
A72 spot-check.

**Estimated:** 7 screens (3.2) + 8 placeholders (3.3) + ~13 file deletions (3.4). ~1–1.5 days.

---

## 4. Performance

### 4.1 Findings

| Area | Finding | Evidence | Severity |
|---|---|---|---|
| **Baseline Profile** | **None.** No `androidx.profileinstaller`, no `:baselineprofile` module, no `baseline-prof.txt`. Cold start + first-scroll run without AOT hints. | `libs.versions.toml`, `settings.gradle.kts` | **P1** |
| **Crash / ANR telemetry** | **None.** No Crashlytics / Sentry / any. Zero production crash & ANR visibility — and this app ships via direct APK / Iranian stores, so there is **no Play Console vitals fallback either.** | grep — nothing found | **P0-adjacent** |
| **Launch screen** | `MainActivity` does not call `installSplashScreen()` (`androidx.core:core-splashscreen`). Activity theme is the full app theme → potential blank/branded flash before the in-app `SplashScreen` composable. `applyRojanEdgeToEdge()` runs *before* `super.onCreate()`. | `MainActivity.kt:16-18` | P2 |
| **Session-restore gating** | After the in-app splash, first route waits on: DataStore read → `GET /users/me` (+ transparent token refresh) → `GET /users/me/salon-access`, sequential. On a slow/offline network the user waits up to `SESSION_RESTORE_TIMEOUT_MS = 5_000` before failing safe to guest. `restoreSession` correctly does not roll back on `/salon-access` failure. | `SessionViewModel.kt`, `AuthViewModel.kt:331-339` | P1 |
| **Recomposition** | `RojanNavGraph` is one ~1300-line composable. `authViewModel.sessionState` is collected at the top (`RojanNavGraph.kt:170`) and `restoreState` too — a mid-session re-emit (every OTP login) recomposes the whole `NavHost` subtree. `startDestination` is correctly frozen with `remember{}` (bug fix comment at `:259-270`), but the collected state still churns the tree. | source | P1/P2 |
| **OkHttp instances** | **4 separate `OkHttpClient`s**: authenticated, `plainAuthApi` (refresh), `buildPlainRetrofit` (public), + Coil's own internal client. Each has its own connection + dispatcher thread pools. No shared base via `.newBuilder()`. | `BackendApiContainer.kt:262-302`, Coil default | P2 |
| **HTTP cache** | No OkHttp `Cache`. `GET /api/v1/salons`, salon detail, slots re-fetched on every screen entry with no conditional-GET / max-age reuse. | `BackendApiContainer.kt` | P2 |
| **Image loading** | Default Coil `ImageLoader` — no `ImageLoaderFactory` on an `Application` (there is no `Application` subclass). Memory cache = 25% RAM, disk = ~2% free (≤250 MB) by default. `RojanRemoteImage` sets `crossfade(true)` but **no `.size()`** hint → decodes at natural size for `ContentScale.Crop` targets. | `RojanRemoteImage.kt:52`, no `Application` class | P2 |
| **Memory** | Prior device run: `TOTAL PSS ≈ 161 MB` after 70 min heavy nav, no Activity leak (`Activities: 1`). High end — Coil bitmap caches dominate. | `CUSTOMER-RELEASE-READINESS-FINAL.md` | P2 |
| **`material-icons-extended`** | Full dependency pulled (`app/build.gradle.kts:57`). Thousands of vector assets; R8 tree-shakes unused ones in release, but it inflates the debug build and the dependency graph. | `build.gradle.kts` | P2 |
| **R8 / shrink** | ✅ `isMinifyEnabled = true`, `isShrinkResources = true` for release. Core-library desugaring on. Release APK was **4.3 MB** (dev flavor). Good. | `build.gradle.kts:197-221` | — |
| **Startup DI** | `BackendApiContainerHolder` builds Retrofit/OkHttp/DataStore lazily on first screen access, not at process start — good (no `Application.onCreate` cost) but the first authenticated screen pays a one-time construction hitch. | `BackendApiContainerHolder.kt` | P2 |

### 4.2 Optimization plan

**Do before external release:**

1. **Add a crash reporter.** Sentry (self-hostable, works without Google Play Services — right
   choice for Iran distribution) or Firebase Crashlytics if GMS is acceptable. Wire an
   `Application` subclass (needed anyway — see 3/5), init in `onCreate`, add
   `Thread.setDefaultUncaughtExceptionHandler` breadcrumbing. **This is the single highest-value
   perf/reliability item** — without it, issues 1 & 5 are invisible in the field.
2. **Baseline Profile.** Add the `:baselineprofile` module + `androidx.benchmark` macrobenchmark,
   generate against the real cold-start → Explore → salon detail → booking journey, ship
   `baseline-prof.txt` in the release AAB/APK. Typically 20–40% faster cold start and first-scroll.
3. **`installSplashScreen()`** in `MainActivity` with a minimal launch theme (brand bg + logo), so
   the window is branded from frame 0 and the in-app `SplashScreen` can be shortened or removed.

**Do during the theme migration (3):**

4. Introduce the `Application` subclass; make it `ImageLoaderFactory` with an explicit Coil
   `ImageLoader` — bounded `MemoryCache` (e.g. 20% RAM), `DiskCache` (fixed 100 MB), and share the
   app's base `OkHttpClient` (public/no-auth variant) so there is one connection pool for images +
   public calls.
5. Consolidate OkHttp: one base client, `.newBuilder()` for the authenticated variant
   (adds `AuthInterceptor` + `TokenAuthenticator`). Set explicit timeouts
   (connect 15s / read 30s / write 30s) tuned for mobile networks in Iran.
6. Add `RojanRemoteImage` `.size(...)` hints (or `Dimension` from layout) for the known slots
   (salon card thumb, avatar, specialist photo).

**Do opportunistically:**

7. Add an OkHttp `Cache` (10–20 MB) + honour `Cache-Control` on `GET /api/v1/salons` &
   salon-detail (needs a backend `Cache-Control` header too — small backend change).
8. Split `RojanNavGraph` — extract each nav-graph section (`profileGraph`, `bookingGraph`,
   `customerGraph`) into its own `NavGraphBuilder` extension file so recomposition scopes shrink and
   the file becomes reviewable.
9. Consider `material-icons-core` + a hand-picked icon set instead of `-extended`.

**Estimated files:** new `RojanApplication.kt` + manifest entry, `BackendApiContainer.kt`,
`RojanRemoteImage.kt`, `MainActivity.kt`, new `:baselineprofile` module, `build.gradle.kts` (deps),
crash-reporter init. **~8 files + 1 module.**

---

## 5. Update system

### 5.1 Current state

**There is no update mechanism of any kind.** No `com.google.android.play:app-update`, no custom
version check, no `/app-version` or `/config` call on startup, no "update available" UI.
`BuildConfig.VERSION_CODE = 1` / `VERSION_NAME = "1.0.0"` are available but read nowhere.

Because the distribution channel is **direct APK + Iranian stores (CafeBazaar/Myket)** and **not
Google Play**, Play In-App Updates (`AppUpdateManager`) is **not** a viable primary mechanism —
CafeBazaar has its own update API, Myket likewise, and direct-APK users get nothing. A
**backend-driven check** is the only channel that covers all three.

### 5.2 Recommended design — backend-driven, store-agnostic

**Backend:** `GET /api/v1/app/version?platform=android&flavor=customer` (public, no auth), returns:

```json
{
  "latestVersionCode": 7,
  "latestVersionName": "1.3.0",
  "minSupportedVersionCode": 4,
  "updateUrl": "https://rojanai.ir/download/customer",
  "releaseNotes": "…",           // markdown or plain, localized (fa)
  "forceUpdate": false            // optional server override independent of minSupported
}
```

Source of truth options: (a) a small `app_release` table the ops team edits; (b) mirror
`ROJAN_Web`'s `lib/downloads/release-registry.ts` (it *already* has `version`,
`minimumSupportedVersion`, `checksum`, `releaseNotes` fields — currently null for `customer`) and
serve it from a Next.js route the app calls. Option (b) keeps one registry.

**Android flow:**

1. `Application` / first post-splash composable → `AppVersionRepository.check()` (fire-and-forget,
   cached ~6 h in DataStore, never blocks UI, fails silent).
2. Compare `BuildConfig.VERSION_CODE`:
   - `< minSupportedVersionCode` **or** `forceUpdate == true` → **blocking** full-screen
     `ForceUpdateScreen` on `CustomerScaffold` (no back, no dismiss, `BackHandler {}` consumed):
     headline "برای ادامه، برنامه را به‌روزرسانی کنید", `RefPrimaryButton("به‌روزرسانی")` →
     `Intent(ACTION_VIEW, updateUrl)`. App is unusable underneath.
   - `minSupported ≤ code < latestVersionCode` → **dismissible** `SoftUpdateDialog`
     (`CustomerConfirmDialog` shape): "نسخه جدیدی موجود است" + release notes, buttons
     "به‌روزرسانی" / "بعداً". Show at most once per `SOFT_UPDATE_SNOOZE` (e.g. 72 h) — persist last-shown
     timestamp + last-seen `latestVersionCode` in DataStore so a newer release re-prompts immediately.
   - `code >= latestVersionCode` → nothing.
3. Store builds (CafeBazaar/Myket) can additionally call their SDK's native updater; the
   `updateUrl` should deep-link to the correct store per build (a `BuildConfig` field per flavor
   dimension, or resolved from `installerPackageName`).

**API requirements:** one public GET endpoint; must be reachable even when the user's token is
expired (hence public); must be versioned defensively (unknown fields ignored on the client).

**Estimated files:** Backend — 1 controller + 1 DTO + (table + migration or a Web route) + 1 test.
Android — `AppVersionApi`, `AppVersionRepository`, `AppUpdateViewModel`, `ForceUpdateScreen.kt`,
`SoftUpdateDialog.kt`, hook in `RojanNavGraph`/`Application`, `BuildConfig` field in
`build.gradle.kts`, DataStore keys. **~10 files.**

---

## 6. Website APK download

### 6.1 Current state — infrastructure is built, Customer data is empty

`ROJAN_Web/apps/website` already has a complete, well-factored download system:

| Piece | State |
|---|---|
| `/no-tenant/download` page (`app-showcase-hero`, `app-carousel-3d`, `download-markets-section`) | Live |
| `/downloads/[appId]` route (`app/downloads/[appId]/route.ts`) | Live — stable URL, resolves the real versioned filename server-side, `307` redirect, `404`s if the file is genuinely absent |
| `/no-tenant/download/[appId]` per-app detail page | Live |
| `lib/downloads/release-registry.ts` | Schema complete: `fileName, version, releaseDate, releaseNotes, minimumSupportedVersion, checksum (SHA-256), architecture` |
| `lib/constants/app-showcase.ts` | Editorial: `available, downloadHref, version, releaseDate, fileSizeLabel` |
| **Reception** | ✅ Fully shipped — `rojan-reception-v1.0.0-win-x64-setup.exe`, real SHA-256, committed under `public/downloads/reception/` |
| **Customer** | ⛔ `RELEASE_REGISTRY.customer = { fileName: null, … all null }`; `APP_SHOWCASE_ENTRIES` customer `available: false`, no `downloadHref`. The route `404`s; the page shows "coming soon". |
| `download-markets-section.tsx` | 4 channels (کافه بازار / مایکت / Google Play / دانلود مستقیم) — **all `href: null`**, all disabled |

So issue 6 is **not** "build the download page" — it is **populate the Customer release + wire the
not-yet-rendered fields (changelog, checksum)**.

### 6.2 Required changes

1. **Build** the signed `customerProductionRelease` APK (P0-1 + P0-2 — done; produce the artifact).
   Decide APK vs. AAB — for direct download it must be an **APK** (AAB is Play-only).
2. **Host the file.** Reception's `.exe` is committed straight into the website repo (no LFS).
   A 15–40 MB Customer APK in a Next.js repo bloats every clone and deploy. **Recommendation:**
   publish via a GitHub Release (as Reception's registry comment already describes doing) or
   object storage, and either (a) commit only into `public/downloads/customer/` at deploy time via
   CI, or (b) change `route.ts` to redirect to the external URL. Pick one now — it affects the
   registry shape.
3. **`release-registry.ts`** — set `RELEASE_REGISTRY.customer`:
   `fileName`, `version: "1.0.0"`, `releaseDate`, `checksum` (real `sha256sum` of the shipped APK),
   `architecture` (`"universal"` or per-ABI), `releaseNotes` (first changelog), `minimumSupportedVersion`
   (ties into issue 5).
4. **`app-showcase.ts`** — customer entry: `available: true`, `downloadHref: "/downloads/customer"`,
   `version`, `releaseDate`, `fileSizeLabel`.
5. **`download-markets-section.tsx`** — set the "دانلود مستقیم (APK)" channel `href: "/downloads/customer"`.
   Leave CafeBazaar/Myket/Play null until those listings exist.
6. **Changelog UI** — `releaseNotes` and `checksum` are in the schema but rendered nowhere.
   Wire them into the `/download/[appId]` detail page: a version history section + a copy-able
   SHA-256 with a one-line "چطور صحت فایل را بررسی کنم" explainer (important for a sideloaded APK —
   users must be able to verify integrity).
7. **`sitemap.ts`** — confirm `/download` + `/download/customer` are listed (they likely are via
   `ROOT_STATIC_PATHS` / dynamic).
8. **Install guidance** — a short "نصب از منبع نامشخص" (enable unknown sources) note on the customer
   download page; first-time sideloaders will be blocked by Android otherwise.
9. **Verify** post-deploy: `curl -I https://rojanai.ir/downloads/customer` → `307` → APK;
   `sha256sum` of the downloaded file matches the registry; `/download` shows the card as available;
   `typecheck` + `lint` + the `route.test.ts` / `app-card.test.tsx` suites pass.

### 6.3 Coordination with issue 5

The website registry's `version` + `minimumSupportedVersion` + `updateUrl` (= `/downloads/customer`)
should be the **same data** the app's version-check endpoint serves (option 5.2b). Design 5 and 6
together so there is one release manifest, not two that drift.

**Estimated files:** `release-registry.ts`, `app-showcase.ts`, `download-markets-section.tsx`,
`app/no-tenant/download/[appId]/page.tsx` (changelog + checksum UI), maybe `route.ts` (if external
host), 1 install-note component, test updates. **~7 files** + the APK artifact + CI/host wiring.

---

## 7. Consolidated priority & sequencing

| Priority | Item | Repo(s) | Rough size | Blocks release? |
|---|---|---|---|---|
| **P0** | **1** — guest/logout discovery 401. Backend public salon list + Android repoint (or min: Explore 401 handling) | Backend + Android | 4–6 files | **Yes** |
| **P0-adjacent** | **4a** — add a crash/ANR reporter (Sentry) + `Application` subclass | Android | ~4 files | Should — no field visibility otherwise |
| **P1** | **5** — backend-driven soft/force update | Backend + Android | ~10 files | Should — no recovery path otherwise |
| **P1** | **6** — populate Customer APK on the website + changelog/checksum UI | Web (+ artifact) | ~7 files | Yes for distribution |
| **P1** | **3.2** — migrate 4 live legacy screens (Search, SalonList, SpecialistProfile, PublicSalon) | Android | 4 screens | No, but visible regression |
| **P1** | **4b** — Baseline Profile + `installSplashScreen` | Android | 1 module + 2 files | No |
| **P2** | **3.3** — placeholder screens: cut or unify (product decision) | Android | 8 screens | No |
| **P2** | **3.1 / 3.4** — foundation consolidation + delete ~13 dead files | Android | cleanup | No |
| **P2** | **4c** — OkHttp consolidation, Coil `ImageLoader`, HTTP cache, NavGraph split | Android + small backend | ~6 files | No |
| **P2** | **1.3** — server-side logout / refresh-token revocation | Backend + Android | ~3 files | No |
| **P2** | **2** — profile avatar (net-new; needs storage decision first) | Backend + Android | ~18 files | No |

### Recommended implementation order

1. **Issue 1** (backend public list + Android). Unblocks first-launch and logout. Hard blocker.
2. **Issue 4a** — Sentry + `Application` class. Do before any external APK so 1/5 regressions are visible.
3. **Issue 5** — update system, sharing the release manifest with issue 6.
4. **Issue 6** — website Customer APK + changelog/checksum, using the P0-1/P0-2 signed build.
5. **Issue 3.2** — the 4 live legacy screens (then 3.3 product call, then 3.4 cleanup).
6. **Issue 4b/4c** — Baseline Profile, splash, OkHttp/Coil/cache, NavGraph split.
7. **Issue 2** — avatar, after the object-storage decision that also affects salon media.

---

## 8. Files & modules touched (aggregate estimate)

| Issue | Backend | Android | Web | New modules |
|---|---|---|---|---|
| 1 | ~4 | ~6 | — | — |
| 2 | ~10 | ~8 | — | S3 storage adapter |
| 3 | — | ~15 screens + ~13 deletions | — | (consolidate 3 foundations → 1) |
| 4 | ~1 (Cache-Control) | ~8 | — | `RojanApplication`, `:baselineprofile` |
| 5 | ~4 | ~10 | shares 6's route | — |
| 6 | — | — | ~7 + artifact | CI/host wiring for the APK |

Nothing in this audit was implemented. No commits made.
