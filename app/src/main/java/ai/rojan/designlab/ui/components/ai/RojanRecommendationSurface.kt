package ai.rojan.designlab.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * The four states a recommendation surface may be in (UI Polish Sprint 4,
 * Task 4). No fifth "here's a guess" state exists — see
 * `docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md`.
 */
enum class RojanRecommendationState {
    /** Real recommendations exist and are passed via `content`. */
    Available,

    /** A real request for recommendations is in flight. */
    Loading,

    /** The request finished and produced nothing to recommend right now. */
    Empty,

    /** No recommendation capability is wired for this surface / account yet. */
    Unavailable,
}

/**
 * ROJAN Smart Recommendation Surface — reusable foundation for future
 * "for you" rails (UI Polish Sprint 4, Task 4).
 *
 * **No recommendation engine, no ranking, no reasons are produced here.**
 * The host screen decides the [state] from real request state and, when
 * [RojanRecommendationState.Available], supplies real recommendation
 * content via [content]. Every other state shows honest, generic copy
 * through [RojanAISurface] — never a stand-in recommendation.
 *
 * Built entirely on [RojanAISurface], so it inherits the same premium AI
 * identity, the calm brand glow (Available / Loading only), palette-driven
 * accent, and reduced-motion behaviour.
 */
@Composable
fun RojanRecommendationSurface(
    state: RojanRecommendationState,
    modifier: Modifier = Modifier,
    title: String = "پیشنهاد رویان",
    loadingLabel: String = "در حال آماده‌سازی پیشنهادها…",
    emptyMessage: String = "در حال حاضر پیشنهادی برای شما نداریم",
    unavailableMessage: String = "این بخش هنوز فعال نشده است",
    onRetry: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalRojanPalette.current

    RojanAISurface(
        modifier = modifier,
        state = when (state) {
            RojanRecommendationState.Available -> RojanAIState.Available
            RojanRecommendationState.Loading -> RojanAIState.Loading
            RojanRecommendationState.Empty -> RojanAIState.Empty
            RojanRecommendationState.Unavailable -> RojanAIState.Disabled
        },
        loadingLabel = loadingLabel,
        emptyTitle = emptyMessage,
        disabledLabel = unavailableMessage,
        onRetry = onRetry,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = RojanTypography.CardTitle, color = palette.textPrimary)
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            content()
        }
    }
}
