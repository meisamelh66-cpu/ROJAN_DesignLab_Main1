# Privacy Policy Implementation — Release Blocker P0-3

**Date:** 2026-09-10 · **Scope:** website content only (`ROJAN_Web` → `apps/website`). **No Android
code touched. Not committed.**

> `rojan-release-manager` is not a registered skill/agent here — work done directly.

---

## 1. What changed (4 files, all in `apps/website/`)

| File | Change |
|---|---|
| `app/no-tenant/privacy/page.tsx` | **Added** a new `<h2>اپلیکیشن موبایل روژان (نسخه مشتری)</h2>` section between the existing "حقوق شما" and "کوکی‌ها" blocks. **All existing salon-owner / platform content is byte-for-byte unchanged.** Also widened `PAGE_DESCRIPTION` to name the app. |
| `app/no-tenant/account-deletion/page.tsx` | **New page** → `https://rojanai.ir/account-deletion`. The dedicated data-deletion URL for Play Console. Same `Section` + `prose` + `LegalNotice` layout as the other legal pages. |
| `app/sitemap.ts` | Added `/account-deletion` to `ROOT_STATIC_PATHS` (indexable, in the root-domain sitemap). |
| `components/marketing/marketing-footer.tsx` | Added `{ href: "/account-deletion", label: "حذف حساب کاربری" }` to `LEGAL_LINKS` (footer of `rojanai.ir` itself — not the tenant-site footer). |

No component was created; both pages reuse the existing `Section` / `LegalNotice` / `next/link`
conventions. No business logic, no API code, no config.

## 2. `/privacy` — the new mobile-app section (as shipped)

Covers, in Persian, matching the page's existing tone and the app's actual behaviour:

- **Collects:** mobile number (OTP login) · optional display name · booking details (salon/service/
  specialist/date/time) · favorited & followed salons · connection/technical data.
- **Permissions:** explicitly states the app requests only INTERNET — **no** location, camera,
  contacts, storage, or advertising ID.
- **Use:** authentication, showing/managing your bookings, keeping your favourites list — nothing else.
- **Sharing:** phone → SMS provider (for the code); each booking → the salon it's for; **no sale, no
  ad/analytics SDK in the app.**
- **On-device security:** login tokens stored encrypted (AES-256-GCM, Android Keystore), not sent to
  the server.
- **`<h3 id="account-deletion">`** anchor → links to the full `/account-deletion` page.
- **Children:** app not designed for children.

The pre-existing sections (salon registration data, Argon2id/JWT, infrastructure sharing, "your
data belongs to you", cookies, changes, contact) are untouched.

## 3. `/account-deletion` — the dedicated page (as shipped)

For Google Play → App content → **Data deletion** URL. Contents:

| Section | Content |
|---|---|
| درخواست حذف | Request via `/contact` (phone / WhatsApp), quoting the account's mobile number; identity is verified against that number before deletion. |
| چه اطلاعاتی حذف می‌شود | Account + mobile number, display name, favourites/follows, on-device login tokens. |
| چه اطلاعاتی ممکن است باقی بماند | Booking records held by the **salon** for its own financial/legal records (salon-controlled, not ROJAN); anonymous/aggregate stats; system backups that cycle out shortly. |
| مدت زمان | **Within 30 days** of identity verification. |
| حذف بدون تماس | In-app deletion is planned for a later version; this path is the current official route. |
| حریم خصوصی | Links back to `/privacy#account-deletion`. |

**Team-adjustable:** the "۳۰ روز" window — 30 days is the standard Play-acceptable commitment; change
it in one place (`app/no-tenant/account-deletion/page.tsx`) if the team decides otherwise. The
`LegalNotice` banner (already on every legal page) states the text should be lawyer-reviewed before
relied upon.

## 4. Verification

| Check | Result |
|---|---|
| `npm run typecheck` (`tsc --noEmit`) | ✅ clean |
| `npm run lint` (`eslint`) | ✅ exit 0 |
| `npm run build` (`next build`) | ✅ **Compiled successfully** — route list includes `ƒ /no-tenant/account-deletion` |
| `npm test` (vitest — footer + no-tenant subset) | ✅ 129 passed (16 files); full suite: _see below_ |
| Local `next start` — served pages | _see URL table_ |

### URLs served (local `next start`, `API_BASE_URL` stubbed — legal pages fetch nothing)

| URL | HTTP | Content check |
|---|---|---|
| `/privacy` | **200** | contains `اپلیکیشن موبایل روژان (نسخه مشتری)`, `id="account-deletion"`, link `/account-deletion`, **and** the untouched `هنگام ثبت‌نام سالن خود` + `Argon2id` |
| `/account-deletion` | **200** | `درخواست حذف`, `حداکثر ۳۰ روز`, links `/privacy#account-deletion` + `/contact` |
| `/terms` | **200** | untouched, still renders |
| `/cookies` | **200** | untouched, still renders |
| `/sitemap.xml` | **200** | contains `rojanai.ir/account-deletion` |
| `/` (home) | **200** | footer shows `href="/account-deletion">حذف حساب کاربری` |

_(The site's middleware throws `API_BASE_URL is not configured` when that env var is unset — a
pre-existing prod-runtime guard that 500s **every** route, `/terms` and `/cookies` included; it is
unrelated to this change. With any value set, all pages render; `next build` passes regardless.)_

## 5. Play Console steps remaining (people, not code)

1. **Lawyer review** the combined `/privacy` text + `/account-deletion` (the `LegalNotice` banner
   requires it) — and confirm/adjust the 30-day window.
2. **Deploy `rojanai.ir`** so both URLs are live (`ROOT_DOMAIN` / `SITE_URL` env already handled by
   the existing deploy).
3. Play Console → App content:
   - **Privacy policy:** `https://rojanai.ir/privacy`
   - **Data deletion:** `https://rojanai.ir/account-deletion`
   - **Data Safety form:** fill per the table in `PRIVACY-POLICY-RELEASE-REPORT.md` §5.
4. *(Recommended, not blocking)* add the in-app privacy link — code is in
   `PRIVACY-POLICY-RELEASE-REPORT.md` §6; deliberately not applied here ("do not modify Android
   business logic").

## 6. Status

**P0-3 website implementation is complete and verified.** The blocker is cleared once the site is
deployed and the two URLs are entered in Play Console. Remaining release blocker: **P0-5** (booking
flow 409). P0-1 / P0-2 / P0-4 already fixed.

**Not committed.**
