# ROJAN Update System Skill v1.0

## Purpose

This skill manages the ROJAN AI application update lifecycle.

The goal is to provide a professional update experience similar to modern mobile applications.

---

# Update Architecture

The update system consists of:

- Backend version service
- Android version checker
- Update dialog
- Changelog display
- Download/update flow


---

# Version API

The application should receive:

Example:

```json
{
  "versionCode": 12,
  "versionName": "1.1.0",
  "title": "نسخه جدید ROJAN AI",
  "message": "بهبود رزرو و عملکرد برنامه",
  "downloadUrl": "https://rojanai.ir/download/app.apk",
  "forceUpdate": false,
  "minimumSupportedVersion": 10
}