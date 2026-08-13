package ai.rojan.designlab.reception.screens.auth

import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Reception App's resolved terminal state for a failed salon-access
 * resolution — reached from [ai.rojan.designlab.reception.navigation.ReceptionRootGraph]
 * (cold-start restore) or from the OTP screen (fresh login) whenever
 * [ReceptionAuthViewModel.activeSalonState] settles on
 * [ActiveSalonUiState.Error]. Added in Phase 1 (authentication completion)
 * specifically to close the gap [ReceptionAuthViewModel.refreshIdentityContext]'s
 * own doc comment describes: previously this state was unreachable and the
 * app hung on an infinite splash screen instead.
 *
 * Navigation-agnostic like every other screen in this flow: never calls a
 * NavController itself, only [onResolved] (fired once [ActiveSalonUiState]
 * leaves [ActiveSalonUiState.Error]/[ActiveSalonUiState.Loading] after a
 * successful retry) and [onLogoutClick].
 */
@Composable
fun ReceptionAccessErrorScreen(
    viewModel: ReceptionAuthViewModel,
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

    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceLG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "دسترسی به سالن امکان‌پذیر نیست",
                        style = RojanTypography.CardTitle,
                        color = ReceptionPalette.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = message,
                        style = RojanTypography.Body,
                        color = ReceptionPalette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    )
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            PremiumButton(
                text = "تلاش مجدد",
                onClick = viewModel::retryIdentityResolution,
                enabled = !isRetrying,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = RojanDimens.ButtonWidth, height = RojanDimens.ButtonHeight),
            )

            TextButton(
                onClick = onLogoutClick,
                enabled = !isRetrying,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("خروج از حساب", color = ReceptionPalette.textSecondary)
            }
        }
    }
}
