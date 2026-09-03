# ROJAN AI DesignLab — UI/UX PRO MAX Audit v1

**Date:** 2026-09-02
**Scope:** Full professional UI/UX audit (analysis only — no code changed, no files modified beyond this report).
**Method:** Direct source review of the design-system layer (`ui/theme`, `ui/components`), Manager module (`manager/`), Customer app (`screens/customer`, `screens/*`), Reception palette, navigation graphs, and the frozen baselines in `CLAUDE.md`.
**Tooling note:** The `ui-ux-pro-max` searchable rule database could not be queried in this environment (no Python runtime available). Findings below are from first-hand code review, cross-checked against the skill's built-in priority framework (Accessibility → Touch → Performance → Style → Layout → Type/Color → Motion → Forms → Navigation). General best-practice references are labelled as such.

**Brand constraint honoured:** This audit does **not** propose replacing the ROJAN identity. Rose Gold, Pink Glass, Purple, Soft White and Navy remain the brand system; every recommendation works inside it.

---

## 1. Executive Summary

ROJAN has an **unusually strong design-system foundation for an app of this stage**: a single canonical glass mechanic, a real token scale, a genuinely well-engineered Persian/Latin text-direction system, and disciplined use of shared primitives across three product flavours. The craft in `PremiumGlassSurface` and `premiumMetallicBorder` is real.

The weaknesses are almost all in the **finishing layer** — the things that separate "well-architected" from "feels premium in the hand":

| # | Theme | Severity |
|---|-------|----------|
| 1 | **No Persian typeface is bundled** — `FontFamily.Default` everywhere, in a Persian-first "luxury" product | P0 |
| 2 | **The entrance-motion system is a no-op** — `rojanEnterAnimation` returns `Modifier` unchanged; ~20 call sites still carry dead `visible`/stagger args. The app has essentially no content choreography. | P0 |
| 3 | **`CLAUDE.md` frozen baselines are stale** — both "Frozen — Manager Dashboard" (Warm White) and "Frozen — Customer Home" (Warm White) now ship as **dark** themes. Tier-0 governance risk: the source of truth contradicts the code. | P0 |
| 4 | **Accessibility gaps** — contrast failures (error text on light, muted text on navy, Gold CTA label), sub-48dp touch targets, no visible focus state anywhere, no reduced-motion handling, duplicate screen-reader announcements, a dead but screen-reader-announced notification bell. | P0/P1 |
| 5 | **Back button overlaps the first content row** on every back-enabled Manager screen, and is pinned to the LTR-leading (left) edge in an RTL app. | P1 |
| 6 | **Spatial RTL is incomplete** — horizontal carousels, icon/text rows in Manager, and text fields are laid out physically left-to-right. | P1 |
| 7 | **Two-tier inconsistency** — Customer Home is dark, the rest of the Customer app is light; Manager has curated nothing for page transitions while Customer has them; primary CTAs differ in kind between apps (bold gradient vs. faint glass). | P1 |
| 8 | **Glass is over-ornamented at small sizes** — 84dp chips and 64dp day-cells carry the full 7-pass metallic glow + two 8-point sparkle stars; at that scale it reads as glitter, not polished metal. | P2 |
| 9 | **Token-comment rot in `RojanTokens.kt`** — many doc comments describe values that no longer match the code (e.g. "Pure White" tokens that are actually dark purple). | P2 |
| 10 | **The AI-native promise is visually mute** — a real server-side insight engine exists, but the AI moment looks like every other card; `RojanAIGlow` renders at 0.05 alpha. | P1 |

### Scorecard (0–10, 10 = best-in-class premium)

| Dimension | Score | One-line |
|---|---|---|
| Premium quality | 6.0 | Excellent primitives; undercut by system font, dead motion, doc drift, ornament overload |
| Glassmorphism implementation | 6.5 | One canonical mechanic (good); not true backdrop-blur, over-decorated small, low tonal lift on dark |
| Design-system consistency | 6.0 | Strong shared layer; 3 palettes, 4 unbuilt unification phases, 2 button/icon/card systems |
| Color system | 6.5 | Brand identity preserved & reinforced; comment rot, error contrast, unratified teal |
| Typography | 4.5 | No Persian font, crowded large scale, tight line-height for Persian, inline overrides |
| Spacing | 7.0 | Clean scale + named rhythm tokens; section==card gap flattens hierarchy; raw dp in icon sizes |
| Components | 5.5 | Good shared primitives; fragile button API, weak Manager CTA, no-ripple/no-focus press, low-polish OTP/empty states |
| Navigation | 5.5 | Sane structure; back-button overlap, non-mirrored back, no Manager transitions, no deep links |
| Mobile UX | 5.0 | Touch-target misses, no insets/ime safety, no reduced-motion, silent error swallowing |
| Manager dashboard | 6.5 | Coherent, correct order, real data; low card/bg separation, redundant header, dead bell, plain states |
| Customer app | 5.5 | Rich content; dark Home vs light rest, 13-section wall, icon-only nav, misleading filter |
| Persian RTL experience | 6.0 | Best-in-class text-direction; spatial RTL (carousels, rows, fields, back) incomplete |
| Accessibility | 4.0 | Contrast fails, sub-48 targets, no focus state, no reduced motion, duplicate SR announcements |
| AI-native product feeling | 4.5 | Real AI backing exists but is invisible; gutted motion; no AI presence in core flows |
| **Overall** | **≈ 5.6** | **Strong bones, unfinished skin.** |

---

## 2. Premium Quality

**Strengths**
- Layered depth model is genuine: ambient blur layer → glass surface → floating content (`HomeCard`, `SalonIdentityCard`, `AISearchBar`), not flat tinted rectangles.
- Four-tier shadow scale (`SoftElevation` 8.4 / `FloatingElevation` 18.9 / `PremiumElevation` 25.2) applied by intent.
- Token discipline is high — hardcoded `Color(0x…)` is contained to the four palette-definition files plus one legacy `GlassOrb.kt`.
- The metallic-border engine (directional sheen, alternating light/shadow bands, corner sparkle) is a real piece of craft.

**What breaks the premium feel**
1. **System font.** No `.ttf`/`.otf` and no `res/font` directory exist. Persian renders through each device's fallback (Noto Naskh / Roboto), which is inconsistent device-to-device and is not a luxury typeface. This is the single largest gap between "premium-looking screenshots" and "premium in the hand."
2. **Nothing moves.** `rojanEnterAnimation` is `fun Modifier.rojanEnterAnimation(...) = this`. Combined with Manager having no page transitions, the app renders instantly with no reveal, no stagger, no "settle." Premium apps feel alive; this currently feels static.
3. **Ornament vs. restraint.** Every glass surface — including the smallest chips — paints a 7-step faux-glow, a 12-stop metallic gradient stroke, a vertical sheen pass, and two star sparkles. On a KPI hero card that's luxurious; on a 64dp calendar day-cell it's noise. Premium is knowing where to stop.
4. **Documentation contradicts the product.** A team member reading `CLAUDE.md` today would build the wrong thing (light Manager, light Home). Internal contradiction erodes the "everything is considered" impression that premium depends on.

---

## 3. Glassmorphism Implementation

**Architecture (good):** one mechanic, `PremiumGlassSurface`, consumed by thin palette-bound wrappers (`GlassSurface` → `CustomerPalette`, `ManagerGlassSurface` → `ManagerPalette`, `HomeGlassSurface` → `CustomerPalette`). Fill alpha (0.14 / 0.06), shadow-stack offsets, and highlight radius (0.35× max dimension) are fixed constants, not per-call knobs — exactly the right call for cross-app consistency. The only overridable knobs (`fillAlpha`, `borderAlpha`) are used correctly for selected/unselected chip states.

**Issues**
1. **It is not true glassmorphism.** The "glass" is a translucent linear-gradient fill (white 14% → tint 6%) plus an ornamental border. There is no backdrop blur of the content behind the surface. On the dark Manager/Home canvases a 14% white fill over a near-solid gradient produces almost no visible "frost" — the metallic border is doing ~90% of the perceived glass effect. If the border language ever changes, the glass collapses.
2. **Low tonal separation on dark.** Card fill vs. `ManagerColors.BasePrimary` (#063B3F) differ by only a few percent luminance. Depth is carried entirely by the border + drop shadow. On AMOLED at low brightness, cards will nearly merge with the background.
3. **Over-scaled ornament.** `premiumMetallicBorder` sizes its glow bloom and sparkles off the element's shorter side with clamps, but a 4-point star flash and 7 concentric glow passes cannot "read as polished metal" on a 200px chip — they read as glitter. Recommend a `compact` border variant (single-stroke metallic gradient, no sparkle, thinner glow) for surfaces below ~120dp.
4. **Continuous redraw.** `HomeMetallicRing` (bottom-bar Home FAB) runs an infinite 5.5s rotation and is always in composition (pinned bar). `GlassOrb`/`FrostedGlassOrb` also animate infinitely. None is gated by reduced-motion or off-screen visibility.

---

## 4. Design System Consistency

**Strong:** `RojanDimens`, `RojanTypography`, `RojanShapes`, `RojanShadows`, the `Text` RTL wrapper, `RojanIconContainer`, `rojanPressable`, `PremiumGlassSurface` — all single-source and widely adopted.

**Fragmented:**
1. **Three live palette families** — `RojanTokens` (light), `ManagerColors` (dark teal/emerald), `HomeColors` (dark navy/purple) — plus a **dead fourth** (`screens/customer/theme/CustomerColors` + siblings), which `CLAUDE.md` says was "identified for deletion" but is still present and still referenced by `CustomerThemeScreenshotTest.kt`.
2. **Customer app has two palettes internally.** `CustomerHomeScreen` uses `HomeBackgroundTheme` (dark); every other Customer screen uses `WarmBackground` (light). `CustomerPalette` still declares `fillTint = HomeColors.Primary`, i.e. the shared palette is tuned for the dark Home but most Customer screens render light. This is the "one palette per app" model breaking down inside one app.
3. **Four unification phases are documented but unbuilt** (icons, buttons, cards, net-new components). Consequences today:
   - **Buttons:** Customer `PremiumButton` (bold purple→magenta gradient pill) vs. Manager `ManagerPrimaryButton` (glass pill, Gold text, fill 0.28). Two different *kinds* of primary affordance.
   - **Icons:** `RojanIconContainer` (plain) vs. `ManagerIconContainer` (glow + gradient fill + gradient rim).
   - **Cards:** `RojanHomeCard` (light) vs. `HomeCard` (dark) vs. Manager's hand-rolled card bodies.
4. **Motion is inconsistent:** Customer nav graph has curated page transitions (`fadeIn` + `scaleIn 0.97`); Manager nav graph has none. Tap feedback: `rojanPressable` (no ripple) on cards/buttons vs. `Modifier.clickable(indication = LocalIndication.current)` (ripple) on bottom-bar tabs.
5. **`RojanTheme` / Material3 theming is decorative** — `RojanColorScheme` is fully specified but "no component reads `MaterialTheme.colorScheme`." Yet `ManagerOtpAuthScreen`'s raw `OutlinedTextField` *does* fall back to Material defaults for its label/placeholder — a leak between the two systems.

---

## 5. Color System

**Brand identity: preserved and reinforced.** Rose Gold + Gold live in `premiumMetallicBorder` and every glass edge. Purple / Magenta / Pink / Blush / Lavender tokens are intact and drive the Customer identity. Navy is the base of `PremiumBackground` and `HomeBackgroundTheme`. Soft White (`RojanWarmWhite`) is the light-canvas base. **No recommendation here disturbs this.**

**Issues**
1. **Comment rot in `RojanTokens.kt`.** Multiple doc comments describe values the code no longer holds:
   - `RojanTextOnDarkSurface` — comment: "now a solid 'Soft White' (#F4F6F8…)"; value: `Color(0xFF6B5579)` (muted purple).
   - `RojanLuxuryPrimaryHeading` "Luxury Soft White", `RojanLuxuryHeroTitle` "Hero Titles - Pure White" — both actually `Color(0xFF4D355F)` (dark purple).
   The entire "Luxury Typography & Contrast Finalization" family collapsed to three dark-purple values while the comments still promise white. A developer trusting the comment would place dark text on a dark surface (invisible). These tokens appear largely superseded by `ManagerColors` / `HomeColors` and should be audited for dead-or-wrong status.
2. **Error text contrast.** `RojanErrorText` #FF5C7A on `RojanWarmWhite` ≈ **2.9:1 — fails** WCAG AA for text (used on the Customer auth screen). On Manager's dark teal ≈ 4.6:1 — marginal pass only.
3. **Muted text on navy.** `HomeColors.TextMuted` #9C8FB5 on `RojanNavy` ≈ **4.0:1** — fails AA for text < 18.66px. It is used as the `AISearchBar` placeholder and for metadata.
4. **Gold CTA label.** `ManagerPrimaryButton` renders `ManagerColors.Gold` (#D4AF37) text on a ~28%-white-over-dark glass fill ≈ 4.5–5:1 — marginal for the app's primary action.
5. **Teal identity is unratified against the stated brand.** `ManagerColors` introduces a full emerald/turquoise system (#063B3F / #00C9C8) that is not one of the five brand colours. `CLAUDE.md` sanctions "Teal + Gold" for Manager, but the teal there is defined as an *alias onto the cyan booking-category token*, not the deep bespoke emerald that now ships. Recommend an explicit written ratification of Manager's dark-emerald palette as an approved per-app expression, so it stops looking like drift.

---

## 6. Typography

1. **No Persian font.** (See §2.) `Vazirmatn` is named as the target in `Type.kt` but no asset exists. **P0 for a Persian-first premium product.**
2. **Crowded large end of the scale.** `Display` 34 / `HeroTitle` 32 / `ScreenTitle` 30 are three near-identical sizes with overlapping roles (`HeroTitle` on the OTP screen, `ScreenTitle` on Calendar, `Display` on KPI numbers). A reader cannot feel the difference between 30 and 32. Collapse to two distinct roles (e.g. Display 34 for hero numbers, ScreenTitle 28 for screen titles) or widen the gap.
3. **Line-height is tight for Persian.** `Body` 17/24 = 1.41; `Caption` 15/20 = 1.33. Persian glyphs are taller and carry more above/below the baseline than Latin; 1.5–1.7 is the comfortable range. Risk of clipped descenders once a real Persian face is installed.
4. **Inline overrides bypass the token system.** `ManagerHeader` hardcodes `fontSize = 22.sp` / `12.sp` / `letterSpacing = 2.sp` / `FontWeight.Bold` instead of `RojanTypography` tokens. The `12.sp` subtitle is below the "never below 15sp" floor the `Type.kt` comments themselves establish.
5. **Mixed numeral systems.** Manager uses `toPersianDigits()` consistently (KPI values, counts, occupancy). Customer screens largely show Western digits (ratings "4.8", prices). Pick one convention app-wide — for a Persian-first product, Persian numerals in Persian contexts.
6. **Positive letter-spacing is correctly avoided on Persian** (only the Latin "ROJAN AI" wordmark carries `2.sp`). Good — keep it that way; Persian is a connected script and letter-spacing breaks it.

---

## 7. Spacing

**Strong:** The `RojanDimens` scale (4/8/16/24/32/48) is clean, and the three *named* dashboard rhythm tokens (`SpaceSectionToSection`, `SpaceCardToCard`, `SpaceTitleToContent`) are a genuinely good idea — semantic roles, retune from one place.

**Issues**
1. **`SpaceSectionToSection == SpaceCardToCard == 16dp`.** Whole sections get no more breathing room than cards *inside* a section, so a dashboard reads as one uniform stack with weak visual grouping. `CLAUDE.md` defends this as "one connected dashboard," and the Customer Home history even shows a pass that raised spacing to 24 then pulled it back to 16 — the codebase is arguing with itself. For a boutique/premium feel, section separation should be perceptibly larger than card separation (e.g. 24 vs 16). The tokens are already named separately — only the values need to diverge.
2. **Un-tokenized icon-container scale.** `ManagerIconContainer` `containerSize` is passed raw at 36 / 40 / 44 / 48 / 56 / 64 dp across `StatCard`, `QuickActionChip`, `ManagerHeader`, `SalonIdentityCard`, `AIInsightCard`, `UpcomingSlotRow`. This is a parallel spacing scale with no token behind it, directly contradicting the "never a raw `.dp` literal for layout" rule.
3. **Other raw `.dp` leaks:** `ManagerHeader` `width(12.dp)` / `size(48.dp)`; `ManagerCalendarScreen` day cell `64×72`, weekly cell `120×220`, status dots `8.dp`/`6.dp`; `CustomerBottomBar` `88`/`72`/`64`/`1.55.dp`; `AISearchBar` `blur(16.dp)`; `RojanIconContainer` `RojanShadows.SoftElevation / 3` (arithmetic on a token).
4. **Screen-edge margins differ by app** — Customer Home `SpaceLG` (24) horizontal, Manager `SpaceMD` (16) all round. Fine if intentional, but it should be a named decision, not an accident of two scaffolds.

---

## 8. Components

| Component | Finding |
|---|---|
| `PremiumButton` | Default `modifier = Modifier.size(240dp × 64dp)`. Any caller passing its own modifier **silently loses the sizing**. Fragile API. A fixed 240dp width also doesn't suit full-width mobile CTAs. Disabled state = 50% alpha on the whole button incl. label → reads "dim," not "disabled." |
| `ManagerPrimaryButton` | The app's Manager primary CTA is a translucent glass pill (fill 0.28) with Gold text — low visual weight for a "confirm booking" action. No loading state (only `enabled` toggles; no spinner) — during OTP verification the button just dims. |
| `rojanPressable` | Scales **up** to 1.06 on press. Standard mobile feedback compresses (0.96–0.98) — "pressed = pushed in." Growing on touch fights that model and risks overlapping neighbours in tight chip rows (calendar toggles, specialist filter). `indication = null` removes the ripple **and** any focus indication — there is no visible focus state anywhere on the app's primary interaction primitive (fails keyboard/switch-access users). |
| `rojanEnterAnimation` | No-op (`= this`). ~20 call sites still pass `visible` / `delayMillis = index * 60` and run `LaunchedEffect { visible = true }` — dead scaffolding. |
| `ManagerHeader` bell | `ManagerIconContainer(contentDescription = "اعلان‌ها")` with `alpha(0.5f)` and **no click handler**. A labelled, half-dimmed control that isn't interactive — a fake affordance that a screen reader announces as actionable. |
| OTP entry (`ManagerOtpAuthScreen`) | Single raw `OutlinedTextField` (breaks the glass language); `KeyboardType.NumberPassword` **masks the code** so the user can't verify what they typed; no segmented 6-box input; no SMS-autofill hint; `Arrangement.Center` on a wrap-height column does nothing; no `imePadding()`/scroll → keyboard can cover the field on short screens. |
| Empty states (Manager) | Plain `Text` ("امروز نوبتی ثبت نشده است.") — no icon, no illustration, no CTA. `RojanEmptyState` exists but Manager doesn't use it. |
| Loading states (Manager) | `SalonIdentityCard` shows "در حال بارگذاری…" text; KPI grid / calendar just show zeros / empty. `RojanShimmer` exists but is unused in Manager — a zero KPI is indistinguishable from a failed load. |
| Status indicators | Color + word ("● فعال") — the word saves it from being color-only. Keep that pattern everywhere. |

---

## 9. Navigation

1. **Back button overlaps content.** `ManagerScaffold` (and `RojanScaffold`) render the back button as a `TopStart` overlay with `padding(SpaceLG)` on top of content that has only `padding(SpaceMD)` and starts at y = 0. On `ManagerCalendarScreen` the first item is `RtlSectionHeader("تقویم")` — the 48dp button lands on the left half of that title band. This repeats on every back-enabled Manager screen (Services, Staff, Customers, Settings, Booking wizard). The scaffold needs to reserve a top inset when `onBackClick != null`.
2. **Back button is not RTL-mirrored.** Pinned to `Alignment.TopStart` (physical top-left). Android's platform convention mirrors the "up" affordance to the top-right in RTL, and Persian users expect it there. `RtlLayoutKit`'s comment explains the deliberate choice (avoiding earlier photo-mirroring bugs), but the fix for *that* was never "pin back to the left" — the two are separable.
3. **Manager has no page transitions** (Customer does). Screens snap in.
4. **No deep links.** Nav args exist (`tag`, `serviceId`, `appointmentId`, `customerId`) but no `deepLinks { }` — no external entry points, no notification → screen routing.
5. **Manager structure is otherwise sound** — hub-and-spoke from the dashboard, one nested booking graph sharing one ViewModel, correct `popUpTo` behaviour. No bottom nav is a defensible choice for a manager tool.
6. **Customer bottom nav** — 5 tabs (within the ≤5 guideline), Home centered/raised. But tabs are **icon-only, no text labels** — hurts discoverability and first-run learnability (screen-reader `contentDescription` is set, so SR users are fine; sighted new users are not). The filter icon in `AISearchBar` has its own `contentDescription = "فیلتر"` but performs the *same* navigation as tapping the bar — a misleading affordance.

---

## 10. Mobile UX

1. **Touch targets below 48dp:** `SpecialistChip` / `ToggleChip` in Calendar (`padding(h16, v8)` on a Caption ≈ 36dp tall, no `heightIn`); `AIInsightCard` inactive-customer line (≈ 36dp); `CalendarPreviewSection` "مشاهده تقویم کامل" row (no min height). `MinTouchTarget = 48.dp` is defined — it just isn't applied to these.
2. **No reduced-motion handling anywhere.** Infinite rotations (`HomeMetallicRing`, `GlassOrb`), press-scale, and page transitions ignore the system animation scale / accessibility setting.
3. **Window insets not handled in the scaffolds.** `ManagerScaffold` / `RojanScaffold` apply no `systemBarsPadding()` / `imePadding()` / `navigationBarsPadding()`. If the activities call `enableEdgeToEdge()` (typical), content runs under the status/nav bars and behind the keyboard. **Verify on device** — if edge-to-edge is on, this is a P1 clipping bug.
4. **Silent error swallowing.** `ManagerDashboardScreen` catches `ManagerRepositories.initialize()` failure and shows nothing — the dashboard renders zero KPIs and an empty calendar with no "couldn't load / retry." This is the same class of bug a prior internal report flagged for salon identity; it still exists for stats and calendar.
5. **No pull-to-refresh** in Manager — data refreshes only on screen entry.
6. **Horizontal carousels are not RTL-mirrored** — see §12.

---

## 11. Manager Dashboard

**What's right**
- Section order exactly matches the frozen spec: Header → Salon Identity → Today Overview → Quick Actions → AI Insight → Calendar Preview.
- 2×2 KPI grid with `Display` 34sp numbers — correct visual weight; the largest text on the screen.
- Alternating Turquoise/Gold KPI icon circles match the reference intent.
- Real backend data throughout (stats, salon, insights, today's slots), with an honest loading state on Salon Identity.

**What holds it back**
1. **Low card-to-background separation** (see §3.2) — the dashboard's depth lives almost entirely in the metallic border. Consider nudging the Manager glass fill up a few percent, or adding a subtle inner top-highlight, so cards lift off #063B3F tonally, not just via ornament.
2. **Redundant brand stack in the header** — logo image + "ROJAN AI" wordmark + "اکوسیستم هوشمند زیبایی" subtitle + "خوش آمدید،" + manager name + dead bell, all stacked above the fold. That's a lot of chrome before the first KPI. The wordmark duplicates the adjacent logo.
3. **Dead notification bell** (see §8).
4. **Plain empty/loading states** (see §8) — the KPI grid showing "۰ / ۰ / ۰ / ٪۰" on a slow network looks like a broken salon, not a loading one.
5. **The AI Insight card doesn't feel like AI** — same glass card, same weight, no motion, no "why," no confidence, no distinct accent beyond the Gold icon. See §14.

---

## 12. Customer App

1. **Theme whiplash.** Home is dark navy (`HomeBackgroundTheme`); Salon Details, Booking, Profile, Favorites, etc. are light (`WarmBackground`). Navigating Home → Salon Details is a full light/dark flip mid-session. Either bring the dark language to the rest of the Customer app or keep Home light — the split is the worst of both.
2. **13 sections in one `LazyColumn` at 16dp spacing** — a very long scroll with weak sectioning. Section headers are also inconsistent (`PopularServices` rolls its own title Row with a trailing action; others use `RtlSectionHeader`; some sections have no header).
3. **Icon-only bottom nav** (see §9.6).
4. **`AISearchBar`** — a tap-to-navigate fake search is a fine pattern, but the separate filter glyph doing the same thing is misleading, and the placeholder text uses a sub-4.5:1 muted color on navy.
5. **Positive:** the `HeroBookingCard` / `HomeCard` layered-glow-behind-glass construction is the design system at its best — reuse this shell for the Manager cards when Phase 4 lands.

---

## 13. Persian RTL Experience

**Best-in-class:** `RojanText` resolves base direction and alignment from each string's **first strong character** (the Unicode Bidi / `dir="auto"` rule), not from device locale. Persian right-aligns, Latin left-aligns, digit-only strings default RTL, mixed runs shape correctly. This is better than most shipping RTL apps. `withDirectionFor(value)` extends it to text fields.

**Incomplete — the spatial layer:**
1. **Horizontal `LazyRow` carousels populate and scroll left→right.** Specialist filter, day selector, weekly overview, quick actions, and every Customer Home carousel (`FeaturedSalons`, `TopSpecialists`, `NearbySalons`, …). In an RTL product these should originate at the **right** edge and scroll leftward. Because the app deliberately avoids `LayoutDirection.Rtl` app-wide, none of them are mirrored.
2. **Manager icon/text rows are physically LTR.** `SalonIdentityCard`, `AppointmentRow`, `UpcomingSlotRow`, `AIInsightCard`, `CalendarPreviewSection` build `Row { Icon ; Column(text) }` — icon on the physical left. `RtlLayoutKit` (`RtlInfoRow`, `RtlListRow`) was built to fix exactly this (leading element on the right), but the Manager components don't use it.
3. **`ManagerTextField`** (OTP screen) doesn't apply `withDirectionFor` — the phone/code field's direction is unmanaged, and its Material label falls back to locale behaviour.
4. **Back button** on the LTR-leading edge (see §9.2).
5. **Numeral inconsistency** (see §6.5).
6. **Icon mirroring is ad hoc** — `ManagerScaffold` correctly uses `Icons.AutoMirrored.Filled.ArrowBack`; `CalendarPreviewSection` hardcodes `Icons.Filled.ChevronLeft` (happens to be correct for RTL "forward," but by luck, not rule).

**Net:** text direction is a genuine strength; layout direction (carousels, rows, fields, back) needs a deliberate pass — ideally scoped RTL wrappers for horizontal lists, the way `RtlLayoutKit` already scopes rows.

---

## 14. Accessibility

| Area | Finding | Severity |
|---|---|---|
| Contrast — error text | `RojanErrorText` on `RojanWarmWhite` ≈ 2.9:1 (fails AA text) | P0 |
| Contrast — muted on navy | `HomeColors.TextMuted` on `RojanNavy` ≈ 4.0:1 (fails AA small text); used as search placeholder | P1 |
| Contrast — Gold CTA label | `ManagerColors.Gold` on Manager primary-button glass ≈ 4.5–5:1 (marginal for the primary action) | P1 |
| Touch targets | Specialist/toggle chips, AI-insight line, calendar-preview CTA row below 48dp | P1 |
| Focus state | `rojanPressable` uses `indication = null` — **no visible focus indicator anywhere** on the primary interaction primitive (keyboard, switch access, D-pad, TV) | P0 |
| Reduced motion | Not honoured — infinite rotations, page transitions, press-scale all unconditional | P1 |
| Screen reader — duplicate announcements | `QuickActionChip` and `StatCard` pass `contentDescription = <label>` on an icon that sits directly above the same visible text → announced twice. Should be `null` when a visible label exists. | P2 |
| Screen reader — dead control | Notification bell announces "اعلان‌ها" but does nothing | P1 |
| Headings | Section headers have no `semantics { heading() }` — SR users can't navigate by heading | P2 |
| Dynamic type | Sizes are in `sp` (good) but fixed-height containers (`HeroHeight` 360, day cells 72, weekly cells 220) will clip text at large font scales | P2 |
| Positive | `contentDescription` is broadly present with real Persian labels; status uses text + color, not color alone | — |

---

## 15. AI-Native Product Feeling

**Real substance exists:** the Manager AI Insight card is backed by a server-side deterministic rule engine over real revenue/booking/customer data, with an honest null state. `RojanAIGlow` is a first-class "AI is active" token. `AutoAwesome` sparkle iconography and the "اکوسیستم هوشمند زیبایی" positioning are consistent.

**But it doesn't *feel* AI-native:**
1. **The AI moment is visually mute.** The Insight card is the same glass card at the same weight as Salon Identity and Calendar Preview. No distinct surface treatment, no reveal motion, no "typing/streaming" affordance, no confidence or "why you're seeing this," no primary action on the recommendation itself (only a secondary "inactive customers" link).
2. **`RojanAIGlow` renders at 0.05 alpha** as a background radial — the "AI active" signal is essentially invisible. The one place the brand says "this is the intelligent layer" is imperceptible.
3. **AI is absent from the core flows.** The booking wizard (both Manager and Customer) has the server data to suggest best times / likely-preferred specialists / smart slots, and does none of it. The `AISearchBar` is a dead-end navigation with no suggestions, recent prompts, or natural-language affordance.
4. **No personalization is surfaced** beyond static section titles ("پیشنهاد شده"). Nothing communicates "this adapts to you."
5. **The gutted motion system** compounds this — an AI product that renders instantly with zero choreography feels like a static catalog, not an assistant. A short "reveal" beat on AI content (even 200–300ms) would do a lot.

---

## 16. Prioritized Recommendations (direction only — no code, no scope commitment)

### P0 — do before any "premium" or "pilot-ready" claim
1. **Bundle a Persian typeface** (Vazirmatn is already the documented target). One `FontFamily`, wired into `RojanTypography` — call sites don't change. Re-check line-heights (§6.3) and fixed-height budgets after.
2. **Reconcile `CLAUDE.md` with reality.** Update the two "Frozen" baseline sections to describe the dark Manager and dark Customer-Home themes that actually ship, or formally record the redesigns as approved changes. This is a Tier-0 governance item — the source of truth currently misinforms.
3. **Restore a real entrance/settle motion** (or delete `rojanEnterAnimation` and its ~20 dead call sites entirely — but a premium product should have the motion). Add Manager page transitions to match Customer.
4. **Give the interaction primitive a visible focus state** — `rojanPressable` currently erases both ripple and focus.
5. **Fix the failing contrast pairs** — error text on light, muted text on navy, Gold primary-CTA label — staying inside the existing hues (darken/adjust the token, don't introduce new colors).
6. **Verify window-insets handling on device.** If the activities are edge-to-edge, add safe-area + IME padding to both scaffolds.

### P1
7. **Fix the back-button overlap** — reserve a top inset in the scaffolds when a back button is present; and mirror its position to the trailing edge for RTL.
8. **Scoped RTL for horizontal lists** — a wrapper (like `RtlLayoutKit` does for rows) that makes carousels originate at the right and scroll leftward.
9. **Migrate Manager icon/text rows onto `RtlInfoRow`/`RtlListRow`.**
10. **Unify primary CTAs** — Manager's primary action needs the same *kind* of weight as Customer's (a real filled/gradient affordance in Manager's own Teal/Gold), not a faint glass pill. This is Phase 3; it's now blocking.
11. **Resolve the Customer light/dark split** — one direction for the whole Customer app.
12. **Make the AI layer visible** — a distinct treatment for AI Insight (accent, a reveal beat, a "why", a primary action), and raise `RojanAIGlow`'s presence where it signals activity.
13. **Real empty/loading states in Manager** — use `RojanEmptyState` and `RojanShimmer`; distinguish "loading" from "zero."
14. **Surface load failures** — replace silent-swallow with an inline retry on the dashboard.
15. **OTP screen polish** — segmented input, unmasked code, glass-consistent field, button loading state, IME padding.
16. **Touch-target sweep** — apply `MinTouchTarget` to the chips and text-link rows that currently miss it.

### P2
17. **`compact` metallic-border variant** for surfaces below ~120dp (no sparkle, single-stroke).
18. **Tokenize the icon-container size scale** (36/40/44/48/56/64) and remove remaining raw `.dp` layout literals.
19. **Diverge `SpaceSectionToSection` from `SpaceCardToCard`** (24 vs 16) for real dashboard grouping.
20. **Clean `RojanTokens.kt`** — fix or delete the rotted comments and the superseded "Luxury*" token family; delete the dead `screens/customer/theme/` package (and its screenshot test).
21. **Decorative-icon `contentDescription = null`** where a visible label already exists; add `heading()` semantics to section headers.
22. **Collapse the crowded large type scale** (Display / HeroTitle / ScreenTitle → two distinct roles).
23. **One numeral convention** app-wide (Persian digits in Persian contexts).
24. **Reception app** currently inherits `WarmBackground` + Amber palette with zero bespoke screens reviewed here — when it's built, hold it to this same checklist.

---

## 17. What NOT to Change

- **The five-color brand system** (Rose Gold, Pink Glass, Purple, Soft White, Navy) — keep it. Every fix above works inside it.
- **`PremiumGlassSurface` as the single glass mechanic** — the architecture is right; only its ornament-at-small-scale and tonal-lift-on-dark need tuning.
- **`RojanText`'s first-strong-character direction system** — this is a genuine asset; extend the *spatial* RTL story to match it, don't replace it.
- **`RojanDimens` token scale and the named rhythm tokens** — sound; only two values need retuning.
- **Manager dashboard section order and KPI weighting** — matches the approved reference; leave it.
- **The metallic-border craft** — beautiful on hero surfaces; just don't paint the full version on 64dp chips.

---

*End of audit. No files were modified other than this report. No code was written. No commits were made.*
