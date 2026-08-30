# 04 — Database Ownership

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 4.1 One database, one owner **[NON-NEGOTIABLE]**

The canonical backend owns the schema and the data for every domain in Root 01
section 1.3. There is exactly one authoritative datastore. No client has direct
database access. No client holds a second copy of a business record that it
treats as authoritative.

## 4.2 Schema is migration-owned **[NON-NEGOTIABLE]**

- The schema is defined and evolved only through versioned migrations (Flyway).
- The ORM runs in **validate** mode — it never mutates the schema itself.
- Migrations are forward-only in production. A migration that cannot be safely
  rolled forward past, or reversed by an explicit compensating migration, is not
  ready to ship.

## 4.3 Migration safety **[NON-NEGOTIABLE]**

Before any release containing a migration:

- The migration is assessed against the Feature Process check (Migration Impact,
  Failure Scenarios).
- Backward compatibility with the currently-deployed application version is
  verified (expand-then-contract: add columns/tables before code depends on
  them; remove only after no code depends on them).
- **A code rollback does not imply a schema rollback.** Deploy scripts revert
  application code only; schema is never auto-reverted. Any release whose
  migration is not forward-compatible with the previous application version must
  state its rollback procedure explicitly before it ships.
- A database backup is taken immediately before any production deployment that
  includes a migration.

## 4.4 No update may destroy business data **[NON-NEGOTIABLE]**

No migration, deployment, or update — backend or client — may silently drop,
overwrite, or corrupt customer business data (salons, bookings, customers,
media, configuration). A destructive migration requires an explicit, named
approval and a verified backup.

## 4.5 Client-side storage

- A client may hold a **read-side projection** for display: read-only,
  reconstructable from the backend at any time, never guaranteed current beyond
  the moment it was fetched, never an input to a business decision.
- A client may hold **local non-business state**: UI state, form state, session
  token, cached preference. This has no business value and no synchronization
  requirement.
- A cache that cannot be dropped at any instant and rebuilt from the backend
  with zero data loss is not a compliant cache.
- Recovery from any uncertain state (crash, timeout, lost connection) is
  **refetch from the backend**, never local reconciliation.
