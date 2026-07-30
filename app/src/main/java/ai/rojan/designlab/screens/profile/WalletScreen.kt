package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.data.demo.DemoWalletTransaction
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta

/**
 * Journey 2, Screen 4: Wallet — now reads the live, shared
 * [CustomerEcosystemViewModel.state] instead of the static repository
 * directly, so a cashback earned via [ai.rojan.designlab.screens.profile.AppointmentsScreen]'s
 * "Complete Appointment" action appears here immediately, balance and
 * transaction list both, with a clear transaction source per the
 * Wallet spec's explicit requirement.
 */
@Composable
fun WalletScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    val state = ecosystemViewModel.state

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("کیف پول", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            item {
                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceLG),
                    ) {
                        Text("موجودی فعلی", style = RojanTypography.Body, color = HomeColors.TextSecondary)
                        Text(
                            "${state.walletBalance} تومان",
                            style = RojanTypography.HeroTitle,
                            color = HomeColors.Glow,
                        )
                    }
                }
            }

            item { Text("تراکنش‌های اخیر", style = RojanTypography.Body, color = HomeColors.TextPrimary) }

            itemsIndexed(state.walletTransactions) { index, txn ->
                TransactionRow(txn, animationDelayMillis = index * 60)
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: DemoWalletTransaction, animationDelayMillis: Int = 0) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (txn.isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = if (txn.isCredit) RojanStatusOnline else RojanVividMagenta,
                )
                Column(modifier = Modifier.padding(start = RojanDimens.SpaceSM)) {
                    Text(txn.title, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Text(txn.dateLabel, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
            }
            Text(
                text = "${if (txn.isCredit) "+" else "-"}${txn.amount} تومان",
                style = RojanTypography.Body,
                color = if (txn.isCredit) RojanStatusOnline else RojanVividMagenta,
            )
        }
    }
}
