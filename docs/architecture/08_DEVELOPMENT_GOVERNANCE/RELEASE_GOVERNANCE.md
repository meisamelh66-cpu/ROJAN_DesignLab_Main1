# Release Governance

هر Component تولیدی باید سه چیز مشخص داشته باشد، قبل از هر Release:

-   **Owner** — شخص یا تیمی که تصمیم نهایی Release را می‌گیرد.
-   **Approval Flow** — چه کسی باید قبل از Deploy تایید بدهد.
-   **Rollback Responsibility** — چه کسی مسئول اجرای Rollback است اگر
    Release مشکل داشت.

Components:

-   Backend
-   Web
-   Mobile
-   Infra

قانون:

-   بدون Owner مشخص، Component نباید Release شود.
-   مکانیزم رسمی Deploy (مثلا Script یا CI/CD) باید قبل از استفاده
    Production تست شود؛ خرابی مکانیزم Deploy خودش یک Release Risk
    است.
-   هر Rollback باید بدون نیاز به تصمیم معماری جدید قابل اجرا باشد
    (نگاه کنید به 08_DEVELOPMENT_GOVERNANCE/FEATURE_PROCESS.md برای
    Failure Scenarios پیش از Feature).

این قانون مکمل RULE 006 (Development Order) است: Release آخرین مرحله
است، نه جایگزین Validation.
