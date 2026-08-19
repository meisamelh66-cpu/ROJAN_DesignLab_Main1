# ADR-003 Validation Report — Environment Configuration Strategy

Companion evidence record for
[`ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md`](ADR-003_ENVIRONMENT_CONFIGURATION_STRATEGY.md).
Documents the original incident and the physical-device evidence gathered
after implementation, closing the loop from investigation → decision →
implementation → validation.


## Status

ADR-003 validated.


## 1. Original Incident

**Symptom.** Manager APK (commit `7842808`) installed on a physical
Samsung Galaxy A72 (`RZ8R81WPS2J`) failed phone-number login at the
OTP-request step with:

> "Connection timed out. Please try again."

**Root cause — `10.0.2.2` is Emulator-only.** The `dev` environment
flavor's `buildConfigField` compiled in a hardcoded default:
```kotlin
"\"${project.findProperty("DEV_API_BASE_URL") ?: "http://10.0.2.2:8080/"}\""
```
`10.0.2.2` is the Android Emulator's fixed alias for the host machine's
`localhost` — it has no meaning outside an emulator's virtual NAT. On a
physical device it is simply an unreachable address.

**Galaxy A72 timeout evidence** — live logcat captured directly from the
device during the original investigation:
```
08-19 08:03:25.448 I/okhttp.OkHttpClient( 4933): --> POST http://10.0.2.2:8080/api/v1/auth/otp/request (31-byte body)
08-19 08:03:35.470 I/okhttp.OkHttpClient( 4933): <-- HTTP FAILED: java.net.SocketTimeoutException:
    failed to connect to /10.0.2.2 (port 8080) from /25.113.58.65 (port 36378) after 10000ms
```
The socket connect attempt sat until OkHttp's default 10-second connect
timeout expired — the app never reached the backend at all. This was
confirmed as purely client-side: the backend had started and served OTP
requests successfully in prior local runs; nothing on the server side was
at fault.


## 2. Resolution

Per ADR-003's decision, the compiled `10.0.2.2` fallback was removed
entirely — no environment ships a default that only resolves in one
runtime context. `dev` now reads `DEV_API_BASE_URL` exclusively from the
developer's own git-ignored `local.properties`, parsed explicitly in
`app/build.gradle.kts` (Gradle does not auto-expose `local.properties`
keys the way it does `gradle.properties`, so this required its own
`Properties()` parsing block, mirroring the existing `keystore.properties`
pattern). `NetworkConfig.kt`'s existing fail-loud `check()` — already used
by `staging`/`production` — now applies identically to `dev`.

**`local.properties` `DEV_API_BASE_URL` (this validation):**
```
DEV_API_BASE_URL=http://192.168.179.199:8080/
```
— the dev machine's own Wi-Fi LAN IP at the time of testing, not a
guessed or compiled-in value. `local.properties.sample` documents both
supported recipes (Emulator: `10.0.2.2`; physical device: the developer's
own LAN IP) for future onboarding.

**Physical-device LAN configuration requirement.** Reaching a LAN IP
requires the device and the backend host to be on the same Wi-Fi network
(the device's outbound source IP, `192.168.179.246`, was verified on the
same `/24` subnet as the backend host's `192.168.179.199` earlier in this
validation). Same-network LAN configuration enabled successful
Android-to-backend communication during validation — this same subnet
condition held during an earlier attempt in this same validation that
still failed with a
`SocketTimeoutException`, because the backend was not yet running at that
point. ADR-003 resolved the emulator-only endpoint issue by introducing
explicit physical-device endpoint configuration; it did not by itself
guarantee connectivity. The final successful validation depended on all
of the following being true together:
- Correct Android configuration (`DEV_API_BASE_URL` pointing at a
  reachable host)
- Backend availability (PostgreSQL + Redis + Spring Boot actually
  running on port 8080)
- Network reachability (device and backend host on the same Wi-Fi
  network)
- SMS provider response (the external gateway accepting and dispatching
  the OTP)

**Debug-only cleartext policy.** `network_security_config.xml` was
broadened from an enumerated host whitelist (`10.0.2.2`/`localhost`/
`127.0.0.1`) to a debug-only `<base-config cleartextTrafficPermitted=
"true" />`, since a developer's LAN IP is DHCP-assigned and cannot be
statically enumerated. Release builds remain cleartext-blocked
everywhere, unaffected by this file (debug source set only).


## 3. Runtime Evidence

**Build.** `:app:assembleManagerDevDebug` — `BUILD SUCCESSFUL`, with
`DEV_API_BASE_URL` resolved from `local.properties` and no compiled
`10.0.2.2` fallback present.

**Installation.** `adb install -r` on `RZ8R81WPS2J` (Galaxy A72,
`SM_A725F`) — `Success`.

**Fail-loud behavior confirmed** (prior to `local.properties` being
configured): the app correctly crashed with a clear, actionable error
rather than a silent wrong address —
```
java.lang.IllegalStateException: API_BASE_URL is not configured for the "managerDev" flavor.
Set DEV_API_BASE_URL in local.properties (see local.properties.sample) for dev, or
STAGING_API_BASE_URL/PRODUCTION_API_BASE_URL (gradle.properties or -P) for staging/production
- before building this environment. See app/build.gradle.kts.
```

**Login E2E — full positive path**, backend running (PostgreSQL + Redis
+ Spring Boot on port 8080), device on the same Wi-Fi network:

- Request: `POST http://192.168.179.199:8080/api/v1/auth/otp/request`
- Android (`OkHttpClient`), device-side:
  ```
  10:58:05.051 I/okhttp.OkHttpClient(15994): --> POST http://192.168.179.199:8080/api/v1/auth/otp/request (31-byte body)
  10:58:09.978 I/okhttp.OkHttpClient(15994): <-- 200 http://192.168.179.199:8080/api/v1/auth/otp/request (4926ms, unknown-length body)
  ```
- Backend, server-side (same request, correlated by recipient/timing):
  ```
  2026-08-19T10:58:09.927+03:30 INFO ... r.b.i.s.MeliPayamakSharedPatternProvider :
      provider=melipayamak-shared requestId=b36e27e3-d75e-4380-bf47-fcb5de2541be
      recipient=09164987585 status=200 elapsedMs=4839 outcome=sent
  ```
- App behavior: transitioned cleanly to the OTP code-entry screen
  ("کد ارسال‌شده به +989164987585 را وارد کنید") — no crash, no error
  banner, resend/edit-number affordances present and functional.

**Comparison with the original incident:**

| | Original incident | This validation |
|---|---|---|
| Target | `http://10.0.2.2:8080/` (Emulator-only) | `http://192.168.179.199:8080/` (configured LAN IP) |
| Backend contacted? | Never — TCP connect never completed | Yes — full request/response cycle |
| Result | `SocketTimeoutException` after 10000ms | `200 OK` after 4926ms |
| User-visible outcome | "Connection timed out. Please try again." | Advanced to OTP code-entry screen |


## 4. Final Status

**ADR-003 validated.** The environment configuration strategy was
implemented as decided, and the original physical-device login timeout
is confirmed resolved — not only structurally (no `10.0.2.2` compiled
into any build) but demonstrated end-to-end on the actual reporting
device: real request, real backend processing, SMS dispatch confirmed
(provider `outcome=sent`), real UI progression to the next login step.
Handset-side receipt of the SMS was not independently verified — this
report confirms the provider accepted and dispatched it, not that it was
read from the device's inbox.
