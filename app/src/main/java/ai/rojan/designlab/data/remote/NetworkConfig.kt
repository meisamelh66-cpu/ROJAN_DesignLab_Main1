package ai.rojan.designlab.data.remote

import ai.rojan.designlab.BuildConfig

/**
 * Backend base URL. Sourced from [BuildConfig.API_BASE_URL] (see
 * `app/build.gradle.kts`'s `dev`/`staging`/`production` environment
 * flavors) rather than hardcoded here — `10.0.2.2` is the Android
 * emulator's standard alias for the host machine's `localhost`, used by
 * the `dev` flavor. `staging`/`production` have no confirmed real URL to
 * point at yet, so their `buildConfigField` is empty by default rather
 * than a guessed/fabricated domain - [BASE_URL] fails loudly (not with a
 * silent empty-string base URL) if accessed on a build where that wasn't
 * actually configured via `-PSTAGING_API_BASE_URL=...`/
 * `-PPRODUCTION_API_BASE_URL=...`.
 */
object NetworkConfig {
    val BASE_URL: String by lazy {
        check(BuildConfig.API_BASE_URL.isNotBlank()) {
            "API_BASE_URL is not configured for the \"${BuildConfig.FLAVOR}\" flavor. " +
                "Set STAGING_API_BASE_URL or PRODUCTION_API_BASE_URL (gradle.properties or -P) " +
                "before building this environment - see app/build.gradle.kts."
        }
        BuildConfig.API_BASE_URL
    }
}
