# ROJAN Customer Android — RC-2 APK/AAB Release Audit

**Date:** 2026-09-11
**Scope:** Customer app only (`ai.rojan.designlab`, `customer` flavor, `production` environment). Audit only — no source file was modified. Building the release artifacts (`assembleCustomerProductionRelease` / `bundleCustomerProductionRelease`) was necessary to inspect them and is not a code change.
**Method:** direct inspection of the built APK/AAB via `aapt`/`aapt2`/`apksigner`/`dexdump` (Android SDK build-tools 36.0.0) plus source-code cross-referencing — every claim below is traced to either a real tool-output line or a specific file/line.

---

## Executive Summary

The release pipeline itself is **solid and correctly configured**: R8 minification + resource shrinking both genuinely ran (confirmed via build log and a 36MB `mapping.txt` + 4.7MB `usage.txt`), the APK and AAB are both signed with a real, custom RSA-4096 release key (v2+v3, not the debug keystore, not v1), `debuggable` is absent from the final merged manifest (correctly `false`), and zero debug/test/tooling classes leaked into the release dex. `applicationId`, `versionCode`, `versionName`, and the application label all match what's expected.

**One concrete, high-value finding dominates this audit:** a single unreferenced 1.78MB image file bundled as a raw asset (`app/src/main/assets/images/specialists/specialist_female_01.png`) — confirmed via a codebase-wide grep to have **zero code references anywhere** — accounts for roughly **41% of the APK's total content**. Its functional twin, `res/drawable/specialist_female_01.png`, is the one actually used by `SampleImageProvider.forSpecialist()`; the `assets/` copy is a pure duplicate that R8/resource-shrinking cannot remove (shrinking never touches raw `assets/`). This alone is worth fixing before submission.

No P0 (release-blocking defect in the artifact itself) was found. One P1 (the dead asset) and one cross-referenced store-readiness gap (privacy policy link, already known from the prior release-readiness audit) remain before Play Store submission.

---

## Release Decision: **READY, CONDITIONALLY**

The build/signing/obfuscation pipeline is release-quality as-is. Submission should wait on:
1. Removing the dead 1.78MB duplicate asset (P1 — trivial, safe, highest size impact found).
2. Confirming the Play Console-side privacy policy URL + Data Safety form are complete (cross-referenced from `customer-release-readiness` — an external/store-listing task, not a code change, but a real submission blocker regardless).

Neither is a structural or signing problem — both are addressable without touching the release pipeline itself.

---

## 1. Release Build Configuration

**Evidence:** `app/build.gradle.kts` (re-verified unchanged since the RC-1 security audit — `git diff --stat` shows only pre-existing session history, not new drift).

| Check | Result | Reference |
|---|---|---|
| `production` flavor `API_BASE_URL` | Committed default `https://api.rojanai.ir/`; a malformed override fails the build (`gradle.taskGraph.whenReady` guard) | lines 62-77, 258-273 |
| `isMinifyEnabled` (release) | `true` | line 205 |
| `isShrinkResources` (release) | `true` | line 206 |
| Signing configuration | Real keystore via `keystore.properties` (present, gitignored) — `RELEASE_STORE_FILE/PASSWORD/KEY_ALIAS/KEY_PASSWORD` all resolve; `enableV2Signing`/`enableV3Signing` = `true`, v1 correctly omitted (minSdk 24) | lines 179-195 |
| Production-release-without-signing guard | Build hard-fails if `releaseSigningReady` is false when any `*ProductionRelease*` task is in the graph | lines 258-291 |
| `BuildConfig` values | Only `API_BASE_URL` (per-flavor) — no secrets/keys exposed | lines 151-176 |
| `debuggable` flag | Not set for `release` buildType → AGP default (`false`); confirmed absent from the actual merged manifest (§3) | — |

**Result:** no findings — this section was already reviewed in the RC-1 audit and is unchanged; today's artifact inspection independently confirms the config actually took effect in the built output (see §2-§3).

---

## 2. APK / AAB Artifact Inspection

**Built this session** (via `./gradlew :app:assembleCustomerProductionRelease` and `:app:bundleCustomerProductionRelease`, both `BUILD SUCCESSFUL`):
- `app/build/outputs/apk/customerProduction/release/app-customer-production-release.apk` — 4,308,195 bytes
- `app/build/outputs/bundle/customerProductionRelease/app-customer-production-release.aab` — 6,770,551 bytes

### Identity (via `aapt dump badging`)
```
package: name='ai.rojan.designlab' versionCode='1' versionName='1.0.0'
compileSdkVersion='37'   sdkVersion(min)='24'   targetSdkVersion='37'
application-label: 'ROJAN AI'   (confirmed present for EVERY locale including 'fa')
launchable-activity: ai.rojan.designlab.MainActivity
```
All five match the expected values exactly: `applicationId = ai.rojan.designlab`, `versionCode = 1`, `versionName = "1.0.0"`, package name = `ai.rojan.designlab` (same as applicationId — no flavor suffix on `customer`), label = "ROJAN AI" (Persian included).

### Signing (via `apksigner verify --print-certs -v`)
```
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme: true
Verified using v3 scheme: true
Number of signers: 1
Signer #1 certificate DN: CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR
Signer #1 key algorithm: RSA   key size: 4096
```
A real, custom certificate — **not** the default debug keystore (`C=US, O=Android, CN=Android Debug`, which this is clearly not). v1 correctly off, v2+v3 correctly on, matching `app/build.gradle.kts`'s configuration exactly. The AAB carries a JAR-style upload-signature (`META-INF/ROJAN-CU.RSA`/`.SF`) from the same keystore alias ("ROJAN-CU") — the expected mechanism for bundle integrity (Play re-signs the derived APKs with the real app-signing key at distribution time; this signature is Play Console's upload-integrity check, verified implicitly by the `signCustomerProductionReleaseBundle` task succeeding).

### `debuggable` flag
Absent from the fully merged `AndroidManifest.xml` extracted from the built APK (`aapt2 dump xmltree`) — confirms AGP's release default (`false`) actually took effect in the artifact, not just in source config.

**Result:** no findings — every identity/signing check passes.

---

## 3. Android Manifest Audit on the Final (Merged) Artifact

**Evidence:** `aapt2 dump xmltree app-customer-production-release.apk --file AndroidManifest.xml` — the real, fully-merged manifest as packaged, not the source `AndroidManifest.xml` alone (dependency manifests are merged in automatically by AGP).

| Component | Exported? | Notes |
|---|---|---|
| `activity: MainActivity` | `true` | Required — the app's only `MAIN`/`LAUNCHER` entry point; API 31+ mandates `exported=true` for this. |
| `provider: androidx.startup.InitializationProvider` | `false` | AndroidX's standard app-startup mechanism (bundled by `androidx.emoji2`/`androidx.lifecycle`/`androidx.profileinstaller` — none of them app code, all transitively pulled in by Compose/other AndroidX libraries). Not exported — safe. |
| `receiver: androidx.profileinstaller.ProfileInstallReceiver` | `true` | The **only** exported non-activity component in the whole artifact — but protected by `android:permission="android.permission.DUMP"`, a **system/signature-level permission no third-party app can hold**. This is the standard, Google-shipped pattern used by every modern app with AndroidX Baseline Profile support — functionally unreachable by any other app on the device despite the `exported=true` flag. |

**No service, no content provider beyond the standard `InitializationProvider`, and no receiver beyond the standard `ProfileInstallReceiver`** — the app's own source declares zero additional exported components (confirmed against `app/src/main/AndroidManifest.xml` directly), and every merged-in component from dependencies is either non-exported or protected by a permission ordinary apps cannot hold.

### Permissions in the final artifact
```
uses-permission: android.permission.INTERNET
permission: ai.rojan.designlab.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION  (protectionLevel=signature)
uses-permission: ai.rojan.designlab.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```
`INTERNET` is the app's own, single explicitly-requested permission (matches source manifest). The second permission/self-grant pair is **auto-injected by AGP's manifest merger**, not requested by app code — it's the standard mechanism AGP adds when any dependency (below API 33) dynamically registers a not-exported `BroadcastReceiver`, to preserve that "not exported" contract on OS versions that don't natively support the flag. It is a signature-level self-permission (only this app's own signed builds can hold it), not a dangerous/runtime permission, and requires no Play Console declaration.

### Backup settings (final artifact)
```
android:allowBackup=true
android:fullBackupContent=@0x7f0d0000  (→ backup_rules.xml)
android:dataExtractionRules=@0x7f0d0001  (→ data_extraction_rules.xml)
```
Both resource references resolve correctly to the files fixed in the prior RC-1 P1 task (`auth_session_preferences` + `secure_token_preferences` now excluded — not re-audited in depth here since that fix was already verified in `CUSTOMER-RC1-BACKUP-RULES-FIX-REPORT.md`; confirmed here only that the manifest still points at the right resource ids in the final build).

**Result:** no findings — the manifest audit on the actual packaged artifact confirms the source-level review from RC-1 held true through the full build/merge/package pipeline.

---

## 4. Release Size Analysis

**Sizes:** APK = 4,308,195 bytes (≈ 4.11 MiB / 4.31 MB) · AAB = 6,770,551 bytes (≈ 6.46 MiB / 6.77 MB). The AAB is larger than the APK because it retains per-density/per-ABI resources uncompressed for all configurations (Play Console derives smaller, device-specific split APKs from it at install time) — this size difference is expected and not itself a finding.

**Largest contributors (uncompressed content size inside the APK, `unzip -l`):**

| Rank | Entry | Size | Category |
|---|---|---|---|
| 🔴 1 | `assets/images/specialists/specialist_female_01.png` | **1,781,255 bytes (1.78 MB, ~41% of total)** | **Dead, unreferenced asset — see finding below** |
| 2 | `classes.dex` | 2,926,744 bytes (2.93 MB) | Code (single dex file — no multidex needed, a lean dependency surface) |
| 3 | 4× Vazirmatn font weights (`res/*.ttf`) | ~123 KB each, ~491 KB total | Legitimate — the app's whole Persian typography system (`ui/theme/Type.kt`) depends on these |
| 4 | `res/*.webp` (9 files) | ~354 KB total | Launcher icon (adaptive icon, all density buckets resolve through one `res/BW.xml` descriptor) + other drawables, resource-shrinker-renamed to short codes |
| 5 | `resources.arsc` | 96,904 bytes | Standard Android resource table |
| 6 | `okhttp3/internal/publicsuffix/publicsuffixes.gz` | 41,394 bytes | Bundled by OkHttp itself (cookie domain matching) — unavoidable, standard |
| 7 | `kotlin/*.kotlin_builtins` | ~48 KB total | Kotlin stdlib reflection metadata — standard, unavoidable |
| 8 | `lib/**/*.so` (2 libraries × 4 ABIs) | 60,292 bytes total | `libandroidx.graphics.path.so` (Compose path rendering) + `libdatastore_shared_counter.so` (Jetpack DataStore) — both standard AndroidX transitive natives, negligible |
| 9 | `assets/dexopt/baseline.prof` | 8,105 bytes | ART Baseline Profile — **positive**, improves cold-start via AOT hints |
| 10 | `META-INF/androidx/**/LICENSE.txt` (multiple) | 51,750 bytes across 83 files | License-attribution files bundled per-artifact — minor, common packaging footprint |

### 🔴 Finding — dead duplicate asset (P1)

`app/src/main/assets/images/specialists/specialist_female_01.png` (1.78 MB) is a byte-identical duplicate of `app/src/main/res/drawable/specialist_female_01.png` (the one actually referenced, via `R.drawable.specialist_female_01` in `ui/assets/SampleImageProvider.kt:64`, used as the placeholder avatar for every specialist across the app). **A codebase-wide search for any reference to the `assets/` folder or `AssetManager`/`context.assets` found zero matches anywhere in `app/src/main/java`.** This file is never read, loaded, or referenced by any code path. Because Android's `assets/` folder is copied verbatim (never touched by `shrinkResources`, which only operates on `res/`), this dead weight ships in **every** build variant unconditionally — dev, staging, and production, APK and AAB alike.

**Required Action:** delete `app/src/main/assets/images/specialists/specialist_female_01.png` (and, since it becomes the only file under `assets/images/`, the now-empty `assets/images/specialists/` directory tree). Keep `app/src/main/assets/fonts/OFL.txt` (4.3 KB) — that one is the legitimate SIL Open Font License attribution file for the bundled Vazirmatn font, correctly present, small, and license-compliance-required. **Estimated impact:** removing the dead file would reduce the APK's total content by roughly 40%, with a proportional (if not fully 1:1, since PNG is already internally compressed) reduction in the actual compressed APK/AAB file sizes.

**Result:** one P1 finding (above); every other size contributor is legitimate, expected, and appropriately small.

---

## 5. R8 / ProGuard Verification

| Check | Result | Evidence |
|---|---|---|
| Obfuscation enabled | Yes — `minifyCustomerProductionReleaseWithR8` ran (not skipped/UP-TO-DATE-cached-empty; genuinely executed this build) | Build log |
| Mapping generation | `app/build/outputs/mapping/customerProductionRelease/mapping.txt` — 36.3 MB (a real, large mapping table, consistent with a full dependency graph being processed and renamed) | Filesystem, this session |
| Mapping packaged into the APK/AAB? | **No** — confirmed it lives only under `app/build/outputs/mapping/`, a separate Gradle build-output directory never included in `packageCustomerProductionRelease`'s inputs | Filesystem layout |
| Shrinking actually removing code | Yes — `usage.txt` (what R8 removed) is 4.7 MB, `seeds.txt` (kept entry points) is 110 KB — a real, substantial shrink occurred, not a no-op pass-through | Filesystem, this session |
| Accidental debug classes in the release dex | **None found** — searched the extracted `classes.dex` (via `dexdump`) and the mapping file for `androidx.compose.ui.tooling.*`, `androidx.compose.ui.test.*`, `androidx.test.*`, and `junit` — zero matches in either. `debugImplementation`-scoped dependencies (`ui-tooling`, `ui-test-manifest`) correctly stayed out of the release build variant. | dexdump + mapping.txt grep, this session |
| Leaked test code | **None found** — same search above covers this; no test-only classes anywhere in the shipped dex. | — |
| `proguard-rules.pro` content | Minimal, no wildcard `-keep class ** { *; }`, no secret embedded — already reviewed in full in the RC-1 audit, unchanged since | `app/proguard-rules.pro` |

**Result:** no findings — R8/shrinking is genuinely active, effective, and clean of debug/test leakage.

---

## 6. Play Store Readiness

| Check | Result |
|---|---|
| `targetSdk` | 37 — current, well ahead of any Play Store minimum-targetSdk policy floor |
| `minSdk` | 24 (Android 7.0) — reasonable floor, covers the vast majority of active devices |
| Required permissions | Exactly one meaningful permission (`INTERNET`) plus the benign, auto-injected signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (§3) — no dangerous/runtime permission requested at all, which substantially simplifies the Play Console "Data Safety" declaration (no location, camera, contacts, storage, etc. to justify) |
| App name / label | "ROJAN AI" — confirmed present and correct in every locale in the built artifact (§2), matches the prior `APP-IDENTITY-REPORT.md` fix |
| Icon resources | Adaptive icon correctly configured — all six density buckets (`160`–`65534`, i.e. mdpi through xxxhdpi + anydpi) resolve through a single icon descriptor (`res/BW.xml`, an AAPT2-renamed adaptive-icon XML), the modern, correct Android 8.0+ pattern — not a set of separately-maintained per-density PNGs that could drift out of sync |
| **Privacy-related declarations** | **Gap — cross-referenced, not re-audited from scratch here.** Per `customer-release-readiness` (prior session memory): the `/privacy` and `/account-deletion` web pages exist and are implemented, but **an in-app privacy-policy link/reference was explicitly not added to the Android app**, and the Play Console Data Safety form + a live, reachable privacy policy URL entered into Play Console are both still outstanding, separate, store-listing-side tasks. This is **not a code defect in the artifact itself** (nothing in the APK is wrong), but it **is** a genuine Play Store submission blocker that exists independently of anything this audit's checklist covers at the code level. |
| Release metadata readiness (screenshots, store listing copy, content rating, etc.) | **Out of this audit's scope** — these are Play Console/marketing assets, not something inspectable from the APK/AAB or source code. Not evaluated here; flagged only so it isn't silently assumed done. |

**Result:** the artifact itself has no Play-Store-blocking technical property (permissions, SDK levels, identity, icon are all correct) — the one real gap is the privacy-policy/Data Safety completion, which is a store-listing task, not a build fix.

---

## Findings Summary (severity)

| # | Finding | Severity | Section |
|---|---|---|---|
| 1 | `app/src/main/assets/images/specialists/specialist_female_01.png` — 1.78 MB, zero code references, ~41% of APK content | **P1** | 4 |
| 2 | Privacy policy URL / Data Safety form / in-app link not completed (Play Console-side, cross-referenced from prior audit, not re-verified from scratch here) | **P1 (store-listing, not code)** | 6 |
| 3 | `META-INF/androidx/**/LICENSE.txt` duplication, ~52 KB across 83 small files | **P2 (optional)** | 4 |
| 4 | `versionCode = 1` — correct for a first submission; remember to increment on every subsequent release | **P2 (informational)** | 6 |
| — | Everything else checked in §1–§6 | **No findings** | — |

No **P0** (a defect that would make the artifact itself unsuitable for submission) was found anywhere in the build configuration, signing, manifest, R8 output, or artifact identity.

---

## Artifact Verification Results (raw evidence, condensed)

```
./gradlew :app:assembleCustomerProductionRelease   → BUILD SUCCESSFUL (9m 31s), R8 + resource shrink ran
./gradlew :app:bundleCustomerProductionRelease      → BUILD SUCCESSFUL (38s), bundle signed

APK: app-customer-production-release.apk — 4,308,195 bytes
AAB: app-customer-production-release.aab — 6,770,551 bytes

aapt dump badging      → applicationId=ai.rojan.designlab, versionCode=1, versionName=1.0.0,
                          minSdk=24, targetSdk=37, label='ROJAN AI' (all locales incl. fa)
aapt2 dump xmltree      → 1 exported activity (required), 1 exported receiver (DUMP-protected,
                          standard AndroidX pattern), debuggable absent, allowBackup=true with
                          correct backup-rules references
apksigner verify        → Verifies=true, v1=false, v2=true, v3=true, RSA-4096, custom cert
                          (CN=ROJAN AI, C=IR) — not the debug keystore
dexdump + mapping grep  → 0 matches for compose.ui.tooling / compose.ui.test / androidx.test / junit
mapping.txt             → 36.3 MB generated, NOT packaged into the APK/AAB
```

**Nothing was modified or committed in the course of this audit.** All artifacts inspected (`app-customer-production-release.apk`, `app-customer-production-release.aab`) were freshly built this session from the existing, unmodified source tree.
