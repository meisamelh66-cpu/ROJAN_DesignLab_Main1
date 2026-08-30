# 10 — No Fake Data Policy

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

*This document makes explicit a practice that was consistent but unwritten across
all four repositories.*

---

## 10.1 Core rule **[NON-NEGOTIABLE]**

**Production code must not silently fall back to fake, mock, demo, or placeholder
data.** When real data or a real backend response is unavailable, the code fails
loudly — a visible error, a stated empty state, a refused start — never a
fabricated success.

## 10.2 What this forbids in a production code path

- A repository or service that returns hardcoded/in-memory sample entities when
  the backend call fails or is not wired.
- A "demo mode" or "offline fallback" that presents invented salons, bookings,
  customers, or metrics as if real.
- Placeholder URLs, fake response objects, or stubbed contract types standing in
  for a real integration.
- A screen that renders its design-time preview defaults indistinguishably from
  real data when the real fetch has not completed or has failed.
- Silently substituting a default when a required configuration value (e.g.
  backend base URL) is blank.

## 10.3 What is allowed

- **Tests**: fakes, mocks, stubs, in-memory repositories, fixtures — freely, in
  test code only.
- **Design-time previews / storybook-style tooling**: sample data, clearly
  confined to preview code, never reachable by a running production build.
- **Honest "coming soon" / "not available" states**: a screen for an unbuilt
  feature says so plainly; it does not simulate the feature with fake content.
- **Explicit, labeled seed data** for local development, behind a dev-only
  profile, never in a production build.

## 10.4 Backend unavailability

When the backend is unreachable or returns an error, a client shows an error or
empty state and offers retry. It never reconstructs "what the answer probably
was" from cache or client state (reinforces Root 04 section 4.5 and Root 05
section 5.3).

## 10.5 Existing violations

Known fake-data fallback paths that predate this policy (identified in
discovery) are logged as remediation items, not grandfathered. Each is removed
or gated behind a dev-only profile before the component it lives in next
releases.
