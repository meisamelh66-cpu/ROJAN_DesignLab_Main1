package ai.rojan.designlab.screens.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Journey 1, Screen 1: Search.
 *
 * Real (if simple) client-side filtering over [DemoSalonRepository] —
 * not a backend search, consistent with this phase's explicit "no
 * networking" scope. Same dark-canvas + glass language as every other
 * screen; no new visual system introduced.
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val catalogEngine = remember { CatalogEngine() }

    val results = remember(query) { catalogEngine.searchSalons(query) }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onClick = onBackClick)
                Spacer(modifier = Modifier.size(RojanDimens.SpaceMD))
                Text(
                    text = "جستجو",
                    style = RojanTypography.HeroTitle,
                    color = RojanTextOnGlass,
                )
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RojanShapes.Small,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = RojanAIGlow,
                    )
                    Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))

                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = RojanTypography.Body.copy(color = RojanTextOnGlass).withDirectionFor(query),
                        cursorBrush = SolidColor(RojanAIGlow),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "نام سالن یا خدمت را جستجو کنید...",
                                    style = RojanTypography.Body,
                                    color = RojanTextOnDarkSurface,
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text(
                text = "نتایج (${results.size})",
                style = RojanTypography.Body,
                color = RojanTextOnDarkSurface,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

            if (results.isEmpty()) {
                RojanEmptyState(
                    title = "نتیجه‌ای یافت نشد",
                    description = "سالن یا خدمت دیگری را جستجو کنید",
                    icon = Icons.Filled.Search,
                )
            } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                items(results) { salon ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSalonClick(salon.id) },
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
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = RojanTextPrimary,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = salon.name,
                                    style = RojanTypography.Body,
                                    color = RojanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RojanIconContainer(
    imageVector = Icons.Filled.Star,
    contentDescription = null,
    tint = RojanTextSecondary,
    size = RojanIconSize.Small,
)
                                    Text(
                                        text = " ${salon.rating}  •  ",
                                        style = RojanTypography.Caption,
                                        color = RojanTextSecondary,
                                    )
                                    RojanIconContainer(
    imageVector = Icons.Filled.LocationOn,
    contentDescription = null,
    tint = RojanTextSecondary,
    size = RojanIconSize.Small,
)
                                    Text(
                                        text = " ${salon.distanceKm} کیلومتر",
                                        style = RojanTypography.Caption,
                                        color = RojanTextSecondary,
                                    )
                                }
                            }

                            // RTL/LTR Foundation Readiness: this app is RTL-first with
                            // a fixed reading direction (never flips LocalLayoutDirection
                            // — see ai.rojan.designlab.ui.text.Text's own doc comment),
                            // so a "this row leads forward" chevron always reads left,
                            // not conditionally — same reasoning already documented at
                            // ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen's
                            // identical chevron. Icons.Filled.ArrowForward (pointing
                            // right) was backwards against that convention; corrected
                            // to match.
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = RojanTextSecondary,
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
