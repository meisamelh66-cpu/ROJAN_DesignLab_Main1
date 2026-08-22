# Future Scale Rules

طراحی باید برای آینده آماده باشد:

-   Multi Tenant
-   Enterprise
-   Franchise
-   Marketplace
-   AI Platform

راه حل کوتاه مدت نباید آینده را محدود کند.

## Scale Readiness (RULE 010)

علاوه بر رشد Multi Tenant، معماری باید این‌ها را هم پشتیبانی کند:

-   **Horizontal Scaling** — Backend نباید به State محلی یک Instance
    وابسته باشد؛ هر Instance باید قابل تکثیر باشد.
-   **Observability** — از قبل در
    11_TECHNICAL_STANDARDS/TECHNICAL_STANDARDS.md به عنوان یک اصل
    فنی ثبت شده است؛ اینجا فقط ارجاع داده می‌شود.
-   **Failure Recovery** — هر Component باید بتواند بعد از خرابی
    (Crash، Deploy ناموفق، Network Partition) بدون از دست رفتن داده
    Recovery شود؛ نگاه کنید به
    10_ARCHITECTURE_DECISIONS_ADR/ADR-004_BOOKING_MUTATION_RELIABILITY.md
    برای نمونه مشخص در Booking.
