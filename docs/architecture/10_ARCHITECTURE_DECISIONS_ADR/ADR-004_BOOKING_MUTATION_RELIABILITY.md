# ADR-004 Booking Mutation Reliability

## Status

Accepted

## Context

Booking و Availability مرجع Backend هستند
(02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md). چند Client (Website،
Desktop، Manager، Specialist، Customer) می‌توانند هم‌زمان روی یک Salon
عملیات Booking انجام دهند. بدون قوانین صریح Reliability، خطرهای زیر
واقعی است:

-   ارسال دوباره یک درخواست (Network Retry / Double Tap) باعث دو
    Booking تکراری شود.
-   دو کاربر هم‌زمان یک Slot را رزرو کنند (Conflict).
-   یک Mutation نیمه‌کاره باعث وضعیت ناسازگار بین Booking و Calendar
    شود.

## Decision

هر Mutation روی Booking باید این پنج ویژگی را داشته باشد:

1.  **Idempotency** — تکرار یک درخواست با همان Idempotency Key نباید
    نتیجه تکراری تولید کند.
2.  **Duplicate Handling** — Backend باید درخواست‌های تکراری را تشخیص
    و رد یا بی‌اثر کند، نه اینکه به Client واگذار کند.
3.  **Conflict Handling** — تداخل Slot باید در لحظه Mutation، نه بعد
    از آن، تشخیص داده و رد شود.
4.  **Retry Safety** — Client مجاز است درخواست را Retry کند؛ Retry
    هرگز نباید حالت کسب‌وکار را خراب کند.
5.  **Transaction Boundary** — هر Mutation باید در یک مرز تراکنشی
    واحد Backend انجام شود؛ هیچ Mutation نیمه‌کاره نباید برای Client
    قابل مشاهده باشد.

## Impact

-   Client (هر پلتفرم) فقط مسئول ارسال درخواست با Idempotency Key
    است؛ منطق تشخیص تکرار/تداخل هرگز در Client پیاده نمی‌شود.
-   Desktop SQLite (Projection) نباید برای تصمیم Conflict استفاده
    شود — تصمیم همیشه از Backend می‌آید
    (03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md).

## Migration Plan

Feature یا Endpoint جدید Booking باید قبل از Release این پنج ویژگی را
طبق 08_DEVELOPMENT_GOVERNANCE/FEATURE_PROCESS.md بررسی و تایید کند.
