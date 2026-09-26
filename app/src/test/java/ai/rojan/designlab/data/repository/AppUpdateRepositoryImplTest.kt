package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AppReleaseApi
import ai.rojan.designlab.data.remote.dto.AppReleaseLatestResponseDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Android App Update Client, Phase 2B. `safeApiCall`'s own exception-mapping
 * behavior (IOException -> NetworkUnavailableException, SerializationException
 * -> MalformedResponseException, etc.) is already locked in by
 * [ai.rojan.designlab.data.remote.SafeApiCallTest] - these tests only cover
 * this repository's own responsibility: forwarding the exact
 * applicationId/versionCode it was constructed with (never hardcoded, never
 * cross-flavor), and mapping a real response into
 * [ai.rojan.designlab.domain.repository.AppUpdateInfo] without inventing or
 * dropping any field.
 */
class AppUpdateRepositoryImplTest {

    private class FakeAppReleaseApi(
        private val response: AppReleaseLatestResponseDto? = null,
        private val throwing: Throwable? = null,
    ) : AppReleaseApi {
        var lastApplicationId: String? = null
            private set
        var lastVersionCode: Int? = null
            private set

        override suspend fun getLatestRelease(applicationId: String, versionCode: Int): AppReleaseLatestResponseDto {
            lastApplicationId = applicationId
            lastVersionCode = versionCode
            throwing?.let { throw it }
            return response!!
        }
    }

    private fun realResponse(
        updateAvailable: Boolean = true,
        forceUpdate: Boolean = false,
        latestVersionCode: Int = 2,
    ) = AppReleaseLatestResponseDto(
        updateAvailable = updateAvailable,
        forceUpdate = forceUpdate,
        latestVersion = "1.0.$latestVersionCode",
        latestVersionCode = latestVersionCode,
        downloadUrl = "https://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.$latestVersionCode.apk",
        sha256 = "2898b950da790e12f7197fd0cf30c6543ebff9e929d45c533056f13f2ed7efcd",
        fileSizeBytes = 6_064_180L,
        releaseNotes = "Bug fixes",
        releaseDate = "2026-09-26",
    )

    // ---------- current == latest / current < latest (versionCode comparison) ----------

    @Test
    fun `current versionCode equal to latest - server says no update available, mapped through unchanged`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse(updateAvailable = false, latestVersionCode = 1))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", currentVersionCode = 1)

        val result = repository.checkForUpdate()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.updateAvailable)
    }

    @Test
    fun `current versionCode behind latest - server-computed updateAvailable and real latestVersionCode preserved`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse(updateAvailable = true, latestVersionCode = 5))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", currentVersionCode = 1)

        val info = repository.checkForUpdate().getOrNull()!!

        assertTrue(info.updateAvailable)
        assertEquals(5, info.latestVersionCode)
        assertEquals(1, api.lastVersionCode)
    }

    // ---------- optional vs mandatory mapping fidelity ----------

    @Test
    fun `optional update - forceUpdate false is preserved unchanged`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse(updateAvailable = true, forceUpdate = false))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1)

        val info = repository.checkForUpdate().getOrNull()!!

        assertTrue(info.updateAvailable)
        assertFalse(info.forceUpdate)
    }

    @Test
    fun `mandatory update - forceUpdate true is preserved unchanged, never recomputed client-side`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse(updateAvailable = true, forceUpdate = true))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1)

        val info = repository.checkForUpdate().getOrNull()!!

        assertTrue(info.forceUpdate)
    }

    @Test
    fun `sha256 and fileSizeBytes are preserved unchanged for future integrity verification`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse())
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1)

        val info = repository.checkForUpdate().getOrNull()!!

        assertEquals("2898b950da790e12f7197fd0cf30c6543ebff9e929d45c533056f13f2ed7efcd", info.sha256)
        assertEquals(6_064_180L, info.fileSizeBytes)
    }

    // ---------- failure paths: fail open, never a thrown exception ----------

    @Test
    fun `a network failure never throws - comes back as Result failure`() = runTest {
        val api = FakeAppReleaseApi(throwing = IOException("no route to host"))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1)

        val result = repository.checkForUpdate()

        assertTrue(result.isFailure)
    }

    @Test
    fun `a malformed response never throws - comes back as Result failure`() = runTest {
        val api = FakeAppReleaseApi(throwing = SerializationException("decode failed"))
        val repository = AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1)

        val result = repository.checkForUpdate()

        assertTrue(result.isFailure)
    }

    // ---------- three real applicationIds, never hardcoded, never cross-flavor ----------

    @Test
    fun `Manager applicationId is forwarded exactly as constructed - never hardcoded`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse())
        AppUpdateRepositoryImpl(api, "ai.rojan.designlab.manager", 1).checkForUpdate()

        assertEquals("ai.rojan.designlab.manager", api.lastApplicationId)
    }

    @Test
    fun `Customer applicationId is forwarded exactly as constructed - never hardcoded`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse())
        AppUpdateRepositoryImpl(api, "ai.rojan.designlab", 1).checkForUpdate()

        assertEquals("ai.rojan.designlab", api.lastApplicationId)
    }

    @Test
    fun `Reception applicationId is forwarded exactly as constructed - never hardcoded`() = runTest {
        val api = FakeAppReleaseApi(response = realResponse())
        AppUpdateRepositoryImpl(api, "ai.rojan.designlab.reception", 1).checkForUpdate()

        assertEquals("ai.rojan.designlab.reception", api.lastApplicationId)
    }
}
