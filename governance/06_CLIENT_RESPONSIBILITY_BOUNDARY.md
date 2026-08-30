# 06 — Client Responsibility Boundary

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 6.1 A client's role, stated positively **[NON-NEGOTIABLE]**

Every ROJAN client — Website, Android, Desktop, and every future product — has
exactly this role:

1. **Collect input** from the user.
2. **Submit it as a request** to the canonical backend — with an
   `Idempotency-Key` whenever the request mutates state.
3. **Render whatever the backend returns.**

A client may additionally hold a read-side projection for display (Root 04
section 4.5) and local non-business state. It may do nothing else with business
meaning.

## 6.2 What a client must never do **[NON-NEGOTIABLE]**

- Compute whether a business mutation should succeed (permission, conflict,
  duplicate, eligibility) — even non-authoritatively.
- Hold a business record it treats as more current than the backend.
- Create an independent business/domain model.
- Reinterpret, extend, or filter a contract field beyond the backend's response.
- Fall back to fabricated data when the backend is unavailable (Root 10).
- Embed authentication or authorization logic (Root 05).

## 6.3 What is entirely the client's own

Each client repository owns, and Root Governance does not constrain:

- Programming language, UI framework, build system, dependency management.
- UI/UX design system, visual language, navigation model, animation.
- Local storage mechanism, offline projection strategy, session persistence.
- Packaging and distribution format (APK/AAB, installer/ZIP, container/static
  site).
- Platform lifecycle handling, threading, rendering.

These are Tier-1 (Project) concerns. A client's Project rules **may add**
constraints on top of Root; they may not weaken a Root NON-NEGOTIABLE.

## 6.4 Shared client obligations

Every client, in its own stack:

- Applies Clean Architecture with dependencies pointing inward, enforced by an
  executable architecture-test suite in CI.
- Fails loudly on missing critical configuration (e.g. a blank backend base URL)
  rather than silently degrading.
- Runs a build + unit + architecture-test gate before merge and before release
  (Root 07 section 7.4).
- Follows Git Discipline (Root 09).
