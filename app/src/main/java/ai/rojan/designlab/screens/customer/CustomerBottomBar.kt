package ai.rojan.designlab.screens.customer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPremiumBorderGold
import ai.rojan.designlab.ui.theme.RojanPremiumBorderHighlight
import ai.rojan.designlab.ui.theme.RojanPremiumBorderRoseGold
import ai.rojan.designlab.ui.theme.RojanPremiumBorderShadow
import ai.rojan.designlab.ui.theme.RojanPremiumBorderSpecular
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes

/** Fake, local-only tab identifiers — no navigation graph change, purely this bar's own active-state tracking. */
enum class CustomerHomeTab { HOME, SEARCH, BOOKINGS, FAVORITES, PROFILE }

private data class TabItem(
    val tab: CustomerHomeTab,
    val icon: ImageVector,
    val label: String,
)

private val tabs = listOf(
    TabItem(CustomerHomeTab.PROFILE, Icons.Filled.Person, "پروفایل"),
    TabItem(CustomerHomeTab.FAVORITES, Icons.Filled.Favorite, "علاقه‌ها"),
    TabItem(CustomerHomeTab.HOME, Icons.Filled.Home, "خانه"),
    TabItem(CustomerHomeTab.BOOKINGS, Icons.Filled.CalendarMonth, "نوبت‌ها"),
    TabItem(CustomerHomeTab.SEARCH, Icons.Filled.Search, "جستجو"),
)

/**
 * Customer Home bottom navigation — Home Visual Language Unification.
 *
 * Same 5 tabs, same active-state tracking, same "no real navigation
 * wiring here" scope as before — dark glass bar per the approved
 * reference, with the Home tab raised into a circular glowing button
 * (a size/shape treatment on an existing item, not a new tab or a new
 * interaction) when it's the active tab, matching the reference's
 * center Home button.
 */
@Composable
fun CustomerBottomBar(
    modifier: Modifier = Modifier,
    activeTab: CustomerHomeTab = CustomerHomeTab.HOME,
    onTabSelected: (CustomerHomeTab) -> Unit = {},
) {
    // Home FAB floats outside the glass pill (top overlap + bottom overlap),
    // per the reference. It's rendered as a SIBLING of HomeGlassSurface
    // below, not nested inside its content — [Modifier.shadow] (used twice
    // inside [ai.rojan.designlab.ui.components.glass.PremiumGlassSurface])
    // defaults `clip = true` whenever `elevation > 0.dp`, which it always is
    // here, so anything drawn *inside* the glass surface gets clipped flush
    // to its own (now much shorter) rounded-rect bounds — verified on
    // device: nesting the button there cut its top/bottom into a flat edge
    // instead of a floating circle. Rendering it as a sibling sidesteps
    // that entirely — only the clipping/layering changes here. The
    // reference box below (padding top = SpaceXS, height = 20dp, offset
    // -4dp) reproduces the exact same position the button had when it was
    // still nested in the Row (a 20dp-tall column, top-aligned under the
    // Row's own SpaceXS padding, biased up 4dp) — same offset, same size,
    // same position, just unclipped.
    val homeButtonOuterSize = 88.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        HomeGlassSurface(
            // Narrower outer container only (~78% of screen width) — icon
            // placement relative to the container is untouched. Safe at
            // this width now that each item below carries
            // Modifier.weight(1f) (equal, bounded slots instead of
            // intrinsic-content sizing) and no longer renders a text label
            // — the earlier breakage at 81-88% was caused by unconstrained
            // label text competing for space under Arrangement.SpaceEvenly,
            // not by the container width itself; with that root cause gone,
            // icon-only content scales cleanly to a narrower pill.
            modifier = Modifier.fillMaxWidth(0.78f),
            shape = RojanShapes.GlassCard,
            // Bottom Navigation refinement: ~14% thinner than the shared
            // PremiumGlassTheme.BorderStrokeWidth default every card still
            // uses — scoped to this one container only, same metallic
            // gradient/reflections/glow/sparkles otherwise unchanged.
            borderStrokeWidth = 1.55.dp,
            // Minimal reduction to this instance's outer shadow/glow bleed
            // only (every other glass surface keeps the 16dp/FloatingElevation
            // defaults). Glass fill, border rendering, sparkles, and layout
            // are untouched — only how far the two elevation-based shadows
            // spread past the container's true edge.
            glowSpread = 6.dp,
            elevation = RojanShadows.SoftElevation,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Geometry refinement (matching the reference's slim
                    // 48dp bar): tightened from 6dp/SpaceMD to hug the icon
                    // row — less empty glass above/below and to either side
                    // of the icon group, not a size/spacing change to the
                    // icons themselves.
                    .padding(vertical = RojanDimens.SpaceXS, horizontal = RojanDimens.SpaceSM),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                tabs.forEach { item ->
                    val isActive = item.tab == activeTab

                    if (item.tab == CustomerHomeTab.HOME) {
                        // Reserves this slot's width (so the other 4 icons'
                        // spacing/positions are exactly as if Home rendered
                        // here) with no visual content and no click handler
                        // — the real, visible Home button is the floating
                        // sibling below, drawn on top of this reserved gap.
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val tint = if (isActive) HomeColors.Glow else HomeColors.TextSecondary
                        val interactionSource = remember { MutableInteractionSource() }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                    onClick = { onTabSelected(item.tab) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            RojanIconContainer(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                size = RojanIconSize.Medium,
                            )
                        }
                    }
                }
            }
        }

        val homeInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Exact same reference geometry the nested Column used to
                // have: Row's own SpaceXS top padding, a 20dp-tall slot
                // (RojanIconSize.Medium), then a 4dp upward bias — FAB
                // offset/size/position unchanged, only unclipped now.
                .padding(top = RojanDimens.SpaceXS)
                .height(RojanIconSize.Medium.dp)
                .offset(y = (-4).dp)
                .clickable(
                    interactionSource = homeInteractionSource,
                    indication = LocalIndication.current,
                    onClick = { onTabSelected(CustomerHomeTab.HOME) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // requiredSize (not size): the enclosing Box above is
            // intentionally constrained to a 20dp height so it doesn't
            // inflate the Row's measured height — but plain `.size()`
            // still gets clamped to fit whatever incoming constraint an
            // ancestor imposes, which squished these circles into a
            // stadium/pill shape (height clamped to 20dp, width untouched).
            // `.requiredSize()` ignores incoming constraints entirely, so
            // these stay true circles regardless of the slot they overflow.
            RojanAmbientGlow(
                modifier = Modifier.requiredSize(homeButtonOuterSize),
                color = HomeColors.Glow,
                alpha = 0.55f,
            )
            HomeMetallicRing(modifier = Modifier.requiredSize(72.dp))
            Box(
                modifier = Modifier
                    // Reference spec: center button = 64dp.
                    .requiredSize(64.dp)
                    .background(HomeColors.Glow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                RojanIconContainer(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "خانه",
                    tint = HomeColors.TextPrimary,
                    // Home is the bar's primary focal point — larger than
                    // the other four tabs' RojanIconSize.Medium (20dp).
                    // Icon glyph itself unchanged.
                    sizeOverride = 24.dp,
                )
            }
        }
    }
}

/**
 * Thin metallic gold ring around the Home button — same color tokens as
 * the app's shared [ai.rojan.designlab.ui.components.glass.premiumMetallicBorder]
 * (gold/rose-gold/highlight/shadow/specular), arranged as a sweep gradient
 * so continuous rotation reads as light chasing around the ring rather
 * than a static circle (a plain radial/symmetric glow wouldn't show any
 * visible motion when rotated). Rotation only — no pulsing, scaling, or
 * color change.
 */
@Composable
private fun HomeMetallicRing(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "homeRingRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
        ),
        label = "homeRingAngle",
    )

    Box(
        modifier = modifier
            .rotate(angle)
            .border(
                width = 1.55.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        RojanPremiumBorderShadow,
                        RojanPremiumBorderGold,
                        RojanPremiumBorderHighlight,
                        RojanPremiumBorderRoseGold,
                        RojanPremiumBorderSpecular,
                        RojanPremiumBorderRoseGold,
                        RojanPremiumBorderHighlight,
                        RojanPremiumBorderGold,
                        RojanPremiumBorderShadow,
                    ),
                ),
                shape = CircleShape,
            ),
    )
}
