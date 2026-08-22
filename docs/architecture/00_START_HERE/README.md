# ROJAN Architecture Governance V1.0

این مجموعه مرجع معماری رسمی ROJAN است.

هدف: جلوگیری از تضاد معماری، مدل‌های موازی، قراردادهای ناسازگار و
تصمیم‌های بدون ثبت.

ترتیب استفاده (RULE 006، الزامی): Architecture → Contract →
Implementation → Migration → Validation → Release

Domain Ownership بخشی از مرحله Architecture است، نه مرحله جداگانه
(02_DOMAIN_ARCHITECTURE/DOMAIN_OWNERSHIP.md). Migration Impact زودتر
در 08_DEVELOPMENT_GOVERNANCE/FEATURE_PROCESS.md بررسی می‌شود؛ اجرای
واقعی Migration همیشه همین‌جا، بعد از Implementation است.
