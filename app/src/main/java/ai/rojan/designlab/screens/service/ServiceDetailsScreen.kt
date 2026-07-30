package ai.rojan.designlab.screens.service

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/** Journey 1, Screen 4: Service Details. */
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val service = catalogEngine.findServiceById(serviceId)

    HomeBackgroundTheme {
        if (service == null) {
            Box(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD), contentAlignment = Alignment.Center) {
                RojanErrorState(title = "خدمت یافت نشد")
            }
            return@HomeBackgroundTheme
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(RojanDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            ) {
                item { GlassBackButton(onClick = onBackClick) }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(service.colorSeed.copy(alpha = 0.5f), RojanShapes.GlassCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        RojanIconContainer(
    imageVector = Icons.Filled.ContentCut,
    contentDescription = null,
    tint = HomeColors.TextPrimary,
    sizeOverride = 48.dp,
)
                    }
                }

                item {
                    Column {
                        Text(service.name, style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, null, tint = HomeColors.TextSecondary, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                            Text(" ${service.durationMinutes} دقیقه", style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (service.discountPrice != null) {
                            Text(
                                text = "${service.price} تومان",
                                style = RojanTypography.Caption,
                                color = HomeColors.TextSecondary,
                                textDecoration = TextDecoration.LineThrough,
                            )
                            Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
                            Text(
                                text = "${service.discountPrice} تومان",
                                style = RojanTypography.HeroTitle,
                                color = HomeColors.Glow,
                            )
                        } else {
                            Text(
                                text = "${service.price} تومان",
                                style = RojanTypography.HeroTitle,
                                color = HomeColors.Glow,
                            )
                        }
                    }
                }

                item {
                    HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                        Text(
                            text = service.description,
                            style = RojanTypography.Body,
                            color = HomeColors.TextPrimary,
                            modifier = Modifier.padding(RojanDimens.SpaceMD),
                        )
                    }
                }

                item { Text("مزایا", style = RojanTypography.Body, color = HomeColors.TextPrimary) }
                itemsIndexed(service.benefits) { index, benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.rojanEnterAnimation(delayMillis = index * 60),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = HomeColors.Glow, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                        Text(" $benefit", style = RojanTypography.Caption, color = HomeColors.TextPrimary)
                    }
                }

                item {
                    Column {
                        Text("آماده‌سازی قبل از مراجعه", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                        Text(service.preparation, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                    }
                }

                item {
                    Column {
                        Text("مراقبت پس از خدمت", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                        Text(service.aftercare, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                    }
                }
            }

            Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                PremiumButton(
                    text = "رزرو این خدمت",
                    onClick = { onBookClick(service.id) },
                )
            }
        }
    }
}
