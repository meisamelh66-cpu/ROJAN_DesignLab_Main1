package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

private data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/**
 * Journey 2, Screen 1: Profile hub — the customer's real identity
 * center.
 *
 * Production Data Integrity Phase 1: the Wallet/Loyalty/Membership
 * summary strip and the Beauty Score/Profile Completion/Preferred Salon/
 * Recent Activity "insights" card were removed — both were entirely
 * `CustomerEcosystemViewModel`/`ProfileInsightsEngine` demo derivations
 * with no backend counterpart. Each area they surfaced (Wallet, Coupons,
 * Membership, Loyalty, Reviews, Beauty Timeline) already has its own
 * menu item below, each navigating to its own honestly-gated screen
 * (Phase 2, C7) — no separate placeholder is needed here.
 *
 * Customer Journey Audit Phase A (P0-4) fix: the displayed name now
 * comes from [AuthViewModel.currentDisplayName] — a real lookup of the
 * actually signed-in person that already existed but was never called
 * from any screen — instead of a hardcoded literal shown to every user
 * regardless of who was really logged in.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onAppointmentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onWalletClick: () -> Unit,
    onCouponsClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onLoyaltyClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onBeautyTimelineClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    val menuItems = listOf(
        ProfileMenuItem(Icons.Filled.CalendarMonth, "نوبت‌های من", "مشاهده نوبت‌های آینده و گذشته", onAppointmentsClick),
        ProfileMenuItem(Icons.Filled.Favorite, "علاقه‌مندی‌ها", "سالن‌های ذخیره‌شده", onFavoritesClick),
        ProfileMenuItem(Icons.Filled.AccountBalanceWallet, "کیف پول", "موجودی و تراکنش‌ها", onWalletClick),
        ProfileMenuItem(Icons.Filled.CardGiftcard, "کدهای تخفیف", "تخفیف‌های فعال شما", onCouponsClick),
        ProfileMenuItem(Icons.Filled.WorkspacePremium, "عضویت", "سطح عضویت و مزایا", onMembershipClick),
        ProfileMenuItem(Icons.Filled.Stars, "امتیازات وفاداری", "امتیازهای کسب‌شده", onLoyaltyClick),
        ProfileMenuItem(Icons.Filled.RateReview, "نظرات من", "نظراتی که ثبت کرده‌اید", onReviewsClick),
        ProfileMenuItem(Icons.Filled.History, "تاریخچه زیبایی", "خدمات دریافت‌شده در طول زمان", onBeautyTimelineClick),
        ProfileMenuItem(Icons.Filled.Logout, "خروج از حساب", "خروج از حساب کاربری", onLogoutClick),
    )

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                GlassBackButton(onClick = onBackClick)
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(HomeColors.Glow.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = HomeColors.TextPrimary, modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    Text(authViewModel.currentDisplayName ?: "کاربر", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                    currentUser?.email?.let {
                        Text(it, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                    }
                }
            }

            itemsIndexed(menuItems) { index, menuItem ->
                HomeGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rojanEnterAnimation(delayMillis = index * 60)
                        .rojanPressable(onClick = menuItem.onClick),
                    shape = RojanShapes.Small,
                ) {
                    RtlListRow(
                        title = menuItem.title,
                        titleColor = HomeColors.TextPrimary,
                        subtitle = menuItem.subtitle,
                        subtitleColor = HomeColors.TextSecondary,
                        icon = menuItem.icon,
                        iconTint = HomeColors.Glow,
                        modifier = Modifier.padding(RojanDimens.SpaceMD),
                    )
                }
            }
        }
    }
}
