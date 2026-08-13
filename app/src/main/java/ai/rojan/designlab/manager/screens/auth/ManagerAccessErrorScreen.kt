package ai.rojan.designlab.manager.screens.auth

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Manager App's resolved terminal state for a failed salon-access
 * resolution — reached from [ai.rojan.designlab.manager.navigation.ManagerRootGraph]
 * (cold-start restore) or from [ManagerOtpAuthScreen] (fresh login)
 * whenever [ManagerAuthViewModel.activeSalonState] settles on
 * [ActiveSalonUiState.Error]. Added in System2 Android Parallel Work,
 * Phase A item 3, mirroring [ai.rojan.designlab.reception.screens.auth.ReceptionAccessErrorScreen]
 * exactly — same gap, same fix, applied to the second app that had it.
 *
 * Navigation-agnostic: never calls a NavController itself, only
 * [onResolved] and [onLogoutClick].
 */
@Composable
fun ManagerAccessErrorScreen(
    viewModel: ManagerAuthViewModel,
    onResolved: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val activeSalonState by viewModel.activeSalonState.collectAsStateWithLifecycle()

    LaunchedEffect(activeSalonState) {
        val state = activeSalonState
        if (state !is ActiveSalonUiState.Error && state !is ActiveSalonUiState.Loading) {
            onResolved()
        }
    }

    val message = (activeSalonState as? ActiveSalonUiState.Error)?.message
        ?: "خطایی در دریافت اطلاعات دسترسی رخ داد"
    val isRetrying = activeSalonState is ActiveSalonUiState.Loading

    ManagerScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceLG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "دسترسی به سالن امکان‌پذیر نیست",
                        style = RojanTypography.CardTitle,
                        color = ManagerColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = message,
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    )
                }
            }

            ManagerPrimaryButton(
                text = "تلاش مجدد",
                onClick = viewModel::retryIdentityResolution,
                enabled = !isRetrying,
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )

            TextButton(onClick = onLogoutClick, enabled = !isRetrying) {
                Text("خروج از حساب", color = ManagerColors.TextSecondary)
            }
        }
    }
}
