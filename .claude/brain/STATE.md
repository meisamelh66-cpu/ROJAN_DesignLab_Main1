# STATE — Tech Stack & Repo Composition

> Last verified: 2026-09-03 · HEAD: 499ab45 (feature/android-first-salon-pilot) · Scope: current toolchain, dependencies, and repo composition. Generated from `build.gradle.kts` / `gradle/libs.versions.toml` / `settings.gradle.kts` and file counts — regenerate rather than hand-edit.

## Toolchain

- Kotlin 2.2.10 · AGP 9.3.1 · Gradle 9.6.1 (wrapper) — version-catalog based
- compileSdk 37 · targetSdk 36 · minSdk 24
- Jetpack Compose via Compose BOM 2026.06.00, Material3, Navigation Compose 2.9.1

## Dependencies (deliberately narrow)

- Networking: Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx-serialization-json 1.11.0 (no Gson/Moshi)
- Images: Coil-Compose 2.7.0 — the only remote-image library
- Persistence: DataStore Preferences 1.1.2 — **no Room, no local database**
- DI: **none** — no Hilt, no Koin. Manual singleton containers instead
  (`di/BackendApiContainer.kt`, `di/BackendApiContainerHolder.kt`, plus
  per-flavor `ManagerRepositories.kt` / `ReceptionRepositories.kt`)

## Module shape

Single Gradle module (`:app`), no multi-module split. Two flavor dimensions × 3 each =
**9 build variants**: `target` (`customer` / `manager` / `reception`, each its own
`applicationId`) × `environment` (`dev` / `staging` / `production`, URL injected via
`local.properties` or `-P` Gradle property, never hardcoded).

## Composition (counts)

- 707 tracked files total
- 415 Kotlin source files (387 `main` / 22 `test` / 4 `androidTest` + per-flavor entry activities)
- 55 ViewModels
- 38 repository interfaces (16 shared domain-level, 9 manager-scoped, 3 reception-scoped, 10 misc.)
- 15 `*RepositoryImpl.kt` in shared `data/repository/`, plus flavor-scoped `Backend*Repository.kt` impls

For layering and per-flavor structure, see [ARCHITECTURE.md](ARCHITECTURE.md), not this file.
