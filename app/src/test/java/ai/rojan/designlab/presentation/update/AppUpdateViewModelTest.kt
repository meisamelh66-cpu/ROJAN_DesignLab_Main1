package ai.rojan.designlab.presentation.update

import ai.rojan.designlab.data.remote.AppReleaseApi
import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.MalformedResponseException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.data.remote.dto.AppReleaseLatestResponseDto
import ai.rojan.designlab.data.repository.AppUpdateRepositoryImpl
import ai.rojan.designlab.domain.repository.AppUpdateInfo
import ai.rojan.designlab.domain.repository.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Android App Update Client, Phase 2B. The server is the sole authority for
 * updateAvailable/forceUpdate/version/URL/notes (already verified server-side
 * in the backend's own `AppReleaseUseCasesTest`); these tests cover only this
 * ViewModel's own job - mapping a real [AppUpdateInfo] (or a real failure)
 * into the correct [AppUpdateUiState], exactly once per instance, and never
 * letting a mandatory update be bypassed.
 *
 * "Update action opens the correct server URL" (a required Phase 2B test
 * scenario) is verified here as far as a plain JVM unit test can reach
 * without Robolectric or an instrumented test (neither is part of this
 * project) - by asserting the exact, real, non-hardcoded `downloadUrl` a
 * caller would pass to `Intent.ACTION_VIEW` is present, unchanged, on the
 * exposed state. The actual `Intent`/`startActivity` call itself
 * ([ai.rojan.designlab.ui.components.update.AppUpdateGate]) mirrors this
 * app's one existing, likewise-untested precedent for firing such an intent
 * ([ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s address-click
 * handler) - this codebase has no existing convention or dependency for unit
 * -testing that exact call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAppUpdateRepository(
        private val result: Result<AppUpdateInfo>,
    ) : AppUpdateRepository {
        var callCount = 0
            private set

        override suspend fun checkForUpdate(): Result<AppUpdateInfo> {
            callCount++
            return result
        }
    }

    private fun info(
        updateAvailable: Boolean = true,
        forceUpdate: Boolean = false,
        downloadUrl: String = "https://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.1.apk",
        latestVersionCode: Int = 2,
    ) = AppUpdateInfo(
        updateAvailable = updateAvailable,
        forceUpdate = forceUpdate,
        latestVersionName = "1.0.$latestVersionCode",
        latestVersionCode = latestVersionCode,
        downloadUrl = downloadUrl,
        sha256 = "2898b950da790e12f7197fd0cf30c6543ebff9e929d45c533056f13f2ed7efcd",
        fileSizeBytes = 6_064_180L,
        releaseNotes = "Bug fixes",
        releaseDate = "2026-09-26",
    )

    // ---------- current == latest / current < latest ----------

    @Test
    fun `current version equals latest - state stays Hidden, app continues normally`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = false))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `current version behind latest, not mandatory - Optional state with the real info`() {
        val expected = info(updateAvailable = true, forceUpdate = false)
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(expected)))

        val state = viewModel.state
        assertTrue(state is AppUpdateUiState.Optional)
        assertEquals(expected, (state as AppUpdateUiState.Optional).info)
    }

    @Test
    fun `mandatory update - Mandatory state with the real info`() {
        val expected = info(updateAvailable = true, forceUpdate = true)
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(expected)))

        val state = viewModel.state
        assertTrue(state is AppUpdateUiState.Mandatory)
        assertEquals(expected, (state as AppUpdateUiState.Mandatory).info)
    }

    // ---------- fail-open: network / HTTP / malformed / invalid download URL ----------

    @Test
    fun `a network failure fails open - state stays Hidden, never blocks the app`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.failure(NetworkUnavailableException(Exception("offline")))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `an HTTP error fails open - state stays Hidden, never blocks the app`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.failure(BackendApiException(500, null))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `a malformed response fails open - state stays Hidden, never blocks the app`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.failure(MalformedResponseException(Exception("bad body")))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `updateAvailable true with a blank downloadUrl fails open rather than showing an unusable prompt`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, downloadUrl = ""))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `a malformed downloadUrl fails open rather than showing an unusable prompt`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, downloadUrl = "not-a-url"))))

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `a downloadUrl with an unsupported scheme fails open rather than showing an unusable prompt`() {
        val viewModel = AppUpdateViewModel(
            FakeAppUpdateRepository(
                Result.success(info(updateAvailable = true, downloadUrl = "ftp://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.1.apk")),
            ),
        )

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `a valid https downloadUrl is still actionable as an Optional update`() {
        val realUrl = "https://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.1.apk"
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, downloadUrl = realUrl))))

        val state = viewModel.state
        assertTrue(state is AppUpdateUiState.Optional)
        assertEquals(realUrl, (state as AppUpdateUiState.Optional).info.downloadUrl)
    }

    @Test
    fun `a valid http downloadUrl is still actionable as an Optional update`() {
        val realUrl = "http://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.1.apk"
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, downloadUrl = realUrl))))

        val state = viewModel.state
        assertTrue(state is AppUpdateUiState.Optional)
        assertEquals(realUrl, (state as AppUpdateUiState.Optional).info.downloadUrl)
    }

    @Test
    fun `a mandatory update with a malformed downloadUrl fails open instead of trapping the user`() {
        val viewModel = AppUpdateViewModel(
            FakeAppUpdateRepository(Result.success(info(updateAvailable = true, forceUpdate = true, downloadUrl = "not-a-url"))),
        )

        assertEquals(
            "a malformed URL must never produce an un-bypassable Mandatory dialog",
            AppUpdateUiState.Hidden,
            viewModel.state,
        )
    }

    // ---------- current versionCode greater than latest: exercises the real repository path ----------

    @Test
    fun `current versionCode greater than latest - state stays Hidden through the real repository, not a canned constant`() {
        val fakeApi = object : AppReleaseApi {
            override suspend fun getLatestRelease(applicationId: String, versionCode: Int): AppReleaseLatestResponseDto {
                // Mirrors the real backend contract: the server compares versionCode and
                // reports updateAvailable=false once the caller is already ahead of latest.
                return AppReleaseLatestResponseDto(
                    updateAvailable = versionCode < 1,
                    forceUpdate = false,
                    latestVersion = "1.0.1",
                    latestVersionCode = 1,
                    downloadUrl = "https://rojanai.ir/downloads/manager/ROJAN-AI-Manager-1.0.1.apk",
                    sha256 = "2898b950da790e12f7197fd0cf30c6543ebff9e929d45c533056f13f2ed7efcd",
                    fileSizeBytes = 6_064_180L,
                    releaseNotes = "Bug fixes",
                    releaseDate = "2026-09-26",
                )
            }
        }
        val repository = AppUpdateRepositoryImpl(fakeApi, "ai.rojan.designlab.manager", currentVersionCode = 5)

        val viewModel = AppUpdateViewModel(repository)

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    // ---------- exactly once per instance, never re-triggered by recomposition ----------

    @Test
    fun `the update check runs exactly once per ViewModel instance`() {
        val repository = FakeAppUpdateRepository(Result.success(info(updateAvailable = false)))
        val viewModel = AppUpdateViewModel(repository)

        // Reading .state repeatedly is what a recomposing caller does - it must never re-trigger the check.
        repeat(5) { viewModel.state }

        assertEquals(1, repository.callCount)
    }

    // ---------- optional dismissal vs mandatory blocking ----------

    @Test
    fun `an optional update can be dismissed`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, forceUpdate = false))))
        assertTrue(viewModel.state is AppUpdateUiState.Optional)

        viewModel.dismissOptionalUpdate()

        assertEquals(AppUpdateUiState.Hidden, viewModel.state)
    }

    @Test
    fun `a mandatory update cannot be dismissed - state stays Mandatory`() {
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(updateAvailable = true, forceUpdate = true))))
        assertTrue(viewModel.state is AppUpdateUiState.Mandatory)

        viewModel.dismissOptionalUpdate()

        assertTrue("mandatory update must not be bypassable", viewModel.state is AppUpdateUiState.Mandatory)
    }

    // ---------- the update action's target URL is the real, server-provided one ----------

    @Test
    fun `the exposed downloadUrl for an optional update is the real server URL, never a hardcoded one`() {
        val realUrl = "https://rojanai.ir/downloads/customer/ROJAN-AI-Customer-1.0.2.apk"
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(downloadUrl = realUrl))))

        val state = viewModel.state as AppUpdateUiState.Optional
        assertEquals(realUrl, state.info.downloadUrl)
    }

    @Test
    fun `the exposed downloadUrl for a mandatory update is the real server URL, never a hardcoded one`() {
        val realUrl = "https://rojanai.ir/downloads/reception/ROJAN-AI-Reception-1.0.3.apk"
        val viewModel = AppUpdateViewModel(FakeAppUpdateRepository(Result.success(info(forceUpdate = true, downloadUrl = realUrl))))

        val state = viewModel.state as AppUpdateUiState.Mandatory
        assertEquals(realUrl, state.info.downloadUrl)
    }
}
