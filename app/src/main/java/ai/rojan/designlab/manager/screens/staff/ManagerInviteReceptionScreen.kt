package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.presentation.staff.InviteReceptionUiState
import ai.rojan.designlab.manager.presentation.staff.ManagerInviteReceptionViewModel
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Gap P0-3-REC-INVITE-UI - owner-only in effect (see
 * [ManagerInviteReceptionViewModel]'s own doc comment on how that's
 * actually enforced). Reached from [ManagerStaffScreen] via a new "دعوت
 * پذیرش" entry point, same standalone-screen shape as
 * [ai.rojan.designlab.manager.screens.settings.ManagerWorkingHoursScreen].
 * The real [SalonInvite.token] is shown exactly once, selectable for the
 * owner to copy and share out-of-band with the real person being invited -
 * never logged, never sent anywhere by this app itself.
 */
@Composable
fun ManagerInviteReceptionScreen(
    viewModel: ManagerInviteReceptionViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val current = state) {
            is InviteReceptionUiState.Idle -> InviteIdleContent(onCreateClick = viewModel::createInvite)
            is InviteReceptionUiState.Creating -> InviteCreatingContent()
            is InviteReceptionUiState.Created -> InviteCreatedContent(invite = current.invite)
            is InviteReceptionUiState.Error -> InviteErrorContent(message = current.message, onRetryClick = viewModel::createInvite)
        }
    }
}

@Composable
private fun InviteIdleContent(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(text = "دعوت پذیرش", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary)
        Text(
            text = "یک دعوت‌نامه برای عضویت به‌عنوان «پذیرش» در این سالن ایجاد کنید. این دعوت‌نامه یک‌بار قابل استفاده است و پس از مدتی منقضی می‌شود.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
        )
        ManagerPrimaryButton(text = "ایجاد دعوت‌نامه", onClick = onCreateClick)
    }
}

@Composable
private fun InviteCreatingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun InviteCreatedContent(invite: SalonInvite) {
    Column(
        modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(text = "دعوت‌نامه پذیرش ایجاد شد", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary)

        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                Text(text = "نقش: پذیرش", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                Text(text = "تاریخ انقضا: ${invite.expiresAt}", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                Text(
                    text = "کد دعوت‌نامه را برای فرد مدنظر ارسال کنید تا پس از ورود، آن را در اپلیکیشن پذیرش وارد کند:",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
                SelectionContainer {
                    Text(text = invite.token, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun InviteErrorContent(message: String, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = RojanTypography.Body, color = RojanErrorText)
        ManagerPrimaryButton(text = "تلاش مجدد", onClick = onRetryClick)
    }
}
