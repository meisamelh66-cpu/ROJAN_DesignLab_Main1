# ROJAN Reception Implementation Plan v1

**Scope:** Plan only, per instruction — no code written. Branch `feature/android-reception-app` created from `feature/manager-backend-integration` @ current HEAD (42 commits ahead of `origin/main`, which has none of the RBAC/membership scaffolding below — confirmed the correct baseline to branch from).

**Baseline verified against:**
- Android: `ROJAN_DesignLab` (this repo), branch `feature/manager-backend-integration`.
- Backend: `ROJAN_Backend`, **not** local `main` (a single-commit deployment snapshot with no OTP/CRM/reception work at all) but `origin/feature/auth-rate-limit-finalization` — the branch that actually contains OTP auth, Customer CRM, and the Reception booking-for-customer endpoint. This is the backend baseline every estimate below assumes. It is not merged to `main` and not checked out locally; treat it as reference-only until confirmed deployed.

---

## 1. Headline finding: "System 1 RBAC & Architecture Contract v1.0" is client-side only

This is the single fact that should drive sequencing, so it's stated first, not buried.

The Android app already has a complete, coherent RBAC **data contract** built and presumably approved as System 1:

- `domain/repository/CurrentUserIdentityContext.kt` — `ownedSalons` / `memberships` / `specialistLinks`, each carrying a `permissions: Set<String>`.
- `domain/repository/CurrentUserIdentityContext.kt`'s `SalonPermissions` object — `MANAGE_BOOKINGS`, `MANAGE_OWN_BOOKINGS`, `VIEW_CRM`, `MANAGE_CRM`, `MANAGE_SCHEDULE_OWN`, `MANAGE_SCHEDULE_ALL`, `MANAGE_STAFF`, `MANAGE_CATALOG`, `MANAGE_MEMBERSHIP`, `MANAGE_SALON` — documented as mirroring a backend `ai.rojan.backend.domain.salon.Permission` enum.
- `manager/domain/membership/SalonMember.kt` — `SalonMemberRole.{MANAGER, RECEPTIONIST}`.
- `manager/domain/repository/SalonMembershipRepository.kt` + `BackendSalonMembershipRepository` + `SalonMembershipApi` + `SalonMembershipDtos.kt` — full list/assign/remove data layer, wired into `di/BackendApiContainer.kt`.
- `data/repository/CurrentUserIdentityContextRepositoryImpl.kt` — calls `GET /api/v1/users/me/salon-access`.

**Verified against the actual backend source (both `main` and `origin/feature/auth-rate-limit-finalization`), none of this exists server-side:**

| Client expects | Backend reality |
|---|---|
| `GET /api/v1/users/me/salon-access` | Does not exist. Only `GET /api/v1/users/me` (`UserController.kt`) exists. |
| `GET/PUT/DELETE /api/v1/salons/{salonId}/members/{userId}` (`SalonMembershipController`) | Does not exist. No controller, no domain type, anywhere in the repo. |
| `ai.rojan.backend.domain.salon.Permission` | Does not exist. No such class/enum in the backend. |
| `UserRole` / `SalonMemberRole` including a distinct `RECEPTIONIST` identity | Backend `UserRole` enum (`domain/user/User.kt`) is only `CUSTOMER, MANAGER, SPECIALIST` — no receptionist value anywhere. |
| Reception can call booking/customer endpoints on a salon it doesn't own | Every relevant endpoint enforces `salon.ownerId == callerId` literally — `SalonBookingController.list/createForCustomer`, every method in `CustomerController`, availability/schedule controllers. `SalonBookingController.createForCustomer`'s own KDoc says *"Reception/owner only"* but the actual check is owner-only; there is no reception path at all. |

This is also stated explicitly, backend-side, in `ROJAN_Reception_Booking_Backend_Implementation_Report_v1.md` §7.4 (on the `auth-rate-limit-finalization` branch): *"A distinct Reception/Staff role — authorization here is still ownership-based ... 'Reception' continues to mean 'authenticated as the owner.'"*

**Consequence for this plan:** the Reception app can be fully built, navigable, and demoed against local/mock data immediately. It **cannot** be logged into as a real, distinct receptionist — with its own audit trail, its own revocable access, scoped to only booking/CRM permissions — until the three backend items in §4 ship. Building the UI now and wiring auth last is deliberate sequencing, not a shortcut.

---

## 2. Existing architecture verified reusable (no new mechanics needed)

**Compose / module structure** (`app/build.gradle.kts`):
- Two flavors today on `flavorDimensions("target")`: `customer` (zero overrides, `applicationId = ai.rojan.designlab`) and `manager` (`applicationId = ai.rojan.designlab.manager`, own manifest/Activity/launcher icon in `src/manager/`). Both compile the same shared `src/main` package — nothing duplicated.
- `flavorDimensions("environment")`: `dev`/`staging`/`production`, each with its own `API_BASE_URL` — applies automatically to any new `target` flavor added.
- CLAUDE.md's own "Shared Premium Glass Design System" section already names this: *"Every ROJAN app (Manager, Customer, and future Specialist/**Reception**/Accountant/Inventory apps) renders every UI mechanic ... through one shared engine. The only thing allowed to differ per app is the color palette"* — and explicitly scopes future-app work as *"new `RojanAppPalette` instance + root provider, zero new glass/border/button/card/icon code."* Reception is a named, pre-approved case in the frozen baseline, not a new precedent.

**Design system** (`ui/theme/RojanAppPalette.kt`): `ManagerPalette`/`CustomerPalette` are the only two instances; adding `ReceptionPalette` is the entire palette-layer task — every glass/shadow/typography/spacing mechanic (`PremiumGlassSurface`, `RojanShadows`, `RojanTypography`, `RojanDimens`) is shared and untouched.

**Auth layer** (Manager's, to be mirrored, not the older Customer demo mock):
- `manager/presentation/auth/ManagerAuthViewModel.kt` — real OTP flow (`otp/request` → `otp/verify`), gates on `user.role == "MANAGER"` client-side after verification, persists session via `AuthSessionRepository`/`TokenRepository` (encrypted, `AuthInterceptor`/`TokenAuthenticator` already handle silent refresh).
- Note: `domain/identity/PersonRole.kt` (`OWNER, GENERAL_MANAGER, RECEPTION, FINANCE, HR, SPECIALIST, CUSTOMER`) and `domain/identity/IdentityProvider.kt` are the **older, superseded demo/mock identity system** — its own KDoc confirms `personByPhone`/mock OTP callers were already removed once "real backend OTP replaced the mock flow." Do not build Reception auth on this; it has no backend behind it at all (not even the partial state of §1). Use the Manager-pattern real-backend auth as the template instead.
- `manager/domain/auth/ActiveSalonUiState.kt` + `resolveActiveSalon()` — multi-salon staff selection flow, reusable as-is once real membership data exists.

**Networking layer** (`data/remote/`, `di/BackendApiContainer.kt`): Retrofit + OkHttp + kotlinx.serialization, one central DI container. Already-registered repositories directly reusable by Reception: `bookingRepository`, `availabilityRepository`, `serviceRepository`, `serviceCategoryRepository`, `specialistRepository`, `salonRepository`, `salonMembershipRepository` (client-side ready, backend pending per §1), `customerRelationshipRepository`, `currentUserIdentityContextRepository`.

**Manager-scoped repositories** (`manager/data/ManagerRepositories.kt`): a per-active-salon singleton container (`AppointmentRepository`, `ServiceRepository`, `SpecialistRepository`, `CustomerRepository`, all backend-integrated, `Empty*` fallbacks before a salon resolves — "honest nothing-loaded-yet, not fake data"). Reception's booking/customer/calendar screens should consume this exact same container rather than a parallel one.

**Manager screens available as direct templates:**
- `manager/screens/auth/` — `ManagerOtpAuthScreen`, `ManagerSalonSelectionScreen`.
- `manager/screens/booking/` — full 7-screen wizard (`Start/Customer/Service/Specialist/DateTime/Review/Success`), backed by `ManagerBookingViewModel`. This is the direct template for Reception's core "book a walk-in/phone customer" flow.
- `manager/screens/calendar/` — `ManagerCalendarScreen` (daily/weekly), `ManagerAppointmentDetailScreen`.
- `manager/screens/customers/` — list/profile/edit, CRM insights.
- `manager/screens/splash/`, `manager/screens/profile/`.
- **Deliberately not templates for Reception** (Manager/Owner-only by business role, matches `SalonMemberRole.RECEPTIONIST`'s own doc comment — *"booking operations only"*): `manager/screens/staff/`, `manager/screens/services/`, `manager/screens/settings/`.

---

## 3. Reusable components inventory

| Component | Location | Reuse for Reception |
|---|---|---|
| `PremiumGlassSurface`, `premiumMetallicBorder`, `RojanShadows` | `ui/components/glass/` | As-is, palette-bound to new `ReceptionPalette` |
| `RojanTypography`, `RojanDimens` | `ui/theme/` | As-is |
| `RtlSectionHeader`, RTL layout kit | `ui/components/rtl/RtlLayoutKit.kt` | As-is |
| `ManagerGlassSurface`/`ManagerAccent` pattern (Teal+Gold) | `manager/components/ManagerGlassTheme.kt` | Pattern to copy for a Reception-specific accent (own palette instance, not a new mechanic) |
| OTP auth screen + ViewModel | `manager/screens/auth/`, `manager/presentation/auth/` | Copy/adapt: same shape, different role gate |
| Active-salon selection flow | `manager/domain/auth/ActiveSalonUiState.kt` | As-is once memberships exist |
| Booking wizard (7 screens + ViewModel) | `manager/screens/booking/`, `manager/presentation/booking/` | Direct template for Reception's primary flow |
| Calendar (daily/weekly + detail) | `manager/screens/calendar/` | Direct template — Reception needs read/check-in, not full Manager edit rights |
| Customer list/profile | `manager/screens/customers/` | Direct template, trimmed to CRM-view-only where the eventual permission model says so |
| `ManagerRepositories.kt` container pattern | `manager/data/` | Reuse the same per-salon repository container, don't fork a second one |
| `di/BackendApiContainer.kt` | `di/` | Extend with a `salonMembershipRepository`-driven check once §4 lands; no new networking mechanic needed |

---

## 4. Missing backend dependencies (blocking, in priority order)

1. **`GET /api/v1/users/me/salon-access`** — the endpoint `CurrentUserIdentityContextRepositoryImpl` already calls. Must return `ownedSalons`/`memberships` (with real `SalonMemberRole` and a real `permissions` set)/`specialistLinks`. Nothing about "who am I, and what can I do" resolves without this.
2. **`SalonMembershipController`** (`GET/PUT/DELETE /api/v1/salons/{salonId}/members/{userId}`) — so a salon owner can actually grant a phone number `RECEPTIONIST` access. Without this, membership can only ever be seeded manually (e.g. direct DB insert), which is fine for internal testing but not shippable.
3. **A real backend `Permission` model** (`ai.rojan.backend.domain.salon.Permission` or equivalent) wired into `/salon-access`'s response — the Android `SalonPermissions` constants currently have nothing behind them; any permission-gated UI built against them today is checking values the backend never sends.
4. **Authorization broadened on operational endpoints** — `SalonBookingController` (`list`, `createForCustomer`), every `CustomerController` method, and the availability/schedule controllers currently check `salon.ownerId == callerId` only. Each needs a second accepted condition: "caller has an active salon membership carrying the relevant permission" (e.g. `MANAGE_BOOKINGS`/`MANAGE_OWN_BOOKINGS` for bookings, `VIEW_CRM`/`MANAGE_CRM` for customers). This is the change that actually makes "Reception" mean something other than "logged in as the owner."
5. **Global `UserRole` decision** — confirm whether a receptionist needs a new global `UserRole` value (parallel to `CUSTOMER/MANAGER/SPECIALIST`, mirroring how `ManagerAuthViewModel` gates on `role == "MANAGER"`) or whether the plan is for reception staff to hold `MANAGER`-role accounts scoped down purely by salon-membership permissions. This changes what the Reception app's OTP-verify gate checks (§5, Phase 2) and should be decided before that phase starts, not discovered mid-implementation.

None of items 1-4 require inventing new architecture — they extend patterns (`ownerId` ownership check, `AuthResponse`/`ApiError` shapes, Spring Security config) already established and audited elsewhere in the same backend codebase. Item 5 is a real product decision, not a technical one, and is the one item this plan cannot resolve unilaterally.

---

## 5. Permission dependencies (Reception-specific)

Assuming the `SalonPermissions` vocabulary in §1 is confirmed real once §4 lands, Reception's screens map to:

| Screen/action | Required permission |
|---|---|
| View calendar / appointment list | `MANAGE_BOOKINGS` (or a narrower read-only variant, TBD — not currently distinguished in the client vocabulary) |
| Create booking for a customer | `MANAGE_BOOKINGS` |
| Confirm/cancel/complete a booking | `MANAGE_BOOKINGS` |
| Search/view customer profile | `VIEW_CRM` |
| Add a customer note | `MANAGE_CRM` (open question: should Reception have this, or read-only `VIEW_CRM`? — flag for the same sign-off as item 5 above) |
| Staff/services/settings screens | Not exposed to Reception at all — `MANAGE_STAFF`/`MANAGE_CATALOG`/`MANAGE_SALON` stay Manager/Owner-only, consistent with `SalonMemberRole.RECEPTIONIST`'s existing "booking operations only" doc comment |

---

## 6. Screens to implement

New `reception` flavor package `manager`-mirrors (`ai.rojan.designlab.reception.*`), reusing shared `src/main` design-system code exactly as Manager does:

| Screen | Template | Notes |
|---|---|---|
| Splash | `manager/screens/splash/` | Reception launcher icon (new asset needed) |
| OTP Auth | `manager/screens/auth/ManagerOtpAuthScreen` | Adapt role gate per §4 item 5 decision |
| Salon Selection | `manager/screens/auth/ManagerSalonSelectionScreen` | As-is once memberships resolve real salons |
| Reception Dashboard (new, no Manager equivalent) | Loosely modeled on `ManagerDashboardScreen` layout rhythm, own content | Today's bookings, quick "new booking" / "check in" actions — no KPI/revenue cards (owner-only data) |
| Calendar (daily/weekly) | `manager/screens/calendar/ManagerCalendarScreen` | Read + status-transition actions only, no schedule editing |
| Appointment Detail / Check-in | `manager/screens/calendar/ManagerAppointmentDetailScreen` | Add a check-in state transition if the product wants one (not in current `BookingStatus` lifecycle — `PENDING/CONFIRMED/COMPLETED/CANCELLED` only; a `CHECKED_IN` status would itself be a backend change, flag separately if wanted) |
| Booking wizard (7 screens) | `manager/screens/booking/*` | Direct copy/adapt, same `ManagerBookingViewModel` shape against `POST /salons/{salonId}/bookings` |
| Customer search/profile | `manager/screens/customers/ManagerCustomersListScreen` + `ManagerCustomerProfileScreen` | View-only unless §5's `MANAGE_CRM` question resolves otherwise |
| Profile / Logout | `manager/screens/profile/ManagerProfileScreen` | As-is pattern |

---

## 7. API endpoints in play

**Already real and reusable as-is** (from `origin/feature/auth-rate-limit-finalization`):
- `POST /api/v1/auth/otp/request` / `/otp/verify` / `/otp/resend`
- `GET /api/v1/users/me`
- `GET /api/v1/salons/mine`, `GET /api/v1/salons/{salonId}`
- `GET /api/v1/salons/{salonId}/categories/**`, `.../services/**`, `.../specialists/**` (read)
- `GET /api/v1/salons/{salonId}/specialists/{id}/available-slots`
- `POST /api/v1/salons/{salonId}/bookings` (create for customer — **owner-only today**, needs §4.4)
- `GET /api/v1/salons/{salonId}/bookings` (list — **owner-only today**, needs §4.4)
- `PATCH /api/v1/bookings/{id}/confirm|cancel|complete`, `PUT .../reschedule` (**owner-only today**, needs §4.4)
- `GET/POST /api/v1/salons/{salonId}/customers/**` (**owner-only today**, needs §4.4)

**Do not yet exist, required before real reception auth works:**
- `GET /api/v1/users/me/salon-access` (§4.1)
- `SalonMembershipController` (§4.2)

---

## 8. Estimated implementation order

**Phase 0 — Scaffolding (unblocked, start immediately):**
1. `reception` product flavor in `app/build.gradle.kts` (mirror `manager` flavor exactly: own `applicationId` suffix, `src/reception/AndroidManifest.xml`, `ReceptionActivity`, launcher icon placeholder, `app_name` override).
2. `ReceptionPalette` in `RojanAppPalette.kt` + root `CompositionLocalProvider` wiring in `ReceptionActivity`.
3. `ReceptionDestinations` + nav graph, mirroring `ManagerDestinations`/`ManagerNavGraph` shape.
4. Splash + OTP auth + salon-selection screens, copied/adapted from Manager's — functionally testable only against a manually-seeded backend account/membership row (no `/salon-access` endpoint yet, so this runs against a stub or a temporary direct query until §4.1 lands).

**Phase 1 — Core booking flow (unblocked for UI, blocked for real multi-tenant auth):**
5. Reception Dashboard (new screen, own layout).
6. Calendar (daily/weekly) + appointment detail, adapted from Manager's, read/status-transition only.
7. Booking wizard (7 screens), adapted from Manager's `ManagerBookingViewModel`/screens, targeting the existing `POST /salons/{salonId}/bookings` endpoint.

**Phase 2 — Customer lookup:**
8. Customer list/search + profile screens, adapted from Manager's, scoped to view-only pending §5's note-permission decision.

**Phase 3 — Real RBAC wiring (blocked on backend, §4 items 1-4):**
9. Wire `currentUserIdentityContextRepository`/`salonMembershipRepository` for real once backend ships `/salon-access` and `SalonMembershipController`.
10. Replace every Phase 0-2 screen's provisional "assume authorized" data access with real permission checks against `SalonPermissions`, once a real backend `Permission` payload exists to check against.
11. End-to-end test: owner grants a phone number `RECEPTIONIST` membership → that phone logs into the Reception app via OTP → can book/view calendar/view customers → cannot reach Manager-only actions (staff/services/settings/KPIs) even if navigated to directly.

**Explicit dependency:** Phase 3 cannot start meaningfully until someone (not this plan) resolves §4 item 5 and schedules backend work for §4 items 1-4. Phases 0-2 are real, demoable progress in the meantime, not throwaway — every screen built there is the actual target UI, just running against provisional/seeded data until the backend catches up.

---

## Open questions requiring sign-off before Phase 2/3 start

1. §4.5 — new global `UserRole.RECEPTIONIST`, or salon-membership-scoped `MANAGER`-role accounts?
2. §5 — does Reception get `MANAGE_CRM` (can add notes/tags) or only `VIEW_CRM` (read-only)?
3. Appointment check-in — is a new `BookingStatus.CHECKED_IN` wanted, or does Reception operate entirely within the existing `PENDING/CONFIRMED/COMPLETED/CANCELLED` lifecycle?
4. Confirm backend work for §4 items 1-4 gets scheduled — this plan assumes it will, but doesn't own that backlog.

**No code will be written until this plan is reviewed.**
