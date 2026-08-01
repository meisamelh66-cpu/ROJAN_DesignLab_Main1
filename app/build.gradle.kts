plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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
        versionName = "1.0"

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
    flavorDimensions += "target"
    productFlavors {
        create("customer") {
            dimension = "target"
        }
        create("manager") {
            dimension = "target"
            applicationId = "ai.rojan.designlab.manager"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
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


    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


    // Debug
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}