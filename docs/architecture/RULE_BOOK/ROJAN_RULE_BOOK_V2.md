# ROJAN RULE BOOK V2

## 1. Product Principles
- ROJAN باید یک SaaS حرفه‌ای Beauty Management باشد.
- هر قابلیت باید دارای Backend واقعی، تست و مسیر توسعه مشخص باشد.

## 2. UI/UX Rules
- طراحی Premium Glassmorphism حفظ شود.
- RTL فارسی اولویت اصلی است.
- هیچ UI بدون state واقعی یا backend واقعی ساخته نشود.

## 3. Booking Rules
- وضعیت‌های رزرو باید از State Machine بک‌اند پیروی کنند.
- تغییر زمان رزرو باید conflict-safe باشد.
- زمان فعلی رزرو نباید باعث حذف اشتباه گزینه‌های reschedule شود.

## 4. Authentication Rules
- Session management باید از الگوی مرکزی استفاده کند.
- Logout باید امن و مبتنی بر POST باشد.
- Header باید نسبت به وضعیت ورود کاربر آگاه باشد.

## 5. Schedule Rules
- مدیریت زمان متخصص باید از Permission های backend پیروی کند.
- Weekly Availability، Overrides، Leaves و Blocks ماژول‌های مستقل هستند.

## 6. ROJAN Media Engine Rules

### MEDIA-001 Smart Upload
هیچ تصویر خامی نباید بدون پردازش وارد نمایش نهایی شود.

### MEDIA-002 User Controlled Crop
کاربر باید کادر نمایش تصویر را تعیین کند.

### MEDIA-003 Focal Point
سیستم باید نقطه تمرکز تصویر را ذخیره کند.

### MEDIA-004 Image Quality Analysis
کیفیت، رزولوشن، نور و وضوح تصویر باید بررسی شود.

### MEDIA-005 Optimized Delivery
تصاویر باید نسخه‌های مختلف برای نمایش سریع داشته باشند:
- thumbnail
- card
- detail
- original

## 7. Architecture Decision Records

ADR-0015
Decision:
Implement ROJAN Media Engine

Reason:
پلتفرم Beauty بدون مدیریت حرفه‌ای تصویر تجربه کامل ندارد.

Status:
Approved