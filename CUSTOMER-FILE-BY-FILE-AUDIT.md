# ROJAN AI Customer App — File-by-File, Feature-by-Feature Release Audit

Date: 2026-09-14
Scope: `ai.rojan.designlab` Customer-flavor source only — `screens/` (excluding
none), `presentation/`, `data/`, `domain/`, `di/`, `ui/`, `components/`,
`navigation/`. `manager/` and `reception/` package trees are separate flavors
and are explicitly **out of scope** for this pass (one cross-cutting Manager
bug found incidentally is reported below for visibility, not fixed here).

Method: 12 independent, parallel research passes (one per functional/
architectural area, six of them a literal file-by-file inventory covering
every `.kt` file in scope — ~280 files total) each re-derived findings from
current source, not from any prior audit doc. The main session then
independently re-verified the highest-severity claims against source and
against the existing test suite before accepting them (one YELLOW claim was
investigated and found to be **incorrect** — see the domain/booking/rules
section — and reverted rather than "fixed").

---

## TOP-LINE FINDING — read this first

**A second, parallel design-system implementation is running in production
across ~20+ Customer screens, in direct conflict with this repo's own
frozen governance (`CLAUDE.md`'s "Shared Premium Glass Design System"
section: "never a forked component or a second implementation of the same
mechanic").**

`screens/customer/components/CustomerRefComponents.kt` ("Quiet Luxury" /
"DESIGN FOUNDATION") defines `RefSurface`, `RefListRow`, `RefSelectableCell`,
`RefPrimaryButton`, `CustomerSectionLabel`, etc. — a flat, bordered,
semi-transparent `Box` mechanic with **no blur, no shadow stack, no metallic
border** — as a declared replacement for the canonical `PremiumGlassSurface`/
`GlassSurface` mechanic. Confirmed live (not leftover, not dead) in
`SpecialistProfileScreen.kt`, `ServiceDetailsScreen.kt`, `SalonListScreen.kt`,
`SearchScreen.kt`, `SalonDetailsScreen.kt`, `PublicSalonScreen.kt`,
`ProfileScreen.kt`, `FollowedSalonsScreen.kt`, `FavoritesScreen.kt`,
`BeautyDnaScreen.kt`, `AppointmentsScreen.kt`, `AppointmentDetailsScreen.kt`,
`CustomerHomeScreen.kt`, `CustomerDashboardScreen.kt`,
`BookingConfirmationScreen.kt`, `BookingSuccessScreen.kt`, `AuthScreen.kt`,
`RescheduleAppointmentScreen.kt` — i.e. the large majority of Customer's
non-legacy screens. The component's own header comment says other
"un-migrated screens adopt these next," meaning this is a **deliberate,
ongoing expansion**, not an isolated leftover or a regression introduced
recently.

Two independent agents (RTL/theme audit, profile-screen inventory)
independently converged on this same finding from different entry points,
which is strong corroboration it's real and not a one-off misreading.

**This is not treated as a bug to silently fix or revert** — per this
project's own `CLAUDE.md` ("On any conflict... STOP and report"), and
because a 20+-screen visual migration in either direction is exactly the
kind of "architecture changes" / "changing project structure" this repo's
rules require explicit confirmation for before touching. **This needs an
explicit decision**: either this flat "Quiet Luxury" system is the actual
intended current direction (in which case `CLAUDE.md`'s frozen section is
stale and needs to be updated to reflect it as the new baseline), or it's
genuinely out of compliance and these ~20 screens need a deliberate,
reviewed migration back onto `PremiumGlassSurface`. No code was changed for
this finding.

Smaller instances of the same pattern (a second implementation instead of
reusing the canonical one) also found: `CustomerTextField`/
`CustomerSearchField` vs. `RojanTextField`/`RojanSearchField`;
`CustomerStates` (`CustomerLoadingState`/`EmptyState`/`ErrorState`, 11 real
call sites) vs. `RojanStateView`/`RojanLoadingState`/`RojanEmptyState`/
`RojanErrorState` (1 real call site total across the three). All are
functionally correct, well-built, and shipping — this is a documentation/
governance-compliance question, not a correctness defect.

---

## Design-system, RTL, and legacy-theme findings

| File:Line | Finding | Class |
|---|---|---|
| `screens/customer/components/CustomerRefComponents.kt` + 20 consumers | Second glass-mechanic implementation — see Top-Line Finding above | 🔴 Governance conflict (not a functional bug) |
| `components/GlassOrb.kt` (`FrostedGlassOrb`, `SplashScreen.kt:120,136,152,167`) | Bespoke translucent `Box` glow used for the splash's floating orbs, bypassing the shared `RojanAmbientGlow` primitive already used elsewhere (`HomeHeader.kt`) | 🟡 Recommendation — reroute to the shared primitive; deferred here since it's a cosmetic change on the app's first-impression screen with no way to visually verify a re-render in this environment (no reachable emulator/device this pass) |
| `components/GlassOrb.kt` (`GlassOrb()` function only, not `FrostedGlassOrb`) | Zero call sites anywhere except its own `@Preview` | 🔘 Dead code, confirmed by grep — not deleted (file deletion requires confirmation per this repo's rules) |
| `screens/customer/hometheme/HomeTextField.kt` | Hand-rolled glass-like input bypassing any shared mechanic — matches a gap `CLAUDE.md` itself already lists as unclosed (Phase 5), not a fresh regression. Also confirmed **fully dead** (zero call sites) by a separate pass | 🟡/🔘 |
| `screens/splash/SplashScreen.kt` `SplashText` | Raw `fontSize`/`fontWeight`/`letterSpacing` literals instead of `RojanTypography` tokens | 🟡 Isolated to the one-shot splash screen |
| RTL architecture (`ui/text/RojanText.kt`, `ui/components/rtl/RtlLayoutKit.kt`) | Deliberate, consistently-applied per-string bidi resolution (never flips ambient `LayoutDirection`) — confirmed clean across all Customer screens; one side-effect noted below | 🟢 |
| Every `Icons.AutoMirrored.*` icon (24+ call sites) | Because ambient `LayoutDirection` never flips, these icons never actually mirror — harmless today (nothing needs the flip) but a misleading doc comment overstates the benefit | 🟡 Documentation accuracy only |
| `screens/bookingflow/BookingTimeScreen.kt` time-slot grid | Confirmed the **only** file with the pre-approved fixed-LTR chronological exception; verified no other file copied the pattern | 🟢 Correct, documented exception |
| `screens/profile/RescheduleAppointmentScreen.kt` date-chip `LazyRow` | `reverseLayout = true` bug fix (from the pre-restart session) re-verified intact — now the only `LazyRow` in the app with no `reverseLayout`, consistent with every other carousel | 🟢 Confirmed fixed |
| Text alignment, `absolutePadding`, `LayoutDirection` overrides, raw `SimpleDateFormat` | Zero hits anywhere in scope | 🟢 |

---

## Booking-409 investigation (Section 9 of the task brief)

**Root cause found and independently confirmed against live production —
resolved to the extent this session's tooling allows.**

1. **Exact path**: `BookingConfirmationViewModel.confirmBooking()` →
   `BookingRepositoryImpl.createBooking` → `POST /api/v1/bookings` →
   backend `BookingController.create()` → `EnsureCustomerAssociationUseCase`
   → `CreateBookingUseCase` → `BookingRepositoryAdapter.reserve()`.
2. **Cause**: the authenticated salon-browse endpoint (`GET /api/v1/salons`,
   what a logged-in customer's booking flow actually uses to pick a salon)
   used to return salons regardless of `onboardingStatus`, letting a
   customer select a **DRAFT** (never-activated) salon, walk the entire
   booking UI, and only get rejected at the final POST with
   `SalonNotActiveException → 409 SALON_NOT_ACTIVE` — every single time,
   regardless of slot/date/retry, because the salon itself was always the
   problem. This exactly matches the historical QA report's symptom (4+
   tries, always 409, "fully-configured" pilot salon that was in fact never
   explicitly activated).
3. **Fix status — already shipped**: backend commit `2e59b3d`
   ("fix: hide inactive salons from customer booking discovery",
   2026-09-10) fixes exactly this, with a commit message that names this
   precise failure mode. Confirmed via `git merge-base --is-ancestor` that
   `2e59b3d` is an ancestor of `release/production-v24`'s current tip
   (`e15bdfb`).
4. **Confirmed against the actual running production container** (not just
   branch history) via SSH: `backend-app-1`'s deploy checkout
   (`/opt/rojan/backend`) is at commit `e15bdfb`, and `2e59b3d` is
   confirmed an ancestor of that exact commit. **Production is running the
   fix.**
5. **Remaining unknown, explicitly not resolved this pass**: whether the
   pilot salon has actually since had `POST /api/v1/salons/{id}/activate`
   called. A read-only Postgres query to check `onboarding_status` directly
   was attempted via the already-established SSH access and was **blocked
   by this session's own auto-mode safety classifier** (docker exec into
   the production database container) — this was not routed around, per
   this session's safety instructions. **This one item needs either a
   manual check by someone with direct VPS access, or an explicit
   permission grant to this session to re-attempt it.**
6. Every other 409 trigger on this path (`BookingConflictException`,
   `IdempotencyKeyConflictException`, `SpecialistNotEligibleForServiceException`)
   was confirmed to be a real, correctly-functioning business rule, not a
   bug — none of them can explain "409 on literally every attempt."
7. Client/server request-shape comparison found no mismatch (`startTime`
   format matches exactly).

**Conclusion: the code-level root cause is fixed and deployed. The release
gate below is now bounded by one narrow, concrete question (is the pilot
salon activated) rather than an open-ended "not root-caused" unknown.**

---

## Cross-app correctness bug found (Manager flavor — reported, not fixed, out of Customer scope)

`manager/presentation/booking/ManagerBookingViewModel.kt:227-264`'s
"book on behalf of a customer" flow calls `POST /api/v1/bookings`
(`BookingApi.createBooking`) with a `customerId` field, but the backend's
`CreateBookingRequest` DTO (`api/.../booking/BookingDtos.kt:12-27`) **has no
such field at all** — confirmed by direct read of both DTOs — and
`BookingController.create()` (line 103) unconditionally resolves
`customerId = currentUserResolver.resolve(principal)`, i.e. always the JWT
caller. **A manager booking on behalf of a customer silently creates the
booking under their own account instead**, and pollutes that salon's CRM.
The correct endpoint already exists and is correctly built
(`POST /api/v1/salons/{salonId}/bookings` / `CreateBookingForCustomerRequestDto`
/ `ManagerBookingApi.createForCustomer`) — `ManagerBookingViewModel` simply
isn't calling it. This is Manager-flavor code, explicitly out of this
Customer-app audit's scope to fix (`CLAUDE.md`: "Do not modify unrelated
files"), but is flagged here as urgent for a Manager-track fix.

---

## Fixes applied this pass (Customer scope only, all re-verified against the existing test suite)

| # | File(s) | What changed | Why |
|---|---|---|---|
| 1 | `presentation/booking/BookingConfirmationViewModel.kt`, `screens/bookingflow/BookingConfirmationScreen.kt` | `loadSummary` now captures and surfaces a real `summaryError` (new `retryLoadSummary()` added) instead of `getOrNull()`-swallowing every failure into permanent "—" placeholders with no way to tell the customer or retry; `submitError`'s text color changed from `HomeColors.TextSecondary` to `RojanErrorText` for visual consistency with `AuthScreen.kt`'s identical use case | Real RED bug: a network failure while loading the confirmation summary was completely silent |
| 2 | `screens/profile/BeautyDnaScreen.kt` | Corrected the on-screen disclosure from "saved only on this device" to "kept only while the app is open, cleared once it's closed" | The prior text was factually false — `InMemoryBeautyProfileRepository` is process-memory-only and clears on restart; nothing is actually saved on-device |
| 3 | `di/BackendApiContainer.kt` | The 3 separate `OkHttpClient` instances (authenticated / refresh-only / public) now share one `ConnectionPool` | Minor connection-reuse efficiency win, zero behavior change, all 3 clients already talk to the same host |
| 4 | `screens/customer/CustomerDashboardScreen.kt` | Added a real error+retry row (new `HomeSectionErrorRow`) for the salon-list and booking-history sections, which previously just silently vanished on a network failure with no message and no retry; removed a confirmed-dead `RefCardRadius` import | Real bug: Home tab going partially blank on failure with no way to recover short of leaving and re-entering |
| 5 | `screens/customer/CustomerHomeScreen.kt` | Removed the same confirmed-dead `RefCardRadius` import | Dead-code cleanup, zero behavior change |
| 6 | `app/src/test/.../presentation/booking/BookingConfirmationViewModelTest.kt` | Added 3 regression tests for the new `summaryError`/`retryLoadSummary` behavior | Coverage for fix #1 |
| 7 | `app/src/test/.../presentation/specialist/SpecialistSelectionViewModelTest.kt` (new file) | Added a full test suite (4 tests) | This ViewModel had zero test coverage, unlike every sibling booking-flow ViewModel |
| 8 | `app/src/test/.../presentation/booking/RescheduleViewModelTest.kt` (new file) | Added a full test suite (7 tests) | This is the actual live reschedule ViewModel (confirmed via `RescheduleAppointmentScreen.kt`'s import) and had zero coverage, while a dead, never-wired duplicate (`presentation/profile/RescheduleAppointmentViewModel.kt`) had full coverage instead |

**One change attempted and reverted**: `domain/booking/rules/BookingValidationRules.kt`'s
`isReadyForConfirmation` was initially changed to also require
`specialistId != null`, based on an inconsistency flagged by one research
pass. Before committing to it, the existing test suite was checked and
found `BookingEngineTest.kt:110-112` explicitly asserts
`` `isReadyForConfirmation does not require a specialist` ``, a
deliberate, already-tested design (the step resolver gates specialist
selection separately; `confirmBooking()` is the real final safety net for
a null specialist at submit time). **The edit was reverted.** Recorded here
per this project's own instruction to verify every fix against evidence
rather than trust a single research pass's claim.

---

## Confirmed dead code (zero call sites, verified by repo-wide grep — not deleted; this repo's rules require confirmation before deleting files)

- `screens/dashboard/DashboardPlaceholder.kt` and the separate, differently-packaged `ui/components/DashboardPlaceholder.kt`
- `screens/customer/{AISearchBar,FeaturedSalons,FollowedSalons,HomeHeader,NearbySalons,PopularServices,PromotionsSection,RecentVisits,RecommendedSalons,SearchModeTabs,TopSpecialists,UpcomingBookings}.kt` — all superseded by inline sections in the rebuilt `CustomerDashboardScreen`/`CustomerHomeScreen` per their own doc comments
- `screens/customer/hometheme/HomeTextField.kt` (doc comment claiming live callers is stale/incorrect)
- `screens/customer/hometheme/HomeGlassSurface.kt`'s `HomeRatingRow` (zero call sites); the rest of the file is transitively dead too (only called from the already-dead files above)
- `presentation/profile/RescheduleAppointmentViewModel.kt` + its factory + its own test — a fully-built, fully-tested, **never-wired** duplicate of the live `presentation/booking/RescheduleViewModel.kt`
- `presentation/common/InviteErrorMessages.kt` — disclosed prep code, not yet called, pending a backend `InviteController`
- `domain/booking/rules/{BookingAIRecommendationRules,BookingPromotionRules}.kt` — disclosed scaffolding, not yet referenced by `BookingEngine`
- `domain/identity/SessionState.kt`'s `AwaitingOtp`/`AwaitingFirstName` cases + `OtpVerificationResult` — leftover from a removed mock-OTP flow
- `domain/repository/CurrentUserIdentityContext.kt`'s `SalonPermissions` constants object — documented as the intended client-side permission-check pattern but never actually referenced anywhere
- `ui/assets/RojanAssetNames.kt`, `ui/components/ai/*` (5 files, disclosed unshipped foundation), `ui/components/brand/{RojanLogo,VersionFooter}.kt` (package/directory mismatch also noted), `ui/components/hero/HeroBookingCard.kt` (openly disclosed as removed by its own former callers' doc comments) + its orphaned `ui/components/hero`-adjacent `IllustrationPlaceholder.kt`, `ui/components/feedback/RojanSuccessCheck.kt`, `ui/components/input/RojanSearchField.kt`, `ui/components/loading/RojanShimmer.kt`, `ui/theme/Gradients.kt`'s `ImageScrim`, `components/{AIHeader,Badge}.kt`, `components/GlassOrb.kt`'s `GlassOrb()` function (not `FrostedGlassOrb`)

One claim corrected in the other direction: `CLAUDE.md` and `WarmBackground.kt`'s
own doc comment both claim it's dead — **false**, it has a live call site in
`reception/screens/splash/ReceptionSplashScreen.kt`. Not deleted, and the
stale doc claim should be corrected.

---

## Section tables (file-by-file)

### screens/{auth, booking, bookingflow, splash, dashboard}

| FILE | PURPOSE | CLASS | NOTES | ACTION |
|---|---|---|---|---|
| `screens/auth/AuthScreen.kt` | Phone→OTP login | GREEN | Byte-identical wiring to `AuthViewModel`; correct back-step behavior | none |
| `screens/booking/SalonListScreen.kt` | Booking Journey salon picker | GREEN | Debounced search, pagination, 401-retry-on-resume all present | none |
| `screens/booking/SpecialistSelectionScreen.kt` | Booking Journey specialist picker | YELLOW→GREEN | Had zero test coverage; now covered (fix #7 above) | none remaining |
| `screens/bookingflow/BookingConfirmationScreen.kt` | Order summary + payment + confirm | YELLOW→fixed | Silent error swallow + color inconsistency — both fixed (fix #1) | none remaining |
| `screens/bookingflow/BookingDateScreen.kt` | Date picker | GREEN | `launchSingleTop` fix for the historical BOOKING_TIME double-back-press bug confirmed present and correct | none |
| `screens/bookingflow/BookingSuccessScreen.kt` | Post-booking success | YELLOW | `summary: BookingSuccessSummary?` param/recap-card branch is dead in practice — its one real call site never supplies it | Wire it through, or remove the unused param/branch |
| `screens/bookingflow/BookingTimeScreen.kt` | Time slot grid | GREEN | Presentation-only day-part bucketing; documented LTR exception | none |
| `screens/bookingflow/components/{BookingRefComponents,BookingScaffold}.kt` | Thin compatibility forwarders to `customer.components.*` | GREEN | Real call sites throughout bookingflow; explicitly documented as temporary | Migrate imports directly and delete forwarders — non-blocking cleanup |
| `screens/splash/SplashScreen.kt` | App splash | GREEN | Readiness-gated, not a fixed timer; see startup section below | none |
| `screens/dashboard/DashboardPlaceholder.kt` | Unused shared placeholder | GRAY | Zero call sites confirmed | none (deletion candidate, needs confirmation) |

### screens/{customer, salon, search, service, specialist}

| FILE | PURPOSE | CLASS | NOTES | ACTION |
|---|---|---|---|---|
| `screens/customer/AISearchBar.kt` | Old Home search chip | GRAY | Zero call sites | none |
| `screens/customer/CustomerBottomBar.kt` | Bottom nav | GREEN | Correct RTL semantics | none |
| `screens/customer/CustomerDashboardScreen.kt` | Home tab | YELLOW→fixed | Silent section-hide on error — fixed (fix #4) | none remaining |
| `screens/customer/CustomerHomeScreen.kt` | Explore tab | GREEN | Best-in-class state handling in this scope | none |
| `screens/customer/CustomerMainScaffold.kt` | Bottom-bar shell | GREEN | Purely structural | none |
| `screens/customer/{FeaturedSalons,FollowedSalons,HomeHeader,NearbySalons,PopularServices,PromotionsSection,RecentVisits,RecommendedSalons,SearchModeTabs,TopSpecialists,UpcomingBookings}.kt` | Old Home sections | GRAY | All 11 confirmed zero call sites — superseded by inline sections | none (deletion candidates, needs confirmation) |
| `screens/customer/components/{CustomerBookingStatus,CustomerComingSoonScreen,CustomerConfirmDialog,CustomerRefComponents,CustomerScaffold,CustomerStates,CustomerStepIndicator,CustomerTextField}.kt` | "Quiet Luxury" shared foundation | GREEN (functionally) | See Top-Line Finding — governance-conflict flag applies to `CustomerRefComponents`/`CustomerTextField`/`CustomerStates` specifically | See Top-Line Finding |
| `screens/customer/hometheme/{HomeBackgroundTheme,HomeColors}.kt` | Dark-canvas theme | GREEN | Used app-wide | none |
| `screens/customer/hometheme/HomeGlassSurface.kt` | Glass wrapper | YELLOW | Transitively dead (only called from other dead files) except unused `HomeRatingRow` | Re-verify before any deletion pass |
| `screens/customer/hometheme/HomeTextField.kt` | Dark-glass input | GRAY | Zero call sites; doc comment claiming callers is stale | none |
| `screens/salon/PublicSalonScreen.kt` | QR/deep-link salon view | GREEN | Full state handling | none |
| `screens/salon/SalonDetailsScreen.kt` | Salon detail | YELLOW | Duplicates `RefSurface`/`RefRowDivider`/etc. as private local composables instead of importing the consolidated versions | Consolidate onto shared `CustomerRefComponents` |
| `screens/search/SearchScreen.kt` | Search | GREEN | Debounce + pagination correct | none |
| `screens/service/ServiceDetailsScreen.kt` | Service detail | GREEN | Full state handling, no fabricated fields | none |
| `screens/specialist/SpecialistProfileScreen.kt` | Specialist profile | GREEN | Full state handling | none |

### screens/profile (7 files)

| FILE | PURPOSE | CLASS | NOTES | ACTION |
|---|---|---|---|---|
| `AppointmentDetailsScreen.kt` | Booking detail + invoice | GREEN | Full state handling | none |
| `AppointmentsScreen.kt` | Booking list | GREEN | Confirm dialogs, reminder toggle wired | none |
| `BeautyDnaScreen.kt` | Beauty preferences | GREEN→fixed | Title/collapse/single-expand fixes re-verified correct and intact; misleading persistence disclosure fixed (fix #2) | Optional: auto-collapse after selection (not implemented, UX polish only) |
| `FavoritesScreen.kt` | Favorited salons | GREEN | Full state handling | none |
| `FollowedSalonsScreen.kt` | Followed salons | GREEN | Full state handling | none |
| `ProfileScreen.kt` | Profile hub | GREEN | Guest/logout gating fix re-verified correct and intact | Optional: "بیوتی DNA من" menu label vs. the screen's own "DNA" title — cosmetic, not implemented |
| `RescheduleAppointmentScreen.kt` | Reschedule picker | GREEN | `reverseLayout` fix re-verified correct and intact | none |

*(All 7 screens run on the `CustomerRefComponents` foundation — see Top-Line Finding.)*

### presentation/ (46 files)

| FILE | PURPOSE | CLASS | NOTES | ACTION |
|---|---|---|---|---|
| `auth/AuthViewModel.kt` | Session root | GREEN | Reactive logout propagation, suspend session restore | none |
| `auth/{AuthViewModelFactory,CustomerOtpStep}.kt` | Support | GREEN | | none |
| `beauty/BeautyProfileViewModel.kt` + factory | Beauty DNA state | GREEN | Disclosed in-memory-only | none |
| `booking/AppointmentDetailsViewModel.kt` | Booking detail load | GREEN | `runCatching` → `UiState.Error`, no silent swallow | none |
| `booking/BookingConfirmationViewModel.kt` + factory | Confirm + submit | GREEN (fixed) | See fix #1 | none remaining |
| `booking/{BookingDateViewModel,BookingTimeViewModel}.kt` + factories | Date/time selection | GREEN | Tested, nullable-id guards | none |
| `booking/BookingHistoryViewModel.kt` | Booking list | GREEN | Cancel-and-replace load guard | none |
| `booking/BookingViewModel.kt` + factory | Wizard state | GREEN | `SavedStateHandle`-backed, event-driven | none |
| `booking/ReminderViewModel.kt` | Local reminders | GREEN | Disclosed on-device-only, no real notification delivery | none |
| `booking/RescheduleViewModel.kt` | **Live** reschedule flow | GREEN (fixed) | Was untested; now covered (fix #8) | none remaining |
| `common/ErrorMessages.kt` | Error mapping | GREEN | Tested, never leaks raw text | none |
| `common/InviteErrorMessages.kt` | Invite error mapping | GRAY | Zero call sites, disclosed prep code | none |
| `common/UiState.kt` | Shared state shape | GREEN | | none |
| `profile/AppointmentsViewModel.kt` + factory | Cancel-capable booking list | GREEN | Double-cancel guard | none |
| `profile/ProfileMediaViewModel.kt` + factory | Avatar/cover upload | GREEN | Independent busy-flags per slot | none |
| `profile/RescheduleAppointmentViewModel.kt` + factory | **Dead** duplicate reschedule VM | RED | Zero production call sites; fully tested but never wired | Delete alongside its test, or wire in and retire the live one instead — needs confirmation |
| `relationship/{FavoriteSalonsViewModel,FollowedSalonsViewModel,SalonRelationshipViewModel}.kt` + factories | Follow/favorite | GREEN | Best-in-scope guest-mode handling (`SalonRelationshipViewModel`) | none |
| `salon/{PublicSalonViewModel,SalonDetailsViewModel,SalonListViewModel}.kt` + factories | Salon browsing | GREEN | `SalonListViewModel` correctly routes guests to the public endpoint | none |
| `service/ServiceDetailsViewModel.kt` + factory | Service lookup | GREEN | | none |
| `session/SessionViewModel.kt` + factory | Cold-start session restore | GREEN | Independent 5s hard timeout + non-swallowed cancellation | Consider a unit test given its criticality (not implemented — narrow, well-commented logic) |
| `specialist/{SpecialistProfileViewModel,SpecialistSelectionViewModel}.kt` + factories | Specialist browsing | GREEN (fixed) | `SpecialistSelectionViewModel` was untested; now covered (fix #7) | none remaining |

### data/, domain/, di/ (121 files)

All 25 Retrofit API interfaces, all 3 `data/identity`, all 3 `data/local`,
and all 18 repository implementations: **GREEN** — production base URL
(`https://api.rojanai.ir/`) correctly wired with a build-time guard, every
repository routes through `safeApiCall` (HTTP errors/timeouts/malformed
JSON/unexpected exceptions all become typed `Result.failure`, never a raw
leak), `CancellationException` correctly special-cased, `TokenAuthenticator`
only clears session on genuine 400/401/403.

| Area | Finding | Class |
|---|---|---|
| `data/remote/dto/{AuthDtos,BookingDtos,CustomerDtos,DashboardDtos,SalonMembershipDtos}.kt` | `NetworkUserRole`/`NetworkBookingStatus`/`NetworkCustomerStatus`/`NetworkRecommendationPriority`/`NetworkSalonRole` are plain enums with no unknown-value fallback — a future new backend value fails the *entire* containing response (caught safely as `MalformedResponseException`, not a crash, but the whole list/page still fails), inconsistent with `SalonAccessDtos`' `Set<String>` and `CustomerRelationshipRepositoryImpl`'s `runCatching`-wrapped mapping in the same codebase | YELLOW — recommendation only, not implemented. No active bug (backend/Android enums match exactly today); fixing it well requires new UI representations for "unknown" across ≥2 domain enums shared with Manager-flavor code (`AppointmentStatus`), which is a bigger, cross-flavor decision than this pass's safe-fix scope |
| `di/BackendApiContainer.kt` | 3 `OkHttpClient`s, no shared pool | YELLOW → fixed (fix #3) | none remaining |
| `domain/booking/rules/BookingValidationRules.kt` | Investigated a claimed inconsistency (specialist not required for `isReadyForConfirmation`) | GREEN — confirmed intentional and already tested; **not a bug** (see "Fixes applied" section) |
| `domain/booking/rules/{BookingAIRecommendationRules,BookingPromotionRules}.kt` | Disclosed empty scaffolds | GRAY | none |
| `domain/identity/SessionState.kt` | 3 dead cases from a removed mock-OTP flow | YELLOW | Remove now that real OTP fully replaced the mock flow — not implemented (scoped cleanup, needs confirmation since it's a public sealed-interface shape) |
| `domain/repository/CurrentUserIdentityContext.kt` | `SalonPermissions` constants never actually referenced | YELLOW | Wire real checks or drop the doc claim — not implemented |
| `presentation/profile/RescheduleAppointmentViewModel.kt` + factory | Dead duplicate (see above) | RED | Not deleted — needs confirmation |

### ui/, components/, navigation/ (63 files)

All of `ui/theme/`, `ui/motion/`, `ui/text/`, `ui/window/`,
`ui/components/{glass,buttons,cards,icon,interaction,navigation,rtl,image,input(partial),scaffold(partial),state}/`,
and `navigation/{RojanDestinations,RojanNavGraph}.kt`: **GREEN** — the
canonical `PremiumGlassSurface`/`PremiumMetallicBorder`/`RojanTokens`/
`RojanTypography`/`RojanDimens`/`RtlLayoutKit`/`GlassBackButton`/
`RojanPressable` primitives are confirmed live with real, heavy, cross-flavor
usage, and `RojanNavGraph.kt`'s ~1350 lines contain no dead/unreachable
route.

| File | Finding | Class |
|---|---|---|
| `ui/background/WarmBackground.kt` | `CLAUDE.md` and its own doc comment claim it's dead — **false**, live caller in `reception/screens/splash/ReceptionSplashScreen.kt` | YELLOW — correct the stale doc claim |
| `ui/components/scaffold/RojanScaffold.kt` | Framed as "the shared top-level wrapper" but has exactly 1 real caller | YELLOW |
| `ui/components/input/RojanSearchField.kt` | Framed as canonical but zero call sites — every real search bar hand-rolls its own | GRAY |
| `ui/components/loading/RojanShimmer.kt`, `ui/components/feedback/RojanSuccessCheck.kt` | Built to be adopted, never wired | GRAY |
| `ui/theme/Theme.kt` | `RojanTheme{}` wrapper is genuinely called everywhere, but its Material3 `ColorScheme`/`Typography`/`Shapes` content is read by essentially nothing (everything reads `RojanTokens`/`RojanTypography`/`RojanShapes` directly) | YELLOW — low priority |
| `ui/theme/RojanTokens.kt` | `RojanLuxuryCaption`'s doc comment claims a live caller that no longer exists | YELLOW — stale doc only |
| `ui/theme/Gradients.kt` | `ImageScrim` zero call sites | GRAY |
| `ui/components/hero/HeroBookingCard.kt` | Confirmed dead — openly disclosed as removed by its own former callers | RED (dead, not a live defect) |
| `ui/components/DashboardPlaceholder.kt`, `ui/assets/RojanAssetNames.kt`, `ui/components/ai/*` (5 files), `ui/components/brand/{RojanLogo,VersionFooter}.kt`, `components/{AIHeader,Badge,IllustrationPlaceholder}.kt` | Confirmed dead by direct grep | GRAY |

---

## Startup / Auth / Navigation architecture (re-verified fresh, not from a prior report)

- **Startup**: genuinely one unified experience for cold-launch, session-restored, and guest paths — the `RestoringSessionContent` branch in `RojanNavGraph.kt` is correctly unreachable by construction (traced the actual boolean gate, not just the comment claiming it). One minor gap: no client-side timeout if session restore hangs indefinitely on a stalled connection (not a double-screen bug — a rare, unexercised edge case). 🟢
- **Auth/session**: reactive, single-source-of-truth propagation confirmed — `TokenAuthenticator` only clears on genuine rejection, `AuthViewModel` centrally resets all state on any `null` personId emission (covers both explicit logout and a forced mid-session token death), every consumer reads reactively, no stale-UI risk found. 🟢
- **Navigation**: full route graph enumerated, no traps found. The historical "BOOKING_TIME needs two back presses" bug is **confirmed fixed** — `launchSingleTop = true` on the `BOOKING_DATE → BOOKING_TIME` push, traced through the `AUTH` round-trip case too (one system Back correctly returns to the previous step in both the direct and interrupted-by-login paths). 🟢

---

## States, performance, accessibility, security (Customer scope)

- **Loading/Empty/Error**: 18 screens confirmed clean with real, distinct handling; the `BookingConfirmationScreen` and `CustomerDashboardScreen` gaps found here are both fixed (fixes #1, #4). No hardcoded/demo data found standing in anywhere for real results.
- **Error messages**: `presentation/common/ErrorMessages.kt` is the single, consistently-used mapper; no raw exception/stack trace/HTTP status ever shown to a user. 🟢
- **Performance**: single shared `OkHttpClient` builder (now pool-shared, fix #3); no duplicate fetches found; Coil's default loader is correct (no auth-gated image URLs); one unmemoized `filter` in `SalonDetailsScreen.kt` (low severity, small lists, not fixed — cosmetic-perf only) and the splash's intentional ~2.6s branding pacing delay (by design, not a bug). 🟡 (minor, undone)
- **Accessibility**: touch targets, content descriptions, destructive-action confirmations all clean. 🟢
- **Security**: no hardcoded secrets, zero `Log.d`/`println` calls anywhere, `HttpLoggingInterceptor` hard-off (`NONE`) in release, no cleartext traffic outside a documented debug-only config, no unnecessary exported manifest components, no `WebView`. 🟢
- **Tests**: 39 Customer-relevant test files inventoried; only genuinely stale/environment-dependent ones are the two `localhost:8080`-dependent `BackendAuthFlowVerificationTest` cases (by design, self-documented as manual) and the standard `Example*Test.kt` boilerplate.

---

## API contracts (43 Customer-facing endpoints checked against `release/production-v24`)

All 43 match on path, method, and auth requirement. Five DTO-completeness
gaps found, all `ignoreUnknownKeys`-safe (no crash risk), none implemented
as fixes this pass since none represent an active defect:

- `PublicSalonSummaryResponseDto` is missing `city` (present server-side) and carries `phone`/`email`/`address`/`description`/`latitude`/`longitude` fields the browse endpoint never actually populates (the by-slug endpoint does).
- `SalonResponseDto` is missing `slug`/`onboardingStatus`/`city` (harmless for Customer, shared with Manager).
- `SpecialistResponseDto`/create/update DTOs are missing `mobileNumber`/`specialty`.
- `WorkingHoursResponseDto` is missing `createdAt`/`updatedAt`.

No `localhost`/`10.0.2.2`/`192.168.x` reference anywhere in `app/src/main`.

**Customer ↔ Manager**: no client-side cache exists anywhere in Customer's
`data/` layer — every screen reads live, so Manager-side changes to salons/
services/specialists/schedules/bookings/media are always immediately
visible to Customer. Fresh `UUID`-keyed storage on every media re-upload
means Coil can't serve stale bytes for a new upload either. Clean, except
the Manager-side booking-attribution bug reported separately above.

**Customer ↔ Website**: privacy policy covers deletion (policy-text-only,
already-known and acceptable per the prior audit's framing); no user-facing
"ROJAN_DesignLab" branding found anywhere; production URLs correctly
guarded on the website side; both sides genuinely have no deep-link scheme
(consistent, not a gap). Clean.

---

## Second-pass regression check

After applying fixes #1–#5, re-read every edited file's surrounding code and
its direct callers:
- `BookingConfirmationScreen.kt`: confirmed the new `summaryError` block
  only renders when non-null, doesn't shift the existing summary card's
  layout when absent, and `retryLoadSummary` correctly forces a re-fetch
  (bypasses the `loadedForKey` memo) rather than being a no-op.
- `CustomerDashboardScreen.kt`: confirmed `HomeSectionErrorRow` is only
  reachable on a genuine `UiState.Error`, never shows alongside the
  Loading/Success branches, and both call sites pass the correct
  ViewModel's own `retry()`.
- `di/BackendApiContainer.kt`: `ConnectionPool` sharing is additive only —
  no client's `Authenticator`/interceptor chain or per-client behavior
  changed.
- `BeautyDnaScreen.kt`: confirmed the corrected string is the only line
  changed; no other reference to the old copy exists elsewhere.
- Full unit test suite re-run after all edits (see final report for the
  pass/fail count) — no test written or modified was deleted or weakened,
  and the one incorrect fix attempt was caught by an existing test and
  reverted before it could regress `BookingEngineTest`.

No navigation, RTL, visual, or API-contract regression risk identified from
any edit made this pass — all edits were additive (new error branches, new
tests, an import removal, a copy correction, and connection-pool sharing),
none removed or altered existing successful-path behavior.
