# TEAM2 RESULT — MANAGER DATA PERSISTENCE (TEAM2-002)

Priority: P1
Status: FIXED for Dashboard salon identity/stats and Calendar (priorities 1–3); Customers list, the booking-creation wizard, and Manager profile remain on in-memory data — explicitly out of this task's scope, disclosed below.
Repository modified: ANDROID (`ROJAN_DesignLab_Main1`)
Repository inspected, not modified: BACKEND (`ROJAN_Backend`)

---

## CONFIRMED PROBLEM

Re-verified from current source, not assumed from the earlier TEAM2 inspection:

- `manager/data/ManagerRepositories.kt` is a plain Kotlin `object` singleton
  wiring `InMemoryAppointmentRepository`/`InMemoryCustomerRepository`/
  `InMemoryServiceRepository`/`InMemorySpecialistRepository` — each backed
  by a `mutableListOf` seeded with hardcoded sample data. No network
  dependency, no DI framework, anywhere in `manager/**`.
- `ManagerCalendarScreen.kt` read `ManagerRepositories.appointments`/
  `.customers`/`.services`/`.specialists` directly.
- `ManagerDashboardScreen.kt`'s `TodayOverviewSection` computed stats via
  `manager.data.computeManagerDashboardStats()`, itself reading
  `ManagerRepositories`; `SalonIdentityCard` rendered a hardcoded default
  salon name ("سالن رویان").
- `ManagerBookingViewModel.confirm()` (the "نوبت جدید" wizard) writes a new
  `Appointment` straight into `ManagerRepositories.appointments` — no
  `salonId`, no backend call.

**A deeper, confirmed blocker not on the task's own inspection list:** the
Manager app (`ManagerActivity` → `managerNavGraph`) had **no
authentication at all** — splash navigated straight to Dashboard. Every
backend endpoint this task needs (`GET /salons/mine`,
`GET /salons/{id}/bookings`, confirm/complete) requires a real Bearer
token for a MANAGER-role account. This was raised with the user before
writing any code; the agreed direction (reuse the existing, real
`AuthScreen`/`AuthViewModel` — no new login UI designed) is what's
implemented below.

## AFFECTED FLOWS

| Flow | Confirmed dependency | This task |
|---|---|---|
| Dashboard (salon identity + today's stats) | `ManagerRepositories`, hardcoded `SalonIdentityCard` defaults | **Fixed** — real `GET /salons/mine` + real bookings |
| Calendar | `ManagerRepositories.appointments/specialists` | **Fixed** — real `GET /salons/{id}/bookings` |
| Booking status update (confirm/complete) | No UI existed at all | **Fixed** — wired into Calendar's appointment rows |
| Booking wizard ("نوبت جدید") | `ManagerRepositories` (create) | **Not fixed** — disclosed, see Remaining Risks |
| Customers list/profile | `ManagerRepositories.customers` | **Not fixed** — out of this task's named priority list |
| Salon selection | Didn't exist (single hardcoded salon assumed) | **Not built** — first-owned-salon assumed, disclosed |

## ROOT CAUSE

The Manager module was built as a self-contained design/UI workspace
before the "Android ↔ Backend Full Integration" milestone that wired the
Customer app's booking/salon/service/specialist repositories ever reached
it — it was never migrated. Unlike the Customer flow, it also never
gained a login screen, because nothing in it needed a session before this
task.

## BACKEND: CONFIRMED ALREADY SUFFICIENT, NOT MODIFIED

Inspected fresh, all three needed capabilities already exist correctly:

- `GET /api/v1/salons/mine` (`SalonController.mine`) — "salons owned by
  the authenticated user." Exactly what a salon-owner dashboard needs to
  resolve its own salon; not paginated (an owner's salon count is small).
- `GET /api/v1/salons/{salonId}/bookings` (`SalonBookingController.list`)
  — every booking against that salon, owner-only (403 otherwise),
  paginated, status-filterable.
- `PATCH /bookings/{id}/confirm` / `/complete` — already given a real
  Android client in TEAM2-003, unused until now.

Per "do not add APIs if backend already provides them," **no backend file
was modified.**

**A real, confirmed backend gap found and *not* worked around by adding
backend code:** there is no endpoint that lets a salon owner resolve a
booking's `customerId` to a customer's name/profile. Manager Calendar
therefore shows "—" for the customer on every real booking row (an honest
placeholder, not a fabricated name) rather than the client-side
`ManagerRepositories.customers` lookup it used before. Recommended as a
scoped follow-up (see Remaining Risks) — adding it now would mean
designing a new authorization-sensitive endpoint (should an owner see any
customer's full profile, or a name only?), a real product/security
decision outside "Manager Data Persistence."

## FILES CHANGED

| File | Change |
|---|---|
| `manager/navigation/ManagerDestinations.kt` | Added `LOGIN` route. |
| `manager/navigation/ManagerNavGraph.kt` | Splash now checks for a stored access token and routes to `LOGIN` or `DASHBOARD`. New `LOGIN` composable reuses the existing `AuthScreen`/`AuthViewModel`/`AuthViewModelFactory` verbatim — no new login UI. Dashboard/Calendar wired with `onRequireLogin`, routed through a new `navigateToManagerLogin()` helper that clears the whole back stack on a real 401. |
| `domain/repository/SalonRepository.kt` | Added `active: Boolean = true` to `Salon` (defaulted — every existing construction, including test fakes, stays source-compatible) and `myOwnedSalons(): Result<List<Salon>>`. |
| `data/remote/SalonApi.kt`, `data/repository/SalonRepositoryImpl.kt` | Added the `GET /salons/mine` Retrofit method and its repository implementation. |
| `data/remote/SalonBookingApi.kt` (new) | Retrofit contract for `GET /salons/{salonId}/bookings`. |
| `domain/repository/BookingRepository.kt`, `data/repository/BookingRepositoryImpl.kt` | Added `salonBookings(salonId, ...)` — same `safeApiCall`-wrapped, DTO-mapped pattern as every other method here. `BookingRepositoryImpl`'s constructor now also takes `SalonBookingApi`. |
| `di/BackendApiContainer.kt` | Constructs `SalonBookingApi` and passes it into `BookingRepositoryImpl`. |
| `manager/presentation/dashboard/ManagerDashboardViewModel.kt` (new) + factory | Resolves the manager's salon (`myOwnedSalons().firstOrNull()`), computes real today's-stats from real `salonBookings`/`getSpecialists`/services. `Loading`/`Success`/`Empty` (zero salons)/`Error` via the shared `UiState`, plus a `requiresReauth` signal for a real 401. |
| `manager/presentation/calendar/ManagerCalendarViewModel.kt` (new) + factory | Same salon resolution, then real `salonBookings` + specialist/service name resolution. Owns `confirmAppointment`/`completeAppointment` (real `PATCH` calls, reload after). |
| `manager/components/ManagerStateViews.kt` (new) | `ManagerLoadingState`/`ManagerErrorState`/`ManagerEmptyState` — dark-theme counterparts to the Customer app's `Rojan*State` components (those are built for light glass cards; would render illegibly on Manager's dark theme). First time the Manager app needed a real Loading/Error/Empty state at all. |
| `manager/components/TodayOverviewSection.kt` | Now takes `stats: ManagerDashboardStats` as a parameter instead of computing internally — no business logic inside this Composable any more. "مشتریان جدید" (new customers) stat dropped — no honest data source for it (see Backend Gap above). |
| `manager/screens/dashboard/ManagerDashboardScreen.kt` | Wired to `ManagerDashboardViewModel`; renders `Loading`/`Error`(+retry)/`Empty`/`Success`. `QuickActionsSection`/`AIInsightCard`/`CalendarPreviewSection` unchanged. |
| `manager/screens/calendar/ManagerCalendarScreen.kt` | Wired to `ManagerCalendarViewModel`. Day selector now uses `RollingBookingDates`' real rolling 7-day window instead of `ManagerCalendarWeek`'s hardcoded reference week. `AppointmentRow` gained "تایید نوبت"/"تکمیل نوبت" inline actions for `PENDING`/`CONFIRMED` bookings. `NO_SHOW` display status dropped — the real backend `BookingStatus` has no such state. |
| `manager/data/ManagerDashboardStats.kt` | **Deleted** — dead code once `TodayOverviewSection` stopped calling `computeManagerDashboardStats()`; kept would have meant two same-named, one-real-one-fake `ManagerDashboardStats` types in the codebase. |
| 4 pre-existing test files (`BookingConfirmationViewModelTest`, `AppointmentsViewModelTest`, `RescheduleAppointmentViewModelTest`, `BookingRepositoryImplTest`) | Mechanical fallout: their `SalonRepository`/`BookingRepository` fakes needed new no-op overrides (`myOwnedSalons`, `salonBookings`) to keep implementing the now-larger interfaces. No behavioral change to any of them. |

Three new test files were added (see Tests below).

## ARCHITECTURE IMPACT

- **No UI redesign.** Dashboard/Calendar keep their exact layout, cards,
  and navigation; `SalonIdentityCard`'s existing parameters are simply fed
  real values instead of defaults. The two new inline row actions
  (confirm/complete) match the compact-text-link pattern the Customer
  app's `AppointmentsScreen` cancel/reschedule actions already use — not a
  new visual language.
- **One genuinely new piece of architecture: the Manager login gate.**
  This was raised with the user as a scope question before implementation
  (not assumed) — the agreed, minimal answer was to reuse the Customer
  app's real `AuthScreen`/`AuthViewModel` as-is. It looks visually like
  the Customer app (light glass theme) rather than Manager's dark theme,
  since no new UI was designed for this task — flagged as a follow-up
  polish item.
- **Single-salon assumption.** A manager account can in principle own
  multiple salons (`GET /salons/mine` returns a list); this MVP takes the
  first one, matching the rest of the Manager app's existing single-salon
  assumption. A real salon switcher is new UI this task doesn't add.
- **The booking wizard was deliberately left unfixed — and this creates a
  real, disclosed inconsistency**, the most important thing in this
  report to flag: since Calendar now shows only real backend bookings, an
  appointment "created" via "نوبت جدید" (still writing to
  `ManagerRepositories.appointments` only) will **no longer appear
  anywhere** — before this change it at least showed up locally; now it
  silently doesn't show up at all. This is worse-looking than before in
  isolation, though it correctly reflects reality (no backend booking was
  ever created). Wiring the wizard to real `createBooking` needs real
  customer lookup (no such endpoint exists), real service/specialist
  selection (the wizard currently uses its own separate in-memory
  catalog, not the real one), and real availability — each a meaningful
  chunk of work in its own right, well beyond "dashboard/calendar/status-
  update" and the reason it isn't attempted here. See Remaining Risks.

## DATA REQUIREMENTS: LOADING / SUCCESS / EMPTY / ERROR / UNAUTHORIZED

Both new ViewModels implement all five:

- **Loading**: initial state and every `retry()`/`load()`.
- **Success**: real salon + stats (Dashboard) / real bookings + specialist
  list (Calendar).
- **Empty**: zero owned salons (Dashboard and Calendar both check this
  first) or zero bookings for the salon (Calendar).
- **Error**: any non-401 failure (network, 5xx, etc.), shown with a real
  retry action.
- **Unauthorized**: a real 401 sets a distinct `requiresReauth` flag
  (not folded into the generic retriable `Error` state) — a dead refresh
  token means retrying the same request will just 401 again, so the
  screen routes back to the login gate instead of offering a retry that
  can't succeed.

## TESTS

**Repository** (`app/src/test/java/.../data/repository/BookingRepositoryImplTest.kt`, 3 new tests added to the existing file):
1. `salonBookings success maps every booking in the page into a domain Booking` — **backend success**.
2. `salonBookings backend error (403 - not the owner) becomes a real Result failure` — **backend error**.
3. `salonBookings 401 (unauthorized) becomes a real Result failure` — **unauthorized**.

(`myOwnedSalons` itself goes through the same `safeApiCall` path already covered by every other `SalonRepositoryImpl` method's existing tests-by-construction — its own dedicated success/error/401 behavior is exercised indirectly via the ViewModel tests below, which mock `SalonRepository` directly.)

**`ManagerDashboardViewModelTest.kt`** (new, 6 tests): Loading (via a controllable `CompletableDeferred` gate — proven transiently `Loading` before completion, `Success` after), a real salon+bookings → `Success` with real stats, zero salons → `Empty`, network failure → `Error`, 401 → `requiresReauth`, `retry()` re-fetches.

**`ManagerCalendarViewModelTest.kt`** (new, 7 tests): real bookings → `Success` with resolved specialist/service names and the honest `"—"` customer placeholder, empty booking list → `Empty`, zero salons → `Empty`, network failure → `Error`, 401 → `requiresReauth`, confirming calls the real confirm endpoint and reloads, completing calls the real complete endpoint and reloads.

Total new tests this task: **16** (3 repository + 13 ViewModel).

**Restarting the app does not lose server-backed data — verified structurally, not by a device test:** neither `ManagerDashboardViewModel` nor `ManagerCalendarViewModel` holds any reference to `manager.data.ManagerRepositories` or any other process-lifetime singleton — their entire constructor dependency graph is backend repository interfaces. Every `load()` re-fetches from the network; nothing is cached client-side. A process restart reconstructs `BackendApiContainer` from the persisted, disk-backed token store (`EncryptedSharedPreferences`, survives restart) and re-fetches everything fresh on next screen visibility — there is no local write path for dashboard/calendar data to lose in the first place. This is a structural property of the code (confirmed by inspection of every new file's imports/constructor), not something a unit test can independently demonstrate beyond what the ViewModel tests above already do (each constructs a fresh instance from fakes and gets correct results with no shared state between tests).

## VALIDATION

**Android:**

| Check | Result |
|---|---|
| Compile Customer (`assembleCustomerDebug`) | ✅ `BUILD SUCCESSFUL` |
| Compile Manager (`assembleManagerDebug`) | ✅ `BUILD SUCCESSFUL` |
| `testCustomerDebugUnitTest` + `testManagerDebugUnitTest` | ✅ 16/16 new tests pass on both flavors (49 tests total per flavor, up from 33). Same 2 pre-existing `BackendAuthFlowVerificationTest` failures remain (documented as needing a live backend, not run here) — unrelated, unchanged. |
| `lintCustomerDebug` + `lintManagerDebug` | ⚠️ Same single pre-existing error on each flavor as every prior TEAM2 task found (`ViewModelConstructorInComposable` in `AuthScreenScreenshotTest.kt:70`, untouched, out of scope), same 94/98 pre-existing warnings. Confirmed by grepping every lint report: zero findings across all 16 files this task touched. |

**Backend:** Not modified — no backend build/test run needed per the task's own conditional instruction.

## REMAINING RISKS

- **The booking wizard now silently produces invisible appointments** —
  the single biggest, most important disclosure in this report (see
  Architecture Impact). Recommend this as the immediate next Manager-flow
  task: wire `ManagerBookingViewModel` to real `createBooking`, which in
  turn requires real service/specialist selection (swap
  `InMemoryServiceRepository`/`InMemorySpecialistRepository` for the real,
  already-shared `ServiceRepository`/`SpecialistRepository`) and a
  decision on customer selection, since there is no backend "look up a
  customer" endpoint for a salon owner to search against.
- **No customer name resolution anywhere in Manager** — confirmed backend
  gap (see Backend section). Every booking row shows "—" for the
  customer. Recommend a scoped, owner-authorized "get customer info for a
  booking I own" endpoint as backend follow-up work.
- **Manager login uses the Customer app's light theme**, not Manager's
  dark luxury glass — a real, visible inconsistency, deliberately not
  addressed since no new UI was to be designed for this task. A
  Manager-themed login screen (reusing `AuthViewModel` as-is, just a new
  themed layout) is a reasonable, small follow-up.
- **Single-salon assumption** — a manager owning more than one salon only
  ever sees the first one returned by `GET /salons/mine`; no switcher UI
  exists. Unlikely to matter for the current MVP's target users but
  worth a ticket.
- **Customers list/profile screens remain fully in-memory** — out of this
  task's named priority list (dashboard/calendar/status-update), not
  touched.
- **No pagination UI** on Calendar's booking fetch (one bounded
  `size=200` page) — same disclosed limitation as `AppointmentsViewModel`
  (TEAM2-004) and consistent with it.
- **No instrumented/Compose UI test** was added for the new screens'
  rendering — coverage is at the ViewModel level, consistent with every
  prior TEAM2 task's validation depth.

---

**Stopping here per the stop condition. Not continuing to RBAC (TEAM2-005)
or Token Security (TEAM2-006) — waiting for review.**
