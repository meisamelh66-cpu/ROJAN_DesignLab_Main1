# Authentication Data Separation

این چهار مفهوم هرگز نباید با هم یکی در نظر گرفته شوند:

  مفهوم         تعریف
  ------------- -------------------------------------------------
  Identity      هویت واقعی کاربر در Backend (منبع: PERMISSION_MATRIX.md)
  Session       اعتبار موقت دسترسی (JWT/Token) با انقضای مستقل
  Cache         تصویر بازسازی‌پذیر داده (نگاه کنید به CACHE_POLICY.md)
  Client State  وضعیت محلی و موقت UI/فرم، بدون ارزش کسب‌وکار

قانون:

-   انقضای Session نباید به معنی از دست رفتن داده باشد.
-   Logout یا Session Expiration فقط Session را باطل می‌کند؛ Identity
    در Backend دست‌نخورده باقی می‌ماند.
-   Cache و Client State هرگز نباید برای اثبات یا بازسازی Identity یا
    Session استفاده شوند.
-   Refresh/Re-authentication باید بدون نیاز به بازسازی دستی داده‌های
    کسب‌وکار انجام شود.

این قانون مکمل PERMISSION_MATRIX.md است: Permission همیشه از Identity
معتبر Backend محاسبه می‌شود، نه از Session ذخیره‌شده در Client.
