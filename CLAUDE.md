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
- **Glass system** — `GlassSurface.kt` defaults: `glassAlpha` 0.46f /
  `glassSecondaryAlpha` 0.18f, `borderAlpha` 0.24f /
  `borderSecondaryAlpha` 0.12f, highlight radial 0.32f/0.12f. Every glass
  surface goes through this one component — never a bespoke translucent
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
- **Glass style** — Every `GlassSurface` call in `manager/components/`
  passes the `ManagerGlass` constants (`manager/components/ManagerTheme.kt`):
  `glassAlpha` 0.72f / `glassSecondaryAlpha` 0.5f, `borderAlpha` 0.35f /
  `borderSecondaryAlpha` 0.2f — more opaque than the shared `GlassSurface`
  default (0.46f/0.18f fill, 0.24f/0.12f border), per the "semi-opaque,
  maximum readability" direction. The shared `GlassSurface.kt` itself
  stays untouched; Customer screens keep the original defaults.
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
