package ai.rojan.designlab.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Confidence for an AI insight.
 *
 * **Pass this ONLY when the upstream source genuinely reports a
 * confidence / score.** There is no default, and no way to render a
 * confidence bar without real data — an invented "87%" is exactly the
 * kind of fake intelligence
 * `docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md` forbids.
 *
 * @param fraction 0f..1f, from the source.
 * @param label optional human phrasing the source provides (e.g.
 *   "اطمینان بالا"); the component never derives one.
 */
data class RojanAIConfidence(
    val fraction: Float,
    val label: String? = null,
)

/**
 * ROJAN AI Insight Card — reusable foundation for future salon /
 * customer / booking / beauty intelligence surfaces (UI Polish Sprint 4,
 * Task 2).
 *
 * **No insight is generated here.** The caller passes a real [title] and
 * [description] that came from a real source (today: the Manager
 * dashboard's server-side rule engine). Everything optional
 * ([status], [timestamp], [confidence], [actionLabel]) renders **only when
 * the caller supplies it** — the card degrades to just title + description
 * when that's all that's real.
 *
 * Non-`Available` states (loading / empty / error / disabled) are handled
 * by [RojanAISurface] with honest, generic copy — no stand-in content.
 *
 * RTL: the whole card is a right-aligned column via the app's [Text]
 * system; the accent icon leads on the physical right through the badge/
 * icon-row composition, same pattern as the existing Manager AI card.
 */
@Composable
fun RojanAIInsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    state: RojanAIState = RojanAIState.Available,
    status: String? = null,
    timestamp: String? = null,
    confidence: RojanAIConfidence? = null,
    icon: ImageVector = Icons.Filled.AutoAwesome,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    accentColor: Color = LocalRojanPalette.current.textAccent,
) {
    val palette = LocalRojanPalette.current

    RojanAISurface(
        modifier = modifier,
        state = state,
        accentColor = accentColor,
        onRetry = onRetry,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            RojanIconContainer(
                imageVector = icon,
                contentDescription = null,
                size = RojanIconSize.Large,
                tint = accentColor,
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    Text(
                        text = title,
                        style = RojanTypography.CardTitle,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!status.isNullOrBlank()) {
                        Text(
                            text = status,
                            style = RojanTypography.Caption,
                            color = accentColor,
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.14f), RojanShapes.Circle)
                                .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                        )
                    }
                }

                Text(
                    text = description,
                    style = RojanTypography.Body,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )

                if (!timestamp.isNullOrBlank()) {
                    Text(
                        text = timestamp,
                        style = RojanTypography.Caption,
                        color = palette.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }

                confidence?.let { RojanConfidenceBar(it, accentColor, palette.textSecondary) }

                if (!actionLabel.isNullOrBlank() && onAction != null) {
                    Spacer(Modifier.height(RojanDimens.SpaceSM))
                    Text(
                        text = actionLabel,
                        style = RojanTypography.Button,
                        color = accentColor,
                        modifier = Modifier
                            .align(Alignment.End)
                            .rojanPressable(onClick = onAction)
                            .padding(RojanDimens.SpaceSM),
                    )
                }
            }
        }
    }
}

/**
 * A thin determinate confidence meter. Determinate = it only ever shows a
 * real number; there is no indeterminate / "estimating" mode, because
 * that would be a placeholder for data we don't have.
 */
@Composable
private fun RojanConfidenceBar(
    confidence: RojanAIConfidence,
    accentColor: Color,
    mutedColor: Color,
) {
    val fraction = confidence.fraction.coerceIn(0f, 1f)
    val pct = (fraction * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = RojanDimens.SpaceSM)
            .semantics { stateDescription = confidence.label ?: "اطمینان ${pct} درصد" },
    ) {
        Text(
            text = confidence.label ?: "اطمینان: ${pct}٪",
            style = RojanTypography.Caption,
            color = mutedColor,
            modifier = Modifier.padding(bottom = RojanDimens.SpaceXS),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(mutedColor.copy(alpha = 0.2f), RojanShapes.Circle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(accentColor, RojanShapes.Circle),
            )
        }
    }
}
