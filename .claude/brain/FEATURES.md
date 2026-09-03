# FEATURES — Per-Flavor Status

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: feature completion status, cross-referenced against code and prior audit reports. Status labels are a judgment call, not a build output — re-verify before relying on them for a release decision. Backend-blocked items reflect Android readiness only.

Legend: `complete` · `frozen` (extend-only, needs approval to redesign — see root `CLAUDE.md`) ·
`blocked-backend` (Android side ready, waiting on System 1) · `deferred` (deliberate scope cut) ·
`not-started`

## Customer (`ai.rojan.designlab`)

| Feature | Status |
|---|---|
| Auth / session routing | complete |
| Home (13 sections) | complete |
| Salon search / list / details, specialist profile, service details | complete |
| Booking flow | complete — confirm/complete UI trigger `deferred` (repository support exists, `BookingApi.completeBooking`) |
| Specialist → Services integration (real eligible-services filter) | complete, tested — **uncommitted**, see SESSION.md |
| Follow/favorite, profile/settings | complete |

## Manager (`ai.rojan.designlab.manager`)

| Feature | Status |
|---|---|
| Dashboard v1.0 + Calendar MVP | `frozen` — see root CLAUDE.md § Design Baseline v1.0 (Manager Dashboard) |
| Customers / services / staff / settings | complete, frozen scope, live backend |
| Media (logo/cover/gallery upload + display) | complete |
| Appointment confirm/cancel/complete actions | complete, functionally verified — no test coverage yet — **uncommitted**, see SESSION.md |
| Manager Invite (`ManagerInviteRepository`) | not implemented — interface only, placeholder pending backend `InviteController` |

## Reception (`ai.rojan.designlab.reception`) — Phase 1, "Controlled Implementation"

| Feature | Status |
|---|---|
| OTP auth, salon selection, profile, dashboard, booking wizard, customer list | complete — real backend, zero mock data |
| Non-owner Dashboard access | `blocked-backend` — needs `SalonMembership` persistence + `/users/me/salon-access` endpoint |
| Invite acceptance | `blocked-backend` — needs backend `InviteController` |
| Calendar UI / Invite UI | `not-started` — outside approved Phase 1 scope, correctly absent |

For the full approved-scope statement and backend-blocked details, see root `CLAUDE.md`
§ ROJAN RECEPTION APP. For historical acceptance evidence, see
`ROJAN_Reception_Phase1_Final_Acceptance_Report_v1.md` at repo root — not reproduced here.
