# Android Build Signing Skill v1.0

## Purpose

This skill manages the Android build, release packaging, and signing validation process for ROJAN.

The goal is to guarantee that every distributed build is correctly generated, secured, and ready for publication.

---

# Build Validation

Before any release:

Check:

- Gradle configuration
- Product flavors
- Build types
- VersionName
- VersionCode
- Compile SDK
- Target SDK
- Minimum SDK
- Dependencies

---

# Build Types

Validate separately:

## Debug

Requirements:

- Development testing
- Debuggable enabled
- Test backend allowed


## Release

Requirements:

- Debug disabled
- Optimization enabled
- R8 verified
- Resource shrinking checked
- Production configuration loaded


---

# Artifact Generation

Prepare:

Android Package:

- APK for direct download
- AAB for stores


Verify:

- File exists
- Correct version
- Correct applicationId
- Correct variant


---

# Signing Management

Before release verify:

- Release keystore exists
- Keystore password configured securely
- Signing certificate valid
- APK/AAB signed correctly


Never release:

- unsigned APK
- debug signed production build
- exposed credentials


---

# Gradle Commands

Typical validation:

Build:
