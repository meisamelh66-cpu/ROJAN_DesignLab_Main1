# ROJAN Manager Android Pilot Validation Report v1

**Scope:** System 2 (Android) — Manager App live validation on a connected physical device (Samsung SM-A725F, `RZ8R81WPS2J`).
**Date:** 2026-08-21
**Branch / build under test:** `feature/android-first-salon-pilot` @ `717dff4` — Manager flavor, `dev` environment (`ai.rojan.designlab.manager`, live backend `https://api.rojanai.ir/`), APK from this session's `assembleManagerDevDebug` (no rebuild during validation).
**Account under test:** `+989164987585` (authorized test number). Session established via a real OTP login (phone entry → OTP send → OTP verify), confirmed working end-to-end.
**Active salon selected for this pass:** `ROJAN AI Pilot Salon` (membership role — `عضو`, not owner), chosen specifically because it exercises the salon-lookup RBAC fix landed in `717dff4` (`GET /api/v1/salons/{salonId}` for a genuine membership-based manager, not the owner-only `GET /salons/mine`).

No app data was cleared, no additional OTP was requested, and no test booking was created in producing this report.

---

## 1. Dashboard — **FAIL**

**What works:** The screen loads without crashing, renders the full layout (header, KPI grid, quick actions, AI insight card, today's schedule section), and the empty-state KPI values (`۰` appointments, `۰` revenue, `۰` new customers, `٪۰` occupancy) are consistent with a salon that has no bookings.

**What fails:** The Salon Identity card at the top of the Dashboard does not reliably reflect the actual authenticated/active salon. Observed across this session:

| Point in session | Salon name shown on Dashboard identity card | Actually active salon |
|---|---|---|
| Pre-existing session, first cold launch (before any logout) | `سالن رویان` | — (this name doesn't appear in this account's own salon list at all: `میاه`, `روژ`, `ROJAN AI Pilot Salon`) |
| Same pre-existing session, Settings → Edit Salon screen | `میاه` | — |
| Same pre-existing session, after a Profile screen round-trip | `روژ` | — |
| Fresh login this session, explicit selection of `ROJAN AI Pilot Salon` on the Salon Selection screen | `سالن رویان` (again) | `ROJAN AI Pilot Salon` |

The card shows a different name almost every time it's read, and the name shown after **explicitly selecting** `ROJAN AI Pilot Salon` was `سالن رویان` — a value that isn't even a member of this account's three salons. The specialist filter on the Calendar screen (`Pilot Specialist`) correctly reflects `ROJAN AI Pilot Salon`, confirming the *backend selection* is right; the Dashboard identity card specifically is reading/rendering something else.

**Verdict:** FAIL — active-salon identity display on the Dashboard is not trustworthy for a multi-salon account.

---

## 2. Booking List (Calendar) — **PASS**

- Screen loads via Dashboard → "مشاهده تقویم کامل".
- Daily/Weekly view toggle both work.
- Specialist filter correctly shows `Pilot Specialist`, matching the selected salon (`ROJAN AI Pilot Salon`).
- Day navigation (tabs in Daily view, horizontal swipe in Weekly view) works correctly.
- Empty-state messaging renders correctly and consistently: `نوبتی برای این روز ثبت نشده است` (Daily) / `خالی` (Weekly), for every day checked.
- Checked 7 consecutive days (۲۱–۲۷) in both Daily and Weekly view — all empty, no rendering errors, no crashes.

**Verdict:** PASS.

---

## 3. Booking Detail — **BLOCKED, not tested**

No booking exists anywhere in the 7-day window checked (۲۱–۲۷) for `ROJAN AI Pilot Salon`, so there is no record to open the detail screen against. Per this turn's explicit instruction, no test booking was created to unblock this. Not attempted, not a code-level finding — purely a data-availability blocker.

---

## 4. Confirm Booking — **BLOCKED, not tested**

Same root cause as §3 — the `717dff4` Confirm-booking action (`PATCH /api/v1/bookings/{id}/confirm` via `ManagerAppointmentDetailViewModel`) has no reachable booking to act on for this salon. Not attempted.

---

## 5. Complete Booking — **BLOCKED, not tested**

Same root cause as §3/§4 — the `717dff4` Complete-booking action (`PATCH /api/v1/bookings/{id}/complete`) has no reachable booking to act on. Not attempted.

---

## Remaining Blocker

**Active salon context inconsistency (§1) is the one remaining, unresolved issue from this validation pass.** It doesn't block Booking List, but it does mean nothing on the Dashboard that's scoped "per active salon" (the identity card, and by extension anything else that trusts the same resolved value) can currently be trusted to describe the salon the manager actually selected. This is a pre-existing app-level finding, surfaced here per validation scope — no code was changed to investigate or fix it, consistent with this session's "Manager validation only, no code changes" constraint.

Unblocking items 3–5 requires either an existing booking on `ROJAN AI Pilot Salon` (none currently exists) or an explicit decision to create one — deferred per this turn's "no data creation" instruction.

---

*No code changes, no backend changes, no app data cleared, no additional OTP requested, no booking created in producing this report.*
