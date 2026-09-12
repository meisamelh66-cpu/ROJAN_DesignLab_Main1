# Customer Profile Screen — Redesign Review

**Date:** 2026-09-09 · **Scope:** the Customer Profile hub (`screens/profile/ProfileScreen.kt`, route `PROFILE`) only. No other screen touched.
**Not committed.**

---

## 1. File changed (1)

`screens/profile/ProfileScreen.kt` — rewritten, visual only. Public signature (all 12 params:
`authViewModel`, `onBackClick`, `onBeautyDnaClick`, `onAppointmentsClick`, `onFollowedSalonsClick`,
`onFavoritesClick`, `onWalletClick`, `onCouponsClick`, `onMembershipClick`, `onLoyaltyClick`,
`onReviewsClick`, `onBeautyTimelineClick`, `onLogoutClick`) is byte-identical.

**Behaviour preserved verbatim:** the displayed name still reads `authViewModel.currentDisplayName`,
the contact fields still read `authViewModel.currentUser` (`phoneNumber`, `email`), and **every one of
the 12 callbacks is invoked exactly where it was** — each menu row calls its `on…Click`, back calls
`onBackClick`, and logout calls `onLogoutClick()` unchanged (now on the dialog's confirm). No
ViewModel, repository, API, auth/session logic, navigation route, or data model is touched.

## 2. Before → After

| | Before | After |
|---|---|---|
| Shell | floating `GlassBackButton` orb + bare `LazyColumn` | `CustomerScaffold` (title "حساب کاربری", flat 56dp top bar, outlined back, 1px hairline) |
| Identity | **88dp disc** filled with `HomeColors.Glow` violet at 25% + **filled** `Icons.Filled.Person` 44dp, name in `HeroTitle` 32sp, centred | slim row: name in `Display` 26sp (left/RTL) + a **60dp flat circle** (4.5% fill + 9% hairline) showing the name initial; no glow, no filled icon, no oversized disc |
| Personal info | `HomeGlassSurface` card (✦ sparkle corners, metallic border, 7-pass glow) with `RtlListRow`s, every icon tinted violet `HomeColors.Glow`, phone's "تایید شده" in violet | one flat divided `RefSurface`: phone row (outlined `Phone`, "تایید شده" badge in **rose-gold** — the screen's single accent) + email row (outlined `MailOutline`) when present; `اطلاعاتی ثبت نشده است` fallback if both are absent |
| Menu | **7 sections × 1 row each**, each row its own glass `HomeGlassSurface` + `rojanEnterAnimation` stagger, filled violet icons, `RtlSectionHeader` between | **3 grouped `RefSurface` blocks** — «فعالیت من» (نوبت‌ها · بیوتی دی‌ان‌ای · تاریخچه زیبایی · نظرات من), «سالن‌های من» (علاقه‌مندی‌ها · دنبال‌شده‌ها), «امکانات حساب» (کیف پول · کدهای تخفیف · عضویت · امتیازات وفاداری) — divided `RefListRow`s, all icons **outlined** and neutral (`TextMuted`), forward chevron on the left |
| Logout | a glass menu row identical to the rest, filled `Icons.Filled.Logout`, violet tint, **fires immediately** | a calm flat bottom action, outlined `Icons.AutoMirrored.Outlined.Logout`, label in the muted rose-red error token (`RojanErrorText`), gated by a **`CustomerConfirmDialog`** ("از حساب کاربری خود خارج می‌شوید؟" · انصراف / خروج) |
| Motion | per-row enter stagger (`rojanEnterAnimation`) | none |
| Icons | all `Icons.Filled.*` | all `Icons.Outlined.*` / `AutoMirrored.Outlined.*` |

The 7-section-headers-with-one-row-each layout was the heaviest offender against "minimal hierarchy";
folding them into 3 labelled groups keeps every destination while removing ~10 lines of chrome. No
menu item was removed — all 10 navigation destinations plus logout remain.

## 3. Foundation usage

`CustomerScaffold` · `CustomerSectionLabel` · `RefSurface` · `RefListRow` · `RefRowDivider` ·
`CustomerConfirmDialog` · tokens `CustomerAccent` / `CustomerScreenMargin` / `CustomerCardShape` /
`CustomerSurfaceFill` / `CustomerHairline` · `rojanPressable`. Error token `RojanErrorText` for the
logout label.

**Screen-local:** `IdentityHeader`, `MenuGroup`, `RowIcon`, `LogoutRow`, and the `ProfileMenuItem`
data class — thin composition helpers over the foundation, no new tokens.

**"Empty / error / loading states using CustomerStates":** this screen has **no async load** — it
renders synchronously from two `StateFlow`s that the session already populated, so there is no
loading or error path and `CustomerLoadingState` / `CustomerErrorState` don't apply. The one real
edge — `currentUser` with neither phone nor email — is handled inline with a calm
`اطلاعاتی ثبت نشده است` row in the personal-info card. (The route is auth-guarded; a fully-null
user is not reachable in normal flow.)

**"Use CustomerConfirmDialog if needed":** used — logout is the one destructive action on the
screen. The dialog only *gates* `onLogoutClick()`; the logout itself (`authViewModel.logout()` +
navigate to `EXPLORE`, in the nav graph) is unchanged.

## 4. Validation

**Device:** Samsung Galaxy A72 (SM-A725F), Android 14, account "گیتا" (+989164987585).

| Task | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL**, exit 0 |
| `:app:installCustomerDevDebug` | **Installed on 1 device**, exit 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL**, exit 0 — zero findings for `ProfileScreen.kt` |
| Samsung A72 verification | **PASS** (see §5) |

## 5. Device verification

Reached via the Home profile-avatar (route `PROFILE`), signed in as "گیتا".

| Screenshot | What it shows |
|---|---|
| `docs/design-review/customer/PR_after_top.png` | Flat "حساب کاربری" top bar + hairline; identity row — "گیتا" in `Display 26sp` + 60dp flat initial circle; «اطلاعات شخصی» card — phone `+989164987585` with outlined phone icon and rose-gold "تایید شده" (no email row — گیتا has none); «فعالیت من» group (4 divided rows, outlined icons, left chevrons). No glow disc, no sparkle cards, no violet. |
| `docs/design-review/customer/PR_after_bottom.png` | «سالن‌های من» (2 rows) + «امکانات حساب» (4 rows) groups; calm flat "خروج از حساب" action in rose-red at the bottom. |
| `docs/design-review/customer/PR_after_logout_dialog.png` | `CustomerConfirmDialog` — flat dark-navy card, hairline border, no glass; RTL buttons: "انصراف" text action (right) + solid rose-gold "خروج" (left). |

**Interaction checks:** "انصراف" dismisses the dialog and the session is intact (Profile still shows
"گیتا" / verified phone). A menu row (`کیف پول` → `onWalletClick`) and `سالن‌های دنبال‌شده` →
`onFollowedSalonsClick` both navigate to their existing destinations — callback wiring unchanged.
Full logout→login was already exercised end-to-end in the Auth task (`authViewModel.logout()` →
navigate to `EXPLORE`); this screen's dialog calls that same unchanged callback on confirm. No crash.

**Stop after this screen. No other screen redesigned. No commit.**
