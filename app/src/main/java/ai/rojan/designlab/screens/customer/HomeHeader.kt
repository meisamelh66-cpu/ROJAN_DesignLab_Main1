package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Home header — Design Board v1.0, Section 1 (Header layer).
 *
 * Premium Header, Phase 1: promoted from a plain wordmark+icons row
 * sitting directly on the canvas to a real Premium Top App Bar — a
 * [GlassSurface] chip spanning the width, matching the reference Home
 * mockup (`docs/design-reference/customer_journey_reference.png.png`,
 * "خانه" dashboard screen): notification bell at the far left, identity
 * centered, profile avatar at the far right. Layout uses [Alignment.CenterStart]/
 * [Alignment.CenterEnd] (not literal Left/Right) — this app never flips
 * Compose's ambient `LayoutDirection` for RTL (see
 * [ai.rojan.designlab.ui.text.Text]'s own doc comment: Persian/RTL is
 * handled per-string, at the text-shaping level, not by mirroring
 * container layout), so Start/End already resolve to left/right exactly
 * as the reference shows.
 *
 * [statusBarsPadding] is applied here, not on [CustomerHomeScreen]'s own
 * background/list, so only this header (this screen's sole consumer)
 * clears the status bar — the background itself still extends behind it
 * unchanged.
 *
 * No per-customer profile photo exists in the current demo data model
 * ([ai.rojan.designlab.data.demo.DemoProfileModels]), so the avatar slot
 * falls back to the same "circular glass chip + Person glyph" pattern
 * used everywhere else in the app an image-less avatar is needed (see
 * [ai.rojan.designlab.ui.components.image.SpecialistAvatar]'s own
 * fallback) rather than inventing a new placeholder graphic.
 *
 * Plays a single fade-in (via [rojanEnterAnimation], the app's existing
 * one-shot content-entrance primitive) the first time this composes —
 * i.e. once per Home open, not a repeating or heavy animation.
 *
 * Home Premium Polish Phase: greeting text unchanged (kept per explicit
 * instruction); everything around it upgraded instead — more vertical
 * breathing room (padding SpaceMD -> SpaceLG), a touch of letter-spacing
 * on the greeting for a less-cramped "luxury" feel, and the notification
 * bell now sits in its own glass chip ([showGlassBackground] on both
 * sides now, previously only the avatar had one) so the header reads as
 * symmetric rather than avatar-heavy.
 *
 * 3D Glassmorphism Depth Pass: a blurred [RojanAIGlow] wash now sits
 * behind the whole bar (background ambient glow, matching [GlassSurface]'s
 * own footprint via `matchParentSize`), the bar's shadow raised to
 * [RojanShadows.PremiumElevation], and both tap targets (bell, avatar)
 * switched from plain `Modifier.clickable` to [rojanPressable] for a
 * real scale-down/return press response.
 *
 * Depth Pass V2: dropped the trailing wave emoji per explicit instruction
 * ("remove fixed emoji if it reduces luxury feeling") — greeting is now
 * plain "سلام {name} جان", matching the approved reference text exactly.
 */
@Composable
fun HomeHeader(
    displayName: String? = null,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(22.dp)
                .background(RojanAIGlow.copy(alpha = 0.12f), RojanShapes.GlassCard),
        )

        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .rojanEnterAnimation(visible = visible),
            shape = RojanShapes.GlassCard,
            elevation = RojanShadows.PremiumElevation,
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceLG),
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "اعلان‌ها",
                tint = RojanTextOnDarkSurface,
                size = RojanIconSize.Medium,
                showGlassBackground = true,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .rojanPressable(onClick = onNotificationsClick),
            )

            val firstName = displayName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "کاربر"
            Text(
                text = "سلام $firstName جان",
                color = RojanTextOnGlass,
                style = RojanTypography.CardTitle.copy(letterSpacing = 0.3.sp),
                modifier = Modifier.align(Alignment.Center),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .rojanPressable(onClick = onProfileClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RojanAmbientGlow(
                        modifier = Modifier.size(56.dp),
                        color = RojanAIGlow,
                        alpha = 0.30f,
                    )
                    RojanIconContainer(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "پروفایل",
                        tint = RojanTextOnDarkSurface,
                        size = RojanIconSize.Medium,
                        showGlassBackground = true,
                    )
                }
                RojanIconContainer(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = RojanTextOnDarkSurface,
                    size = RojanIconSize.Small,
                )
            }
        }
        }
    }
}
