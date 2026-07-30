package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 3: Favorites — real shared mutable state (item 6 of
 * the Journey 2 completion checklist), not a static read of
 * [DemoSalonRepository] anymore. The heart icon genuinely toggles via
 * [CustomerEcosystemEngine.toggleFavoriteSalon] → [EcosystemEventReducer]
 * → [CustomerEcosystemState.favoriteSalonIds], and un-favoriting a salon
 * here actually removes it from the visible list, since this screen
 * filters the full salon catalog by that live set rather than showing
 * a fixed list.
 */
@Composable
fun FavoritesScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val favoriteIds = ecosystemViewModel.state.favoriteSalonIds
    val favoriteSalons = catalogEngine.allSalons().filter { it.id in favoriteIds }

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("سالن‌های دنبال‌شده", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            if (favoriteSalons.isEmpty()) {
                item {
                    RojanEmptyState(
                        title = "هنوز سالنی را دنبال نکرده‌اید",
                        description = "سالن‌هایی که دنبال می‌کنید اینجا نمایش داده می‌شوند",
                        icon = Icons.Filled.FavoriteBorder,
                    )
                }
            } else {
                itemsIndexed(favoriteSalons) { index, salon ->
                    FavoriteSalonCard(
                        salon = salon,
                        isFavorite = true,
                        onClick = { onSalonClick(salon.id) },
                        onToggleFavorite = { ecosystemViewModel.toggleFavoriteSalon(salon.id) },
                        animationDelayMillis = index * 60,
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSalonCard(
    salon: DemoSalon,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    animationDelayMillis: Int = 0,
) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(salon.colorSeed.copy(alpha = 0.5f), RojanShapes.Small),
                contentAlignment = Alignment.Center,
            ) {
                if (salon.assetRes != null) {
                    RojanSampleImage(
                        resId = salon.assetRes,
                        contentDescription = salon.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
                }
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = RojanDimens.SpaceSM)) {
                Text(salon.name, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = HomeColors.Gold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text(" ${salon.rating}", style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
            }

            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "لغو دنبال کردن این سالن",
                tint = HomeColors.Magenta,
                modifier = Modifier.clickable(onClick = onToggleFavorite),
            )
        }
    }
}
