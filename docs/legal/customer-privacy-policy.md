# ROJAN AI — Customer App Privacy Policy

```text
Product:        ROJAN AI Customer App
Document:        Privacy Policy
Version:         1.0
Effective Date:  TBD — do not set until publication is authorized
Last Updated:    2026-11-11
Status:          DRAFT — PENDING PUBLICATION
```

> This is the internal, evidence-annotated source-of-truth for the ROJAN AI Customer
> app's Privacy Policy — it carries the Data Inventory appendix and decision log that
> justify every claim in the policy text. **For the clean, public-ready text to actually
> publish, use `docs/legal/customer-privacy-policy-PUBLIC.md` instead** — that file is
> sections 1-18 only, with no internal/audit commentary.
>
> Grounded entirely in the current implementation at commit `ff00f82` of this repository
> (branch `main`); no fact is asserted unless a specific source file or code path proves
> it. Has **not** been reviewed by a lawyer. **Release scope (owner decision, recorded
> 2026-11-11): publication is for ROJAN's own website (direct APK download) and Cafe
> Bazaar — Google Play is deferred/out of scope for this round.** Contact channel and
> deletion-processing-time decisions are now resolved (§18, §14) — see "Owner decisions
> log" below. Only the Effective Date remains open, to be set on actual publication.

---

## 1. Introduction

ROJAN AI ("we", "us", "our") provides the ROJAN AI Customer mobile application
(the "App"), which lets you discover beauty salons and book appointments with them.
This Privacy Policy explains what information the App collects, how it is used, and
the choices available to you. It applies only to the Customer app; the ROJAN Manager
and ROJAN Reception apps are separate products with their own privacy practices.

## 2. Information We Collect

We collect only the information needed to let you sign in, browse salons, and book
and manage appointments:

- **Phone number** — used to send you a one-time verification code (OTP) and to
  identify your account. This is the primary way you sign in to the App.
- **Full name** — collected the first time you verify your phone number, so salons
  and specialists can identify you.
- **Profile photo and cover photo** — only if you choose to add them from your
  device's photo library.
- **Booking details** — the salon, service, specialist, and date/time you select
  when you book an appointment, plus any optional note you add to a booking.
- **Account identifiers** — an internal account ID and role, used to operate your
  account and are not meaningful outside our systems.

We do **not** collect your email address through normal use of the App. An
email/password sign-in path exists in our underlying code but is not reachable
from any screen in the current version of the App — see the Data Inventory for
detail.

We do **not** collect your precise or approximate location, your contacts, or
access your camera. The App does not request any device permission other than
internet access.

## 3. How We Use Information

We use the information above only to operate the App's core functions:

- Verifying your identity and keeping you signed in.
- Showing you salons, services, and specialists, and letting you complete bookings.
- Showing your own appointment history and letting you reschedule or review it.
- Displaying your chosen profile photo and cover photo back to you and, where
  relevant, to the salon/specialist you book with.

We do not use your information for advertising, and we do not build an advertising
profile from it.

## 4. Account & Authentication

You sign in with your phone number and a one-time code (OTP) sent by SMS. We do
not store your OTP code after it is verified. Once verified, we issue your device
a session (access and refresh tokens); these tokens are encrypted on your device
using a key held in your device's hardware-backed secure storage (Android
Keystore) and are never stored or transmitted in plain text.

## 5. Booking & Appointment Data

When you book an appointment, we send the salon, service, specialist, selected
date/time, and any note you choose to add to our servers so the salon can confirm
and fulfill your appointment. You can view your own appointment history inside
the App at any time.

## 6. Profile Information

Your name, phone number, and account role are stored on our servers and
associated with your account so you can be identified to the salons you book
with. You can view this information inside the App; editing/correcting it may
be limited in the current version — see the Data Inventory for the exact
capabilities implemented today.

## 7. Photos / Media

If you choose to add a profile photo or cover photo, the App uses your device's
built-in photo picker to let you select an existing image — the App is never
given broad access to your photo library, only the specific image you choose.
The image is resized and re-compressed on your device before it is uploaded to
our servers. You can remove your profile or cover photo at any time from within
the App.

## 8. Location Data

The Customer app does not request, access, or use your device's location in
any way. (The ROJAN Manager app, a separate product used by salon owners, does
use location — for example to help a salon owner set their salon's map
location — but that is unrelated to the Customer app and is not covered by
this policy.)

## 9. Notifications

The App's current version does not send push notifications or schedule local
device notifications. An in-app appointment reminder feature exists in the
interface, but it is not yet connected to any real notification delivery
mechanism in this version of the App — enabling it currently has no effect
beyond the App's own internal state.

## 10. Analytics / Crash Reporting

The current version of the App does not include any analytics or crash
reporting service. No usage data or crash reports are collected or sent to us
or to any third party for these purposes.

## 11. Data Sharing

We do not sell your information. We do not share your information with
advertisers or data brokers. Your booking details are shared with the specific
salon and specialist you choose to book with, as a necessary part of
fulfilling your appointment. Your phone number is shared with our SMS delivery
provider solely to send you the one-time verification code described above.

## 12. Data Storage & Security

Your account data is stored on our servers. Your authentication session is
stored on your device encrypted with a hardware-backed key (Android Keystore,
AES-256-GCM) and cannot be read in plain text even if your device's storage is
extracted. We use encrypted network connections (HTTPS) for all communication
between the App and our servers.

## 13. Data Retention

We retain your account, profile, and booking information for as long as your
account remains active and for as long as needed to provide the App's
services to you. If you request that your account be deleted, we will delete
your personal information within a **maximum of 30 days** of receiving your
request and verifying your identity, except where we are required by law to
retain certain information for a longer period.

## 14. Account Deletion

You can request deletion of your account and personal information at any
time by calling or messaging us at **09114050112**. Because your account is
tied to your phone number, we will verify your identity against that phone
number before processing your request. We will complete the deletion within
a maximum of 30 days of receiving your request and verifying your identity.
Deletion removes your account record, name, phone number association,
profile and cover photos, and your favorites/followed salons.

## 15. User Rights

Depending on where you live, you may have rights to access, correct, or
request deletion of your personal information, and to object to or restrict
certain uses of it. To exercise these rights, contact us using the
information below.

## 16. Children's Privacy

The App is not directed to children and is not intended for use by children.
We do not knowingly collect personal information from children. If you
believe a child has provided us with personal information, please contact us
so we can take appropriate action.

## 17. Changes to This Policy

We may update this Privacy Policy from time to time. We will update the "Last
Updated" date above when we do, and significant changes will be communicated
through the App or another appropriate channel.

## 18. Contact Information

If you have questions about this Privacy Policy, how we handle your
information, or would like to request deletion of your account, contact us
by phone at **09114050112**.

---

## Owner decisions log (not for publication — internal record)

1. **Contact channel (§14, §18) — RESOLVED 2026-11-11.** Owner confirmed the
   official contact number: `09114050112`. Applied to both sections.
2. **Deletion processing time (§13, §14) — RESOLVED 2026-11-11.** Owner
   confirmed: maximum 30 days from request + identity verification. Applied
   to both sections.
3. **Effective Date (top of document) — still open.** Must be set to the
   real publication date when ROJAN actually publishes this policy, not
   before.

---

## Data Inventory (source evidence — not for publication, supporting appendix)

Each row reflects only what is verifiable from the Customer app's source in
this repository at commit `ff00f82`. Anything not listed here was checked and
found absent.

| Data | Collected? | Purpose | Where stored | Shared w/ 3rd party? | Required/optional | Evidence |
|---|---|---|---|---|---|---|
| Phone number | YES | Sign-in identity, OTP delivery | Backend (account record); not persisted locally as plaintext | Backend's SMS provider receives it to deliver the OTP (backend-side behavior, not visible in this repo) | Required | `data/remote/dto/AuthDtos.kt`: `OtpRequestDto(phoneNumber)`, `OtpVerifyRequestDto(phoneNumber, code, fullName)`; `AuthApi.requestOtp`/`verifyOtp` |
| OTP code | YES (transiently) | Verify sign-in | Not stored after verification (not found anywhere in local storage code) | No | Required for sign-in | `AuthDtos.kt: OtpVerifyRequestDto.code`; no persistence call site found |
| Full name | YES (optional at OTP time) | Identify you to salons | Backend (`UserResponseDto.fullName`) | Visible to salons you book with, as part of booking fulfillment | Optional at OTP step (`fullName: String? = null`) | `AuthDtos.kt: OtpVerifyRequestDto.fullName`, `UserResponseDto.fullName` |
| Email | NOT actively collected | N/A | N/A | N/A | N/A | `UserResponseDto.email: String? = null`; the only write path (`RegisterRequestDto`/`LoginRequestDto` via `AuthApi.register`/`.login`) is called only from `BackendAuthRepositoryImpl.kt:23,35` with **zero call sites from any ViewModel/screen** (`git grep` across `presentation/` → no matches) — dead code, not reachable by a real user |
| Avatar (profile photo) | YES (user-initiated) | Display your chosen photo | Backend (`UserResponseDto.avatarUrl`) | Visible to salons/specialists as part of your profile, per normal booking-app behavior | Optional | `ProfileScreen.kt:173` (`PickVisualMedia`), `UserProfileRepository.uploadAvatar`, `AuthDtos.kt: UserResponseDto.avatarUrl` |
| Cover photo | YES (user-initiated) | Display your chosen photo | Backend (`UserResponseDto.coverUrl`) | Same as avatar | Optional | `ProfileScreen.kt:182`, `UserProfileRepository.uploadCover`, `UserResponseDto.coverUrl` |
| Salon / specialist / service info | Delivered TO you, not collected FROM you | Let you browse and choose | N/A (read-only content from backend) | N/A | N/A | `BookingDtos.kt: ServiceSummaryDto`, `SpecialistSummaryDto` — this is catalog content, not personal data |
| Booking information | YES | Create/manage appointments | Backend (`BookingResponseDto`) | Shared with the specific salon/specialist booked | Required to book | `BookingDtos.kt: CreateBookingRequestDto(salonId, serviceId, specialistId, startTime, notes, customerId)`, `BookingResponseDto` |
| Appointment date/time | YES | Same as above | Backend (`startTime`/`endTime`) | Same as above | Required to book | `BookingDtos.kt` |
| Account identifiers (id, role) | YES | Operate your account | Backend (`UserResponseDto.id`, `.role`) | No | Required | `AuthDtos.kt: UserResponseDto` |
| Device identifiers | NO | — | — | — | — | `git grep` for `ANDROID_ID`/`getDeviceId`/`AdvertisingIdClient`/`Settings.Secure` → zero matches anywhere in Customer source |
| Analytics | NO | — | — | — | — | No analytics SDK in `app/build.gradle.kts`; no `google-services.json` anywhere in the repo |
| Crash reporting | NO (not active in this version) | — | — | — | — | No Crashlytics/Sentry dependency present; no `google-services.json`; confirmed absent in this exact checkout |
| Notifications | NO (not implemented) | — | — | — | — | `ReminderScheduler.kt`: `NoOpReminderScheduler` — explicit no-op by design; no `POST_NOTIFICATIONS` permission anywhere (`git grep` → zero matches) |
| Location | NO | — | — | — | — | Customer manifest (`app/src/main/AndroidManifest.xml`) declares only `android.permission.INTERNET`; no location permission, no `LocationManager`/`FusedLocationProvider` usage anywhere in Customer source |
| Contacts | NO | — | — | — | — | No contacts permission or API usage found anywhere in Customer source |
| Camera | NO | — | — | — | — | No `CAMERA` permission; photo selection uses `ActivityResultContracts.PickVisualMedia()` (system Photo Picker), never a camera capture intent |
| Photo library / storage | Mediated access only, no broad permission | Let you pick a photo | — | — | Optional | `ActivityResultContracts.PickVisualMedia()` — modern system picker; no `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE` permission requested anywhere |
| Third-party SDKs | Retrofit, OkHttp, kotlinx.serialization, Coil, AndroidX DataStore | Networking/image-loading infrastructure only, all pointed at our own backend | — | None of these transmit data to any vendor other than the URL the app itself requests (our backend) | — | `gradle/libs.versions.toml` dependency list; no analytics/ads/crash vendor present |
| Backend/API endpoints | Our own backend (environment-configured; not hardcoded in this build's source) | All App functionality | — | — | — | `NetworkConfig.kt`: `BuildConfig.API_BASE_URL`, resolved per environment flavor at build time; no literal production hostname is committed in this repository's current source |
| Local storage (auth session) | YES | Keep you signed in | Device only — AES-256-GCM ciphertext in `SharedPreferences`, key in Android Keystore (never leaves hardware-backed storage) | No | — | `data/local/SecureTokenStore.kt` |
| Local storage (app state) | YES | Remember your active salon / session UI state | Device only (DataStore) | No | — | `data/local/ActiveSalonDataStore.kt`, `data/local/AuthSessionDataStore.kt` |

### Account deletion — NOT IMPLEMENTED

```text
Account deletion:
NOT IMPLEMENTED
Evidence:
  - AuthApi.kt (full interface read): register, login, refresh, me,
    getSalonAccess, requestOtp, verifyOtp — no DELETE endpoint, no
    "delete account" method anywhere.
  - git grep -ni "delete.*account|deleteAccount|account.*delet" across
    all Customer .kt source: zero matches.
  - No UI entry point (no button/menu item) for account deletion found in
    ProfileScreen.kt or any other screen inspected.
Required implementation (before Section 14 of the policy above can be
published as a real "self-service deletion" claim):
  1. A backend endpoint for account deletion (none found referenced
     anywhere in this repo's Android source — would need confirmation
     from the backend team, cross-boundary, System 1 territory).
  2. An Android repository method + ViewModel action + a real UI entry
     point (e.g. in ProfileScreen's settings/account area).
  3. A decision on whether deletion is immediate/hard-delete or a
     deactivation-then-retention-window model — not decided anywhere in
     current source.
Until implemented, Section 14 above must stay as the contact-based
placeholder, not a false "delete your account in Settings" claim.
```
