package ai.rojan.designlab.screens.bookingflow.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.screens.customer.components.CustomerScaffold

/**
 * Forwarder — the real implementation is
 * [ai.rojan.designlab.screens.customer.components.CustomerScaffold]. Kept so
 * the already-migrated booking screens compile unchanged during the app-wide
 * migration; new code should call `CustomerScaffold` directly.
 */
@Composable
fun BookingScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    step: Int? = null,
    totalSteps: Int = 5,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = CustomerScaffold(
    title = title,
    onBackClick = onBackClick,
    modifier = modifier,
    step = step,
    totalSteps = totalSteps,
    bottomBar = bottomBar,
    content = content,
)
