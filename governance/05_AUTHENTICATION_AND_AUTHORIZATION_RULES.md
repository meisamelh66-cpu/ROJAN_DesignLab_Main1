# 05 — Authentication & Authorization Rules

**Set:** ROJAN Ecosystem Governance v2.0 · **Status:** MATERIALIZED (PASS G4)

---

## 5.1 Authentication is backend-owned **[NON-NEGOTIABLE]**

- Identity, credential verification, OTP issuance/verification, and token
  issuance happen only in the backend.
- There is no client-side authentication logic, no hardcoded bypass, no
  client-issued or client-forged token, no "developer mode" that skips real auth
  in a production build.
- The token mechanism, algorithm, and lifetime are backend decisions. Clients
  treat tokens as opaque.

## 5.2 Authorization is computed only in the backend **[NON-NEGOTIABLE]**

Permission flows in one direction only:

```
Login -> Identity -> Salon Access -> Membership -> Permission -> Client UI
```

- The client is **not allowed to compute a permission decision** — not
  authoritatively, not advisorily, not "to hide a button early."
- Permission is always derived from a valid backend Identity, never from a
  stored session, a cached role, or client state.
- RBAC changes (roles, permission rules, membership semantics) are backend
  changes and require ecosystem-owner confirmation.

## 5.3 The four concepts must never be conflated **[NON-NEGOTIABLE]**

| Concept | Definition |
|---|---|
| **Identity** | The user's real identity in the backend. The source of truth for who someone is. |
| **Session** | A temporary access credential (token) with its own independent expiry. |
| **Cache** | A reconstructable projection of data. Never proof of anything. |
| **Client State** | Local, temporary UI/form state. No business value. |

Rules:

- Session expiry **must never** mean data loss.
- Logout or session expiration invalidates **only the session**. Identity in the
  backend is untouched.
- Cache and Client State must **never** be used to prove or reconstruct Identity
  or Session.
- Refresh / re-authentication must complete **without any manual rebuild of
  business data**.

## 5.4 Failure scenarios are backend-managed

Access revocation, ownership transfer, member departure, branch change, and role
change are all handled by the backend. A client observes the new state on its
next authenticated request; it does not manage the transition.
