# ROJAN Product Constitution

## اصل اصلی

Backend Domain Model مرجع نهایی حقیقت است.

تمام محصولات: - Website - Desktop - Manager - Specialist - Customer

مصرف کننده Backend هستند.

هیچ Client مالک Domain کسب‌وکار نیست.

## Tenancy (RULE 007)

Tenant تولید فعلی: **Salon**. هر Tenant امروز دقیقاً یک Salon است.

Organization (چند Salon زیر یک مالکیت، Enterprise) بخشی از این
Constitution امروز **نیست** — Future Enterprise Architecture است،
نگاه کنید به 12_FUTURE_SCALE/FUTURE_RULES.md. هیچ Client یا Backend
Feature نباید امروز فرض کند Tenant می‌تواند بیش از یک Salon باشد، مگر
اینکه ADR جداگانه‌ای این تغییر را تایید کند.

## اهداف ۵ ساله

ROJAN باید توانایی پشتیبانی از: - هزاران سالن - چند شعبه - سازمان‌های
بزرگ - Marketplace - AI Services

را داشته باشد.
