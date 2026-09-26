package ai.rojan.designlab.presentation.update

import ai.rojan.designlab.domain.repository.AppUpdateInfo
import ai.rojan.designlab.domain.repository.AppUpdateRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException

/** What [AppUpdateGate][ai.rojan.designlab.ui.components.update.AppUpdateGate] renders on top of the app's normal content. */
sealed interface AppUpdateUiState {
    /** No update, or the check hasn't resolved (yet, or ever) — the app behaves exactly as if this ViewModel didn't exist. */
    data object Hidden : AppUpdateUiState
    data class Optional(val info: AppUpdateInfo) : AppUpdateUiState
    data class Mandatory(val info: AppUpdateInfo) : AppUpdateUiState
}

/**
 * Runs the real update check exactly once per instance — from [init], never
 * re-triggered by [dismissOptionalUpdate] or by a caller reading [state]
 * repeatedly. Since this ViewModel is obtained via Compose's `viewModel()`
 * (see [ai.rojan.designlab.ui.components.update.AppUpdateGate]), the same
 * instance survives every recomposition of its caller for as long as the
 * owning Activity does — "once per app process/open" falls out of that
 * existing, well-understood Compose/ViewModel lifetime guarantee, not from
 * anything bespoke here.
 *
 * Fails open on any repository failure (network, timeout, HTTP error,
 * malformed response) — [state] simply stays [AppUpdateUiState.Hidden]; this
 * class never surfaces an error to the user or blocks anything. A
 * structurally well-formed but practically unusable response (`updateAvailable`
 * true with a blank, malformed, or non-http(s) `downloadUrl`) is treated the
 * same way — never shown as an update the user can't actually act on, and
 * never as a mandatory dialog the user can't bypass or escape. The server
 * remains the sole authority for `updateAvailable`/`forceUpdate`/version/
 * URL/notes; this class only maps that real result to a UI state, never
 * recomputes it.
 */
class AppUpdateViewModel(
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    var state by mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Hidden)
        private set

    init {
        viewModelScope.launch {
            val info = appUpdateRepository.checkForUpdate().getOrNull() ?: return@launch
            state = info.toUiState()
        }
    }

    /**
     * The «بعداً» action. A no-op while [state] is [AppUpdateUiState.Mandatory] —
     * this is the one place a mandatory update's "cannot bypass" requirement
     * is enforced at the state layer (the mandatory dialog itself never even
     * renders a dismiss control that could call this, so this guard is
     * defense in depth, not the only mechanism).
     */
    fun dismissOptionalUpdate() {
        if (state is AppUpdateUiState.Mandatory) return
        state = AppUpdateUiState.Hidden
    }

    private fun AppUpdateInfo.toUiState(): AppUpdateUiState = when {
        !updateAvailable -> AppUpdateUiState.Hidden
        !downloadUrl.isActionableDownloadUrl() -> AppUpdateUiState.Hidden
        forceUpdate -> AppUpdateUiState.Mandatory(this)
        else -> AppUpdateUiState.Optional(this)
    }
}

/**
 * True only for a syntactically valid `http://`/`https://` URL — anything
 * else (blank, malformed, or another scheme such as `ftp://`) is not
 * something [AppUpdateGate][ai.rojan.designlab.ui.components.update.AppUpdateGate]
 * can safely hand to `Intent.ACTION_VIEW`, so it's treated the same as "no
 * update" rather than shown as an actionable (and possibly un-bypassable
 * mandatory) prompt. Uses only [java.net.URI] from the standard library —
 * no third-party URL-validation dependency.
 */
private fun String.isActionableDownloadUrl(): Boolean {
    if (isBlank()) return false
    val uri = try {
        URI(this)
    } catch (e: URISyntaxException) {
        return false
    }
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}
