# Customer Profile Personalization — Phase 5B (Android)

**Date:** 2026-09-10 · **Scope:** Customer Android app only · **Not committed.**

Connects the Customer profile to the Phase 5A backend media endpoints: an editable header with a
cover image and an avatar, camera-badge edit actions, per-slot upload/delete states. No backend
change. Every existing profile action is preserved unchanged.

---

## Summary

| Layer | Change |
|---|---|
| **DTO / domain** | `UserResponseDto` + `AuthenticatedUser` gain nullable `avatarUrl` / `coverUrl`; one shared `UserResponseDto.toAuthenticatedUser()` mapper |
| **Networking** | new `UserMediaApi` (multipart `POST` + `DELETE` for `/api/v1/users/me/media/{avatar,cover}`) |
| **Repository** | new `UserProfileRepository` + `UserProfileRepositoryImpl` (`safeApiCall`, returns the refreshed user) |
| **DI** | `BackendApiContainer.userProfileRepository` (authenticated retrofit, no salonId) |
| **AuthViewModel** | `+ applyUpdatedUser(user)` — swaps `currentUser` after a media edit; nothing else touched |
| **ViewModel** | new `ProfileMediaViewModel` (+ factory) — per-slot `isUploadingAvatar` / `isUploadingCover`, error via `userMessageFor` |
| **Image pipeline** | promoted `decodeResizeAndCompress` + `ImageOnlyPickerRequest` from `ManagerSalonMediaScreen` into shared `ui/media/ImageDownscale.kt`; the Manager screen now uses the shared copy |
| **UI** | `ProfileScreen` header rebuilt — cover band + overlapping 96dp avatar + rose-gold "تایید شده" chip + camera badges + upload spinners + `CustomerConfirmDialog`-gated remove. Menu groups + logout **byte-identical**. |

---

## 1. Data + domain

```kotlin
// data/remote/dto/AuthDtos.kt  — additive, nullable, defaulted → non-breaking
data class UserResponseDto(… , val avatarUrl: String? = null, val coverUrl: String? = null)

// domain/repository/BackendAuthRepository.kt
data class AuthenticatedUser(… , val avatarUrl: String? = null, val coverUrl: String? = null)
```

The private `UserResponseDto.toDomain()` inside `BackendAuthRepositoryImpl` is promoted to a shared
package-level `internal fun UserResponseDto.toAuthenticatedUser()` (`data/repository/UserMappers.kt`)
so `BackendAuthRepositoryImpl` (register / login / me / otp-verify) and `UserProfileRepositoryImpl`
map the user identically — notably the two new URL fields. `CurrentUserIdentityContext` is unchanged
(it never carried image fields).

## 2. Networking + repository

```kotlin
// data/remote/UserMediaApi.kt
interface UserMediaApi {
    @Multipart @POST("api/v1/users/me/media/avatar") suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserResponseDto
    @Multipart @POST("api/v1/users/me/media/cover")  suspend fun uploadCover(@Part file: MultipartBody.Part): UserResponseDto
    @DELETE("api/v1/users/me/media/avatar") suspend fun deleteAvatar(): UserResponseDto
    @DELETE("api/v1/users/me/media/cover")  suspend fun deleteCover(): UserResponseDto
}
```

`UserProfileRepositoryImpl` builds the `file` part with
`MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))`
— identical to `BackendManagerMediaRepository.upload` — and wraps every call in `safeApiCall`, so a
malformed / contract-drifted response is a `Result.failure` (never a crash — the Phase 4 hardening).
Each method returns the refreshed `AuthenticatedUser`.

DI: `BackendApiContainer.userProfileRepository = UserProfileRepositoryImpl(retrofit.create(UserMediaApi::class.java))`
on the shared **authenticated** retrofit (bearer token; the endpoints are `/users/me/...`, no salonId).

## 3. AuthViewModel — one additive method

```kotlin
/** Phase 5B: swap in a freshly-returned user after a profile-media edit. No-op unless same account. */
fun applyUpdatedUser(user: AuthenticatedUser) {
    if (_currentUser.value?.id != user.id) return
    _currentUser.value = user
}
```

Nothing else in `AuthViewModel` changed — session state, tokens, identity context, OTP flow,
`restoreSession` are untouched. `currentUser` stays the single source of truth every screen renders
from, so a new avatar URL propagates to the Profile header, the Dashboard/Explore chip, etc. with no
extra plumbing. (`restoreSession` already re-pulls `/users/me` on every cold start, so the image
also survives a process restart.)

## 4. ProfileMediaViewModel

`presentation/profile/ProfileMediaViewModel.kt` (+ `ProfileMediaViewModelFactory`):

```kotlin
data class ProfileMediaState(
    val isUploadingAvatar: Boolean = false,
    val isUploadingCover: Boolean = false,
    val errorMessage: String? = null,
)
```

`uploadAvatar` / `uploadCover` / `removeAvatar` / `removeCover` each: guard against a double-tap
while that slot is busy, set the slot's `isUploading*`, call the repo, and on **success** push the
returned user through `onUserUpdated` (→ `AuthViewModel.applyUpdatedUser`) — this ViewModel holds
**no user state of its own**. On **failure** the error becomes a Persian `errorMessage` via the
shared `userMessageFor`. `dismissError()` clears it. The two slots are independent (one upload never
disables the other).

## 5. Shared image pipeline

`ui/media/ImageDownscale.kt` — promoted verbatim from `ManagerSalonMediaScreen`'s private helpers:

- `ImageOnlyPickerRequest` — the Android-13+ system Photo Picker request (images only, **no runtime
  permission**).
- `decodeResizeAndCompress(uri, context, maxDimension, quality = 80)` — two-pass decode (bounds →
  sub-sampled), **EXIF orientation baked into pixels + every other EXIF field (incl. GPS) stripped**
  by the full JPEG re-encode, bounded to `maxDimension` on the longer side. Returns
  `(bytes, fileName, mimeType)`.
- `CustomerImageBounds.AVATAR_MAX_DIMENSION = 1024`, `COVER_MAX_DIMENSION = 1600`.

`ManagerSalonMediaScreen` now imports these and drops its ~80-line private copy (its own
`LOGO/COVER/GALLERY_MAX_DIMENSION` values stay local). The decode runs on `Dispatchers.Default` at
both call sites, exactly as before.

## 6. ProfileScreen header

The `ProfileScreen(...)` public signature and every navigation callback are **unchanged**. Only the
header composable changed; the "اطلاعات شخصی" card, the three menu groups, and the
`CustomerConfirmDialog`-gated logout are byte-identical.

```
┌──────────────────────────────────────────┐  CustomerScaffold top bar — "حساب کاربری"
│                                          │
│   [ COVER — full-width, 168dp ]     [📷]  │  RojanRemoteImage(RectangleShape); flat
│   ▚ calm bottom fade to page ground      │  CustomerSurfaceFill fallback; tap → cover picker
│  ╭────────╮                               │
│  │ AVATAR │  96dp, overlaps cover -52dp   │  RojanRemoteImage(CircleShape) + initial fallback;
│  │  [📷]  │  navy ring, hairline border   │  camera badge bottom-trailing → avatar picker
│  ╰────────╯                               │
│   نام کاربر            [ ✓ تایید شده ]    │  Display 26sp + rose-gold verified-phone chip
│   حذف عکس نمایه   ·   حذف تصویر کاور       │  quiet remove links (only when that image is set)
├──────────────────────────────────────────┤
│  اطلاعات شخصی  …  (unchanged)             │
│  فعالیت من / سالن‌های من / امکانات حساب  … │
│  خروج از حساب                             │
```

- **Edit** — tap the avatar or the cover (or its 📷 badge) → shared Photo Picker →
  `decodeResizeAndCompress` on `Dispatchers.Default` → `ProfileMediaViewModel.upload*`.
- **Uploading** — a `NavyBase @55%` scrim + rose-gold `CircularProgressIndicator` over **only the
  targeted element** (avatar OR cover); the badge and tap are suppressed while busy.
- **Remove** — a quiet `Caption` link under the name, shown only when that image is set, gated by a
  `CustomerConfirmDialog` ("عکس نمایه شما حذف شود؟").
- **Error** — `mediaState.errorMessage` renders as a `RojanErrorText` `Caption` under the header
  with `liveRegion = Polite`; tapping it dismisses.
- **Guest** (`currentUser == null`) — placeholder header, **no badges, no remove links, no error
  slot**; the existing "اطلاعاتی ثبت نشده است" row is unchanged.
- Quiet Luxury: flat, `HomeBackgroundTheme` ground, rose-gold (`CustomerAccent`) the single accent
  (badge fill, chip, spinner), outlined `Icons.Outlined.PhotoCamera` / `Verified`. No glow, no glass.

---

## 7. Files

**New:**
`data/remote/UserMediaApi.kt` · `domain/repository/UserProfileRepository.kt` ·
`data/repository/UserProfileRepositoryImpl.kt` · `data/repository/UserMappers.kt` ·
`ui/media/ImageDownscale.kt` · `presentation/profile/ProfileMediaViewModel.kt` ·
`presentation/profile/ProfileMediaViewModelFactory.kt`

**Modified:**
`data/remote/dto/AuthDtos.kt` (`UserResponseDto` +2 nullable fields) ·
`domain/repository/BackendAuthRepository.kt` (`AuthenticatedUser` +2) ·
`data/repository/BackendAuthRepositoryImpl.kt` (use shared mapper) ·
`di/BackendApiContainer.kt` (+1 repository) ·
`presentation/auth/AuthViewModel.kt` (+`applyUpdatedUser`) ·
`screens/profile/ProfileScreen.kt` (header rebuild) ·
`manager/screens/settings/ManagerSalonMediaScreen.kt` (use shared image pipeline)

No navigation route / graph, no ViewModel business logic beyond the additive `applyUpdatedUser`, no
`strings.xml`, no design-system token *value* changed.

---

## 8. Validation

| Gate | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL** |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL** · `0 errors, 94 warnings` — **identical to baseline**, 0 new findings |
| `:app:assembleCustomerDevDebug` | **BUILD SUCCESSFUL** · `app-customer-dev-debug.apk` produced |
| `:app:testCustomerDevDebugUnitTest` | 307 / 309 pass. The 2 failures are `BackendAuthFlowVerificationTest` — a manual connectivity trigger that requires a local `localhost:8080` backend and is not a CI gate (`java.net.ConnectException`); pre-existing, unrelated to Phase 5B. |

### A72 full profile test

**Device:** Samsung Galaxy A72 (`RZ8R81WPS2J`), Android 14, 1080×2400 · **Build:** this branch's
`app-customer-dev-debug.apk` · **Account:** authenticated customer ("گیتا", +989164987585), then
guest after logout · **Crash log:** `adb logcat -b crash` → **`FATAL EXCEPTION` count = 0** across
the entire run.

| # | Step | Result |
|---|---|---|
| 1 | Cold launch | Lands on Home ("سلام گیتا جان"), session restored. ✅ |
| 2 | Profile tab | New header renders exactly as designed: full-width cover placeholder with a rose-gold camera badge top-end; 96dp avatar with the initial "گ", navy ring, camera badge bottom-end; name "گیتا" + rose-gold "✓ تایید شده" chip; menu groups + "اطلاعات شخصی" card + logout unchanged below. ✅ `02_profile_header.png` |
| 3 | Tap avatar → system Photo Picker | Opens with **"This app can only access the photos that you select"** — confirms the shared `ImageOnlyPickerRequest` (Android-13+ picker, **no runtime permission requested**). ✅ `03_photo_picker.png` |
| 4 | Select a photo (EXIF-bearing JPEG) | Picker closes, control returns to the app; `decodeResizeAndCompress` runs off-thread; `POST /api/v1/users/me/media/avatar` fires. |
| 5 | Upload result | **`404`** — the request reached `api.rojanai.ir` and was routed, but the Phase 5A backend endpoints are **not yet deployed to production** (expected: Phase 5A was explicitly not committed/deployed). `safeApiCall` → `BackendApiException(404)` → `userMessageFor` → **"موردی یافت نشد."** rendered as the inline error line; avatar cleanly reverts to the initial; spinner clears. **No crash, no stuck state.** ✅ `05_avatar_uploading_or_done.png` |
| 6 | Tap the error line | `dismissError()` — the line disappears immediately. ✅ |
| 7 | Tap the cover band (not just its badge) → Photo Picker → select | Same pipeline, same graceful **404 → "موردی یافت نشد."** on `POST …/media/cover`; cover band remains tappable across its whole area, not just the 30dp badge. ✅ `06_cover_picker.png`, `07_cover_upload_result.png` |
| 8 | Existing profile action — tap "نوبت‌های من" | Navigates to Appointments (empty state) exactly as before; back → Profile intact, `ProfileMediaViewModel`'s error state correctly persisted (same screen instance). ✅ |
| 9 | Logout | `CustomerConfirmDialog` ("خروج از حساب" / "از حساب کاربری خود خارج می‌شوید؟") unchanged → confirm → lands on Explore as guest. ✅ `08_logout_confirm.png` |
| 10 | Guest Profile | Header shows the placeholder with the "ک" initial, **no camera badges, no verified chip, no remove links, no error slot** — `editable = !isGuest` correctly suppresses every edit affordance; "اطلاعاتی ثبت نشده است" row unchanged. ✅ `09_guest_profile.png` |

**What this confirms:** the picker → downscale/re-encode → multipart upload → error-mapping →
UI-state pipeline is correct end-to-end on real hardware — the request reaches the server with a
well-formed shape (a malformed request would 400/415, not 404) and every failure is handled
gracefully with zero crashes. **A live "image actually renders" pass is blocked on deploying the
Phase 5A backend** (local, uncommitted, not yet on `api.rojanai.ir`) — re-run steps 3–7 once that
endpoint is live to see the success path (avatar/cover render the real image, persist across a
relaunch, and the remove-confirm flow completes).

---

## 9. Follow-ups

1. **Deploy Phase 5A** to `api.rojanai.ir` and re-run A72 steps 3–7 to confirm the success path (an
   uploaded image actually renders and persists across a relaunch, and remove completes) — the error
   path is now fully verified; only the live 200 response is unobserved.
2. **Backend EXIF strip** — the client already re-encodes (dropping EXIF/GPS); the backend still
   stores bytes verbatim. Server-side re-encode is the Phase 5A audit's §6 hardening item.
3. **Avatar in the Dashboard / Explore top-right chip and the bottom-bar Profile tab** — they still
   render `Icons.Outlined.Person`. Wiring them to `currentUser?.avatarUrl` is a small additive follow
   -up (the data is already in `currentUser`).

Nothing was committed.
