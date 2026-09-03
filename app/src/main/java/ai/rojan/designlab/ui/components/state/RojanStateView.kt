package ai.rojan.designlab.ui.components.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ROJAN content-state dispatcher (UI Polish Sprint 3, Task 6).
 *
 * Renders **exactly one** of the three shared state cards
 * ([RojanLoadingState] / [RojanErrorState] / [RojanEmptyState]) or the
 * success [content] — never a blank screen, never "zeros that might be a
 * load failure". Screens that route their `Loading / Error / Empty /
 * Success` view-state through this get a consistent, unambiguous
 * treatment for free.
 *
 * Precedence (deliberate): loading → failure → empty → content. A
 * failure while stale content exists still shows the error card, because
 * silently showing old data as if it were fresh is the exact bug this
 * component exists to prevent.
 *
 * No data is fabricated anywhere here — [content] is only ever invoked
 * when the caller says there is real content to show.
 */
@Composable
fun RojanStateView(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    loadingMessage: String? = null,
    emptyTitle: String = "چیزی برای نمایش نیست",
    emptyDescription: String? = null,
    emptyIcon: ImageVector = Icons.Filled.Inbox,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    errorTitle: String = "مشکلی پیش آمد",
    retryLabel: String = "تلاش مجدد",
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> RojanLoadingState(modifier = modifier, message = loadingMessage)

        error != null -> RojanErrorState(
            title = errorTitle,
            modifier = modifier,
            description = error,
            actionLabel = onRetry?.let { retryLabel },
            onAction = onRetry,
        )

        isEmpty -> RojanEmptyState(
            title = emptyTitle,
            modifier = modifier,
            description = emptyDescription,
            icon = emptyIcon,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
        )

        else -> content()
    }
}
