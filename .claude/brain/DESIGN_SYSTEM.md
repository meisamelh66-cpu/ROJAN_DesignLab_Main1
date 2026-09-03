# DESIGN_SYSTEM — Token Inventory & Glass System Status

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: design token/theme file inventory and Premium Glass System phase status. Frozen-baseline specs (exact values, do-not-change lists) are **not reproduced here** — see root `CLAUDE.md` § Design Baseline v1.0 and § Shared Premium Glass Design System.

## Token files (`ui/theme/`)

`RojanTokens.kt` (355 lines, core token set) + `RojanAppPalette.kt`, `Gradients.kt`,
`Shadows.kt`, `Shapes.kt`, `Type.kt`, `Theme.kt`, `Dimensions.kt`.

## Premium Glassmorphism Design System

- **Phase 1 — done**: shared glass-surface mechanic unified into
  `ui/components/glass/PremiumGlassSurface.kt`. Per-app color identity via `RojanAppPalette`,
  provided once at each root Activity — no default; an unprovided palette fails loudly.
- **Phases 2–6 — planned, not built**: icon rendering, shared buttons, shared cards, net-new
  shared components (dialog/sheet/chip/badge/search/input/bottom-nav), future-app support.
  Full phase breakdown: root `CLAUDE.md` § Shared Premium Glass Design System.

## Spacing rhythm

Named tokens `SpaceSectionToSection` / `SpaceCardToCard` / `SpaceTitleToContent` are enforced
on stacked-card "dashboard" screens — verified live on `ManagerDashboardScreen.kt` and
`CustomerHomeScreen.kt`. ~13 other `RtlSectionHeader` call sites (booking flows, detail/list
screens) are deliberately not migrated — different layout shape, not an oversight.

## Known dead code

`screens/customer/theme/` package (`CustomerColors.kt`, `CustomerGlassTheme.kt`,
`CustomerIconTheme.kt`, + 2 more) — a superseded second Customer visual identity, zero real
call sites. Flagged for deletion in root `CLAUDE.md`; not yet removed as of this snapshot.

## RTL

Persian-first RTL is treated as first-class and frozen throughout, with its own
`ui/components/rtl/` package.
