package ai.rojan.designlab.screens.bookingflow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.screens.bookingflow.components.BookingAccent
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.RefPrimaryButton
import ai.rojan.designlab.screens.bookingflow.components.RefRowDivider
import ai.rojan.designlab.screens.bookingflow.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Optional recap shown on the success screen. Constructed by the caller from
 * the just-confirmed [ai.rojan.designlab.presentation.booking.BookingSummary]
 * (a `data class`, no logic). Defaulted to `null` so the current
 * `RojanNavGraph` call site — which passes only `onDoneClick` — compiles and
 * behaves unchanged; wiring the real values through is a one-line navigation
 * change intentionally left for the Confirmation-screen task.
 */
data class BookingSuccessSummary(
    val salonName: String,
    val serviceName: String,
    val dateLabel: String,
    val timeLabel: String,
)

/**
 * Journey 1, Screen 8: Success — end of the flow.
 *
 * Quiet Luxury pass: the 96dp violet glow disc + filled `CheckCircle` +
 * gradient pill are replaced by a calm state on the dark ground — a single
 * outlined rose-gold check, a plain headline, one clear next action. The only
 * motion is a 350ms alpha settle (no scale, spring, or bounce).
 *
 * [onDoneClick] still pops the entire booking sub-graph off the back stack
 * (handled by the caller in [ai.rojan.designlab.navigation.RojanNavGraph]),
 * which is also what clears
 * [ai.rojan.designlab.presentation.booking.BookingViewModel]'s state — this
 * screen stays a pure presentation layer, no ViewModel, no reset call. Visual
 * only: no business logic, navigation, repository, or API touched.
 */
@Composable
fun BookingSuccessScreen(
    onDoneClick: () -> Unit,
    summary: BookingSuccessSummary? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "success-settle",
    )

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha)
                .padding(horizontal = BookingScreenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = BookingAccent,
                modifier = Modifier.size(56.dp),
            )

            Spacer(Modifier.height(RojanDimens.SpaceLG))

            Text(
                "رزرو شما با موفقیت ثبت شد",
                style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                color = HomeColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(RojanDimens.SpaceSM))

            Text(
                "پیامک تایید به شماره شما ارسال خواهد شد",
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            summary?.let { recap ->
                Spacer(Modifier.height(RojanDimens.SpaceXL))
                RefSurface {
                    Column(Modifier.fillMaxWidth()) {
                        SuccessRecapRow("سالن", recap.salonName)
                        RefRowDivider()
                        SuccessRecapRow("خدمت", recap.serviceName)
                        RefRowDivider()
                        SuccessRecapRow("تاریخ", recap.dateLabel)
                        RefRowDivider()
                        SuccessRecapRow("ساعت", recap.timeLabel)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            RefPrimaryButton(
                label = "بازگشت به خانه",
                onClick = onDoneClick,
                modifier = Modifier.padding(bottom = RojanDimens.SpaceMD),
            )
        }
    }
}

/** Static recap row: value on the left, label anchored right (RTL reading). */
@Composable
private fun SuccessRecapRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(
            value,
            style = RojanTypography.Body,
            color = HomeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            label,
            style = RojanTypography.Caption,
            color = HomeColors.TextMuted,
        )
    }
}
