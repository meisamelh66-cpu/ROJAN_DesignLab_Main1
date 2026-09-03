# ROJAN UI Polish — Sprint 1 Report (Design Foundation)

**Type:** Controlled UI Foundation Improvement — implementation report
**Date:** 2026-09-02
**Scope executed:** Sprint 1 (Design Foundation) only, per
`ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` and the "UI POLISH IMPLEMENTATION
— SPRINT 1" instruction.
**Reference:** `ROJAN_UIUX_PRO_MAX_AUDIT_v1.md`,
`ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md`
**Companion:** `docs/uiux/BASELINE_CHANGELOG.md`

**No visual redesign. No product-identity change.** Rose Gold, Pink
Glass, Purple, Soft White, Navy, the Premium Glass system, and the
RTL-first experience are untouched. No token *values* were changed.

---

## A. Files changed

### Code — behaviour-preserving

| File | Change | Kind |
|---|---|---|
| `app/src/main/java/ai/rojan/designlab/ui/theme/Type.kt` | Introduced `RojanFontFamily` (single font-family swap point) and `RojanFontWeights` (named weight scale). Renamed the previously-dead `val Typography` → `RojanBaseTypography` and routed it, plus every `RojanTypography` style, through the two new declarations. Font family stays `FontFamily.Default`; weight *values* unchanged. | Foundation setup — **no visual change** |
| `app/src/main/java/ai/rojan/designlab/ui/theme/RojanTokens.kt` | **Comment-only.** Corrected doc comments that described colours the code does not hold (`RojanTextOnDarkSurface` / `RojanDarkSurfaceText` claimed "Soft White #F4F6F8" — actual values are plum `#6B5579` / `#4D355F`); rewrote the "LUXURY TYPOGRAPHY" block header and its 8 token comments ("Soft White"/"Pure White"/"Light Gray" labels were all inaccurate). **No `Color(...)` value changed.** | Doc accuracy |
| `app/src/main/java/ai/rojan/designlab/ui/theme/Theme.kt` | **Comment-only.** Added a "LARGELY LEGACY / 2026-09-02" note to `RojanColorScheme`'s KDoc — both apps now render dark, and (as its own pre-migration audit already noted) nothing reads `MaterialTheme.colorScheme`. Scheme values untouched. | Doc accuracy |
| `app/src/main/java/ai/rojan/designlab/ui/background/WarmBackground.kt` | **Comment-only.** Added a "⚠️ SUPERSEDED (2026-09-02)" note — the Customer app moved to `HomeBackgroundTheme` app-wide; this component has no live call sites. Code untouched. | Doc accuracy |
| `app/src/main/java/ai/rojan/designlab/screens/customer/hometheme/HomeBackgroundTheme.kt` | **Comment-only.** Corrected the KDoc's false scoping claim ("Used only by `CustomerHomeScreen` … every other Customer screen keeps that frozen light background") to reflect app-wide adoption. Dropped a `@link` to the now-deleted `CustomerBackgroundTheme`. Code untouched. | Doc accuracy |
| `app/src/main/java/ai/rojan/designlab/screens/customer/hometheme/HomeColors.kt` | **Comment-only.** Same false-scoping correction in the KDoc. Colour values untouched. | Doc accuracy |

### Code — dead scaffolding removed (build-verified)

| File | Reason |
|---|---|
| `app/src/main/java/ai/rojan/designlab/screens/customer/theme/CustomerBackgroundTheme.kt` | Unused. Part of a superseded second "Customer" visual identity. Zero production call sites. `CLAUDE.md` already named this package "identified for deletion as part of this phase." |
| `app/src/main/java/ai/rojan/designlab/screens/customer/theme/CustomerColors.kt` | ″ |
| `app/src/main/java/ai/rojan/designlab/screens/customer/theme/CustomerGlassTheme.kt` | ″ |
| `app/src/main/java/ai/rojan/designlab/screens/customer/theme/CustomerIconTheme.kt` | ″ |
| `app/src/main/java/ai/rojan/designlab/screens/customer/theme/CustomerTextFieldTheme.kt` | ″ |
| `app/src/androidTest/java/ai/rojan/designlab/customer/CustomerThemeScreenshotTest.kt` | The **only** reference to the package above. A self-described "one-off visual-verification aid… NOT wired into any real screen/navigation… Safe to remove once reviewed." |

### Documentation

| File | Change |
|---|---|
| `CLAUDE.md` | Added three dated block-quote annotations (**history preserved, nothing removed**): "⚠️ PARTIALLY SUPERSEDED — 2026-09-02" on *Design Baseline v1.0 (Frozen — Customer Home)* and *(Frozen — Manager Dashboard)*; "ℹ️ Note — 2026-09-02" on *Shared Premium Glass Design System*. Plus one parenthetical in the *ROJAN MANAGER FOUNDATION* section pointing at the Manager note. The Manager annotation **ratifies** the dark-emerald `ManagerColors` palette as the approved per-app brand expression. |
| `docs/uiux/BASELINE_CHANGELOG.md` | **New.** The dated record of every baseline reconciliation, so drift cannot recur silently (roadmap T1.18). Five entries covering this sprint. |
| `docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md` | **New.** This report. |
| `docs/uiux/ROJAN_UIUX_PRO_MAX_AUDIT_v1.md`, `docs/uiux/ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md` | Previously produced (audit + roadmap turns); were untracked. Included in this commit as the reference set this foundation work implements against. |

**Not touched (confirmed):** no backend files, no `ROJAN_Backend`, no API/DTO/endpoint code, no database/migration code, no `build.gradle*` / `libs.versions.toml` / manifests / resources, no navigation graphs, no ViewModels, no screen composables (beyond two KDoc-only edits in `hometheme/`), no other modules. The pre-existing unstaged edit to `manager/data/BackendAppointmentRepository.kt` is **not mine** and is **excluded** from the commit.

---

## B. Reason for every change

| # | Change | Audit / roadmap driver | Rationale |
|---|---|---|---|
| 1 | `RojanFontFamily` + `RojanFontWeights` in `Type.kt` | Audit P0 "no Persian typeface"; roadmap T1.1 / T1.2 | The app hard-codes `FontFamily.Default` in 8 places and `FontWeight.*` literals throughout. Consolidating to one family `val` and a named weight object makes the eventual Vazirmatn swap a **one-line change** and removes ad-hoc weight literals from the foundation. Zero behaviour change today (`RojanFontFamily == FontFamily.Default`; weight values identical). |
| 2 | Rename dead `val Typography` → `RojanBaseTypography`, fully-qualify the Material3 type | roadmap T1.2; audit "decorative Material theming" | The old name shadowed the Material3 `Typography` class (`import … Typography` + `val Typography = Typography(...)`), was unreferenced, and used `FontFamily.Default` directly. Renamed + routed through the swap point + shadow removed. Kept (not deleted) as a deliberate Material `bodyLarge` floor. |
| 3 | `RojanTokens.kt` comment corrections | Audit §5.1 "comment rot"; roadmap T1.7 | Comments claimed `#F4F6F8` / "Pure White" for tokens whose committed values are dark plum. A developer trusting the comment would place dark text on a dark surface (invisible). Fixing the words (not the values) is the safe, in-scope fix; the value question is deferred to an authorized token-cleanup pass. |
| 4 | `Theme.kt` / `WarmBackground.kt` / `HomeBackgroundTheme.kt` / `HomeColors.kt` KDoc corrections | Audit §0.2 correction; roadmap T1.13 / T1.17 | These files' comments asserted the app is light / that the dark theme is "Home only." Both are false since the app-wide dark migration. Comment-only. |
| 5 | `CLAUDE.md` block-quote annotations | Audit finding #3 (Tier-0 governance); roadmap T1.13–T1.16 | The frozen baselines described a warm-white product that no longer exists. Highest-severity governance item. Annotated in place (history kept), with the Manager palette explicitly ratified. |
| 6 | `BASELINE_CHANGELOG.md` | roadmap T1.18 | A protocol so a future baseline change is logged, not silent. |
| 7 | Delete `screens/customer/theme/` + its test | Audit §4.1; roadmap T2.0 note; `CLAUDE.md` ("identified for deletion") | Proven unused (`git grep`), self-described as removable, build stays green. The cleanest possible dead-code removal. |

---

## C. Before / after foundation state

| Aspect | Before | After |
|---|---|---|
| **Font family** | `FontFamily.Default` hard-coded in 8 `TextStyle` literals + the Material base | One `RojanFontFamily` val; all 9 styles route through it. Vazirmatn = 1-line change. Still renders `FontFamily.Default` (no visual change). |
| **Font weights** | Bare `FontWeight.Bold` / `.SemiBold` / `.Medium` / `.Normal` per style | Named `RojanFontWeights` scale (Light…ExtraBold) with documented role guidance; the 4 used weights unchanged in value |
| **`Type.kt` dead code** | `val Typography` (0 references, shadowed the Material3 class) | Renamed `RojanBaseTypography`, routed through the swap point, shadow removed |
| **Token doc accuracy** | ~11 comments in `RojanTokens.kt` describing colours the code doesn't hold ("Soft White", "Pure White", "Light Gray") | All corrected to state the actual value + legacy/usage status; **no value changed** |
| **`CLAUDE.md` frozen baselines** | Describe a warm-white Customer Home + warm-white Manager Dashboard — contradicted by the shipped dark apps | Dated "SUPERSEDED"/"Note" annotations; dark themes described + Manager palette ratified; original text retained as history |
| **Baseline-change protocol** | None — drift was silent and untracked | `BASELINE_CHANGELOG.md` + a documented protocol |
| **Dead theme scaffolding** | `screens/customer/theme/` (5 files, 783 LOC) + 1 orphan test, unused since the dark migration | Removed; build green |
| **Legacy light-mode surface** (`WarmBackground`, light `RojanColorScheme`, `RojanLuxury*`) | Live-looking, no "legacy" marker | Marked legacy in-code + in the changelog; **left in place** (removal is a later authorized pass) |

---

## D. Build / test result

Environment: JDK = Android Studio JBR (`…\jbr`), Android SDK =
`…\AppData\Local\Android\Sdk`, Gradle offline.

| Check | Command | Result |
|---|---|---|
| Kotlin compile — all 3 target flavours | `:app:compileCustomerDevDebugKotlin :app:compileManagerDevDebugKotlin :app:compileReceptionDevDebugKotlin` | ✅ **BUILD SUCCESSFUL** (only pre-existing warnings in `di/BackendApiContainer.kt`, not touched by this sprint) |
| Debug APK assembly — all 3 flavours | `:app:assembleCustomerDevDebug :app:assembleManagerDevDebug :app:assembleReceptionDevDebug` | ✅ **BUILD SUCCESSFUL** |
| JVM unit tests | `:app:testCustomerDevDebugUnitTest` / `…Manager…` | ⚠️ **158 / 160 pass.** The 2 failures are `BackendAuthFlowVerificationTest` — a **manual live-backend connectivity trigger** (its own KDoc: *"Requires `ROJAN_Backend`'s Spring Boot app running locally at http://localhost:8080 … not a hermetic unit test, and is not wired into any CI/build gate"*). Failure causes: `java.net.SocketException: Permission denied: getsockopt` / `NetworkUnavailableException: No network connection` — **environmental (no network in this sandbox), pre-existing, unrelated to this sprint.** The test file references none of the changed symbols. |

**RQG:** `assembleDebug` succeeds (all flavours); no new hardcoded
colours (no colours added at all); design tokens/glass/RTL untouched.
No device screenshot is included — this sprint makes **no rendered
visual change** (font family unchanged, all edits are comments / dead
code / doc). A screenshot verification pass belongs to Sprint 2+, when
the Vazirmatn asset actually changes rendering.

No architecture-check / lint gate is wired into this project's build
(verified — no `detekt` / `ktlintCheck` / custom verification task in
`app/build.gradle.kts` or the root build). Nothing to run.

---

## E. Remaining Sprint 2 prerequisites

| Prerequisite | Needed for | Notes |
|---|---|---|
| **Vazirmatn font files** (`.ttf`, weights 300–800) committed under `app/src/main/res/font/`, **or** a product decision to use Android Downloadable Fonts | Roadmap T1.1 completion (real Persian typography) | The wiring is done (`RojanFontFamily`). This sprint could not add the binary asset — no network to fetch it and it is a binary file. Downloadable Fonts would add a Google Play Services provider + a runtime network dependency, which the roadmap flags as an architecture-level decision requiring its own approval. **Owner decision required.** |
| Persian line-height re-tune sign-off | Roadmap T1.3 | Deferred deliberately — re-tuning line-heights (`Body` 24→~26, `Caption` 20→~22, etc.) only makes sense once the real Persian face is rendering, and it *is* a visible change. Do it in the same PR as the font asset. |
| Token Reconciliation Report review | Roadmap T1.7–T1.12 | This sprint corrected the token *comments*. The next step (classify LIVE / LEGACY / removable, propose the 3-tier target, list safe deletions) is analysis + a doc, still unstarted. The 7 unused `RojanLuxury*` tokens + the legacy `WarmBackground` / light `RojanColorScheme` / `RojanScaffold` / `PremiumBackground` are the removal candidates — **not removed here** (bigger blast radius, own reviewed PR). |
| Motion token layer (`RojanMotion`) + reduced-motion gate | Roadmap Phase 2 (T2.0) — blocks all of Phase 2 | Independent of Sprint 1; can start once Sprint 2 is authorized. |
| `DashboardPlaceholder.kt` disposition | Enables removing `RojanScaffold` / `PremiumBackground` / `RojanTextOnDarkSurface` | It is the last live consumer of the light scaffold. Decide whether it is still needed before those removals. |

---

## F. Risks discovered

| # | Risk | Severity | Detail / mitigation |
|---|---|---|---|
| R1 | **Line endings.** The repo is `core.autocrlf=true` with CRLF `.kt` files and no `.gitattributes`. `Type.kt` was rewritten; it has been converted to CRLF to match siblings, and the staged diff was verified as content-only (not line-ending noise) via `git diff --cached --ignore-all-space`. Recommend adding a `.gitattributes` (`*.kt text eol=crlf` or `text=auto`) in a later chore to make this deterministic. | Low | Handled this sprint; flagged for a follow-up. |
| R2 | **`RojanLuxuryCaption` is the lone survivor** of the "Luxury" family — used once in `RojanNavGraph.kt:1155`. When that family is eventually removed, that call site needs a replacement token (`RojanTextSecondary` or a `HomeColors` value, depending on the surface). Documented in the changelog + the token comments; not actioned here. | Low | Deferred to the token-cleanup pass. |
| R3 | **`RojanTextOnDarkSurface` / `RojanDarkSurfaceText` values are mislabelled *and* arguably wrong** — they are dark plum, used on nominally "dark" surfaces (`VersionFooter`, `AIHeader`, `DashboardPlaceholder`). If those surfaces are actually dark, the text may be low-contrast. This sprint only fixed the comments (fixing the value is a redesign, out of scope). Flag for the accessibility pass (roadmap T5.5) to check the real rendered contrast on those 3 surfaces. | Medium | Logged; belongs to Phase 5. |
| R4 | **`BackendAuthFlowVerificationTest` fails in any no-network environment** and is in `src/test` (runs with the normal unit-test task), not quarantined. It is *documented* as manual-only but nothing enforces that, so `./gradlew test` is red by default off-network. Not caused by this sprint; worth moving to a tagged/`@Ignore`-by-default or a separate source set in a later chore. | Low | Pre-existing; noted for a future cleanup. |
| R5 | **`Type.kt` shows as a large diff** (~168 lines) because it was restructured. Every `RojanTypography` style is individually verified unchanged in size/line-height/weight-value; the churn is the new declarations + per-style `fontFamily`/`fontWeight` reference swaps + expanded KDoc. Reviewers should diff with whitespace ignored. | Low | Expected; called out for review. |

---

## Commit

Staged scope (`git diff --cached --stat`):

```
 CLAUDE.md                                          |  53 ++-
 .../customer/CustomerThemeScreenshotTest.kt        |  95 -----   (deleted)
 .../customer/hometheme/HomeBackgroundTheme.kt      |  18 +-
 .../screens/customer/hometheme/HomeColors.kt       |   8 +-
 .../customer/theme/CustomerBackgroundTheme.kt      | 246 ------   (deleted)
 .../screens/customer/theme/CustomerColors.kt       |  44 ---   (deleted)
 .../screens/customer/theme/CustomerGlassTheme.kt   | 231 -----   (deleted)
 .../screens/customer/theme/CustomerIconTheme.kt    | 116 ----   (deleted)
 .../customer/theme/CustomerTextFieldTheme.kt       | 146 ----   (deleted)
 .../designlab/ui/background/WarmBackground.kt      |  15 +-
 .../ai/rojan/designlab/ui/theme/RojanTokens.kt     |  64 ++-
 .../main/java/ai/rojan/designlab/ui/theme/Theme.kt |  10 +
 .../main/java/ai/rojan/designlab/ui/theme/Type.kt  | 170 ++++----
 docs/uiux/BASELINE_CHANGELOG.md                    | new
 docs/uiux/ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md    | new
 docs/uiux/ROJAN_UI_POLISH_SPRINT1_REPORT_v1.md     | new
 docs/uiux/ROJAN_UIUX_PRO_MAX_AUDIT_v1.md           | new
```

Scope confirmation: **no backend files · no API files · no database files ·
no `build.gradle*` / manifests / resources · no other modules · no
navigation · no ViewModels.** The unrelated pre-existing edit to
`manager/data/BackendAppointmentRepository.kt` is excluded.

Proposed message: `feat(ui): establish premium design foundation`

**Status: staged, not committed — awaiting explicit go-ahead** (per
`CLAUDE.md`: "Committing or pushing git — always ask first").

---

*End of Sprint 1 report. Stopping here. Awaiting authorization before Sprint 2.*
