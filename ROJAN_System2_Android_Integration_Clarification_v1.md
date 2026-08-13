# ROJAN System 2 → System 1: Android Integration Clarification v1.0

**Purpose:** Exact Android consumption requirements, for the System 1 Backend & Architecture Team. Clarification only — no code was modified, no feature implemented, no API created, no architecture changed to produce this document.

**Branch:** `feature/android-reception-app` (Android, `ROJAN_DesignLab`).

**Method:** Every claim below was verified directly against source — the Android client's actual Retrofit interfaces/DTOs (what it already sends/expects), and the backend's actual controllers on `origin/feature/auth-rate-limit-finalization` (the most current backend branch; `main` is a single-commit deployment snapshot with none of this). Where a backend doc comment on the Android side claimed a contract that turned out not to exist in the backend source, that discrepancy is called out explicitly rather than repeated as fact — this happened at least once (§2) and readers should treat every "✅ real" / "❌ missing" marker below as independently re-checked, not inherited from prior Android-side documentation.

---

## 1. Authentication Flow

### OTP Request

| | |
|---|---|
| Status | **✅ Real — implemented and verified on `origin/feature/auth-rate-limit-finalization`, field-for-field match with what Android sends/expects.** |
| Endpoint | `POST /api/v1/auth/otp/request` |
| HTTP method | `POST` |
| Auth | Public (no bearer token) |
| Request fields | `phoneNumber: String` (E.164 format, e.g. `+989123456789`) |
| Response fields | `phoneNumber: String`, `expiresInSeconds: Long`, `canResendAfterSeconds: Long` — `200 OK` |
| Errors | `400` (phoneNumber not valid E.164), `429` (rate-limited, per-phone or per-IP) |

### OTP Verify

| | |
|---|---|
| Status | **✅ Real — implemented and verified, field-for-field match.** |
| Endpoint | `POST /api/v1/auth/otp/verify` |
| HTTP method | `POST` |
| Auth | Public (this call *is* the credential exchange) |
| Request fields | `phoneNumber: String`, `code: String`, `fullName: String?` (optional — used only if this is a first-time verification that auto-registers the account) |
| Response fields | Full `AuthResponse` — see "Login success" below. `200 OK` |
| Errors | `401` (code invalid/expired/no matching request), `429` (too many verify attempts for this phone) |

### Login success

Required fields, all present in the real, verified `AuthResponse` shape (identical for `/otp/verify` and the pre-existing `/auth/login`):

- **`user`**: `{ id, email?, phoneNumber?, fullName, role }` — `email`/`phoneNumber` are each independently nullable (a phone-only OTP account has no email; an email/password account has no phone). `role` is one of `CUSTOMER | MANAGER | SPECIALIST` today — **no `RECEPTIONIST` value exists** (see §5 blockers).
- **`accessToken`**: `String` (JWT, short-lived — 15 min default per backend config)
- **`accessTokenExpiresAt`**: `String` (ISO-8601 timestamp)
- **`refreshToken`**: `String` (JWT, long-lived — 30 days default)
- **`refreshTokenExpiresAt`**: `String` (ISO-8601 timestamp)

Both tokens are required — Android's refresh flow (see "Logout/session expiration" below) depends on the refresh token being present in every successful login/verify/refresh response.

### Token storage

**Android-side mechanism (already built, works today, not a requirement on backend):** `SharedPreferences`, with each value AES-encrypted (Android Keystore–backed key, `TokenCipher`) before write and decrypted after read — never written to disk unencrypted. Session identity (`personId`) is stored separately via Jetpack DataStore Preferences. Documented here only so System 1 knows Android already has a secure storage answer and does not need to provide one (e.g., no requirement for the backend to issue short-lived opaque session identifiers instead of JWTs).

### Identity fields required

| Requested field | Current backend reality |
|---|---|
| `userId` | ✅ Present (`user.id` in `AuthResponse`) |
| Phone number | ✅ Present (`user.phoneNumber`, nullable) |
| Roles | ⚠️ Partial — `user.role` is a single global role (`CUSTOMER/MANAGER/SPECIALIST`). Per-salon roles (e.g. `RECEPTIONIST`) are a *separate* concept the client already models (`memberships[].role`) but has no backend endpoint to fetch (see §2). |
| Permissions | ❌ Missing entirely — no endpoint returns a permission set today. Android has a client-side `SalonPermissions` vocabulary (`MANAGE_BOOKINGS`, `VIEW_CRM`, etc.) it is prepared to consume, but nothing backend-side populates it. |
| Account status | ❌ Not exposed as a field anywhere. `UserResponse`/`AuthResponse` carry no `active`/`status` field. The only signal Android has today is an indirect one: a deactivated account's `/auth/login` fails with `403`, per the documented error-format convention — there is no equivalent for OTP verify's behavior against a deactivated account, and no way to check status without attempting a full login. |

### Logout/session expiration

**Expected/implemented Android-side behavior (documented for System 1's awareness, not a request):**
- **Access token expiry**: Android's `TokenAuthenticator` (an OkHttp `Authenticator`) intercepts any `401` response, exchanges the stored refresh token via `POST /auth/refresh`, retries the original request once with the new access token. Requires the backend to return `401` (not `403`) for an expired/invalid access token — confirmed this is already the backend's documented behavior.
- **Refresh token expiry/revocation**: if the refresh call itself fails, Android clears both stored tokens *and* the persisted session identity (`personId`) — a session is never left in a state where the app looks logged-in locally after the backend has genuinely revoked it.
- **Explicit logout**: clears tokens, clears persisted `personId`, clears persisted active-salon selection. No backend logout/revocation endpoint is called or required — the backend has no server-side session/token revocation mechanism today (a stolen refresh token remains valid until natural expiry; this is a known, previously-documented backend gap, not something this report is newly raising).

---

## 2. Salon Access Context

**Required flow**, exactly as specified:
```
OTP → Identity → Salon Access → Dashboard
```

**Status: ❌ The "Salon Access" step is entirely missing backend-side.** This is the single most important finding in this report.

Android's `GET /api/v1/users/me/salon-access` call (via `AuthApi.getSalonAccess()`) expects:

```
SalonAccessResponse {
  ownedSalons:    [{ salonId, salonName, active, permissions }]
  memberships:    [{ membershipId, salonId, salonName, active, role, permissions }]
  specialistLinks:[{ specialistId, salonId, salonName, active, permissions }]
}
```

**Verified this endpoint does not exist** on `origin/feature/auth-rate-limit-finalization` (`UserController` has only `GET /users/me`) or on `main`. **One prior Android-side doc comment claimed this DTO "mirrors `ROJAN_Backend/api/.../user/SalonAccessDtos.kt` exactly"** and separately claimed a `SalonPermissionResolver` application class exists — both re-checked directly against backend source across every branch and **confirmed false**. Flagging this specifically so System 1 does not assume any part of this contract is already staged backend-side in a form Android is merely waiting to be pointed at — nothing exists.

### Required Android data (per entry, all three lists)

- `salonId`, `salonName` — for display
- `membershipId` (memberships only) — the row identity, needed for later revoke/update actions
- `role` (memberships only) — currently expected as a raw string (`"MANAGER"` / `"RECEPTIONIST"`, per §5)
- `permissions` — a `Set<String>` per entry, server-resolved, never re-derived client-side. Android treats an unrecognized permission value as "grants nothing" (fails safe), so a permission string added backend-side that Android doesn't yet know about is safe by construction, not a breaking change.
- `active` (membership status) — a `false` entry is excluded from selectable salons entirely, not shown greyed-out.

### Single-salon behavior

Exactly one active entry across `ownedSalons + memberships + specialistLinks` (deduped by `salonId`, precedence Owner > Member > Specialist if the same salon appears via more than one relation) → auto-selected, persisted locally, no prompt shown.

### Multiple-salon behavior

More than one active entry, and no previously-persisted selection still present among them → a selection screen is shown; user must pick explicitly.

### Active salon selection

- A previously-persisted `salonId` that is still among the currently-available active entries → resolved silently, no prompt.
- Selection is purely a client-side persisted preference (`salonId` only, re-validated against a fresh `/salon-access` response every login/cold-start) — **no backend endpoint or field for "current active salon" is required.** The JWT deliberately carries no `salonId` claim; every subsequent request resolves salon context from the path, not the token.

---

## 3. Reception Invite Flow

**Status: ❌ Nothing exists — no backend, no Android code, not even a confirmed design decision.**

This section is written as **Android's requirement specification**, not a description of anything built or agreed. Whether "invite" is even the chosen mechanism (versus an owner directly assigning a role to an existing account by phone/email) is an **open decision System 1 has not yet made** — see §5/§6.

**One concrete clue already present in backend source, worth System 1's attention:** `SecurityConfig.kt` (`origin/feature/auth-rate-limit-finalization`) already reserves a public route pattern for `GET /api/v1/invites/{token}` and `POST /api/v1/invites/{token}/accept` — but **no `InviteController`, no invite domain type, and no invite persistence exist anywhere in the backend.** This is either a forgotten stub for exactly this flow, or dead configuration that should be removed. Either way, it's the only existing signal about which direction was originally intended, and System 1 should resolve it explicitly rather than leave it unexplained.

### Flow (as specified)

```
Invite received → Open invite → Accept invite → Membership activated → Dashboard access
```

### Required screens (Android, not yet built)

1. Invite entry point — how the invite reaches the device is itself undecided (deep link? SMS with a code the user types in? in-app "enter invite code" field?). This decides whether Android needs deep-link handling in the manifest at all.
2. Invite preview/confirmation — showing what's being accepted (salon name, role) before commit.
3. Accept-in-progress / result state (success or failure).

### Required API sequence (as Android would need it, none of it real today)

1. `GET /api/v1/invites/{token}` (public, per the reserved route pattern) — returns invite details for display before acceptance: salon name, offered role, expiry, whether it's already been accepted/revoked.
2. `POST /api/v1/invites/{token}/accept` (per the reserved route pattern — this one is **not** in the public matcher list, i.e. requires an authenticated caller) — the accepting user must already be logged in (their own account, resolved via their bearer token) for this to make sense as "activate MY membership."

### Invite states (proposed, for System 1 to confirm or replace)

`PENDING` (issued, not yet acted on) → `ACCEPTED` (membership now active) / `EXPIRED` / `REVOKED` (owner cancelled it before acceptance) / `ALREADY_USED` (re-opening an already-accepted invite link).

### Success response (proposed shape, mirroring existing DTO conventions)

```
{ membershipId, salonId, salonName, role, active: true }
```

### Failure states Android needs distinguishable error codes for

- Invalid/unknown token
- Expired invite
- Already-accepted invite (re-open, not an error exactly — should probably succeed idempotently or return a clear "already active" signal, not a generic 400/404)
- Invite revoked by the owner
- Accepting user's phone/account doesn't match whatever the invite was issued for (if invites are phone-scoped — undecided)

**None of this can be scoped further until System 1 decides whether invites are the chosen mechanism at all.**

---

## 4. Booking UI Data Requirements

### Booking list

**Required display fields, mapped against the real, verified `BookingResponse`:**

| Required | Backend reality |
|---|---|
| `bookingId` | ✅ `id` |
| Date/time | ✅ `startTime`, `endTime` (ISO-8601 local date-time) |
| `status` | ✅ `PENDING \| CONFIRMED \| CANCELLED \| COMPLETED` |
| Customer basic identity | ❌ **Only `customerId` (a raw UUID) is returned — no name, phone, or any display value.** |
| Service information | ❌ **Only `serviceId` (a raw UUID) — no name, duration, or price.** |
| Specialist information | ❌ **Only `specialistId` (a raw UUID) — no display name.** |

**This is a real, concrete requirement gap, not a permissions issue:** even with full authorization, a Reception booking list built directly against today's `BookingResponse` would show three raw UUIDs and a time range — not a usable staff-facing screen. Two ways to close this, for System 1 to choose between:
1. Enrich `BookingResponse` with nested summary objects (`customer: { id, name, phone }`, `service: { id, name }`, `specialist: { id, name }`) — one call, no N+1.
2. Leave `BookingResponse` as-is and require Android to make additional lookup calls per booking (`GET .../customers/{id}`, `GET .../services/{id}`, `GET .../specialists/{id}`) — works today for owner-authenticated callers (endpoints already exist), but is real N+1 request overhead for a list screen, and is itself blocked for Reception until §5's authorization broadening lands.

**Recommendation (Android's stated preference, not a decision System 1 owes to us):** option 1. It's strictly less total request volume and avoids partial-failure states (booking loads, but its customer name lookup fails).

### Booking actions

| Action | Endpoint | Status | Request fields | Response fields | Permission dependency |
|---|---|---|---|---|---|
| Confirm | `PATCH /api/v1/bookings/{bookingId}/confirm` | ✅ Exists backend-side, per `API_CONTRACT.md` — **but zero Android client binding exists anywhere** (neither Customer's `BookingApi.kt` nor Manager's `ManagerBookingApi.kt` implements it) | None (empty body) | `BookingResponse` | Owner only today; Reception needs `MANAGE_BOOKINGS` once membership authorization exists (§5) |
| Cancel | `PATCH /api/v1/bookings/{bookingId}/cancel` | ✅ Exists, ✅ **already has an Android client binding** (`BookingRepository.cancelBooking`) | None (empty body) | `BookingResponse` | "Customer or owner" today; Reception needs "... or salon member with `MANAGE_BOOKINGS`" |
| Complete | `PATCH /api/v1/bookings/{bookingId}/complete` | ✅ Exists backend-side — **zero Android client binding exists**, same gap as Confirm | None (empty body) | `BookingResponse` | Owner only today; same broadening needed as Confirm |
| Create for customer | `POST /api/v1/salons/{salonId}/bookings` | ✅ Exists, ✅ has an Android client binding (`ManagerBookingApi.createForCustomer`) | `customerId, serviceId, specialistId, startTime, notes?` | `BookingResponse` | Owner only today (its own KDoc already says "Reception/owner only" — aspirational, not yet enforced); needs `MANAGE_BOOKINGS` broadening |
| List (salon-scoped) | `GET /api/v1/salons/{salonId}/bookings` | ✅ Exists, ✅ has a binding | — | Paginated `BookingResponse[]` | Owner only today; needs `MANAGE_BOOKINGS` (or a narrower read variant — undecided, see §5) |

**Net for System 1:** Confirm/Complete need new Android Retrofit bindings regardless of the authorization decision (pure client-side gap, no backend action needed there) — listed here so it's not mistaken for a backend blocker. Every action's *authorization* is the real, shared backend blocker.

---

## 5. Current Android Branch Status

### Reception

| | |
|---|---|
| Flavor name | `reception` |
| `applicationId` | `ai.rojan.designlab.reception` |
| Build variants | 6 total — flavor dimensions `target=reception` × `environment={dev,staging,production}` × build type `{debug,release}` (e.g. `assembleReceptionDevDebug`, `assembleReceptionProductionRelease`) |

**Implemented screens (real, backend-connected, build-verified):**
- Splash
- OTP Auth (phone entry → code entry, real `/otp/request`/`/otp/verify` calls)
- Salon Selection (real `/salon-access`-driven, currently unreachable in practice — see below)
- Access-Error (new: the resolved terminal state for a `/salon-access` failure — message, retry, logout)
- Dashboard — **placeholder only**, no real salon-scoped data, exists to give the auth flow a real end destination
- Profile (real name/phone display, real logout)

**Blocked screens (not started, per the approved implementation plan's phased order):**
- Calendar (daily/weekly)
- Booking wizard (create/review/confirm)
- Customer search/profile

### Current blockers

**Missing backend endpoints:**
1. `GET /api/v1/users/me/salon-access` (§2)
2. `SalonMembershipController` — `GET/PUT/DELETE /api/v1/salons/{salonId}/members/{userId}` (no endpoint exists to grant a phone number `RECEPTIONIST` access at all)
3. Any invite-related endpoint (§3), if invites are the chosen mechanism instead of #2

**Missing DTO contracts:**
1. A real backend `Permission`/permission-resolution model — Android's `SalonPermissions` vocabulary (`MANAGE_SALON, MANAGE_MEMBERSHIP, MANAGE_CATALOG, MANAGE_STAFF, MANAGE_SCHEDULE_ALL, MANAGE_SCHEDULE_OWN, VIEW_CRM, MANAGE_CRM, MANAGE_BOOKINGS, MANAGE_OWN_BOOKINGS`) has nothing backend-side populating it
2. Enriched booking response for list display (§4)

**Missing authorization decisions:**
1. Global role vs. membership-scoped: does Reception need a new `UserRole.RECEPTIONIST` value, or do reception staff hold `MANAGER`-role accounts scoped down purely by salon-membership permissions? Android's current OTP gate (`ReceptionAuthViewModel`) reuses the `MANAGER` check as an explicitly-marked **provisional placeholder** pending this decision.
2. Per-membership permission bundles: fixed by role, or independently settable per assignment?
3. CRM write access for Reception (`MANAGE_CRM` vs. read-only `VIEW_CRM`)
4. Whether booking check-in needs a new `BookingStatus` value

**Practical consequence, verified end-to-end this phase:** a real OTP login today succeeds (§1 is real), then deterministically fails at salon-access resolution and lands on the new Access-Error screen. This is now the *correct, non-broken* behavior — previously it was an infinite hang — but it means **no account can reach the Reception Dashboard with real data today**, regardless of role.

---

## 6. Recommendation to System 1

### Minimum backend contracts required (in the order that unblocks the most Android work per item)

1. **`GET /api/v1/users/me/salon-access`** — unblocks identity resolution for every authenticated screen, even before membership-granting exists (an account with zero memberships still needs this to return an honest empty/owned-only result instead of 404).
2. **`SalonMembershipController`** (or the invite flow, per whichever §3/§6 decision is made) — unblocks actually creating a receptionist that isn't the owner.
3. **Authorization broadening** on `SalonBookingController`, `CustomerController`, and the availability/schedule controllers — from strict `ownerId == callerId` to also accept an active membership with the relevant permission. Unblocks every operational screen at once.
4. **Booking response enrichment** (§4) or an explicit decision to require N+1 client-side lookups instead.
5. **Confirm/Complete Android bindings** — not backend work, listed for completeness: these can be added client-side the moment #3 lands, no further backend action needed.

### Dependencies blocking Android Phase 1

Everything past Splash/OTP is blocked on #1-#3 above. §5's four authorization decisions block #1-#3 from being scoped precisely — they are the actual critical path, not the code itself.

### Order of implementation after approval

1. Decide §5's open questions (global role model, permission bundle model, CRM/check-in scope) — product/architecture decisions, not engineering work, and nothing below can be scoped correctly without them.
2. `GET /users/me/salon-access` (item 1 above) — smallest, most-unblocking single endpoint.
3. Membership-granting mechanism, per whichever direction §3/decision-1 settles on.
4. Authorization broadening across the operational controllers (item 3 above) — can proceed in parallel with #3 once the permission vocabulary itself (decision-2) is fixed.
5. Booking response enrichment (item 4) — independent of the above, can be done in parallel at any point.

---

**End of report. No code changes, no commits, no pushes were made in producing this document.**
