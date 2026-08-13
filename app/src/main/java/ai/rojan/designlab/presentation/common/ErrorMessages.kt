package ai.rojan.designlab.presentation.common

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.data.remote.RequestTimeoutException

/**
 * Maps a repository failure to a user-facing, Persian message consistent
 * with the rest of the app's copy. Deliberately does NOT surface
 * [BackendApiException.apiError]'s raw backend message (English, and
 * potentially containing an internal identifier like a UUID) — status-code
 * driven generic copy instead, differentiated enough to be useful without
 * leaking backend internals to the UI. This applies to `400` too (System2
 * Android Parallel Work, Phase A item 2) — a validation failure gets its
 * own distinct, generic Persian message, not the raw backend validation
 * text, and specifically never the *network*-failure message: a `400`
 * means the request reached the server and was understood, the opposite
 * condition from [NetworkUnavailableException]/[RequestTimeoutException],
 * and conflating the two would tell a user to "check your connection" for
 * a problem connectivity had nothing to do with.
 */
fun userMessageFor(throwable: Throwable): String = when (throwable) {
    is RequestTimeoutException -> "زمان اتصال به پایان رسید. لطفاً دوباره تلاش کنید."
    is NetworkUnavailableException -> "اتصال اینترنت برقرار نیست. لطفاً دوباره تلاش کنید."
    is BackendApiException -> when (throwable.statusCode) {
        400 -> "اطلاعات وارد‌شده نامعتبر است. لطفاً دوباره بررسی کنید."
        401 -> "برای این عملیات نیاز به ورود مجدد دارید."
        403 -> "اجازه دسترسی به این بخش را ندارید."
        404 -> "موردی یافت نشد."
        409 -> "این عملیات با وضعیت فعلی سازگار نیست."
        in 500..599 -> "خطایی در سرور رخ داد. لطفاً بعداً دوباره تلاش کنید."
        else -> "خطایی در ارتباط با سرور رخ داد."
    }
    else -> "خطایی غیرمنتظره رخ داد."
}
