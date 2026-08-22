# Cache Policy

Cache قابل بازسازی است.

Cache هرگز مرجع حقیقت کسب‌وکار نیست.

قانون:

-   هر مقدار Cache باید از یک منبع Backend قابل بازتولید باشد.
-   از دست رفتن Cache نباید باعث از دست رفتن داده شود.
-   Cache نباید برای تصمیم‌های Permission یا Booking استفاده شود؛ این
    تصمیم‌ها همیشه باید در لحظه از Backend گرفته شوند (نگاه کنید به
    05_AUTHENTICATION_SECURITY/PERMISSION_MATRIX.md).

رابطه با Client Local Storage در
03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md مشخص شده است.
