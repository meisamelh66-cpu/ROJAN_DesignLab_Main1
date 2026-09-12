package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — the app shell.
 *
 * The single scaffold every Customer screen sits in, so the whole app shares
 * one structure and the approved "quiet luxury" chrome (Golden Reference:
 * docs/design-review/customer/REFERENCE-SPEC-salon-detail.md).
 *
 * Replaces, for the screen that adopts it, the floating `GlassBackButton` orb
 * + bare `HeroTitle` + ad-hoc `Column`/`LazyColumn` scaffolding with:
 *   • a flat 56dp top bar — outlined back arrow on the content margin,
 *     RTL-centred title (Body weight), 1px bottom hairline; no orb, no glass;
 *   • an optional quiet rose-gold [CustomerStepIndicator] under the bar (for
 *     the multi-step booking journey; omit it everywhere else);
 *   • a full-width content slot (the screen supplies its own list + margins);
 *   • an optional pinned bottom-bar slot for the primary CTA.
 *
 * It owns its own window insets (top bar → statusBarsPadding, bottom slot →
 * navigationBarsPadding), so it applies [HomeBackgroundTheme] with
 * `applyContentInsets = false`. Screens should NOT wrap themselves in
 * [HomeBackgroundTheme] again.
 *
 * Adds NO design-system token and edits NO shared component. Touches no
 * ViewModel, repository, navigation route, or API.
 * ========================================================================== */

@Composable
fun CustomerScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    step: Int? = null,
    totalSteps: Int = 5,
    showBackButton: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    HomeBackgroundTheme(
        modifier = modifier.fillMaxSize(),
        applyContentInsets = false,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CustomerTopBar(title = title, onBackClick = onBackClick, showBackButton = showBackButton)

            if (step != null) {
                CustomerStepIndicator(step = step, totalSteps = totalSteps)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = content,
            )

            bottomBar?.let { bar ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = CustomerScreenMargin,
                            vertical = RojanDimens.SpaceMD,
                        ),
                ) {
                    bar()
                }
            }
        }
    }
}

// --- Flat top bar ---------------------------------------------------------

@Composable
private fun CustomerTopBar(title: String, onBackClick: () -> Unit, showBackButton: Boolean = true) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CustomerTopBarHeight)
                .padding(horizontal = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(RojanDimens.MinTouchTarget)
                        .rojanPressable(onClick = onBackClick, role = Role.Button),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = HomeColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(RojanDimens.MinTouchTarget))
            }

            Text(
                title,
                style = RojanTypography.Body,
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RojanDimens.SpaceXS),
            )

            // Balances the leading 48dp back target so the title is truly centred.
            Spacer(Modifier.size(RojanDimens.MinTouchTarget))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CustomerHairline),
        )
    }
}
