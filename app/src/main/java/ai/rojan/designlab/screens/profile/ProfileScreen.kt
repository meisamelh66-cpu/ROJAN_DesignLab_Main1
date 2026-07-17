package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.customer.insights.ProfileInsightsEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

private data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/**
 * Journey 2, Screen 1: Profile hub — the customer's real identity
 * center. Wallet/Loyalty/Membership summary read live shared state;
 * Beauty Score, Profile Completion, Preferred Salon/Specialist,
 * Upcoming Appointment, Last Visit, and Recent Activity are computed
 * via [ProfileInsightsEngine] — real derivations and (where genuinely a
 * judgment call) real rule-provider-backed calculations, not hardcoded
 * display strings.
 */
@Composable
fun ProfileScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onAppointmentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onWalletClick: () -> Unit,
    onCouponsClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onLoyaltyClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onBeautyTimelineClick: () -> Unit,
) {
    val tier = ecosystemViewModel.membershipTier()
    val state = ecosystemViewModel.state
    val insights = remember(state) { ProfileInsightsEngine().computeInsights(state) }

    val menuItems = listOf(
        ProfileMenuItem(Icons.Filled.CalendarMonth, "نوبت‌های من", "مشاهده نوبت‌های آینده و گذشته", onAppointmentsClick),
        ProfileMenuItem(Icons.Filled.Favorite, "علاقه‌مندی‌ها", "سالن‌های ذخیره‌شده", onFavoritesClick),
        ProfileMenuItem(Icons.Filled.AccountBalanceWallet, "کیف پول", "موجودی و تراکنش‌ها", onWalletClick),
        ProfileMenuItem(Icons.Filled.CardGiftcard, "کدهای تخفیف", "تخفیف‌های فعال شما", onCouponsClick),
        ProfileMenuItem(Icons.Filled.WorkspacePremium, "عضویت", "سطح عضویت و مزایا", onMembershipClick),
        ProfileMenuItem(Icons.Filled.Stars, "امتیازات وفاداری", "امتیازهای کسب‌شده", onLoyaltyClick),
        ProfileMenuItem(Icons.Filled.RateReview, "نظرات من", "نظراتی که ثبت کرده‌اید", onReviewsClick),
        ProfileMenuItem(Icons.Filled.History, "تاریخچه زیبایی", "خدمات دریافت‌شده در طول زمان", onBeautyTimelineClick),
    )

    PremiumBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = RojanTextOnGlass)
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(RojanAIGlow.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = RojanTextOnGlass, modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    Text("رها احمدی", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)
                    Text("عضو ${tier.currentTierName}", style = RojanTypography.Body, color = RojanTextOnDarkSurface)
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD)
                            .clickable(onClick = onWalletClick),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.walletBalance}", style = RojanTypography.Body, color = RojanAIGlow)
                            Text("موجودی کیف پول", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.loyaltyPoints}", style = RojanTypography.Body, color = RojanAIGlow)
                            Text("امتیاز وفاداری", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tier.currentTierName, style = RojanTypography.Body, color = RojanAIGlow)
                            Text("سطح عضویت", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                    }
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${insights.beautyScore}", style = RojanTypography.Body, color = RojanAIGlow)
                            Text("امتیاز زیبایی", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${insights.profileCompletionPercent}٪", style = RojanTypography.Body, color = RojanAIGlow)
                            Text("تکمیل پروفایل", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.customerBirthday, style = RojanTypography.Body, color = RojanAIGlow)
                            Text("تولد", style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                    }
                }
            }

            if (insights.upcomingAppointment != null || insights.lastVisit != null || insights.preferredSalonName != null) {
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                            insights.upcomingAppointment?.let {
                                InsightRow("نوبت پیش‌رو", "${it.salonName} • ${it.dateLabel}")
                            }
                            insights.lastVisit?.let {
                                InsightRow("آخرین مراجعه", "${it.salonName} • ${it.dateLabel}")
                            }
                            insights.preferredSalonName?.let {
                                InsightRow("سالن مورد علاقه", it)
                            }
                            insights.preferredSpecialistName?.let {
                                InsightRow("متخصص مورد علاقه", it)
                            }
                            InsightRow("تعداد نوبت‌های انجام‌شده", "${insights.completedAppointmentCount}")
                            insights.daysSinceLastVisit?.let {
                                InsightRow("روز از آخرین مراجعه", "$it روز")
                            }
                        }
                    }
                }
            }

            if (insights.recentActivity.isNotEmpty()) {
                item { Text("فعالیت‌های اخیر", style = RojanTypography.Body, color = RojanTextOnGlass) }
                items(insights.recentActivity) { activity ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RojanDimens.SpaceMD),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(activity.label, style = RojanTypography.Caption, color = RojanTextPrimary)
                            Text(activity.dateLabel, style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                    }
                }
            }

            items(menuItems) { menuItem ->
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = menuItem.onClick),
                    shape = RojanShapes.Small,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(menuItem.icon, contentDescription = null, tint = RojanAIGlow)
                        Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(menuItem.title, style = RojanTypography.Body, color = RojanTextPrimary)
                            Text(menuItem.subtitle, style = RojanTypography.Caption, color = RojanTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = RojanTypography.Caption, color = RojanTextSecondary)
        Text(value, style = RojanTypography.Caption, color = RojanTextPrimary)
    }
}
