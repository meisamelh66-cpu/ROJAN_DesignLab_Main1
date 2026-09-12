# Customer Home — Visual Specification v1 (adopts the Salon Detail golden reference)

**Status:** proposed. Implemented in `screens/customer/CustomerDashboardScreen.kt` this pass. **Not committed** — awaiting review.
**Screen:** the Customer's post-auth landing — route `RojanDestinations.CUSTOMER_HOME`, rendered by `CustomerDashboardScreen` (the bottom bar's **خانه** tab). `CustomerHomeScreen` (route `EXPLORE`, the **جستجو** tab) is a different screen and is **out of scope** — untouched.
**Scope:** this one screen only. No ViewModel, repository, domain, API, navigation graph, auth, or backend-contract change. No shared component / design-system token / colour / icon edited. Everything below is screen-local `private` code in the one file.
**Direction:** the approved *quiet luxury · dark editorial · Persian-first* language from `REFERENCE-SPEC-salon-detail.md` — same `Ref*` tokens, same flat surface, same solid rose-gold CTA, same outlined icons.

---

## 1. Removed vs. the current Home

| Removed | Replaced with |
|---|---|
| `HomeHeader` — glass app-bar chip + blurred glow wash + `RojanAmbientGlow` behind a filled-Person avatar + `KeyboardArrowDown` + a dead notifications bell (`onNotificationsClick` is unwired on this route) | flat greeting row: "سلام {نام} جان" + a 40dp outlined-Person avatar button (→ profile). 1px bottom hairline. No glass, no glow, no bell. |
| `HeroBookingCard` — 360dp glass card, 2 gradient tint layers + blurred glow twin, AI salon photo (`salon_demo_1`) with a white border, `PremiumButton` gradient pill | one full-width **solid rose-gold** button, 52dp, 12dp radius — "رزرو نوبت" (same `onBookAppointmentClick`). The primary action is now unmissable and quiet. |
| `HomeSearchEntry` — `HomeGlassSurface` (metallic border + corner sparkles) | flat `RefSurface` (4.5% white fill + 9% hairline, 14dp), outlined search glyph. |
| `FeaturedSalons` — `HomeCard` (per-salon tinted glass, `accentColor`, `salonAccentColorFor`), giant centred `Storefront` filling a 90dp band, name/address crammed under it | flat `RefSurface` salon card, 200dp: small outlined storefront tile, then salon name (`Body`), then a `Place` + address meta row. Content-first. |
| `RtlSectionHeader` (`RojanTypography.Body`, full-weight white) | `RefSectionLabel` — `Caption`/SemiBold, muted, caps-quiet. |
| `RecommendedSalons` → `RojanComingSoonState` (NoOp AI provider — no engine exists) | **not composed on Home.** Shared composable stays in the codebase for `CustomerHomeScreen`. |
| `TopSpecialists` → `RojanComingSoonState` (no cross-salon endpoint exists) | **not composed on Home.** |
| `FollowedSalons` → `RojanComingSoonState` (no follow-persistence endpoint exists) | **not composed on Home.** |
| `UpcomingBookings` / `RecentVisits` always rendering their `RtlSectionHeader` even with zero items | section (label + content) renders **only when it has real data**. |
| `CustomerBottomBar` — protruding 64dp glowing Home disc + rotating metallic ring + `RojanAmbientGlow`; other 4 icons ~20dp filled | screen-local `HomeBottomBar`: 5 equal-weight tabs, 24dp **outlined** icons on one baseline, flat, 1px top hairline, active = rose-gold + a 3dp dot. Same `CustomerHomeTab` enum, same `onTabSelected` wiring. Shared `CustomerBottomBar` untouched (still used by `CustomerHomeScreen`). |

## 2. Kept

- Route, `CustomerDashboardScreen` public signature, every `on*` callback, the nav-graph wiring — **byte-identical**.
- `HomeBackgroundTheme` dark-navy ground (`applyContentInsets = false`; header takes `statusBarsPadding`, bottom bar takes `navigationBarsPadding`).
- Real data, real ViewModels: `SalonListViewModel` (`GET /api/v1/salons`, same factory `FeaturedSalons` uses), `BookingHistoryViewModel` (`GET /api/v1/bookings/mine`, one instance shared by upcoming + recent — as today).
- `AuthViewModel.currentDisplayName` for the greeting (same first-name derivation as `HomeHeader`).
- `ai.rojan.designlab.ui.text.Text` (content-direction), `rojanPressable` press feedback, `UiState`, `BookingStatus` filters, the `onSalonClick` / booking / explore / profile intents.
- Nothing fabricated, nothing mocked, no real feature removed (the three dropped sections have no backend behind them and only rendered a "به‌زودی" card).

## 3. Colour (screen-local constants — identical to the salon-detail reference, zero token edits)

| Role | Value | Origin |
|---|---|---|
| Ground | navy → deep-purple wash | `HomeBackgroundTheme` (unchanged) |
| Surface fill (cards / rows / search / avatar) | `White @ 4.5%` | `RefSurfaceFill` |
| Hairline | `White @ 9%` | `RefHairline` |
| Divider / row separator | `White @ 7%` | `RefDivider` |
| **Accent** (CTA fill, active tab, active-tab dot, salon-card monogram) | `#E0A67A` | `RojanPremiumBorderRoseGold` |
| Accent-on (CTA label) | `#1B1530` | `RefOnAccent` |
| Text primary | `#FFFBFF` | `HomeColors.TextPrimary` |
| Text secondary | `#CBBEE0` | `HomeColors.TextSecondary` |
| Text muted / meta / inactive tab | `#9C8FB5` | `HomeColors.TextMuted` |

No pink, magenta, violet-glow, gold-metallic, gradient, or AI photography anywhere on this screen.

## 4. Typography (`RojanTypography` subset)

| Element | Style |
|---|---|
| Greeting | `SectionTitle` (24/SemiBold) · primary · 1 line · ellipsis |
| Search placeholder | `Caption` · muted |
| CTA label | `Button` (16/SemiBold) · `RefOnAccent` |
| Section label | `Caption.copy(SemiBold)` · muted |
| Salon name / booking salon | `Body` (17/Medium) · primary · 1 line |
| Address / date / specialist / time | `Caption` (15/Regular) · muted |

## 5. Spacing

- Screen horizontal margin **20dp** (`RefScreenMargin`); rails bleed to the edge via `LazyRow` `contentPadding`.
- `LazyColumn`: top `SpaceLG`, inter-item `SpaceLG`, bottom `SpaceLG + measured bottom-bar height` (content scrolls under the bar, as today).
- Greeting → hairline `SpaceMD` · then search · then CTA.
- Section label → its content `SpaceSM`; a section starts `SpaceXL` after the previous one's content (asymmetric, on purpose — matches the salon reference).
- Salon card **200dp** wide · rail item gap `SpaceMD` · card radius **14dp** · CTA radius **12dp** / height **52dp**.
- Bottom bar: 5 × `weight(1f)`, each ≥ 48dp target, 24dp icon, `navigationBarsPadding`.

## 6. Layout (top → bottom)

1. **Greeting row** — `statusBarsPadding`; Persian greeting fills, 40dp outlined-Person avatar button at the end (→ `onProfileClick`). 1px bottom hairline.
2. **Search** — flat `RefSurface`, outlined `Search` + "جستجوی سالن، خدمت یا متخصص…"; whole surface pressable → `onSearchClick`.
3. **Primary CTA** — full-width solid rose-gold "رزرو نوبت" → `onBookAppointmentClick`.
4. **سالن‌های پیشنهادی** — `RefSectionLabel` + `LazyRow` of `HomeSalonCard` from `SalonListViewModel`. `Loading` → 2 skeleton cards. `Empty` / `Error` / unauthorised → the whole section (label included) is not rendered.
5. **نوبت‌های پیش‌رو** — only when `BookingHistoryViewModel` has `PENDING`/`CONFIRMED` items: label + a vertical stack of flat booking rows (salon · specialist? · date · time).
6. **بازدیدهای اخیر** — only when there are `COMPLETED` items: label + flat rows; tap → `onSalonClick(salonId)`.
7. **`HomeBottomBar`** — pinned bottom; Profile · Favorites · **Home (active)** · Bookings · Search.

## 7. States

| State | Rendering |
|---|---|
| Greeting, no name yet | "سلام کاربر جان" (same fallback as `HomeHeader`). |
| Salons loading | label + 2 pulsing `RefSurface` skeleton cards. |
| Salons empty / error / 401 guest | section omitted entirely (no dead label, no "retry" — Home is not the place to recover a salon list; the CTA and search remain). |
| No upcoming / no recent | those sections omitted entirely. |
| Any row / card / avatar / CTA / tab press | `rojanPressable` scale (unchanged mechanic). |
| Backgrounding / re-auth | unchanged — same ViewModels, same `currentDisplayName`. |

## 8. Screen-local components (all `private`, in `CustomerDashboardScreen.kt`)

`RefSurface` · `RefSectionLabel` · `RefPrimaryButton` · `HomeGreetingRow` · `HomeAvatarButton` · `HomeSearchBar` · `HomeSalonSection` · `HomeSalonCard` · `HomeSkeletonCard` · `HomeBookingSection` · `HomeBookingRow` · `HomeBottomBar` · `HomeTabItem`.

## 9. Icons

`Icons.Outlined.*` only, one weight: `Person`, `Search`, `Storefront`, `Place`, `Schedule`, `CalendarMonth`, `Home`, `FavoriteBorder`. Sizes 24 (bar) / 20 (inline) / 14 (meta).

## 10. Explicitly out of scope / deferred

- `CustomerHomeScreen` (Explore) and its shared sections (`HomeHeader`, `HeroBookingCard`, `FeaturedSalons`, `AISearchBar`, `PromotionsSection`, `NearbySalons`, …) — a later screen-by-screen pass.
- Promoting `Ref*` to the design system, retiring `HomeGlassSurface` / `PremiumButton` / `CustomerBottomBar` / the sparkle + metallic-border tokens app-wide — the app-wide phase, after Home is approved.
- A shared bottom-bar redesign — `CustomerBottomBar` is used by another screen; changing it there is not "Home only".
- Wiring the three placeholder sections to real backends (needs new endpoints) — backend work, flagged in `CUSTOMER-APP-RELEASE-AUDIT.md`.
