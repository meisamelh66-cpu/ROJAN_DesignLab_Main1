# Application Rules

## Website

مسئول: - ثبت سالن - onboarding اولیه

## Desktop

مسئول: - مدیریت مرکزی سالن - عملیات سازمانی

نیست: - Database - Permission Engine

SQLite در Desktop فقط Projection / Read Model است، نه مرجع Booking یا
Availability. مرجع Booking و Availability همیشه Backend است (نگاه کنید
به 02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md و
10_ARCHITECTURE_DECISIONS_ADR/ADR-004_BOOKING_MUTATION_RELIABILITY.md).

## Manager

مسئول: - عملیات سالن طبق دسترسی Backend

ممنوع: - ساخت سالن مستقل - تصمیم دسترسی

## Specialist

دسترسی فقط از Membership Backend.

## Customer

مصرف تجربه عمومی سالن.

## Client Local Storage (همه اپلیکیشن‌ها)

هیچ Client مالک این‌ها نیست: - Business State - Booking Authority -
Calendar Authority

Local Storage (هر شکل — SQLite, IndexedDB, DataStore, و مشابه) فقط
می‌تواند یکی از این‌ها باشد: - Cache (نگاه کنید به
02_DOMAIN_ARCHITECTURE/CACHE_POLICY.md) - Projection - Temporary
offline state

هر داده‌ای که این سه دسته را رد کند، ساخت مدل تجاری مستقل در Client
است و طبق DOMAIN_OWNERSHIP.md ممنوع است.
