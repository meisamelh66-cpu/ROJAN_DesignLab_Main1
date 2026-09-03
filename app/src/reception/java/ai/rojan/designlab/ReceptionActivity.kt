package ai.rojan.designlab

import ai.rojan.designlab.reception.navigation.ReceptionRootGraph
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ROJAN Reception app's launcher entry point — the `reception` product
 * flavor's counterpart to Customer's [MainActivity] and Manager's
 * [ManagerActivity] (both left untouched; this flavor removes MainActivity
 * via `src/reception/AndroidManifest.xml`, and does not touch Manager's
 * flavor-only source set at all). Same shape as [ManagerActivity]: no
 * JWT/session/OTP logic lives in this Activity itself, only theming setup
 * and a handoff to [ReceptionRootGraph].
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 *
 * P1-1, Reception Invite Acceptance Flow: [extractInviteToken] reads a
 * `rojanreception://invite/{token}` deep link (registered in
 * `src/reception/AndroidManifest.xml`) and threads the token through to
 * [ReceptionRootGraph]'s [inviteToken] parameter — the one piece of state
 * that determines whether the invite acceptance screen (outside the
 * authentication gate) or the normal gated app renders. [onNewIntent] keeps
 * this working if the Activity is already running when a second invite
 * link is opened (`launchMode="singleTask"`-equivalent re-delivery), not
 * only on a cold start via [onCreate]. The token is held only in this
 * `mutableStateOf` and passed straight to [ReceptionRootGraph] — never
 * logged, never written to any persistence layer (see
 * [ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel]'s
 * own doc comment for the same constraint on its side).
 *
 * **Known gap, disclosed rather than silently unhandled:** the real invite
 * link the backend generates (`GenerateSalonInviteQrCodeUseCase`) is a
 * plain HTTPS URL (`{publicBaseUrl}/invite/{token}`), not this custom
 * scheme — making that HTTPS form auto-open this app requires an Android
 * App Links `autoVerify="true"` intent-filter backed by a real, hosted
 * `assetlinks.json` at that domain, which needs the actual `publicBaseUrl`
 * per environment (a System 1/infra fact this Android-only change doesn't
 * have) plus infra work outside this repo. This custom scheme is the
 * Android-side-only, zero-dependency piece of that: fully working today
 * (via a QR code encoding it, or `adb shell am start -a
 * android.intent.action.VIEW -d "rojanreception://invite/TOKEN"`), and the
 * one addition needed once the HTTPS form is ready is a second
 * `<intent-filter>` in the manifest — no Kotlin change here, since
 * [extractInviteToken] already reads [android.net.Uri.getLastPathSegment]
 * generically rather than assuming this scheme specifically.
 */
class ReceptionActivity : ComponentActivity() {

    private var inviteToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inviteToken = extractInviteToken(intent)

        setContent {
            CompositionLocalProvider(LocalRojanPalette provides ReceptionPalette) {
                RojanTheme {
                    ReceptionRootGraph(inviteToken = inviteToken)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inviteToken = extractInviteToken(intent)
    }

    private fun extractInviteToken(intent: Intent?): String? =
        intent?.data
            ?.takeIf { it.scheme == "rojanreception" && it.host == "invite" }
            ?.lastPathSegment
            ?.takeIf { it.isNotBlank() }
}
