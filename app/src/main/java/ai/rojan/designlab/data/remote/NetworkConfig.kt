package ai.rojan.designlab.data.remote

import ai.rojan.designlab.BuildConfig

/**
 * Backend base URL. Sourced from [BuildConfig.API_BASE_URL] (see
 * `app/build.gradle.kts`) rather than hardcoded here — `10.0.2.2` is the
 * Android emulator's standard alias for the host machine's `localhost`. A
 * physical device instead needs the host's real LAN IP; a real deployed
 * backend environment needs a per-build-type value. Both are just a matter
 * of changing `buildConfigField` once either exists — no code change here.
 */
object NetworkConfig {
    const val BASE_URL = BuildConfig.API_BASE_URL
}
