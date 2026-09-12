# Customer Auth Screen — Redesign Review (P0)

**Date:** 2026-09-09 · **Scope:** the Customer authentication screen (`screens/auth/AuthScreen.kt`, route `AUTH`) only. No other screen touched.
**Not committed.**

---

## 1. File changed (1)

`screens/auth/AuthScreen.kt` — rewritten, visual only. Public signature (`authViewModel`, `onBackClick`, `onExistingUserAuthenticated`) byte-identical.

**Auth flow preserved verbatim:** every read of / call into `AuthViewModel` — `sessionState`, `otpStep` (`EnteringPhone` / `AwaitingCode`), `errorMessage`, `isSubmitting`, `editPhoneNumber()`, `requestOtp(phoneNumber)`, `verifyOtp(code, fullName)`, `resendOtp()` — is unchanged, in the same places. The `LaunchedEffect(sessionState) { … onExistingUserAuthenticated() }` and the two-step-on-one-screen structure are unchanged. The screen stays navigation-agnostic. **No** ViewModel, OTP logic, API call, navigation route, session restore, or repository touched.

## 2. Before → After

| | Before | After |
|---|---|---|
| Shell | `GlassBackButton` orb + `verticalScroll` `Column` | `CustomerScaffold` (title "ورود به روژان", flat 56dp top bar, no step indicator) — back on `EnteringPhone` → `onBackClick`, on `AwaitingCode` → clear code + `editPhoneNumber()` (same as before) |
| Headline | **`Text("سلام 🌸")`** `HeroTitle` — **emoji brand mark** | `"ورود به روژان"` in `Display.copy(26sp)` — no emoji |
| Subtitle | step-dependent `Body` / `TextSecondary` | **same strings**, same style |
| Field container | one **32dp `HomeGlassSurface`** card (metallic border + ✦) wrapping an `AnimatedContent` | no card — fields sit directly on the ground |
| Fields | **`HomeTextField`** — glass fill, violet (`HomeColors.Glow`) cursor / focus / label | screen-local **`AuthField`** — muted `Caption` label above a flat `RefSurface`-style box (4.5% fill + 9% hairline, 14dp), `BasicTextField`, **rose-gold cursor** (`CustomerAccent`), muted placeholder, content-direction text |
| "ارسال مجدد کد" | `TextButton`, label in **violet** `HomeColors.Glow` | a 48dp centred text button, label in **rose-gold**, 40% alpha while submitting |
| Error | `Caption` in `RojanErrorText`, `liveRegion` | **same** — `Caption` in `RojanErrorText` (the app's error token, a muted rose-red — not the vivid `HomeColors.Magenta` the audit flagged elsewhere), `liveRegion = Polite`; now sits inline under the fields on the flat ground |
| Primary CTA | **`PremiumButton`** magenta→pink **gradient pill** (50dp), inside a **`blur(36.dp)` radial violet `HomeColors.Glow` glow halo** + a 14dp drop `shadow` + a white top-sheen `verticalGradient` | solid rose-gold **`RefPrimaryButton`** (12dp radius, 52dp, `#E0A67A`), **no** glow, shadow, or sheen; 40% alpha while `isSubmitting` |
| Motion | `AnimatedContent` between steps | none — plain `when(otpStep)` (the step change is network-driven, not instant) |
| Keyboard | `verticalScroll` only | `verticalScroll` + `imePadding()` — the CTA stays above the keyboard when a field is focused |

Removed imports: `AnimatedContent`, `HomeGlassSurface`, `HomeTextField`, `PremiumButton`, `GlassBackButton`, `HomeBackgroundTheme`, `RojanShapes`, `Brush`, `blur`, `BlurredEdgeTreatment`, `shadow`, `Color`, `LocalTextStyle`, `TextButton`.

## 3. Foundation usage

`CustomerScaffold` · `RefPrimaryButton` · tokens `CustomerAccent` / `CustomerScreenMargin` / `CustomerCardShape` / `CustomerSurfaceFill` / `CustomerHairline` · `rojanPressable`.

**One screen-local primitive:** `AuthField` — a flat outlined text field. The foundation has no `CustomerTextField` (the migration task's scope didn't include one; audit item A-7 flagged that Salon List and Search also need a flat field). `AuthField` should be promoted to `customer.components` when those two screens are redesigned.

**"calm error/loading states using CustomerStates":** Auth's error is inherently *inline and recoverable* (fix the number / re-enter the code and retry on the same form), and its loading is just a disabled form — neither is a full-screen state, so the full-screen `CustomerLoadingState` / `CustomerErrorState` don't apply here. The same calm visual language is used: a muted `Caption` error with a live-region announcement, and the CTA's built-in 40%-alpha disabled state while submitting.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F).

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | **Installed on 1 device**, exit 0 (device dropped ADB once after compile; reconnected, re-run clean) |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `AuthScreen.kt` |
| Samsung A72 screenshot verification | **PASS** (see §5) |

## 5. Device verification

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14. Reached `AuthScreen` (route `AUTH`) by logging out
account "گیتا" from Profile, then tapping a guest-guarded destination ("نوبت‌های من") — which routes to login.
End-to-end auth flow was exercised against the **live** backend (`api.rojanai.ir`); a real OTP SMS was sent
and the session was re-authenticated (state fully restored — no regression).

| Step | Screenshot | Result |
|---|---|---|
| Phone entry | `AU06_appointments_tap.png` | Flat 56dp top bar "ورود به روژان" + outlined back + hairline; plain `Display 26sp` headline; `Body`/`TextSecondary` subtitle; muted `Caption` "شماره موبایل" label over a flat outlined field (4.5% fill + 9% hairline, 14dp); solid rose-gold `RefPrimaryButton` "ارسال کد تایید". **No** emoji, glass card, violet, gradient, or glow. |
| Phone typed | `AU07_phone_typed.png` | Content-direction digits, **rose-gold cursor**; `imePadding()` keeps the CTA above the IME. |
| OTP requested | `AU08_otp_requested.png` | `requestOtp("09164987585")` fired → real SMS sent → step advanced to `AwaitingCode`; subtitle now shows "کد ارسال‌شده به +989164987585 را وارد کنید"; "کد تایید" + "نام شما (اختیاری)" flat fields; rose-gold "تایید و ورود"; **"ارسال مجدد کد" now in rose-gold** (was violet). |
| Code entry | `AU09_code_typed.png` | Real OTP entered, rose-gold cursor. |
| Verify | `AU10_after_verify.png` | `verifyOtp(code, "")` → authenticated; `LaunchedEffect(sessionState)` fired `onExistingUserAuthenticated()` → navigated to the originally-guarded destination. No error line, no crash. |
| Session restored | `AU11_home_after.png` | Profile shows "گیتا" · "+989164987585" · "تایید شده". Session identical to pre-test — **no auth / navigation regression**. |

Back-navigation behaviour verified implicitly: on `AwaitingCode` the top-bar back clears the code and calls
`editPhoneNumber()` (unchanged); on `EnteringPhone` it calls `onBackClick()`.

**Stop after this screen. No commit.**
