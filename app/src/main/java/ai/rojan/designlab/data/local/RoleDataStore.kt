package ai.rojan.designlab.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single top-level DataStore instance for role-related preferences,
 * following the standard `preferencesDataStore` delegate pattern — one
 * DataStore per name for the whole app process. Always accessed via
 * [Context.applicationContext] by callers to avoid creating more than
 * one instance pointed at the same file.
 */
val Context.roleDataStore: DataStore<Preferences> by preferencesDataStore(name = "role_preferences")

/** Preference keys used by [ai.rojan.designlab.data.repository.RoleRepositoryImpl]. */
internal object RolePreferencesKeys {
    val SELECTED_ROLE = stringPreferencesKey("selected_role")

    /**
     * Post Build UX Flow Audit — hard safeguard against Manager
     * Dashboard (or any role dashboard) ever appearing automatically
     * without a genuine, explicit Welcome-screen tap. An exhaustive
     * audit of every layer in the restore chain (repository, session
     * ViewModel, role-type mapping, card wiring, backup-exclusion
     * rules) found no defect, yet the reported symptom persisted — so
     * rather than relying on [SELECTED_ROLE] alone (a single value that
     * could in principle be wrong for a reason this environment can't
     * fully reproduce or rule out), auto-routing now requires BOTH this
     * flag AND [SELECTED_ROLE] to be present together. This flag is
     * set *only* inside [ai.rojan.designlab.data.repository.RoleRepositoryImpl.saveSelectedRole]
     * — i.e. only as the direct result of a real, explicit role
     * selection — never as a side effect of anything else, so there is
     * no code path where [SELECTED_ROLE] could exist without this flag
     * also genuinely being true.
     */
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
