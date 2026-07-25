package ai.rojan.designlab.components.hero

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

import ai.rojan.designlab.components.IllustrationPlaceholder
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography


/**
 * ROJAN AI Hero Booking Card — Component Migration Phase v1.0, Component 1.
 *
 * Glass UI Refinement Phase v1 (Option A decision): this is now the one
 * component that follows Master Background → Glass Surface → Content
 * directly, with no intermediate colored fill — [GlassSurface]'s own
 * translucent film sits right on top of whatever the app background
 * (`bg_master_luxury_salon.webp`) shows through it. The previous
 * [RojanGradients.Background] fill (added to fix a real contrast bug
 * against the *old light* app background) was removed per an explicit
 * decision that this card, specifically, should show the master
 * background through neutral glass — smaller pastel-tinted cards
 * elsewhere in the app (FeaturedSalons, TopSpecialists, etc.) are
 * unaffected and keep their approved tint-under-glass pattern.
 *
 * Disclosed risk, not silently assumed safe: a simulated composite of
 * this exact change against the real master background asset showed the
 * background photo has real local contrast variance (bright highlights
 * near dark shadow areas within a small region) that GlassSurface's
 * light 10–32% white veil doesn't fully neutralize — unlike the flat
 * colored fill it replaces, text legibility here is no longer
 * position-independent. Not compensated for here since that wasn't part
 * of the decision made; flagged for a possible follow-up (e.g. a
 * text-only scrim) if real-device testing confirms it's a problem.
 *
 * Design System Foundation Refactor addition (this pass): the external
 * `.shadow()` this file used to apply itself has been removed —
 * [GlassSurface] now owns elevation directly (see its own doc comment),
 * so this card just requests the Premium tier explicitly via
 * `elevation = RojanShadows.PremiumElevation` rather than maintaining a
 * second, separate shadow mechanism alongside the shared one.
 *
 * Home Screen Production Pass, Task 5: the title/subtitle block now sits
 * beside [IllustrationPlaceholder] (the approved `hero_calendar` 3D
 * illustration — see that component's own doc comment) in a `Row`
 * instead of alone in a fully centered `Column`, balancing the card's
 * left/right visual weight. [PremiumButton] deliberately stays outside
 * that `Row`, full-width-centered exactly as before: it has a fixed
 * [RojanDimens.ButtonWidth] (240dp), which does not fit beside the
 * illustration in the narrower half of the card on real phone widths —
 * keeping it in its own row below preserves the exact fit this card's
 * [RojanDimens.HeroHeight] budget was already tuned for (see
 * [PremiumButton]'s own doc comment) with zero risk of overflow.
 */
@Composable
fun HeroBookingCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RojanDimens.HeroHeight)
            .clip(RojanShapes.GlassCard)
    ) {

        GlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = RojanShapes.GlassCard,
            elevation = RojanShadows.PremiumElevation
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceLG),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center

            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = "رزرو نوبت زیبایی",
                            style = RojanTypography.HeroTitle,
                            color = RojanTextOnGlass,
                            textAlign = TextAlign.Start
                        )

                        Spacer(
                            modifier = Modifier.height(RojanDimens.SpaceSM)
                        )

                        Text(
                            text = "بهترین متخصصان زیبایی در کنار شما\nنوبت خود را سریع و آسان رزرو کنید",
                            style = RojanTypography.Body,
                            color = RojanTextOnDarkSurface,
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(RojanDimens.SpaceMD)
                    )

                    IllustrationPlaceholder(
                        size = 110.dp,
                    )
                }

                Spacer(
                    modifier = Modifier.height(RojanDimens.SpaceLG)
                )

                PremiumButton(
                    text = "دریافت نوبت",
                    onClick = onClick
                )
            }
        }
    }
}
