# 08 — Update Policy

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

*This document governs what happens to an already-installed/deployed component
when a new version reaches it. It is net-new to the ecosystem (only fragments
existed).*

---

## 8.1 Operational continuity **[NON-NEGOTIABLE]**

An update must not unexpectedly interrupt salon operations. Backend and infra
updates use graceful shutdown and health-gated cutover. A client update must not
force a salon offline mid-operation without warning.

## 8.2 Data survives updates **[NON-NEGOTIABLE]**

- **Application data is independent of the application binary.** Databases,
  uploaded media, secure local stores, and local projections live outside the
  deployable artifact (bind-mounted volumes for server components; OS-managed
  app storage for clients).
- An update **preserves** local data, session, settings, and salon
  configuration. A user does not re-authenticate or reconfigure because a new
  version was installed, unless a security reason explicitly requires it (and
  then the reason is stated).
- **No update may silently damage, overwrite, or delete customer business data**
  (restates Root 04 section 4.4 at the update boundary — same NON-NEGOTIABLE
  force).

## 8.3 Compatibility before rollout **[NON-NEGOTIABLE]**

Before an update is rolled out:

- Backward compatibility with the data and the contract version the target
  already has is verified.
- Migration safety (Root 04 section 4.3) is confirmed.
- The `minimumSupportedVersion` metadata (Root 07 section 7.3) is honored: a
  target below it is told to take an intermediate update first, never
  force-migrated across an unsupported gap.

## 8.4 Update classes

- **Normal updates** follow the full Release Policy validation gate and staged
  rollout.
- **Critical updates** (security fix, data-integrity fix, production outage) may
  follow an expedited approval path — but never skip section 8.2 (data safety)
  or section 8.3 (compatibility). The expedited path is defined in advance and
  names who may authorize it.

## 8.5 Controlled installation (forward-looking)

Future client update systems (auto-update, staged/phased rollout,
forced-minimum-version enforcement) must support **controlled installation**:
the ability to pause, target a subset, and roll back a client-side rollout.
Until such a system exists, client updates are manual and this clause is a
design constraint on building one, not a current obligation.

## 8.6 Download route stability

Public download routes for client binaries remain stable across releases. A
versioned artifact filename may change; the route a user or a link points at
does not. A download only goes live when both an editorial decision and a
verified real artifact agree — a premature flag results in a clean "not
available" response, never a broken link.
