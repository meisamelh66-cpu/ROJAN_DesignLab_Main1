# ROJAN Design Baseline — Changelog

Every change to a **frozen design baseline** in `CLAUDE.md` (or to the
Shared Premium Glass Design System, tokens, or typography foundation) is
recorded here with date, what changed, why, and who approved it. This
exists so baseline drift cannot recur silently — if `CLAUDE.md` and the
shipped product disagree, the reconciliation is logged here.

Format per entry: **Date · Area · What · Why · Approved by · Evidence.**

---

## 2026-09-02 · Customer app canvas: warm white → dark navy/deep-purple

**What.** `CLAUDE.md` → "Design Baseline v1.0 (Frozen — Customer Home)"
described `WarmBackground.kt` (solid `RojanWarmWhite` base). The shipped
Customer app has migrated **app-wide** to
`screens/customer/hometheme/HomeBackgroundTheme.kt` +
`HomeColors.kt` — a dark navy / deep-purple canvas — across Home, Search,
Auth, Splash, salon, booking, and all profile screens.
`ui/background/WarmBackground.kt`, the light `RojanColorScheme` in
`ui/theme/Theme.kt`, and the light `RojanLuxury*` / `RojanTextOnDarkSurface`
token family are now legacy with few or no live call sites.

**Why.** The migration ("Home Visual Language Unification", per
`design/reference/ROJAN_Manager_Reference (2).png`) happened incrementally
across many prior commits without the frozen-baseline section being
updated. UI Polish Sprint 1 (documentation alignment) reconciles the
document to the shipped reality. The **glass mechanic, shadow scale,
typography approach, spacing rhythm, animation rules and RTL rules** in
the original baseline remain authoritative — only the canvas colour
changed.

**Approved by.** Repository owner — via the "UI POLISH IMPLEMENTATION —
SPRINT 1" instruction (Task 3: "The documentation must match: Current
shipped Customer dark navy/purple theme").

**Evidence.** `docs/uiux/ROJAN_UIUX_PRO_MAX_AUDIT_v1.md` §12 (corrected in
`docs/uiux/ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` §0.2);
`docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md`; `git grep -l
HomeBackgroundTheme app/src/main` covers the whole Customer surface.

---

## 2026-09-02 · Manager app canvas + palette: warm white / `ManagerAccent` → dark emerald / `ManagerColors`

**What.** `CLAUDE.md` → "Design Baseline v1.0 (Frozen — Manager
Dashboard)" described a Warm White background
(`WarmBackground` via `ManagerScaffold`) and a "Teal + Gold identity"
sourced from `ManagerAccent` (Teal = `RojanCategorySkinIcon`, Gold =
`RojanRatingGold`). The shipped Manager app uses
`manager/components/ManagerBackgroundTheme.kt` (deep teal gradient
`#084C52 → #063B3F → #041E2A` + quiet turquoise and gold glow zones) and a
**bespoke** `manager/components/ManagerColors.kt` object
(`BasePrimary #063B3F`, `BaseDeep #041E2A`, `Turquoise #00C9C8`,
`Gold #D4AF37`, `TextPrimary #FFFFFF`, `TextSecondary #C7D8D8`) that
replaced the earlier `ManagerAccent` aliases.

**Why.** Same as above — an approved visual redesign ("ROJAN AI Manager
Visual Theme Implementation", per `design/reference/ROJAN_Manager_Reference.png`)
that predated this changelog. Sprint 1 **ratifies the dark-emerald
palette as the approved per-app expression of the ROJAN brand for the
Manager app**, consistent with the "one shared glass + rose-gold-border
mechanic, palette differs per app" model the *Shared Premium Glass Design
System* section already defines. Section order, card hierarchy/weighting,
typography hierarchy, and `RojanDimens` spacing rhythm in the original
baseline remain authoritative.

**Approved by.** Repository owner — "UI POLISH IMPLEMENTATION — SPRINT 1",
Task 3.

**Evidence.** `manager/components/ManagerColors.kt`,
`ManagerBackgroundTheme.kt`, `ManagerScaffold.kt` doc comments;
`docs/uiux/ROJAN_UIUX_PRO_MAX_AUDIT_v1.md` §11;
`docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md`.

---

## 2026-09-02 · Typography foundation: single font-family swap point + named weight scale

**What.** `ui/theme/Type.kt` now defines `RojanFontFamily` (one `val`, the
single place the app's typeface is set) and `RojanFontWeights` (named
weight roles). Every `RojanTypography` style and the Material base style
route through them. Font family is still `FontFamily.Default` — **no
visual change** — but a future Vazirmatn integration is now a one-line
change. The `CLAUDE.md` "Animation/Typography style" frozen note that
said swapping to Vazirmatn is "a one-line change per style" is now
literally true (one line total).

**Why.** Audit finding P0 — no Persian typeface bundled; roadmap task
T1.1/T1.2. Sprint 1 does the safe wiring; the font *asset* drop is a
Sprint 2 prerequisite (font files + a `FontFamily` from them, or a
product decision on downloadable fonts).

**Approved by.** Repository owner — "UI POLISH IMPLEMENTATION — SPRINT 1",
Task 1.

**Evidence.** `docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md` §A/§B.

---

## 2026-09-02 · Removed dead theme scaffolding: `screens/customer/theme/`

**What.** Deleted the unused `screens/customer/theme/` package
(`CustomerBackgroundTheme`, `CustomerColors`, `CustomerGlassTheme`,
`CustomerIconTheme`, `CustomerTextFieldTheme` — a second, superseded
"Customer" visual identity) and the one-off
`androidTest/.../CustomerThemeScreenshotTest.kt` that was its only
reference. `CLAUDE.md`'s Shared Premium Glass section already named this
package "identified for deletion as part of this phase."

**Why.** Zero production call sites (verified by `git grep`); the only
consumer was a screenshot test whose own doc comment says "Safe to remove
once reviewed." Build verified green (all three flavours assemble; unit
tests unchanged) after removal.

**Approved by.** Repository owner — "UI POLISH IMPLEMENTATION — SPRINT 1",
Task 4 ("Removing dead unused design scaffolding only if confirmed
safe").

**Evidence.** `docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md` §A/§D.
