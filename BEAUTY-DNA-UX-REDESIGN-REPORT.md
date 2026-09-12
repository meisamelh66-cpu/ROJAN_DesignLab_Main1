# ROJAN Customer — Beauty DNA UX Redesign

**Date:** 2026-09-10
**Scope:** Customer Android app only (`screens/profile/BeautyDnaScreen.kt` + two title-only references). No backend changes.
**Status:** ✅ Complete. Compile + lint both green, lint warning count unchanged at baseline (94).

---

## 1. Title rename

The visible Persian transliteration "دی ان ای" / "دی‌ان‌ای" is replaced with the Latin "DNA" everywhere a user sees it:

| Location | Before | After |
|---|---|---|
| `BeautyDnaScreen.kt` — `CustomerScaffold` title | `"بیوتی دی‌ان‌ای"` | `"بیوتی DNA"` |
| `ProfileScreen.kt` — activity-menu entry that opens this screen | `"بیوتی دی‌ان‌ای من"` | `"بیوتی DNA من"` |

One more occurrence exists in the codebase — a default parameter string in `ui/components/ai/RojanBeautyDnaSignature.kt` ("امضای بصری بیوتی دی‌ان‌ای"). That composable has **zero call sites** in Customer scope (confirmed via grep, consistent with the exclusion list in the prior RTL audit) — it is dead code, never rendered, so it was left untouched rather than edited for a string no user can ever see.

Mixed Persian+"DNA" renders correctly under the shared bidi-aware `Text` (`ui/text/RojanText.kt`): the string's first strong character is Persian, so the whole label resolves RTL, and the embedded Latin "DNA" run displays left-to-right in place per the Unicode Bidi algorithm — this is exactly how "DNA" already reads in ordinary Persian UI copy, no special-casing needed.

---

## 2. Accordion redesign

### Before
`BeautyDnaScreen` rendered a flat `LazyColumn` of 6 always-expanded option groups (نوع مو / رنگ مو / سابقه خدمات مو / نوع پوست / نگرانی‌های پوستی / سبک مورد علاقه), each a full `RefSurface` with every option row visible — the whole page was several screens tall on first open, all 6 groups competing for attention.

### After
The three domains (`مو` / `پوست` / `ناخن`) are now `AccordionSection`s — one `RefSurface` per domain, each with a tappable header and collapsible body:

```
DNA

▼ مو  ·  صاف، مشکی
   نوع مو
     [options...]
   رنگ مو
     [options...]
   سابقه خدمات مو
     [options...]

▶ پوست  ·  نرمال، آکنه

▶ ناخن
```

- **Default state:** `مو` (the first section) starts expanded; `پوست` and `ناخن` start collapsed — via `remember { mutableStateOf<DnaSection?>(DnaSection.HAIR) }`.
- **Single-expansion:** one shared `expandedSection: DnaSection?` state drives all three headers' `onToggle`; setting it to a new section implicitly closes whichever was open (tapping the open section's own header collapses it to `null`, matching standard accordion behaviour of "tap again to close").
- **Opening one closes others:** enforced structurally — `expanded = expandedSection == DnaSection.X` for each of the three, backed by one variable, so two sections can never be expanded simultaneously.
- **Selected values stay visible in the header:** each header shows a rose-gold (`CustomerAccent`) one-line summary of every currently-selected value in that domain — `hairType` + `hairColor` + `treatmentHistory` joined with "، " for مو, `skinType` + `concerns` for پوست, `stylePreference` for ناخن — computed live from `viewModel.profile` and shown **whether the section is expanded or collapsed**, so a user never has to open a section to confirm what they already picked. No summary line renders when nothing is selected in that domain.
- **Smooth open/close:** `Modifier.animateContentSize()` on each accordion body animates the height change instead of an abrupt jump-cut.
- The 3 previously-separate `SectionHeader("مو")`/`SectionHeader("پوست")`/`SectionHeader("ناخن")` items are gone — each domain name now lives in its own accordion header instead of a standalone label above 2-3 stacked cards.

### ViewModel / API — untouched
Every selection still calls the exact same methods with the exact same payloads: `viewModel.updateHair(profile.hair.copy(...))`, `viewModel.updateSkin(profile.skin.copy(...))`, `viewModel.updateNails(NailProfile(stylePreference = it))`. `SingleSelectRows`/`MultiSelectRows`/`OptionRow`/the deselect-on-retap single-select behaviour/the `toggle()` multi-select helper are all byte-identical to before, just re-parented under `AccordionSection` → `DnaOptionGroup` instead of directly under `LazyColumn` → `OptionGroup`. `BeautyProfileViewModel`, `BeautyProfileRepository`, `BeautyProfileViewModelFactory`, and the screen's public signature (`customerId`, `beautyProfileRepository`, `onBackClick`, `viewModel`) are all unchanged. No API, repository, or navigation route was touched.

---

## 3. Visual style — Quiet Luxury conformance

- **`CustomerScaffold`** — kept as the screen shell (title now "بیوتی DNA").
- **`RefSurface`** — each accordion section is still exactly one `RefSurface` (previously one per option group; now one per domain, holding all of that domain's option groups internally, divided by `RefRowDivider`).
- **`CustomerSectionLabel`** — the standalone `SectionHeader` composable that used `CustomerSectionLabel` for مو/پوست/ناخن labels is removed, since those names now live inside each accordion's own header row (`RojanTypography.Body` + `FontWeight.SemiBold`, `HomeColors.TextPrimary`) — visually equivalent role, just relocated into the tappable header per the accordion pattern. Sub-group labels inside each expanded section (نوع مو, رنگ مو, …) keep the same muted `Caption` treatment as before.
- **Rose-gold accent** — `CustomerAccent` is used for the selected-option check icon (unchanged) and now also for the header selection-summary text, giving the accent a second, consistent role: "this is what you've chosen."
- **RTL compliance** — the accordion header follows the same list-row convention audited and fixed earlier this session (`RTL-COMPLIANCE-AUDIT-REPORT.md`): expand/collapse indicator first in code (→ physical left), a right-anchored `Column(horizontalAlignment = Alignment.End)` for title+summary. The indicator itself uses `Icons.Outlined.KeyboardArrowDown` when expanded and the already-audited `Icons.AutoMirrored.Outlined.KeyboardArrowLeft` when collapsed (same "chevron" icon `RefListRow` uses elsewhere) rather than a rotating arrow, so its direction is guaranteed correct under the app's ambient-LTR + explicit-RTL-composition architecture without relying on a rotation-angle guess.

---

## 4. Spacing & touch targets

- Accordion header: `Modifier.heightIn(min = RojanDimens.MinTouchTarget)` (48dp) + `padding(horizontal = SpaceMD, vertical = SpaceMD)` — full 48dp tap target guaranteed even for a header with no summary line (title-only).
- Header is one `rojanPressable` spanning the full row width, not just the icon — the whole header (icon + title + summary) is tappable, not a small icon-only hit-target.
- Option rows unchanged: `RefListRow` already enforces `heightIn(min = MinTouchTarget)` — verified still true, untouched.
- Sub-group label padding standardized to `padding(horizontal = SpaceMD, vertical = SpaceSM)` for a consistent 8dp/16dp rhythm between the header divider and the first option row.
- Vertical spacing between the three accordion cards: `SpaceMD` (16dp), matching the previous inter-group spacing exactly — no visual rhythm change at the top level.

---

## 5. Validation

```
:app:compileCustomerDevDebugKotlin  → BUILD SUCCESSFUL
:app:lintCustomerDevDebug           → BUILD SUCCESSFUL, 94 warnings (unchanged from session baseline, 0 new findings, 0 errors, 0 findings in BeautyDnaScreen.kt)
```

No `assemble`/instrumented run was performed or required per the task's "no device required" instruction.

## 6. Files changed

- `app/src/main/java/ai/rojan/designlab/screens/profile/BeautyDnaScreen.kt` — accordion redesign, title rename.
- `app/src/main/java/ai/rojan/designlab/screens/profile/ProfileScreen.kt` — one string: menu-entry label rename ("بیوتی دی‌ان‌ای من" → "بیوتی DNA من").

Nothing else touched. No backend, ViewModel, repository, or navigation-route file was edited. Nothing committed.
