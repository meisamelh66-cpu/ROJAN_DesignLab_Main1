package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTypography

@Composable
fun ServiceCategories() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        items(catalogEngine.serviceCategories()) { category ->

            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 84.dp)
                    .background(
                        category.tint.copy(alpha = 0.35f),
                        RojanShapes.Small
                    )
            ) {

                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = { },
                        ),
                    shape = RojanShapes.Small,
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {

                        // Circle with visible white ring
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color.White.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        category.tint.copy(alpha = 0.55f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                if (category.iconRes != null) {
                                    Image(
                                        painter = painterResource(category.iconRes),
                                        contentDescription = category.label,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else if (category.fallbackIcon != null) {
                                    RojanIconContainer(
                                        imageVector = category.fallbackIcon,
                                        contentDescription = category.label,
                                        tint = RojanTextPrimary,
                                        sizeOverride = 22.dp
                                    )
                                }
                            }
                        }

                        Text(
                            text = category.label,
                            style = RojanTypography.Caption.rojanPressedShadow(interactionSource),
                            color = RojanTextPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}