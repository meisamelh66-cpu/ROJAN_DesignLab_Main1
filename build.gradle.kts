// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Pass 62 (ROJAN_Backend already proved this exact plugin/config shape - mirrored here, not
    // reinvented): report-only OWASP Dependency-Check baseline. Android had no dependency-
    // vulnerability scanning at all before this (confirmed: zero prior reference to this plugin
    // anywhere in this file or app/build.gradle.kts).
    id("org.owasp.dependencycheck") version "13.0.0"
}

// Report-only baseline, same policy as ROJAN_Backend's own dependencyCheck block:
// failBuildOnCVSS stays above the real 10.0 ceiling (11.0f) so this task can never fail the build
// on severity - a deliberate, explicit policy choice (report-only, not yet a blocking gate),
// matching the Backend precedent rather than inventing a stricter default unilaterally. NVD_API_KEY
// is read only from the environment at CI time (set via a GitHub Actions secret in the companion
// workflow change) - never hardcoded, never printed, never logged.
dependencyCheck {
    failBuildOnCVSS = 11.0f
    formats = listOf("HTML", "JSON")
    nvd {
        // .trim(): the real stored secret value was confirmed (via a bounded, content-free CI
        // diagnostic - length/whitespace checks only, never the value itself) to carry 2 trailing
        // control characters beyond the real 36-character key. Stripped, the same key returned a
        // genuine HTTP 200 from NVD's own API. This is a defensive trim at the point of
        // consumption, not a workaround for an actually-invalid key - the key itself is valid.
        apiKey = System.getenv("NVD_API_KEY")?.trim()
    }
    data {
        directory = "${rootProject.projectDir}/.dependency-check-data"
    }
}
