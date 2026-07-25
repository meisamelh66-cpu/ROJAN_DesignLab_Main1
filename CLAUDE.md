# ROJAN AI DesignLab — Claude Instructions

Android app (Kotlin, Jetpack Compose). Clean Architecture: `domain/` (no Android
imports), `data/`, `navigation/`, `screens/`, `ui/` (design system: tokens,
glass components, backgrounds, buttons, interaction).

## ROJAN Development Rules

- Preserve Premium Glassmorphism Design System.
- Preserve RTL Persian-first experience.
- Follow ROJAN Quality Gate (RQG).
- Do not modify unrelated files.
- Do not remove existing features without approval.
- Before major architectural changes, ask for confirmation.

## Automatic Actions

Proceed without asking:
- Build project
- Run tests
- Inspect code
- Run emulator checks
- Capture screenshots
- Fix UI issues described in the task

## Confirmation Required

Ask only for:
- Delete files
- Database migrations
- Architecture changes
- Breaking API changes
- Large refactors

When a fix is found mid-task that's outside the current request's scope
(e.g. a regression in an unrelated file), surface it and propose a minimal,
scoped fix rather than folding it into the current change silently.

## ROJAN Quality Gate (RQG)

Before considering any UI task complete:
1. `assembleDebug` succeeds.
2. No new hardcoded colors/raw values outside token-definition files —
   route colors through `RojanTokens.kt`, surfaces through `GlassSurface`,
   backgrounds through `PremiumBackground`.
3. Design tokens/glass system used consistently with the rest of the
   screen/module.
4. RTL layout intact.
5. Build, install on emulator (or a connected device), and provide a
   screenshot of the actual result before calling the task done — if no
   device/emulator is reachable, say so explicitly rather than claiming
   visual verification that didn't happen.

When investigating uncommitted or ambiguous changes, diff against
`git show HEAD:<path>` to establish what the last-known-good baseline
actually was before proposing a fix.

## Environment notes

- Android SDK: `C:\Users\Rojan\AppData\Local\Android\Sdk`
- JDK: bundled with Android Studio (`Program Files\Android\Android
  Studio\jbr`) — set `JAVA_HOME` per-command, it's not in the environment by
  default.
- `sdkmanager` cannot reach the network in this environment (fails to
  download source lists/manifests) — only already-installed SDK packages are
  usable; don't attempt to download new system images, build-tools, etc.
- Available AVD: `Pixel_4` (`android-37.1 google_apis_playstore_ps16k`,
  x86_64) — the only system image installed. This is a heavy Play Store
  image and has been unreliable on this machine: repeated cold-boot attempts
  died silently right after WHPX init with no logged error. Give a boot
  attempt a real window (5+ min) before concluding it's stuck; if it dies
  repeatedly, don't keep retrying blindly — report it and prefer a connected
  physical device for verification when one is available.
