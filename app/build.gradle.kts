import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing (Release Blocker P0-2 fix, 2026-09-10). No secret ever
// lives in this committed file. Credentials + keystore path come from
// EITHER of two sources, checked in order:
//   1. `keystore.properties` at the repo root — gitignored (see .gitignore),
//      for local / workstation release builds. Template: keystore.properties.sample.
//   2. Environment variables — for CI, where secrets are injected, not filed:
//      RELEASE_STORE_FILE  RELEASE_STORE_PASSWORD  RELEASE_KEY_ALIAS  RELEASE_KEY_PASSWORD
//      (RELEASE_STORE_FILE is a path relative to the repo root, or absolute.)
// The keystore itself (keystore/*.jks) is gitignored too.
//
// A fresh checkout with neither source still builds fine: `debug` builds use
// the auto debug keystore, and a `dev`/`staging` release just comes out
// unsigned (unchanged behaviour). The one hard rule — enforced by the
// `gradle.taskGraph.whenReady` guard near the bottom of this file — is that
// a *production* release (`*CustomerProductionRelease` etc.) will NOT build
// unsigned: it fails loudly rather than silently emit an unsigned APK/AAB.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

// Resolve one signing credential: keystore.properties wins, else the env var,
// else null. Blank counts as absent.
fun signingCredential(key: String): String? =
    (keystoreProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

// True only when all four credentials resolve AND the keystore file actually
// exists on disk. Drives whether the `release` signingConfig is populated
// and whether `buildTypes.release` attaches it.
val releaseSigningReady: Boolean = run {
    val storeFilePath = signingCredential("RELEASE_STORE_FILE") ?: return@run false
    val hasAllCreds = listOf("RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
        .all { signingCredential(it) != null }
    hasAllCreds && rootProject.file(storeFilePath).exists()
}

// Environment Configuration (ADR-003): DEV_API_BASE_URL is read from
// `local.properties` (repo root, gitignored — see .gitignore) rather than
// a Gradle project property, so each developer's own value (emulator vs.
// physical-device LAN IP — see local.properties.sample) never leaves
// their machine. Gradle does not auto-expose local.properties keys the
// way it does gradle.properties, so this is parsed the same way
// keystoreProperties above already is.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { load(it) }
    }
}

// Production API endpoint (Release Blocker P0-1 fix, 2026-09-10). ADR-003
// originally left the `production` flavor's URL blank because there was no
// confirmed production deployment to point at, which made a production
// release build hard-fail. That is no longer true: the backend is live and
// the URL below is the approved one. It is committed here as the default so
// a production release build needs NO machine-local configuration
// (local.properties, ~/.gradle props, or -P). An explicit
// `-PPRODUCTION_API_BASE_URL=https://…/` still overrides it — for a future
// domain migration or a pre-prod smoke test — and a blank/whitespace
// override is ignored in favour of the default. `dev` (local.properties)
// and `staging` (-PSTAGING_API_BASE_URL) are untouched.
val productionApiBaseUrl: String =
    (project.findProperty("PRODUCTION_API_BASE_URL") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "https://api.rojanai.ir/"

android {
    namespace = "ai.rojan.designlab"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ai.rojan.designlab"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Manager App split (see CLAUDE.md "App ID Separation" audit): separate
    // installable apps from one codebase, no code duplicated/moved.
    // `customer` has zero overrides — it inherits defaultConfig exactly
    // as-is, so the existing Customer app's applicationId/manifest/
    // branding are byte-for-byte unchanged. `manager` gets its own
    // applicationId + a flavor-only source set (src/manager/...) adding
    // ManagerActivity + a manifest + an app_name override; it reuses the
    // same src/main Manager package (screens/navigation/components) and
    // the existing rojan_manager_logo asset — nothing duplicated.
    //
    // `reception` (ROJAN_Reception_Implementation_Plan_v1.md, Phase 0):
    // same shape as `manager` — own applicationId suffix + flavor-only
    // source set (src/reception/...) adding ReceptionActivity + manifest +
    // app_name override, reusing a shared src/main `reception` package.
    // No bespoke launcher art exists yet, so this flavor points at the
    // existing generic `@mipmap/ic_launcher` as a placeholder rather than
    // fabricating "Reception" branding — swap this out once real art is
    // approved.
    //
    // Environment Configuration (ADR-003,
    // docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md):
    // no `environment` flavor ships a compiled default that only resolves
    // in one runtime context. `dev` reads `DEV_API_BASE_URL` from
    // `local.properties` (see `localProperties` above and
    // `local.properties.sample` for the emulator/physical-device recipes)
    // - never a hardcoded `10.0.2.2` fallback. `staging`/`production`
    // deliberately do NOT hardcode a guessed real URL either (there is no
    // confirmed staging/production deployment to point at yet, and
    // inventing one would fabricate infrastructure that doesn't exist) -
    // each reads its URL from a Gradle property
    // (`-PSTAGING_API_BASE_URL=...` / `-PPRODUCTION_API_BASE_URL=...`, or
    // the per-user `~/.gradle/gradle.properties` - never the repo's own
    // tracked `gradle.properties`, which is committed, not gitignored).
    // NetworkConfig.kt fails loudly at first use if a flavor's URL was
    // left unset, rather than silently falling back to a fake-looking
    // default - true for all three environments now, dev included.
    flavorDimensions += "target"
    flavorDimensions += "environment"
    productFlavors {
        create("customer") {
            dimension = "target"
        }
        create("manager") {
            dimension = "target"
            applicationId = "ai.rojan.designlab.manager"
        }
        create("reception") {
            dimension = "target"
            applicationId = "ai.rojan.designlab.reception"
        }

        create("dev") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("DEV_API_BASE_URL") ?: ""}\"",
            )
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${project.findProperty("STAGING_API_BASE_URL") ?: ""}\"",
            )
        }
        create("production") {
            dimension = "environment"
            // Approved production backend, committed as the default — see
            // `productionApiBaseUrl` above. No -P flag required for a
            // release build; an override is still honoured when passed.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"$productionApiBaseUrl\"",
            )
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(signingCredential("RELEASE_STORE_FILE")!!)
                storePassword = signingCredential("RELEASE_STORE_PASSWORD")
                keyAlias = signingCredential("RELEASE_KEY_ALIAS")
                keyPassword = signingCredential("RELEASE_KEY_PASSWORD")
                // v1/JAR signing stays off — it only matters below API 24 and
                // minSdk is 24. v2 is the AGP default; v3 is enabled
                // explicitly so the artifact carries a rotation-capable
                // signing block (relevant only for self-managed signing —
                // Play App Signing handles rotation itself).
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // R8 code shrinking/obfuscation + resource shrinking (Sprint 5A-2).
            // App code has no reflection, no dynamic class/resource loading,
            // no WebView, no @Keep/@Parcelize; the only libraries with R8
            // hazards (Retrofit, kotlinx.serialization, OkHttp, coroutines)
            // all ship their own bundled R8 rules, so app/proguard-rules.pro
            // stays minimal — see that file.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Separate, newer AGP "application optimization" toggle — left
            // off deliberately (was already false). Independent of
            // isMinifyEnabled above, which is what drives the R8 task.
            optimization {
                enable = false
            }
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Makes java.time.* (and other API 26+ java.* APIs) available on
        // minSdk 24 devices. The manager calendar/booking and reception
        // booking code uses java.time.* directly; without this those call
        // sites are a runtime crash on API 24-25 (NewApi lint triage).
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Explicit lint gate (Sprint 5A). No baseline file — findings are
    // triaged and fixed, not snapshotted. `NewerVersionAvailable` is
    // disabled because it hits the network on every run and makes lint
    // output non-deterministic offline; `GradleDependency` still reports
    // offline-known stale versions.
    lint {
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = false
        disable += "NewerVersionAvailable"
    }
}

// Production release hardening — runs at build time (not first runtime
// access) and inspects the *resolved* task graph, so an aggregate invocation
// like `./gradlew build` that pulls in a Production+Release variant is
// covered too, not just a direct `assembleCustomerProductionRelease`. A
// `whenReady` listener isn't skipped when the configuration cache is reused,
// so a stale "it was fine last time" can't mask a broken build today.
// Dev/staging are untouched by both checks.
gradle.taskGraph.whenReady {
    val buildsProductionRelease = allTasks.any { it.name.contains("ProductionRelease") }
    if (buildsProductionRelease) {

        // P0-1 — the API URL that will be compiled in must be well-formed.
        // (`productionApiBaseUrl` defaults to https://api.rojanai.ir/; this
        // only fires when a malformed -PPRODUCTION_API_BASE_URL is passed.)
        if (!(productionApiBaseUrl.startsWith("https://") && productionApiBaseUrl.endsWith("/"))) {
            throw GradleException(
                "Production API base URL is malformed: \"$productionApiBaseUrl\"\n" +
                    "\n" +
                    "It must be an absolute https URL with a trailing slash (e.g. https://api.rojanai.ir/).\n" +
                    "The build uses https://api.rojanai.ir/ by default; this error means an explicit\n" +
                    "-PPRODUCTION_API_BASE_URL override was passed and is invalid.",
            )
        }

        // P0-2 — never emit an unsigned production APK/AAB. Debug and
        // dev/staging release builds are unaffected.
        if (!releaseSigningReady) {
            throw GradleException(
                "Production release signing is not configured — refusing to build an UNSIGNED " +
                    "production APK/AAB.\n" +
                    "\n" +
                    "Provide the release keystore via ONE of:\n" +
                    "  • keystore.properties at the repo root (gitignored) — see keystore.properties.sample\n" +
                    "  • env vars: RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD\n" +
                    "\n" +
                    "All four values must resolve and RELEASE_STORE_FILE must point to an existing .jks.\n" +
                    "See SIGNING-SETUP-REPORT.md for the full setup.",
            )
        }
    }
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    // Compose core
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Material Icons (برای GlassButton و RoleCard)
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Role Selection flow: persistence, navigation, ViewModel + coroutines support
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Backend auth networking (ROJAN_Backend integration) — see
    // data/remote/ and di/BackendAuthContainer.kt
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Salon Discovery: first real remote-image rendering in this app
    // (salon logos, specialist photos) - every other avatar/logo slot
    // deliberately stayed a color-tinted icon placeholder specifically
    // because no image-loading library existed yet (see RojanRemoteImage.kt's
    // own doc comment). Coil chosen as the standard, lightweight,
    // Compose-native option - approved explicitly before adding, since
    // this app's design system treats "add a new rendering mechanic" as
    // a frozen-baseline-level decision, not a routine dependency bump.
    implementation(libs.coil.compose)

    // Hardened EXIF reader for the salon-media upload flow (replaces
    // android.media.ExifInterface).
    implementation(libs.androidx.exifinterface)

    // Core library desugaring runtime — pairs with
    // android.compileOptions.isCoreLibraryDesugaringEnabled above.
    coreLibraryDesugaring(libs.desugar.jdk.libs)


    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


    // Debug
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}