# ADR-003 Environment Configuration Strategy


## Status

Accepted


## Context

ROJAN Android apps (Customer, Manager, Reception) resolve their backend
base URL through a `target` × `environment` build flavor matrix
(`app/build.gradle.kts`). Each `environment` flavor (`dev`, `staging`,
`production`) injects `BuildConfig.API_BASE_URL`, consumed by
`NetworkConfig.kt`.

`staging` and `production` already follow a fail-loud contract: their
`buildConfigField` defaults to an empty string, and `NetworkConfig.kt`
`check()`s that the value is non-blank before use — no fabricated domain
is ever compiled in.

`dev` was the exception:

```kotlin
create("dev") {
    buildConfigField("String", "API_BASE_URL",
        "\"${project.findProperty("DEV_API_BASE_URL") ?: "http://10.0.2.2:8080/"}\"")
}
```

`10.0.2.2` is the Android **Emulator's** fixed alias for the host
machine's `localhost`. It has no meaning outside an emulator's virtual
NAT — on a physical device it is simply an unreachable address.

### The incident

A Manager APK (commit `7842808`) built with this default was installed
on a physical Samsung Galaxy A72 for pilot testing. Phone number login
failed at the OTP-request step with:

> "Connection timed out. Please try again."

Investigation (device connected via `adb`, live logcat capture) confirmed
the exact failing call:

```
08-19 08:03:25.448 I/okhttp.OkHttpClient( 4933): --> POST http://10.0.2.2:8080/api/v1/auth/otp/request (31-byte body)
08-19 08:03:35.470 I/okhttp.OkHttpClient( 4933): <-- HTTP FAILED: java.net.SocketTimeoutException:
    failed to connect to /10.0.2.2 (port 8080) from /25.113.58.65 (port 36378) after 10000ms
```

The app never reached the backend at all — the socket connect attempt to
`10.0.2.2:8080` sat until OkHttp's default 10s connect timeout expired.
This was not a backend defect: the backend has started and served OTP
requests successfully in prior local runs. It was purely a client-side
configuration value that only makes sense inside an emulator, silently
shipped to a physical device.

### Governance gap

No section of `/docs/architecture/` previously defined an environment
configuration policy. `staging`/`production` had independently arrived
at a fail-loud pattern; `dev` diverged from it with its own silent
convention. Per `09_AI_AGENT_GOVERNANCE/CLAUDE_RULES.md`
("ممنوع: ساخت معماری موازی" — parallel architecture is forbidden), an
undocumented second convention sitting beside an already-established one
is exactly the condition this ADR closes.


## Decision

Environment configuration for every ROJAN Android app follows one
uniform contract across `dev`, `staging`, and `production` — no
environment is a special case.

### 1. No environment-specific compiled fallback

No `environment` flavor may ship a `buildConfigField` default that only
resolves correctly in one specific runtime context (an emulator, one
developer's machine, one physical network). `staging`/`production`
already satisfy this by defaulting to `""`. `dev`'s `10.0.2.2` default
violated it and is removed.

### 2. Explicit `DEV_API_BASE_URL` requirement

`dev` adopts the identical contract already used by
`STAGING_API_BASE_URL`/`PRODUCTION_API_BASE_URL`:

```kotlin
create("dev") {
    buildConfigField("String", "API_BASE_URL",
        "\"${project.findProperty("DEV_API_BASE_URL") ?: ""}\"")
}
```

`NetworkConfig.kt`'s existing `check(BuildConfig.API_BASE_URL.isNotBlank())`
already fails loudly for a blank value — this extends that same runtime
guard to `dev` with no new mechanism required.

### 3. Emulator vs. Physical Device separation

Emulator and physical-device testing are **not** separate build flavors
— they are two developer-local values for the same `dev` flavor, supplied
via a git-ignored `local.properties`:

- **Emulator**: `DEV_API_BASE_URL=http://10.0.2.2:8080/` — valid only
  inside the Android Emulator's virtual NAT.
- **Physical device**: `DEV_API_BASE_URL=http://<dev-machine-LAN-IP>:8080/`
  — requires the device to be on the **same Wi-Fi network** as the
  backend host. Cellular data cannot reach a private LAN address; this
  is a precondition, not an app-level concern.

A documented `local.properties.sample` (or equivalent onboarding note)
records both recipes so the choice is explicit per developer/session,
never a guessed compiled-in default.

### 4. Staging/Production fail-loud consistency

`staging` and `production` are unchanged — this decision brings `dev`
into alignment with the pattern they already established, not the
reverse. All three environments now share one rule: **unconfigured means
the build fails, never a silent, possibly-wrong default.**

### 5. Environment Ownership

- **Developer** owns local development (`dev`) URL values — set
  per-machine, per-session, in git-ignored `local.properties`, never
  centrally.
- **Infrastructure/Backend** owns `staging` and `production` endpoint
  values — provisioning the real URL and communicating it to Android is
  a System 1 responsibility, not Android's.
- **Android (System 2)** owns only environment *selection* (which
  `environment` flavor resolves which `BuildConfig` field) and
  *fail-loud enforcement* (the `check()` guard in `NetworkConfig.kt`,
  the empty-string-unless-configured contract in `build.gradle.kts`).
- **Android does not own endpoint values.** For `staging`/`production`,
  Android never guesses, fabricates, or hardcodes a real address — it
  only consumes whatever Infrastructure/Backend provides, exactly as
  `NetworkConfig.kt`'s existing doc comment already states for those two
  environments.


## Reason

Before this decision, `dev` was the one environment where a wrong or
stale assumption (emulator-only reachability) could compile successfully
and only surface as a runtime failure — on a physical device, potentially
during pilot testing with a real user, as it did here. `staging` and
`production` had already independently solved this by refusing to
compile a guess. Extending that same, already-proven mechanism to `dev`
requires no new pattern — only removing the one place it wasn't applied
consistently.


## Impact

Positive:

- Physical-device testing failures caused by a stale/wrong URL now fail
  at build time (or with an immediate, unambiguous check failure) instead
  of a 10-second runtime timeout discovered mid-pilot.
- One consistent contract across `dev`/`staging`/`production` — no
  environment is a special case a developer has to remember.
- No change to `staging`/`production` behavior, no change to any API
  contract, no change to the Backend.

Affected components:

- `app/build.gradle.kts` (`dev` flavor's `buildConfigField` default)
- Android developer onboarding docs / `local.properties.sample`
  (new file, git-ignored, documents both recipes)

No impact:

- `NetworkConfig.kt` (its fail-loud `check()` already generalizes to all
  three environments unchanged)
- Backend, API contracts, Permission/Media/Salon domains
- `staging`/`production` build behavior


## Migration Plan

1. Every developer machine currently relying on the compiled-in
   `10.0.2.2` default must add `DEV_API_BASE_URL` to their local
   `gradle.properties`/`local.properties` before their next `dev` build
   — either the emulator address or their own LAN IP, per target.
2. Remove the `?: "http://10.0.2.2:8080/"` fallback in
   `app/build.gradle.kts`'s `dev` flavor.
3. Add a git-ignored `local.properties.sample` documenting both recipes
   (emulator / physical device) with inline comments.
4. Validate `assembleDebug` across the full `target` × `environment`
   matrix (`customer`/`manager`/`reception` × `dev`/`staging`/
   `production`): confirm `dev` now fails without `DEV_API_BASE_URL` set
   and succeeds with either recipe supplied.
5. Re-run physical-device OTP login against a correctly configured LAN
   URL to confirm the original incident is resolved end-to-end.

No backend migration, no data migration, no API contract change.


## Failure Scenarios

- **Developer runs a `dev` build with no `DEV_API_BASE_URL` set**:
  build succeeds (compiles an empty string), app fails fast at first
  network call via `NetworkConfig.kt`'s existing `check()`, with a clear
  message naming the missing property — not a silent wrong address and
  not a 10-second timeout.
- **Physical device configured with the emulator recipe
  (`10.0.2.2`)**: unchanged from today — still a `SocketTimeoutException`
  at the network layer. This ADR does not eliminate that failure mode by
  itself; it eliminates the *silent, undocumented* version of it. Closing
  the remaining case (a developer picking the wrong recipe for their
  target) is deferred to the optional runtime guard under Future
  Evolution.
- **Physical device on cellular data, correct LAN URL configured**: still
  fails — expected and out of scope for this ADR. Reachability requires
  the device to be on the same Wi-Fi network as the backend host, or a
  real `staging` URL; this is an operational precondition the onboarding
  doc must state, not something build configuration can enforce.
- **Backend not running on the target host/port**: still fails — this
  ADR governs client-side URL *configuration*, not backend availability.


## Future Evolution

- **Emulator vs. physical-device runtime guard**: a debug-only check
  (e.g. comparing `Build.FINGERPRINT`/`Build.HARDWARE` against the
  configured host) that warns or blocks when a `10.0.2.2`-style host is
  detected on what looks like real hardware — closes the remaining
  "wrong recipe chosen" failure mode noted above. Not required for this
  ADR's acceptance.
- **CI matrix validation**: as the `target` × `environment` matrix grows
  with future apps (Specialist, Accountant, Inventory, per the Shared
  Premium Glass Design System roadmap), consider a CI check that fails
  a PR if any `environment` flavor reintroduces a compiled-in,
  context-specific default.
- **Staging environment activation**: `staging`'s `buildConfigField` is
  still `""` by design (no confirmed URL exists yet) — this ADR does not
  change that; activating `staging` for real use is a separate decision
  when a staging backend exists.
