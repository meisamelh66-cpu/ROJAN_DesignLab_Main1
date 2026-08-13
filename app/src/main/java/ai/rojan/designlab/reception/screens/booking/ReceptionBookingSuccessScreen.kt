package ai.rojan.designlab.reception.screens.booking

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ReceptionBookingSuccessScreen(onDoneClick: () -> Unit) {
    ReceptionScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "نوبت با موفقیت ثبت شد", style = RojanTypography.HeroTitle, color = ReceptionPalette.textPrimary)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))
            PremiumButton(text = "پایان", onClick = onDoneClick)
        }
    }
}
