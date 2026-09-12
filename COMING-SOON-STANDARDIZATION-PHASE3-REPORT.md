# Coming-Soon Standardization — Phase 3

**Date:** 2026-09-10 · **Scope:** Customer coming-soon placeholder screens only ·
**Visual + consolidation only — no ViewModel / API / route change.** **Not committed.**

Implements audit item **P1-8** (`CUSTOMER-UI-COMPONENT-STANDARDIZATION-AUDIT.md` §7) — the last
open item in the Customer UI standardization program.

---

## Verdict: seven byte-identical placeholder screens collapsed into one.

All seven "coming soon" surfaces — **Wallet, Coupons, Membership, Loyalty, My Reviews, Waitlist,
Beauty Timeline** — now render through a single **`CustomerComingSoonScreen`** built on
`CustomerScaffold` + the flat `CustomerEmptyState`. The seven old files are deleted; every route is
preserved and now points at the shared screen with only its title string varying.

---

## Audit of the "before"

The seven screens were **structurally identical** — each was:

```kotlin
@Composable
fun WalletScreen(onBackClick: () -> Unit) {
    HomeBackgroundTheme {
        LazyColumn(Modifier.fillMaxSize().padding(RojanDimens.SpaceMD), …) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("کیف پول", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }
            item { RojanComingSoonState() }
        }
    }
}
```

Differences between the seven: the **title string**, and whether the outer container was `LazyColumn`
or `Column` (`Membership`, `Waitlist`). `Waitlist` also had a stray `padding(vertical = SpaceMD)` on
its title. Every one carried the same three legacy dependencies:

| Legacy element | Problem |
|---|---|
| `GlassBackButton` | floating glass pill, not the flat quiet-luxury top-bar arrow; no title bar, no hairline |
| `Text(style = RojanTypography.HeroTitle)` | inline giant heading in the scroll body instead of a real 56dp app-bar title |
| `RojanComingSoonState()` | wraps itself in `GlassSurface` — a light glass card floating on the dark canvas; uses the `RojanText*` family and `PremiumButton`; not the flat `CustomerEmptyState` |

Six were reachable from Profile's menu (`WALLET`, `COUPONS`, `MEMBERSHIP`, `LOYALTY`, `MY_REVIEWS`,
`BEAUTY_TIMELINE`). `WAITLIST` (wrapped in `CustomerAccessGuard`) is currently **unreachable** —
`AppointmentsScreen.onWaitlistClick` has a `{}` default and no call site, its UI entry point having
been removed when the feature was gated. This phase leaves that wiring exactly as it found it.

**Why these are placeholders (unchanged by this phase):** none of the seven has a backend capability
— they were demo/in-memory state (`DemoWalletRepository`, `DemoCouponRepository`, …) gated out under
"Production Data Integrity Phase 1 / no mock data in production flows". The entry points stay
reachable so the customer sees the feature is planned; the content is a coming-soon state until a
real endpoint exists.

---

## Implementation

### New — `screens/customer/components/CustomerComingSoonScreen.kt`

```kotlin
@Composable
fun CustomerComingSoonScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    body: String = "این بخش به‌زودی با اطلاعات واقعی فعال می‌شود",
    icon: ImageVector = Icons.Outlined.Schedule,
) {
    CustomerScaffold(title = title, onBackClick = onBackClick, modifier = modifier) {
        CustomerEmptyState(title = "به‌زودی", body = body, icon = icon)
    }
}
```

- `CustomerScaffold` brings the approved chrome: flat 56dp top bar, RTL-centred title, outlined
  back arrow on the content margin, 1px bottom hairline, `HomeBackgroundTheme`. No `GlassBackButton`,
  no inline `HeroTitle`.
- `CustomerEmptyState` (from `CustomerStates.kt`) is the flat quiet-luxury empty state — outlined
  40dp icon tinted `HomeColors.TextMuted`, `CardTitle` "به‌زودی", `Caption` body, centred, with a
  `liveRegion = Polite` announcement. No `GlassSurface`, no glow, no gradient card.
- It is the sole `ColumnScope` child of the scaffold content slot; `CustomerCenteredState`'s
  `fillMaxSize()` centres it in the space between the bar and the nav-bar inset.
- Adds **no** design-system token and edits **no** shared component.

### Deleted (7 files)

`screens/profile/WalletScreen.kt` · `CouponsScreen.kt` · `MembershipScreen.kt` · `LoyaltyScreen.kt`
· `MyReviewsScreen.kt` · `WaitlistScreen.kt` · `BeautyTimelineScreen.kt`

### `RojanNavGraph.kt`

- Removed 7 imports (`…screens.profile.WalletScreen`, …), added 1
  (`…screens.customer.components.CustomerComingSoonScreen`).
- 7 `composable` bodies now call `CustomerComingSoonScreen(title = "…", onBackClick = { navController.popBackStack() })`:

  | Route | Title |
  |---|---|
  | `WALLET` | `کیف پول` |
  | `COUPONS` | `کدهای تخفیف` |
  | `MEMBERSHIP` | `عضویت` |
  | `LOYALTY` | `امتیازات وفاداری` |
  | `MY_REVIEWS` | `نظرات من` |
  | `BEAUTY_TIMELINE` | `تاریخچه زیبایی` |
  | `WAITLIST` | `لیست انتظار من` |

- **Every route string, `arguments`, `enterTransition`/`exitTransition`, nested-graph placement, and
  the `CustomerAccessGuard` around `WAITLIST` are unchanged.** `onBackClick` is the same
  `{ navController.popBackStack() }` lambda as before. No navigation behaviour changed.

`RojanComingSoonState` stays in the codebase — it is still used by the Home dashboard section stubs
(`NearbySalons`, `PopularServices`, `PromotionsSection`, …), which are out of this phase's scope.

---

## Files changed

| File | Change |
|---|---|
| `screens/customer/components/CustomerComingSoonScreen.kt` | **new** — the shared screen (~30 lines) |
| `navigation/RojanNavGraph.kt` | −7 imports / +1; 7 `composable` bodies re-pointed at the shared screen |
| `screens/profile/{Wallet,Coupons,Membership,Loyalty,MyReviews,Waitlist,BeautyTimeline}Screen.kt` | **deleted** (7 files, ~310 lines) |

Net: **−7 files, ~−280 lines.** No ViewModel, repository, API, DTO, route, or shared component touched.

---

## Requirements check

| Requirement | Status |
|---|---|
| Use `CustomerScaffold` | ✅ — the shared screen's shell |
| Use `CustomerStates` style | ✅ — `CustomerEmptyState` (flat, no card) |
| Follow Quiet Luxury design | ✅ — flat 56dp bar, RTL arrow, hairline, outlined icon, no glass/glow |
| Remove old glass/glow/legacy styling | ✅ — `GlassBackButton`, inline `HeroTitle`, `RojanComingSoonState`/`GlassSurface` all gone from these routes |
| Preserve routes | ✅ — all 7 route strings + the `WAITLIST` access guard unchanged |
| No ViewModel/API changes | ✅ — none of the 7 had a ViewModel; none added |
| No navigation breaking changes | ✅ — same `popBackStack` back lambda, same graph structure, same transitions |

---

## Validation

| Gate | Result |
|---|---|
| `:app:compileCustomerDevDebugKotlin` | **BUILD SUCCESSFUL** · EXIT 0 |
| `:app:lintCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `0 errors, 94 warnings` — identical to baseline. **Zero findings reference `CustomerComingSoonScreen.kt` or `RojanNavGraph.kt`.** |
| `:app:assembleCustomerDevDebug` | **BUILD SUCCESSFUL** · EXIT 0 · `app-customer-dev-debug.apk` (~33.9 MB, ~0.3 MB smaller — 7 classes removed) |

---

## Device test — A72

**Device:** Samsung Galaxy A72 (`RZ8R81WPS2J`), Android 14, 1080×2400 · **Build:** this branch ·
**Account:** authenticated customer ("گیتا") · **Crash log:** `adb logcat -b crash` →
**`FATAL EXCEPTION` count = 0** across the run.

| Screen | Route | Result |
|---|---|---|
| **Wallet** (`کیف پول`) | `WALLET` | Profile → امکانات حساب → opens `CustomerComingSoonScreen`: flat top bar, RTL "کیف پول" title, outlined back arrow, hairline; centred outlined clock icon + "به‌زودی" + body. No glass/glow. Back arrow → Profile. ✅ `01_wallet.png` |
| **Coupons** (`کدهای تخفیف`) | `COUPONS` | Same shell, title "کدهای تخفیف". Back → Profile. ✅ `02_coupons.png` |
| **Membership** (`عضویت`) | `MEMBERSHIP` | Same shell, title "عضویت". Back → Profile. ✅ `03_membership.png` |
| **Loyalty** (`امتیازات وفاداری`) | `LOYALTY` | Same shell, title "امتیازات وفاداری". Back → Profile. ✅ `04_loyalty.png` |
| **Beauty Timeline** (`تاریخچه زیبایی`) | `BEAUTY_TIMELINE` | Profile → فعالیت من → same shell, title "تاریخچه زیبایی". Back → Profile. ✅ `05_beauty_timeline.png` |
| **My Reviews** (`نظرات من`) | `MY_REVIEWS` | Same shell, title "نظرات من". Back → Profile. ✅ `06_reviews.png` |
| **Waitlist** (`لیست انتظار من`) | `WAITLIST` (guarded) | Verified by equivalence — the route is **currently unreachable via the UI** (`AppointmentsScreen.onWaitlistClick` has a `{}` default and no call site; the entry point was removed when the feature was gated, pre-dating this phase). Its `composable` body is the identical `CustomerComingSoonScreen(title = "لیست انتظار من", …)` call inside the **unchanged** `CustomerAccessGuard`; compiles + lint-clean. |

**6 of 7 routes walked on-device.** Each: title reads correctly in the 56dp top bar (not the scroll
body), outlined back arrow returns to the entry screen, flat quiet-luxury empty state, no glass/glow,
**no crash** (`FATAL EXCEPTION` count = 0). The 7th (`WAITLIST`) has no live UI entry point in this
build and is covered by code equivalence + the build gates.

---

## Follow-ups (not in this phase)

1. **Home dashboard section stubs** (`NearbySalons`, `PopularServices`, `PromotionsSection`,
   `RecommendedSalons`, `TopSpecialists`, `FollowedSalons`) still call `RojanComingSoonState` inside
   `CustomerHomeScreen`/`CustomerDashboardScreen`. They are *sections*, not screens — a separate
   decision (keep the section, or drop it until it has data).
2. **`RojanComingSoonState` / `RojanEmptyState` / `GlassSurface`** can be retired from the shared
   `ui/components/state/` layer once the dashboard stubs above move off them — part of the P2
   dead-code / legacy-component cleanup.
3. When a real backend capability lands for any of these seven, that route swaps
   `CustomerComingSoonScreen(...)` for the real screen — a one-line nav-graph change.

Nothing was committed.
