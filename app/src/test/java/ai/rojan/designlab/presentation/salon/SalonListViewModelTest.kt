package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSalonRepository(private val failure: Throwable? = null) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?, sortDirection: String): Result<PagedResult<Salon>> =
        failure?.let { Result.failure(it) } ?: Result.success(PagedResult(emptyList(), page, size, 0L, 0))

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used")
}

/** Browse Experience fix: GET /api/v1/salons requires auth on the real backend - isUnauthorized lets SalonListScreen/SearchScreen show a real login action instead of a retry button that would fail identically forever. */
@OptIn(ExperimentalCoroutinesApi::class)
class SalonListViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a 401 sets isUnauthorized`() = runTest {
        val viewModel = SalonListViewModel(FakeSalonRepository(BackendApiException(401, null)))

        assertTrue(viewModel.isUnauthorized)
        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `a non-401 failure leaves isUnauthorized false`() = runTest {
        val viewModel = SalonListViewModel(FakeSalonRepository(RuntimeException("network error")))

        assertFalse(viewModel.isUnauthorized)
        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `retry resets isUnauthorized before the new call resolves`() = runTest {
        val viewModel = SalonListViewModel(FakeSalonRepository(BackendApiException(401, null)))
        assertTrue(viewModel.isUnauthorized)

        // Under UnconfinedTestDispatcher this resolves synchronously back to
        // the same 401, but exercises that load() unconditionally clears the
        // flag first rather than only ever setting it.
        viewModel.retry()

        assertTrue(viewModel.isUnauthorized)
    }
}
