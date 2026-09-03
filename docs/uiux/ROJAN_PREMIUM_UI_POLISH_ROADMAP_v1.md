# ROJAN AI — Premium UI Polish Roadmap v1

**Type:** Professional UI/UX Upgrade Roadmap
**Mode:** Planning only — no code, no file changes except this document, no commit, no push.
**Companion:** [`ROJAN_UIUX_PRO_MAX_AUDIT_v1.md`](./ROJAN_UIUX_PRO_MAX_AUDIT_v1.md)
**Date:** 2026-09-02
**Status:** DRAFT — awaiting implementation authorization. Nothing here is approved for execution.

---

## 0. Preamble

### 0.1 Non-negotiable constraints

The existing **ROJAN Design System is authoritative and is not replaced**. Every task below operates inside it. Preserved without exception:

- **Brand colors:** Rose Gold, Pink Glass, Purple, Soft White, Navy
- **Premium Glass surfaces** — the `PremiumGlassSurface` mechanic and the shared metallic border language
- **RTL-first, Persian-first experience**
- **Feminine luxury AI identity** ("اکوسیستم هوشمند زیبایی" / BeautyOS)
- The `RojanDimens` spacing scale, `RojanShapes`, `RojanShadows` tiers, and the `RojanText` first-strong-character direction engine

"Polish" here means **tuning values, completing half-built systems, and adding the finishing layer** — not new visual languages.

### 0.2 Audit corrections carried into this roadmap

Deeper review since the audit refined three findings — the roadmap is built on the corrected picture:

| Audit said | Corrected picture |
|---|---|
| "Customer Home is dark, the rest of the Customer app is light — theme whiplash" (§12.1) | **The entire Customer app has already migrated to `HomeBackgroundTheme` (dark navy/purple)** — Home, Search, Auth, Splash, Salon Details, all booking steps, all ~15 Profile screens. `WarmBackground` / `PremiumBackground` / `RojanScaffold` / light `GlassSurface` / the `RojanColorScheme` light scheme / the light `RojanText*` tokens are now **legacy scaffolding with almost no live call sites**. The real system is two coherent dark-luxury themes (Customer navy/purple, Manager emerald/gold) over one shared glass + rose-gold-border mechanic. This is *good* for consistency and *much worse* for documentation drift. |
| "Manager has plain empty/loading states; `RojanEmptyState`/`RojanShimmer` unused" (§8) | Accurate for **Manager**. The **Customer** app already uses `RojanEmptyState`, `RojanErrorState`, `RojanSkeletonBox` properly (see `SearchScreen`). The gap is Manager-only + inconsistent adoption. |
| "No success-animation primitive" implied | `RojanSuccessCheckmark` **exists** (spring overshoot, ring-then-check beat). It is underused, not missing. |

### 0.3 Maturity model

Scored 0–10 per dimension (10 = best-in-class premium AI product). "Current" is from the audit scorecard.

| Dimension | Current | Target v1 | Target ceiling |
|---|---|---|---|
| Premium quality | 6.0 | 8.5 | 9.5 |
| Glassmorphism implementation | 6.5 | 8.5 | 9.0 |
| Design-system consistency | 6.0 | 9.0 | 9.5 |
| Color system | 6.5 | 8.5 | 9.0 |
| Typography | 4.5 | 8.5 | 9.5 |
| Spacing | 7.0 | 8.5 | 9.0 |
| Components | 5.5 | 8.5 | 9.0 |
| Navigation | 5.5 | 8.0 | 9.0 |
| Mobile UX | 5.0 | 8.5 | 9.0 |
| Manager experience | 6.5 | 8.5 | 9.5 |
| Customer experience | 5.5 | 8.5 | 9.5 |
| Persian RTL experience | 6.0 | 9.0 | 9.5 |
| Accessibility | 4.0 | 8.0 | 9.0 |
| AI-native product feeling | 4.5 | 8.5 | 9.5 |
| **Weighted overall** | **≈ 5.6** | **≈ 8.5** | **≈ 9.2** |

**Current maturity score: 5.6 / 10 — "Strong bones, unfinished skin."**
**Target maturity score (this roadmap): 8.5 / 10 — "Premium AI-native BeautyOS, pilot-ready."**

### 0.4 Legend

- **Priority:** P0 (blocks premium/pilot claim) · P1 (materially raises quality) · P2 (refinement)
- **Impact:** Low / Medium / High (on perceived product quality)
- **Effort:** Small (≤1 dev-day) · Medium (2–5 dev-days) · Large (1–3 dev-weeks)
- **Dependencies:** what must land first
- **Governance tag:** `[S1]` = touches System 1 territory (backend / contract / RBAC) → confirmation required · `[GOV]` = touches a frozen baseline or `CLAUDE.md` · `[SAFE]` = inside automatic-action scope

---

## PHASE 1 — Design Foundation

Goal: fix the layer everything else sits on — type, tokens, and the documents of record — before touching motion or components.

### 1.1 Typography

| ID | Task | Priority | Impact | Effort | Dependencies | Tag |
|---|---|---|---|---|---|---|
| T1.1 | **Integrate a Persian variable font** (Vazirmatn is the documented target). Bundle weights 300–800, register one `FontFamily`, wire into `RojanTypography` only (call sites unchanged). Keep a Latin fallback stack in the same family for mixed runs. | P0 | High | Medium | none | `[SAFE]` |
| T1.2 | **Define the weight system.** Map semantic roles → weights: Display/Screen/Section titles = 700; Card titles/greeting = 600; Body = 500; Body-emphasis = 600; Caption/meta = 400. Document as named constants, remove ad-hoc `FontWeight.Bold` inline (e.g. `ManagerHeader`). | P1 | Medium | Small | T1.1 | `[SAFE]` |
| T1.3 | **RTL typography hierarchy pass.** After the real font lands: re-tune `lineHeight` for Persian (Body → ~1.55, Caption → ~1.5, titles ~1.2–1.25); verify no positive `letterSpacing` reaches Persian strings; confirm numerals. Add `heading()` semantics to `RtlSectionHeader`. | P1 | High | Medium | T1.1 | `[SAFE]` |
| T1.4 | **Collapse the crowded display scale.** `Display 34` / `HeroTitle 32` / `ScreenTitle 30` → two roles with a real gap (e.g. Display 34 for hero numbers/moments, ScreenTitle 28 for screen titles); retire `HeroTitle` or re-point it. Audit `HeroHeight`/`ButtonHeight` fixed budgets against the new sizes + dynamic type. | P1 | Medium | Medium | T1.1, T1.2 | `[GOV]` (frozen "Typography style") |
| T1.5 | **One numeral convention.** Persian digits in Persian contexts app-wide. Manager already has `toPersianDigits()`; extend the helper to Customer (prices, ratings, counts) or add a shared formatter. | P2 | Medium | Medium | none | `[SAFE]` |
| T1.6 | **Display / Body / Caption usage rules doc.** A one-page "when to use which token" reference with do/don't screenshots, checked into `docs/uiux/`. | P2 | Low | Small | T1.4 | `[SAFE]` |

**Phase-1 typography DoD:** real Persian font renders on device across all three flavours; no inline `fontSize`/`fontWeight` literals remain in shared components; type-scale reference doc exists; screenshot tests updated.

### 1.2 Design Token Cleanup (analysis + plan only — **no token values change in Phase 1**)

The audit found the token *architecture* is sound but carries dead weight and rotted comments from the light→dark migration. This sub-phase produces a **Token Reconciliation Report**, not edits.

| ID | Task | Priority | Impact | Effort | Dependencies | Tag |
|---|---|---|---|---|---|---|
| T1.7 | **Color-token audit.** Classify every token in `RojanTokens.kt` as: LIVE / LEGACY-LIGHT (dead now that Customer is dark) / WRONG-COMMENT / ALIAS-CHAIN. Deliverable: a table + a proposed 3-tier target (primitive → semantic → per-app palette) that keeps `RojanAppPalette` as the seam. Flag the "Luxury*" family (comments say "Pure White", values are `#4D355F`) and `RojanTextOnDarkSurface`. | P1 | High | Medium | none | `[GOV]` |
| T1.8 | **Spacing-token audit.** Confirm the 4/8/16/24/32/48 scale is fully adopted; list every raw `.dp` layout literal in shared components + Manager (icon container sizes 36/40/44/48/56/64, `ManagerHeader` `12.dp`, calendar cell sizes, bottom-bar `88/72/64`). Propose an **icon-container size token set** (`IconContainerSM/MD/LG/XL`) to sit alongside the existing `IconSize*` glyph tokens. | P1 | Medium | Small | none | `[SAFE]` |
| T1.9 | **Border-token audit.** Document the `premiumMetallicBorder` stop list + the 5 rose-gold/gold tokens; propose a `compact` variant contract (single-stroke metallic gradient, no sparkle, thinner glow) for surfaces < ~120dp. No rendering change yet — spec only. | P1 | Medium | Small | none | `[GOV]` (frozen glass) |
| T1.10 | **Shadow / glow-token audit.** Verify the 3-tier elevation scale usage; catch arithmetic-on-token (`SoftElevation / 3`); define a named `RojanGlow` scale (currently `RojanAIGlow` is used at one hard-coded 0.05 alpha). Propose glow-intensity tiers for the motion phase. | P1 | Medium | Small | none | `[SAFE]` |
| T1.11 | **Icon-sizing-token audit.** Reconcile `RojanIconSize` (14/20/32/56) with the ad-hoc `sizeOverride` values still in use; propose whether the 4 tiers need a 5th. | P2 | Low | Small | T1.8 | `[SAFE]` |
| T1.12 | **Token Reconciliation Report** — single doc consolidating T1.7–T1.11 with a proposed migration sequence, risk per change, and an explicit "these tokens are safe to delete" list (gated behind its own future authorization). | P1 | High | Small | T1.7–T1.11 | `[GOV]` |

**Phase-1 token DoD:** Token Reconciliation Report reviewed and signed off; no token values changed; a prioritized, risk-assessed token-cleanup backlog exists for a later authorized phase.

### 1.3 Documentation Alignment

The single highest-severity governance item. `CLAUDE.md`'s Tier-1 frozen baselines now actively misdescribe the product.

| ID | Task | Priority | Impact | Effort | Dependencies | Tag |
|---|---|---|---|---|---|---|
| T1.13 | **Reconcile `CLAUDE.md` "Design Baseline v1.0 (Frozen — Customer Home)".** It describes `WarmBackground` (solid warm white). Reality: the whole Customer app is `HomeBackgroundTheme` (dark navy/purple). Rewrite the section to describe the shipped dark baseline, OR formally record the light→dark migration as an approved change with date + approver. | P0 | High | Small | none | `[GOV]` |
| T1.14 | **Reconcile "Design Baseline v1.0 (Frozen — Manager Dashboard)".** It describes `WarmBackground` + Teal-aliased-onto-cyan. Reality: `ManagerBackgroundTheme` (deep emerald) + bespoke `ManagerColors`. Same choice: rewrite or ratify. **Ratify Manager's dark-emerald palette explicitly as an approved per-app expression of the brand** so it stops reading as drift. | P0 | High | Small | none | `[GOV]` |
| T1.15 | **Reconcile "Shared Premium Glass Design System" phase status.** CLAUDE.md lists Phases 2–6 as "planned, not built." This roadmap's Phase 3 is those phases. Update CLAUDE.md to point at this roadmap as the execution plan of record. | P0 | Medium | Small | none | `[GOV]` |
| T1.16 | **Reconcile the "Animation style" frozen rule.** It says `rojanEnterAnimation` (fade + upward motion, `index*60` stagger) is the only entrance mechanism. It is currently a no-op (`= this`). Update to reflect Phase 2's motion system as the new source of truth. | P0 | Medium | Small | none | `[GOV]` |
| T1.17 | **Superseded-doc sweep.** Mark the Persian `docs/architecture/` set and any `WarmBackground`/light-mode references in `docs/` as historical; update the design-reference index. | P1 | Medium | Small | T1.13–T1.16 | `[SAFE]` |
| T1.18 | **Establish a "baseline change" protocol** in `CLAUDE.md`: any frozen-baseline change requires a dated entry (what/why/approver) in a `docs/uiux/BASELINE_CHANGELOG.md`, so drift can't silently recur. | P1 | Medium | Small | none | `[GOV]` |

**Phase-1 docs DoD:** `CLAUDE.md` frozen sections match the shipped product or carry a dated ratification; `BASELINE_CHANGELOG.md` exists; this roadmap is referenced as the execution plan; no contradiction remains between the source of truth and the code.

---

## PHASE 2 — Premium Motion System

Goal: restore and elevate motion to "AI product that feels alive." All motion is **token-driven** (durations, easings, travel distances) and **reduced-motion aware from day one**.

Foundational task for the whole phase:

| ID | Task | Priority | Impact | Effort | Dependencies | Tag |
|---|---|---|---|---|---|---|
| T2.0 | **Motion token layer + reduced-motion gate.** One `RojanMotion` object: duration tiers (instant 0 / micro 120 / short 200 / medium 320 / reveal 450 / page 280), standard easings, stagger step. One `rememberReducedMotion()` reading `Settings.Global.ANIMATOR_DURATION_SCALE` (+ a manual app toggle). Every motion primitive below consults it: reduced → cross-fade only, no travel/scale/loop. | P0 | High | Medium | none | `[SAFE]` |

### 2.1–2.7 Motion tasks

| ID | Task | Current issue | Premium target | Approach | Priority | Impact | Effort | Deps | Risk |
|---|---|---|---|---|---|---|---|---|---|
| T2.1 | **Page transitions** | Customer nav = fade + `scaleIn 0.97`; Manager nav = none (snap). | Unified shared spatial transition: forward = subtle slide-from-leading + fade (RTL-correct: new screen enters from the left); back = reverse; modal = scale + fade. | Extract `RojanNavTransitions` from `RojanAnimations.PageEnter/Exit`; make RTL-aware; apply to `managerNavGraph` (currently bare `composable{}`). | P1 | High | Medium | T2.0 | Nav-graph regressions; test back-stack + predictive-back. |
| T2.2 | **Glass reveal animation** | Glass surfaces just appear. | On first appearance: border "draws"/brightens over ~300ms, fill fades 0→target, highlight sweeps once. Hero surfaces only; chips get a plain fade. | New `Modifier.glassReveal()` consumed by `PremiumGlassSurface` via an opt-in flag; gated by reduced-motion and by "first composition only" (fix the old scroll-recycle replay bug by keying on lazy-item key, not local `remember`). | P1 | High | Medium | T2.0 | Perf on low-end devices; must not replay on scroll. |
| T2.3 | **Card entrance choreography** | `rojanEnterAnimation` is a no-op; ~20 dead call sites. | Restore fade + short upward travel (`RojanMotion.reveal`), staggered `index * step` within a section, **once per screen entry** (not per scroll-recycle). Sections stagger; items within a visible list do not re-animate on scroll-back. | Reimplement `rojanEnterAnimation` properly with a scroll-safe "has appeared" latch tied to the lazy key; keep the existing parameter surface so no call site changes. Remove the now-unnecessary `LaunchedEffect{visible=true}` scaffolding. | P0 | High | Medium | T2.0 | The exact bug that caused it to be gutted — must be provably scroll-safe. |
| T2.4 | **AI glow effects** | `RojanAIGlow` renders at 0.05 alpha — the "AI active" signal is invisible. | A visible, restrained "intelligence" cue: a slow breathing glow (≤0.12 alpha, 3–4s) behind AI surfaces (AI Insight card, AI search entry, Beauty DNA result); a one-shot "thinking → revealed" pulse when AI content loads. Never loops when off-screen or reduced-motion. | `RojanAIGlow` modifier with intensity tiers (from T1.10); visibility-gated infinite transition; single-shot variant for content arrival. | P1 | High | Medium | T2.0, T1.10 | Overuse cheapens it — restrict to genuinely AI-driven surfaces; document the rule. |
| T2.5 | **Success animations** | `RojanSuccessCheckmark` exists (good) but is used in few places; booking success screens vary. | Standardize the confirmation moment: ring overshoot → check → subtle particle/glow bloom in brand colors → content settle. One component, used by every "done" state (booking confirmed, salon setup saved, service added, review posted). | Extend `RojanSuccessCheckmark` with an optional bloom layer; adopt in `BookingSuccessScreen`, `ManagerBookingSuccessScreen`, salon setup, service/staff edit. | P1 | Medium | Small | T2.0 | Low. |
| T2.6 | **Loading states** | Manager shows zeros/empty text; `RojanShimmer`/`RojanSkeletonBox` exist but Manager doesn't use them; Customer does. | Skeletons everywhere a real fetch precedes content: Manager KPI grid, Salon Identity, Calendar preview/day/week, Services/Staff/Customers lists. Skeleton shape mirrors the real card. Distinguish loading ≠ empty ≠ error. Add a branded indeterminate bar (`PremiumLoadingBar` tokens already exist). | Adopt `RojanSkeletonBox` in Manager; add a `ManagerSkeletonCard`; wire real View-state (`Loading/Content/Empty/Error`) into Manager screens. | P0 | High | Medium | T2.0 | Requires Manager screens to expose a real load state (some just read singletons). |
| T2.7 | **Reduced-motion support** | None anywhere; infinite rotations (`HomeMetallicRing`, `GlassOrb`) unconditional. | Every animation degrades gracefully; infinite decorative loops stop entirely; a visible in-app "reduce motion" setting in Profile/Settings. | The `rememberReducedMotion()` gate from T2.0 applied to `HomeMetallicRing`, `GlassOrb`, `premiumMetallicBorder` sparkle/glow passes, page transitions, all `rojanPressable`/reveal motion. | P0 | High | Medium | T2.0 | Must be genuinely exhaustive — audit every `infiniteRepeatable`/`animate*`. |

### 2.8 Interaction feedback (folded into Phase 2)

| ID | Task | Priority | Impact | Effort | Deps | Tag |
|---|---|---|---|---|---|---|
| T2.8 | **Fix `rojanPressable`.** Change press-scale from **up (1.06)** to **down (~0.97)** — "pressed = pushed in," and eliminates the overlap risk in tight chip rows. Add a visible **focus state** (the primitive currently sets `indication = null`, so there is no focus ring anywhere). Reconcile with the ripple-based feedback on `CustomerBottomBar` tabs — one feedback language. | P0 | High | Small | T2.0 | `[GOV]` (frozen "Animation style" — press scale value) |

**Phase-2 DoD:** no dead animation code; every screen has entrance choreography that is provably scroll-safe; Manager has skeletons; reduced-motion fully honored with an in-app toggle; press feedback compresses and has a focus state; `CLAUDE.md` animation rule updated (T1.16). Perf: no dropped frames on the reference mid-range device for dashboard scroll.

---

## PHASE 3 — Premium Component Upgrade

Goal: finish the "Shared Premium Glass Design System" Phases 2–6 that `CLAUDE.md` lists as unbuilt, plus fix the component-level audit findings. **Manager remains the master reference; only the palette differs per app.**

### 3.1 GlassSurface / PremiumGlassSurface

- **Current issue:** Not true backdrop-blur (translucent fill + ornamental border only); on the dark canvases a 14% white fill gives almost no "frost" — the border does ~90% of the work. Low tonal lift of cards off the background (AMOLED risk). Full metallic ornament (7 glow passes + 2 sparkle stars) painted even on 64dp chips.
- **Premium target:** Cards read as glass through material, not just edge; visible tonal separation from the canvas; ornament scaled to surface size.
- **Implementation approach:** (a) Add a real backdrop blur option (`RenderEffect`/`Modifier.blur` on a captured backdrop) for hero surfaces, gated by API level + perf tier, plain fill fallback below that. (b) Nudge the per-palette fill/highlight so cards lift ~4–6% luminance off the canvas without new colors. (c) Introduce the `compact` metallic border variant (T1.9) and auto-select it below a size threshold inside `PremiumGlassSurface`.
- **Risk:** Backdrop blur is GPU-expensive and historically flaky on some Android GPUs — must be perf-gated and A/B-verified; fill changes touch every card in both apps (screenshot-test heavy). **Medium-High risk.**
- **Priority:** P1 · **Impact:** High · **Effort:** Large · **Deps:** T1.9, T1.10

### 3.2 GlassCard (the layered card shell)

- **Current issue:** Three parallels — `RojanHomeCard` (light, legacy), `HomeCard` (dark, live), Manager's hand-rolled card bodies. `CLAUDE.md` Phase 4.
- **Premium target:** One `RojanCard` shell: accent-blur layer → `PremiumGlassSurface` → content slot → entrance stagger, palette-driven.
- **Approach:** Promote `HomeCard`'s construction to a shared `RojanCard` in `ui/components/cards`; bind palette via `LocalRojanPalette`; retrofit Manager's `StatCard`/`SalonIdentityCard`/`AIInsightCard`/`AppointmentRow` onto it; delete `RojanHomeCard` (dead).
- **Risk:** Wide blast radius across both apps; Manager cards have bespoke internal layouts. **Medium risk.**
- **Priority:** P1 · **Impact:** High · **Effort:** Large · **Deps:** 3.1, T2.3

### 3.3 Buttons

- **Current issue:** Customer `PremiumButton` (bold purple→magenta gradient pill, but fragile API — default `modifier` carries the size, lost if caller overrides). Manager `ManagerPrimaryButton` (faint glass pill, Gold text ≈4.5:1, no loading state). Two different *kinds* of primary affordance. `CLAUDE.md` Phase 3.
- **Premium target:** One `PremiumButton` with palette-supplied fill mode (`Gradient` | `Glass` | `Outline`), a real filled primary in each app's own colors, states: default/pressed(compress)/loading(spinner, no size jump)/disabled(distinct, not just dim)/focus.
- **Approach:** New shared `PremiumButton` in `ui/components/buttons`; `fillMode` from palette; size via explicit params not a defaulted modifier; Manager gets a `Gradient` primary in Teal→Gold; migrate `ManagerPrimaryButton` call sites (5 booking screens + auth) and Customer call sites.
- **Risk:** Primary CTA is the most-tested surface; Manager's booking flow depends on it. **Medium risk.**
- **Priority:** P0 (Manager CTA weakness is pilot-blocking) · **Impact:** High · **Effort:** Medium · **Deps:** T2.0, T2.8

### 3.4 Search bars

- **Current issue:** Home `AISearchBar` is a shallow tap-to-navigate stub with a filter glyph that does the same thing (misleading); placeholder color ≈4.0:1 on navy. The real `SearchScreen` is decent (debounce, skeleton, pagination, `withDirectionFor`) but has no suggestions/history/NL affordance.
- **Premium target:** The AI search entry *looks* intelligent: animated prompt hints ("سالن رنگ مو نزدیک من"), recent searches, a subtle AI glow; the results screen adds suggestion chips and (when backend supports) semantic results.
- **Approach:** Rebuild `AISearchBar` as a presentational component with rotating placeholder + AI glow (T2.4); on `SearchScreen` add a recent-searches store (local) and suggestion chips; separate the filter action into a real filter sheet or remove the glyph. NL/semantic search itself is `[S1]` — spec only, flag for backend.
- **Risk:** Low for the visual layer; NL search is backend-gated. **Low risk.**
- **Priority:** P1 · **Impact:** High · **Effort:** Medium · **Deps:** T2.4, Phase 6

### 3.5 Bottom navigation

- **Current issue:** Icon-only, no text labels (discoverability/first-run cost). Center Home = perpetually rotating gold ring (unconditional motion). Feedback uses ripple while the rest of the app uses `rojanPressable`.
- **Premium target:** Icon + short label (or label-on-active), a refined active indicator, motion that rests, consistent feedback.
- **Approach:** Add labels back (Caption, active-tinted); replace the infinite ring rotation with a one-shot shimmer on tab-change + a static metallic ring (or reduced-motion-gated slow rotation); unify feedback on `rojanPressable` with focus state.
- **Risk:** Layout at narrow widths (the reason labels were removed) — needs `heightIn` + marquee-free truncation testing. **Low-Medium risk.**
- **Priority:** P1 · **Impact:** Medium · **Effort:** Medium · **Deps:** T2.7, T2.8

### 3.6 Dashboard cards (Manager)

- **Current issue:** Low card/background separation; KPI cards, Salon Identity, AI Insight all same visual weight; no skeletons; redundant brand stack in `ManagerHeader` + a dead notification bell.
- **Premium target:** Clear hierarchy (Salon Identity hero > KPI grid > AI Insight accent > supporting), cards that lift off the canvas, real loading states, a lean header.
- **Approach:** Adopt `RojanCard` (3.2); apply the fill/tonal nudge (3.1); give AI Insight a distinct accent + glow (T2.4); skeletons (T2.6); collapse `ManagerHeader` to logo + greeting + (real or removed) bell; if the bell stays, wire a notifications destination `[S1]` or remove it.
- **Risk:** `CLAUDE.md` freezes the Manager Dashboard layout/section order/weighting — **layout order is preserved**; only surface treatment + header change. Still `[GOV]`. **Medium risk.**
- **Priority:** P1 · **Impact:** High · **Effort:** Medium · **Deps:** 3.1, 3.2, T2.4, T2.6

### 3.7 Empty states

- **Current issue:** Manager uses bare `Text` ("امروز نوبتی ثبت نشده است."); `RojanEmptyState` exists and is used well in Customer.
- **Premium target:** Every empty state = icon/illustration + Persian headline + one-line guidance + (where useful) a primary action, in the glass language.
- **Approach:** Adopt `RojanEmptyState` across Manager (calendar day/week, services, staff, customers, dashboard preview); add brand-appropriate line illustrations for the top ~5.
- **Risk:** Low. · **Priority:** P1 · **Impact:** Medium · **Effort:** Small · **Deps:** none

### 3.8 Error states

- **Current issue:** `ManagerDashboardScreen` swallows `initialize()` failure silently — zeros are indistinguishable from a failed load. `RojanErrorState` exists (Customer uses it).
- **Premium target:** Every fetch surfaces failure with an inline, retryable `RojanErrorState`; no silent zeros.
- **Approach:** Introduce a real `Loading/Content/Empty/Error` state to Manager dashboard + calendar + list screens; wire `RojanErrorState` with retry; keep offline-tolerant partial rendering where sensible.
- **Risk:** Touches Manager data plumbing (currently reads singletons directly). **Medium risk.** · **Priority:** P0 · **Impact:** High · **Effort:** Medium · **Deps:** T2.6

### 3.9 AI widgets

- **Current issue:** AI Insight card visually identical to every other card; no "why", no confidence, no primary action on the recommendation; `RojanAIGlow` invisible.
- **Premium target:** A recognizable "AI surface" pattern: distinct accent + breathing glow, an "AI" chip, the insight + a short rationale, a primary action, a reveal beat on load.
- **Approach:** New `RojanAIInsightCard` / `RojanAISurface` pattern component; adopt in Manager AI Insight, Beauty DNA result, smart recommendation surfaces; consumes T2.4 glow. Content/rationale from backend `[S1]` where not already present.
- **Risk:** Restraint — must not turn every surface into "AI". Document the eligibility rule. **Low-Medium risk.** · **Priority:** P1 · **Impact:** High · **Effort:** Medium · **Deps:** T2.4, Phase 4

**Phase-3 DoD:** one shared card, one shared button, one glass mechanic with size-aware ornament and optional true blur; Manager CTA has real weight; every empty/error/loading state uses the shared primitives; an "AI surface" pattern exists; `RojanHomeCard` and dead light-mode wrappers deleted; screenshot suite green for both apps; `CLAUDE.md` glass-phase status updated.

---

## PHASE 4 — AI Experience Layer

Goal: make ROJAN *look and feel* intelligent — the BeautyOS promise. Visual/interaction layer here; any new data or model work is `[S1]` and flagged spec-only.

| ID | Feature | Current | Premium target | Approach (client) | Priority | Impact | Effort | Deps | Tag |
|---|---|---|---|---|---|---|---|---|---|
| T4.1 | **AI Insight (Manager)** | Real rule-engine text in a plain card. | A living insight surface: category icon, headline, "چرا این پیشنهاد؟" expandable rationale, confidence/impact hint, primary action ("مشاهده مشتریان", "ارسال یادآوری"), reveal beat. Rotating multi-insight carousel. | `RojanAIInsightCard` (3.9); consume existing `ManagerCrmInsightCategory` + `topRecommendationMessage`; rationale/confidence fields `[S1]`. | P1 | High | Medium | 3.9, T2.4 | `[S1]` for new fields |
| T4.2 | **Beauty DNA visualization** | `BeautyDnaScreen` is preference capture (chips); `BeautyTimelineScreen` exists. No visual synthesis. | A "Beauty DNA" visual identity: a generative, brand-colored radial/petal signature derived from the profile (hair/skin/nails), shown on the profile header and Beauty DNA screen; animates on first reveal; updates as the profile changes. | New `BeautyDnaSignature` Canvas component, deterministic from profile values; palette from brand tokens; reduced-motion → static. Purely client-side from existing `BeautyProfileRepository`. | P1 | High | Large | T2.0, Phase 6 | `[SAFE]` |
| T4.3 | **Smart recommendations (Customer)** | Static "پیشنهاد شده" section titles; no personalization surfaced. | "برای شما" rails with a short reason per item ("چون رنگ مو را دنبال می‌کنید"), an AI glow on the rail header, subtle confidence ordering. | Presentational rail + reason chip; recommendation logic + reasons `[S1]`. Client can ship the pattern with rule-based reasons first. | P1 | High | Medium | 3.9, Phase 6 | `[S1]` |
| T4.4 | **Predictive booking** | Booking flow has no AI assist despite server-side data. | In the date/time step: "بهترین زمان پیشنهادی" highlighted slots, "معمولاً این متخصص را انتخاب می‌کنید", est. wait/availability confidence. | Client renders highlighted slots + hint chips from a new suggestions field `[S1]`; falls back to no-hint cleanly. | P1 | High | Medium | Phase 6/7 booking, 3.9 | `[S1]` |
| T4.5 | **Salon analytics intelligence (Manager)** | KPI grid = raw numbers. | Each KPI gets a trend sparkline + a plain-language delta ("۱۲٪ بیشتر از هفته گذشته") + an occasional AI callout ("سه‌شنبه‌ها ظرفیت خالی دارید"). | Sparkline component (chart tokens exist in the system's chart guidance); deltas need historical data `[S1]`; callouts reuse T4.1. | P1 | High | Large | T4.1, Phase 7 | `[S1]` |
| T4.6 | **Personalized customer experience** | Home is the same for everyone. | Home adapts: greeting by time/name, "ادامه رزرو" if mid-flow, seasonal/next-service nudges, Beauty DNA signature in the header. | Client-side personalization from existing session/booking-history data; deeper personalization `[S1]`. | P2 | Medium | Medium | T4.2, Phase 6 | partial `[S1]` |
| T4.7 | **AI presence system** | `RojanAIGlow` at 0.05 alpha, no consistent "AI is here" cue. | A consistent visual vocabulary for AI across both apps: the glow (T2.4), an "AI" chip/badge, a shared reveal motion, a restraint rule (only genuinely AI-driven surfaces). Documented. | Spec doc + the shared `RojanAISurface` primitive; adoption checklist. | P1 | High | Small | T2.4, 3.9 | `[SAFE]` |

**Phase-4 DoD:** an "AI surface" vocabulary exists and is documented with an eligibility rule; AI Insight, Beauty DNA, and at least one recommendation surface use it; predictive-booking + analytics-intelligence client scaffolding is in place with graceful no-data fallback; every backend dependency is itemized in a spec for System 1.

---

## PHASE 5 — RTL + Accessibility

Goal: close the audit's P0/P1 accessibility gaps and complete the *spatial* RTL story (text direction is already strong).

| ID | Task | Current issue | Target | Approach | Priority | Impact | Effort | Deps | Tag |
|---|---|---|---|---|---|---|---|---|---|
| T5.1 | **Layout direction — horizontal lists** | Every `LazyRow` (carousels, specialist filter, day selector, weekly overview, quick actions) originates and scrolls left→right. | Horizontal lists originate at the right, scroll leftward; snap/paging RTL-correct. | Scoped RTL wrapper for horizontal lists (the way `RtlLayoutKit` scopes rows) — `reverseLayout` + start-alignment, applied per component, not app-wide `LayoutDirection.Rtl` (which the project deliberately rejected for photo/back-button reasons). | P1 | High | Medium | none | `[SAFE]` |
| T5.2 | **Manager icon/text rows** | `SalonIdentityCard`, `AppointmentRow`, `UpcomingSlotRow`, `AIInsightCard`, `CalendarPreviewSection` build icon-left/text-right (physical LTR). | Leading element (icon) on the right. | Migrate onto `RtlInfoRow` / `RtlListRow` (already built for exactly this) or apply the same explicit-composition pattern. | P1 | Medium | Medium | none | `[SAFE]` |
| T5.3 | **Forms / text fields** | `ManagerTextField` (OTP) doesn't apply `withDirectionFor`; raw `OutlinedTextField` label falls back to Material/locale; OTP code is masked (`NumberPassword`); no `imePadding`. | All fields use `withDirectionFor(value)`; labels/placeholders in `RojanTypography` + correct direction; OTP = unmasked segmented input; keyboard never covers the field. | Build a shared `RojanTextField` + `RojanOtpField` (glass language); add `imePadding()` + scroll to the auth scaffolds. | P1 | High | Medium | 3.3 | `[SAFE]` |
| T5.4 | **Touch targets** | Specialist/toggle chips, AI-insight inactive line, calendar-preview CTA row < 48dp. | Every interactive element ≥ 48×48dp. | Apply `MinTouchTarget` via `heightIn`/`sizeIn` + `Modifier.minimumInteractiveComponentSize()`; audit sweep across both apps. | P1 | High | Small | none | `[SAFE]` |
| T5.5 | **Contrast** | Error text on light ≈2.9:1 (legacy light — mostly dead now); `HomeColors.TextMuted` on navy ≈4.0:1; Gold CTA label ≈4.5:1; glass fill barely separates from canvas. | All text ≥ 4.5:1 (≥3:1 large); UI boundaries ≥ 3:1. | Adjust `HomeColors.TextMuted`, the Manager Gold-on-glass CTA (→ filled button in 3.3 largely resolves it), and the glass fill nudge (3.1) — all within existing hues. Part of the T1.7 token report; changes gated behind token-cleanup authorization. | P0 | High | Medium | T1.7, 3.1, 3.3 | `[GOV]` |
| T5.6 | **Focus states** | `rojanPressable` uses `indication = null` — no visible focus anywhere. | Every interactive element has a visible focus indicator (keyboard, D-pad, switch access). | Done in T2.8 (add focus ring to `rojanPressable`); verify with hardware keyboard + TalkBack + Switch Access across key flows. | P0 | High | Small | T2.8 | `[SAFE]` |
| T5.7 | **Screen readers** | Duplicate announcements (icon `contentDescription` = the visible label right next to it); dead bell announces "اعلان‌ها"; no heading semantics; some decorative icons carry descriptions. | Clean semantics: decorative → `null`; labelled controls announce once; sections are headings; live regions for async content (search count, booking status). | Semantics audit sweep; `heading()` on `RtlSectionHeader` (T1.3); `contentDescription = null` where a visible label exists; `liveRegion` on result counts/status; remove or wire the bell. | P1 | High | Medium | T1.3 | `[SAFE]` |
| T5.8 | **Reduced motion** | None. | Full support + in-app toggle. | Delivered in T2.0/T2.7; Phase 5 verifies coverage (every `infiniteRepeatable`, page transition, press/reveal) and adds the Settings toggle UI. | P0 | High | Small | T2.7 | `[SAFE]` |
| T5.9 | **Back button placement** | Pinned `Alignment.TopStart` (physical left) in an RTL app; also overlaps the first content row (`ManagerScaffold`/`RojanScaffold` overlay with no top inset). | Back affordance on the trailing (right) edge per platform RTL; scaffold reserves top inset so it never overlaps content. | Update both scaffolds: mirror position, add conditional top content-padding when `onBackClick != null`. `[GOV]` — `RtlLayoutKit` comment documents the deliberate left placement; this needs sign-off. | P1 | High | Small | none | `[GOV]` |
| T5.10 | **Dynamic type** | Fixed-height containers (`HeroHeight` 360, day cells 72, weekly 220) clip text at large font scale. | Content reflows; no clipping at max system font size. | Convert fixed heights to `heightIn(min=)`; test at 200% font scale; part of T1.4. | P2 | Medium | Medium | T1.4 | `[SAFE]` |

**Phase-5 DoD:** WCAG 2.2 AA pass on the audited flows (contrast, targets, focus, SR, motion); horizontal lists and Manager rows are RTL-correct; a shared `RojanTextField`/`RojanOtpField` exists; back button mirrored + non-overlapping; verified with TalkBack + hardware keyboard + Switch Access on device.

---

## PHASE 6 — Customer Experience

Goal: raise the Customer app from "rich but flat" to premium boutique. Builds on Phases 1–5.

| Area | Current issue | Premium improvement | Priority | Impact | Effort | Deps |
|---|---|---|---|---|---|---|
| **Home** | 13 sections in one `LazyColumn` at 16dp, weak sectioning, inconsistent headers, redundant with `CustomerDashboardScreen` (same shell). | Editorial rhythm: fewer, stronger sections; section-to-section spacing > card spacing (T1.4/tokens); consistent `RtlSectionHeader` + "مشاهده همه"; a personalized hero (greeting + Beauty DNA signature + "ادامه رزرو"); entrance choreography (T2.3). Clarify Home vs Dashboard roles. | P1 | High | Large | Phase 2, T4.2, T4.6 |
| **Search** | Shallow `AISearchBar` entry; filter glyph misleads; real `SearchScreen` lacks suggestions/history. | Animated AI prompt hints + glow on the entry; recent searches + suggestion chips on the results screen; a real filter sheet; results with richer cards (once backend gives rating/distance `[S1]`). | P1 | High | Medium | 3.4, T2.4 |
| **Salon discovery** | `SalonListScreen`/`SearchScreen` share a VM; deterministic color seeds stand in for salon branding; no rating/distance/reviews (`[S1]` gap, disclosed). | Premium salon cards (hero image treatment, glass overlay, rating/distance when available), map/list toggle, "پیشنهاد رویان" reasoning chips, skeletons already good. | P1 | High | Medium | 3.2, Phase 2 |
| **Booking journey** | Multi-step wizard, `HomeBackgroundTheme` applied; success screens vary; no AI assist. | Consistent stepper with progress; predictive slot highlighting + specialist hint (T4.4); a unified premium success moment (T2.5); review step with clear edit affordances; RTL-correct step transitions (T2.1). | P0 | High | Large | T2.1, T2.5, T4.4, 3.3 |
| **Profile** | ~15 screens (Wallet, Loyalty, Membership, Reviews, Coupons, Beauty DNA/Timeline, Appointments, Favorites, Followed, Waitlist) all on `HomeBackgroundTheme`; quality varies; some session-only data disclosed honestly. | Profile header with Beauty DNA signature + tier; consistent list-row component (`RtlListRow`) and section grouping; premium states for each; consolidate near-duplicate screens where sensible. | P1 | Medium | Large | 3.2, 3.7, T4.2 |
| **Loyalty** | `LoyaltyScreen` + `MembershipScreen` + `WalletScreen` + `CouponsScreen` exist separately. | A unified "Rewards" hub: tier progress ring (brand-colored, animates), points, perks, redeemable coupons in one place; celebratory motion on tier-up (T2.5); clear earn/spend history. Backend model `[S1]` where not present. | P1 | High | Medium | 3.2, T2.5, T4.7 |

**Phase-6 DoD:** Home reads as an editorial premium surface with personalization; booking journey has a consistent stepper + premium success + AI assist scaffolding; a unified Rewards hub exists; all Customer screens use the shared card/row/state components; Customer-experience maturity ≥ 8.5.

---

## PHASE 7 — Manager Experience

Goal: enterprise-grade Manager app (BeautyOS back-of-house). Note: **Revenue, Inventory, and Reports screens do not exist today** — those are net-new and require System 1 scope confirmation for data.

| Area | Current state | Enterprise-grade improvement | Priority | Impact | Effort | Deps | Tag |
|---|---|---|---|---|---|---|---|
| **Dashboard** | Correct section order, real data, coherent dark-emerald theme; low card lift, redundant header, dead bell, no skeleton/error states, undistinguished AI card. | 3.6 (card lift + hierarchy) + 3.8 (error states) + T2.6 (skeletons) + T4.1 (living AI insight) + T4.5 (KPI sparklines + deltas) + lean header. Add pull-to-refresh. Keep the frozen layout order. | P1 | High | Medium | 3.6, 3.8, T2.6, T4.1, T4.5 | `[GOV]` |
| **Calendar** | Daily/weekly, real appointments, status dots (color + label — good), specialist filter; back button overlaps "تقویم" title; `LazyRow` day selector not RTL; no month view; no drag/reschedule. | Fix back-overlap (T5.9) + RTL day selector (T5.1); add a month overview; time-grid day view with proportional blocks; inline status change; premium empty/loading (3.7/T2.6); appointment detail as a polished sheet. Reschedule/drag `[S1]` for conflict rules. | P1 | High | Large | T5.1, T5.9, 3.7, T2.6 | partial `[S1]` |
| **Revenue** | **No screen.** `todaysRevenueLabel` exists on the dashboard only. | New Revenue screen: today/week/month/custom range, trend chart, breakdown by service/specialist, comparison deltas, target progress. Chart types per the design system's chart guidance (accessible palette, legends, tooltips). | P1 | High | Large | Phase 2/3, chart tokens | `[S1]` (revenue aggregation endpoints) |
| **Services** | `ManagerServicesScreen` + edit exist (backend-wired). | Premium list (category grouping, price/duration, active toggle), bulk actions, premium edit form (shared `RojanTextField`), image handling, empty/error states. | P2 | Medium | Medium | 3.3, 3.7, T5.3 | `[SAFE]` mostly |
| **Inventory** | **No screen, no domain model.** | New module: product list, stock levels, low-stock AI alerts (T4.1 pattern), usage-vs-service linkage, reorder flow. Fully net-new. | P2 | Medium | Large | Phase 3/4 | `[S1]` (entire inventory domain + endpoints) |
| **Reports** | **No screen.** | New Reports hub: occupancy, retention, no-show rate, specialist performance, customer cohort insights; exportable; AI-narrated summary ("این ماه نرخ عدم حضور ۸٪ بود، بیشتر سه‌شنبه‌ها"). | P2 | High | Large | Phase 4, chart tokens | `[S1]` (analytics endpoints) |
| **Staff / Customers** | Screens exist, backend-wired. | Consistent premium list + profile + edit (shared components); staff schedule/availability view; customer profile with Beauty DNA + visit history + AI tags (VIP/inactive already modeled). | P2 | Medium | Medium | 3.2, 3.7, T5.3 | partial `[S1]` |
| **OTP / Auth** | Raw `OutlinedTextField`, masked code, no loading state, breaks glass language, no `imePadding`. | Shared `RojanOtpField` (segmented, unmasked, SMS autofill), glass-consistent, button loading state, IME-safe, vertically centered. | P0 | Medium | Small | T5.3, 3.3 | `[SAFE]` |

**Phase-7 DoD:** Manager dashboard + calendar are enterprise-grade (states, skeletons, AI, sparklines); OTP is premium; Revenue exists (P1); Services polished; Inventory + Reports have approved specs + client scaffolding (data `[S1]`); Manager-experience maturity ≥ 8.5; frozen dashboard layout order preserved.

---

## FINAL OUTPUT

### 1. Current maturity score

**5.6 / 10** — "Strong bones, unfinished skin." Excellent design-system architecture and a genuinely good Persian text engine, held back by: no bundled Persian font, a gutted motion system, `CLAUDE.md` frozen baselines that contradict the shipped product, accessibility gaps (contrast, focus, targets, reduced-motion), incomplete spatial RTL, and four unbuilt component-unification phases.

### 2. Target maturity score

**8.5 / 10** — "Premium AI-native BeautyOS, pilot-ready." Real Persian typography, a living motion system with reduced-motion support, one shared card/button/glass system, a documented AI-surface vocabulary, WCAG 2.2 AA on core flows, complete spatial RTL, and documentation that matches reality. Ceiling with full System 1 data support: ~9.2.

### 3. Recommended execution order

```
1. Phase 1.3  Documentation Alignment      ← do FIRST; unblocks governance, cheap, stops drift
2. Phase 1.1  Typography (Persian font)    ← everything visual depends on final metrics
3. Phase 1.2  Token Cleanup (report only)  ← parallel with typography
4. Phase 2    Motion System                ← foundational; T2.0 gate blocks 2.1–2.8
5. Phase 3    Component Upgrade            ← needs stable type + motion
6. Phase 5    RTL + Accessibility          ← overlaps Phase 3 (shares button/field/pressable work)
7. Phase 4    AI Experience Layer          ← needs the AI-surface component from Phase 3
8. Phase 6    Customer Experience          ← needs Phases 2–5
9. Phase 7    Manager Experience           ← needs Phases 2–5; Revenue/Inventory/Reports gated on S1
```

Rationale: docs first (near-zero cost, removes Tier-0 risk, prevents re-drift). Typography before any pixel work. Motion and components before the experience phases. Accessibility interleaves with components because they share primitives (`rojanPressable`, buttons, fields). AI layer after its host component exists. Experience phases last.

### 4. Sprint breakdown (2-week sprints, ~1–2 UI engineers)

| Sprint | Focus | Key deliverables | Exit criteria |
|---|---|---|---|
| **S1** | Docs + Typography kickoff | T1.13–T1.18 (all doc reconciliation, `BASELINE_CHANGELOG.md`); T1.1 (font integration); T1.2 (weight system) | `CLAUDE.md` matches reality; Persian font on device in all 3 flavours |
| **S2** | Typography finish + Token report | T1.3, T1.4, T1.5, T1.6; T1.7–T1.12 (Token Reconciliation Report — analysis only) | Type hierarchy retuned for Persian; token report signed off; no token values changed |
| **S3** | Motion foundation | T2.0 (motion tokens + reduced-motion gate); T2.3 (card entrance, scroll-safe); T2.8 (fix pressable + focus); T2.1 (page transitions incl. Manager) | Nothing animates without a reduced-motion path; entrance choreography provably scroll-safe; Manager screens transition |
| **S4** | Motion finish + states | T2.2 (glass reveal); T2.4 (AI glow); T2.5 (success); T2.6 (Manager skeletons); T2.7/T5.8 (reduced-motion coverage + toggle) | Manager has skeletons; AI glow visible + restrained; `CLAUDE.md` animation rule updated |
| **S5** | Core components I | 3.3 (unified `PremiumButton` + Manager filled CTA); 3.1 (glass fill/tonal nudge + compact border; blur spike); T5.3 (`RojanTextField`/`RojanOtpField`) | One button system; Manager CTA has real weight; shared field components exist |
| **S6** | Core components II | 3.2 (`RojanCard` + retrofit); 3.7 (Manager empty states); 3.8 (Manager error states + real load state) | One card system; `RojanHomeCard` deleted; no silent-zero dashboards |
| **S7** | Accessibility + RTL | T5.1 (horizontal-list RTL wrapper); T5.2 (Manager rows); T5.4 (touch targets); T5.5 (contrast, gated on token auth); T5.7 (semantics); T5.9 (back button) | WCAG 2.2 AA on audited flows; horizontal lists RTL-correct; verified on device w/ TalkBack + keyboard |
| **S8** | AI Experience | 3.9 + T4.7 (`RojanAISurface` + vocabulary doc); T4.1 (living AI Insight); T4.2 (Beauty DNA signature — start); T4.5 (KPI sparklines — start) | AI-surface pattern documented + adopted in ≥2 places; backend-dependency spec handed to S1 |
| **S9** | Customer Experience | Home editorial rebuild; Search AI entry; booking stepper + success + predictive scaffolding; Rewards hub | Customer maturity ≥ 8.5 in internal review |
| **S10** | Manager Experience | Dashboard polish; Calendar (month view, RTL, states); OTP premium; Revenue screen (P1); Services polish | Manager maturity ≥ 8.5; Revenue live; Inventory/Reports specs approved |
| **S11** | Hardening | Perf pass (dashboard scroll, glass blur, motion on mid-range device); screenshot-test refresh; a11y regression; dynamic-type pass | Target overall ≥ 8.5; DoD checklist green |

~22 weeks / ~5.5 months at 1–2 engineers. Phases 4/6/7 backend items (`[S1]`) run in parallel on the System 1 track and may extend S9–S11.

### 5. Risk assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| **Card-entrance motion reintroduces the scroll-recycle replay bug** (the reason it was gutted) | Medium | High | Latch on the lazy-list item *key*, not local `remember`; explicit scroll-up/down test in the screenshot/instrumented suite before merge. |
| **Glass fill/tonal changes regress every card in both apps** | High | Medium | Screenshot-test-gated; roll out behind a flag; per-palette review; keep changes within existing hues. |
| **True backdrop blur is slow / GPU-flaky on some Android devices** | Medium | Medium | Perf-tier + API gate; plain-fill fallback; A/B on device matrix; hero surfaces only. |
| **Token cleanup deletes something still referenced** | Medium | High | Report + explicit "safe to delete" list gated behind its own authorization; delete in a dedicated PR with full-build + test verification; never fold into a feature PR. |
| **Frozen-baseline changes (type scale, back button, dashboard surface) breach `CLAUDE.md` governance** | High (by design) | Medium | Each `[GOV]` task gets an explicit dated ratification in `BASELINE_CHANGELOG.md` + owner sign-off *before* code; STOP-and-report on any conflict. |
| **AI-surface pattern gets overused → "AI" loses meaning** | Medium | Medium | Written eligibility rule ("only surfaces driven by a real model/rule engine"); adoption checklist; design review gate. |
| **Persian font increases APK size / first-frame cost** | Medium | Low | Variable font (single file), subsetted to Persian+Latin; `Font(..., FontLoadingStrategy)` async; measure cold-start. |
| **Phase 4/6/7 AI + Revenue/Inventory/Reports blocked on System 1** | High | Medium | Client ships pattern + graceful no-data fallback first; every backend need itemized in a spec at S8; scope-confirm early. |
| **RTL horizontal-list wrapper interacts badly with paging/snap** | Medium | Medium | Prototype on one carousel first (specialist filter); expand only after verification. |
| **`rojanPressable` scale-direction flip (up→down) is a frozen-rule change felt app-wide** | Medium | Low | `[GOV]` ratification; it's a strict UX improvement; screenshot diffs are motion-only. |

### 6. Components affected

**Shared design system:** `RojanTypography`, `Type.kt`, `RojanTokens` (report only in v1), `RojanDimens`, `Shadows`, `Gradients`, `RojanAnimations`, `rojanPressable`, `rojanEnterAnimation`, `PremiumGlassSurface`, `premiumMetallicBorder`, `GlassSurface`, `RojanIconContainer`, `RojanText`/`RtlLayoutKit`, `RojanScaffold`, `PremiumButton`, `RojanSuccessCheckmark`, `RojanShimmer`/`RojanSkeletonBox`, `RojanEmptyState`, `RojanErrorState`, `RojanAppPalette` (+ `CustomerPalette`/`ManagerPalette`).

**New shared components:** `RojanMotion`, `rememberReducedMotion`, `RojanNavTransitions`, `Modifier.glassReveal`, `RojanAIGlow` (modifier), `RojanCard`, `RojanTextField`, `RojanOtpField`, `RojanAISurface` / `RojanAIInsightCard`, `BeautyDnaSignature`, sparkline component, horizontal-list RTL wrapper, `IconContainer*` size tokens.

**Manager:** `ManagerHeader`, `ManagerScaffold`, `ManagerGlassTheme`, `ManagerIconContainer`, `ManagerPrimaryButton` (→ retired), `ManagerColors` (ratify), `TodayOverviewSection`, `SalonIdentityCard`, `AIInsightCard`, `QuickActionsSection`, `CalendarPreviewSection`, `ManagerDashboardScreen`, `ManagerCalendarScreen`, `ManagerOtpAuthScreen`, Services/Staff/Customers screens; **new:** Revenue, Inventory, Reports.

**Customer:** `HomeColors`, `HomeBackgroundTheme`, `HomeGlassSurface`/`HomeCard`, `CustomerHomeScreen`, `CustomerDashboardScreen`, `CustomerBottomBar`, `AISearchBar`, `SearchScreen`, `SalonListScreen`, booking-flow screens, `BookingSuccessScreen`, all `screens/profile/*` (Loyalty/Membership/Wallet/Coupons consolidation, Beauty DNA/Timeline), `AuthScreen`, `SplashScreen`.

**Legacy to remove (gated):** `RojanHomeCard`, `screens/customer/theme/*` (+ its screenshot test), and — pending the token report — `WarmBackground`, light `RojanColorScheme`, light `RojanText*` tokens, `RojanScaffold`/`PremiumBackground` if `DashboardPlaceholder` is retired.

### 7. Components protected — DO NOT CHANGE

- **Brand color values:** `RojanVividPurple`, `RojanVividMagenta`, `RojanRose`, `RojanBlushPink`, `RojanSoftLavender`, `RojanNavy`, `RojanWarmWhite`, and the rose-gold/gold border tokens (`RojanPremiumBorderRoseGold/Gold/Highlight/Shadow/Specular`). Tuning *usage/alpha* is in scope; changing the hex is not.
- **The `PremiumGlassSurface` mechanic identity** — fixed fill ratio concept, two-shadow stack, single highlight radial, the metallic border language. We add a compact variant and an optional true-blur; we do not fork it or replace the look.
- **`RojanDimens` scale values** (4/8/16/24/32/48) — we *add* icon-container tokens and may diverge `SectionToSection` from `CardToCard`; we don't rescale.
- **`RojanText` first-strong-character direction engine** — extend spatial RTL around it; never replace it, never switch to app-wide `LayoutDirection.Rtl`.
- **`RojanAppPalette` / `LocalRojanPalette` seam** — the "one palette differs per app, one mechanic" model stays.
- **Manager Dashboard section order & weighting** (`ManagerHeader → SalonIdentityCard → TodayOverviewSection → QuickActionsSection → AIInsightCard → CalendarPreviewSection`) — surface treatment changes, order does not.
- **Customer & Manager `applicationId`s, flavor architecture, navigation-graph isolation** — untouched.
- **Reception app** — out of scope for this roadmap beyond inheriting shared-component upgrades.
- **Anything under `ROJAN_Backend`, DTO shapes, endpoints, RBAC** — every AI/analytics/inventory data need is spec-only for System 1.

### 8. Definition of Done (roadmap-level)

The Premium UI Polish Phase is **done** when:

1. **Typography:** a real Persian variable font renders across all three flavours; the type scale is retuned for Persian metrics; no inline font literals in shared components.
2. **Documentation:** `CLAUDE.md` frozen baselines match the shipped product (or carry dated ratifications); `BASELINE_CHANGELOG.md` and the baseline-change protocol exist; this roadmap is the referenced execution plan; zero contradiction between docs and code.
3. **Tokens:** the Token Reconciliation Report is signed off; agreed cleanups executed under their own authorization; no rotted comments remain in `RojanTokens.kt`.
4. **Motion:** no dead animation code; every screen has scroll-safe entrance choreography; Manager has page transitions and skeletons; a documented AI-glow vocabulary; reduced-motion fully honored with an in-app toggle; press feedback compresses and shows focus.
5. **Components:** one shared card, one shared button (with a real filled Manager primary), one glass mechanic with size-aware ornament; shared text/OTP fields; every empty/error/loading state uses shared primitives; an `RojanAISurface` pattern exists with an eligibility rule; legacy duplicates deleted.
6. **AI experience:** AI Insight is a living surface; Beauty DNA has a visual signature; predictive-booking and analytics-intelligence client scaffolding is in place with graceful no-data fallback; every backend dependency is itemized for System 1.
7. **RTL + Accessibility:** WCAG 2.2 AA on all audited flows (contrast, ≥48dp targets, visible focus, correct SR semantics, reduced motion), verified on device with TalkBack + hardware keyboard + Switch Access; horizontal lists and Manager rows are RTL-correct; back button mirrored and non-overlapping.
8. **Customer experience** maturity ≥ 8.5: editorial Home with personalization, consistent premium booking journey, unified Rewards hub.
9. **Manager experience** maturity ≥ 8.5: dashboard with states + AI + sparklines, calendar with month view + RTL + states, premium OTP, live Revenue screen; Inventory & Reports have approved specs.
10. **Quality gates:** `assembleDebug` green for all flavours; screenshot suite refreshed and passing; no new hardcoded colors outside token files; RTL intact; performance — no dropped frames on the reference mid-range device for dashboard scroll and glass-heavy screens; **overall maturity ≥ 8.5 / 10** in internal review.
11. **Governance:** every `[GOV]` change ratified with a dated changelog entry and owner sign-off before merge; every `[S1]` item confirmed with System 1 or shipped behind a graceful fallback; no unapproved cross-boundary change.

---

## Appendix A — Task index by priority

**P0 (blocks premium/pilot claim):** T1.1, T1.13, T1.14, T1.15, T1.16, T2.0, T2.3, T2.6, T2.7, T2.8, 3.3, 3.8, T5.5, T5.6, T5.8, Manager OTP (Phase 7), Booking journey (Phase 6).

**P1 (materially raises quality):** T1.2, T1.3, T1.4, T1.7–T1.12, T1.17, T1.18, T2.1, T2.2, T2.4, T2.5, 3.1, 3.2, 3.4, 3.5, 3.6, 3.7, 3.9, T4.1–T4.5, T4.7, T5.1, T5.2, T5.3, T5.4, T5.7, T5.9, and most of Phases 6–7.

**P2 (refinement):** T1.5, T1.6, T1.11, T4.6, T5.10, Services/Inventory/Reports/Staff-Customers polish, Home-vs-Dashboard consolidation.

## Appendix B — System 1 dependency register (spec-only, for confirmation)

| Need | For | Phase |
|---|---|---|
| Insight rationale + confidence/impact fields | T4.1 living AI Insight | 4 |
| Personalized recommendation results + reasons | T4.3, T4.6 | 4/6 |
| Predictive booking: suggested slots, specialist affinity, wait estimate | T4.4 | 4/6 |
| Historical KPI series (for sparklines + deltas) | T4.5, Manager Revenue | 4/7 |
| Revenue aggregation endpoints (range, by service/specialist) | Manager Revenue screen | 7 |
| Inventory domain + endpoints (products, stock, usage, reorder) | Manager Inventory module | 7 |
| Analytics endpoints (occupancy, retention, no-show, cohorts) | Manager Reports hub | 7 |
| Rewards/loyalty model (tiers, points, perks, history) | Customer Rewards hub | 6 |
| Beauty DNA persistence endpoint | T4.2 (currently session-only) | 4/6 |
| Semantic/NL salon search + salon rating/distance/reviews | Search, Discovery | 6 |
| Notifications destination (or confirm removal of the Manager bell) | 3.6 | 3/7 |

---

*End of roadmap. Planning document only — no code, no file changes except this document, no commit, no push. Execution is not authorized. Awaiting implementation authorization.*
