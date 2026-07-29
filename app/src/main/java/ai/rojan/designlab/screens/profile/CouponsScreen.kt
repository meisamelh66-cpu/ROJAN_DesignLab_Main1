package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.data.demo.DemoCoupon
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta

/**
 * Journey 2, Screen 5: Coupons — now with real redemption, not just a
 * static list. Discount is computed against a representative service
 * price (the demo has no active cart/booking context to price against
 * here) — a disclosed simplification, not a hidden one. The "already
 * used" edge case is real: redeeming twice is genuinely rejected by
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine], not
 * just described as a rule.
 */
@Composable
fun CouponsScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val referencePrice = catalogEngine.firstService()?.price ?: 0
    val usedCouponIds = ecosystemViewModel.state.usedCouponIds
    val coupons = ecosystemViewModel.allCoupons()

    WarmBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("کدهای تخفیف", style = RojanTypography.HeroTitle, color = RojanTextOnGlass) }

            if (coupons.isEmpty()) {
                item {
                    RojanEmptyState(
                        title = "کد تخفیفی موجود نیست",
                        description = "در حال حاضر کد تخفیفی برای شما وجود ندارد",
                        icon = Icons.Filled.CardGiftcard,
                    )
                }
            } else {
                items(coupons) { coupon ->
                    CouponCard(
                        coupon = coupon,
                        isUsed = coupon.id in usedCouponIds,
                        onRedeem = {
                            // Result surfaces via isUsed flipping (redeemed) or staying
                            // used (rejected) - state-driven UI is the primary feedback
                            // mechanism here, consistent with the rest of this app.
                            ecosystemViewModel.redeemCoupon(coupon, referencePrice)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CouponCard(coupon: DemoCoupon, isUsed: Boolean, onRedeem: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = if (isUsed) RojanTextSecondary else RojanAIGlow)

            Column(modifier = Modifier.weight(1f).padding(horizontal = RojanDimens.SpaceSM)) {
                Text(coupon.title, style = RojanTypography.Body, color = RojanTextPrimary)
                Text(coupon.description, style = RojanTypography.Caption, color = RojanTextSecondary)
                Text("کد: ${coupon.code}  •  تا ${coupon.expiryLabel}", style = RojanTypography.Caption, color = RojanTextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${coupon.discountPercent}٪", style = RojanTypography.HeroTitle, color = RojanAIGlow)
                Text(
                    text = if (isUsed) "استفاده شده" else "استفاده از کد",
                    style = RojanTypography.Caption,
                    color = if (isUsed) RojanVividMagenta else RojanStatusOnline,
                    modifier = if (isUsed) Modifier else Modifier.clickable(onClick = onRedeem),
                )
            }
        }
    }
}
