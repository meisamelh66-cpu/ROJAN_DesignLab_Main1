# ACTIVE SALON CONTEXT ROOT CAUSE REPORT v1

**Scope:** System 2 (Android) — investigation only. No code modified, no commit, no push, no deploy, no data created.
**Date:** 2026-08-21
**Trigger:** Dashboard showed "Salon Royan" (`سالن رویان`) after explicitly selecting "ROJAN AI Pilot Salon" on the Salon Selection screen.
**Method:** Static source read + on-device runtime verification (persisted DataStore file pulled via `adb run-as`, live `logcat` OkHttp request/response lines from this session's own traffic — no new requests triggered for this investigation).

---

## 1. Where Manager Dashboard gets salon identity data

`app/src/main/java/ai/rojan/designlab/manager/screens/dashboard/ManagerDashboardScreen.kt:117-124`

```kotlin
item {
    val salon = ManagerRepositories.salon
    if (salon != null) {
        SalonIdentityCard(salonName = salon.name, salonCategory = salon.description, isActive = salon.active)
    } else {
        SalonIdentityCard()
    }
}
```

`ManagerRepositories.salon` is populated by `ManagerRepositories.initialize()`, triggered once per Dashboard entry via:

```kotlin
LaunchedEffect(Unit) {
    ManagerRepositories.initialize(context)
    refreshKey++
}
```
(`ManagerDashboardScreen.kt:103-106`)

## 2. Active salon ID stored in Android state

`ActiveSalonContextRepository` (`domain/repository`), implemented by `ActiveSalonContextRepositoryImpl` (`data/repository/ActiveSalonContextRepositoryImpl.kt`), backed by Preferences DataStore `active_salon_preferences` (`data/local/ActiveSalonDataStore.kt`), key `active_salon_id`.

**Verified directly on-device** (file pulled via `adb shell run-as ai.rojan.designlab.manager cat files/datastore/active_salon_preferences.preferences_pb`):

```
active_salon_id = 165958d6-1762-4520-a93f-c92fc1ba6cf9
```

## 3. API request used by Dashboard

`ManagerRepositories.initialize()` (`manager/data/ManagerRepositories.kt:213-284`) reads the persisted id and calls:

```kotlin
val activeSalonId = container.activeSalonContextRepository.observeActiveSalonId().first()
val salonDto = container.salonApi.getSalon(activeSalonId)
```

`SalonApi.getSalon` (`data/remote/SalonApi.kt:20-21`):
```kotlin
@GET("api/v1/salons/{salonId}")
suspend fun getSalon(@Path("salonId") salonId: String): SalonResponseDto
```
→ `GET /api/v1/salons/{salonId}`. Deliberately not `GET /api/v1/salons/mine` (that endpoint is owner-only and returns empty for a membership-based manager — see this same file's own doc comment, added in `717dff4`).

## 4. Whether requested salonId matches `165958d6-1762-4520-a93f-c92fc1ba6cf9`

**Yes — confirmed twice from this session's own `logcat` (OkHttp `Level.BASIC`, `BackendApiContainer.kt:281-282`), no new request triggered for this check:**

```
08-21 18:03:28.502 --> GET https://api.rojanai.ir/api/v1/salons/165958d6-1762-4520-a93f-c92fc1ba6cf9
08-21 18:03:28.804 <-- 200 https://api.rojanai.ir/api/v1/salons/165958d6-1762-4520-a93f-c92fc1ba6cf9 (300ms)
...
08-21 18:06:43.284 --> GET https://api.rojanai.ir/api/v1/salons/165958d6-1762-4520-a93f-c92fc1ba6cf9
08-21 18:06:43.762 <-- 200 https://api.rojanai.ir/api/v1/salons/165958d6-1762-4520-a93f-c92fc1ba6cf9 (476ms)
```

Both calls match the DataStore-persisted id exactly and both returned `200`. The subsequent `/categories`, `/specialists` calls in the same burst also correctly used the same salon id, and `/specialists` is what populated the Calendar screen's correctly-shown `Pilot Specialist` filter — independent confirmation the right salon was in play for that request chain.

(For contrast: an earlier burst at `17:39`/`17:42`, from the pre-existing session before logout, used a *different* id, `c5d20a00-5139-4e8a-b811-3892e02cfa69` — consistent with a different salon being active at that time, not a bug in itself.)

## 5. Backend response salon identity

`Level.BASIC` logging doesn't capture response bodies, so the JSON `name` field itself isn't in the log — but the request/response pair is unambiguous: `GET /api/v1/salons/165958d6-...` → `200 OK`, and the sibling `/specialists` call for the same salon id in the same call chain correctly surfaced "Pilot Specialist" data on-screen. There is no evidence of a backend-side mismatch — the request layer asked for the right salon and got a successful response for it.

---

## Root Cause

**The Dashboard's salon-identity `item {}` block never re-reads `ManagerRepositories.salon` after it's populated.**

`ManagerRepositories` (`manager/data/ManagerRepositories.kt:127-142`) is a plain Kotlin `object` singleton; `salon`/`salonId`/etc. are ordinary `var` properties — **not** `State`, `StateFlow`, or anything else Compose can observe. The Dashboard's salon-identity `item {}` (`ManagerDashboardScreen.kt:117-124`) reads `ManagerRepositories.salon` as a one-shot value with no Compose-observed input. Contrast with the sibling items in the same `LazyColumn`:

```kotlin
item { TodayOverviewSection(refreshKey = refreshKey) }        // reads refreshKey → recomposes
item { CalendarPreviewSection(slots = remember(refreshKey) {...}) } // reads refreshKey → recomposes
item { val salon = ManagerRepositories.salon; ... }            // reads NOTHING observable → never recomposes
```

`refreshKey` (a `mutableIntStateOf`, incremented after `initialize()` completes) is exactly the mechanism the other two items use to pick up the post-fetch state — the salon-identity item was left out of that pattern. Sequence on a real Dashboard entry:

1. First composition pass runs **synchronously**, before `LaunchedEffect`'s coroutine has had a chance to run. `ManagerRepositories.salon` is whatever it was left as previously (frequently still `null`, or a stale value from a previously-active salon within the same still-alive process — see below).
2. Since `salon` is `null` at that first read, the `else` branch renders `SalonIdentityCard()` with **no arguments** — which resolves to `SalonIdentityCard.kt:42-46`'s hardcoded defaults, written for the component's own `@Preview`, not as a real fallback:
   ```kotlin
   fun SalonIdentityCard(
       salonName: String = "سالن رویان",
       salonCategory: String? = "آرایش و زیبایی بانوان",
       isActive: Boolean = true,
       ...
   )
   ```
3. `initialize()` finishes shortly after and correctly sets `ManagerRepositories.salon` to the real "ROJAN AI Pilot Salon" data (confirmed by §3-5 above) and bumps `refreshKey` — but the salon-identity `item {}` scope has no dependency on `refreshKey` (or on `salon` as an observable), so Compose has no reason to re-invoke it. The placeholder text from step 2 keeps showing indefinitely, silently, with no error and no visual sign it's stale.

This is a plain data-race-with-Compose-semantics bug, not a network/backend/DTO issue — §3-5 confirm the request/response side is correct.

**Compounding factor (why "میاه"/"روژ" were seen at other points, not just the placeholder):** `ManagerRepositories` is process-wide and is **never reset on logout** — `ManagerAuthViewModel.clearSession()` clears tokens and persisted ids but never touches `ManagerRepositories`. If the identity item's scope *does* happen to recompose for an unrelated reason (e.g. a parent recomposition from navigating back into a fresh `ManagerDashboardScreen` instance) at a moment when the singleton holds a different salon's data than whatever DataStore currently says is active, the card will show that stale salon instead — explaining the earlier "میاه" (from `ManagerSalonSetupScreen`, a separate screen/bug, see below) and "روژ" observations from this session.

**Separate, secondary, already-documented issue (not this bug, but same symptom family):** `ManagerSalonSetupScreen` ("تنظیمات" → edit salon) sources its data from `ManagerSalonRepository.getMySalon()`, which calls the owner-only `GET /salons/mine` and takes `.firstOrNull()` — completely independent of `ActiveSalonContextRepository`. For this account (owns `میاه` and `روژ`, member of `ROJAN AI Pilot Salon`), that screen can never reflect whichever salon is actually active; it always shows whichever salon the backend lists first for `mine`. This was already flagged as a known limitation in `ROJAN_PhaseA_Salon_Identity_Continuation_State_v1.md` §5 ("Single-salon assumption") — not new, and a separate fix surface from the Dashboard bug above.

---

## Recommended Fix (not implemented)

1. **Primary:** Make `ManagerRepositories.salon` (and ideally the other snapshot properties it exposes) Compose-observable — e.g. back it with a `StateFlow<ManagerSalonSummary?>` (or at minimum have `ManagerDashboardScreen`'s salon-identity `item {}` key off `refreshKey` the same way its sibling items already do, as the smallest possible patch). The `StateFlow` route is more robust since it also fixes any other current/future call site that reads `ManagerRepositories.salon` expecting it to be live.
2. **Guard the fallback:** `SalonIdentityCard()`'s no-arg defaults (`سالن رویان` / `آرایش و زیبایی بانوان`) should not be reachable from `ManagerDashboardScreen`'s real "salon is null / still loading" branch — that branch should use an explicit loading/empty state (e.g. a skeleton or "در حال بارگذاری…" label) so a genuinely-unloaded state is never visually indistinguishable from real data for a salon that happens to share attributes with the preview defaults.
3. **Reset `ManagerRepositories` on logout:** `ManagerAuthViewModel.clearSession()` should clear `ManagerRepositories`'s cached snapshot (salon/services/appointments/etc. back to their empty defaults) so a later `initialize()` for a different account/salon can never be preceded by a visible frame of the previous session's stale data.
4. **Separate follow-up (not blocking this fix):** `ManagerSalonSetupScreen`/`ManagerSalonRepository.getMySalon()` should resolve via `ActiveSalonContextRepository`'s active salon id (mirroring `ManagerRepositories.initialize()`'s approach) rather than `GET /salons/mine` + `.firstOrNull()`, so it's consistent for multi-salon owners. Tracked as a pre-existing, already-documented limitation — not part of this root cause, listed here only because it produces a visually identical symptom (wrong salon name shown).

---

*No code changes, no backend changes, no commits, no pushes, no deployment, no data created in producing this report. On-device evidence gathered via read-only `adb run-as` file pull and `logcat -d` (existing buffer only — no new network requests triggered).*
