package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, spec sections 6 & 7 — reached only after
 * a successful Auth. "Categories exist ONLY after pressing Book
 * Appointment. Display square category cards." Reuses
 * [ai.rojan.designlab.data.demo.DemoServiceCategoryRepository] (the
 * same 10-category source already used by
 * [ai.rojan.designlab.screens.customer.ServiceCategories] on Home,
 * before that section was removed from Home per section 2 — not a new,
 * separate dataset).
 */
@Composable
fun ServiceCategoriesScreen(
    onBackClick: () -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "دسته‌بندی خدمات",
                style = RojanTypography.HeroTitle,
                color = RojanTextOnGlass,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(catalogEngine.serviceCategories()) { category ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(category.tint.copy(alpha = 0.30f), RojanShapes.Small)
                    ) {
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onCategorySelected(category.label) },
                            shape = RojanShapes.Small,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (category.iconRes != null) {
                                    Image(
                                        painter = painterResource(id = category.iconRes),
                                        contentDescription = category.label,
                                        modifier = Modifier.size(32.dp),
                                    )
                                } else if (category.fallbackIcon != null) {
                                    Icon(
                                        imageVector = category.fallbackIcon,
                                        contentDescription = category.label,
                                        tint = RojanTextPrimary,
                                    )
                                }

                                Text(
                                    text = category.label,
                                    style = RojanTypography.Caption,
                                    color = RojanTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
