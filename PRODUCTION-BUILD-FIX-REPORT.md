# Production Build Fix — Release Blocker P0-1

**Date:** 2026-09-10 · **Scope:** P0-1 only (make `customerProductionRelease` buildable). No UI,
screen, ViewModel, repository, or business-logic change. **Not committed.**

---

## 1. Root cause

`./gradlew :app:assembleCustomerProductionRelease` failed at task-graph-ready with:

```
Missing required property: PRODUCTION_API_BASE_URL
```

Two things combined:

1. **The `production` flavor had no URL to compile in.**
   ```kotlin
   create("production") {
       dimension = "environment"
       buildConfigField("String", "API_BASE_URL",
           "\"${project.findProperty("PRODUCTION_API_BASE_URL") ?: ""}\"")   // → ""
   }
   ```
   Per ADR-003, `staging`/`production` deliberately defaulted to `""` because *"there is no
   confirmed staging/production deployment to point at yet"*.

2. **A build-time hard-fail guard enforced that a property be passed.**
   ```kotlin
   gradle.taskGraph.whenReady {
       if (buildsProductionRelease) {
           val productionUrl = project.findProperty("PRODUCTION_API_BASE_URL") as String?
           if (productionUrl.isNullOrBlank()) { throw GradleException("Missing required property: …") }
       }
   }
   ```

So a production release could only be built by someone who knew to pass
`-PPRODUCTION_API_BASE_URL=…`. `NetworkConfig.kt` was **not** the problem — its
`check(BuildConfig.API_BASE_URL.isNotBlank())` is a correct runtime guard and needs no change; it
only ever fired because the field was empty.

**The precondition ADR-003 was waiting on is now met:** the production backend is live and the
approved URL — `https://api.rojanai.ir/` — has been provided. This is the ADR's own
"Staging/Production environment activation … a separate decision when a backend exists" step.

## 2. Change (1 file: `app/build.gradle.kts`)

### a. New top-level resolver — the approved URL as the committed default

```kotlin
// Production API endpoint (Release Blocker P0-1 fix, 2026-09-10). …
val productionApiBaseUrl: String =
    (project.findProperty("PRODUCTION_API_BASE_URL") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "https://api.rojanai.ir/"
```

- **Zero-config by default:** a production release build needs no `local.properties`, no
  `~/.gradle` property, no `-P` flag.
- **Override preserved:** `-PPRODUCTION_API_BASE_URL=https://other/` still wins (domain migration,
  pre-prod smoke test). A blank / whitespace override is trimmed and ignored in favour of the
  default (can't accidentally ship an empty base URL).
- Sits next to the existing `keystoreProperties` / `localProperties` parsing — same place, same
  style.

### b. `production` flavor now consumes it

```kotlin
create("production") {
    dimension = "environment"
    buildConfigField("String", "API_BASE_URL", "\"$productionApiBaseUrl\"")
}
```

`applicationId` is unchanged — `production` has no `applicationIdSuffix`, so the release identity
stays `ai.rojan.designlab` (the same value the pre-existing config produced).

### c. The build-time guard now validates the URL instead of demanding a property

```kotlin
gradle.taskGraph.whenReady {
    val buildsProductionRelease = allTasks.any { it.name.contains("ProductionRelease") }
    if (buildsProductionRelease && !(productionApiBaseUrl.startsWith("https://") && productionApiBaseUrl.endsWith("/"))) {
        throw GradleException("Production API base URL is malformed: \"$productionApiBaseUrl\" …")
    }
}
```

Same mechanism (build-time, not first-runtime-access; inspects the *resolved* task graph so
`./gradlew build` is covered too; a `whenReady` listener still runs under a reused configuration
cache). Its job shifts from *"a property must be present"* → *"the URL that will actually be
compiled in is well-formed https + trailing slash"*. For the default `https://api.rojanai.ir/` it
passes silently; it only fires on a malformed explicit override.

### What did NOT change

| | |
|---|---|
| `dev` flavor | untouched — still reads `DEV_API_BASE_URL` from git-ignored `local.properties` |
| `staging` flavor | untouched — still reads `-PSTAGING_API_BASE_URL`, defaults to `""` |
| `buildTypes.release` | untouched (R8 / shrink / signing-config-if-present) |
| `signingConfigs` | untouched — **P0-2 (unsigned) is still open**, out of scope here |
| `NetworkConfig.kt` | untouched — its `check()` now simply passes for `production` |
| any Kotlin source, resource, manifest | untouched |
| `BuildConfig` field names / types | unchanged (`API_BASE_URL: String`, `FLAVOR`, …) |

## 3. Verification

| Command | Result |
|---|---|
| `./gradlew :app:assembleCustomerProductionRelease` | **BUILD SUCCESSFUL** in 16m 1s, exit 0 (52 tasks; full R8 minify + resource shrink + ART profile) |
| `./gradlew :app:lintCustomerProductionRelease` | **BUILD SUCCESSFUL**, exit 0 — **93 warnings / 0 errors** (`abortOnError = true`; same 93 as `customerDevRelease` — `UnusedResources`, `IconLocation`, etc., all non-blocking) |

**Artifact:** `app/build/outputs/apk/customerProduction/release/app-customer-production-release-unsigned.apk`
— 4,324,403 bytes (identical size to the dev-release APK; unsigned = P0-2, out of scope).

**Compiled `BuildConfig` (`.../customerProduction/release/.../BuildConfig.java`):**

| Field | Value |
|---|---|
| `API_BASE_URL` | `"https://api.rojanai.ir/"` ✅ — the approved URL, **no `-P` flag, no `local.properties`** |
| `APPLICATION_ID` | `"ai.rojan.designlab"` (no suffix — unchanged) |
| `FLAVOR` | `"customerProduction"` |
| `VERSION_CODE` / `VERSION_NAME` | `1` / `"1.0.0"` (unchanged) |

**Guard behaviour verified (`--dry-run`):**

| Invocation | Outcome |
|---|---|
| _(no `-P`)_ | ✅ SUCCESS → compiles `https://api.rojanai.ir/` |
| `-PPRODUCTION_API_BASE_URL="  "` (blank) | ✅ SUCCESS → trimmed & ignored → default |
| `-PPRODUCTION_API_BASE_URL=https://pre-prod.rojanai.ir/` | ✅ SUCCESS → uses the override |
| `-PPRODUCTION_API_BASE_URL=http://insecure.example` (no https / no `/`) | ⛔ **BUILD FAILED** — "Production API base URL is malformed" |

## 4. How the release build is invoked now

```
# Signed production release (once P0-2 / keystore is in place):
./gradlew :app:assembleCustomerProductionRelease          # → ai.rojan.designlab, https://api.rojanai.ir/
./gradlew :app:bundleCustomerProductionRelease            # AAB for Play

# Override the endpoint (domain migration / pre-prod):
./gradlew :app:assembleCustomerProductionRelease -PPRODUCTION_API_BASE_URL=https://staging.rojanai.ir/
```

No `local.properties`, no per-machine setup, no CI secret required for the URL (a secret is still
needed for **signing** — P0-2).

## 5. Follow-ups (not done here — out of scope)

- **P0-2 signing** — the release APK is still `-unsigned`. Separate blocker.
- **ADR-003** (`docs/architecture/10_.../ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md`) still
  states production's `buildConfigField` is `""` "by design" and that "Android never … hardcodes a
  real address". That stance was explicitly conditioned on *"no confirmed deployment yet"*; that
  condition no longer holds. The ADR needs a superseding note recording that `production` was
  activated with the approved `https://api.rojanai.ir/` default (override retained, so
  Infrastructure still owns the value via CI `-P` per ADR §5). Governance-doc edit — deferred to
  the ADR owner.
- **`NetworkConfig.kt` doc comment** now slightly overstates the situation ("staging/production …
  empty by default"). Cosmetic; left untouched to keep this change to one file.
- `staging` remains `""` / `-P`-only — unchanged, still no confirmed staging backend.

**No commit.**
