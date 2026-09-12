# Reference Screen — Salon Detail — Visual Specification v1

**Status:** proposed reference. Implemented in `screens/salon/SalonDetailsScreen.kt` this pass. **Not committed** — awaiting human approval.
**Scope:** this one screen only. No shared component, design-system token, colour, icon set, ViewModel, or navigation is modified. Everything below is achieved with **screen-local `private` primitives**.
**Direction:** quiet luxury · premium beauty-tech · mature · elegant · Persian RTL · feminine, not childish.

---

## 1. Removed vs. the current screen

| Removed | Replaced with |
|---|---|
| ✦ corner sparkles (every surface) | nothing — clean edges |
| Glowing gold metallic border (`HomeGlassSurface` → `PremiumMetallicBorder`) | flat surface + 1px hairline @ 9% white |
| Circular glass "orb" back button (`GlassBackButton`) | a real 56dp top app bar, 24dp back arrow on the content margin |
| 32dp card radius (`RojanShapes.GlassCard`) | **14dp** (screen-local `RefCardRadius`) |
| Gradient pill CTA (`PremiumButton`, 50dp pill) | solid rose-gold button, **12dp** radius, 52dp height |
| Tinted `HERO_SHAPE` colour band + `RojanGradients.ImageScrim` + overlapping-offset logo | calm centred **logo + name** header on the plain ground |
| Filled Material icons | **outlined** Material icons, one family |
| `HomeColors.Glow` `#7C4DFF` (violet) as the price/accent colour | **rose gold `#E0A67A`** |
| 6 declared font weights in play | 4 real weights only (Bold / SemiBold / Medium / Regular) |

## 2. Kept

- ROJAN identity · dark navy ground (`HomeBackgroundTheme`, untouched)
- **Rose gold `#E0A67A`** (`RojanPremiumBorderRoseGold`) as the *single* accent
- Glass **only as subtle depth**: a ~4.5% white translucent lift on content surfaces — no border, no glow, no metallic edge
- The screen's public function signature, its `SalonDetailsViewModel` / `SalonRelationshipViewModel`, and `RojanNavGraph` wiring — byte-identical
- Every backend field currently shown (salon, services, specialists, working hours, follow/favourite) — nothing fabricated, nothing dropped
- Content-direction `Text`, `rojanPressable` press feedback, the `isOpenNow` / Persian-day-label helpers, the Maps/dialer intents, the login-required redirect

## 3. Colour (screen-local constants — zero token edits)

| Role | Value | Origin |
|---|---|---|
| Ground | navy → deep-purple wash | `HomeBackgroundTheme` (unchanged) |
| Surface fill (cards/rows) | `White @ 4.5%` | `RefSurfaceFill` |
| Hairline | `White @ 9%` | `RefHairline` |
| Divider | `White @ 7%` | `RefDivider` |
| **Accent** (CTA fill, today-bar, active icon, price, links, open-now) | `#E0A67A` | `RojanPremiumBorderRoseGold` |
| Accent-on (label on the CTA) | `#1B1530` | `RefOnAccent` |
| Text primary | `#FFFBFF` | `HomeColors.TextPrimary` |
| Text secondary | `#CBBEE0` | `HomeColors.TextSecondary` |
| Text muted / meta / inactive icon | `#9C8FB5` | `HomeColors.TextMuted` |

No pink, magenta, violet-glow, or gold-metallic anywhere on this screen.

## 4. Typography (`RojanTypography` subset — one `.copy` size tweak only)

| Element | Style |
|---|---|
| Top-bar title | `Body` 17/Medium · 1 line · ellipsis |
| **Salon name** | `Display.copy(fontSize = 26.sp, lineHeight = 34.sp)` · Bold |
| Description | `Body` 17/Medium · secondary · ≤ 3 lines |
| Section label | `Caption.copy(fontWeight = SemiBold)` · muted |
| Meta row | `Caption` 15/Regular · muted |
| Service name / day | `Body` 17/Medium · primary |
| Duration / hours value | `Caption` 15/Regular · muted |
| Price | `Caption.copy(fontWeight = SemiBold)` · accent |
| Specialist name | `Caption` 15/Regular · secondary · ≤ 2 lines |
| CTA label | `Button` 16/SemiBold |

## 5. Spacing (`RojanDimens` + a screen-local 20dp margin)

- Screen horizontal margin: **20dp** (`RefScreenMargin`)
- Top bar: 56dp · logo 72dp · 20dp below the bar
- logo → name `SpaceMD` 16 · name → description `SpaceXS` 4 · description → meta `SpaceSM` 8
- identity → CTA `SpaceLG` 24 · CTA → first section `SpaceXL` 32
- **section label → its content: `SpaceSM` 8** (label belongs to what's below)
- **section content → next label: `SpaceXL` 32** (asymmetric, on purpose)
- list rows: flush, separated by a 1px divider; row vertical padding `SpaceMD` 16
- specialist rail: item gap `SpaceMD` 16 · avatar 64dp
- card radius `RefCardRadius` **14dp** · button radius **12dp** · button height 52dp
- bottom padding `SpaceXXL` 48

## 6. Layout (top → bottom)

1. **Top app bar** — flat, transparent, 1px bottom hairline. Trailing (RTL-right): back `Icons.AutoMirrored.Outlined.ArrowBack`. Centre: salon name (`Body`, ellipsis). Leading (RTL-left): favourite + follow icons (24dp, 48dp targets; accent when active, muted when not; spinner while toggling). `followError` → 1-line caption under the bar.
2. **Header** — centred. 72dp circular logo (`RojanRemoteImage` / initials-on-`RefSurface` fallback). → name (`Display.copy 26`). → description (≤ 3 lines). → meta row: `Place` + address (tap → Maps) · `·` · `Schedule` + "باز تا HH:MM" / "اکنون تعطیل" (computed).
3. **Primary CTA** — *only when `onContinueBooking != null`* (unchanged contract): full-width solid rose-gold, "ادامه رزرو". When null: no CTA; a one-line hint under the "خدمات" label ("برای رزرو، خدمت مورد نظر را انتخاب کنید") and the service rows are the path.
4. **خدمات** — label → one `RefSurface` card of service rows (dividers). Row: name / duration · price (accent) · chevron `KeyboardArrowLeft`. Tap → `onServiceClick`.
5. **متخصصان** — label → horizontal `LazyRow`, **no card**. 64dp circular avatar + name (≤ 2 lines). Tap → `onSpecialistClick`.
6. **ساعات کاری** — label → one `RefSurface` card. Optional open-now dot + label. 7 day rows (day right, hours left). Today: 3dp accent bar + primary-white day label.
7. **تماس** — label → one `RefSurface` row: `Phone` + number (renders LTR). Tap → dialer.

## 7. States

| State | Rendering |
|---|---|
| **Loading** | `RefLoadingSkeleton` — header block + 3 section stubs, `RefSurface`, slow alpha pulse (0.4↔0.8, 1100ms). Top bar with back present. **No spinner, no photo.** |
| **Not found** | `RefCenteredState` — `SearchOff` 40dp muted · "سالن یافت نشد" (`CardTitle`) · body · `RefPrimaryButton` "بازگشت". Vertically centred. |
| **Error** | `RefCenteredState` — `CloudOff` 40dp · `state.message` · `RefPrimaryButton` "تلاش مجدد" → `retry()`. |
| **Empty section** | not rendered (unchanged). |
| **Favourite / follow toggling** | icon outline↔filled + accent tint; `rojanPressable` tick; 16dp `CircularProgressIndicator` (muted) while in-flight; error → caption under the bar. Snackbar **deferred** (separate shared-component task). |
| **Login required** | `LaunchedEffect` → `onLoginRequired()` + consume (unchanged). |
| **Row / avatar / CTA press** | `rojanPressable` scale (unchanged mechanic). |

## 8. Screen-local components (all `private`, in the screen file)

`RefTopBar` · `RefHeader` · `RefMetaRow` · `RefPrimaryButton` · `RefSectionLabel` · `RefSurface` · `RefServiceRow` · `RefSpecialistItem` · `RefHoursCard` · `RefContactRow` · `RefCenteredState` · `RefLoadingSkeleton`.

## 9. Icons

`Icons.Outlined.*` + `Icons.AutoMirrored.Outlined.*`, one 1.5px weight: `ArrowBack`, `KeyboardArrowLeft` (chevron), `Place`, `Schedule`, `Phone`, `FavoriteBorder`/`Favorite`, `NotificationsNone`/`NotificationsActive`, `Storefront`, `SearchOff`, `CloudOff`. Sizes 24 / 20 / 15.

## 10. Explicitly out of scope / deferred (flagged for the build agent)

- A shared **Snackbar** component (would be used here for favourite/follow confirmation, and by the empty-state review item). Functional-adjacent — not added.
- Promoting the header to a **16:9 cover photo** hero — the backend has only a `logoUrl`, no cover/gallery. Revisit when real salon photography exists.
- Global token changes (`RojanRadius` 32→14, a neutral grey ramp, a 3-tier `PremiumButton`) — these belong to a **later app-wide phase**, only after this reference screen is approved.
