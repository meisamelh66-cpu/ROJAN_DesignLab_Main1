# ROJAN UI Polish — Sprint 2 Report (Premium Motion System)

**Type:** Controlled UI Motion Enhancement — implementation report
**Date:** 2026-09-02
**Baseline:** commit `9ec0ee2` — `feat(ui): establish premium design foundation`
**Scope executed:** Sprint 2 (Premium Motion System) only, per
`ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` Phase 2 and the "UI POLISH
IMPLEMENTATION — SPRINT 2" instruction.

**No new screens, no features, no navigation-architecture change, no
backend/API/DB change, no colour/token/theme redesign.** Rose Gold, Pink
Glass, Purple, Navy, the Premium Glass mechanic and the RTL-first
experience are untouched. Every motion value that already existed keeps
its value for non-reduced-motion users — this sprint adds a token layer,
a reduced-motion gate, page transitions where there were none, and opt-in
AI-motion primitives.

---

## A. Files changed

### New — motion foundation (`ui/motion/`)

| File | Purpose |
|---|---|
| `app/src/main/java/ai/rojan/designlab/ui/motion/RojanMotion.kt` | The single source of truth for animation timings, easing curves, travel distances, scale factors and ready-made specs. Duration scale: `Instant 0` / `Fast 150` / `Quick 220` / `Standard 280` / `StandardExit 160` / `Reveal 420` / `Slow 600` / `Ambient 4200` / `Shimmer 1100` / `LoadingSweep 2500`. All either match a value the codebase already used or name a former ad-hoc literal. |
| `app/src/main/java/ai/rojan/designlab/ui/motion/ReducedMotion.kt` | `rememberReducedMotion()` — reads the system animator-duration-scale (the standard Android "remove animations" proxy) and an optional `LocalReducedMotionOverride` (for a future in-app toggle; `null` today). Returns `true` when animation must be suppressed. |
| `app/src/main/java/ai/rojan/designlab/ui/motion/RojanNavTransitions.kt` | `pageEnter` / `pageExit` / `popEnter` / `popExit`, each taking `reduceMotion`. Direction-neutral fade + slight scale-in (identical to what Customer already shipped) → shared with the Manager graph. RTL-safe by construction (no horizontal slide). |
| `app/src/main/java/ai/rojan/designlab/ui/motion/RojanAiGlow.kt` | Opt-in AI-motion primitives, **not wired to any screen** (per "do not create new AI features"): `Modifier.rojanAiGlow(color, reduceMotion)` (slow low-alpha breathing glow), `Modifier.rojanGlassSheen(reduceMotion)` (subtle moving glass highlight), `RojanAiThinkingIndicator(color, reduceMotion)` (calm 3-dot "thinking" indicator — the intelligent alternative to a spinner). Every one is a static no-op under reduced motion. |

### Modified — behaviour preserved for non-reduced-motion users

| File | Change |
|---|---|
| `ui/animation/RojanAnimations.kt` | Re-sourced its literals from `RojanMotion` (values unchanged: page 280/160, content-reveal 420, travel 16dp). `PageEnter`/`PageExit` now delegate to `RojanNavTransitions`. **`rojanEnterAnimation` reimplemented** — was a no-op (`= this`) since it was removed for a scroll-recycle replay bug; now a calm, **scroll-safe**, reduced-motion-aware fade + 8dp settle (see §C). |
| `navigation/RojanNavGraph.kt` | Removed the two top-level `motionEnter`/`motionExit` vals; declared them as **locals** inside the `NavHost` composable so they can read `rememberReducedMotion()`. The ~30 `enterTransition = { motionEnter }` / `exitTransition = { motionExit }` call sites are **unchanged** — they now resolve to the reduced-motion-aware locals. Non-reduced users get exactly the prior transition. |
| `manager/navigation/ManagerRootGraph.kt` | Added `enterTransition` / `exitTransition` / `popEnterTransition` / `popExitTransition` at the `NavHost` level (previously **none** — Manager screens snapped in). Same `RojanNavTransitions` set as Customer, reduced-motion aware. `managerNavGraph`'s own `composable { }` blocks are untouched. |
| `ui/components/loading/RojanShimmer.kt` | `rojanShimmer` now holds a static mid-sweep under reduced motion (skeleton still reads as "loading"); loop duration via `RojanMotion.Shimmer` (1100, unchanged). |
| `components/PremiumLoadingBar.kt` | Static centred glow under reduced motion; sweep duration default via `RojanMotion.LoadingSweep` (2500, unchanged). |
| `screens/customer/CustomerBottomBar.kt` | `HomeMetallicRing` — the perpetual 5.5s gold-ring rotation is now **static** under reduced motion (was unconditional). |
| `components/GlassOrb.kt` | The infinite float loop rests at its neutral position under reduced motion. |
| `ui/components/feedback/RojanSuccessCheck.kt` | Under reduced motion, `RojanSuccessCheckmark` snaps to its end state (no spring overshoot, no ring→check stagger delay). |

### New — documentation

| File | |
|---|---|
| `docs/uiux/ROJAN_UI_POLISH_SPRINT2_MOTION_REPORT_v1.md` | This report. |

**Not touched:** `PremiumGlassSurface.kt` and the metallic border (frozen glass mechanic — glass motion is delivered as separate opt-in modifiers, not by editing it); `rojanPressable` scale value (frozen baseline `1.06f` — see §F); any screen layout, navigation graph structure, ViewModel, token value, colour, or backend/API/DB file.

---

## B. Motion system architecture

```
ui/motion/
├── RojanMotion.kt          ← values only: durations, easings, distances, specs
├── ReducedMotion.kt        ← rememberReducedMotion()  +  LocalReducedMotionOverride
├── RojanNavTransitions.kt  ← page transitions (consume RojanMotion; reduceMotion param)
└── RojanAiGlow.kt          ← opt-in AI-motion modifiers (consume RojanMotion + reduceMotion)

ui/animation/RojanAnimations.kt  ← legacy holder, now a thin layer over the above
                                    (kept so existing imports resolve)
```

**Principles applied**
- **One source of truth.** No screen or component hand-writes a `tween(300)` any more; they consume a named `RojanMotion` role. Existing scattered literals (`280`, `160`, `420`, `150`, `1100`, `2500`, `5500`) are now either a named constant or explicitly documented as "kept at prior value".
- **Reduced motion is a first-class input, not an afterthought.** Every animation utility added or touched this sprint takes `reduceMotion` (or reads it) and has a defined static fallback. **No information depends on animation** — a reduced-motion user sees the same end state, just without the transition (verified per-site in §C).
- **Calm.** Durations sit in the 150–420ms band for anything the user triggers; ambient loops are ≥4s and low-alpha. No overshoot on ordinary content (springs reserved for the one success moment). No horizontal slides. Nothing spins except the pre-existing decorative gold ring, which now stops for reduced-motion users.
- **Opt-in for the expressive stuff.** Glass sheen and AI glow are modifiers a caller adds deliberately; they are not baked into shared surfaces, so no screen changed appearance this sprint.
- **Navigation architecture untouched.** Page transitions use only the `EnterTransition`/`ExitTransition` parameters the `NavHost` / `composable {}` APIs already expose.

---

## C. Before / after behaviour

| Area | Before | After (normal motion) | After (reduced motion) |
|---|---|---|---|
| **Motion values** | ~7 animation literals scattered across 6 files | All named in `RojanMotion`; scattered literals removed or documented as intentionally-preserved | same values, gated off where they loop |
| **Customer page transitions** | fade 280 + scaleIn 0.97, fade-out 160 (per-composable) | **identical**, now sourced from `RojanNavTransitions` | short scale-free cross-fade (fade-in 220 / fade-out 150) |
| **Manager page transitions** | **none** — screens snapped in | fade 280 + scaleIn 0.97 / fade-out 160, applied once at the `NavHost` level to every Manager destination | short scale-free cross-fade |
| **Card / content entrance** (`rojanEnterAnimation`, ~20 call sites) | **no-op** (`Modifier = this`) — content appeared instantly | one-shot fade + 8dp upward settle over 420ms on first appearance; **latch is `rememberSaveable`** so a keyed list row / `item {}` block plays **exactly once** and does not replay on scroll-back. No stagger delay (the specific thing that caused the old replay "pop-in-late" bug). | no-op — content appears instantly (unchanged from before) |
| **Skeleton shimmer** | infinite diagonal sweep, 1100ms | identical | static mid-sweep gradient (still reads as a loading placeholder) |
| **`PremiumLoadingBar`** | infinite glow sweep, 2500ms | identical | static centred glow |
| **Bottom-bar Home ring** | infinite rotation, 5500ms, **always** (even off-screen/reduced) | identical | static metallic ring |
| **`GlassOrb`** | infinite float loop | identical | rests at neutral position |
| **`RojanSuccessCheckmark`** | spring overshoot, ring→check 120ms stagger | identical | snaps to final state, no stagger |
| **AI-motion vocabulary** | none | `rojanAiGlow` / `rojanGlassSheen` / `RojanAiThinkingIndicator` available (unused, ready for Sprint 3/4) | all three render static |

**Reduced-motion "no information lost" check:** page transitions — destination content is identical, only the arrival differs; skeleton/loading — still visibly a loading state; success check — the check mark and colour still communicate success; Home ring — purely decorative; `rojanEnterAnimation` — end state (alpha 1, no offset) is what a reduced-motion user sees immediately. ✅ No state, label, count, or status anywhere depends on an animation running.

---

## D. Performance notes

- **No new always-on animations.** Everything added is either opt-in (AI primitives — currently zero call sites) or a page transition that runs once per navigation. The reduced-motion work *removes* two unconditional infinite loops (`HomeMetallicRing`, `GlassOrb`) for affected users.
- **`rojanEnterAnimation`** uses `graphicsLayer` only (alpha + `translationY`) — a draw-layer property change, no recomposition and no relayout of the item or its siblings. The `rememberSaveable` latch means it settles to a stable `progress == 1f` and the animation stops; no ongoing cost after ~420ms per item's first appearance.
- **Page transitions** are the standard Compose Navigation `AnimatedContent` path — the same machinery the Customer graph already used; Manager now gets it too. `scaleIn(0.97f)` is a `graphicsLayer` scale, GPU-cheap.
- **`RojanMotion` / `RojanNavTransitions`** are stateless value holders — zero runtime cost.
- **APK size:** four small new files (~9 KB of source; the AI primitives are ~3 KB compiled and currently unreferenced — a deliberate foundation cost for Sprint 3/4).
- Build timing unchanged (compile + assemble times identical to Sprint 1 within noise).

**No performance regression identified.** The net effect for a reduced-motion device is *fewer* running animations than before.

---

## E. Build / test results

Environment: JDK = Android Studio JBR, Android SDK local, Gradle offline.

| Check | Command | Result |
|---|---|---|
| Kotlin compile — all 3 target flavours | `:app:compileCustomerDevDebugKotlin :app:compileManagerDevDebugKotlin :app:compileReceptionDevDebugKotlin` | ✅ **BUILD SUCCESSFUL** (no new warnings) |
| Debug APK assembly — all 3 flavours | `:app:assembleCustomerDevDebug :app:assembleManagerDevDebug :app:assembleReceptionDevDebug` | ✅ **BUILD SUCCESSFUL** |
| JVM unit tests | `:app:testCustomerDevDebugUnitTest` / `:app:testManagerDevDebugUnitTest` | ⚠️ **158 / 160 pass** — the **same 2** failures as the Sprint 1 baseline: `BackendAuthFlowVerificationTest` (`SocketException: Permission denied` / `NetworkUnavailableException`), a manual live-backend connectivity trigger that is not a CI gate and requires a local Spring Boot server. **No network in this environment; unrelated to Sprint 2; identical failure set and count to commit `9ec0ee2`.** No new failures, no errors. |

- **No animation crashes:** all animation code compiles and the assembled APKs build; the reimplemented `rojanEnterAnimation` and the reduced-motion branches were reviewed for conditional-composable-call safety (`rememberReducedMotion()` is stable per Activity lifetime, so the early-return branch is consistent per composition).
- **No navigation regression:** Customer transitions are byte-identical for normal-motion users (same `EnterTransition`/`ExitTransition` values, same ~30 call sites, no structural change). Manager gains transitions at the `NavHost` level only; `managerNavGraph`, back-stack behaviour, and `startDestination` logic are untouched.
- **No performance regression:** see §D.
- No lint / detekt / architecture gate exists in this project (confirmed in Sprint 1) — nothing additional to run.
- **On-device visual verification not performed** — no emulator/device was reachable in this environment. The motion is code-reviewed and build-verified; a device pass (page transitions in both apps, `rojanEnterAnimation` scroll-back on a long list, reduced-motion toggle) is recommended before the Sprint 2 commit is promoted beyond this branch.

---

## F. Remaining Sprint 3 prerequisites

| Prerequisite | For | Notes |
|---|---|---|
| **On-device motion pass** | Sign-off of this sprint | Verify: Manager page transitions feel right; `rojanEnterAnimation` does **not** replay on scroll-back in the Customer carousels / Manager lists (the historical risk); reduced-motion toggle genuinely stills everything. Do this before Sprint 3 builds on top. |
| **`rojanPressable` scale direction + focus state** | Roadmap T2.8 (folded into Phase 2, **not done here**) | The frozen baseline specifies `scale to 1.06f` (grow on press). Standard mobile feedback compresses (~0.97f), and `rojanPressable` also sets `indication = null` so there is **no focus indicator anywhere**. Both are real issues but changing them touches a frozen `CLAUDE.md` value and every interactive surface — it needs a `BASELINE_CHANGELOG.md` ratification entry first. Deferred to Sprint 3 (components) with that ratification. |
| **Wire the AI-motion primitives** | Roadmap Phase 3.9 / Phase 4 | `rojanAiGlow` / `RojanAiThinkingIndicator` / `rojanGlassSheen` exist but are unused. Adopting them (AI Insight card, AI search entry, Beauty-DNA result, AI-driven loading) is component + AI-layer work — Sprint 3/4, not motion foundation. |
| **`glassReveal` on `PremiumGlassSurface`** | Roadmap T2.2 | A border-draw / fill-fade "glass arrives" reveal integrated into the surface itself was **not** done — it requires touching the frozen glass mechanic (a defaulted opt-in param) and belongs with the Phase 3 glass work, with its own review. `rojanGlassSheen` (a separate overlay modifier) is the non-invasive placeholder delivered this sprint. |
| **In-app "Reduce motion" toggle UI** | Roadmap T5.8 | `LocalReducedMotionOverride` is in place and honoured; the Settings/Profile toggle that sets it is a small UI addition for the accessibility sprint (Phase 5), not motion foundation. |
| **Manager skeletons / loading states adoption** | Roadmap T2.6 | `rojanShimmer` is now reduced-motion aware, but Manager screens still don't *use* skeletons (they show zeros/empty text). Wiring `RojanSkeletonBox` into the Manager dashboard/calendar/lists is component work (Sprint 3), and needs those screens to expose a real `Loading` state. |
| **`RojanAnimations` legacy shims** | cleanup | `RojanAnimations.Enter` / `.Glow` / `rememberScaleAnimation` are retained but lightly used; fold-in or removal is a later chore, not blocking. |

---

## Commit

**Not committed.** Per the commit rule and `CLAUDE.md` ("Committing or pushing git — always ask first").

`git diff --stat` (modified tracked files; excludes the pre-existing unstaged `manager/data/BackendAppointmentRepository.kt` which is **not** part of this work):

```
 app/.../components/GlassOrb.kt                       |  33 ++--
 app/.../components/PremiumLoadingBar.kt              |  37 +++--
 app/.../manager/navigation/ManagerRootGraph.kt       |  15 ++
 app/.../navigation/RojanNavGraph.kt                  |  24 ++-
 app/.../screens/customer/CustomerBottomBar.kt        |  25 +--
 app/.../ui/animation/RojanAnimations.kt              | 178 +++++++++--------
 app/.../ui/components/feedback/RojanSuccessCheck.kt  |  28 ++--
 app/.../ui/components/loading/RojanShimmer.kt        |  30 ++--
```

New (untracked): `app/src/main/java/ai/rojan/designlab/ui/motion/` (4 files) + this report.

**Scope confirmation:** ✅ no backend files · ✅ no API / DTO / endpoint files · ✅ no database / migration files · ✅ no `build.gradle*` / manifests / resources · ✅ no navigation-graph structure change · ✅ no ViewModels · ✅ no colour / token-value / theme change · ✅ no new screens or features.

Suggested message (pending your approval): `feat(ui): premium motion system + reduced-motion support`

---

*End of Sprint 2 report. Stopping here. Awaiting commit authorization; Sprint 3 not started.*
