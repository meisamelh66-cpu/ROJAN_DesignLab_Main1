# ROJAN UI Polish — Sprint 4 Report (AI Experience Layer Foundation)

**Type:** Controlled AI-Native Experience Enhancement — implementation report
**Date:** 2026-09-03
**Baseline:** Sprint 1 `9ec0ee2` · Sprint 2 `6b2aab4` · Sprint 3 (staged, commit pending)
**Scope executed:** Sprint 4 (AI Experience Layer Foundation), per
`ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` Phase 4 and the "UI POLISH
IMPLEMENTATION — SPRINT 4" instruction.

**No AI model integration, no backend/API/DB change, no fake insights /
analytics / recommendations, no new business logic, no new screens.** The
brand system (Rose Gold / Pink Glass / Purple / Navy), the Premium Glass
mechanic, and the RTL-first experience are preserved.

**Delivery shape:** this sprint is **entirely new/refined shared UI
components + one governance document**. Nothing is wired to a screen.
Every component is designed so that it is *structurally impossible* for it
to present fabricated intelligence — the honest "nothing / loading /
error / not-available" states are first-class, and content only ever
comes from a caller that has real data.

---

## A. AI components created

All in `app/src/main/java/ai/rojan/designlab/ui/components/ai/`.

| Component | Task | What it is | Fake-data guard |
|---|---|---|---|
| `RojanAISurface` (+ `RojanAIState`, `RojanAIBadge`) | 1 | The shared "this surface is intelligent" shell — frozen `PremiumGlassSurface` + calm breathing brand glow + "AI" chip, **now with a full 5-state machine**: `Available` / `Loading` / `Empty` / `Error` / `Disabled`. Palette-driven accent (Gold ⁄ Customer-purple). Glow breathes **only** in `Available` + `Loading`. | `content` renders **only** in `Available`; every other state shows honest, generic copy (caller-supplied or a fact-only default). A component can never set itself to `Available`. |
| `RojanAIInsightCard` (+ `RojanAIConfidence`) | 2 | Reusable insight card: `title` + `description` (required, real) and **optional** `status` / `timestamp` / `confidence` / `action` — each rendered only if the caller passes it. Non-`Available` states delegate to `RojanAISurface`. | `RojanAIConfidence` has **no default and no "estimate" path** — a confidence bar cannot be drawn without a real number. The card degrades to just title+description when that's all that's real. |
| `RojanRecommendationSurface` (+ `RojanRecommendationState`) | 4 | "For you" rail shell, states `Available` / `Loading` / `Empty` / `Unavailable`. Built entirely on `RojanAISurface`. | No engine, no ranking, no reasons. `Unavailable` is the honest "not wired yet" state. `content` only in `Available`. |
| `RojanBeautyDnaSignature` (+ `RojanDnaFacet`) | 3 | **Visualization only.** Deterministic radial "bloom" from caller-supplied `facets` (weight + colour). Same facets → same signature. One-shot grow-in on first appearance (scroll-safe latch), then static; reduced-motion → instant. | `RojanDnaFacet` carries **no attribute name, no category, no inference** — only "how prominent" and "what colour", both decided by the caller (a future screen maps the customer's *own explicit* profile selections). No facets → a neutral dashed ring, clearly "not set up". |
| `RojanAILoadingState` | 5 | The one calm inline "AI is working" treatment: a very faint breathing glow behind `RojanAiThinkingIndicator` (both Sprint 2 primitives) + a label. No spinner, no bar, no %. | It is a wait indicator — shows no data. Reduced-motion → static; label alone still says "working". |

**Motion:** all AI components consume the Sprint 2 motion system —
`rojanAiGlow`, `RojanAiThinkingIndicator`, `RojanMotion` tokens,
`rememberReducedMotion()`. No new animation primitive. No infinite
animation except the Sprint-2 glow/dots, which already stop under reduced
motion.

---

## B. Screens affected

**None.**

- No screen imports any `ui/components/ai/*` symbol yet (verified: `git grep` finds zero references outside the `ai/` package and its own KDoc).
- Every other file in the repo is unchanged.
- `RojanAISurface.kt` was introduced in Sprint 3 (staged) and is **refined** here (the state machine); the Sprint 3 → Sprint 4 delta on that one file is additive (new `state` param defaulted to `Available`, new state branches).

Adoption — wiring `RojanAIInsightCard` into the Manager dashboard's AI
Insight card, `RojanBeautyDnaSignature` into the Beauty-DNA screen,
`RojanRecommendationSurface` into Customer "for you" rails — is Sprint 5+
work and is gated on a real data source per surface (see §F).

---

## C. Data dependency

**Zero backend dependency added.** No new endpoint, DTO, contract, repository,
ViewModel, or domain type. No `System 1` change.

The components are **data-agnostic sinks**: they render what a caller
passes and otherwise show honest states. The only real AI data source
that exists in the app today is unchanged:

- Manager: `ManagerDashboardInsights.topRecommendationMessage` +
  `ManagerCrmInsightCategory` (server-side rule engine). A future
  `RojanAIInsightCard` on the Manager dashboard would be fed from that
  existing signal — no new backend work needed for that one surface.

Everything else (customer insights, booking intelligence, beauty
recommendations, confidence values, timestamps, analytics numbers)
requires a real System 1 source before its component may leave the
`Empty` / `Unavailable` state. This is itemised in
`docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md` §5 and §F below.

---

## D. Trust rules

Documented in the new **`docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md`**
(Task 6). Summary:

1. **No-Fake-Intelligence Rule** — an AI component must not display
   anything that did not come from a real upstream source: no generated
   insight/recommendation text, no fabricated number, no invented
   confidence, no inferred customer attribute, no placeholder shown *as*
   content.
2. **Data Trust Principle** — the UI is a faithful window onto backend
   truth; it never invents, infers, or averages something into existence.
   Failed fetch → error state, never stale-as-fresh. "Nothing to tell you
   right now" is a valid premium answer.
3. **When a component may show information** — the 5-state table: content
   only in `Available`, set only by the host from real request state; the
   glow breathes only where "AI is working / has something".
4. **AI UI rules** — one shell (`RojanAISurface`), palette not hue, calm
   reduced-motion-safe motion, the "AI" chip only on genuinely-AI
   surfaces, a fixed content hierarchy, `RojanDimens` + `RojanText` RTL.
5. **PR review checklist** for any future AI surface.

The components enforce the rules structurally where possible
(`RojanAIConfidence` with no default; `content` gated to `Available`;
glow gated to active states; `RojanDnaFacet` carrying no attributes).

---

## E. Build / test results

Environment: JDK = Android Studio JBR, Android SDK local, Gradle offline.

| Check | Command | Result |
|---|---|---|
| Kotlin compile — all 3 flavours | `:app:compile{Customer,Manager,Reception}DevDebugKotlin` (forced `--rerun-tasks`) | ✅ **BUILD SUCCESSFUL** — only pre-existing deprecation warnings (`Icons.Filled.Logout` / `KeyboardArrowLeft` / `Json`), none in the new files |
| Debug APK assembly — all 3 flavours | `:app:assemble{Customer,Manager,Reception}DevDebug` | ✅ **BUILD SUCCESSFUL** |
| JVM unit tests | `:app:testCustomerDevDebugUnitTest` | ⚠️ **158 / 160 pass** — the same 2 `BackendAuthFlowVerificationTest` failures as every prior sprint (`SocketException` / `NetworkUnavailableException`; manual live-backend trigger, no network here). Identical set and count to the Sprint 3 tree. No new failures, no errors. |

- **No fake data:** verified by design — see §A / §D. Grep for hard-coded
  Persian insight strings in the new components: only the honest
  state-copy defaults ("در حال بررسی…", "نکته‌ای برای نمایش نیست",
  "دریافت اطلاعات ممکن نشد", "این قابلیت فعال نیست") — all fact-only,
  none imply content.
- **No backend dependency:** §C. No import of `data/` / `domain/` /
  `presentation/` / `di/` from any new component.
- **No existing screen regression:** no existing file changed except
  `RojanAISurface.kt` (introduced Sprint 3, still unwired). All flavours
  compile + assemble.
- **RTL preserved:** components use `RojanText` (first-strong-character
  direction) and `RojanDimens`; the Beauty-DNA canvas is radially
  symmetric; no `LayoutDirection` change.

---

## F. Sprint 5 prerequisites

| Prerequisite | For | Notes |
|---|---|---|
| **On-device visual pass** | Sign-off of this sprint | Render each of the 5 `RojanAISurface` states; `RojanAIInsightCard` with and without optional fields; `RojanBeautyDnaSignature` with 0 / 3 / 6 facets; reduced-motion. |
| **Wire `RojanAIInsightCard` into the Manager dashboard AI card** | Roadmap T4.1 | The one adoption possible with an existing real source (`topRecommendationMessage`). A small, contained screen change — Sprint 5 or a follow-up, device-verified. Confidence / timestamp stay hidden until the engine reports them (`[S1]`). |
| **Beauty-DNA screen adoption** | Roadmap T4.2 | Map the customer's existing `BeautyProfileRepository` selections → `RojanDnaFacet` list (weights/colours chosen from the brand palette by category). Client-only; `BeautyProfileRepository` is currently session-only, so persistence is a separate `[S1]` item. |
| **Real sources for the other AI surfaces** | Roadmap T4.3 / T4.4 / T4.5 | Customer recommendations + reasons, predictive-booking slots, KPI sparkline history + deltas, confidence values — all `[S1]`. Until each lands, its component stays in `Empty` / `Unavailable`. |
| **`CLAUDE.md` pointer to the AI governance doc** | discoverability | A one-line reference so future sessions find `ROJAN_AI_UI_GOVERNANCE_v1.md` before touching an AI surface. Deferred to keep this sprint's diff to `docs/uiux/` + `ui/components/ai/` only; add in Sprint 5's doc pass. |
| **`rojanPressable` scale direction + focus ring** | carried from Sprint 2/3 | Still blocked on a `BASELINE_CHANGELOG.md` ratification; affects the tap targets inside AI cards (retry / action links currently use `rojanPressable`). |

---

## Commit

**Not committed.** Per the commit rule and `CLAUDE.md`.

> **Note on sequencing:** Sprint 3 is **staged but not yet committed**
> (the plan was approved; an explicit "commit" was not given before
> Sprint 4 was launched). Sprint 4 adds new files and refines one Sprint 3
> file (`RojanAISurface.kt`). Recommended: commit **Sprint 3 first** with
> its message, then **Sprint 4** as a separate commit. Both are presented
> for approval together below.

### Sprint 3 (already staged — 15 files)
Message: `feat(ui): premium component upgrade — shared inputs, AI surface, enterprise CTA`
(unchanged from the Sprint 3 report; `RojanAISurface.kt` will commit at its Sprint 3 version)

### Sprint 4 (this report — 7 files, all additive)
```
 ui/components/ai/RojanAISurface.kt              (+state machine on the S3 file)
 ui/components/ai/RojanAIInsightCard.kt                              (new)
 ui/components/ai/RojanAILoadingState.kt                             (new)
 ui/components/ai/RojanBeautyDnaSignature.kt                         (new)
 ui/components/ai/RojanRecommendationSurface.kt                      (new)
 docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md                              (new)
 docs/uiux/ROJAN_UI_POLISH_SPRINT4_AI_EXPERIENCE_REPORT_v1.md        (new)
```

**Scope confirmation (Sprint 4):** ✅ no backend files · ✅ no API / DTO /
endpoint files · ✅ no database / migration files · ✅ no AI-backend /
model / engine logic · ✅ no `data/` / `domain/` / `presentation/` / `di/`
imports · ✅ no new business logic · ✅ no new screens · ✅ no
navigation / theme / colour-identity / token-value change · ✅ no fake
insight / analytic / recommendation anywhere.

Suggested Sprint 4 message (pending approval):
`feat(ui): AI experience layer foundation — surfaces, insight card, beauty-DNA signature, governance`

---

*End of Sprint 4 report. Stopping here. Awaiting commit authorization for
Sprint 3 and Sprint 4; Sprint 5 not started.*
