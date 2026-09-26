package ai.rojan.designlab.ui.components.update

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.update.AppUpdateUiState
import ai.rojan.designlab.presentation.update.AppUpdateViewModel
import ai.rojan.designlab.presentation.update.AppUpdateViewModelFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Android App Update Client, Phase 2B: the single, shared integration point
 * for all three flavors — wraps [content] (each Activity's existing root
 * graph: `RojanNavGraph`/`ManagerRootGraph`/`ReceptionRootGraph`, all left
 * completely unmodified) rather than being inserted inside any of them.
 * [content] always renders; a [AppUpdateDialog] only ever overlays on top of
 * it, so existing splash/auth/navigation/session logic inside [content] is
 * never delayed, blocked, or otherwise touched by this gate.
 *
 * The real update check itself, and the "run once, fail open" behavior, live
 * in [AppUpdateViewModel] — this composable only wires that ViewModel to the
 * real, app-wide [ai.rojan.designlab.domain.repository.AppUpdateRepository]
 * singleton (already flavor-correct — see `BackendApiContainer`'s own
 * `appUpdateRepository` wiring, keyed off `BuildConfig.APPLICATION_ID`) and
 * renders whatever [AppUpdateUiState] results.
 *
 * The actual `Intent.ACTION_VIEW` call — never an automatic APK
 * install, never a permission request — mirrors the app's one existing
 * precedent for opening an external link
 * ([ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s address-click
 * handler) exactly: a plain `Intent`, `runCatching` around `startActivity`
 * so a device with no app that can handle it never crashes.
 */
@Composable
fun AppUpdateGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val viewModel: AppUpdateViewModel = viewModel(
        factory = AppUpdateViewModelFactory(BackendApiContainerHolder.get(appContext).appUpdateRepository),
    )

    content()

    when (val state = viewModel.state) {
        is AppUpdateUiState.Optional -> AppUpdateDialog(
            info = state.info,
            mandatory = false,
            onUpdateClick = { downloadUrl -> openDownloadUrl(context, downloadUrl) },
            onDismiss = viewModel::dismissOptionalUpdate,
        )

        is AppUpdateUiState.Mandatory -> AppUpdateDialog(
            info = state.info,
            mandatory = true,
            onUpdateClick = { downloadUrl -> openDownloadUrl(context, downloadUrl) },
            onDismiss = {},
        )

        AppUpdateUiState.Hidden -> Unit
    }
}

private fun openDownloadUrl(context: android.content.Context, downloadUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
    runCatching { context.startActivity(intent) }
}
