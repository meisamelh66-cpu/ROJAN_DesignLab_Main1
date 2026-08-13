# ROJAN Reception Phase 1 Updated Plan v2

**Basis:** `ROJAN_System1_Backend_Decision_v2.md` (`ROJAN_Backend`), read in full. Supersedes the relevant sections of `ROJAN_Reception_Implementation_Plan_v1.md` and narrows/removes several open questions carried since `ROJAN_Reception_Backend_Dependency_Checklist_v1.md` and `ROJAN_System2_Android_Integration_Clarification_v1.md`.

**Status: decisions only, nothing implemented.** System 1's document is an approved architecture decision, not a shipped backend. Every endpoint this plan now targets is still absent from `ROJAN_Backend` today (re-confirmed: `SalonMembership`, `InviteController`, `/users/me/salon-access`, and the `BookingResponse` enrichment all remain unbuilt). The practical, in-app consequence described in `ROJAN_Reception_Phase1_Auth_Block_State_Report_v1.md` — a real login deterministically reaching the Access-Error screen — is unchanged by this document. What changes is that the target contract is now fixed and approved, so Android work can be scoped precisely instead of provisionally.

**No code written, no implementation file modified, in producing this plan.**

---

## 1. Removed assumptions

Per instruction, explicitly checked off — each of these was a real assumption present in prior planning documents or in already-written (Phase 0/1) code, and each is now resolved:

- [x] **No direct user assignment.** `ROJAN_Reception_Implementation_Plan_v1.md` and the already-built `manager/domain/repository/SalonMembershipRepository.kt` / `data/remote/SalonMembershipApi.kt` / `data/remote/dto/SalonMembershipDtos.kt` assumed an owner directly assigns a role to an existing account by raw `userId` (`PUT /salons/{salonId}/members/{userId}`). System 1 explicitly rejected this shape in favor of invite-based membership (decision §2). This assumption is removed — see §3 below for what replaces it, and §5 for what happens to the existing code that assumed it.
- [x] **No fake salon-access.** This plan does not propose, and will not propose, any client-side mock/stub of `/users/me/salon-access` or any other backend response, at any point. The Phase 1 authentication-completion work already established this precedent (real backend calls only, a real Access-Error screen for the real current failure) and this plan continues it unchanged. Where the app cannot yet do something because a decided-but-unbuilt endpoint doesn't exist, the correct behavior is the existing honest blocked state, not fabricated data.
- [x] **No custom Reception global role.** System 1 decision §1b is explicit: no `UserRole.RECEPTIONIST`. `ReceptionAuthViewModel`'s `RECEPTION_GATE_ROLE = "MANAGER"` constant — previously flagged in its own doc comment as **provisional**, pending exactly this decision — is now **confirmed correct as originally written.** This plan removes it from the open-questions list entirely; §3 covers the one remaining code-comment update this implies (not performed now).

---

## 2. Alignment: Authentication flow

**No contract change.** System 1 approved Android's existing `SalonAccessResponse`/`CurrentUserIdentityContext` shape and `AuthResponse`/OTP request-verify shapes as-is (decision §1a) — every DTO already in `data/remote/dto/AuthDtos.kt` and `SalonAccessDtos.kt` is correct and requires no changes when the backend ships.

**One confirmed, no-code-change-needed outcome:** decision §1b means `ReceptionAuthViewModel`'s role gate does not need to change. When this plan is approved for implementation, the only actual edit warranted here is cosmetic — updating that constant's doc comment to cite this decision instead of describing itself as provisional/pending. Not done now, listed for completeness.

**One new field to plan for:** decision §1e adds `active: Boolean` to `UserResponse`/`AuthResponse.user`. Client-side, this means `data/remote/dto/AuthDtos.kt`'s `UserResponseDto` gains a matching field (additive, backward-compatible — `kotlinx.serialization` will not break on the old shape while the field is absent, since it will be added as non-optional only once the backend actually sends it, or defaulted during a transition). No behavior currently reads it; this is forward preparation only, not a Phase 1 blocker.

---

## 3. Alignment: Invite flow

**Fully re-specified, replacing the prior "no invite scaffolding exists" gap** (`ROJAN_Reception_Phase1_Readiness_Report_v1.md` §4.2) with a concrete target, per decision §2:

### Flow, with the identity-role gap this plan surfaces

```
Owner issues invite (Manager app — out of scope for Reception, noted for completeness)
  → Receptionist opens invite (public GET, no auth required)
  → If not authenticated: existing OTP Auth flow
  → POST accept (authenticated, phone number must match the invite)
  → Membership activated → salon-access now includes it → Dashboard
```

**Gap surfaced by this alignment, not resolved by System 1's decision document — flagged, not silently assumed away:** decision §2's `accept` endpoint requires the caller to already be authenticated, but does not address what happens when the invited phone number has **no existing ROJAN account.** Per the already-verified backend OTP behavior, a brand-new phone number auto-registers as `UserRole.CUSTOMER` on first verification — which would then **fail** `ReceptionAuthViewModel`'s `MANAGER`-role gate before the user could ever reach the accept step. This is a real, unaddressed sequencing problem: either (a) invite acceptance itself needs to be able to promote a `CUSTOMER`-role account to `MANAGER` server-side, or (b) first-time OTP verification needs a way to know "this phone is completing an invite, register as MANAGER, not CUSTOMER" — neither is in System 1's decision. **This plan does not resolve it — it is called out here as a required follow-up decision before Invite flow implementation can actually start**, not something Android can work around unilaterally.

### Required screens (Reception app)

1. **Invite preview** — `GET /api/v1/invites/{token}` (public). Shows salon name, offered role, and current invite status (`PENDING`/`EXPIRED`/`REVOKED`/`ACCEPTED`) before any commitment. Reachable without an existing session.
2. **Auth handoff** — if not authenticated, route into the existing `ReceptionOtpAuthScreen` (unchanged), then return to the invite flow — not a new auth screen, reuse of the existing one. Blocked on the gap above for the "brand-new phone number" case specifically.
3. **Accept confirmation / result** — `POST /api/v1/invites/{token}/accept`, success or one of the distinguishable failure states below.

### API sequence (target, per decision §2 — not yet real)

| Step | Endpoint | Auth |
|---|---|---|
| Issue (Manager app, not Reception) | `POST /api/v1/salons/{salonId}/invites` | Owner only |
| Preview | `GET /api/v1/invites/{token}` | Public |
| Accept | `POST /api/v1/invites/{token}/accept` | Authenticated, phone must match |

### Invite states (approved, per decision §2)

`PENDING → ACCEPTED` (terminal) `/ EXPIRED` (terminal) `/ REVOKED` (terminal).

### Success response (approved shape)

`{ membershipId, salonId, salonName, role, active: true }`

### Failure states, with approved distinguishable error codes

`404` unknown token · `409 INVITE_EXPIRED` · `409 INVITE_ALREADY_ACCEPTED` · `409 INVITE_REVOKED` · `403 INVITE_PHONE_MISMATCH`. Each maps to a distinct, specific UI message — not a generic error state, consistent with this app's existing `userMessageFor`-style error handling elsewhere.

---

## 4. Alignment: Membership handling

**Direct-assign is superseded, not extended.** The existing `manager/domain/membership/SalonMember.kt`, `manager/domain/repository/SalonMembershipRepository.kt`, `manager/data/BackendSalonMembershipRepository.kt`, `data/remote/SalonMembershipApi.kt`, and `data/remote/dto/SalonMembershipDtos.kt` (list/assign-by-userId/remove) were built against a contract System 1 has now explicitly rejected in favor of invites (decision §2). Per this plan:

- **No further code should be built on top of this existing stack.** It already has zero screen call sites (per its own Phase 2/M4 doc comment — "no screen consumes this yet"), so nothing user-facing is broken by leaving it as-is for now.
- **It is not deleted by this plan** — deletion is a code change, out of scope here ("do not modify implementation files"). It is flagged as superseded, to be replaced by a real invite-flow client (`InviteApi`, `InviteRepository`, matching DTOs) once Invite flow implementation actually starts, and removed at that point rather than kept as a second, parallel membership-granting path.
- **`SalonMember`/`SalonMemberRole`** (the `MANAGER`/`RECEPTIONIST` enum) remains correct and reusable — the *role vocabulary* wasn't rejected, only the *assignment mechanism*. This type is still what a membership row's `role` field resolves to, both under the old (dead) direct-assign shape and the new invite-accept shape.

---

## 5. Alignment: Booking DTO expectations

Per decision §3, `BookingResponseDto` (`data/remote/dto/BookingDtos.kt`) will need additive fields once the backend ships the enrichment:

```
BookingResponseDto {
  id, salonId, startTime, endTime, status, notes, createdAt, updatedAt,
  serviceId, specialistId, customerId,        // unchanged — kept for compatibility
  service:    ServiceSummaryDto?,             // NEW: { id, name }
  specialist: SpecialistSummaryDto?,          // NEW: { id, name }
  customer:   CustomerSummaryDto?,            // NEW: { id, name, phone? }
}
```

**Nullable new fields, deliberately** — until the backend actually ships this, or during any transitional rollout, the app must not assume these are always present. `domain/repository/BookingRepository.kt`'s `Booking` domain type gains matching optional fields, mapped through the existing `toDomain()`-style conversion pattern already used throughout `data/remote/dto/`.

**Scope confirmation:** per decision §3, this is a **global** enrichment (every booking-returning endpoint, both Customer's `/bookings/mine` and Reception's `/salons/{salonId}/bookings`), not Reception-specific — this plan's Dashboard/Booking-management screens can rely on `service`/`specialist`/`customer` names being present without any Reception-only special-casing, once the backend change ships.

**Confirm/Complete bindings** (flagged in `ROJAN_System2_Android_Integration_Clarification_v1.md` §4 as a pure Android-side gap, no backend dependency): unaffected by this decision, still simply missing client-side. Listed again here only for completeness of the Booking DTO picture, not a new finding.

---

## 6. Updated screens/API/permission table

| Reception screen | Target endpoint(s) | DTO status | Permission (per decision §1c) |
|---|---|---|---|
| OTP Auth | `/auth/otp/*` | ✅ Real, unchanged | — (public) |
| Salon Selection / Dashboard resolution | `/users/me/salon-access` | Contract approved, not built | — (any authenticated user) |
| Invite preview | `GET /invites/{token}` | Contract approved, not built | Public |
| Invite accept | `POST /invites/{token}/accept` | Contract approved, not built | Authenticated + phone match; **blocked by §3's surfaced role-promotion gap** |
| Dashboard (real) | `GET /salons/{salonId}/bookings` | Enrichment approved, not built | `MANAGE_BOOKINGS` |
| Booking wizard | `POST /salons/{salonId}/bookings` | Enrichment approved, not built | `MANAGE_BOOKINGS` |
| Booking confirm/complete | `PATCH /bookings/{id}/confirm|complete` | Exists backend-side; Android binding still missing (unrelated to this decision) | `MANAGE_BOOKINGS` |
| Customer lookup | `GET /salons/{salonId}/customers` | Unchanged | `VIEW_CRM` (confirmed — decision §1c fixes Reception at read-only CRM access, not `MANAGE_CRM`) |

---

## 7. Updated implementation order

Unchanged in spirit from `ROJAN_Reception_Implementation_Plan_v1.md` §8, re-sequenced against System 1's approved dependency order (decision §4):

1. **Backend (System 1, not this team):** `SalonMembership` persistence → `/users/me/salon-access` → `SalonPermissionResolver` → `SalonInvite`/`InviteController` → authorization broadening. `active` flag and `BookingResponse` enrichment can ship independently, any time.
2. **Android, unblocked now, no backend dependency:** cosmetic update to `ReceptionAuthViewModel`'s doc comment (§2 above); Confirm/Complete Retrofit + repository bindings (identified in the Clarification report, still outstanding, no backend blocker).
3. **Android, blocked on backend step 1's `/salon-access`:** real Dashboard, Calendar, Booking wizard (all already scoped in the v1 plan, unchanged by this update except for consuming the richer `BookingResponse` once available).
4. **Android, blocked on backend `SalonInvite`/`InviteController` AND the role-promotion gap this document surfaces:** Invite preview/accept screens (§3). This is now the most-specified but also most-blocked item — well-defined once the gap is resolved, not startable before then.

---

## Open items requiring further decision before implementation

1. **The invite-acceptance-for-a-brand-new-phone-number gap (§3)** — not addressed by `ROJAN_System1_Backend_Decision_v2.md`, blocks Invite flow specifically, does not block Dashboard/Booking work.
2. Everything else from `ROJAN_Reception_Backend_Dependency_Checklist_v1.md`'s original open-questions list is now resolved by the System 1 decision, except this one.

**No code will be written until this plan is reviewed and approved.**
