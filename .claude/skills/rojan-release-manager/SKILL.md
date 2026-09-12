# ROJAN Release Manager Skill v1.0

## Purpose

This skill manages the complete ROJAN AI release lifecycle.

The goal is to prepare production-quality releases for:
- Android application
- Website distribution
- App stores
- Future update cycles

This skill must prioritize stability over speed.

---

# Release Workflow

When asked to prepare a release, execute these phases:

## Phase 1 — Release Audit

Check:

- Current git branch
- Uncommitted changes
- VersionName
- VersionCode
- Build variants
- Environment configuration
- API endpoints
- Signing configuration
- Permissions
- Crash reporting
- Privacy requirements

Generate:

CUSTOMER-RELEASE-AUDIT.md


---

# Phase 2 — Build Validation

Verify:

- Debug build
- Release build
- Production build configuration
- Gradle success
- R8 status
- Resource shrinking
- APK/AAB generation

Never declare release ready without successful release build.


---

# Phase 3 — Quality Gate

Require:

- Real device test
- Login flow
- OTP verification
- Home rendering
- Search
- Salon details
- Booking flow
- Profile
- Logout
- Update check

Generate:

RELEASE-QA-REPORT.md


---

# Phase 4 — Version Management

Rules:

Every public release requires:

versionName:
Example:
1.0.0

versionCode:
Integer increase only.

Never reuse a published versionCode.


Maintain:

CHANGELOG.md


Format:

## Version 1.0.0

Added:
-

Improved:
-

Fixed:
-

---

# Phase 5 — Release Artifacts

Prepare:

- Signed APK
- Signed AAB
- Release notes
- Screenshots checklist
- Store description
- Website download package


---

# Phase 6 — Release Decision

Allowed outputs:

READY FOR RELEASE

or

NOT READY FOR RELEASE

If NOT READY:

List blockers by priority:

P0:
Must fix before release

P1:
Recommended before release

P2:
Future improvement


---

# ROJAN Rules

Always preserve:

- Clean Architecture
- Kotlin + Jetpack Compose standards
- RTL Persian experience
- Premium brand identity
- Backend contracts
- Existing navigation

Never:

- Modify business logic during visual review
- Skip device testing
- Publish unsigned builds
- Ignore versioning
- Break existing flows


---

# Release Checklist

Before final approval:

[ ] Production backend configured

[ ] Release signing configured

[ ] Version updated

[ ] Build successful

[ ] APK/AAB generated

[ ] Real device tested

[ ] Crash monitoring active

[ ] Privacy policy available

[ ] Store assets ready

[ ] Website download ready

[ ] Update system verified


---

# Output Style

Reports must be:

- Clear
- Technical
- Prioritized
- Actionable

Every release decision must include evidence.