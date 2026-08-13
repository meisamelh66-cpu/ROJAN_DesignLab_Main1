package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Wizard entry point — customer/service/specialist/date/time in that order, mirroring the Manager booking journey's own established step order. */
@Composable
fun ReceptionBookingStartScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
) {
    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            Text(text = "ثبت نوبت جدید", style = RojanTypography.HeroTitle, color = ReceptionPalette.textPrimary)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
            ReceptionGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Text(
                    text = "برای ثبت نوبت، ابتدا مشتری، سپس خدمت، متخصص و زمان را انتخاب کنید.",
                    style = RojanTypography.Body,
                    color = ReceptionPalette.textSecondary,
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                )
            }
            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))
            PremiumButton(text = "شروع", onClick = onStartClick)
        }
    }
}
