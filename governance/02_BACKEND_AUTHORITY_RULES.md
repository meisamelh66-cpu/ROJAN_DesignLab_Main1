# 02 — Backend Authority Rules

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 2.1 The canonical backend **[NON-NEGOTIABLE]**

`ROJAN_Backend` (package `ai.rojan.backend`) is the one and only authoritative
backend for the ROJAN ecosystem. It is the system of record, the contract owner,
and the deployment that serves production.

- `ROJAN_Web/platform-core` and `ROJAN_Desktop/Rojan.Server` are
  **non-canonical**. No client integrates with them. Their code is dormant.
- Any proposal to replace, fork, or split the canonical backend is an
  ecosystem-owner decision, documented as an ADR, never an incremental drift.

## 2.2 Business authority is computed once, in the backend **[NON-NEGOTIABLE]**

Any decision that determines a business outcome — *is this allowed, does this
conflict, is this a duplicate, is this permitted* — is computed **exactly once,
in the backend, inside a single transaction boundary.**

- This already applies to Permission and to Booking conflict resolution.
- It applies **by default** to every future domain (media validation, salon
  verification, pricing, promotions, ...) without re-derivation.
- A client may not compute such a decision even "advisorily" or "for UX." The
  advisory middle ground is explicitly rejected ecosystem doctrine.

## 2.3 Backend as a closed contract surface

- Clients communicate with the backend **only** over its published REST/JSON
  API. No shared database, no shared message bus, no shared library that encodes
  business rules.
- The backend does not depend on any client. Client needs are met by adding to
  the API contract (Root 03), never by a client reaching around it.

## 2.4 Clean architecture, enforced

The backend follows Clean Architecture with dependencies pointing inward,
enforced by an executable architecture-test suite that runs in CI. A boundary
violation is a build failure, not a review comment. *(Each client repository
applies the same principle in its own stack — see Root 06.)*

## 2.5 Change control

The backend's shipped behavior is a frozen baseline. Extend it additively (new
endpoints and use cases consuming existing primitives). Any architectural change
— auth mechanism, module structure, persistence strategy, tenancy model —
requires explicit ecosystem-owner sign-off first, recorded as an ADR.

## 2.6 Reliability standard for mutations **[NON-NEGOTIABLE]**

Every state-mutating backend operation must provide, or have a documented plan to
provide:

1. **Idempotency** — a repeated request carrying the same `Idempotency-Key`
   produces no duplicate effect.
2. **Duplicate handling** — the backend detects and rejects or no-ops
   duplicates; never delegated to a client.
3. **Conflict handling** — resource conflicts (e.g. double-booked slots) are
   detected **at the moment of mutation**, inside the transaction, not
   afterward.
4. **Retry safety** — a client may always retry; a retry reuses the original
   idempotency key and never corrupts business state.
5. **Transaction boundary** — the whole mutation completes in one backend
   transaction; no partial mutation is ever visible to a client.

Booking's `create` path is the reference implementation. Booking's
confirm/cancel/complete/reschedule endpoints and every other mutating endpoint
(media, salon, CRM, ...) inherit this standard as they are built or hardened.

## 2.7 Observability

Structured logging around every mutating operation (operation type, idempotency
key, outcome, latency) is a first-class requirement, not an afterthought.
Metrics are exposed on a scrapeable endpoint. The audit-event capability is
wired to real domain events as they are added.
