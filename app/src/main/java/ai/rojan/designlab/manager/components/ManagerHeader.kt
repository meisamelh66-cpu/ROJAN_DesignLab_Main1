package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.data.toPersianDigits
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * Manager App workspace — dashboard header.
 *
 * Logo-header removal pass: the previous top row (manager logo image +
 * "ROJAN AI" wordmark + "اکوسیستم هوشمند زیبایی" subtitle) is gone
 * entirely — no image, no leftover spacing reserved for it. This
 * composable now renders nothing but the hero banner, so the Dashboard
 * starts directly with it — everything below moves up automatically
 * (the `LazyColumn`'s own item spacing is unaffected; only this item's
 * height shrank). The real salon name lives in [SalonIdentityCard],
 * rendered as the very next item once real data loads — no salon name is
 * duplicated or invented here.
 *
 * Full-bleed pass: the hero uses [fullBleedHorizontal] — a small, local
 * `Modifier.layout {}` that widens the *measurement* constraints given to
 * the banner by exactly [ManagerScaffold]'s own content margin on each
 * side, then places the result shifted back by that same amount. This
 * reaches the screen's edges without touching `ManagerScaffold.kt` and
 * without any negative-padding modifier (the technique used previously,
 * which is what's being avoided this time) — it only ever adds a
 * non-negative offset to the incoming constraints, so `minWidth`/`maxWidth`
 * can never go negative or invalid, and it leaves height untouched
 * entirely (no interaction with `LazyColumn`'s own unbounded-height item
 * measurement).
 */
/**
 * [onNotificationsClick] is currently unused: no notifications screen
 * exists yet, so the bell renders dimmed/inert rather than being wired
 * to a placeholder. Kept as a parameter (not removed) so wiring it up
 * later, once a real destination exists, is a one-line change here.
 */
@Composable
fun ManagerHeader(
    modifier: Modifier = Modifier,
    managerName: String = "مدیر سالن",
    todaysAppointmentCount: Int? = null,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onViewTodayClick: () -> Unit = {},
) {
    ManagerHeroBanner(
        modifier = modifier,
        managerName = managerName,
        todaysAppointmentCount = todaysAppointmentCount,
        onProfileClick = onProfileClick,
        onNotificationsClick = onNotificationsClick,
        onViewTodayClick = onViewTodayClick,
    )
}

/**
 * Widens this element's own measured width by `2 * amount` (never
 * negative — [amount] is coerced to at least zero) and shifts its
 * placement back by [amount] on the leading edge, so it visually bleeds
 * past a parent's positive padding by exactly that much on both sides.
 * Height is passed through completely untouched. Uses raw [Placeable.place]
 * (not `placeRelative`) so the bleed is symmetric regardless of layout
 * direction — this is a geometric expansion, not an RTL start/end concept.
 */
private fun Modifier.fullBleedHorizontal(amount: Dp): Modifier = this.layout { measurable, constraints ->
    val extraPx = amount.roundToPx().coerceAtLeast(0)
    if (extraPx == 0) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    val widened = constraints.copy(
        minWidth = (constraints.minWidth + 2 * extraPx).coerceAtLeast(0),
        maxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth + 2 * extraPx).coerceAtLeast(0)
        } else {
            constraints.maxWidth
        },
    )
    val placeable = measurable.measure(widened)
    // Report the ORIGINAL (unwidened) footprint to the parent, so the
    // surrounding Column/LazyColumn item sizing is unaffected — only the
    // drawn/placed content extends beyond it.
    layout(placeable.width - 2 * extraPx, placeable.height) {
        placeable.place(-extraPx, 0)
    }
}

/** Thin outlined circle, no fill — matches the reference's plain bell/profile treatment. A fixed-size Box with ordinary positive padding, no layout risk. */
@Composable
private fun QuietIconBadge(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .border(1.dp, ManagerColors.TextSecondary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RojanIconContainer(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            size = RojanIconSize.Medium,
        )
    }
}

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "صبح بخیر"
        in 11..16 -> "ظهر بخیر"
        in 17..20 -> "عصر بخیر"
        else -> "شب بخیر"
    }
}

/** Wide, low, clearly rectangular banner — a smaller, explicit radius local to this composable (not the shared 32dp `RojanShapes.GlassCard`, tuned for taller cards). */
private val HeroBannerShape = RoundedCornerShape(20.dp)

@Composable
private fun ManagerHeroBanner(
    modifier: Modifier = Modifier,
    managerName: String,
    todaysAppointmentCount: Int?,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onViewTodayClick: () -> Unit,
) {
    ManagerGlassSurface(
        modifier = modifier
            // Edge-to-edge: cancels exactly ManagerScaffold's own default
            // content margin (RojanDimens.SpaceMD on each side) — reading
            // that shared constant, not editing ManagerScaffold.kt itself.
            .fullBleedHorizontal(RojanDimens.SpaceMD)
            // Turquoise ambient glow — a plain, standard `shadow()` call.
            // This only affects drawing, never measurement/layout.
            .shadow(
                elevation = 12.dp,
                shape = HeroBannerShape,
                ambientColor = ManagerColors.Turquoise.copy(alpha = 0.30f),
                spotColor = ManagerColors.Turquoise.copy(alpha = 0.20f),
            ),
        shape = HeroBannerShape,
        // `compact` drops the sparkle/sheen passes; the lower border alpha
        // keeps the remaining metallic stroke a quiet hairline.
        compact = true,
        borderAlpha = 0.32f,
        borderSecondaryAlpha = 0.28f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        size = RojanIconSize.Medium,
                        tint = ManagerColors.Gold,
                    )
                    Text(
                        text = "${greetingForNow()}، $managerName",
                        style = RojanTypography.CardTitle,
                        // Pixel-perfect pass: reference.png shows the
                        // greeting in white — gold is reserved for the
                        // CTA only, never headline text.
                        color = ManagerColors.TextPrimary,
                    )
                }

                // Profile access stays inside the hero, alongside
                // notifications — same [onProfileClick] handler as before.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuietIconBadge(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "اعلان‌ها",
                        tint = ManagerColors.TextSecondary,
                        modifier = Modifier.alpha(0.6f),
                    )
                    QuietIconBadge(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "پروفایل مدیر",
                        tint = ManagerColors.TextPrimary,
                        modifier = Modifier.rojanPressable(onClick = onProfileClick),
                    )
                }
            }

            Text(
                text = if (todaysAppointmentCount != null) {
                    "امروز ${todaysAppointmentCount.toPersianDigits()} نوبت برای سالن شما ثبت شده است."
                } else {
                    "با هم امروز روزی موفق و پربار بسازیم."
                },
                style = RojanTypography.Body,
                color = ManagerColors.TextSecondary,
            )

            // Real gold CTA pill — same [onViewTodayClick] handler as
            // before, just a proper button affordance.
            Row(
                modifier = Modifier
                    .padding(top = RojanDimens.SpaceXS)
                    .background(ManagerColors.Gold, RojanShapes.PremiumButton)
                    .rojanPressable(onClick = onViewTodayClick)
                    .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
            ) {
                Text(
                    text = "مشاهده برنامه امروز",
                    style = RojanTypography.Button,
                    color = ManagerColors.BaseDeep,
                )
                RojanIconContainer(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    size = RojanIconSize.Small,
                    tint = ManagerColors.BaseDeep,
                )
            }
        }
    }
}

