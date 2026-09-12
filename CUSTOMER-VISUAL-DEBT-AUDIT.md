# ROJAN Customer App — Visual Debt Audit

**Date:** 2026-09-09 · **Type:** READ ONLY. No source modified, no code created, nothing redesigned. Awaiting approval.
**Method:** source inspection of every Customer route in `RojanNavGraph.kt` + the shared UI components they consume. Manager (`manager/`) and Reception (`reception/`) screens are a different app target and are **out of scope**.
**Reference language (target):** dark navy ground · rose-gold `#E0A67A` accent · flat `RefSurface` (4.5% white fill + 1px 9% hairline) · 14dp card / 12dp button radius · outlined icons · one calm screen fade max · no sparkle / glow / gradient / metallic border / orb / hero band. Established by: Salon Detail (golden reference), Customer Home, Customer Explore, the shared `CustomerBottomBar`, and `screens/bookingflow/components/*` (`BookingScaffold`, `RefSurface`, `RefListRow`, `RefSelectableCell`, `RefPrimaryButton`, `BookingLoadingRows`, `BookingCenteredState`).

**Severity:** **P0** = breaks premium perception · **P1** = inconsistent with the new design · **P2** = minor cleanup.

---

## 0. Scoreboard

| Area | Screens | Status |
|---|---|---|
| **Done & approved** | Salon Detail · Home · Explore · Bottom Nav · Booking Success / Confirmation / Date / Time · Service Details · Specialist Selection | ✅ 10 |
| **Remaining — full screens** | Splash · Session-Restore · Auth · Salon List · Search · Specialist Profile · Public Salon · Profile · Appointments · Appointment Details · Reschedule · Waitlist · Favorites · Followed Salons · Wallet · Coupons · Membership · Loyalty · My Reviews · Beauty Timeline · Beauty DNA | ⛔ 21 |
| **Shared components carrying debt** | `GlassBackButton` · `PremiumButton` · `HomeGlassSurface` / `PremiumGlassSurface` / `premiumMetallicBorder` · `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` / `RojanComingSoonState` · `RojanScaffold` · `PremiumBackground` · `HomeTextField` · `AISearchBar` · `HeroBookingCard` | ⛔ 10 |
| **Orphaned dead code** (post Home/Explore rewrites) | `HomeHeader` · `AISearchBar` · `SearchModeTabs` · `PopularServices` · `PromotionsSection` · `NearbySalons` · `HeroBookingCard` · `FeaturedSalons` · `TopSpecialists` · `RecommendedSalons` · `FollowedSalons` · `UpcomingBookings` · `RecentVisits` · `CustomerScreenScaffold` | 🧹 14 |

The 21 remaining screens are almost entirely **compositions of the same ~10 shared components**. Fixing the shared components (Section A) removes most of the debt in Section B mechanically.

---

## A. Systemic component debt

### A-1 · `premiumMetallicBorder` / `PremiumGlassSurface` / `HomeGlassSurface` — the sparkle + glow + metallic engine

- **Files:** `ui/components/glass/PremiumMetallicBorder.kt`, `ui/components/glass/PremiumGlassSurface.kt`, `ui/components/glass/GlassSurface.kt`, `screens/customer/hometheme/HomeGlassSurface.kt`
- **Screens affected:** every remaining screen (Auth, Salon List, Search, Specialist Profile, Public Salon, Profile, Appointments, Appointment Details, Reschedule, Favorites, Followed Salons, + all 7 "coming soon" screens via `RojanComingSoonState`).
- **Current visual problem:** `premiumMetallicBorder` draws an alternating **gold / rose-gold / specular** metallic stroke, a **7-pass glow bloom**, and **literal 4-point star sparkles** (`drawSparkle`) in the top-start corner and partway along the top edge (`PremiumMetallicBorder.kt:161-186`). `PremiumGlassSurface` adds a **gold-tinted ambient shadow** (`RojanPremiumBorderGold.copy(alpha=0.36f)`). `HomeGlassSurface` is the Customer wrapper. This is the single largest driver of the "childish / trophy / mobile-game" read the design review flagged — it is on essentially every container in the un-redesigned app.
- **Severity:** **P0**
- **Recommendation:** these screens should stop calling `HomeGlassSurface` and adopt the booking-flow `RefSurface` (flat fill + 1px hairline, 14dp, no border-glow, no sparkle). Do **not** edit the glass components in place — they're the Manager app's reference implementation too; the Customer redesign has consistently replaced *usage* with screen-local `Ref*` primitives. Once no Customer screen references them, they can be deleted in a later cleanup.

### A-2 · `GlassBackButton` — the orb

- **File:** `ui/components/navigation/GlassBackButton.kt` · **~19 call sites** across the remaining screens.
- **Problem:** a 48dp **circular `GlassSurface` orb** (glass + metallic border + sparkle), floated top-left, off the content grid, breaking the left margin. The approved language replaced it everywhere with the flat 56dp top bar (`BookingScaffold` / Salon Detail's `RefTopBar`): outlined `Icons.AutoMirrored.Outlined.ArrowBack` on the content margin + centred `Body` title + 1px bottom hairline.
- **Severity:** **P0**
- **Recommendation:** every remaining screen adopts a flat top bar. `BookingScaffold` already exists for the booking-adjacent ones (Specialist Profile, Reschedule); the Profile-graph and standalone screens need an equivalent — either extend `BookingScaffold` out of the `bookingflow` package into a neutral `CustomerScaffold`, or give each screen its own inline `RefTopBar` (Salon Detail's pattern). **Decision needed** (see D-1).

### A-3 · `PremiumButton` — the gradient pill

- **File:** `ui/components/buttons/PremiumButton.kt` (default `style = RojanButtonStyle.Gradient` → `RojanGradients.PremiumButton` magenta→pink linear gradient, `RojanShapes.PremiumButton` = `Pill` 50dp).
- **Customer call sites:** `AuthScreen:236`, `PublicSalonScreen:194`, `RescheduleAppointmentScreen:182`, `AppointmentDetailsScreen:141`, and **`RojanEmptyState`/`RojanStateCard:169`** (so every empty-state CTA app-wide).
- **Problem:** magenta→pink gradient, 50dp pill. The approved CTA is the flat solid rose-gold `RefPrimaryButton` (12dp radius, 52dp, `#E0A67A` fill, `#1B1530` label, 40% alpha when disabled).
- **Severity:** **P0**
- **Recommendation:** replace call sites with `RefPrimaryButton`. `PremiumButton` also exposes `Outline` and `Glass` styles that are unused in Customer — no need to keep any of it once call sites migrate.

### A-4 · `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` / `RojanComingSoonState`

- **File:** `ui/components/state/*` — all route through `RojanStateCard` → `GlassSurface` (glass card, 16dp) + `RojanIconContainer` XLarge with **filled** icons (`Icons.Filled.Inbox`, `Icons.Filled.Schedule`) + `PremiumButton` (gradient) for the action.
- **Customer call sites:** Search, Salon List, Appointments, Appointment Details, Reschedule, Favorites, Followed Salons, Specialist Profile, Public Salon, FeaturedSalons(orphan), + the 7 "coming soon" screens.
- **Problem:** a glass "card" with a filled XLarge icon, low-contrast body text on translucent glass, and a gradient pill. The design review's VIS-08 / §4.4 called this out — the moment the user needs guidance most, the app is least legible.
- **Severity:** **P1** (P0 for the empty states with no action, e.g. Appointments)
- **Recommendation:** the booking flow already solved this — `BookingCenteredState` (calm centred stack: outlined 40dp icon in `TextMuted`, `CardTitle` title near-white, `Caption` body, optional `RefPrimaryButton`; **no card**) and `BookingLoadingRows` (flat pulsing skeletons). Promote both out of `bookingflow.components` into a shared Customer states module, or re-point the remaining screens at them.

### A-5 · `PremiumBackground` — the AI candy-pink salon photo

- **File:** `ui/background/PremiumBackground.kt` (`R.drawable.bg_master_luxury_salon` + `RojanAIGlow` radial). `WarmBackground` is a thin alias of it.
- **Customer surfaces still using it:**
  - **`RojanNavGraph.RestoringSessionContent()` (`:1146`)** — the session-restore screen ("در حال بازیابی نشست شما"). This is the **first sustained brand image** for a returning user (design review VIS-01 / §4.1). Full-bleed over-saturated AI render + a `PremiumLoadingBar`.
  - `screens/dashboard/DashboardPlaceholder.kt` via `RojanScaffold` — Manager/Stylist only, effectively out of scope, but shares the asset.
  - `RojanEmptyState.kt:45` — preview only.
- **Severity:** **P0** (the session-restore screen)
- **Recommendation:** `RestoringSessionContent()` should render on `HomeBackgroundTheme` (dark navy) with the ROJAN monogram + one quiet indeterminate line — the same treatment already used by `SplashScreen`. No photography in system chrome.

### A-6 · `PremiumLoadingBar`

- **File:** `components/PremiumLoadingBar.kt` — a sweeping **warm-amber glow gradient** bar (`RojanTokens.kt:320-323`).
- **Used by:** `SplashScreen:246`, `RestoringSessionContent`.
- **Problem:** a glowing gradient sweep. Minor, but it's a "glow effect" and off-palette (amber, not rose-gold).
- **Severity:** **P2**
- **Recommendation:** a thin flat indeterminate track in `#E0A67A` at low alpha, or a plain `LinearProgressIndicator` tinted rose-gold.

### A-7 · `HomeTextField`

- **File:** `screens/customer/hometheme/HomeTextField.kt` (9 references to `HomeColors.Glow`/violet).
- **Used by:** `AuthScreen`, `SalonListScreen`.
- **Problem:** a glass text field with a violet (`HomeColors.Glow`) cursor / focus / label accent, on a `HomeGlassSurface`-style fill.
- **Severity:** **P1**
- **Recommendation:** a flat field — `RefSurface` fill + hairline, rose-gold cursor/focus, muted placeholder. Match the flat search entry already shipped on Home/Explore (`HomeSearchBar` / `ExploreSearchBar`), which is a tap-target, not a live field — the two live fields (Auth, Salon List) need the real editable version.

### A-8 · `AISearchBar` / `HeroBookingCard` — now orphaned but still carry debt

- `screens/customer/AISearchBar.kt` — glass button + a 16dp-blur `HomeColors.Glow` glow wash behind it.
- `ui/components/hero/HeroBookingCard.kt` — 360dp card, 2 gradient tint layers + a blurred glow twin, an **AI salon photo** (`R.drawable.salon_demo_1`) with a white border, gradient `PremiumButton`.
- **Severity:** **P2** (no live call sites after the Home + Explore rewrites — see Section C).

---

## B. Per-screen findings

> Every screen below wraps `HomeBackgroundTheme` (dark navy — **keep**) and, unless noted, repeats the same stack: `GlassBackButton` orb (A-2) → bare `HeroTitle` → `HomeGlassSurface` content (A-1) → glass state components (A-4) → `rojanEnterAnimation(index * 60)` staggered list entrance. Only the deltas are listed.

### B-1 · Session Restore — `RojanNavGraph.kt:1146` `RestoringSessionContent()`
- **Problem:** full-bleed AI candy-pink salon photo (`PremiumBackground`, A-5) + `PremiumLoadingBar` (A-6). No branding on it. Shows for ~1–2s on every authenticated cold start.
- **Severity:** **P0**
- **Recommendation:** dark navy `HomeBackgroundTheme`, centred ROJAN monogram (`ic_launcher_foreground`, ≤ 96dp, like `SplashScreen`), one 2px rose-gold indeterminate line, caption at ~40% opacity. No photo.

### B-2 · `screens/splash/SplashScreen.kt`
- **Problem:** already on dark navy with the logo + "ROJAN AI" + subtitle — good. **But** 4 decorative **`FrostedGlassOrb`** blobs float around it (`SplashScreen.kt:114-174`, tints `HomeColors.Lavender` / `White` / `Glow` / `Rose`), and a `PremiumLoadingBar` (A-6). `SplashText` uses raw `fontSize`/`FontWeight` instead of `RojanTypography`.
- **Severity:** **P1**
- **Recommendation:** delete the 4 `FrostedGlassOrb`s (pure decoration), swap the loading bar per A-6, route text through `RojanTypography` (`ScreenTitle` / `Caption`). Keep the calm logo fade-in.

### B-3 · `screens/auth/AuthScreen.kt` (route `AUTH`) — login / OTP
- **Problems:**
  - `GlassBackButton` orb.
  - `Text("سلام 🌸", HeroTitle)` — **emoji as brand mark** (design review VIS-11 / §2 Persian typography).
  - `HomeGlassSurface(shape = RojanShapes.GlassCard)` — **32dp** glass card wrapping the fields.
  - `HomeTextField` (A-7); "ارسال مجدد کد" `TextButton` in `HomeColors.Glow` (violet).
  - **A large `blur(36.dp)` radial `HomeColors.Glow` (violet) glow halo** behind the CTA (`AuthScreen.kt:208-234`) + a separate `shadow(14.dp)` + `PremiumButton` gradient pill.
  - Error text in `RojanErrorText`.
- **Severity:** **P0** (first-run / re-auth screen; the violet glow halo + gradient pill + emoji are the strongest off-brand signals in the app after the session-restore photo).
- **Recommendation:** flat top bar; drop the 🌸 (use "ورود به روژان" or the monogram); flat `RefSurface` field group at 14dp; `RefPrimaryButton` with **no** glow/shadow behind it; error as `Caption` in `TextSecondary`; "ارسال مجدد کد" as a plain text button in rose-gold.

### B-4 · `screens/booking/SalonListScreen.kt` (routes `MEMBER_SALONS_LIST`, `SALON_LIST`) — "browse to book"
- **Problems:** `GlassBackButton` orb · `HeroTitle` "انتخاب سالن" · `HomeTextField` (A-7) · `SalonFilterChip` = `HomeGlassSurface` glass pills ("همه" / "نزدیک من") · `SalonCard` = `HomeGlassSurface` + `rojanEnterAnimation(index*60)` · `RojanErrorState`/`RojanEmptyState` glass cards, `Icons.Filled.Storefront` empty icon · `CircularProgressIndicator` in `HomeColors.Glow` (violet) · "ورود کسب‌وکار" glass chip · `SalonListSkeleton` (glass).
- **Severity:** **P1** (this screen is the "رزرو نوبت" CTA destination — high traffic in the booking funnel).
- **Recommendation:** it already has a redesigned twin — the `HomeSalonCard` / `HomeSalonSection` shipped on Home & Explore (flat card, storefront tile, name `CardTitle`, address row, chevron). Adopt `BookingScaffold` ("انتخاب سالن", no step indicator or step 0) + the flat salon card + the flat search field + `RefSelectableCell` for the filter chips + `BookingCenteredState` / `BookingLoadingRows`. Rose-gold spinner.

### B-5 · `screens/search/SearchScreen.kt` (route `SEARCH`) — live salon search
- **Problems:** `GlassBackButton` orb · `HeroTitle` "جستجو" · search field = `HomeGlassSurface` + `BasicTextField` with a **violet `Icons.Filled.Search`** and violet cursor (`HomeColors.Glow`) · `SearchResultRow` = `HomeGlassSurface` + `rojanEnterAnimation(index*60)`, a `colorSeedFor(id)` **colour-tinted** 56dp logo box, filled `Storefront` / `NotificationsActive` / `Favorite` / `KeyboardArrowLeft` icons in `HomeColors.Glow` · `SearchResultsSkeleton` = 6 glass rows · `RojanErrorState` / `RojanEmptyState` (the 401 "برای جستجوی سالن‌ها وارد شوید" was the design review's *best* state — keep the copy/behaviour, restyle the shell) · `RojanShapes.Small` chips.
- **Severity:** **P1**
- **Recommendation:** `BookingScaffold`-style flat bar; live field on a flat `RefSurface`; result rows = the flat salon-card pattern from Explore; `BookingCenteredState` for the login-required / empty / error states.

### B-6 · `screens/specialist/SpecialistProfileScreen.kt` (route `SPECIALIST_PROFILE`) — booking-journey detour
- **Problems:** `GlassBackButton` orb (×2, in the state scaffold + the content) · 96dp `accentFor(id).copy(alpha=0.5f)` **colour-tinted avatar circle** · name in `HeroTitle` directly under it, no breathing room · bio wrapped in its own `HomeGlassSurface` card · "خدمات قابل رزرو" = `RtlSectionHeader` (Body/white) · each service row a `HomeGlassSurface` card + `rojanEnterAnimation(index*60)` · `RojanLoadingState` / `RojanErrorState` glass cards.
- **Severity:** **P1** (it's part of the booking journey the rest of which is done — inconsistency is visible mid-flow).
- **Recommendation:** it's the twin of the redesigned Salon Detail specialist section + Specialist Selection. Use `BookingScaffold` (title = specialist name, `step = null`), a centred initials avatar (no tint, like Salon Detail's `RefSpecialistItem` / the new Specialist Selection avatar), `Display.copy(26sp)` name, bio as plain `Body` (no card), a `BookingSectionLabelStyle` "خدمات قابل رزرو" + a divided `RefSurface` list of `RefListRow`s, `BookingCenteredState` states.

### B-7 · `screens/salon/PublicSalonScreen.kt` (route `PUBLIC_SALON`) — unauthenticated salon page
- **Problems:** `GlassBackButton` orb · `RojanLoadingState` / `RojanErrorState` glass cards · 3× `HomeGlassSurface` (info card with a tuned `glassAlpha`, service rows, specialist rows) each with `rojanEnterAnimation(index*60)` · `PremiumButton` gradient pill (the "ورود" CTA) · 3× `Icons.Filled.*`.
- **Severity:** **P1** (low traffic — deep-link only, no link registered yet — but it's a public-facing brand surface).
- **Recommendation:** mirror the redesigned Salon Detail exactly (`RefTopBar`, `RefHeader`, `RefSurface` service/hours rows, `RefSpecialistItem`), swap the login CTA for `RefPrimaryButton`.

### B-8 · `screens/profile/ProfileScreen.kt` (route `PROFILE`) — **worst offender after session-restore**
- **Problems:**
  - `GlassBackButton` orb.
  - **88dp avatar circle** with a `HomeColors.Glow.copy(alpha=0.25f)` **violet** fill + a 44dp **filled** `Icons.Filled.Person`. Name in `HeroTitle`.
  - Every section header = `RtlSectionHeader` (`Body`, full-weight white) — not the quiet caps label.
  - **Every menu row (~14 of them) = a `HomeGlassSurface(RojanShapes.Small)` glass card** + `rojanEnterAnimation(index*60)` stagger + `RtlListRow` with a **filled** `Icons.Filled.*` icon tinted **`HomeColors.Glow` (violet)**. (`Icons.Filled` count on this file: **16**.) This is the design review's VIS-05 — a flat navigation list rendered as a stack of glowing trophy cards, ~4 visible per screen.
  - The "تایید شده" phone subtitle is in violet.
- **Severity:** **P0**
- **Recommendation:** a quiet grouped list. Flat top bar; a plain 64dp initials avatar (no violet fill, no filled glyph) + name in `Display.copy(26sp)`; section headers as `BookingSectionLabelStyle` caps-muted; menu = one `RefSurface` per group with divided `RefListRow`s (leading 20dp **outlined** icon in `TextMuted`, title `Body`, optional trailing value, chevron, 1px divider); no per-row card, no stagger, no glow. Rose-gold only on an active/verified indicator. The whole menu should fit in ~1.3 screens instead of ~4.

### B-9 · `screens/profile/AppointmentsScreen.kt` (route `APPOINTMENTS`)
- **Problems:** `GlassBackButton` orb · `HeroTitle` "نوبت‌های من" · `RojanLoadingState` / `RojanErrorState` glass cards · **empty state = `RojanEmptyState("هنوز نوبتی ندارید", "…به صفحه اصلی بازگردید")` with NO `actionLabel`/`onAction`** — no CTA (design review VIS-19 / §4.4) · "پیش‌رو" / "گذشته" section headers = plain `Body`/white · `AppointmentCard` = `HomeGlassSurface` + `rojanEnterAnimation(index*60)` · **cancel-appointment `AlertDialog` = default Material3** (light surface, default type, not dark/RTL-tuned) — see B-10 · "لغو نوبت" in `RojanErrorText`.
- **Severity:** **P0** (the empty state with no action + a raw M3 dialog on a destructive action).
- **Recommendation:** `BookingScaffold` ("نوبت‌های من", no step); flat `RefSurface` appointment rows in two labelled groups (پیش‌رو / گذشته); `BookingCenteredState` empty state **with a "رزرو نوبت" primary CTA**; a ROJAN-themed confirm dialog (dark navy `RefSurface`, `RojanTypography`, rose-gold confirm) — see B-10.

### B-10 · Dialogs — `AppointmentsScreen.kt:246` (the only dialog in the Customer app)
- **Problem:** `androidx.compose.material3.AlertDialog` with zero ROJAN theming — light M3 container colour, M3 default typography, LTR button order, no dark-navy surface, no rose-gold. Jarring against the rest of the app.
- **Severity:** **P1**
- **Recommendation:** a small shared `BookingDialog` / `RojanConfirmDialog` primitive: `RefSurface` on the scrim, `Display.copy` title, `Body` message, `RefPrimaryButton` (destructive → still rose-gold, or a muted variant) + a text "انصراف". RTL button order. This is the one net-new component the remaining work needs.

### B-11 · `screens/profile/AppointmentDetailsScreen.kt` (route `APPOINTMENT_DETAILS`)
- **Problems:** `GlassBackButton` orb · `RojanLoadingState` / `RojanErrorState` glass cards · `HomeGlassSurface(shape = RojanShapes.GlassCard)` — **32dp** card (the header) + `HomeGlassSurface(RojanShapes.Small)` (details) · `PremiumButton` gradient pill · 1× `Icons.Filled`.
- **Severity:** **P1**
- **Recommendation:** `BookingScaffold` + a flat divided `RefSurface` summary (mirror the redesigned Booking Confirmation exactly — same rows, same rose-gold price treatment), `RefPrimaryButton` for any action.

### B-12 · `screens/profile/RescheduleAppointmentScreen.kt` (route `RESCHEDULE_APPOINTMENT`)
- **Problems:** `GlassBackButton` orb · `RojanLoadingState` / `RojanErrorState` / `RojanEmptyState` glass cards · 2× `HomeGlassSurface` (the date-row list + a time-grid wrapper) · `PremiumButton` gradient pill · 4× `HomeColors.Glow` (violet) — it's a date + time re-picker.
- **Severity:** **P1**
- **Recommendation:** it is functionally Booking Date + Booking Time on one screen — reuse the exact primitives already shipped there (`RefSelectableCell` day cells with weekday/date hierarchy + rose-gold selected; flat time-chip grid; `BookingCenteredState` states) inside a `BookingScaffold`.

### B-13 · `screens/profile/FavoritesScreen.kt` & `FollowedSalonsScreen.kt` (routes `FAVORITES`, `FOLLOWED_SALONS`)
- **Problems (both):** `GlassBackButton` orb (as a `LazyColumn` item) · `RojanLoadingState` (as an item) · `RojanEmptyState` / `RojanErrorState` glass cards · salon rows = `HomeGlassSurface` · violet accents · `Icons.Filled` (2 each). `FollowedSalonsScreen` uses a `NotificationsActive`-style filled bell.
- **Severity:** **P1**
- **Recommendation:** `BookingScaffold` + the flat Explore salon-card pattern + `BookingCenteredState` empty states (with a "کشف سالن‌ها" CTA → Explore). Same treatment for both — they're near-identical list screens.

### B-14 · The 7 "Coming Soon" screens — `WalletScreen` · `CouponsScreen` · `MembershipScreen` · `LoyaltyScreen` · `MyReviewsScreen` · `BeautyTimelineScreen` · `WaitlistScreen`
- **Shared shape:** `Column(padding SpaceMD) { GlassBackButton ; Text(HeroTitle) ; RojanComingSoonState() }`.
- **Problems:** `GlassBackButton` orb · `HeroTitle` · `RojanComingSoonState` = glass card + **filled** `Icons.Filled.Schedule` XLarge + "به‌زودی" + faint body text on translucent glass.
- **Severity:** **P2** (each is a single gated placeholder — low visual weight, low traffic, and they may be removed from the shipping build entirely per the design review).
- **Recommendation:** one shared `BookingScaffold` + `BookingCenteredState` (outlined `Schedule` / `HourglassEmpty`, "به‌زودی", one calm line). Or — preferred — **hide these entries from the Profile menu until the backend capability exists** (decision for the product owner; a nav-layer change, flag only).

### B-15 · `screens/profile/BeautyDnaScreen.kt` (route `BEAUTY_DNA`)
- **Problems:** `GlassBackButton` orb · `HomeGlassSurface` (`BeautyDnaScreen.kt:200`) · 1× `Icons.Filled`. Content is a static "quiz/results" layout.
- **Severity:** **P2** (also likely a placeholder — verify whether it has real backend data before investing).
- **Recommendation:** if it stays, `BookingScaffold` + flat `RefSurface` sections; if it's a placeholder, treat as B-14.

---

## C. Orphaned / dead code (P2 — cleanup, not redesign)

After the Customer Home and Explore rewrites, these `screens/customer/*` section composables have **no remaining call sites** in `main` (verified — each appears only as its own `fun` definition):

| File | Note |
|---|---|
| `HomeHeader.kt` | glass app-bar chip + blurred `HomeColors.Glow` wash + `RojanAmbientGlow`. Still referenced by `androidTest/HomeHeaderSemanticsTest.kt` — delete the test with it. |
| `AISearchBar.kt` | glass button + glow wash |
| `SearchModeTabs.kt` | gradient-ish glass pills (سالن‌ها / خدمات) — fed nothing even when live |
| `PopularServices.kt`, `PromotionsSection.kt`, `NearbySalons.kt` | `RojanComingSoonState` placeholders |
| `HeroBookingCard.kt` (`ui/components/hero/`) | 360dp card, gradient tints, blurred glow twin, **AI salon photo** `R.drawable.salon_demo_1`, gradient `PremiumButton` |
| `FeaturedSalons.kt`, `TopSpecialists.kt`, `RecommendedSalons.kt`, `FollowedSalons.kt` | tinted-glass `HomeCard` rails / coming-soon |
| `UpcomingBookings.kt`, `RecentVisits.kt` | tinted-glass booking rails (the real data now renders via the redesigned Home) |
| `CustomerScreenScaffold.kt` (`screens/customer/`) | defined, **zero call sites** — an un-adopted consolidation wrapper; still uses `GlassBackButton` |
| `screens/customer/hometheme/HomeCard.kt`, `HomeTextField.kt` | `HomeCard` orphaned; `HomeTextField` still used by Auth + Salon List (see A-7) |

**Recommendation:** delete in one pass **after** Auth and Salon List stop using `HomeTextField` — until then leave them so the tree compiles. `DashboardPlaceholder.kt` / `RojanScaffold.kt` / `PremiumBackground.kt` stay (Manager/Stylist use them) but should be verified as truly Customer-free.

---

## D. Recommended approach & open decisions

### D-1 · One decision blocks the rest: the shared scaffold

`BookingScaffold` (flat top bar + optional step indicator + optional bottom CTA) is exactly what all 21 remaining screens need, minus the step indicator. **Options:**
- **(a) Promote it** — move `BookingScaffold` + the `Ref*` primitives + `BookingCenteredState` / `BookingLoadingRows` out of `screens/bookingflow/components/` into a neutral `screens/customer/components/` (or `ui/components/customer/`), rename `Booking*` → `Customer*`, keep the booking screens pointing at the new location. One shared foundation for the whole Customer app. **Recommended.**
- **(b) Add a second scaffold** — leave `BookingScaffold` where it is, add `CustomerScaffold` (top bar, no step) alongside. Some duplication of the 56dp-bar code.
- **(c) Inline per screen** — each screen gets its own `RefTopBar` (Salon Detail's pattern). ~21× duplication.

### D-2 · The one net-new component

`RojanConfirmDialog` / `BookingDialog` (B-10) — the app has exactly one dialog and it's un-themed. Small, self-contained.

### D-3 · Suggested redesign sequence (after approval)

| # | Batch | Screens | Why first |
|---|---|---|---|
| 1 | **Foundation** | promote the scaffold + `Ref*` + states per D-1; add `RojanConfirmDialog` | unblocks everything; zero visual change until screens adopt it |
| 2 | **P0 — first impressions** | Session Restore (B-1) · Auth (B-3) · Splash cleanup (B-2) | the first + re-entry brand frames |
| 3 | **P0 — Profile** | Profile (B-8) | the "worst offender" menu; highest-traffic settings surface |
| 4 | **P1 — booking-journey consistency** | Specialist Profile (B-6) · Salon List (B-4) · Reschedule (B-12) · Appointment Details (B-11) | finishes the funnel the rest of which is already done |
| 5 | **P1 — Appointments + list screens** | Appointments + its dialog (B-9/B-10) · Favorites + Followed (B-13) · Search (B-5) | |
| 6 | **P1 — public** | Public Salon (B-7) | low traffic, mirrors Salon Detail |
| 7 | **P2 — placeholders + cleanup** | the 7 coming-soon screens (B-14) · Beauty DNA (B-15) · delete Section C orphans + A-3/A-4 shared components once call-site-free |

### D-4 · Colour-token cleanup (P2, do last)

`HomeColors.Glow` (`RojanAIGlow` violet `#7C4DFF`), `.Primary` (`RojanVividPurple`), `.Magenta` (`RojanVividMagenta`), `.Rose`, `.Lavender`, `.Gold` are still referenced by ~21 files. Once every Customer screen is on the `Ref*` palette (navy ground + rose-gold + `HomeColors.TextPrimary/Secondary/Muted` only), these can be removed from `HomeColors` and the underlying `RojanTokens.kt` accents demoted. Not before — Manager/Reception may still use them.

---

**No source code was modified. No redesign performed. Awaiting approval of the sequence in D-3 (and a decision on D-1) before any implementation.**
