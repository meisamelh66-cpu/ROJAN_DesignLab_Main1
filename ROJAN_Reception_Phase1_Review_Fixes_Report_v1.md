# ROJAN Reception Phase 1 Review Fixes Report v1

**Scope:** Applies only the three findings from `ROJAN_Reception_Phase1_Review_Report_v1.md` §4. Nothing else touched. No architecture change, no backend change, no mock data introduced, no commit, no push.

---

## Fix 1 — `ReceptionBookingViewModel.loadServices()` no longer hides partial failures

**Before:** if some categories' `getServices` calls succeeded and others failed, the method silently returned `UiState.Success` with only the successful categories — no signal that anything was missing.

**After** (`reception/presentation/booking/ReceptionBookingViewModel.kt`): any failure, partial or total, now surfaces as `UiState.Error` — never a silently-incomplete `Success`.
- **Total failure** (no category succeeded): same as before, the underlying error's message.
- **Partial failure** (some succeeded, some didn't): a new, distinct message — *"دریافت برخی خدمات (X از Y دسته) با خطا مواجه شد. لطفاً دوباره تلاش کنید."* — so a receptionist sees explicitly that the list is incomplete, not a clean success.

**Retry, added generically, not just for this one screen:** `ReceptionUiStateList` (`reception/components/`) gained an optional `onRetryClick: (() -> Unit)? = null` parameter — `null` by default, so every other existing call site (Dashboard, Customers, Specialist, DateTime) is unaffected and compiles unchanged. Wired only on the Service screen (`onRetryClick = viewModel::loadServices`), since that's the finding in scope — `loadServices()` is itself the retry path, already idempotent and safe to call again.

---

## Fix 2 — Duplicated `BookingResponseDto` mapper extracted

**Before:** `BookingRepositoryImpl.kt` and `BackendReceptionBookingRepository.kt` each carried an independent, private, byte-for-byte-identical copy of `BookingResponseDto.toDomain()` (plus the three summary mappers and the status mapper) — no compiler enforcement that a future change got applied to both.

**After:** new shared file `data/remote/dto/BookingResponseMapper.kt` — four public top-level extension functions (`BookingResponseDto.toDomain()`, `NetworkBookingStatus.toDomain()`, `ServiceSummaryDto.toDomain()`, `SpecialistSummaryDto.toDomain()`, `CustomerSummaryDto.toDomain()`). Both repositories now import and call this instead of maintaining their own copy — their private duplicate functions were removed entirely, not just deprecated.

**Behavior unchanged** — this is a pure extraction, verified by the fact every existing booking-related test still passes (see Verification below) and both repositories' public method signatures are untouched.

---

## Fix 3 — Confirm/Complete UI call sites added

**Before:** `BookingRepository.confirmBooking`/`completeBooking` existed (added in the controlled-implementation phase) but had zero call sites anywhere in the app.

**After** (`reception/presentation/dashboard/ReceptionDashboardViewModel.kt` + `reception/screens/dashboard/ReceptionDashboardScreen.kt`):
- `ReceptionDashboardViewModel` gained `confirmBooking(bookingId)`/`completeBooking(bookingId)`, both real calls through the already-registered `genericBookingRepository` (`ReceptionRepositories.genericBookingRepository` — no new wiring needed, it already existed in the container, just wasn't passed to this ViewModel before). A success re-`refresh()`es the list, so the row reflects the real new status rather than an optimistic guess; a failure surfaces via a new `actionError` state, shown inline above the booking list.
- Each `BookingRow` on the Dashboard now shows a status-gated action button: `PENDING` → "تأیید نوبت" (confirm), `CONFIRMED` → "ثبت انجام‌شدن" (complete), `CANCELLED`/`COMPLETED` → no button (terminal states). The button disables and shows a loading spinner (`PremiumButton`'s existing `enabled`/`loading` params) while that specific booking's action is in flight, tracked via a new `processingBookingId` state so only the row being acted on is affected, not the whole list.
- `ReceptionDashboardViewModelFactory` updated to pass `genericBookingRepository` through.

**Honest, unchanged status:** these buttons call the real, owner-only-gated endpoint — pressing one today will surface a real authorization error via `actionError`, not silently succeed or fall back to fake data. That's correct per "no mock data," not a regression.

---

## Files changed

| File | Change |
|---|---|
| `reception/presentation/booking/ReceptionBookingViewModel.kt` | Fix 1 |
| `reception/components/ReceptionUiStateList.kt` | Fix 1 (added optional retry param) |
| `reception/screens/booking/ReceptionBookingServiceScreen.kt` | Fix 1 (wired retry) |
| `data/remote/dto/BookingResponseMapper.kt` | Fix 2 (new file) |
| `data/repository/BookingRepositoryImpl.kt` | Fix 2 (duplicate removed) |
| `reception/data/BackendReceptionBookingRepository.kt` | Fix 2 (duplicate removed) |
| `reception/presentation/dashboard/ReceptionDashboardViewModel.kt` | Fix 3 |
| `reception/presentation/dashboard/ReceptionDashboardViewModelFactory.kt` | Fix 3 (wiring) |
| `reception/screens/dashboard/ReceptionDashboardScreen.kt` | Fix 3 (UI) |

No other file touched. `domain/repository/BookingRepository.kt` and `data/remote/BookingApi.kt` (modified in an earlier phase, not this one) are unchanged by this fixes pass — listed in `git status` only because they were already dirty from before.

---

## Verification

- `./gradlew assembleReceptionDevDebug` — **BUILD SUCCESSFUL**, no errors.
- `./gradlew testCustomerDevDebugUnitTest` — **112/114 pass**, identical result to before these fixes. The 2 failures are the same pre-existing, network-dependent `BackendAuthFlowVerificationTest` cases (can't reach the live backend from this sandbox) — unrelated to booking code, present before this session started, unchanged by it.

**No architecture change, no backend change, no mock data, no commit, no push.**
