# ROJAN System 2 — Reception Phase 1 Status Report v1

**Scope:** "Controlled Implementation" per your approved allowed-scope list (UI foundation, Compose navigation, authentication state handling, repository interfaces for approved backend contracts, loading/error/empty states, Booking UI screens, Customer Identity UI component). Basis: `ROJAN_System1_Backend_Decision_v2.md`, `ROJAN_Reception_Phase1_Updated_Plan_v2.md`.

**Rules honored, verified not just followed:** no mock API, no fake data (every repository call targets a real endpoint — some currently return real authorization errors, which is the correct, honest behavior, not a workaround); no authentication bypass (`RECEPTION_GATE_ROLE` unchanged); no RBAC modification (permission checks are entirely backend-side and untouched); no backend contract changes (this session touched zero files in `ROJAN_Backend`); no architecture change (extends the established Clean Architecture / per-flavor-independence pattern already in place). For the one unavailable backend endpoint (`InviteController`), only an integration-interface placeholder was created — no Retrofit binding, no implementation, no DI wiring, no UI.

**Git state:** all changes uncommitted, working tree only. No commit, no push, performed or requested.

---

## 1. Completed screens

| Screen | New/Updated | Data source |
|---|---|---|
| Splash, OTP Auth, Salon Selection, Access-Error, Profile | Unchanged (Phase 0/1) | Real, already verified |
| **Dashboard** | Rewritten — real booking list (Loading/Error/Empty), quick actions (new booking / customers / profile) | Real `GET /salons/{salonId}/bookings` — currently returns an authorization error (owner-only today), rendered as a real `UiState.Error`, not hidden |
| **Booking wizard** (7 screens: Start, Customer, Service, Specialist, DateTime, Review, Success) | New | Customer/Booking: real, owner-only-gated endpoints. Service/Specialist/Availability: real, "any authenticated user" endpoints — these actually succeed today |
| **Customers list** | New | Real `GET /salons/{salonId}/customers` — owner-only today, same honest-error status as bookings |

**Shared UI foundation added:** `ReceptionUiStateList` (one Loading/Error/Empty/Success renderer, reused by 6 screens) and `ReceptionCustomerIdentityCard` (the requested Customer Identity UI component — read-only, `VIEW_CRM`-scoped by design: no lifetime value, no tag/note affordance).

**Navigation:** `ReceptionDestinations`/`ReceptionNavGraph` extended with `DASHBOARD` (real), `CUSTOMERS`, and a nested `BOOKING_FLOW_GRAPH` (7 routes, one shared `ReceptionBookingViewModel` per wizard session, mirroring `ManagerNavGraph.kt`'s established pattern). No Invite routes exist — consistent with the placeholder-only rule.

**Authentication state:** `ReceptionAuthViewModel`'s `RECEPTION_GATE_ROLE` doc comment updated to reflect System 1's confirmation (§1b) that this was correct all along — no behavioral change, comment only.

---

## 2. Pending APIs

Client-side bindings added this session for endpoints that **already exist backend-side** (no backend work needed, purely a prior Android-side gap):

- `PATCH /api/v1/bookings/{bookingId}/confirm` — `BookingApi.confirmBooking` / `BookingRepository.confirmBooking`
- `PATCH /api/v1/bookings/{bookingId}/complete` — `BookingApi.completeBooking` / `BookingRepository.completeBooking`

Not yet wired into any screen (no confirm/cancel/complete action button exists on the Dashboard's booking rows yet) — the repository-layer binding was in scope this session; surfacing it in the UI was not explicitly listed and was left out to stay inside the approved scope.

**DTO enrichment prepared, not yet populated by the backend:** `BookingResponseDto`/`Booking` gained nullable `service`/`specialist`/`customer` summary fields (System 1 decision §3's approved shape). Every real response today omits them — the Dashboard's booking rows already fall back to raw ids (`booking.customerId`, etc.) when they're `null`, so nothing breaks and nothing is faked while waiting for the backend to populate them.

---

## 3. Blocked items

| Item | Blocked on | Status |
|---|---|---|
| Real data on Dashboard / Booking wizard / Customers list | `ROJAN_System1_Backend_Decision_v2.md` §4 item 6 (authorization broadening) | Every screen is built and will render correctly the moment this ships — currently shows a real, honest authorization error |
| Salon-access / Dashboard reachability at all | §4 items 1-2 (`SalonMembership` persistence, `/users/me/salon-access`) | Unchanged from the Phase 1 Auth Block-State report — still the root blocker for reaching any of the above with a non-owner account |
| Invite acceptance flow | §4 item 5 (`SalonInvite`/`InviteController`) **and** the still-unresolved OTP-auto-registers-as-CUSTOMER gap (`ROJAN_Reception_Phase1_Updated_Plan_v2.md` §3) | Domain contract captured (`ReceptionInviteRepository`, placeholder only); zero UI, zero network binding — correctly out of scope per your "integration interface placeholder only" rule |
| Booking confirm/complete surfaced in UI | Nothing backend — purely a "not yet built this session" scope choice | Repository layer ready; no screen calls it yet |

---

## 4. Integration points

Where a future backend change plugs in with **zero further Android-side redesign**:

- **`GET /users/me/salon-access` ships** → `ReceptionAuthViewModel` already calls it; Dashboard becomes reachable with no code change.
- **Authorization broadened (§4 item 6)** → `ReceptionBookingRepository`/`ReceptionCustomerRepository`/`ReceptionDashboardViewModel` already call the real endpoints; their `UiState.Error` states resolve to `UiState.Success` automatically, no code change.
- **`BookingResponse` enrichment ships (§3)** → `service`/`specialist`/`customer` fields go from `null` to populated; Dashboard's booking rows already prefer them over raw ids when present, no code change.
- **`InviteController` ships (§4 item 5)** → `BackendReceptionInviteRepository` (new, implementing the already-defined `ReceptionInviteRepository` interface) is the only new class needed; the interface's own doc comment states this explicitly.
- **§5's remaining role-promotion gap gets resolved** → whatever the answer, it determines whether Invite UI screens get built next, or whether `ReceptionAuthViewModel`'s gate needs a second, invite-aware path.

**No implementation performed beyond what's listed above. No commit, no push.**
