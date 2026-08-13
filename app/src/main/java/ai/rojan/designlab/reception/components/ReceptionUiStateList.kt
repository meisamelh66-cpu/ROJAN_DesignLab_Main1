package ai.rojan.designlab.reception.components

import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Shared Loading/Error/Empty/Success rendering for every list-backed
 * screen in Reception (booking wizard's customer/service/specialist/time
 * pickers, the customer list, the dashboard's booking list) — one place
 * so all of them present the same three non-success states identically,
 * per `ROJAN System2 Reception Phase1 Controlled Implementation`'s
 * explicit "implement loading/error/empty states" scope item.
 *
 * [onRetryClick] (per `ROJAN_Reception_Phase1_Review_Fixes_Report_v1.md`,
 * fix 1) renders a retry action on the Error state when supplied —
 * `null` by default so existing call sites are unaffected; pass the same
 * `load*()` function that originally populated [state], since every real
 * loader in this codebase is already safe to call again.
 */
@Composable
fun <T> ReceptionUiStateList(
    state: UiState<List<T>>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> ReceptionStateNotice(modifier = modifier, showSpinner = true, text = "در حال بارگذاری…")
        is UiState.Error -> ReceptionStateNotice(modifier = modifier, text = state.message, onRetryClick = onRetryClick)
        UiState.Empty -> ReceptionStateNotice(modifier = modifier, text = emptyMessage)
        is UiState.Success -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            items(state.data) { item -> itemContent(item) }
        }
    }
}

@Composable
private fun ReceptionStateNotice(
    text: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false,
    onRetryClick: (() -> Unit)? = null,
) {
    ReceptionGlassSurface(modifier = modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showSpinner) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = RojanDimens.SpaceSM).size(28.dp), color = ReceptionPalette.textAccent)
            }
            Text(text = text, style = RojanTypography.Body, color = ReceptionPalette.textSecondary, textAlign = TextAlign.Center)
            if (onRetryClick != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                PremiumButton(text = "تلاش مجدد", onClick = onRetryClick)
            }
        }
    }
}
