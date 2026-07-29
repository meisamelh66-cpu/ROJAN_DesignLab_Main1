package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.components.cards.RojanHomeCard
import ai.rojan.designlab.ui.components.cards.RojanRatingRow
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home "Followed Salons" — UX Refactor Phase 1 rename of what
 * was "FavoriteSalons," per the confirmed decision to reuse the existing
 * Favorite system under Follow terminology rather than build a second,
 * separate concept. Same underlying state as
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s Follow toggle
 * and [ai.rojan.designlab.screens.profile.FavoritesScreen] —
 * [CustomerEcosystemViewModel.state]'s
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemState.favoriteSalonIds]
 * cross-referenced with [CatalogEngine.allSalons].
 *
 * UX Refactor Phase 1: [onSalonClick] now wires this section's cards to
 * real navigation (Salon Profile) — previously `clickable {}` (dead).
 */
@Composable
fun FollowedSalons(ecosystemViewModel: CustomerEcosystemViewModel, onSalonClick: (String) -> Unit = {}) {
    val catalogEngine = remember { CatalogEngine() }
    val favoriteIds = ecosystemViewModel.state.favoriteSalonIds
    val followedSalons = catalogEngine.allSalons().filter { it.id in favoriteIds }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(followedSalons) { index, salon ->
            RojanHomeCard(
                accentColor = salon.colorSeed,
                onClick = { onSalonClick(salon.id) },
                index = index,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(salon.colorSeed.copy(alpha = 0.5f), RojanShapes.Small),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (salon.assetRes != null) {
                            RojanSampleImage(
                                resId = salon.assetRes,
                                contentDescription = salon.name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            RojanIconContainer(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = RojanTextPrimary,
                                size = RojanIconSize.Large,
                            )
                        }
                    }

                    RojanIconContainer(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "دنبال‌شده",
                        tint = RojanVividMagenta,
                        size = RojanIconSize.Small,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(RojanDimens.SpaceXS),
                    )
                }

                Text(
                    text = salon.name,
                    style = RojanTypography.Caption,
                    color = RojanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                RojanRatingRow(rating = salon.rating)

                Text(
                    text = salon.tagline,
                    style = RojanTypography.Caption,
                    color = RojanTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
