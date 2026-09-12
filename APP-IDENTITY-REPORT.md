# App Identity Fix — Release Blocker P0-4

**Date:** 2026-09-10 · **Scope:** the Customer app's user-visible display name only. No business
logic, screen, ViewModel, repository, `applicationId`, or package touched. **Not committed.**

> Note: `rojan-release-manager` is not a registered skill/agent in this environment. Work done
> directly.

---

## 1. How the app name is wired

`AndroidManifest.xml` sets `android:label="@string/app_name"` on `<application>` **and** on each
flavor's launcher `<activity>` (`.MainActivity` for Customer). The **string value** is resolved
per flavor via source-set overrides:

| Flavor | `app_name` source | Value (before) |
|---|---|---|
| **customer** | `app/src/main/res/values/strings.xml` (no `src/customer/` override exists) | **`ROJAN_DesignLab`** ← P0-4 |
| manager | `app/src/manager/res/values/strings.xml` (override) | `ROJAN Manager` |
| reception | `app/src/reception/res/values/strings.xml` (override) | `ROJAN Reception` |

So the Customer name lives in exactly one place, and changing it there affects **only** the
Customer app.

## 2. Inspection — every launcher label / user-visible name

| Location | What it is | User-visible? | Action |
|---|---|---|---|
| `app/src/main/res/values/strings.xml` → `app_name` | Customer launcher label, task switcher, App info, system permission dialogs, share targets | **Yes** | **CHANGED → `ROJAN AI`** |
| `app/src/main/AndroidManifest.xml` `<application android:label>` + `.MainActivity android:label` | both point at `@string/app_name` | Yes (via the string) | no change needed — resolves to the new value |
| `app/src/manager/…/strings.xml` `app_name` = `ROJAN Manager` | Manager app name | Yes (Manager only) | **untouched** |
| `app/src/reception/…/strings.xml` `app_name` = `ROJAN Reception` | Reception app name | Yes (Reception only) | **untouched** |
| No `values-fa/` / `values-en/` / `values-night/` `strings.xml` | — | — | nothing to localise; single-value resource |
| No Kotlin code references `R.string.app_name` | the name never renders inside the UI | — | — |
| In-app brand text (`SplashScreen.kt` "ROJAN AI", `VersionFooter.kt` "ROJAN AI", session-restore `contentDescription = "ROJAN AI"`) | shown on splash / footer | Yes | already `"ROJAN AI"` — consistent, no change |
| `@style/Theme.ROJAN_DesignLab` (`themes.xml` + 3 manifest refs) | a **style resource name** | **No** — never shown to a user | left as-is (renaming would churn manager/reception manifests for zero user benefit; noted below) |
| `components/AIHeader.kt:63` `text = "ROJAN"` | a legacy glass header (part of the pre-redesign component set; not in the redesigned Home/Explore) | Yes *if still rendered* | out of scope — this is a `"ROJAN"` vs `"ROJAN AI"` inconsistency, not the `ROJAN_DesignLab` blocker; flagged below |
| Doc-comment headers `* ROJAN DesignLab` in `GlassOrb.kt` / `PremiumLoadingBar.kt` | source comments | No | left as-is |

## 3. Change (1 file)

`app/src/main/res/values/strings.xml`:

```diff
- <string name="app_name">ROJAN_DesignLab</string>
+ <string name="app_name">ROJAN AI</string>
```

(plus a comment documenting the per-flavor override model and that `applicationId` is unchanged.)

`applicationId` / `namespace` = **`ai.rojan.designlab`** — **unchanged**. That is the immutable
Play package id, not a display name; it never appears in the launcher.

## 4. Verification

### Production build still succeeds — ✅

```
./gradlew :app:assembleCustomerProductionRelease :app:lintCustomerProductionRelease
  → BUILD SUCCESSFUL in 7m 47s
  → lint: 93 warnings / 0 errors  (unchanged from before)
```

### The compiled APK now shows "ROJAN AI" — ✅

```
aapt2 dump badging  app-customer-production-release.apk
  package: name='ai.rojan.designlab'  versionCode='1'  versionName='1.0.0'  targetSdk='37'
  application-label:'ROJAN AI'                     (and every application-label-<locale> = 'ROJAN AI')
  launchable-activity: name='ai.rojan.designlab.MainActivity'  label='ROJAN AI'

aapt2 dump resources  →  string/app_name  ()  "ROJAN AI"
```

- `application-label` and the launcher activity label both read **`ROJAN AI`** — this is what the
  home screen, task switcher, App info, and permission dialogs display.
- `package: name='ai.rojan.designlab'` — **applicationId / package unchanged**.
- `versionCode` / `versionName` unchanged.

### P0-1 and P0-2 still intact — ✅

```
apksigner verify --verbose  →  Verifies   |   v2: true   |   v3: true
BuildConfig.API_BASE_URL = "https://api.rojanai.ir/"   (from the prior P0-1 fix)
```

### Manager / Reception unaffected — ✅

`app/src/manager/res/values/strings.xml` (`ROJAN Manager`) and
`app/src/reception/res/values/strings.xml` (`ROJAN Reception`) are byte-for-byte unchanged; they
override `app_name` in their own source sets and never read `src/main`'s value.

_(On-device launcher screenshot not captured — the A72 was disconnected from ADB at verification
time. The `aapt2` inspection above reads the label straight out of the built APK's compiled
resource table, which is exactly what the launcher renders, so it is conclusive.)_

## 5. Out of scope / follow-ups

- **`Theme.ROJAN_DesignLab`** — style-resource name, not user-visible. Rename is optional cosmetic
  cleanup; it would touch `themes.xml` + the `manager`/`reception` manifests. Deferred.
- **`AIHeader.kt` shows `"ROJAN"`** (not `"ROJAN AI"`) — a separate brand-text inconsistency in a
  legacy component. Not the P0-4 blocker. Worth a follow-up pass if that component is still on any
  screen.
- Remaining release blockers: **P0-3** (privacy policy), **P0-5** (booking flow 409). P0-1
  (production URL) and P0-2 (signing) fixed in prior tasks.

**Not committed.**
