package ai.rojan.designlab.screens.dashboard

import ai.rojan.designlab.ui.components.scaffold.RojanScaffold
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Shared minimal layout for the Manager and Stylist dashboards.
 *
 * Production Readiness Fix 1: previously used `GradientBackground` (the
 * original light-pastel component), while Customer Home had already
 * moved to [PremiumBackground] (the Dark Lavender Luxury Canvas) —
 * meaning Manager/Specialist looked like a different, older app.
 * Now unified. Text colors were *also* corrected here, not left as-is:
 * this title/subtitle sits directly on the dark canvas with no glass
 * card underneath it, so per the Background System's two-surface text
 * model, it needs the on-dark tokens ([RojanTextOnGlass]/
 * [RojanTextOnDarkSurface]), not the original light-system
 * [ai.rojan.designlab.ui.theme.RojanTextPrimary]/[ai.rojan.designlab.ui.theme.RojanTextSecondary]
 * it had before — those would have rendered as dark text on a dark
 * background, i.e. barely visible, if the background swap were made
 * without this.
 *
 * Post Build UX Flow & Design System Audit, Issue 2: migrated onto the
 * new shared [RojanScaffold] — this file was hand-implementing exactly
 * the pattern that component now consolidates (PremiumBackground +
 * optional top-start GlassBackButton), just for this one screen family.
 */
@Composable
internal fun DashboardPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    RojanScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = RojanTypography.ScreenTitle,
                color = RojanTextOnGlass,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = RojanTypography.Body,
                color = RojanTextOnDarkSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = RojanDimens.SpaceSM),
            )
        }
    }
}
