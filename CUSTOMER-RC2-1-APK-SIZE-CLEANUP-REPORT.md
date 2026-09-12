# ROJAN Customer Android — RC-2.1 APK Size Cleanup

**Date:** 2026-09-11
**Scope:** Customer app only. Removes the single confirmed-unused asset found in `CUSTOMER-RC2-APK-RELEASE-AUDIT.md`.
**Status:** ✅ Complete. Build + signing + identity all re-verified on the rebuilt artifacts. Not committed.

---

## 1. Verification before deletion

Searched the entire project (all Kotlin, XML, Gradle, properties, JSON, txt, and ProGuard files) for any reference to the filename and to any generic asset-loading mechanism that could reach it:

```
grep -rn "specialist_female_01" --include="*.kt" --include="*.xml" --include="*.gradle" \
  --include="*.gradle.kts" --include="*.properties" --include="*.json" --include="*.txt" \
  --include="*.pro" .  (excluding build/ output)
```
2 hits, both in `app/src/main/java/ai/rojan/designlab/ui/assets/SampleImageProvider.kt`:
- line 58 — a doc-comment mentioning the filename in prose, not a code reference.
- line 64 — `fun forSpecialist(specialistId: String): Int = R.drawable.specialist_female_01`, which resolves through the generated `R` class to `app/src/main/res/drawable/specialist_female_01.png` — a **separate file** from the one being removed. `R.drawable.*` references never touch the `assets/` folder; these are two entirely distinct Android resource mechanisms.

Separately, a project-wide search for any generic asset-loading API found **zero matches anywhere in the codebase** (source files only; the only hits were inside the RC-2 audit's own `.md` report, which is documentation, not code):
```
grep -rn "assets/images|assets\.open|AssetManager|context\.assets|LocalContext\.current\.assets" .
```

Since `app/src/main/assets/` is part of the shared `main` source set (used by all three flavors — `customer`, `manager`, `reception`), the search above was run project-wide, not customer-scope-only, to confirm no other flavor's code depends on this file either. None does.

**Conclusion: the file is unreferenced by any Kotlin code, any XML, any Compose resource loader, and any Gradle/build configuration. Confirmed safe to delete.**

## 2. Deleted file

```
app/src/main/assets/images/specialists/specialist_female_01.png   (1,781,255 bytes)
```
Only this one file was removed. The sibling `app/src/main/assets/fonts/OFL.txt` (4,391 bytes — the SIL Open Font License attribution file for the bundled Vazirmatn font, still legitimately in use) was left untouched. `app/src/main/assets/images/specialists/` is now an empty directory (Git does not track empty directories, so no further cleanup is needed there).

## 3. What was NOT modified

Per the task's explicit constraint — confirmed via `git status --short -- app/src/main/assets/`, which shows exactly one line, the single deletion above:
- No Kotlin file was touched.
- No UI/Compose code was touched.
- No resource actually used by the app (`res/drawable/specialist_female_01.png`, the real image the app renders, is untouched — see §5).
- No API/repository/network code was touched.
- No Gradle file (`app/build.gradle.kts`, `gradle/libs.versions.toml`) was touched.
- No signing/build configuration was touched.

## 4. Rebuild

```
./gradlew :app:assembleCustomerProductionRelease   → BUILD SUCCESSFUL (3m 36s)
                                                       mergeCustomerProductionReleaseAssets and
                                                       packageCustomerProductionRelease both re-ran
                                                       (picked up the deletion, not a stale cache hit)
./gradlew :app:bundleCustomerProductionRelease      → BUILD SUCCESSFUL (23s)
                                                       packageCustomerProductionReleaseBundle and
                                                       signCustomerProductionReleaseBundle both re-ran
```
Both tasks compiling and packaging successfully end-to-end is itself confirmation that Kotlin compilation, resource processing, R8/minification, and signing all still succeed after the deletion — no separate `compileCustomerProductionReleaseKotlin`/lint invocation was needed beyond what `assemble`/`bundle` already require and already ran.

## 5. Size reduction

| Artifact | Before (RC-2 audit) | After | Reduction | % smaller |
|---|---|---|---|---|
| APK | 4,308,195 bytes (4.11 MiB) | **2,526,759 bytes (2.41 MiB)** | **1,781,436 bytes (1.70 MiB)** | **~41.3%** |
| AAB | 6,770,551 bytes (6.46 MiB) | **5,140,444 bytes (4.90 MiB)** | **1,630,107 bytes (1.55 MiB)** | **~24.1%** |

The APK's reduction (1,781,436 bytes) is almost exactly the deleted file's own size (1,781,255 bytes) — a clean, essentially 1:1 removal with no other side effects on the package contents, confirmed by re-inspecting the new APK directly:
```
assets/ contents (new APK):
  assets/dexopt/baseline.prof    8,105 bytes   (ART Baseline Profile — unrelated, unaffected)
  assets/dexopt/baseline.profm     971 bytes   (unrelated, unaffected)
  assets/fonts/OFL.txt            4,391 bytes  (font license — unchanged, correctly preserved)

res/ total (new APK): 25 files, 908,484 bytes — byte-identical to the RC-2 baseline, confirming
  res/drawable/specialist_female_01.png (the image the app actually renders via
  SampleImageProvider.forSpecialist()) is fully intact and untouched.
```
The AAB's reduction is somewhat smaller in absolute terms than the APK's (bundles retain resources per-configuration rather than flattening them the way a single APK does), but is still the dominant single change in this build.

## 6. Build verification

- **Signing remains valid**, using the same key: re-ran `apksigner verify --print-certs -v` on the rebuilt APK — `Verifies: true`, v1 still `false`, v2 and v3 still `true`, and every certificate digest (SHA-256, SHA-1, MD5) is **byte-identical** to the RC-2 audit's baseline (`Signer #1 certificate SHA-256 digest: 637e29ea913ba4b2a62bd426411f7997a66670c1ebeaeb1ee781d36f75389f6b`, DN `CN=ROJAN AI, OU=Mobile, O=ROJAN AI, L=Tehran, ST=Tehran, C=IR`, RSA 4096) — confirming the exact same release keystore signed this rebuild, not a different or accidental key.
- **`applicationId`/`versionCode`/`versionName` unchanged**: re-ran `aapt dump badging` on the rebuilt APK — `name='ai.rojan.designlab' versionCode='1' versionName='1.0.0'`, `minSdk=24`, `targetSdk=37`, permissions (`INTERNET` + the standard auto-injected `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) all identical to the RC-2 baseline.

## 7. Release impact

- **No functional change**: the specialist placeholder avatar the app actually displays (`res/drawable/specialist_female_01.png`, referenced via `R.drawable.specialist_female_01`) is completely unaffected — users will see the exact same image everywhere they did before.
- **~1.70 MB smaller APK, ~1.55 MB smaller AAB** — a meaningful download/install-size improvement for a first release, at zero functional or signing risk.
- No new finding introduced, no other RC-2 finding addressed or affected (the privacy-policy/Data Safety store-listing gap and the minor `META-INF` license-file footprint noted in the RC-2 audit remain open, unrelated to this task).

---

**Nothing else in the repository was modified. Nothing was committed.**
