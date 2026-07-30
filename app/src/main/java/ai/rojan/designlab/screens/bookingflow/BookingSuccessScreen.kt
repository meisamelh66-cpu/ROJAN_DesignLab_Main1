package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Journey 1, Screen 8: Success — end of the flow.
 *
 * [onDoneClick] is expected to pop the entire booking sub-graph off the
 * back stack, handled by the caller in
 * [ai.rojan.designlab.navigation.RojanNavGraph] rather than here,
 * keeping this screen a pure presentation layer. Popping the whole
 * sub-graph is also what clears
 * [ai.rojan.designlab.presentation.booking.BookingViewModel]'s state
 * automatically (see that class's own doc comment) — no manual reset
 * call needed here.
 */
@Composable
fun BookingSuccessScreen(
    onDoneClick: () -> Unit,
) {
    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(HomeColors.Glow.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                RojanIconContainer(
    imageVector = Icons.Filled.CheckCircle,
    contentDescription = null,
    tint = HomeColors.Glow,
    size = RojanIconSize.XLarge,
)
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text(
                text = "رزرو شما با موفقیت ثبت شد",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

            Text(
                text = "پیامک تایید به شماره شما ارسال خواهد شد",
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            PremiumButton(
                text = "بازگشت به خانه",
                onClick = onDoneClick,
            )
        }
    }
}
