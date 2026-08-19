import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: credentials + keystore path live in `keystore.properties`
// (repo root, gitignored — never commit) rather than here, so this file
// stays safe to commit with no secrets in it. The keystore itself
// (keystore/rojan-manager-release.jks) is also gitignored (*.jks, see
// .gitignore). Both are absent on a fresh checkout that hasn't been given
// the real signing material, which is why every read below is guarded by
// `keystorePropertiesFile.exists()` — a release build still succeeds
// (unsigned, same as before this setup existed) rather than hard-failing
// for anyone who doesn't have the production keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
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
        targetSdk = 36
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
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${project.findProperty("PRODUCTION_API_BASE_URL") ?: ""}\"",
            )
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Production API Configuration Hardening (Phase 10, Step 8): the
// `production` flavor's buildConfigField above already reads
// PRODUCTION_API_BASE_URL and NetworkConfig.kt already fails loudly if it's
// blank - but only the first time BASE_URL is actually accessed at runtime,
// which is well after a "production" release APK/AAB has already been
// built, signed, and could be handed out. This closes that gap at build
// time instead: inspecting the *resolved* task graph (not just the
// literally-typed task names) means this also catches aggregate
// invocations like `./gradlew build` that pull in a Production+Release
// variant indirectly, not only a direct `assembleManagerProductionRelease`.
// Re-evaluated on every invocation (a `whenReady` listener, unlike plain
// configuration-phase code, isn't skipped when the configuration cache is
// reused), so a stale cached "it was fine last time" can't mask a
// genuinely missing property today. Dev/staging are untouched - dev keeps
// its hardcoded local URL, staging keeps its own existing
// STAGING_API_BASE_URL handling exactly as before.
gradle.taskGraph.whenReady {
    val buildsProductionRelease = allTasks.any { it.name.contains("ProductionRelease") }
    if (buildsProductionRelease) {
        val productionUrl = project.findProperty("PRODUCTION_API_BASE_URL") as String?
        if (productionUrl.isNullOrBlank()) {
            throw GradleException(
                "Missing required property: PRODUCTION_API_BASE_URL\n" +
                    "\n" +
                    "Production release builds require a real, deployed backend URL - there is no " +
                    "local fallback or fake endpoint for this environment.\n" +
                    "\n" +
                    "Provide it one of these ways:\n" +
                    "  - Command line:   ./gradlew assembleManagerProductionRelease -PPRODUCTION_API_BASE_URL=https://your-real-backend/\n" +
                    "  - gradle.properties (local only - never commit a real value): PRODUCTION_API_BASE_URL=https://your-real-backend/\n" +
                    "  - CI: inject it as a Gradle property or environment-backed property for the release job.",
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