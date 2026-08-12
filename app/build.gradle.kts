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

    // Manager App split (see CLAUDE.md "App ID Separation" audit): two
    // installable apps from one codebase, no code duplicated/moved.
    // `customer` has zero overrides — it inherits defaultConfig exactly
    // as-is, so the existing Customer app's applicationId/manifest/
    // branding are byte-for-byte unchanged. `manager` gets its own
    // applicationId + a flavor-only source set (src/manager/...) adding
    // ManagerActivity + a manifest + an app_name override; it reuses the
    // same src/main Manager package (screens/navigation/components) and
    // the existing rojan_manager_logo asset — nothing duplicated.
    //
    // Manager App Phase 2 (Environment Configuration): a second,
    // independent flavor dimension replaces the single hardcoded
    // API_BASE_URL with three real, separately-buildable environments.
    // `dev` keeps the previous emulator-local value unchanged - no
    // regression for existing local development. `staging`/`production`
    // deliberately do NOT hardcode a guessed real URL (there is no
    // confirmed staging/production deployment to point at yet, and
    // inventing one would fabricate infrastructure that doesn't exist) -
    // each reads its URL from a Gradle property
    // (`-PSTAGING_API_BASE_URL=...` / `-PPRODUCTION_API_BASE_URL=...`, or
    // set in `gradle.properties`, which is gitignored for real values).
    // NetworkConfig.kt fails loudly at first use if that property was left
    // unset for the environment actually being built, rather than silently
    // falling back to a fake-looking default.
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

        create("dev") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
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