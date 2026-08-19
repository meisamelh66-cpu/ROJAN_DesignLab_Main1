package ai.rojan.designlab.data.remote

import ai.rojan.designlab.BuildConfig

/**
 * Backend base URL. Sourced from [BuildConfig.API_BASE_URL] (see
 * `app/build.gradle.kts`'s `dev`/`staging`/`production` environment
 * flavors) rather than hardcoded here. Per ADR-003
 * (`docs/architecture/10_ARCHITECTURE_DECISIONS_ADR/ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md`),
 * no environment ships a compiled default that only resolves in one
 * runtime context: `dev` reads `DEV_API_BASE_URL` from the developer's
 * own git-ignored `local.properties` (see `local.properties.sample` for
 * the emulator/physical-device recipes); `staging`/`production` have no
 * confirmed real URL to point at yet, so their `buildConfigField` is
 * empty by default rather than a guessed/fabricated domain. [BASE_URL]
 * fails loudly (not with a silent empty-string base URL) if accessed on
 * a build where the relevant value wasn't actually configured.
 */
object NetworkConfig {
    val BASE_URL: String by lazy {
        check(BuildConfig.API_BASE_URL.isNotBlank()) {
            "API_BASE_URL is not configured for the \"${BuildConfig.FLAVOR}\" flavor. " +
                "Set DEV_API_BASE_URL in local.properties (see local.properties.sample) for dev, " +
                "or STAGING_API_BASE_URL/PRODUCTION_API_BASE_URL (gradle.properties or -P) for " +
                "staging/production - before building this environment. See app/build.gradle.kts."
        }
        BuildConfig.API_BASE_URL
    }
}
