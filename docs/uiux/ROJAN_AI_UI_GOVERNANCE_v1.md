# ROJAN AI UI Governance v1

**Status:** Normative for all AI-facing UI in the ROJAN Android apps.
**Introduced:** UI Polish Sprint 4 (AI Experience Layer Foundation).
**Applies to:** every use of `ui/components/ai/*`, `ui/motion/RojanAiGlow.kt`,
and any screen that presents salon / customer / booking / beauty
intelligence.

The goal of the AI layer is that a manager or customer feels **"ROJAN
understands my business / my style"** — earned by honest presentation of
real signals, never by fabricated intelligence.

---

## 1. The No-Fake-Intelligence Rule (non-negotiable)

An AI component **must not display anything that did not come from a real
upstream source.**

Concretely, the following are **forbidden**, in production and in any
build that ships:

- a generated insight, tip, or recommendation text;
- a fabricated number — count, percentage, revenue, "customers at risk",
  occupancy, trend;
- a **confidence / score** that was not reported by the source
  (`RojanAIConfidence` has no default and no "estimate" path for exactly
  this reason);
- a Beauty-DNA pattern that implies an inferred attribute the customer
  did not enter themselves;
- a "for you" item chosen by any client-side ranking;
- a plausible-looking placeholder shown *as if* it were content while
  real data loads or is missing.

If there is no real data, the component shows an **honest state**
(§3), not a stand-in.

Today the only real AI source in the app is the Manager dashboard's
**server-side rule engine** (`ManagerDashboardInsights.topRecommendationMessage`
+ `ManagerCrmInsightCategory`). Every other AI surface is **foundation
only** until System 1 provides a real source.

---

## 2. Data Trust Principle

> The UI is a faithful window onto backend truth. It never invents,
> never infers, never averages something into existence.

- **Attribution:** an AI surface may only show a value the backend
  computed and returned. The client does no aggregation, prediction, or
  scoring.
- **Freshness:** if a fetch fails, show the error state — never keep
  showing stale AI content as if it were current (`RojanStateView` and
  `RojanAISurface` both enforce this precedence: loading → error →
  empty → content).
- **Honesty of absence:** "we have nothing to tell you right now" is a
  perfectly good, premium thing for an intelligent product to say. It is
  always preferable to a guess.
- **No inference about people:** the Beauty-DNA visualization
  (`RojanBeautyDnaSignature`) renders only facets the caller passes,
  which a screen derives **solely from the customer's own explicit
  profile selections**. No skin/hair/age/spend inference, ever.

---

## 3. When an AI component may show information

An AI surface is always in exactly one state
(`RojanAIState` / `RojanRecommendationState`):

| State | Shows | When |
|---|---|---|
| **Available** | the caller's real content | a real source returned real content |
| **Loading** | calm thinking indicator + label | a real request is in flight |
| **Empty** | honest "nothing right now" copy | the request completed and there is genuinely nothing |
| **Error** | honest failure copy + optional retry | the request failed |
| **Disabled / Unavailable** | "this isn't active yet" copy, muted, no glow | the capability is not provisioned for this account / not built |

- Content (`Available`) is **only** rendered when the host screen, from
  real request state, says there is real content.
- The brand **glow breathes only in `Available` and `Loading`** — the
  states where "AI is working / has something". Empty / Error / Disabled
  do not glow; nothing should imply activity where there is none.
- A component may **never** transition itself into `Available` — only the
  host, from real data, sets that state.

---

## 4. AI UI rules (visual + motion)

- **One shell.** Every AI surface renders through `RojanAISurface`
  (directly, or via `RojanAIInsightCard` / `RojanRecommendationSurface`).
  No bespoke "AI card". It uses the frozen `PremiumGlassSurface`
  mechanic — an AI surface is unmistakably ROJAN glass.
- **Palette, not hue.** The AI accent is
  `LocalRojanPalette.current.textAccent` — Gold in Manager, the AI-glow
  purple in Customer. Never a hard-coded colour. This keeps the brand
  identity (Rose Gold / Pink Glass / Purple / Navy) intact while letting
  each app express AI in its own voice.
- **Calm motion only.** The glow is a slow (~4.2s), low-alpha (≤0.15)
  pulse. The loading indicator is three fading dots
  (`RojanAiThinkingIndicator`) — no spinner, no bar, no progress %. All
  AI motion is a no-op / static under reduced motion
  (`rememberReducedMotion()`), and no AI information ever depends on an
  animation running.
- **The "AI" chip** (`RojanAIBadge`) marks a surface as AI-driven. Put it
  only on surfaces that genuinely are. Overusing it makes it meaningless.
- **Hierarchy inside an AI card:** accent icon → title (`CardTitle`) →
  optional status chip → description (`Body`) → optional timestamp
  (`Caption`, muted) → optional confidence meter (real data only) →
  optional single action. Optional parts appear only when real.
- **Spacing / RTL:** `RojanDimens` tokens only; text direction via the
  app's `RojanText` first-strong-character system; no layout mirroring
  introduced by AI components.

---

## 5. Component reference (Sprint 4 foundation)

| Component | Purpose | Real data required to show content |
|---|---|---|
| `RojanAISurface` | the shared AI shell + 5-state machine | caller's `content` for `Available` |
| `RojanAIInsightCard` | title / description / status / timestamp / confidence | title + description (real); every other field optional, shown only if passed |
| `RojanRecommendationSurface` | "for you" rail shell, 4 states | caller's `content` for `Available` |
| `RojanBeautyDnaSignature` | deterministic visual signature from caller-supplied facets | `facets` derived from the customer's own explicit selections; empty → neutral placeholder ring |
| `RojanAILoadingState` | the one inline "AI is working" treatment | — (it is a wait indicator) |
| `RojanAIBadge` | the "AI" chip | — (a label) |

All six are **unwired** as of Sprint 4 — no screen references them yet.
Adoption happens when a real source exists for each surface (Sprint 5+,
and gated on System 1 for anything server-side).

---

## 6. Review checklist for any PR touching an AI surface

- [ ] Every value shown traces to a real backend field (name it in the PR).
- [ ] No client-side ranking, scoring, averaging, or text generation.
- [ ] `confidence` passed only if the source reports one.
- [ ] Loading / empty / error / disabled states are wired from real
      request state, not faked.
- [ ] Copy for empty/error states states a fact, implies no content.
- [ ] Accent comes from `LocalRojanPalette`, not a literal.
- [ ] Motion is calm and reduced-motion-safe.
- [ ] Beauty-DNA facets come only from the customer's own explicit input.
- [ ] Any new server dependency is listed for System 1 confirmation.
