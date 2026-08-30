# 01 — Product Constitution

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 1.1 Founding principle **[NON-NEGOTIABLE]**

**The backend domain model is the final source of truth for every business fact
in ROJAN.**

Every business fact has exactly one authoritative record, held by the canonical
backend (`ROJAN_Backend`) in its PostgreSQL database. No client — Website,
Android, Desktop, or any future product — owns a business domain, and no
client-held copy of a business fact may ever be more current or more
authoritative than the backend's.

## 1.2 Products and their consumer status

All ROJAN products are **consumers** of the backend over REST/JSON:

| Product | Responsible for | Explicitly not responsible for |
|---|---|---|
| **Website** | Salon registration, owner onboarding, public salon presence, app-download distribution | Database, permission computation, business-decision logic |
| **Desktop** | Central salon management, organizational operations | Database, permission engine, business-decision logic |
| **Manager (Android)** | Salon operations within backend-granted access | Independent salon creation, access/permission decisions |
| **Reception (Android)** | Front-desk operations within backend-granted access | Same as Manager |
| **Specialist** (future) | Own schedule/work within backend-granted membership | Same |
| **Customer (Android / Web)** | Consuming the public salon experience, booking | Any staff-facing or administrative surface |

No client creates an independent business model. No client owns a domain.

## 1.3 Domain ownership map **[NON-NEGOTIABLE]**

Every domain below is owned solely by the canonical backend:

| Domain | Owner |
|---|---|
| Identity | Backend |
| Salon | Backend |
| Membership | Backend |
| Permission | Backend |
| Media | Backend |
| Booking (including Calendar / Availability) | Backend |
| Customer Relationship | Backend |
| Tenant data | Backend |

Calendar and Availability are **part of the Booking domain**, not a separate
domain. Creating an independent business model for any of these in a client is
forbidden.

**Domains designed but not yet owned by the canonical backend** (website-builder,
commerce, finance, communication — currently existing only as dormant
`platform-core` design): if built, they are added to this table under
`ROJAN_Backend`, under the same rules. They do not create a second backend.

## 1.4 Tenancy

- Today's tenant is the **Salon**. Every business query and mutation is
  salon-scoped.
- Organization / multi-branch tenancy, when it arrives, is a **second dimension
  layered above Salon**, never a replacement for it.

## 1.5 Five-year direction

ROJAN must be able to support: thousands of salons, multi-branch operations,
large organizations, a marketplace, and AI services. **A short-term solution
must not foreclose any of these.** AI capabilities are delivered as an
independent, optional module — never a hard dependency of a core flow.

## 1.6 Product values

- **Persian-first, RTL-first** user experience across every product.
- **Premium material design language**, expressed per-product through
  color/brand, never through a forked rendering mechanism.
