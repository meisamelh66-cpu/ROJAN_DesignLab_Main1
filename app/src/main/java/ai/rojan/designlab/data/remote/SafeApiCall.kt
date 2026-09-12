package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.ApiErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** A non-2xx response the backend understood well enough to describe via its standard `ApiError` shape — see `ROJAN_Backend/API_CONTRACT.md`. */
class BackendApiException(
    val statusCode: Int,
    val apiError: ApiErrorDto?,
) : IOException(apiError?.message ?: "Backend request failed with status $statusCode")

/** No response reached the backend at all — offline, DNS failure, etc. Distinct from [BackendApiException] so callers can offer a "you're offline" state rather than a generic error. */
class NetworkUnavailableException(cause: Throwable) : IOException("No network connection", cause)

/**
 * The request reached (or attempted to reach) the backend but no response
 * arrived in time — distinct from [NetworkUnavailableException] (System2
 * Android Parallel Work, Phase A item 2: "separate ... timeout errors").
 * Previously indistinguishable from a plain connectivity failure (a
 * [SocketTimeoutException] is itself an [IOException], so it fell into the
 * generic `catch (e: IOException)` branch below) — callers can now offer
 * "the server is taking too long" rather than "you're offline," which is a
 * different, often actionable-differently condition (retry now vs. check
 * your connection).
 */
class RequestTimeoutException(cause: Throwable) : IOException("Request timed out", cause)

/**
 * A response arrived but its body did not match the DTO the client expects —
 * a backend contract drift: a renamed or removed field, a changed type, or
 * otherwise malformed JSON (kotlinx.serialization's [SerializationException],
 * which covers `JsonDecodingException` and `MissingFieldException`).
 *
 * Engineering Cleanup Phase 4 (P2): a [SerializationException] is a
 * `RuntimeException`, not an [IOException], so before this it fell through
 * every `catch` below and propagated out of `safeApiCall` — killing the
 * process (exactly the crash seen when `/api/v1/public/salons` shipped a
 * leaner shape than the DTO declared). It is now caught here and returned as
 * a normal `Result.failure`, so the screen shows an error state and offers a
 * retry instead of crashing. Modelled as an [IOException] subtype so the
 * many callers that already branch on `IOException` treat it gracefully.
 */
class MalformedResponseException(cause: Throwable) : IOException("Malformed response from server", cause)

/**
 * Any failure `safeApiCall` did not specifically anticipate. Surfaced as a
 * `Result.failure` rather than thrown (Engineering Cleanup Phase 4, P2), so a
 * single unforeseen exception type can never crash a repository call —
 * [userMessageFor][ai.rojan.designlab.presentation.common.userMessageFor]
 * renders it as the generic "unexpected error" message.
 */
class UnexpectedApiException(cause: Throwable) : IOException("Unexpected error", cause)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

/**
 * Every repository's suspend network call should be wrapped in this rather
 * than a bare `runCatching`, so a non-2xx response consistently becomes a
 * [BackendApiException] carrying the decoded [ApiErrorDto] (when the body
 * parses) instead of an opaque [HttpException], a timeout consistently
 * becomes [RequestTimeoutException], a malformed / contract-drifted body
 * consistently becomes [MalformedResponseException] (never an uncaught
 * crash), any other connectivity failure consistently becomes
 * [NetworkUnavailableException], and anything else at all becomes
 * [UnexpectedApiException] — narrow, predictable failure types instead of
 * callers having to inspect a raw [Throwable] themselves, and no path out of
 * here that throws.
 *
 * [CancellationException] is deliberately re-thrown, not converted: a
 * coroutine cancelled mid-call (screen left, ViewModel cleared) must
 * propagate as cancellation for structured concurrency, not be reported as a
 * failed request.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val apiError = e.response()?.errorBody()?.let { body ->
        runCatching { errorBodyJson.decodeFromString<ApiErrorDto>(body.string()) }.getOrNull()
    }
    Result.failure(BackendApiException(e.code(), apiError))
} catch (e: SocketTimeoutException) {
    Result.failure(RequestTimeoutException(e))
} catch (e: SerializationException) {
    Result.failure(MalformedResponseException(e))
} catch (e: IOException) {
    Result.failure(NetworkUnavailableException(e))
} catch (e: Exception) {
    Result.failure(UnexpectedApiException(e))
}
