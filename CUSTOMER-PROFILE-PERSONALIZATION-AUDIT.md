# Customer Profile Personalization — Audit

**Date:** 2026-09-10 · **Scope:** Android Customer Profile + Backend User system ·
**Audit only — no code changed, nothing committed.**

Goal: what it takes to ship (1) user avatar upload, (2) user profile cover image,
(3) a redesigned profile header — reusing the media infrastructure that already exists for salons.

---

## TL;DR

- **A complete, production media pipeline already exists** — direct multipart upload, `MediaStoragePort`
  (local disk today, S3-ready), nginx `/media/` static serving, a Coil `RojanRemoteImage` seam, and a
  battle-tested Android pick→resize→compress→upload flow. **All of it is salon-scoped and
  owner-permission-gated.**
- **Nothing about a *user* image exists** — not a column, not an enum case, not an endpoint, not a
  DTO field, not a ViewModel. There is also **no user-profile write endpoint at all** (`User.rename()`
  exists on the aggregate but no API calls it).
- The media schema was *explicitly designed* to grow a non-salon owner (`MediaAsset` doc: *"[ownerId]
  is polymorphic by design … includes specialist and service images … Add a case here only when that
  phase actually starts"*). **This is that phase, for `USER`.**
- Recommended path: **extend the polymorphic media system to a `USER` owner** (Option A). A lighter
  "URL columns on `users`" path (Option B) is viable if user images will forever be "one avatar + one
  cover."
- **Backend is the critical path** (schema + use cases + endpoint + deploy). Android is
  straightforward once the contract exists.
- **Security gaps to close regardless of option:** the backend trusts the client's `Content-Type`
  (no content sniff), never re-encodes, and **never strips EXIF/GPS**. For user-uploaded photos that
  is a privacy defect, not a nice-to-have.

---

## 1. Current state

### 1.1 Android — `ProfileScreen`

`app/src/main/java/ai/rojan/designlab/screens/profile/ProfileScreen.kt` (316 lines), migrated to
Quiet Luxury (`CustomerScaffold`, flat `RefSurface` / `RefListRow`, single rose-gold accent, no glow).

| Aspect | Current |
|---|---|
| **Header** | `IdentityHeader(name)` — the display name (`Display` 26sp) beside a **60dp circle** containing the name's first letter, or an outlined `Person` icon. `CustomerSurfaceFill` + 1px `CustomerHairline`. **No image. No cover. No edit affordance.** |
| **Data source** | `authViewModel.currentUser` (`AuthenticatedUser`) + `authViewModel.currentDisplayName`. Nothing else. |
| **Personal-info card** | Shows only backend-real fields: phone (+ "تایید شده" badge), email. The file's own doc comment: *"birthday/city/photo are intentionally not shown — no such fields exist on the backend `User` model and no update-profile endpoint exists yet."* |
| **State ownership** | None. `ProfileScreen` is stateless over `AuthViewModel`; the only local state is `showLogoutConfirm`. No `ProfileViewModel`. |
| **Guest** | Reachable. Renders `"کاربر"` + the `Person` placeholder + `"اطلاعاتی ثبت نشده است"`. |

The same 60dp `Person` placeholder also appears in:
- `CustomerDashboardScreen.HomeGreetingRow` (top-right tap-target, `content-desc="پروفایل"`)
- `CustomerHomeScreen` (guest Explore, top-right chip)
- `CustomerBottomBar` PROFILE tab (`Icons.Outlined.Person`)

### 1.2 Android — user model

```kotlin
// domain/repository/BackendAuthRepository.kt
data class AuthenticatedUser(
    val id: String, val email: String?, val phoneNumber: String? = null,
    val fullName: String, val role: String,
)   // ← no avatarUrl, no coverUrl, no bio

// data/remote/dto/AuthDtos.kt
data class UserResponseDto(
    val id: String, val email: String? = null, val phoneNumber: String? = null,
    val fullName: String, val role: NetworkUserRole,
)
```

- `AuthViewModel._currentUser: MutableStateFlow<AuthenticatedUser?>` is **in-memory only**. The auth
  DataStore persists **just `personId`**. On every cold start `restoreSession()` re-pulls
  `GET /api/v1/users/me` → `onAuthenticated(user)` sets `_currentUser`.
- `_currentUser` is written in exactly two places: OTP-verify success and `restoreSession`. **There
  is no "refresh the current user" method** — `AuthViewModel` has `refreshIdentityContext()` but that
  only re-pulls *salon access*, not `/users/me`, and never touches `_currentUser`.
- `CurrentUserIdentityContext` (`domain`) bundles `/users/me` + `/users/me/salon-access` — also
  carries no image fields.

### 1.3 Android — APIs (`AuthApi`)

`register`, `login`, `refresh`, `GET /api/v1/users/me`, `GET /api/v1/users/me/salon-access`,
`POST /api/v1/auth/otp/request`, `POST /api/v1/auth/otp/verify`.
**No profile-write endpoint is bound. No customer media endpoint is bound.**

### 1.4 Android — media / upload infrastructure (exists, **manager-only**)

| Component | What it does | Reusable for a user? |
|---|---|---|
| `data/remote/ManagerMediaApi.kt` | `@Multipart POST /api/v1/salons/{salonId}/media` (`file` + `mediaType` parts), `GET`, `DELETE` | ❌ salon path |
| `manager/data/BackendManagerMediaRepository.kt` | `upload(salonId, mediaType, bytes, name, mime)` → `MultipartBody.Part.createFormData(...)` + `safeApiCall`, maps `MediaAssetResponseDto` → domain | pattern ✅, signature ❌ |
| `manager/presentation/settings/ManagerSalonMediaViewModel.kt` | compound **pick → `upload` → `assignIdentity`**; separate `isUploadingLogo` / `isUploadingCover` flags; optimistic gallery; `userMessageFor(...)` errors | pattern ✅ |
| `manager/screens/settings/ManagerSalonMediaScreen.kt` | `ActivityResultContracts.PickVisualMedia` (Android-13+ Photo Picker — **no runtime permission**); `decodeResizeAndCompress(uri, ctx, maxDimension, quality=80)` — two-pass decode (`inJustDecodeBounds` → sub-sampled decode), **EXIF rotation baked into pixels**, JPEG re-encode. `LOGO_MAX=1024`, `COVER_MAX=1600`, `GALLERY_MAX=2048` | **directly reusable — needs promoting out of this file** |
| `ui/components/image/RojanRemoteImage.kt` | the one Coil `AsyncImage` seam — `url` null/blank/failed → `fallback()`, `crossfade`, `ContentScale.Crop`, `clip(shape)` | ✅ (small/square/circular today; a cover needs a wide variant) |
| `ui/components/image/SpecialistAvatar.kt` | circular `RojanRemoteImage` + `Person` icon fallback + fade-in | ✅ direct model for the avatar |

**Everything media-related takes `salonId` and is gated on `Permission.MANAGE_SALON`.** There is no
customer-facing media repository, API, or DI wiring.

### 1.5 Backend — user system

```kotlin
// domain/user/User.kt  — aggregate root
class User private constructor(
    val id: UserId, email: Email?, passwordHash: String?, phoneNumber: PhoneNumber?,
    fullName: String, val role: UserRole, active: Boolean, createdAt, updatedAt,
)
// mutators: rename(newFullName), deactivate(), changePasswordHash(newHash)
// factories: register(), registerWithPhone(), reconstitute()
```

- `rename()` exists **but nothing calls it** — grep for `Update*User` / `UpdateProfile` /
  `EditProfile` use cases returns **only `User.kt` itself**.
- `users` table (V1 + V5): `id, email?, password_hash?, phone_number?, full_name, role, active,
  created_at, updated_at`. **No image columns.** CHECK constraint: `email IS NOT NULL OR phone_number
  IS NOT NULL`.
- `UserController` (`/api/v1/users`): `GET /me` → `UserResponse(id, email?, phoneNumber?, fullName,
  role)`, `GET /me/salon-access`. **No `PATCH` / `PUT /me`.**
- `UserRepository`: `save`, `findById`, `findByEmail`, `existsByEmail`, `findByPhoneNumber`,
  `existsByPhoneNumber`.
- `SecurityConfig`: `/api/v1/auth/**` + `/api/v1/public/**` permitAll, everything else
  `authenticated()`. A new `/api/v1/users/me/**` route is authenticated by default with no config
  change.

### 1.6 Backend — media system

```kotlin
// domain/media/MediaAsset.kt
enum class MediaOwnerType { SALON }                       // ← only SALON
enum class MediaType { LOGO, COVER, GALLERY, PORTFOLIO }
class MediaAsset(
    val id, val salonId: SalonId,                         // ← tenant boundary, NOT NULL in DB
    val ownerType: MediaOwnerType, val ownerId: UUID,     // ← "polymorphic by design"
    val mediaType, storageKey, fileName, mimeType, fileSize, url, timestamps,
)
```

| Piece | Detail |
|---|---|
| `media_assets` table (V16) | `salon_id UUID **NOT NULL** REFERENCES salons(id)`; indexes on `salon_id`, `(salon_id, media_type)`, `(owner_type, owner_id)` |
| `salons.logo_media_id` / `cover_media_id` (V17) | FK "identity slot" refs; `salons.logo_url` (V11) kept as legacy fallback |
| `MediaController` | `@RequestMapping("/api/v1/salons/{salonId}/media")` — `POST` multipart (`mediaType` param), `GET` list, `DELETE`. **All gated `Permission.MANAGE_SALON`, all salon-path-scoped.** |
| `UploadMediaUseCase` | **hard-requires a salon**: `salonRepository.findById(cmd.salonId) ?: throw SalonNotFoundException`; `salonPermissionResolver.require(salon.id, callerId, MANAGE_SALON)`. `storageKey = "salon/{salonId}/{randomUUID}.{ext}"` |
| `AssignSalonIdentityMediaUseCase` | slot assignment with `requireBelongsToSalon(mediaId, salonId)` tenant check |
| `MediaStoragePort` | `store(key, bytes, mime) -> url`, `delete(key)`. Impl `LocalDiskMediaStorageAdapter` writes to `rojan.media.storage-root` (`/app/uploads`), returns `{rojan.public.base-url}/media/{key}`, path-traversal guarded. Doc: *"Swappable for an S3/object-storage adapter later without any application/domain change."* |
| `MediaProperties` | `maxFileSizeBytes = 5 MB`, `allowedMimeTypes = [image/jpeg, image/png, image/webp]` |
| Spring multipart | `max-file-size: 10MB`, `max-request-size: 10MB` |
| nginx | `location /media/ { alias /var/www/uploads/; add_header X-Content-Type-Options "nosniff" always; }` — **public, unauthenticated**; `client_max_body_size 12m` |
| **Content validation** | `mimeType = file.contentType ?: "application/octet-stream"` — **trusts the client's declared type, no magic-byte sniff**; **no server-side re-encode; EXIF is never stripped** |

### 1.7 Storage strategy

- **Local disk** today: `${ROJAN_DATA_ROOT:-/opt/rojan}/uploads` bind-mounted `rw` into the app
  container, `ro` into nginx, served static at `/media/`.
- **Direct multipart** upload (not signed-URL two-phase — deliberate, see `MediaStoragePort` doc).
- URLs are **permanent, public, CDN-style**, stored on the asset row. The only unguessable part is a
  random UUIDv4 `storageToken` in the key. Deletion = remove the asset row + call
  `MediaStoragePort.delete(key)`.
- **`MediaStoragePort` is the seam** — moving to S3/R2/object storage is one adapter class, no
  application/domain/API change.

> **Branch note:** the local backend checkout is on `fix/customer-booking-discovery-active-filter`;
> production runs a `release/v1.2.0-manager-dashboard-rbac-fix`-lineage branch. V16/V17 are old
> enough that the media system is certainly deployed. Whoever implements must branch from the correct
> release base.

---

## 2. Missing pieces

### Backend

| # | Gap | Blocking? |
|---|---|---|
| B1 | `MediaOwnerType` has only `SALON` | yes (Option A) |
| B2 | `media_assets.salon_id` is `NOT NULL` with FK to `salons` — a user asset has no salon | yes (Option A) |
| B3 | `UploadMediaUseCase` structurally needs a `Salon` + `MANAGE_SALON` — cannot be reused | yes |
| B4 | No user-scoped media endpoint (`/api/v1/users/me/media` or `/avatar` `/cover`) | yes |
| B5 | `User` aggregate + `users` table have no `avatar*` / `cover*` | yes |
| B6 | No `UpdateUserUseCase` / no `PATCH /api/v1/users/me` (nothing calls `User.rename()`) | yes for header edit; independent of images |
| B7 | `UserResponse` / `UserResponseDto` carry no image URLs | yes |
| B8 | No "clear avatar / clear cover" path | yes |
| B9 | `SalonPermissionResolver` is salon-centric; self-profile edit needs a trivial `callerId == targetUserId` check instead | yes |
| B10 | No content sniffing, no server-side re-encode, **no EXIF/GPS stripping** | security — see §6 |
| B11 | No orphan cleanup on replace (salon flow leaves old identity assets; a user has exactly one slot each) | quality |

### Android

| # | Gap |
|---|---|
| A1 | `AuthenticatedUser` / `UserResponseDto` — no `avatarUrl` / `coverUrl` |
| A2 | No customer media API / repository (only `ManagerMediaApi`, salon-scoped) |
| A3 | `ProfileScreen` header is a static initial; no cover, no edit affordance, no upload/progress/error state |
| A4 | No `ProfileViewModel` — `ProfileScreen` is stateless over `AuthViewModel` |
| A5 | `AuthViewModel` has no `refreshCurrentUser()` / no way to swap `_currentUser` after an edit |
| A6 | `decodeResizeAndCompress` + `PickVisualMedia` helpers are `private` inside `ManagerSalonMediaScreen.kt` |
| A7 | `RojanRemoteImage` only ever used small/square/circular — a wide cover (with scrim) is a new usage, not a new component |
| A8 | DI: `BackendApiContainer` has no customer media wiring (manager media needs `salonId`; a user endpoint does not) |

---

## 3. Recommended architecture

### 3.1 Backend — Option A (recommended): extend `MediaAsset` to a `USER` owner

Reuses `MediaAsset`, `MediaStoragePort`, disk storage, nginx, the DTO shape, and the Android Coil
seam. The schema was designed for exactly this.

**Domain**
- `enum MediaOwnerType { SALON, USER }`
- `enum MediaType { LOGO, COVER, GALLERY, PORTFOLIO, AVATAR, PROFILE_COVER }`
  (distinct names — keeps per-type validation rules separable: e.g. `AVATAR` should be roughly
  square, `PROFILE_COVER` roughly 16:9)
- `User.assignAvatarMedia(mediaId: MediaAssetId?)` / `User.assignCoverMedia(mediaId: MediaAssetId?)`
  — `null` clears (mirrors `Salon.assignIdentityMedia`)
- `MediaAsset.salonId` becomes nullable **or** introduce `tenantScope` — for a `USER` asset the
  tenant boundary is the user id (`ownerId`), not a salon

**Application**
- `UploadUserMediaUseCase(userRepository, mediaAssetRepository, mediaStoragePort, allowedMimeTypes,
  maxFileSizeBytes, imageProcessor)` — no salon, no `SalonPermissionResolver`; the only check is
  `callerId == targetUserId` (always true for `/users/me/...`). `storageKey = "user/{userId}/{uuid}.{ext}"`.
  **Re-encodes + strips EXIF** (see §6).
- `AssignUserProfileMediaUseCase` — or fold assignment into the upload response (one call: upload →
  assign → return fresh `UserResponse`). A user has exactly one avatar and one cover slot, so the
  "assign a previously-uploaded asset" indirection the salon flow needs buys little here.
- `RemoveUserMediaUseCase` — clears the slot, deletes the `MediaAsset` row + storage file.
- On replace: delete the previous slot asset (avoid orphans — the salon flow's known gap).

**API — `UserController`**
- `POST /api/v1/users/me/media` (multipart: `file`, `mediaType=AVATAR|PROFILE_COVER`) →
  `201` + updated `UserResponse`
- `DELETE /api/v1/users/me/media/{mediaType}` (or `/avatar`, `/cover`) → `200` + updated `UserResponse`
- `UserResponse` (+ `UserResponseDto` mirror) gains `avatarUrl: String?`, `coverUrl: String?`,
  resolved from the assigned asset's `url`
- No `SecurityConfig` change — `/api/v1/users/me/**` is already `authenticated()`

### 3.2 Backend — Option B (lighter): URL columns on `users`

- `users.avatar_url VARCHAR(1000)`, `users.cover_url VARCHAR(1000)` (nullable)
- `MediaStoragePort` still stores the bytes; **skip `MediaAsset` rows** for user images
- `POST /api/v1/users/me/avatar` (multipart) → store → `user.avatarUrl = url` → return `UserResponse`;
  `DELETE` clears + `mediaStoragePort.delete`
- **Pros:** ~half the change; no enum/FK churn; no nullable-`salon_id` migration risk
- **Cons:** diverges from the salon identity pattern; loses the `MediaAsset` metadata/audit row,
  dedup, and any future "media library" story; two parallel media models to maintain

**Recommendation:** **Option A.** The salon side already paid for the polymorphic design and the
code comments explicitly anticipate a non-salon owner. One media system is the lower long-term cost.
Choose B only if there is hard schedule pressure *and* a firm product decision that user images stay
"one avatar + one cover, forever."

### 3.3 Android

```
data/remote/dto/AuthDtos.kt          UserResponseDto += avatarUrl: String? = null, coverUrl: String? = null
domain/repository/BackendAuthRepository.kt   AuthenticatedUser += avatarUrl: String?, coverUrl: String?
data/repository/BackendAuthRepositoryImpl.kt  mapper updated

data/remote/UserMediaApi.kt          NEW  @Multipart POST "api/v1/users/me/media" (file + mediaType), DELETE ".../{mediaType}"
domain/repository/UserProfileRepository.kt    NEW  uploadAvatar / uploadCover / removeAvatar / removeCover -> Result<AuthenticatedUser>
data/repository/UserProfileRepositoryImpl.kt  NEW  multipart via MultipartBody.Part.createFormData + safeApiCall

presentation/auth/AuthViewModel.kt   + fun applyUpdatedUser(user: AuthenticatedUser)  (swap _currentUser)
                                     + suspend fun refreshCurrentUser()               (re-pull /users/me)
presentation/profile/ProfileViewModel.kt   NEW  isUploadingAvatar / isUploadingCover / errorMessage; calls repo; pushes fresh user to AuthViewModel

ui/media/ImagePicker.kt              NEW  promote decodeResizeAndCompress + PickVisualMedia helpers out of ManagerSalonMediaScreen.kt (shared)
di/BackendApiContainer.kt            + userProfileRepository (authenticated retrofit; no salonId)
```

- Reuse `RojanRemoteImage` for both avatar (`shape = CircleShape`) and cover
  (`shape = RectangleShape`, wide box, gradient scrim overlay).
- Reuse client-side sizing: **avatar `maxDimension = 512` (or 1024), cover `maxDimension = 1600`**,
  JPEG q80 — keeps real payloads ~80–400 KB even before the backend re-encodes.
- Errors → existing `userMessageFor(...)` (already handles `MalformedResponseException` etc. from the
  Phase 4 hardening).

### 3.4 Profile header redesign (Quiet Luxury)

Constraints: `HomeBackgroundTheme` dark ground, flat, **rose-gold as the single accent**, no glow,
outlined icons, `CustomerScaffold` unchanged. Only the header changes; menu groups + logout stay
byte-identical.

```
┌─────────────────────────────────────────┐  ← CustomerScaffold top bar "حساب کاربری"
│                                         │
│      [ COVER IMAGE — full width ]        │  ~160–180dp, RojanRemoteImage(RectangleShape,
│      subtle bottom→transparent scrim     │  ContentScale.Crop); fallback = flat CustomerSurface
│                              (edit ✎)    │  fill. Tap anywhere or the ✎ badge → picker.
│                                         │
│        ╭───────╮                         │
│        │ AVATAR │  ← 96dp circle, overlaps cover's bottom edge by ~40dp
│        │  (📷)  │     RojanRemoteImage(CircleShape) + initial fallback (current logic)
│        ╰───────╯     small rose-gold camera badge, bottom-trailing
│                                         │
│   نام کاربر            [ ✓ تایید شده ]   │  name Display 26sp + verified-phone chip (rose-gold)
│                                         │
├─────────────────────────────────────────┤
│  اطلاعات شخصی   … (unchanged)            │
│  فعالیت من      … (unchanged)            │
│  …                                       │
```

- **Uploading:** shimmer / spinner overlay on the *targeted* element only (avatar OR cover), never
  the whole screen — mirrors `ManagerSalonMediaViewModel`'s separate flags.
- **Edit interaction:** tapping the avatar or cover opens the Photo Picker directly; long-press or a
  tiny `CustomerConfirmDialog`-style sheet offers "حذف عکس" (remove) when one is set.
- **Guest / no user:** render the initial placeholder, **no ✎ badge, no cover edit** — optionally a
  quiet "برای شخصی‌سازی، وارد شوید" line.
- **Propagate (additive, optional in the same phase):** `CustomerDashboardScreen` /
  `CustomerHomeScreen` top-right chip and (optionally) `CustomerBottomBar` PROFILE tab render the
  avatar when `currentUser?.avatarUrl != null`, falling back to `Person` exactly as today.

---

## 4. Android changes (checklist)

1. `UserResponseDto` + `AuthenticatedUser` + mapper — add `avatarUrl` / `coverUrl` (nullable).
2. `UserMediaApi` (multipart POST + DELETE), `UserProfileRepository` + impl, DI wiring.
3. `AuthViewModel.applyUpdatedUser(...)` + `refreshCurrentUser()`.
4. `ui/media/ImagePicker.kt` — promote `decodeResizeAndCompress` + `PickVisualMedia` request/launcher.
5. `presentation/profile/ProfileViewModel.kt` — upload state, per-slot flags, error via `userMessageFor`.
6. `ProfileScreen` header rebuild (cover band + overlapping avatar + edit badges + progress);
   **menu groups + logout untouched**.
7. Render `avatarUrl` in the dashboard/home profile chip (+ optionally the bottom bar).
8. Unit tests: `UserProfileRepositoryImpl` (multipart shape, error mapping), `ProfileViewModel`
   (upload → success swaps user; failure surfaces message; per-slot isolation).
9. A72: pick avatar → upload → header updates live → force-stop → relaunch → persists (fresh
   `/users/me`); same for cover; remove; airplane-mode + oversize error states; guest (no crash, no
   edit affordance).

---

## 5. Backend changes (checklist)

**Option A**

1. Migration `Vxx__user_profile_media.sql` (see §7).
2. Domain: `MediaOwnerType.USER`; `MediaType.AVATAR` + `PROFILE_COVER`; `MediaAsset.salonId` nullable
   (+ invariant that a non-`USER` asset has a salon); `User.assignAvatarMedia` / `assignCoverMedia`.
3. Persistence: `MediaAssetJpaEntity.salonId` nullable; `UserJpaEntity` + `avatarMediaId` /
   `coverMediaId`; adapter mappings; `MediaAssetRepository.findByOwnerId` for `USER`.
4. Application: `UploadUserMediaUseCase` (self-scoped, mime + size + **dimension** checks, **re-encode
   + EXIF strip**), assign folded in or `AssignUserProfileMediaUseCase`, `RemoveUserMediaUseCase`
   (delete-old-on-replace).
5. API: `POST /api/v1/users/me/media`, `DELETE /api/v1/users/me/media/{mediaType}`; `UserResponse` +
   `avatarUrl` / `coverUrl` (resolve from assigned asset); bean wiring in an
   `@Configuration` (parallel to `MediaUseCaseConfig`).
6. Guard: in `AssignSalonIdentityMediaUseCase`, additionally require `mediaAsset.ownerType == SALON`
   (block assigning a `USER` asset as a salon logo/cover).
7. Integration tests: self-only (200), other user's `/media` unreachable (path is `/me`, so N/A —
   but test a forged token can't cross), oversize → 413, bad mime → 415, non-image bytes with
   `image/jpeg` header → rejected (after adding sniff), assign-USER-asset-as-salon-logo → 4xx,
   `GET /users/me` returns the URLs, replace deletes the old asset + file, clear works.
8. Deploy on the current release-branch flow — additive, no `SecurityConfig` change.

**Option B**: steps 1 (just two `VARCHAR` columns), 4 (store + set column + return), 5 (endpoints +
response fields), 7 (subset), 8.

---

## 6. Security considerations

| # | Concern | Recommendation |
|---|---|---|
| S1 | **Authorization** | User-media routes are `/users/me/...` only — caller from JWT, **never a `{userId}` path param**. No admin override for v1. |
| S2 | **Cross-owner asset misuse** (Option A) | A `USER` `MediaAsset` must never become a *salon's* logo/cover — add `ownerType == SALON` assertion in `AssignSalonIdentityMediaUseCase`; the user assign path asserts `ownerType == USER && ownerId == callerId`. |
| S3 | **Content-type spoofing** | Backend currently trusts `file.contentType`. **Add magic-byte sniffing** (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF….WEBP`) and reject on mismatch — for user uploads at minimum. |
| S4 | **EXIF / GPS leak** | **Server-side re-encode is a privacy requirement, not optional.** The Android manager client bakes rotation and drops EXIF, but a hostile client can POST a raw camera file with GPS coordinates that then sit on a public `/media/` URL. Re-encode every user upload through `ImageIO`/`Thumbnails`/`scrimage` and write only pixel data. |
| S5 | **Decompression bombs** | Cap decoded dimensions server-side (reject > ~8000px per side / > ~40 MP) before full decode; the re-encode step then also bounds output. |
| S6 | **Size** | Reuse `rojan.media.max-file-size-bytes` (5 MB); nginx `client_max_body_size 12m` and Spring `max-file-size 10MB` already cover it. Client resize keeps real payloads small. |
| S7 | **Public URL permanence** | `/media/` is unauthenticated; the only unguessable token is the `storageKey` UUIDv4. Acceptable for avatars (semi-public by nature). Revocation = delete asset row **and** storage file — make replace + remove do both. |
| S8 | **Rate limiting** | Add a modest per-user cap (e.g. 10 uploads/hour). Not present on the salon endpoint either, but this one is user-facing and cheap to abuse. |
| S9 | **Serving headers** | `/media/` already sets `X-Content-Type-Options: nosniff`. Add an explicit `Content-Type` allowlist and `Content-Disposition: inline` on serve; consider a dedicated cookieless media host/subdomain long-term. |
| S10 | **Moderation** | Out of scope for v1 (profile is private — the avatar is shown only to its owner). **Revisit before** avatars surface anywhere a salon or other customer sees them (reviews, booking lists). |
| S11 | **Storage isolation** | Keep user media under `user/{userId}/...` keys (never `salon/...`) so a future per-tenant bucket/prefix policy is clean. |

---

## 7. Database changes

**Option A** — `Vxx__user_profile_media.sql`:

```sql
-- User-owned profile media (avatar, cover). Extends the polymorphic MediaAsset
-- model to its first non-SALON owner, as domain/media/MediaAsset.kt anticipated.

ALTER TABLE media_assets ALTER COLUMN salon_id DROP NOT NULL;

-- keep the constraint tight: a non-USER asset still must have a salon
ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_assets_owner_scope
    CHECK (owner_type = 'USER' OR salon_id IS NOT NULL);

-- owner_type is VARCHAR already; the new value 'USER' needs no DDL.

ALTER TABLE users ADD COLUMN avatar_media_id UUID REFERENCES media_assets (id);
ALTER TABLE users ADD COLUMN cover_media_id  UUID REFERENCES media_assets (id);

CREATE INDEX idx_media_assets_owner_user
    ON media_assets (owner_id) WHERE owner_type = 'USER';
```

**Option B** — `Vxx__user_profile_images.sql`:

```sql
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(1000);
ALTER TABLE users ADD COLUMN cover_url  VARCHAR(1000);
```

Both are additive and backward-compatible: existing rows get `NULL`, `GET /users/me` returns `null`
URLs, and the Android client (once it reads the new nullable fields) renders exactly today's initial
placeholder until the user uploads.

---

## 8. Implementation order

| Step | Work | Depends on |
|---|---|---|
| 1 | **Backend schema + domain** (Option A migration; `MediaOwnerType.USER`; `MediaType.AVATAR`/`PROFILE_COVER`; `User.assignAvatarMedia`/`assignCoverMedia`; entity + adapter) | — |
| 2 | **Backend use cases** — `UploadUserMediaUseCase` (self-scoped; mime sniff + size + dimension; **re-encode + EXIF strip**), assign (folded or separate), `RemoveUserMediaUseCase` (delete-old-on-replace) | 1 |
| 3 | **Backend API** — `POST/DELETE /api/v1/users/me/media`; `UserResponse` + `avatarUrl`/`coverUrl`; bean config; cross-owner guard in `AssignSalonIdentityMediaUseCase` | 2 |
| 4 | **Backend tests + deploy** — integration coverage (§5.7); deploy additive on the release-branch flow | 3 |
| 5 | **Android data layer** — DTO/domain field + mapper; `UserMediaApi`; `UserProfileRepository` + impl; `BackendApiContainer` wiring; `AuthViewModel.applyUpdatedUser` / `refreshCurrentUser` | 4 |
| 6 | **Android shared util** — `ui/media/ImagePicker.kt` (promote `decodeResizeAndCompress` + `PickVisualMedia`) | — (parallel with 5) |
| 7 | **Android ViewModel** — `ProfileViewModel` (per-slot upload state, error mapping) | 5, 6 |
| 8 | **Android UI** — `ProfileScreen` header redesign (cover + overlapping avatar + edit badges + progress); menu/logout untouched | 7 |
| 9 | **Android propagation** — dashboard/home profile chip (+ optionally bottom bar) render `avatarUrl` | 5 |
| 10 | **A72 verification** — avatar + cover upload/persist/remove; error states (offline, oversize); guest safety | 8, 9 |

**Critical path:** steps 1→4 (backend). Android 5–10 is mechanical once the contract lands. Steps 6
and the `ProfileScreen` visual scaffolding can be built in parallel against a stub.

---

## 9. Effort estimate (rough)

| Area | Option A | Option B |
|---|---|---|
| Backend | ~2–3 days (schema + 3 use cases + endpoint + re-encode/EXIF + tests + deploy) | ~1–1.5 days |
| Android | ~2–3 days (data layer + shared picker util + ViewModel + header redesign + propagation + A72) | ~2–3 days (same) |
| **Total** | **~5–6 days** | **~3.5–4.5 days** |

Nothing was committed.
