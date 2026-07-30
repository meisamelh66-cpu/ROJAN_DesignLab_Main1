package ai.rojan.designlab.components.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.ui.text.Text

import ai.rojan.designlab.R
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
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
 *
 * Home Premium Polish Phase: reverses this file's earlier documented
 * "neutral glass, no colored fill" decision, per an explicit new request
 * to give this card the same soft pastel-tinted-glass language every
 * other Home card already uses. A subtle [RojanBlushPink] → [RojanSoftLavender]
 * gradient now sits behind [GlassSurface] (same tint-under-glass pattern
 * as [ai.rojan.designlab.screens.customer.FeaturedSalons]/[ai.rojan.designlab.screens.customer.TopSpecialists]),
 * at low alpha so it reads as a soft wash, not a flat colored card —
 * [RojanTextOnGlass]/[RojanTextOnDarkSurface] stay legible against it.
 *
 * 3D Glassmorphism Depth Pass: that tint gradient now has a blurred twin
 * sitting directly behind it (background ambient glow, same technique
 * [ai.rojan.designlab.ui.components.cards.RojanHomeCard] now uses) —
 * the card's own outer `.clip` still bounds it, so this stays a diffuse
 * glow *within* the existing footprint, not an escaping halo; no layout
 * or size change.
 *
 * Depth Pass V2: title now carries a soft [RojanShadows.PremiumShadow]
 * text shadow (reduces "flat text feeling" against the glass) and
 * [PremiumButton] gained its own quiet [RojanAmbientGlow] behind it —
 * same restrained pattern already used behind the illustration, not a
 * new effect — so the CTA reads as an invitation sitting in a pool of
 * light rather than a plain button dropped onto the card.
 *
 * Luxury Visual Refinement Phase: `hero_calendar.webp` (the "approved 3D
 * illustration" prior passes documented) turned out, on real-device
 * inspection, to have a checkerboard transparency pattern baked directly
 * into its own pixels — a defective export, not something fixable from
 * this file. No replacement hero-tier illustration exists on disk (the
 * `hero_booking`/`hero_beauty`/`hero_ai`/`hero_products` names
 * [ai.rojan.designlab.ui.assets.RojanAssetNames] documents were never
 * actually shipped as files), so real photography
 * (`salon_demo_1.png`, already an approved asset used elsewhere) takes
 * its place — same [RojanAmbientGlow] behind it, now framed with a thin
 * white border so the photo reads as inset into the glass rather than a
 * separate sticker on top of it.
 */
@Composable
fun HeroBookingCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Final Premium Polish, Phase 2: this is the first real content the
    // customer sees below the header/search bar — a one-shot fade +
    // slight-upward-motion entrance on the whole card (not staggered,
    // there's only one of it) rather than appearing instantly.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RojanDimens.HeroHeight)
            .clip(RojanShapes.GlassCard)
            .rojanEnterAnimation(visible = visible)
    ) {

        // Layer 1: background ambient glow — a blurred twin of the tint below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HomeColors.Glow.copy(alpha = 0.5f),
                            HomeColors.Magenta.copy(alpha = 0.4f),
                        ),
                    ),
                    shape = RojanShapes.GlassCard,
                ),
        )

        // Layer 2: crisp glass tint (unblurred, defines the card's edges)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HomeColors.Glow.copy(alpha = 0.30f),
                            HomeColors.Primary.copy(alpha = 0.30f),
                        ),
                    ),
                    shape = RojanShapes.GlassCard,
                ),
        )

        // Layer 3: glass surface + floating content
        HomeGlassSurface(
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
                            style = RojanTypography.HeroTitle.copy(
                                shadow = Shadow(
                                    color = RojanShadows.PremiumShadow.copy(alpha = 0.35f),
                                    offset = Offset(0f, 3f),
                                    blurRadius = 10f,
                                ),
                            ),
                            color = HomeColors.TextPrimary,
                            textAlign = TextAlign.Start
                        )

                        Spacer(
                            modifier = Modifier.height(RojanDimens.SpaceSM)
                        )

                        Text(
                            text = "بهترین متخصصان زیبایی در کنار شما\nنوبت خود را سریع و آسان رزرو کنید",
                            style = RojanTypography.Body,
                            color = HomeColors.TextSecondary,
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(RojanDimens.SpaceMD)
                    )

                    // Premium Effects: a single quiet AI-glow accent behind the
                    // one illustration on this card — "restraint," not a glow
                    // on every element. RojanAmbientGlow sits *behind* the
                    // illustration (Box stacks bottom-to-top), sized larger so
                    // it reads as ambience rather than a hard-edged halo.
                    Box(contentAlignment = Alignment.Center) {
                        RojanAmbientGlow(
                            modifier = Modifier.size(150.dp),
                            color = HomeColors.Glow,
                            alpha = 0.35f,
                        )
                        RojanSampleImage(
                            resId = R.drawable.salon_demo_1,
                            contentDescription = "فضای سالن زیبایی",
                            shape = RojanShapes.Small,
                            modifier = Modifier
                                .size(width = 104.dp, height = 132.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    shape = RojanShapes.Small,
                                ),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(RojanDimens.SpaceLG)
                )

                Box(contentAlignment = Alignment.Center) {
                    RojanAmbientGlow(
                        modifier = Modifier.size(width = 280.dp, height = 96.dp),
                        color = HomeColors.Glow,
                        alpha = 0.28f,
                    )
                    PremiumButton(
                        text = "دریافت نوبت",
                        onClick = onClick
                    )
                }
            }
        }
    }
}
