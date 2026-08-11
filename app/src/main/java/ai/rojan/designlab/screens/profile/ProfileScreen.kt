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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
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
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
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
 * Restructured into named sections (Personal Information / Beauty DNA /
 * Appointments / Favorite Salons / Settings), per the Customer App
 * architecture spec — was previously one flat, undifferentiated menu list.
 * Wallet/Coupons/Membership/Loyalty/Reviews/Beauty Timeline are existing
 * features, kept (not removed) under their own additional "امکانات حساب"
 * group rather than folded into Settings, which would mislabel them.
 *
 * Production Data Integrity Phase 1: the Wallet/Loyalty/Membership
 * summary strip and the Beauty Score/Profile Completion/Preferred Salon/
 * Recent Activity "insights" card were removed — both were entirely
 * `CustomerEcosystemViewModel`/`ProfileInsightsEngine` demo derivations
 * with no backend counterpart. Each area they surfaced already has its
 * own menu item below, each navigating to its own honestly-gated screen.
 *
 * Customer Journey Audit Phase A (P0-4) fix: the displayed name comes
 * from [AuthViewModel.currentDisplayName] — a real lookup of the actually
 * signed-in person — instead of a hardcoded literal.
 *
 * Personal Information shows only backend-real fields (name, verified
 * phone, email) - birthday/city/photo are intentionally not shown: no
 * such fields exist on the backend `User` model and no update-profile
 * endpoint exists at all yet (confirmed against `UserController`/`User.kt`
 * in ROJAN_Backend), so showing them would mean either fabricating data
 * or a form with nothing real to submit to. A non-null phone number is
 * always verified - the backend's only path to setting it is completing
 * real OTP verification, so no separate "unverified" state exists to
 * represent.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onBeautyDnaClick: () -> Unit,
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

    val accountFeatureItems = listOf(
        ProfileMenuItem(Icons.Filled.AccountBalanceWallet, "کیف پول", "موجودی و تراکنش‌ها", onWalletClick),
        ProfileMenuItem(Icons.Filled.CardGiftcard, "کدهای تخفیف", "تخفیف‌های فعال شما", onCouponsClick),
        ProfileMenuItem(Icons.Filled.WorkspacePremium, "عضویت", "سطح عضویت و مزایا", onMembershipClick),
        ProfileMenuItem(Icons.Filled.Stars, "امتیازات وفاداری", "امتیازهای کسب‌شده", onLoyaltyClick),
        ProfileMenuItem(Icons.Filled.RateReview, "نظرات من", "نظراتی که ثبت کرده‌اید", onReviewsClick),
        ProfileMenuItem(Icons.Filled.History, "تاریخچه زیبایی", "خدمات دریافت‌شده در طول زمان", onBeautyTimelineClick),
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
                }
            }

            item { RtlSectionHeader("اطلاعات شخصی", color = HomeColors.TextPrimary) }
            item {
                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                    ) {
                        RtlListRow(
                            title = authViewModel.currentDisplayName ?: "کاربر",
                            titleColor = HomeColors.TextPrimary,
                            icon = Icons.Filled.Person,
                            iconTint = HomeColors.Glow,
                        )
                        currentUser?.phoneNumber?.let { phone ->
                            RtlListRow(
                                title = phone,
                                subtitle = "تایید شده",
                                titleColor = HomeColors.TextPrimary,
                                subtitleColor = HomeColors.Glow,
                                icon = Icons.Filled.Phone,
                                iconTint = HomeColors.Glow,
                                trailingIcon = Icons.Filled.VerifiedUser,
                            )
                        }
                        currentUser?.email?.let { email ->
                            RtlListRow(
                                title = email,
                                titleColor = HomeColors.TextPrimary,
                                icon = Icons.Filled.Email,
                                iconTint = HomeColors.Glow,
                            )
                        }
                    }
                }
            }

            item { RtlSectionHeader("بیوتی دی‌ان‌ای", color = HomeColors.TextPrimary) }
            item {
                MenuRow(
                    icon = Icons.Filled.Spa,
                    title = "بیوتی دی‌ان‌ای من",
                    subtitle = "مو، پوست و ناخن",
                    onClick = onBeautyDnaClick,
                )
            }

            item { RtlSectionHeader("نوبت‌ها", color = HomeColors.TextPrimary) }
            item {
                MenuRow(
                    icon = Icons.Filled.CalendarMonth,
                    title = "نوبت‌های من",
                    subtitle = "مشاهده نوبت‌های آینده و گذشته",
                    onClick = onAppointmentsClick,
                )
            }

            item { RtlSectionHeader("سالن‌های مورد علاقه", color = HomeColors.TextPrimary) }
            item {
                MenuRow(
                    icon = Icons.Filled.Favorite,
                    title = "علاقه‌مندی‌ها",
                    subtitle = "سالن‌های ذخیره‌شده",
                    onClick = onFavoritesClick,
                )
            }

            item { RtlSectionHeader("امکانات حساب", color = HomeColors.TextPrimary) }
            itemsIndexed(accountFeatureItems) { index, menuItem ->
                MenuRow(
                    icon = menuItem.icon,
                    title = menuItem.title,
                    subtitle = menuItem.subtitle,
                    onClick = menuItem.onClick,
                    animationDelayMillis = index * 60,
                )
            }

            item { RtlSectionHeader("تنظیمات", color = HomeColors.TextPrimary) }
            item {
                MenuRow(
                    icon = Icons.Filled.Logout,
                    title = "خروج از حساب",
                    subtitle = "خروج از حساب کاربری",
                    onClick = onLogoutClick,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    animationDelayMillis: Int = 0,
) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        RtlListRow(
            title = title,
            titleColor = HomeColors.TextPrimary,
            subtitle = subtitle,
            subtitleColor = HomeColors.TextSecondary,
            icon = icon,
            iconTint = HomeColors.Glow,
            modifier = Modifier.padding(RojanDimens.SpaceMD),
        )
    }
}
