package ai.rojan.designlab.manager.screens.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Manager OTP autofill: listens for the incoming OTP SMS via Google's SMS
 * Retriever API — no `READ_SMS`/`RECEIVE_SMS` permission involved, by
 * design (the OS only ever hands this app the one matching message, gated
 * by the 11-character app hash the backend embeds — see
 * `RequestOtpUseCase.buildMessage`/`OtpPolicy.smsRetrieverAppHash` in
 * ROJAN_Backend). [onCodeReceived] fires at most once per successfully
 * retrieved SMS; a timeout, a malformed/non-matching message body, or any
 * non-`SUCCESS` status is silently ignored — manual OTP entry (already
 * always available in [ManagerOtpAuthScreen]) is the fallback for all of
 * those, not a crash or a stuck state.
 *
 * [retryKey] re-registers the receiver and calls
 * `startSmsRetriever()` again whenever it changes — needed because a
 * single `startSmsRetriever()` call only listens for one incoming SMS
 * (it stops after a match or after its own internal ~5-minute timeout).
 * Callers key this on `(phoneNumber, resendCounter)` so entering the
 * "awaiting code" step and every subsequent resend each get a fresh
 * listen window.
 *
 * The receiver is dynamically registered (never declared in
 * AndroidManifest.xml) and always unregistered in `onDispose` — the only
 * way this leaks is if a caller keys it on something that never changes
 * across the screen's lifetime, which would also never re-arm the
 * retriever after a resend, so that mistake is self-revealing.
 */
@Composable
fun ListenForOtpSms(retryKey: Any, onCodeReceived: (String) -> Unit) {
    val context = LocalContext.current
    val currentOnCodeReceived by rememberUpdatedState(onCodeReceived)

    DisposableEffect(retryKey) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivedContext: Context?, intent: Intent?) {
                if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                if (status.statusCode != CommonStatusCodes.SUCCESS) {
                    // Timeout (CommonStatusCodes.TIMEOUT) or any other non-success
                    // status: no-op. Manual entry remains available either way.
                    return
                }

                val smsBody = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return
                extractOtpCode(smsBody)?.let(currentOnCodeReceived)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
        SmsRetriever.getClient(context).startSmsRetriever()

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

/**
 * Matches `RequestOtpUseCase.buildMessage`'s fixed "...verification code is
 * NNNN..." shape (ROJAN_Backend) and recovers the 4-digit code — mirrors
 * the same extraction contract `MeliPayamakSharedPatternProvider`'s own
 * `CODE_PATTERN` already relies on server-side. Never logs [smsBody] or the
 * extracted code.
 */
private val OTP_CODE_PATTERN = Regex("""verification code is (\d{4})(?!\d)""")

internal fun extractOtpCode(smsBody: String): String? =
    OTP_CODE_PATTERN.find(smsBody)?.groupValues?.getOrNull(1)
