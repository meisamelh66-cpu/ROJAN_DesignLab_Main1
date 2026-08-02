package ai.rojan.designlab.di

import android.content.Context

/**
 * Process-wide lazy singleton for [BackendApiContainer]. `RojanNavGraph.kt`
 * is a single ~1300-line composable already wiring the rest of the app;
 * wrapping its whole body in a `CompositionLocalProvider` just to hand
 * screens this container would be a large, risky edit to that file for a
 * dependency only the new backend-integration screens need. This holder
 * gets the same "construct once" property (see [BackendApiContainer]'s own
 * doc comment on why that matters) without touching `RojanNavGraph.kt` at
 * all — screens call [get] with any [Context] and it resolves to the one
 * instance, built from the application context to avoid leaking an
 * Activity/Composable context.
 */
object BackendApiContainerHolder {

    @Volatile
    private var instance: BackendApiContainer? = null

    fun get(context: Context): BackendApiContainer {
        return instance ?: synchronized(this) {
            instance ?: BackendApiContainer(context.applicationContext).also { instance = it }
        }
    }
}
