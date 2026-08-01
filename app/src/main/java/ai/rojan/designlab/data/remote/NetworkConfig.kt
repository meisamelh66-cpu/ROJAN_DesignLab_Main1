package ai.rojan.designlab.data.remote

/**
 * Backend base URL for local development. `10.0.2.2` is the Android
 * emulator's standard alias for the host machine's `localhost` — the way
 * an emulator reaches a backend running on the same dev machine. A
 * physical device instead needs the host's real LAN IP here.
 *
 * TODO: move to a build-variant/BuildConfig value once a real deployed
 * backend environment exists — hardcoded here deliberately for this
 * connectivity-verification milestone (local dev backend only, not prod).
 */
object NetworkConfig {
    const val BASE_URL = "http://10.0.2.2:8080/"
}
