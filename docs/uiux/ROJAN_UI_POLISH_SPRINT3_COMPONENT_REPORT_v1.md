# ROJAN UI Polish — Sprint 3 Report (Premium Component Upgrade)

**Type:** Controlled Design System Component Enhancement — implementation report
**Date:** 2026-09-03
**Baseline:** Sprint 1 `9ec0ee2` · Sprint 2 `6b2aab4`
**Scope executed:** Sprint 3 (Premium Component Upgrade), per
`ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` Phase 3 and the "UI POLISH
IMPLEMENTATION — SPRINT 3" instruction.

**No new product features, no new screens, no backend/API/DB change, no
navigation-architecture change, no theme/colour-identity change, no
component-architecture rewrite.** Rose Gold, Pink Glass, Purple, Navy, the
Premium Glass mechanic and the RTL-first experience are preserved. The
frozen `PremiumGlassSurface` mechanic is **not replaced** — only extended
with one additive, defaulted `compact` flag.

**Delivery shape:** most of this sprint is **new shared components with
zero call sites** (the safest way to raise the ceiling without a
screen-wide regression risk that can't be device-verified here) plus a
small set of additive, backward-compatible improvements to existing
components, plus **one** real screen migration (the Manager OTP screen).

---

## A. Components changed

### New shared components (0 existing call sites — pure additions)

| Component | Task | What it is |
|---|---|---|
| `ui/components/ai/RojanAISurface.kt` (`RojanAISurface`, `RojanAIBadge`) | 5 | The shared "this surface is intelligent" shell: the canonical `PremiumGlassSurface` + a slow low-alpha breathing brand glow (`rojanAiGlow`, from Sprint 2) + an "AI" chip. Palette-driven accent (`LocalRojanPalette.textAccent` — Gold for Manager, AI-purple for Customer). Reduced-motion aware. **No AI logic** — content is caller-supplied. |
| `ui/components/input/RojanTextField.kt` (`RojanTextField`) | 4 | One palette-driven glass text field for every app. Animated focus glow-border, inline **error + helper text**, `error(...)` semantics, `heightIn(min = 56.dp)`, per-keystroke RTL via `withDirectionFor`. Reduced-motion: focus glow snaps. |
| `ui/components/input/RojanOtpField.kt` (`RojanOtpField`) | 4 | Segmented OTP input — one glass cell per digit, **unmasked** (was `NumberPassword`), next-cell accent highlight, `contentDescription` + `error(...)` semantics, `heightIn(min = 56.dp)`. Fully hoisted state; never auto-submits. |
| `ui/components/input/RojanSearchField.kt` (`RojanSearchField`) | 3 | Shared glass search input. **Editable mode** (leading glyph, RTL text, explicit **clear ×** button, a *separate* `trailing` slot for a distinct action like a filter — fixes the "filter icon does the same thing as the bar" confusion) and **tap-to-navigate mode** (`readOnly` + `onClick`, the Home `AISearchBar` pattern). `heightIn(min = 48.dp)`. |
| `ui/components/state/RojanStateView.kt` (`RojanStateView`) | 6 | Dispatcher that renders **exactly one** of `RojanLoadingState` / `RojanErrorState` / `RojanEmptyState` or the success `content` — never a blank screen, never "zeros that might be a failure". Deliberate precedence: loading → failure → empty → content. Fabricates no data. |

### Existing components — additive, backward-compatible

| Component | Task | Change |
|---|---|---|
| `ui/components/glass/PremiumMetallicBorder.kt` | 1 | New `compact: Boolean = false` param. When set: keeps the core alternating-band metallic stroke (the part carrying the Rose-Gold/Gold identity), drops the two 4-point corner sparkles + the directional sheen pass + reduces the glow from 7 passes to 3 — the right weight for chips/day-cells/badges below ~120dp. **Default `false` ⇒ every existing surface renders byte-identically.** |
| `ui/components/glass/PremiumGlassSurface.kt` | 1 | New `compact: Boolean = false`, forwarded to `premiumMetallicBorder`. Frozen mechanic otherwise untouched (fill ratio, shadow stack, highlight radius, border language unchanged). |
| `ui/components/glass/GlassSurface.kt`, `manager/components/ManagerGlassTheme.kt` (`ManagerGlassSurface`), `screens/customer/hometheme/HomeGlassSurface.kt` | 1 | Same `compact: Boolean = false` param, forwarded through. Defaulted ⇒ no change. |
| `manager/components/ManagerPrimaryButton.kt` | 2 | **Rebuilt for "enterprise-grade":** now reads as a *filled* action — a Teal→Gold gradient wash (the two Manager accents at low alpha, **no new colour**) inside the existing glass + metallic border, Gold label. New **`loading: Boolean = false`** (spinner replaces label at fixed height, gradient dropped, click blocked). **Disabled** is now distinct (flat, no gradient, 45% opacity, muted label) not just "dimmer". `role = Button` + `disabled()` + Persian `stateDescription` while loading. `heightIn(min = 56.dp)`. |
| `ui/components/buttons/PremiumButton.kt` | 2 | Screen-reader semantics only: `role = Button`, `disabled()` when not interactive, `stateDescription` while loading. **Visuals and the shared press feedback are unchanged** (the luxury/feminine purple→magenta gradient pill, `enabled`/`loading` states it already had). |
| `ui/components/state/RojanLoadingState.kt` | 6 | Reduced-motion aware (Sprint 2 integration): no spinning indicator under reduced motion — the message alone communicates "loading" (falls back to "در حال بارگذاری…" if none given). API unchanged. |

### One screen migration

| Screen | Task | Change |
|---|---|---|
| `manager/screens/auth/ManagerOtpAuthScreen.kt` | 4 | Replaced the hand-rolled raw `OutlinedTextField` (broke the glass language) + masked `NumberPassword` code entry with `RojanTextField` (phone) and `RojanOtpField` (code, unmasked, segmented). Added `verticalScroll` + `imePadding()` so the keyboard no longer hides the field on a short screen. Buttons now show `loading` during submit. **Navigation, callbacks, `onAuthenticated` contract, and the OTP/JWT ViewModel wiring are unchanged.** |

---

## B. Before / after quality

| Area | Before | After |
|---|---|---|
| **Glass ornament at small sizes** | Every glass surface — 84dp chips, 64dp day-cells — painted the full 7-pass glow + two sparkle stars + sheen ("glitter" at that scale) | A `compact` treatment exists (core metallic stroke only). Opt-in this sprint; the audit's "over-ornamented small surfaces" finding now has a fix available. |
| **Manager primary CTA** | Faint translucent glass pill, Gold text ≈ 4.5:1, **no loading state**, disabled = whole-button 50% opacity | Filled Teal→Gold gradient action, clear weight, loading spinner, a *distinct* disabled state, 56dp min height, button semantics |
| **Customer primary CTA** | Bold purple→magenta gradient pill with `enabled`/`loading` (already good), but announced only as a generic clickable | Same luxury visual, now announces as a **button** with disabled / "processing" state to TalkBack |
| **OTP entry** | Raw Material field on the glass screen; code **masked** (couldn't verify typed digits); keyboard could cover it; no per-button loading feedback | Segmented glass cells, **unmasked**, active-cell highlight, `contentDescription`, scroll + `imePadding`, button loading |
| **Text fields** | Three hand-rolled variants (`CustomerTextField`, `HomeTextField`, Manager's inline `OutlinedTextField`), no shared error/helper pattern | One palette-driven `RojanTextField` with focus glow, inline error + `error()` semantics, helper text, ≥56dp — ready for adoption |
| **Search input** | `AISearchBar` (Home) with a filter glyph that navigates identically to the bar; `SearchScreen` inline `BasicTextField` with no clear button | `RojanSearchField` with an explicit clear (×) button separate from a distinct trailing-action slot; two modes cover both patterns |
| **Loading vs empty vs failure** | Each of ~28 screens decides for itself; a swallowed error can look identical to an empty result | `RojanStateView` enforces exactly-one, with a deliberate precedence and no data fabrication |
| **AI surface identity** | None — the Manager AI Insight card looked like every other card | `RojanAISurface` exists: glass + calm breathing brand glow + "AI" chip, per-app accent, reduced-motion aware — ready for the Sprint 4 AI layer |

---

## C. API compatibility impact

**All changes are source- and binary-compatible within the module. No call site required an edit** (except the deliberate OTP-screen migration).

| Component | Signature change | Compatibility |
|---|---|---|
| `PremiumGlassSurface` / `GlassSurface` / `ManagerGlassSurface` / `HomeGlassSurface` | added `compact: Boolean = false` immediately before the trailing `content` lambda | Every call site uses the trailing-lambda form `GlassSurface(shape = x) { … }` and does not name `compact` → unaffected. Verified: all 3 flavours compile + assemble unchanged. |
| `premiumMetallicBorder` | added `compact: Boolean = false` (last param) | Sole caller is `PremiumGlassSurface` (updated). |
| `ManagerPrimaryButton` | added `loading: Boolean = false` (last param) | 5 Manager booking-flow call sites + the OTP screen; none named a positional 6th arg. Booking screens unaffected; OTP screen now passes `loading` explicitly. |
| `PremiumButton` | none (internal `.semantics` only) | — |
| `RojanLoadingState` | none | — |
| `RojanAISurface`, `RojanTextField`, `RojanOtpField`, `RojanSearchField`, `RojanStateView` | new | No existing code references them yet. |

---

## D. Screens affected

| Screen(s) | Nature of impact |
|---|---|
| `ManagerOtpAuthScreen` | **Directly migrated** — new field components, keyboard-inset handling, button loading. Same layout shape, same navigation, same callbacks, same auth ViewModel. |
| **All 5 Manager booking-flow screens** (`ManagerBookingStart/Customer/Service/Specialist/DateTime/Review`) that use `ManagerPrimaryButton` | **Visual upgrade** — the "continue/confirm" CTA is now a filled Teal→Gold gradient action, 56dp min height, distinct disabled. No behaviour/callback change. This is the intended "enterprise-grade Manager CTA". |
| **~15 `PremiumButton` call sites** (incl. `RojanEmptyState`'s action button, booking, profile) | Semantics added only — **no visual change**. |
| **Every glass-surface call site** (dozens across both apps) | Recompiled; **output byte-identical** (`compact` defaults `false`). |
| No other screen | — |

**RTL preserved:** the new field/search/OTP components resolve text direction per keystroke via the existing `withDirectionFor` / `RojanText` system; OTP cells render LTR (numeric, same rule as phone numbers); no layout mirroring introduced. **Accessibility preserved / improved:** every new interactive element is ≥48dp (fields ≥56dp); buttons and fields carry `role` / `disabled()` / `error()` / `stateDescription` semantics; `RojanLoadingState` no longer depends on an animation running.

---

## E. Build / test result

Environment: JDK = Android Studio JBR, Android SDK local, Gradle offline.

| Check | Command | Result |
|---|---|---|
| Kotlin compile — all 3 flavours | `:app:compile{Customer,Manager,Reception}DevDebugKotlin` (forced `--rerun-tasks`) | ✅ **BUILD SUCCESSFUL** — no new warnings |
| Debug APK assembly — all 3 flavours | `:app:assemble{Customer,Manager,Reception}DevDebug` | ✅ **BUILD SUCCESSFUL** |
| JVM unit tests | `:app:testCustomerDevDebugUnitTest` | ⚠️ **158 / 160 pass** — the same 2 `BackendAuthFlowVerificationTest` failures as every prior sprint (`SocketException: Permission denied` / `NetworkUnavailableException`; a manual live-backend trigger, no network here, not a CI gate). Identical set and count to baseline `6b2aab4`. No new failures, no errors. |

- **Existing screens compile:** ✅ all flavours, forced clean rebuild.
- **No theme regression:** no colour or token *value* changed; the frozen glass mechanic's fill/shadow/highlight/border are untouched; `compact` and `loading` are opt-in defaults. The one visible change (`ManagerPrimaryButton`) is a deliberate, in-identity upgrade of a CTA the audit and Sprint 2 both flagged as too weak.
- **RTL preserved:** see §D.
- **Accessibility preserved:** see §D — and improved (semantics, touch targets, reduced-motion loading).
- No lint / detekt / architecture gate exists in this project (confirmed Sprint 1).
- **On-device visual verification not performed** — no emulator/device reachable. The new components are code-reviewed and build-verified. A device pass (Manager OTP flow end-to-end; `ManagerPrimaryButton` across the booking flow; a `compact`-border chip; reduced-motion) is recommended before this branch is promoted.

---

## F. Remaining Sprint 4 prerequisites

| Prerequisite | For | Notes |
|---|---|---|
| **On-device component pass** | Sign-off of this sprint | Manager OTP flow (request → verify → route); `ManagerPrimaryButton` in the 5 booking screens; a `compact` border on a real chip; reduced-motion. |
| **Adopt `RojanStateView` + fix state-card contrast** | Roadmap 3.7/3.8, T5.5 | `RojanEmptyState`/`RojanErrorState`/`RojanLoadingState` still wrap `GlassSurface` (Customer-light-bound) and hard-code `RojanTextPrimary` (dark plum). On the now-dark Customer canvas this is a latent low-contrast issue. Fixing it means making `RojanStateCard` palette-aware **and** re-verifying it on Reception (light) — a ~28-screen blast radius that must be device-checked. Deliberately **not** touched this sprint. |
| **Wire `RojanTextField` / `RojanSearchField`** into Customer screens + `ReceptionOtpAuthScreen` | Roadmap 3.4, Phase 6 | The shared components exist; migrating `AISearchBar`, `SearchScreen`, `CustomerTextField`/`HomeTextField` call sites, and the Reception OTP screen is screen work (Sprint 6 / a follow-up). |
| **Wire `RojanAISurface`** into the Manager AI Insight card, Beauty-DNA result, smart-recommendation surfaces | Roadmap 3.9 / Phase 4 | This is the AI-experience sprint's job — `RojanAISurface` is the foundation it builds on. |
| **Auto-select `compact`** border by surface size | Roadmap T1.9 | Auto-applying it (e.g. via `BoxWithConstraints` threshold) would change existing chips' appearance → a regression that needs device verification. Opt-in only for now; the small-surface call sites (calendar day-cells, quick-action chips, filter chips) get `compact = true` in a device-verified pass. |
| **`rojanPressable` scale direction + real focus ring** | Roadmap T2.8 (carried from Sprint 2) | Still blocked on a `BASELINE_CHANGELOG.md` ratification (the frozen baseline says `scale to 1.06f`), and it touches every interactive surface. This is why button/field "focus state" is partial — the primitive they all share can't show focus until `rojanPressable` is changed. |
| **`PremiumButton` fragile default modifier** | Roadmap 3.3 | `modifier = Modifier.size(240 × 64)` as a *default* is lost if a caller passes its own modifier. Fixing it safely needs a call-site audit (which callers rely on the default size). Deferred. |
| **`RojanHomeCard` vs `HomeCard` dedup** | Roadmap Phase 4 (Cards) | Two parallel card shells remain. Consolidation is a larger, device-verified change. |
| **`CustomerTextField` removal** | cleanup | Superseded by `HomeTextField` and now `RojanTextField`; 0–2 call sites. Remove in a later chore. |

---

## Commit

**Not committed.** Per the commit rule and `CLAUDE.md`.

`git diff --stat` (staged; the pre-existing unstaged `manager/data/BackendAppointmentRepository.kt` is **not** part of this work and is excluded):

```
 manager/components/ManagerGlassTheme.kt              |   2 +
 manager/components/ManagerPrimaryButton.kt           |  79 ++++++---
 manager/screens/auth/ManagerOtpAuthScreen.kt         | 176 ++++++--------------
 screens/customer/hometheme/HomeGlassSurface.kt       |   2 +
 ui/components/ai/RojanAISurface.kt                    | 127 ++++++++++++++  (new)
 ui/components/buttons/PremiumButton.kt               |  14 ++
 ui/components/glass/GlassSurface.kt                  |   2 +
 ui/components/glass/PremiumGlassSurface.kt           |   5 +
 ui/components/glass/PremiumMetallicBorder.kt         |  13 +-
 ui/components/input/RojanOtpField.kt                 | 143 ++++++++++++++  (new)
 ui/components/input/RojanSearchField.kt              | 146 ++++++++++++++  (new)
 ui/components/input/RojanTextField.kt                | 177 +++++++++++++++++  (new)
 ui/components/state/RojanLoadingState.kt             |  24 ++-
 ui/components/state/RojanStateView.kt                |  66 ++++++++  (new)
 14 files changed, 851 insertions(+), 125 deletions(-)
```
+ this report (`docs/uiux/ROJAN_UI_POLISH_SPRINT3_COMPONENT_REPORT_v1.md`).

**Scope confirmation:** ✅ no backend files · ✅ no API / DTO / endpoint files · ✅ no database / migration files · ✅ no navigation-architecture change (no `NavGraph` / `NavHost` / graph-structure edits) · ✅ no `build.gradle*` / manifests / resources · ✅ no theme / colour-identity / token-value change · ✅ no component-architecture rewrite (frozen glass mechanic extended by one defaulted flag, not replaced) · ✅ no new screens or features.

Suggested message (pending approval): `feat(ui): premium component upgrade — shared inputs, AI surface, enterprise CTA`

---

*End of Sprint 3 report. Stopping here. Awaiting commit authorization; Sprint 4 not started.*
