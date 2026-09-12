package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * ROJAN Customer — the persistent bottom-navigation shell.
 *
 * The one place the customer's four/five main sections (Home, Explore,
 * Appointments, Favorites, Profile) get a shared, always-visible
 * [CustomerBottomBar]. Each of those routes wraps its screen in this shell
 * in `RojanNavGraph`; every other route (booking flow, salon / service /
 * specialist detail, appointment detail, reschedule, auth, the "coming
 * soon" screens) stays a plain pushed route with a back arrow and no bar.
 *
 * Deliberately structural only:
 *  - the bar sits **below** the content ([content] gets `weight(1f)`), it
 *    does not overlay — so screens no longer need to measure the bar's
 *    height and pad their scroll content by it;
 *  - it holds NO navigation logic. [onTabSelected] is raised to the caller
 *    (`RojanNavGraph`), exactly like [CustomerBottomBar]'s own contract;
 *  - it applies no background / theme / insets of its own — [content]
 *    already brings its own `HomeBackgroundTheme` (via `CustomerScaffold`
 *    or directly), and [CustomerBottomBar] paints its own opaque ground
 *    and takes `navigationBarsPadding`.
 *
 * Adds NO design-system token, edits NO shared component, touches no
 * ViewModel, repository, navigation route, or API.
 */
@Composable
fun CustomerMainScaffold(
    activeTab: CustomerHomeTab,
    onTabSelected: (CustomerHomeTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
        CustomerBottomBar(
            activeTab = activeTab,
            onTabSelected = onTabSelected,
        )
    }
}
