package ai.rojan.designlab.reception.screens.profile

import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Reception App's account screen — name/phone (real, from the resolved
 * session) and the real logout action. Only reachable from
 * [ai.rojan.designlab.reception.screens.dashboard.ReceptionDashboardScreen],
 * i.e. only once [ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState]
 * has already resolved to `Active` — so unlike
 * [ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen] (which
 * takes placeholder default strings, no real identity wired), this always
 * has real caller-supplied [fullName]/[phoneNumber] by construction, not
 * defaults.
 *
 * Added in Phase 1 (authentication completion) to replace the inline
 * logout button the Phase 0 placeholder Dashboard carried directly —
 * logout now lives on its own real screen instead.
 */
@Composable
fun ReceptionProfileScreen(
    fullName: String,
    phoneNumber: String?,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    ReceptionScaffold(onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = RojanDimens.SpaceXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = fullName,
                style = RojanTypography.ScreenTitle,
                color = ReceptionPalette.textPrimary,
            )
            Text(
                text = phoneNumber ?: "—",
                style = RojanTypography.Body,
                color = ReceptionPalette.textSecondary,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS),
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceXXL))

            PremiumButton(
                text = "خروج از حساب",
                onClick = onLogoutClick,
            )
        }
    }
}
