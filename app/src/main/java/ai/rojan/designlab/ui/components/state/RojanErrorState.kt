package ai.rojan.designlab.ui.components.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.theme.RojanErrorText

/**
 * ROJAN error-state primitive — Final Premium Polish, Phase 1. Sibling to
 * [RojanEmptyState], sharing its internal [RojanStateCard] layout (same
 * glass card shape/spacing/typography) so empty and error states read as
 * one consistent design language, differing only in icon/tint/default
 * copy. Closes a real, confirmed gap: no error-state component existed
 * anywhere before this — e.g. [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s
 * "salon not found" case rendered as bare centered [ai.rojan.designlab.ui.text.Text],
 * no icon, no card, no retry affordance.
 */
@Composable
fun RojanErrorState(
    title: String = "مشکلی پیش آمد",
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    RojanStateCard(
        title = title,
        modifier = modifier,
        description = description,
        icon = Icons.Filled.ErrorOutline,
        iconTint = RojanErrorText,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}
