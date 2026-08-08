package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelFactory
import ai.rojan.designlab.manager.screens.splash.ManagerSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * OTP Authentication Entry Flow Integration — the real authentication gate
 * for the Manager App, replacing the previous unconditional
 * `ManagerActivity` -> `ManagerSplashScreen` -> Dashboard wiring (see this
 * integration's own report, §"Current Problem").
 *
 * Mirrors [ai.rojan.designlab.navigation.RojanNavGraph]'s established
 * "splash, then decide" shape, but **stricter**: Customer's graph picks a
 * `startDestination` optimistically (from a merely-persisted personId,
 * before the async backend validation finishes) and relies on a reactive
 * guard composable to redirect away if that validation later fails -
 * acceptable there because Customer has real anonymous-browse screens to
 * fall back to. The Manager App has no such screen; every destination in
 * [managerNavGraph] is privileged. So this gate does the stricter, fully
 * correct thing instead: it waits for [ManagerAuthViewModel.authState] to
 * leave [ManagerAuthState.Checking] (the real, validated result -
 * `BackendAuthRepository.currentUser()`, transparently refreshed if needed)
 * *before* the `NavHost` (and therefore its `startDestination`) is even
 * created. The Dashboard route is never composed for a caller whose
 * session hasn't actually been validated yet - not "redirected away from
 * shortly after", never entered in the first place.
 */
@Composable
fun ManagerRootGraph() {
    val appContext = LocalContext.current.applicationContext

    val authViewModel: ManagerAuthViewModel = viewModel(
        factory = ManagerAuthViewModelFactory(appContext),
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Cosmetic minimum splash duration, independent of how fast/slow the
    // real session-validation network call is - avoids an unpleasant
    // flash of the splash screen on a fast connection/cached response,
    // without ever letting it substitute for the real check below.
    var minDisplayElapsed by remember { mutableStateOf(false) }

    if (!minDisplayElapsed || authState is ManagerAuthState.Checking) {
        ManagerSplashScreen(onSplashFinished = { minDisplayElapsed = true })
        return
    }

    val navController = rememberNavController()

    // Captured once: authState has already left Checking by this point
    // (guaranteed by the guard above), so this is the real, validated
    // result - not re-evaluated on a later authState change (e.g. a
    // subsequent logout), exactly the same "capture once" reasoning
    // RojanNavGraph.kt's own startDestination uses, for the same reason
    // (a later change must navigate explicitly, e.g. via the OTP screen's
    // onAuthenticated / Profile's logout callbacks in managerNavGraph, not
    // by silently rebuilding this NavHost and resetting its back stack).
    val startDestination = remember {
        if (authState is ManagerAuthState.Authenticated) {
            ManagerDestinations.DASHBOARD
        } else {
            ManagerDestinations.OTP_AUTH
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        managerNavGraph(navController, authViewModel)
    }
}
