# ROJAN System2 Integration Preparation Report v1

**Branch:** `feature/android-reception-app`. **Scope:** Android integration preparation only, against `ROJAN_System1_Backend_Decision_v2.md`'s approved contracts. Not committed, not pushed.

**Untracked files reviewed before starting, as instructed:**

| File | Origin | Disposition |
|---|---|---|
| `ROJAN_Customer_Git_Status_Report_v1.md` | Pre-existing, unrelated Customer audit | Untouched, not part of this work |
| `ROJAN_System2_Android_Parallel_Work_Report_v1.md` | Earlier session, broader-than-auth report | Untouched, not part of this work |
| `manager/domain/repository/ManagerInviteRepository.kt` | Earlier session (Phase C, Manager issue-side) | Reviewed fresh — already correct, see §2 below, nothing changed |

Backend re-checked: `origin/feature/auth-rate-limit-finalization` unchanged (`28e98421`) since every prior check this branch has done — nothing in `ROJAN_System1_Backend_Decision_v2.md`'s approved contracts has moved.

**Rules verified, not just followed:** fresh grep for `Fake`/`Mock`/`Stub`/temporary-auth patterns across everything touched — zero matches. Fresh search for any `InviteApi` Retrofit interface or `@GET`/`@POST` invite route anywhere in the codebase — zero matches (the only hits were doc-comment *mentions* of the absent file, explaining why it doesn't exist). No backend file touched (re-confirmed via `git status` in `ROJAN_Backend`). No RBAC/permission-check logic added anywhere. No API contract (DTO/endpoint) changed.

---

## 1. SalonAccess repository interfaces

**Finding: already complete, nothing to add.** `domain/repository/CurrentUserIdentityContext.kt` (`CurrentUserIdentityContextRepository`, `OwnedSalonAccess`/`SalonMembershipAccess`/`SpecialistAccess`, `SalonPermissions`) was reviewed fresh against `ROJAN_System1_Backend_Decision_v2.md` §1a, which explicitly approved this exact shape as-is. It is already shared (used by all three apps), already wired into `di/BackendApiContainer.kt`, and already consumed by every auth ViewModel's `refreshIdentityContext()`. No new file, no change — building a second interface here would duplicate an already-approved, already-shared one.

## 2. Invite repository interfaces

**Finding: already complete on both sides, reviewed fresh, nothing to add.**

- `reception/domain/repository/ReceptionInviteRepository.kt` (accept-side: `getInvite`/`acceptInvite`) — re-read in full. Placeholder-only, matches §2's approved contract, zero Retrofit binding, zero implementation, zero DI wiring, zero UI. Confirmed still correct.
- `manager/domain/repository/ManagerInviteRepository.kt` (issue-side: `issueInvite`/`listInvites`/`revokeInvite`) — re-read in full. Same status, confirmed still correct.

Both were built in an earlier session under the same "placeholder only" rule this task restates — re-verified rather than assumed still valid, and found genuinely unchanged and compliant.

## 3. Backend error mapping preparation

**Finding: the one genuinely new item.** `ROJAN_System1_Backend_Decision_v2.md` §2 approved specific invite error codes (`INVITE_EXPIRED`, `INVITE_ALREADY_ACCEPTED`, `INVITE_REVOKED`, `INVITE_PHONE_MISMATCH`) that nothing in the codebase mapped to a user-facing message yet — neither `ErrorMessages.kt`'s generic `userMessageFor` (which only classifies by HTTP status, not `errorCode`) nor any invite-specific function.

**New file:** `presentation/common/InviteErrorMessages.kt` — `inviteErrorMessage(error: Throwable): String`, mapping the four approved codes to distinct Persian messages, falling back to the existing shared `userMessageFor` for anything else (unknown token `404`, network/timeout/other failures) rather than re-deriving that classification.

**Shared, not duplicated** — one function, not a Manager copy and a Reception copy. This directly follows the earlier "prefer shared fixes over duplicated fixes" direction: an invite error code means the same thing regardless of which app's future Invite screen receives it.

**Preparation only, not wired to anything** — no `ManagerInviteRepository`/`ReceptionInviteRepository` implementation calls it (they have no real implementation to call it from yet), and no screen/ViewModel references it. It exists purely so the mapping is typed and ready once `InviteController` ships and real Invite UI gets built — consistent with "no fake APIs, no mock responses": this function performs no network call and requires none to exist.

---

## Changed files

| File | Status |
|---|---|
| `presentation/common/InviteErrorMessages.kt` | New (this session) |
| `reception/domain/repository/ReceptionInviteRepository.kt` | Unchanged, reviewed |
| `manager/domain/repository/ManagerInviteRepository.kt` | Unchanged, reviewed |
| `domain/repository/CurrentUserIdentityContext.kt` | Unchanged, reviewed |

**Build verification:** `compileCustomerDevDebugKotlin compileManagerDevDebugKotlin compileReceptionDevDebugKotlin` — **BUILD SUCCESSFUL**, all three flavors.

No tests were added this session — this task's scope (unlike the prior Authentication Stabilization task) did not include a tests item, and `inviteErrorMessage` has no call site yet to exercise; a test would exist in isolation with nothing consuming it. Flagged here rather than silently added or silently skipped without mention.

---

## Remaining backend dependencies

Unchanged — this was preparation only:

- `SalonMembership` persistence + `GET /users/me/salon-access` still absent.
- `SalonInvite`/`InviteController` still absent on both the issuing (`POST /salons/{salonId}/invites`) and accepting (`GET`/`POST /invites/{token}...`) sides.
- The unresolved OTP-auto-registers-as-`CUSTOMER` gap for a brand-new invited phone number remains unaddressed by any System 1 decision.
- `inviteErrorMessage` has nothing to be exercised against until the above ships and real Invite screens are built.

---

**Not committed, not pushed.**
