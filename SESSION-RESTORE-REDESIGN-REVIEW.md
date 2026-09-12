# Session Restore — Redesign Review (P0)

**Date:** 2026-09-09 · **Scope:** the session-restore / "loading your session" entry screen **only**. No other screen touched.
**Not committed.**

---

## 1. What this screen is

The transient screen shown on an **authenticated cold start** while `authViewModel.restoreSession(personId)` validates the stored session against the backend (`GET /api/v1/auth/me`). It renders for ~1–2s between the Splash and Home.

- **Composable:** `RestoringSessionContent()` — a `private @Composable` in `navigation/RojanNavGraph.kt`.
- **Call sites (unchanged):** `SessionRestoreState.Loading -> RestoringSessionContent()` and, inside the `Restored` branch, `if (isRestoringSession) { RestoringSessionContent(); return }`.
- Distinct from `screens/splash/SplashScreen.kt` (the pre-session logo splash) — that screen was not in scope and is not modified.

## 2. Before → After

| | Before | After |
|---|---|---|
| Ground | **`PremiumBackground`** — the full-bleed AI candy-pink salon photo (`R.drawable.bg_master_luxury_salon`) + a `RojanAIGlow` radial | flat **`HomeBackgroundTheme`** dark navy — no photo, no radial glow |
| Identity | none | the **ROJAN monogram** (`R.mipmap.ic_launcher_foreground`, 56dp — the same mark `SplashScreen` shows) |
| Motion | **`PremiumLoadingBar`** — a sweeping warm-amber glow gradient bar | one **20dp rose-gold (`CustomerAccent` `#E0A67A`) indeterminate `CircularProgressIndicator`**, 2dp stroke |
| Caption | `stringResource(R.string.status_restoring_session)` in `RojanLuxuryCaption`, raw `fontSize = 13.sp` | **same string**, `RojanTypography.Caption` / `HomeColors.TextMuted` |
| Layout | `Box(fillMaxSize, center) { Column { caption · Spacer(12) · bar } }` | `Column(fillMaxSize, `CustomerScreenMargin`, center) { monogram · Spacer(LG) · ring · Spacer(MD) · caption }` |

Meets every reference rule: dark navy · ROJAN identity · rose-gold accent · flat (no card) · minimal motion (a single small ring) · no sparkle · no glow · no gradient · **no AI photography** · no oversized cards.

## 3. Files changed (1)

`navigation/RojanNavGraph.kt`:
- **`RestoringSessionContent()` body** — rewritten (visual only).
- **Imports** — removed `PremiumBackground`, `PremiumLoadingBar`, `RojanLuxuryCaption`, `sp`, and the now-unused `Box` / `width` (all were used only by this function); added `Image`, `Arrangement`, `padding`, `size`, `CircularProgressIndicator`, `ContentScale`, `painterResource`, `HomeBackgroundTheme`, `HomeColors`, `RojanTypography`, `RojanDimens`, and the foundation's `CustomerAccent` / `CustomerScreenMargin`.

**Not changed:** the `SessionRestoreState` `when` block, the `LaunchedEffect(state)` that calls `authViewModel.restoreSession`, `isRestoringSession` bookkeeping, `startDestination` capture, every `composable(...)` route, and all navigation callbacks. No ViewModel, repository, auth logic, or API contract touched. `PremiumBackground` / `PremiumLoadingBar` themselves are untouched (still used by Manager/Stylist surfaces).

## 4. Foundation usage

Uses the new Customer foundation (`screens/customer/components/`): `CustomerAccent`, `CustomerScreenMargin`. (The scaffold/state components don't apply here — this is a full-screen centred moment, not a top-bar screen.)

## 5. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا".

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | Installed on 1 device, **BUILD SUCCESSFUL**, exit 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `RojanNavGraph.kt` |

**On-device (burst-captured cold start):**
- `SR01_session_restore_after.png` — dark navy ground, centred ROJAN monogram, a small rose-gold spinner mid-arc, "در حال بازیابی نشست شما..." caption. No photo, no glow bar.
- The very next frames show the redesigned **Home** with real data ("سلام گیتا جان", 2 real salons) — the session was restored normally; no auth or navigation regression.
- No crash.

## 6. Remaining note

The monogram asset is the app's glossy 3D launcher icon (`ic_launcher_foreground`). It is the established ROJAN identity mark and is used identically on `SplashScreen`, so it's kept here for consistency. A flatter monogram is an **app-icon** concern, out of scope for this screen and flagged in `CUSTOMER-VISUAL-DEBT-AUDIT.md`.

**Stop after this screen. No other screen redesigned. No commit.**
