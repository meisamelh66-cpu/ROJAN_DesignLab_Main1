package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Customer Home top specialists — Design Board v1.0, Secondary Features
 * layer.
 *
 * Demo Content Population Pass: migrated off its previous local
 * `fakeSpecialists` list (which duplicated
 * [ai.rojan.designlab.data.demo.DemoSpecialistRepository]'s data under
 * the same 4 names) onto [CatalogEngine.allSpecialists] — the same
 * canonical source Journey 1 already uses. `DemoSpecialist.assetRes`
 * (added earlier but never populated) is now wired to the real
 * `avatar_01..04` placeholder assets.
 */
@Composable
fun TopSpecialists() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.allSpecialists()) { specialist ->
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 190.dp)
                    .background(specialist.colorSeed.copy(alpha = 0.30f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (specialist.assetRes != null) {
                                Image(
                                    painter = painterResource(id = specialist.assetRes),
                                    contentDescription = specialist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                )
                            } else {
                                RojanIconContainer(
    imageVector = Icons.Filled.Person,
    contentDescription = null,
    tint = RojanTextPrimary,
    sizeOverride = 32.dp,
)
                            }
                        }

                        Text(
                            text = specialist.name,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = specialist.title,
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.Star,
    contentDescription = "امتیاز",
    tint = RojanTextSecondary,
    sizeOverride = 14.dp,
)
                            Text(
                                text = specialist.rating,
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
