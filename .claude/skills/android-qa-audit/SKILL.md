# Android QA Audit Skill v1.0

## Purpose

This skill performs professional quality assurance for ROJAN Android applications before release.

The goal is to detect functional, visual, performance, and release-blocking issues.

---

# QA Principles

Always validate:

- Real device behavior
- Real backend communication
- User journeys
- Release stability

Never approve a release only from compilation success.

---

# Test Environment

Record:

- Device model
- Android version
- App version
- Build variant
- Backend environment
- Test account

Example:

Device:
Samsung Galaxy A72

Android:
14

Build:
Customer Release Candidate

---

# Core Test Matrix

## 1. Installation

Verify:

- APK installs successfully
- App launches
- Splash works
- Session restore works
- No crash on cold start


---

## 2. Authentication

Test:

- Phone entry
- OTP request
- OTP verification
- Invalid OTP
- Resend OTP
- Rate limit handling
- Logout
- Login again


---

## 3. Customer Main Flow

Verify:

Home:

- Data loading
- Empty states
- Error states
- Navigation


Explore:

- Salon list
- Search
- Salon details


Salon:

- Information display
- Services
- Specialists
- Booking entry


---

## 4. Booking Journey

Required:

- Service selection
- Specialist selection
- Date selection
- Time selection
- Confirmation
- Payment selection
- Success state


Record:

- API responses
- Errors
- UI problems


---

## 5. Profile Area

Test:

- Profile loading
- Personal information
- Appointments
- Appointment details
- Reschedule
- Favorites
- Logout


---

# Visual QA

Check:

- RTL layout
- Typography
- Spacing
- Icons
- Colors
- Touch targets
- Loading states
- Empty states

Compare against approved ROJAN design language:

- Dark navy
- Rose gold accent
- Quiet luxury
- Minimal glass usage


---

# Performance QA

Check:

- Startup time
- Frame drops
- Memory leaks
- ANR risks
- Large images
- Slow screens


---

# Network QA

Verify behavior for:

- Offline mode
- Timeout
- 401
- 403
- 409
- 429
- Server errors


The app must never crash.

---

# Regression Rules

Before approving:

Re-test:

- Authentication
- Home
- Booking
- Profile
- Navigation


Any modified shared component requires testing all dependent screens.

---

# Reports

Generate:

CUSTOMER-QA-REPORT.md

Include:

- Passed tests
- Failed tests
- Blockers
- Screenshots
- Device evidence


---

# Release Decision

Output:

PASS

or

FAIL


FAIL requires:

P0:
Release blockers

P1:
Important issues

P2:
Future improvements


---

# ROJAN QA Rule

A build is not release-ready until:

- Real device tested
- Main user journey completed
- No critical crashes
- Release blockers documented