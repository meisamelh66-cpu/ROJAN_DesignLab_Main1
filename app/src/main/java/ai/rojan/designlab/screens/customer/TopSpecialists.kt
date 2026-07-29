package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.cards.RojanHomeCard
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanWarmWhite

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
 *
 * Home Screen Production Pass, Task 7/8: card size moved from a raw
 * `160.dp`/`190.dp` pair onto [RojanDimens.CardWidthStandard]/
 * [RojanDimens.CardHeightStandard] — zero visual change, see
 * [FeaturedSalons]'s identical note.
 *
 * Home Premium Polish Phase: redesigned per the approved reference —
 * larger avatar (64dp -> 84dp) with a soft [RojanAmbientGlow] ring behind
 * it (the same ambient-glow primitive [HeroBookingCard] already uses, not
 * a new effect), a small gold star badge overlaid on its corner, and the
 * rating moved off the shared [ai.rojan.designlab.ui.components.cards.RojanRatingRow]
 * into a dedicated gold-tinted chip local to this file — [RojanRatingRow]
 * stays untouched so [FeaturedSalons]/[NearbySalons]/etc. (its other 4
 * callers) don't inherit a look only this section asked for. Card height
 * bumped (190dp -> 210dp) to fit the larger avatar without cramming.
 *
 * 3D Glassmorphism Depth Pass: avatar shadow raised a tier
 * ([RojanShadows.SoftElevation] -> [RojanShadows.FloatingElevation]) and
 * the glow ring strengthened (0.45f -> 0.55f alpha, 104dp -> 112dp) so the
 * avatar visibly lifts off the card underneath it rather than sitting
 * flush — no position change, purely shadow/glow intensity.
 *
 * Depth Pass V2: avatar enlarged again (84dp -> 92dp, glow ring 112dp ->
 * 124dp) and its shadow raised to [RojanShadows.PremiumElevation] (the
 * Hero-tier shadow, previously only used by full-width cards) for a
 * "luxury beauty expert profile" read. Card height bumped (210dp ->
 * 218dp) to keep fitting the larger avatar without cramming. The rating
 * chip gained a thin gold border and its own small shadow so it reads as
 * a real chip rather than a flat tinted rectangle.
 */
@Composable
fun TopSpecialists() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(catalogEngine.allSpecialists()) { index, specialist ->
            RojanHomeCard(
                accentColor = specialist.colorSeed,
                onClick = { },
                horizontalAlignment = Alignment.CenterHorizontally,
                index = index,
                height = 218.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RojanAmbientGlow(
                        modifier = Modifier.size(124.dp),
                        color = specialist.colorSeed,
                        alpha = 0.55f,
                    )

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .shadow(
                                elevation = RojanShadows.PremiumElevation,
                                shape = CircleShape,
                                ambientColor = specialist.colorSeed.copy(alpha = 0.4f),
                                spotColor = specialist.colorSeed.copy(alpha = 0.4f),
                            )
                            .background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        SpecialistAvatar(
                            assetRes = specialist.assetRes,
                            contentDescription = specialist.name,
                            fallbackIconSize = RojanDimens.IconSizeLarge,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .shadow(elevation = RojanShadows.SoftElevation / 3, shape = CircleShape)
                            .background(RojanWarmWhite, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        RojanIconContainer(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "متخصص برتر",
                            tint = RojanRatingGold,
                            size = RojanIconSize.Small,
                        )
                    }
                }

                Text(
                    text = specialist.name,
                    style = RojanTypography.Caption.copy(fontWeight = FontWeight.Bold),
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
                    modifier = Modifier
                        .shadow(
                            elevation = RojanShadows.SoftElevation / 2,
                            shape = CircleShape,
                            ambientColor = RojanRatingGold.copy(alpha = 0.3f),
                            spotColor = RojanRatingGold.copy(alpha = 0.3f),
                        )
                        .background(RojanRatingGold.copy(alpha = 0.18f), CircleShape)
                        .border(width = 0.6.dp, color = RojanRatingGold.copy(alpha = 0.4f), shape = CircleShape)
                        .padding(horizontal = RojanDimens.SpaceSM, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = RojanRatingGold,
                        size = RojanIconSize.Small,
                    )
                    Text(
                        text = specialist.rating,
                        style = RojanTypography.Caption,
                        color = RojanTextPrimary,
                    )
                }
            }
        }
    }
}
