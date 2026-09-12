# Release Signing Setup — Release Blocker P0-2

**Date:** 2026-09-10 · **Scope:** P0-2 (release signing) only. No business logic, screen, ViewModel,
repository, or manifest touched. **Not committed.**

> Note: the requested helpers `rojan-release-manager` / `android-build-signing` are not registered
> skills or agents in this environment (`Unknown skill` / `Agent type … not found`). The work was
> done directly using standard Android release-signing practice.

---

## 1. Current signing setup (as inspected)

`app/build.gradle.kts` **already had the right shape** — it was just missing the key material:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")   // gitignored
val keystoreProperties = Properties().apply { if (…exists()) load(it) }

signingConfigs { create("release") {
    if (keystorePropertiesFile.exists()) { storeFile = …; storePassword = …; keyAlias = …; keyPassword = … }
} }

buildTypes { release {
    …
    if (keystorePropertiesFile.exists()) { signingConfig = signingConfigs.getByName("release") }
} }
```

- `.gitignore` already covers `*.jks`, `*.keystore`, `keystore.properties` (verified with
  `git check-ignore`).
- **Gaps:** (a) no `keystore.properties`, no `.jks` on disk → every release build came out
  `…-unsigned.apk`; (b) a *production* release **silently** produced an unsigned artifact instead
  of failing; (c) no CI path (file-only, no env-var fallback); (d) no template file.

## 2. Changes

### a. `app/build.gradle.kts` (build config only — not business logic)

| Change | Why |
|---|---|
| New `signingCredential(key)` helper — reads `keystore.properties` **else** the env var of the same name; blank = absent | CI injects secrets as env vars, not files |
| New `releaseSigningReady: Boolean` — true only when all four credentials resolve **and** the `.jks` exists on disk | single source of truth for "can we sign?" |
| `signingConfigs.release` now populated from `signingCredential(...)` when `releaseSigningReady`; `enableV2Signing = true` + `enableV3Signing = true` (v1/JAR left off — minSdk is 24) | v2 covers every supported device (API 24+); v3 adds a rotation-capable signing block |
| `buildTypes.release` attaches the signing config when `releaseSigningReady` (was: `keystorePropertiesFile.exists()`) | also works via env vars now |
| `gradle.taskGraph.whenReady` guard — a `*ProductionRelease` build now **throws** if `!releaseSigningReady` | never ship an unsigned production APK/AAB; merged next to the existing P0-1 URL check |

**Untouched:** `debug` builds (auto debug keystore), `dev`/`staging` **release** builds (still
allowed to be unsigned — unchanged), all Kotlin/resource/manifest, `BuildConfig` fields, R8 config.

### b. New files

| File | Committed? | Purpose |
|---|---|---|
| `keystore/rojan-customer-upload.jks` | **No** (`.gitignore: *.jks`) | the release/upload key (see §3) |
| `keystore.properties` | **No** (`.gitignore: keystore.properties`) | real credentials for local release builds |
| `keystore.properties.sample` | **Yes** (not ignored) | template with `change-me` placeholders + CI note |

`git status` after all changes shows **only** `keystore.properties.sample` and the doc/report files
as new — no secret is stageable.

## 3. The keystore

| Property | Value |
|---|---|
| File | `keystore/rojan-customer-upload.jks` (PKCS12) |
| Alias | `rojan-customer-upload` |
| Key | **RSA 4096-bit**, `SHA384withRSA` self-signed cert |
| Distinguished name | `CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR` |
| Validity | 2026-09-10 → **2056-09-02** (10 950 days — exceeds Google Play's "valid past 2033" requirement) |
| Store / key password | **same** (PKCS12 convention) — see `keystore.properties` (gitignored) |
| SHA-1 fingerprint | `D8:EA:4E:C7:A9:CA:35:A4:50:66:02:92:24:78:13:4E:2D:6B:D4:EB` |
| SHA-256 fingerprint | `63:7E:29:EA:91:3B:A4:B2:A6:2B:D4:26:41:1F:79:97:A6:66:70:C1:EB:EA:EB:1E:E7:81:D3:6F:75:38:9F:6B` |

### ⚠️ Ownership / decision required

This key was **generated during this task** so the signing pipeline could be proven end-to-end.
Before the first Play Store upload the team must decide:

1. **Play App Signing (recommended)** — Google holds the real *app signing key*; this `.jks`
   becomes the **upload key**. If it is ever lost, Google can reset it. Register the SHA-1/SHA-256
   above where third-party services need the signing cert (maps, push, deep links — none currently
   used).
2. **Self-managed app signing** — this `.jks` **is** the app signing key. Losing it =
   **you can never update the app**. Requires offline, redundant, encrypted backup.

Either way: **back up `keystore/rojan-customer-upload.jks` + its password now**, out of the repo,
in a secret manager or an encrypted offline copy. Optionally rotate to team-chosen passwords first
(`keytool -storepasswd` / `-keypasswd`, then update `keystore.properties`).

## 4. How release builds are invoked now

```
# Local signed release (reads keystore.properties):
./gradlew :app:assembleCustomerProductionRelease     # signed APK
./gradlew :app:bundleCustomerProductionRelease       # signed AAB for Play

# CI (no keystore.properties file — export the four vars, keystore restored from a secret):
export RELEASE_STORE_FILE=keystore/rojan-customer-upload.jks
export RELEASE_STORE_PASSWORD=***   RELEASE_KEY_ALIAS=rojan-customer-upload   RELEASE_KEY_PASSWORD=***
./gradlew :app:bundleCustomerProductionRelease

# With neither → build refuses:
#   "Production release signing is not configured — refusing to build an UNSIGNED production APK/AAB."
```

`dev`/`staging` release and all `debug` builds still work with no signing setup at all.

## 5. Verification

### Signed production APK — ✅

```
./gradlew :app:assembleCustomerProductionRelease      → BUILD SUCCESSFUL

apksigner verify --verbose --print-certs app-customer-production-release.apk
  Verifies
  Verified using v2 scheme (APK Signature Scheme v2): true
  Verified using v3 scheme (APK Signature Scheme v3): true
  Number of signers: 1
  Signer #1 certificate DN: CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR
  Signer #1 certificate SHA-256 digest: 637e29ea913ba4b2a62bd426411f7997a66670c1ebeaeb1ee781d36f75389f6b
  Signer #1 key algorithm: RSA   |   key size (bits): 4096
```

- Output file is now `app-customer-production-release.apk` — the `-unsigned` suffix is **gone**.
- `aapt2 dump badging`: `package=ai.rojan.designlab  versionCode=1  versionName=1.0.0  targetSdk=37`.
- P0-1 still intact: `BuildConfig.API_BASE_URL = "https://api.rojanai.ir/"`.

### Signed production AAB (for Play) — ✅

```
./gradlew :app:bundleCustomerProductionRelease        → BUILD SUCCESSFUL
app-customer-production-release.aab  (6.8 MB)  — META-INF/ROJAN-CU.{RSA,SF,MANIFEST.MF}
```
(An AAB is JAR-signed with the upload key; the v2/v3 APK schemes are applied to the split APKs
Play/bundletool generate from it.)

### Guard — production build refuses to go unsigned — ✅

```
# keystore.properties moved aside, no RELEASE_* env vars:
./gradlew :app:assembleCustomerProductionRelease
  → FAILURE: "Production release signing is not configured — refusing to build an
     UNSIGNED production APK/AAB."   (fails in ~7s, config phase, before any work)
```

### Nothing else regressed — ✅

| Build (with `keystore.properties` hidden) | Result |
|---|---|
| `:app:assembleCustomerDevDebug` | BUILD SUCCESSFUL (debug keystore) |
| `:app:assembleCustomerDevRelease` | BUILD SUCCESSFUL — `…-dev-release-unsigned.apk` (unchanged) |
| `:app:assembleCustomerProductionRelease` | **FAILS** (guard) — correct |

### Secrets are not committable — ✅

```
git check-ignore -v  keystore.properties                  → .gitignore:39  (IGNORED)
git check-ignore -v  keystore/rojan-customer-upload.jks    → .gitignore:33  (IGNORED)
git check-ignore -v  keystore.properties.sample            → (not ignored — committable template)
git status --porcelain | grep keystore                     → only "?? keystore.properties.sample"
```

## 6. Still open (out of scope for P0-2)

- Remaining release blockers from `CUSTOMER-RELEASE-READINESS-FINAL.md`: **P0-3** (privacy policy),
  **P0-4** (`app_name = ROJAN_DesignLab`), **P0-5** (booking flow 409). P0-1 (production URL) was
  fixed separately.
- The signing key is generated, not team-provisioned — see §3.
- `keystore.properties.sample` and the `.gitignore` are ready to commit; nothing here is committed.

**Stopped after verification.**
