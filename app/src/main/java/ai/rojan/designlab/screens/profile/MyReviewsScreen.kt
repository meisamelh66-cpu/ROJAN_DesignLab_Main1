package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoUserReview
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/** Journey 2, Screen 8: My Reviews — now reads live shared state, consistent with the rest of the ecosystem (item 12). */
@Composable
fun MyReviewsScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    WarmBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("نظرات من", style = RojanTypography.HeroTitle, color = RojanTextOnGlass) }

            if (ecosystemViewModel.state.reviews.isEmpty()) {
                item {
                    RojanEmptyState(
                        title = "هنوز نظری ثبت نکرده‌اید",
                        description = "پس از دریافت خدمت می‌توانید نظر خود را ثبت کنید",
                        icon = Icons.Filled.Star,
                    )
                }
            } else {
                items(ecosystemViewModel.state.reviews) { review ->
                    MyReviewCard(review)
                }
            }
        }
    }
}

@Composable
private fun MyReviewCard(review: DemoUserReview) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(review.salonName, style = RojanTypography.Body, color = RojanTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = RojanRatingGold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text(" ${review.rating}", style = RojanTypography.Caption, color = RojanTextSecondary)
                }
            }
            Text(review.comment, style = RojanTypography.Caption, color = RojanTextSecondary)
            Text(review.dateLabel, style = RojanTypography.Caption, color = RojanTextSecondary)
        }
    }
}
