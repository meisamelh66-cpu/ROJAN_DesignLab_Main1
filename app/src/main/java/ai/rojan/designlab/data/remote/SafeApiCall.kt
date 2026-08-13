package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.ApiErrorDto
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

private val errorBodyJson = Json { ignoreUnknownKeys = true }

/**
 * Every repository's suspend network call should be wrapped in this rather
 * than a bare `runCatching`, so a non-2xx response consistently becomes a
 * [BackendApiException] carrying the decoded [ApiErrorDto] (when the body
 * parses) instead of an opaque [HttpException], a timeout consistently
 * becomes [RequestTimeoutException], and any other connectivity failure
 * consistently becomes [NetworkUnavailableException] — three distinct,
 * narrow exception types instead of callers having to inspect a raw
 * [IOException] subtype themselves.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val apiError = e.response()?.errorBody()?.let { body ->
        runCatching { errorBodyJson.decodeFromString<ApiErrorDto>(body.string()) }.getOrNull()
    }
    Result.failure(BackendApiException(e.code(), apiError))
} catch (e: SocketTimeoutException) {
    Result.failure(RequestTimeoutException(e))
} catch (e: IOException) {
    Result.failure(NetworkUnavailableException(e))
}
