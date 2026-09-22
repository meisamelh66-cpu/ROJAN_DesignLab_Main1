# Release Signing Setup — Release Blocker P0-2

**Date:** 2026-09-10 · **Scope:** P0-2 (release signing) only. No business logic, screen, ViewModel,
repository, or manifest touched. **Not committed.**

> ## ⚠️ SIGNING IDENTITY UPDATE — 2026-09-14
>
> **The keystore documented in §3 below (SHA-1 `D8:EA:4E:...`) is HISTORICAL /
> RETIRED. It is no longer the active Customer production signing identity.**
> That file could not be located anywhere in this project, its git history, or
> other project-local ROJAN backup/archive directories when searched — the
> keystore currently present at `keystore/rojan-customer-upload.jks` carries a
> **different** certificate, first noticed on 2026-09-14 (its filesystem
> creation timestamp is 2026-09-14 09:57:01, four days after this report's own
> 2026-09-10 generation date — how it arrived in this checkout is unknown; no
> git evidence exists either way, since the keystore path was never tracked).
> No Google Play publishing history could be found for either identity (no
> project record of an actual Play Console upload for `ai.rojan.designlab`,
> and no Play Console access was available to check independently), so this
> change does not break an existing Play App Signing relationship — none is
> recorded as ever having existed.
>
> **After investigation, the user explicitly decided to adopt the currently
> available keystore as the official Customer production signing identity
> going forward**, rather than continue searching for the original file.
>
> ### CURRENT OFFICIAL CUSTOMER PRODUCTION SIGNING IDENTITY (adopted 2026-09-14)
>
> | Property | Value |
> |---|---|
> | File | `keystore/rojan-customer-upload.jks` (unchanged path) |
> | Alias | `rojan-customer-upload` (unchanged) |
> | Key | RSA, 4096-bit |
> | Certificate DN | `CN=ROJAN AI Customer, OU=ROJAN Customer, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR` |
> | SHA-1 | `E3:6D:40:E6:CC:AA:07:A9:56:F5:01:D9:CF:A3:64:CE:7A:B6:37:67` |
> | SHA-256 | `0B:B8:DE:3D:48:E6:7F:AC:00:23:CB:EA:72:67:A7:65:51:E9:F6:26:BB:C3:FC:6B:6A:AE:35:08:DA:DC:BC:DD` |
> | Valid until | Friday, January 30, 2054 |
>
> Verified live on 2026-09-14: `signingReport` resolves the `customerProductionRelease`
> variant to this exact file/alias/fingerprint; a real signed APK
> (`app-customer-production-release.apk`) and AAB (`.aab`) were built and
> `apksigner verify --print-certs` confirmed the APK's certificate fingerprint
> matches this table exactly (v2 + v3 schemes verified).
>
> Application identity unchanged throughout: `applicationId = ai.rojan.designlab`,
> `versionName = 1.0.0`, `versionCode = 1`.
>
> No password or private-key material is recorded here or anywhere in this
> update — none was requested, printed, or exposed at any point in this
> investigation.
>
> **Everything below this notice (§3's key table and the rest of this
> document) describes the 2026-09-10 setup as it was at the time and is kept
> for history — do not treat the SHA-1/SHA-256 in §3 as the current signing
> identity.**

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

## 3. The keystore (HISTORICAL — see the 2026-09-14 update notice at the top of this document; this table describes the original 2026-09-10 key, since retired)

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
