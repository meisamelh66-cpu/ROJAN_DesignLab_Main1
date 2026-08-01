# ROJAN AI DesignLab — Claude Instructions

Android app (Kotlin, Jetpack Compose). Clean Architecture: `domain/` (no Android
imports), `data/`, `navigation/`, `screens/`, `ui/` (design system: tokens,
glass components, backgrounds, buttons, interaction).

## ROJAN Development Rules

- Preserve Premium Glassmorphism Design System.
- Preserve RTL Persian-first experience.
- Follow ROJAN Quality Gate (RQG).
- Do not modify unrelated files.
- Do not remove existing features without approval.
- Before major architectural changes, ask for confirmation.

## Automatic Actions

Proceed without asking:
- Build project
- Run tests
- Inspect code
- Run emulator checks
- Capture screenshots
- Fix UI issues described in the task

## Confirmation Required

Ask only for:
- Delete files
- Database migrations
- Architecture changes
- Breaking API changes
- Large refactors

When a fix is found mid-task that's outside the current request's scope
(e.g. a regression in an unrelated file), surface it and propose a minimal,
scoped fix rather than folding it into the current change silently.

## ROJAN Quality Gate (RQG)

Before considering any UI task complete:
1. `assembleDebug` succeeds.
2. No new hardcoded colors/raw values outside token-definition files —
   route colors through `RojanTokens.kt`, surfaces through `GlassSurface`,
   backgrounds through `PremiumBackground`.
3. Design tokens/glass system used consistently with the rest of the
   screen/module.
4. RTL layout intact.
5. Build, install on emulator (or a connected device), and provide a
   screenshot of the actual result before calling the task done — if no
   device/emulator is reachable, say so explicitly rather than claiming
   visual verification that didn't happen.

When investigating uncommitted or ambiguous changes, diff against
`git show HEAD:<path>` to establish what the last-known-good baseline
actually was before proposing a fix.

## Design Baseline v1.0 (Frozen — Customer Home)

Customer Home's current visual language is the frozen reference
implementation for every future Customer screen (Salon Details,
Specialist Profile, Booking Flow, Customer Dashboard). Extend it
consistently — reuse the same primitives/tokens at their current values —
rather than introducing new visual systems. Do **not** change any of the
following without explicit approval first:

- **Background atmosphere** — `WarmBackground.kt`: solid `RojanWarmWhite`
  base, `RojanBackgroundGradient` wash at 0.14f alpha, single quiet
  `RojanAIGlow` radial zone (top-start, 0.05f alpha). Deliberately calm —
  do not add competing color zones.
- **Glass system** — superseded by the **Shared Premium Glass Design
  System** (see that section below) as of the cross-app unification.
  `GlassSurface.kt` is now a thin wrapper around the canonical
  `PremiumGlassSurface` mechanic, bound to `CustomerPalette`. Every glass
  surface goes through that one mechanic — never a bespoke translucent
  Box.
- **Shadow system** — `RojanShadows.kt` three-tier scale (`SoftElevation`
  8.4dp / `FloatingElevation` 18.9dp / `PremiumElevation` 25.2dp), applied
  by intent: resting elements Soft, standard cards Floating, hero-level
  and "lifted" elements (avatars, primary CTAs) Premium.
- **Color balance** — pastel token pairs from `RojanTokens.kt` only
  (`RojanBlushPink`/`RojanSoftLavender`, `RojanRose`, the
  `RojanCategory*Start/End` per-category accents); no raw hex outside
  `RojanTokens.kt`.
- **Card elevation style** — the layered "background ambient glow → glass
  surface → floating content" structure (see `RojanHomeCard.kt`,
  `HeroBookingCard.kt`) — blurred tint layer behind a crisp `GlassSurface`,
  never a single flat tinted rectangle.
- **Typography style** — named `RojanTypography` tokens only (`.copy()`
  for weight/letterSpacing tweaks is fine; raw `TextStyle` literals are
  not).
- **Animation style** — `rojanEnterAnimation` (fade + slight upward
  motion, staggered via `index * 60ms` in lists) for entrance,
  `rojanPressable` (scale to 1.06f, 150ms) for press feedback. No other
  entrance/press mechanism.

Purely additive changes (new screens/sections that consume these same
primitives at their current values) don't need approval. Changing the
values themselves, or introducing a parallel visual system, does.

## Design Baseline v1.0 (Frozen — Manager Dashboard)

Manager Dashboard's current visual direction (`ai.rojan.designlab.manager`,
isolated from Customer App) is the approved reference implementation for
the Manager module. Extend it consistently — reuse the same primitives/
tokens at their current values — rather than introducing a new visual
system. Do **not** change any of the following without explicit approval
first:

- **Layout** — `ManagerDashboardScreen.kt`'s section order: `ManagerHeader`
  → `SalonIdentityCard` → `TodayOverviewSection` → `QuickActionsSection` →
  `AIInsightCard` → `CalendarPreviewSection`, in a `LazyColumn` with
  `RojanDimens.SpaceLG` (24dp) vertical rhythm between sections.
- **Card hierarchy** — Salon Identity (one wide hero card) and the 2×2
  KPI grid carry the most visual weight; Quick Action chips are
  deliberately lighter/smaller (utility, not content); AI Insight and
  Calendar Preview are full-width supporting cards below. Don't rebalance
  this weighting without approval.
- **Warm white background** — `ManagerScaffold.kt` wraps the shared
  `WarmBackground` (solid `RojanWarmWhite` base + the existing
  `RojanBackgroundGradient` wash), not `PremiumBackground`. This is a
  Manager-only scaffold (`RojanScaffold` has no background parameter to
  swap) — it stays a thin duplicate of `RojanScaffold`'s layout, not a
  fork with new structure.
- **Glass style** — Manager IS the master reference implementation for
  the **Shared Premium Glass Design System** (see that section below):
  `ManagerGlassSurface` in `manager/components/ManagerGlassTheme.kt` now
  delegates to the canonical `PremiumGlassSurface` (fixed fill 0.14f/
  0.06f, border 1f/0.9f via the shared metallic border), bound to
  `ManagerPalette`. Customer's `GlassSurface`/`HomeGlassSurface` render
  through the identical mechanic, recolored via `CustomerPalette` — no
  parallel glass system remains.
- **Teal + Gold identity** — `ManagerAccent` (same file): `Teal` (=
  `RojanCategorySkinIcon`) for booking/customers/quick-actions/calendar;
  `Gold` (= `RojanRatingGold`) for revenue/occupancy/AI Insight. Distinct
  from Customer App's purple/pink identity — don't blend the two.
- **Typography hierarchy** — Section titles via `RtlSectionHeader`
  (`RojanTypography.SectionTitle`, right-aligned, `horizontalPadding =
  0.dp` since screen-level padding is already applied); KPI numbers at
  `RojanTypography.Display` (34sp/Bold — large and bold, intentionally
  the largest text on the screen); card titles/greeting at `CardTitle`;
  body copy at `Body`; secondary/meta text at `Caption`.
- **Spacing** — `RojanDimens` tokens only: `SpaceLG` between sections,
  `SpaceMD` card padding, `SpaceSM`/`SpaceXS` internal spacing;
  `QuickActionChip` is `width(84.dp)` + `heightIn(min = 88.dp)` (grows for
  two-line labels, never a fixed/clipped height).

Purely additive changes (new Manager screens/sections that consume these
same primitives at their current values — e.g. the sibling `calendar/`,
`customers/`, `services/`, `staff/`, `settings/` screens) don't need
approval. Changing the values themselves, or introducing a parallel
visual system, does.

## Shared Premium Glass Design System (Frozen — All Apps)

Every ROJAN app (Manager, Customer, and future Specialist/Reception/
Accountant/Inventory apps) renders every UI mechanic — glass, borders,
shadows, glow, icon rendering, typography, buttons, cards, dialogs,
sheets, navigation, search, inputs, chips, badges, animation — through
**one shared engine**. The **only** thing allowed to differ per app is
the color palette, expressed through `RojanAppPalette`
(`ui/theme/RojanAppPalette.kt`) — never a forked component or a second
implementation of the same mechanic. **Manager is the named master
reference**: when a mechanic needs picking between divergent historical
implementations, Manager's is canonical. This does not mean apps look
identical — same premium material language, different brand color
expression; each app keeps its personality through its palette's
gradients/accents, not through a different rendering mechanic.

**Palette contract:** `RojanAppPalette` (`fillTint`, `shadowAmbient`,
`shadowSpot`, `highlightTint`, `textPrimary`/`Secondary`/`Accent`) is
provided once via `LocalRojanPalette` at each app's root activity
(`MainActivity` → `CustomerPalette`, `ManagerActivity` →
`ManagerPalette`). It has no default — an unprovided palette fails loudly
rather than rendering mystery colors. New apps get a new
`RojanAppPalette` instance + root provider; zero new component code.

**Phase 1 — done:** glass surface mechanic (fill, shadow stack,
highlight, layering) unified into
`ui/components/glass/PremiumGlassSurface.kt` (fixed fill 0.14f/0.06f,
two-shadow stack at `elevation+16dp`/`elevation`, one highlight radial at
0.35× max dimension, the existing shared `premiumMetallicBorder` —
untouched, was already unified before this phase). `GlassSurface`,
`ManagerGlassSurface`, `HomeGlassSurface` are now thin palette-bound
wrappers around it; all existing call sites and their
`fillAlpha`/`borderAlpha` override parameters (used by a handful of
selected/unselected chip states) still work. The dead, unused
`screens/customer/theme/` package (a second, superseded "Customer"
visual identity — colors/glass/background/icon/text-field, zero real
call sites) was identified for deletion as part of this phase.

**Phases 2-6 — planned, not yet built:**
- **Phase 2 — Icon rendering.** Give the shared `RojanIconContainer` the
  glow/gradient-background/gradient-border hooks Manager's
  `ManagerIconContainer` currently bolts on externally; migrate
  Customer's plain call sites to opt in.
- **Phase 3 — Buttons.** One shared `PremiumButton` with a
  palette-supplied fill (`Gradient` vs `Glass`) so a bold CTA gradient is
  an available, identical-in-kind affordance in both apps' own colors —
  not a forked component (`PremiumButton` vs `ManagerPrimaryButton` as
  they exist today).
- **Phase 4 — Cards.** One shared card shell (accent-blur layer +
  `PremiumGlassSurface` + entrance stagger); retrofit Manager's
  hand-rolled cards onto it; resolve dead `RojanHomeCard` vs. live
  `HomeCard`.
- **Phase 5 — Net-new shared components.** Dialog, bottom sheet, chip,
  badge, search bar, input field, bottom navigation — none has a real
  cross-app mechanic to reconcile today (most are single-app or
  duplicated 3-5 ways with no shared base); this is new-component design
  work once Phases 1-4 establish the patterns to build on.
- **Phase 6 — Future apps.** When Specialist/Reception/Accountant/
  Inventory are actually built: new `RojanAppPalette` instance + root
  provider, zero new glass/border/button/card/icon code.

**Spacing rhythm (Frozen — All Apps):** every app uses only the
`RojanDimens` tokens (`ui/theme/Dimensions.kt`) — `SpaceXS` 4dp, `SpaceSM`
8dp, `SpaceMD` 16dp, `SpaceLG` 24dp, `SpaceXL` 32dp, `SpaceXXL` 48dp;
never a raw `.dp` literal for layout spacing. Wherever a screen is a
stacked-card "dashboard" list (a `LazyColumn` of card-like sections, not
a booking-flow step or a detail screen), three **named** rhythm tokens
are canonical — the semantic role, not the raw scale token, is what call
sites reference, so the whole rhythm moves from one place if it's ever
retuned again:
- **`RojanDimens.SpaceSectionToSection`** (= `SpaceMD`, 16dp) — on the
  screen's own top-level `LazyColumn`'s `verticalArrangement`, between
  whole sections/cards. Sections should read as one connected dashboard,
  not isolated islands with large empty gaps between them.
- **`RojanDimens.SpaceCardToCard`** (= `SpaceMD`, 16dp) — between cards
  *within* one section (a KPI grid's rows, a horizontal chip/card row's
  `Arrangement.spacedBy`).
- **`RojanDimens.SpaceTitleToContent`** (= `SpaceXS`, 4dp) — between a
  section's title (`RtlSectionHeader`, `ui/components/rtl/RtlLayoutKit.kt`,
  or an equivalent title `Row`) and the content below it.
  `RtlSectionHeader` itself applies zero vertical padding by design — the
  gap is always the caller's own `padding(top = RojanDimens.SpaceTitleToContent)`
  (or equivalent `Arrangement.spacedBy`), so a title reads as integrated
  into its section, not floating above it.

Screen-edge margins (a screen's own outer padding) are a separate
concern from this section/card/title rhythm — not touched by this rule.
`SectionToSection` and `CardToCard` currently share the same 16dp value,
but they're named separately because they answer different questions
(whole-section rhythm vs. within-section card rhythm) and may need to
diverge later — retuning one must never silently retune the other.

Verified live in `ManagerDashboardScreen.kt` (+
`TodayOverviewSection.kt`/`QuickActionsSection.kt`/
`CalendarPreviewSection.kt`) and `CustomerHomeScreen.kt` (+
`PopularServices.kt`, which keeps its own title `Row` rather than
`RtlSectionHeader` since it needs a trailing "مشاهده همه" action
`RtlSectionHeader` has no slot for — its gap value still matches the
canonical `SpaceTitleToContent`). The other ~13 files using
`RtlSectionHeader` (booking flows, calendar, customer-list screens) are
deliberately out of scope — they're booking-flow steps or detail/list
screens, the exact carve-out this rule already draws, and keep whatever
spacing already suits their own different layout shape. Migrate one onto
these tokens only if it's actually restructured into a stacked-card
dashboard list — not as a drive-by rename.

## ROJAN MANAGER FOUNDATION v1.0 FROZEN

Approved baseline — everything below is frozen. Do not change any of it
without explicit approval first. After this point: **only add new
Manager features, or fix confirmed bugs** — no architecture changes, no
applicationId changes, no Customer App edits, no Manager Dashboard
redesign.

**App architecture (frozen):**
- Two product flavors on the single `:app` module (`app/build.gradle.kts`,
  `flavorDimensions("target")`): `customer` and `manager`. No separate
  Gradle module, no shared library extraction — this was the audited,
  chosen approach (see "App ID Separation" history) specifically because
  it requires zero Customer file changes.
- `customer` flavor: zero overrides, inherits `defaultConfig` exactly —
  `applicationId = "ai.rojan.designlab"`, unchanged.
- `manager` flavor: `applicationId = "ai.rojan.designlab.manager"`,
  own manifest (`src/manager/AndroidManifest.xml`), own entry point
  (`src/manager/java/.../ManagerActivity.kt`), own launcher icon
  (existing `rojan_manager_logo` asset), own `app_name` string override.
- Both flavors compile the identical `src/main` Manager package
  (`manager/screens`, `manager/navigation`, `manager/components`) —
  nothing duplicated between them.
- Result: Customer and Manager install and launch independently on the
  same device, as two separate apps with two separate launcher icons.

**Customer App (frozen, do not touch):**
- `applicationId`: `ai.rojan.designlab` — never changes.
- No Customer screen, route, component, asset, or branding file gets
  edited for any Manager-related work, ever.

**Manager App entry (frozen):**
- Launcher → `ManagerActivity` → `managerNavGraph`, starting at
  `ManagerDestinations.SPLASH` → auto-advances to `DASHBOARD`.
- Splash and header both use the existing `rojan_manager_logo` drawable
  directly — no regenerated/alternate icon asset.

**Manager design system (frozen — see the "Design Baseline v1.0
(Frozen — Manager Dashboard)" section above for the full spec):**
Warm White background (`WarmBackground`/`ManagerScaffold`), Premium
Glassmorphism (`GlassSurface` + `ManagerGlass` opacity constants), Teal
+ Gold identity (`ManagerAccent`), the established typography hierarchy
(`RtlSectionHeader` + `RojanTypography.Display` for KPI numbers, etc.),
the existing RTL foundation, and the `RojanDimens` spacing rhythm.

**Completed modules (frozen scope, extend additively only):**
- Manager Dashboard v1.0 (Header, Salon Identity, KPI cards, Quick
  Actions, AI Insight, Calendar Preview)
- Manager Calendar MVP (Daily/Weekly views, appointment list, status
  indicators, specialist filter foundation, appointment detail entry
  point)
- Dashboard → Calendar navigation (`onViewCalendarClick` through
  `managerNavGraph`)
- Manager Logo integration (Header, Splash, Profile)

**After this freeze:** new Manager screens (`customers/`, `services/`,
`staff/`, `settings/` — still foundation-only folders) and bug fixes
are in scope. Changing the architecture, either applicationId, any
Customer file, or the Manager Dashboard's frozen visual baseline is
not — that requires explicit approval, same as every other frozen
section in this file.

## Environment notes

- Android SDK: `C:\Users\Rojan\AppData\Local\Android\Sdk`
- JDK: bundled with Android Studio (`Program Files\Android\Android
  Studio\jbr`) — set `JAVA_HOME` per-command, it's not in the environment by
  default.
- `sdkmanager` cannot reach the network in this environment (fails to
  download source lists/manifests) — only already-installed SDK packages are
  usable; don't attempt to download new system images, build-tools, etc.
- Available AVD: `Pixel_4` (`android-37.1 google_apis_playstore_ps16k`,
  x86_64) — the only system image installed. This is a heavy Play Store
  image and has been unreliable on this machine: repeated cold-boot attempts
  died silently right after WHPX init with no logged error. Give a boot
  attempt a real window (5+ min) before concluding it's stuck; if it dies
  repeatedly, don't keep retrying blindly — report it and prefer a connected
  physical device for verification when one is available.
