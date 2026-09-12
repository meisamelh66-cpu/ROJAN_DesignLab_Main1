# Privacy Policy — Release Blocker P0-3 (Audit + Implementation Readiness)

**Date:** 2026-09-10 · **Scope:** audit + readiness only. **No source file changed. Not committed.**
Everything below is paste-ready but deliberately left unapplied — the site's own `LegalNotice`
component says legal text must be lawyer-reviewed before it goes live, and the task is to stop at
readiness.

> Note: `rojan-release-manager` is not a registered skill/agent in this environment. Work done directly.

---

## 1. Current state

### Website (`ROJAN_Web` → `apps/website`, Next.js 16 App Router, domain `rojanai.ir`)

| Item | State |
|---|---|
| Privacy page | **Exists** — `app/no-tenant/privacy/page.tsx` → **`https://rojanai.ir/privacy`**. Persian, indexed, in `sitemap.ts`, linked from the site footer (`marketing-footer.tsx` `LEGAL_LINKS`). |
| Terms page | Exists — `/terms` |
| Cookies page | Exists — `/cookies` |
| Tenant-site privacy | `app/sites/[tenant]/privacy/page.tsx` — for salon white-label sites, **separate concern** |
| `LegalNotice` disclaimer | Rendered on every legal page: *"this is a generic template, not legal advice — have a lawyer review it."* |
| Contact channels (`lib/constants/contact-channels.ts`) | Phone/WhatsApp **`+98 911 405 0112`**, Instagram `@rojanai.ir`. **No email** (deliberately absent). |
| Account-deletion / data-request mechanism | **None** — no page, no form, no in-app flow. The current `/privacy` only says "you may request deletion" with no route. |
| `/download` page | Markets Customer + Manager (Android) and Reception (Windows). Customer app `available: false` — **not yet published anywhere** (Play or direct). |

### What the current `/privacy` page covers vs. what it misses

The live text is written for **salon owners registering on the platform** — "هنگام ثبت‌نام سالن خود",
salon/owner contact details, customer/service/appointment data *entered by the salon*, Argon2id
password hashing, JWT sessions.

**It says nothing about the Customer mobile app:** phone-number + SMS-OTP login for end customers,
the bookings/favorites a customer makes in the app, the SMS provider, on-device token storage,
Android permissions, or how an app user deletes their account.

### Android Customer app (`ROJAN_DesignLab`, `ai.rojan.designlab`)

| Item | State |
|---|---|
| In-app privacy/legal link | **None anywhere** — no link on the Auth screen, Profile, or any settings/about screen (there is no about/settings screen). `VersionFooter` composable exists but is unused. |
| Data the app actually handles | Mobile number (OTP login) · optional display name · bookings (salon/service/specialist/date/time) · favorited & followed salons · JWT access/refresh tokens (AES-256-GCM via Android Keystore, on-device only) |
| Permissions | `INTERNET` only. No location, camera, contacts, storage, ad ID. |
| Third parties | ROJAN backend `api.rojanai.ir` (HTTPS); an SMS provider for the OTP (observed sender IDs `+989982004676`, others) |
| Analytics / ads / crash SDKs | **None** |

---

## 2. What P0-3 actually requires (Google Play, first Customer-app publish)

| # | Requirement | Current | Gap |
|---|---|---|---|
| P0-3a | A **privacy policy URL** in Play Console → App content → Privacy policy | `https://rojanai.ir/privacy` exists | URL is fine; **content must be extended to cover the mobile app** (§3) |
| P0-3b | **Data Safety form** filled and consistent with the policy | not started | fill per the mapping in §5 |
| P0-3c | **Account/data deletion** — Play's Data deletion policy: an app with account creation must give users a way to request account + data deletion, declared in Play Console (in-app flow **or** a web URL) | nothing | add a deletion route — recommended: a dedicated web page + URL (§4) |
| P0-3d | Policy is **accessible, non-editable by users, applies to this app, names the entity, not behind a login** | `/privacy` is a public static page | already satisfied |
| P0-3e | *(store-review best practice / some jurisdictions)* privacy policy **reachable from inside the app** | none | add an in-app link (§6) — not a hard Play blocker, but expected and low-cost |

**None of these need a code/business-logic change to ship the blocker-clear:** P0-3a/c are website
content, P0-3b is a Console form, P0-3e is a one-line link. A true in-app "Delete my account" button
(nicest UX) *would* need a backend `DELETE /account` endpoint — out of scope, noted as a follow-up.

---

## 3. `/privacy` page — required content addition (paste-ready)

Add the following section to `app/no-tenant/privacy/page.tsx`, **after** the existing
"اطلاعاتی که جمع‌آوری می‌کنیم" / "نحوه استفاده" blocks and **before** "کوکی‌ها". It reuses the
page's existing `<h2>` / `<p>` / `next/link` `<Link>` conventions — no new component.

```tsx
        <h2>اپلیکیشن موبایل روژان (نسخه مشتری)</h2>
        <p>
          این بخش مخصوص کسانی است که از اپلیکیشن اندروید «روژان» برای پیدا کردن سالن و رزرو نوبت استفاده
          می‌کنند.
        </p>

        <h3>اطلاعاتی که اپلیکیشن جمع‌آوری می‌کند</h3>
        <ul>
          <li>
            <strong>شماره موبایل:</strong> برای ورود به حساب از طریق کد یک‌بارمصرف که با پیامک ارسال می‌شود.
          </li>
          <li><strong>نام (اختیاری):</strong> تنها در صورتی که هنگام ورود آن را وارد کنید.</li>
          <li>
            <strong>اطلاعات نوبت‌ها:</strong> سالن، خدمت، متخصص، تاریخ و ساعتِ نوبت‌هایی که در اپ ثبت یا
            مشاهده می‌کنید.
          </li>
          <li><strong>سالن‌های مورد علاقه و دنبال‌شده:</strong> فهرست سالن‌هایی که در اپ نشان می‌کنید.</li>
          <li>
            <strong>اطلاعات فنی اتصال:</strong> برای ارتباط اپ با سرور روژان از اینترنت دستگاه شما استفاده
            می‌شود. اپلیکیشن فقط مجوز «اینترنت» را می‌گیرد و به موقعیت مکانی، دوربین، مخاطبین، حافظه یا
            شناسه تبلیغاتی دستگاه شما دسترسی ندارد.
          </li>
        </ul>

        <h3>نحوه استفاده</h3>
        <p>
          این اطلاعات فقط برای احراز هویت شما، نمایش و مدیریت نوبت‌هایتان و نگه‌داری فهرست سالن‌های مورد
          علاقه به کار می‌رود.
        </p>

        <h3>اشتراک‌گذاری</h3>
        <ul>
          <li>شماره موبایل شما برای ارسال کد ورود در اختیار سرویس‌دهنده پیامک قرار می‌گیرد.</li>
          <li>اطلاعات هر نوبت با همان سالنی که برای آن رزرو می‌کنید به اشتراک گذاشته می‌شود.</li>
          <li>
            روژان اطلاعات شما را نمی‌فروشد و در اپلیکیشن از هیچ ابزار تبلیغاتی یا تحلیلگر شخص ثالثی استفاده
            نمی‌کند.
          </li>
        </ul>

        <h3>نگه‌داری امن روی دستگاه</h3>
        <p>
          توکن‌های ورود روی دستگاه شما به‌صورت رمزنگاری‌شده (AES-256-GCM با کلیدِ محافظت‌شده در Android
          Keystore) ذخیره می‌شوند و به سرور ارسال نمی‌شوند.
        </p>

        <h3 id="account-deletion">حذف حساب و اطلاعات</h3>
        <p>
          برای حذف حساب کاربری اپلیکیشن و اطلاعات شخصی مرتبط با آن، از طریق{" "}
          <Link href="/contact">صفحه تماس با ما</Link> (تماس تلفنی یا واتساپ با همان شماره موبایلِ حساب)
          درخواست دهید. پس از تأیید هویت، حساب و اطلاعات شخصی شما ظرف حداکثر [[تیم تأیید کند: مثلاً ۳۰]] روز
          حذف می‌شود. سابقهٔ نوبت‌هایی که با یک سالن انجام شده ممکن است برای امور مالی و سوابق آن سالن نزد
          خودش باقی بماند.
        </p>

        <h3>کودکان</h3>
        <p>اپلیکیشن روژان برای استفادهٔ کودکان طراحی نشده است.</p>
```

Also update the page's `PAGE_DESCRIPTION` to mention the app, e.g.:
`"سیاست حریم خصوصی روژان AI — جمع‌آوری، استفاده و محافظت از اطلاعات در وب‌سایت، پلتفرم و اپلیکیشن موبایل مشتری."`

**Open item for the team:** the deletion window (`[[…]]` above). 30 days is the common
Play-acceptable value; pick and confirm one.

---

## 4. Account-deletion route (P0-3c) — options

| Option | Effort | Play-acceptable? | Recommendation |
|---|---|---|---|
| **A. `#account-deletion` anchor on `/privacy`** (the `<h3 id>` above) + point Play Console's "Data deletion" field at `https://rojanai.ir/privacy#account-deletion` | ~0 (comes with §3) | Yes — a labeled, public instruction to request deletion | **Minimum viable — ship this for v1.0** |
| **B. Dedicated page** `app/no-tenant/account-deletion/page.tsx` (`/account-deletion`) with a short form or the WhatsApp/phone steps, added to the footer | ~1 small page | Yes — cleaner, this is the field Play expects | **Preferred — do this if there's time before submission** |
| **C. In-app "حذف حساب" in Profile** → calls a backend `DELETE /api/v1/account` | needs a **new backend endpoint** + app screen | Yes — best UX | **Follow-up** — out of scope ("do not change business logic"); track separately |

For the first publish, **Option A unblocks P0-3c** with zero new surface. The Play Console "Data
deletion" question then answers: *"Users can request deletion — instructions at
rojanai.ir/privacy#account-deletion"*, and *"some data (past bookings held by the salon) may be
retained."*

---

## 5. Play Console → Data Safety form — mapping

Fill exactly this (matches §3, the app's actual code, and the manifest's single `INTERNET` permission):

| Data type | Collected? | Shared? | Purpose | Optional? | Notes |
|---|---|---|---|---|---|
| **Phone number** | Yes | Yes (SMS provider for OTP; the salon you book with) | Account management; App functionality | Required | login identifier |
| **Name** | Yes | Yes (the salon you book with) | App functionality | **Optional** | only if the user types it |
| **App activity** — "Other user-generated content" / in-app actions (bookings, favorites) | Yes | Yes (the salon you book with) | App functionality | n/a | |
| Approximate/precise location | **No** | — | — | — | no location permission |
| Photos / files / contacts / calendar / SMS | **No** | — | — | — | no such permissions |
| Device or other IDs / Advertising ID | **No** | — | — | — | no ad SDK |
| Crash logs / diagnostics / analytics | **No** | — | — | — | no analytics/crash SDK (see readiness report P1 — you may add one later; update this form then) |

- **Data encrypted in transit:** Yes (HTTPS to `api.rojanai.ir`).
- **Data encrypted at rest on device:** Yes (tokens — Android Keystore AES-256-GCM).
- **Users can request data deletion:** Yes → the §4 URL.
- **Independent security review:** No.
- **Committed to Play Families policy:** app is not directed to children (per §3).

---

## 6. In-app privacy link (P0-3e) — where + minimal change

The Customer app has no about/settings screen, so the two natural homes are:

1. **Auth screen** (`screens/auth/AuthScreen.kt`) — a small line under the primary button, shown
   *before* the user submits their phone number. Standard for OTP apps.
2. **Profile screen** (`screens/profile/ProfileScreen.kt`) — a "قوانین و حریم خصوصی" row in the
   "امکانات حساب" group (it already renders grouped `RefListRow`s).

Minimal, self-contained implementation (a new tiny file — touches no ViewModel/repo/nav-route):

```kotlin
// screens/customer/components/LegalLinks.kt
package ai.rojan.designlab.screens.customer.components

import android.content.Intent
import androidx.core.net.toUri
// ...
const val ROJAN_PRIVACY_URL = "https://rojanai.ir/privacy"
const val ROJAN_TERMS_URL = "https://rojanai.ir/terms"

@Composable
fun openUrl(url: String): () -> Unit {
    val context = LocalContext.current
    return { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
```

Then on Auth, under `RefPrimaryButton`:
```kotlin
Spacer(Modifier.height(RojanDimens.SpaceMD))
Text(
    "با ادامه، شما قوانین و حریم خصوصی روژان را می‌پذیرید",
    style = RojanTypography.Caption,
    color = HomeColors.TextMuted,
    modifier = Modifier.rojanPressable(onClick = openUrl(ROJAN_PRIVACY_URL), role = Role.Button),
)
```

and/or a Profile row:
```kotlin
ProfileMenuItem(Icons.Outlined.Shield, "قوانین و حریم خصوصی", openUrl(ROJAN_PRIVACY_URL))
```

`Intent.ACTION_VIEW` needs no new permission (implicit intent to a browser). **This is a UI-only
add — no business logic — but it is left unimplemented per "stop after implementation readiness".**

---

## 7. Checklist to clear P0-3

- [ ] **Lawyer review** the combined `/privacy` text (existing + §3 addition) — the site's own
      `LegalNotice` requires this.
- [ ] Team confirms the deletion window in §3 (`[[…]]`).
- [ ] Apply the §3 content to `app/no-tenant/privacy/page.tsx` (+ update `PAGE_DESCRIPTION`).
- [ ] Pick §4 Option A (min) or B (preferred); if B, add `/account-deletion` + footer link.
- [ ] Deploy `rojanai.ir` so `https://rojanai.ir/privacy` (and `#account-deletion`) is live.
- [ ] Play Console: enter the privacy policy URL; enter the data-deletion URL; fill Data Safety per §5.
- [ ] *(recommended)* Apply §6 in-app link.
- [ ] Update `CUSTOMER-RELEASE-READINESS-FINAL.md` — mark P0-3 done.

**Remaining release blocker after P0-3:** P0-5 (booking flow 409). P0-1 / P0-2 / P0-4 already fixed.

**Stopped after audit + implementation readiness. No file changed. Not committed.**
