# 03 — API Contract Rules

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 3.1 Contract-first **[NON-NEGOTIABLE]**

Before any code is written for an endpoint, its contract is specified and
reviewed:

- **Request** shape
- **Response** shape
- **Error** cases and shape
- **Version**
- **Migration** impact (for any change to an existing endpoint)

## 3.2 The authoritative contract

- The human-readable contract lives in **`ROJAN_Backend/API_CONTRACT.md`**. The
  always-current machine-readable spec is served by the backend at its OpenAPI
  endpoint.
- Root Governance **references** this document; it does not copy it. The backend
  repository owns it.
- Clients mirror DTOs **field-for-field** against it. A client must not
  reinterpret a field's meaning, add computed fields to a response model, or
  filter a response beyond what the backend already returned.

## 3.3 Versioning **[NON-NEGOTIABLE]**

- Every business endpoint lives under a URI version prefix (`/api/v1/...`).
- A **breaking change ships as a new prefix** (`/api/v2/...`). An existing
  version prefix is never mutated in place.
- A breaking change is: removing or renaming a field, changing a field's type or
  meaning, tightening validation, changing an error code's conditions, or
  changing auth requirements.

## 3.4 No contract change without review **[NON-NEGOTIABLE]**

Changing a DTO shape, an endpoint path or method, a request/response field, or an
error condition — from **either** side (backend or client) — requires review
against this document and the Feature Process check. A client-side change that
assumes a not-yet-agreed contract is a governance violation.

## 3.5 Backward compatibility **[NON-NEGOTIABLE]**

Multiple clients are deployed independently and update on their own schedules. A
shipped contract must remain parseable by already-released clients until those
clients are provably retired. Backward compatibility outranks internal
cleanliness: when the two conflict (for example, a preferred error-format
standard versus an error shape a shipped client already parses), **the shipped
clients win**, and the cleaner form waits for the next version prefix.

## 3.6 Canonical cross-cutting contract conventions

Set by the canonical backend, binding on all clients:

- **Error responses:** one consistent JSON error shape for every error
  (validation, not-found, auth, conflict, server error), carrying a correlation
  id. *(The ecosystem uses the canonical backend's current custom error shape.
  An alternative standard — e.g. RFC 7807 — is not adopted ecosystem-wide; see
  `UNRESOLVED_CONFLICTS.md` C-4.)*
- **Cross-tenant access:** a resource that exists but belongs to a different
  tenant returns **404, not 403** — cross-tenant existence is neither confirmed
  nor denied.
- **Pagination:** unbounded result sets are paginated with a documented
  envelope; page size has a hard maximum enforced by the backend, and a request
  above it returns a client error. Clients must not request more than the
  documented maximum.
- **Idempotency:** mutating endpoints accept an `Idempotency-Key` header (see
  Root 02 section 2.6).
- **Media pipeline:** uploaded imagery follows the ecosystem media standard
  (Logo ~1024px, Cover ~1600px, Gallery ~2048px, JPEG quality ~80%), with
  technical resize/compression decided by the system, not the user.
