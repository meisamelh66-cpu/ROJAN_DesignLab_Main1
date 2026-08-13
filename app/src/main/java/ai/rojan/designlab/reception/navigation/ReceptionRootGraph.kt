package ai.rojan.designlab.reception.navigation

import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModelFactory
import ai.rojan.designlab.reception.screens.splash.ReceptionSplashScreen
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
 * The real authentication gate for the Reception App — same shape and
 * same reasoning as
 * [ai.rojan.designlab.manager.navigation.ManagerRootGraph]: waits for
 * [ReceptionAuthViewModel.authState] to leave [ReceptionAuthState.Checking]
 * (and, once authenticated, for [ReceptionAuthViewModel.activeSalonState] to
 * leave [ActiveSalonUiState.Loading]) *before* the `NavHost` (and therefore
 * its `startDestination`) is even created. Every destination in
 * [receptionNavGraph] is privileged — there is no anonymous-browse screen
 * to fall back to, same as Manager.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionRootGraph() {
    val appContext = LocalContext.current.applicationContext

    val authViewModel: ReceptionAuthViewModel = viewModel(
        factory = ReceptionAuthViewModelFactory(appContext),
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val activeSalonState by authViewModel.activeSalonState.collectAsStateWithLifecycle()

    // Cosmetic minimum splash duration, independent of the real
    // session-validation network call's speed.
    var minDisplayElapsed by remember { mutableStateOf(false) }

    val activeSalonPending = authState is ReceptionAuthState.Authenticated && activeSalonState is ActiveSalonUiState.Loading

    if (!minDisplayElapsed || authState is ReceptionAuthState.Checking || activeSalonPending) {
        ReceptionSplashScreen(onSplashFinished = { minDisplayElapsed = true })
        return
    }

    val navController = rememberNavController()

    // Captured once — see ManagerRootGraph.kt's identical "capture once"
    // reasoning for startDestination. The Error branch (Phase 1,
    // authentication completion) is what makes a cold-start restore whose
    // `/salon-access` call fails land on a real, navigable screen instead
    // of the splash gate hanging forever — see
    // ReceptionAuthViewModel.refreshIdentityContext's own doc comment for
    // why that was previously impossible to reach.
    val startDestination = remember {
        when {
            authState !is ReceptionAuthState.Authenticated -> ReceptionDestinations.OTP_AUTH
            activeSalonState is ActiveSalonUiState.SelectionRequired -> ReceptionDestinations.SALON_SELECTION
            activeSalonState is ActiveSalonUiState.Error -> ReceptionDestinations.ACCESS_ERROR
            else -> ReceptionDestinations.DASHBOARD
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        receptionNavGraph(navController, authViewModel)
    }
}
