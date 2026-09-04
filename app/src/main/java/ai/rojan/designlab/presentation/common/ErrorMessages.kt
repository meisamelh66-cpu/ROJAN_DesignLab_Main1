package ai.rojan.designlab.presentation.common

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.InvalidResponseException
import ai.rojan.designlab.data.remote.NetworkUnavailableException

/**
 * Maps a repository failure to a user-facing, Persian message consistent
 * with the rest of the app's copy. Deliberately does NOT surface
 * [BackendApiException.apiError]'s raw backend message (English, and
 * potentially containing an internal identifier like a UUID) — status-code
 * driven generic copy instead, differentiated enough to be useful without
 * leaking backend internals to the UI.
 */
fun userMessageFor(throwable: Throwable): String = when (throwable) {
    is NetworkUnavailableException -> "اتصال اینترنت برقرار نیست. لطفاً دوباره تلاش کنید."
    is InvalidResponseException -> "پاسخ سرور نامعتبر بود. لطفاً دوباره تلاش کنید."
    is BackendApiException -> when (throwable.statusCode) {
        401 -> "برای این عملیات نیاز به ورود مجدد دارید."
        403 -> "اجازه دسترسی به این بخش را ندارید."
        404 -> "موردی یافت نشد."
        409 -> "این عملیات با وضعیت فعلی سازگار نیست."
        else -> "خطایی در ارتباط با سرور رخ داد."
    }
    else -> "خطایی غیرمنتظره رخ داد."
}
