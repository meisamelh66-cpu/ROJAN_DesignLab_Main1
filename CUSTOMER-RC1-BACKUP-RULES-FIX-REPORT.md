# ROJAN Customer Android — RC-1 P1 Fix: Backup Exclusion Rules

**Date:** 2026-09-10
**Scope:** Customer app only. Fixes the single P1 finding from `CUSTOMER-RC1-SECURITY-AUDIT.md` (§3 / §1, "Backup Exclusion ناقص/ناهم‌خوان").
**Status:** ✅ Complete. Compile + lint + assemble all green. Not committed.

---

## 1. Inspection (as required by the task)

- `app/src/main/res/xml/backup_rules.xml` (`full-backup-content`, the pre-API-31 fallback mechanism).
- `app/src/main/res/xml/data_extraction_rules.xml` (the API 31+ mechanism — `<cloud-backup>` + `<device-transfer>`).
- `app/src/main/AndroidManifest.xml` — confirmed `android:allowBackup="true"`, `android:fullBackupContent="@xml/backup_rules"`, `android:dataExtractionRules="@xml/data_extraction_rules"` already correctly reference both files (unchanged — no edit needed here; the manifest wiring was already correct, only the exclusion *content* was incomplete).

## 2. Exact storage locations identified

| Data | Mechanism | File name | Backup domain/path |
|---|---|---|---|
| `auth_session_preferences` (logged-in person's id) | Preferences DataStore, `by preferencesDataStore(name = "auth_session_preferences")` — `app/src/main/java/ai/rojan/designlab/data/local/AuthSessionDataStore.kt:21` | physical file: `datastore/auth_session_preferences.preferences_pb` | `domain="file"`, `path="datastore/auth_session_preferences.preferences_pb"` |
| Access token + refresh token | Plain `SharedPreferences("secure_token_preferences", MODE_PRIVATE)` — `app/src/main/java/ai/rojan/designlab/data/local/SecureTokenStore.kt:72-73` — every value AES-256-GCM-encrypted via Android Keystore before being written (`TokenCipher`, same file, lines 30-66) | physical file: `shared_prefs/secure_token_preferences.xml` | `domain="sharedpref"`, `path="secure_token_preferences.xml"` |
| `role_preferences` (already excluded, pre-existing) | Confirmed **retired** — `grep -rln "role_preferences\|RoleDataStore" app/src/main/java` returns zero matches. The class this exclusion was written for no longer exists in the codebase. | `datastore/role_preferences.preferences_pb` | already present, left in place (see §4) |
| `active_salon_preferences` | Preferences DataStore, `data/local/ActiveSalonDataStore.kt:16` | `datastore/active_salon_preferences.preferences_pb` | **Deliberately NOT added — see §4.** |

No other DataStore/SharedPreferences file in Customer scope holds authentication or session-identifying data — confirmed via `find app/src/main/java/ai/rojan/designlab/data/local -iname "*.kt"`, which lists exactly the three files above (`ActiveSalonDataStore.kt`, `AuthSessionDataStore.kt`, `SecureTokenStore.kt`).

## 3. Changed files

Both are pure XML resource files — no Kotlin, no UI, no ViewModel, no repository, no API, no navigation, no business logic touched, per the task's explicit constraint.

- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## 4. Security reason

`role_preferences` was the only excluded file, protecting against a real, previously-diagnosed risk: Android Auto Backup silently restoring a stale identity/session onto what the user believes is a fresh install (new device, or uninstall+reinstall), causing the app to route straight into a stale dashboard instead of the correct logged-out/Welcome state. That protection was written for `RoleDataStore.kt`, which has since been retired — the *current* active session-restore mechanism, `auth_session_preferences`, was never added to either exclusion file when it took over that role. The exact same failure mode the original exclusion existed to prevent was therefore live and unprotected. `secure_token_preferences` was added alongside it: the token ciphertext already can't be decrypted after a restore onto a different Android Keystore state (the encryption key never leaves Keystore and is not itself included in Android backups), so this specific data cannot be *replayed* after a cross-device restore — but a backed-up copy of authentication material should not exist if it can be avoided at all, which is standard security guidance for token stores regardless of at-rest encryption.

**`active_salon_preferences` was deliberately NOT added**, to keep the change minimal and scoped to the audit's actual finding: it is confirmed (via `grep -rln "activeSalonContextRepository\|ActiveSalonContextRepository"`) to be read/written exclusively by Manager and Reception ViewModels — no Customer-scope screen or ViewModel ever touches it. Adding an exclusion for data the Customer app doesn't itself use would be scope creep beyond the P1 finding, not a security fix.

**`role_preferences`'s exclusion line was left in place** in both files, unmodified — excluding an already-nonexistent file is a harmless no-op, and removing it would be pure code churn with zero security benefit, which the task's "keep the change minimal" instruction argues against.

## 5. Before / after

### `backup_rules.xml`
```diff
 <full-backup-content>
     <exclude domain="file" path="datastore/role_preferences.preferences_pb"/>
+    <exclude domain="file" path="datastore/auth_session_preferences.preferences_pb"/>
+    <exclude domain="sharedpref" path="secure_token_preferences.xml"/>
 </full-backup-content>
```
(plus an added doc-comment block explaining the fix, matching this file's existing documentation style — no removal of the original comment.)

### `data_extraction_rules.xml`
```diff
 <data-extraction-rules>
     <cloud-backup>
         <exclude domain="file" path="datastore/role_preferences.preferences_pb"/>
+        <exclude domain="file" path="datastore/auth_session_preferences.preferences_pb"/>
+        <exclude domain="sharedpref" path="secure_token_preferences.xml"/>
     </cloud-backup>
     <device-transfer>
         <exclude domain="file" path="datastore/role_preferences.preferences_pb"/>
+        <exclude domain="file" path="datastore/auth_session_preferences.preferences_pb"/>
+        <exclude domain="sharedpref" path="secure_token_preferences.xml"/>
     </device-transfer>
 </data-extraction-rules>
```
(plus a short added comment cross-referencing `backup_rules.xml`'s fuller explanation, matching this file's existing style — its original comment is unchanged, only extended.)

**Net effect:** `auth_session_preferences` and `secure_token_preferences` are now excluded from both the API 31+ mechanism (`data_extraction_rules.xml`, `cloud-backup` **and** `device-transfer`) and the pre-31 fallback (`backup_rules.xml`) — full coverage across the whole supported `minSdk=24`–current API range, matching how `role_preferences` was already (correctly) covered on both mechanisms.

## 6. Verification results

```
:app:compileCustomerDevDebugKotlin  → BUILD SUCCESSFUL
:app:lintCustomerDevDebug           → BUILD SUCCESSFUL, 94 warnings (unchanged from session baseline,
                                        0 new findings, 0 findings on either changed XML file)
:app:assembleCustomerDevDebug       → BUILD SUCCESSFUL
```

Additionally spot-checked the actual packaged build output (`app/build/intermediates/packaged_res/customerDevDebug/.../xml/backup_rules.xml`) to confirm the new `<exclude>` entries survive resource merging and reach the built artifact unchanged, not just the source file.

## 7. What was intentionally not changed

- `AndroidManifest.xml` — already correctly references both XML files; no edit needed.
- No Kotlin source file — `AuthSessionDataStore.kt`, `SecureTokenStore.kt`, `TokenRepositoryImpl.kt`, `AuthViewModel.kt`, and every repository/ViewModel/navigation file are all untouched, per the task's explicit constraint.
- `active_salon_preferences` exclusion — not added, per §4's scope reasoning.
- The stale `role_preferences` exclusion line — not removed, per §4's minimal-change reasoning.
- Every other finding from `CUSTOMER-RC1-SECURITY-AUDIT.md` (`FLAG_SECURE`, explicit `usesCleartextTraffic`, Coil version, cert pinning, CI dependency scanning) — out of scope for this task, which addressed the P1 finding only.

Nothing was committed.
