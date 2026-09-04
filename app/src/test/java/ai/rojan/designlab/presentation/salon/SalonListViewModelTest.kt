package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SalonListViewModel] backs both `SearchScreen` and `SalonListScreen`. The
 * 401 → [SalonListViewModel.isUnauthorized] path was covered; 5B-8A adds
 * the load / cache / pagination contract the search-scroll fix (T2-1) and
 * "load more" depend on.
 *
 * A [StandardTestDispatcher] (not auto-advancing) is used so the moment
 * between `load(...)` being called and the request resolving is
 * observable — that window is exactly where the "keep the old list
 * visible, don't flash Loading" behaviour lives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalonListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- fakes / builders -----------------------------------------

    /** Records every page requested and delegates the response to [respond]. */
    private class ScriptedSalonRepository(
        private val respond: suspend (page: Int, nameFilter: String?) -> Result<PagedResult<Salon>>,
    ) : SalonRepository {
        val pagesRequested = mutableListOf<Int>()
        val filtersRequested = mutableListOf<String?>()

        override suspend fun browseSalons(
            page: Int,
            size: Int,
            nameFilter: String?,
            sortDirection: String,
        ): Result<PagedResult<Salon>> {
            pagesRequested += page
            filtersRequested += nameFilter
            return respond(page, nameFilter)
        }

        override suspend fun getSalon(salonId: String): Result<Salon> = error("not used")
    }

    private fun salon(id: String) =
        Salon(id = id, name = "Salon $id", description = null, phone = "0", email = null, address = "addr")

    private fun salons(ids: Iterable<Int>) = ids.map { salon("s$it") }

    private fun page(content: List<Salon>, page: Int, totalPages: Int) = PagedResult(
        content = content,
        page = page,
        size = 20,
        totalElements = (totalPages.toLong() * 20),
        totalPages = totalPages,
    )

    private fun successData(vm: SalonListViewModel): List<Salon> {
        val state = vm.state
        assertTrue("expected UiState.Success, was $state", state is UiState.Success)
        return (state as UiState.Success).data
    }

    // ---- existing: 401 handling ---------------------------------

    @Test
    fun `a 401 sets isUnauthorized`() = runTest(dispatcher) {
        val vm = SalonListViewModel(ScriptedSalonRepository { _, _ -> Result.failure(BackendApiException(401, null)) })
        advanceUntilIdle()

        assertTrue(vm.isUnauthorized)
        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `a non-401 failure leaves isUnauthorized false`() = runTest(dispatcher) {
        val vm = SalonListViewModel(ScriptedSalonRepository { _, _ -> Result.failure(RuntimeException("network error")) })
        advanceUntilIdle()

        assertFalse(vm.isUnauthorized)
        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `retry clears isUnauthorized before the new call resolves, then re-sets it on the same 401`() = runTest(dispatcher) {
        val vm = SalonListViewModel(ScriptedSalonRepository { _, _ -> Result.failure(BackendApiException(401, null)) })
        advanceUntilIdle()
        assertTrue(vm.isUnauthorized)

        vm.retry()
        assertFalse("load() unconditionally clears the flag first", vm.isUnauthorized)

        advanceUntilIdle()
        assertTrue("same 401 on retry re-sets it", vm.isUnauthorized)
    }

    // ---- 1. re-query with a cached result -----------------------

    @Test
    fun `re-query with a cached result keeps the list on screen and never flashes Loading`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { _, _ -> Result.success(page(salons(1..2), page = 0, totalPages = 1)) }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        val cached = successData(vm)

        vm.load("spa")

        // request is in flight — the previous result must still be shown
        assertTrue("state stayed Success, not Loading", vm.state is UiState.Success)
        assertEquals(cached, (vm.state as UiState.Success).data)
        assertTrue("isSearching signals the in-flight follow-up", vm.isSearching)

        advanceUntilIdle()
        assertFalse(vm.isSearching)
        assertTrue(vm.state is UiState.Success)
        assertEquals(listOf(null, "spa"), repo.filtersRequested)
    }

    @Test
    fun `the first load, with no cached result, does go through Loading`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { _, _ -> Result.success(page(salons(1..2), page = 0, totalPages = 1)) }
        val vm = SalonListViewModel(repo)

        // init { load() } launched; nothing resolved yet, no prior Success
        assertTrue("no cached result → Loading", vm.state is UiState.Loading)
        assertFalse(vm.isSearching)

        advanceUntilIdle()
        assertTrue(vm.state is UiState.Success)
    }

    // ---- 2. empty first result --------------------------------

    @Test
    fun `an empty first page resolves to UiState Empty`() = runTest(dispatcher) {
        val vm = SalonListViewModel(ScriptedSalonRepository { p, _ -> Result.success(page(emptyList(), page = p, totalPages = 0)) })
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Empty)
        assertFalse(vm.canLoadMore)
    }

    @Test
    fun `a search that matches nothing resolves to Empty even with a prior Success`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { _, filter ->
            if (filter == null) Result.success(page(salons(1..2), page = 0, totalPages = 1))
            else Result.success(page(emptyList(), page = 0, totalPages = 0))
        }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Success)

        vm.load("no-such-salon")
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Empty)
    }

    // ---- 3. first load failure --------------------------------

    @Test
    fun `a first load failure resolves to UiState Error`() = runTest(dispatcher) {
        val vm = SalonListViewModel(ScriptedSalonRepository { _, _ -> Result.failure(RuntimeException("network")) })
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
    }

    // ---- 4. follow-up failure keeps the previous Success -------

    @Test
    fun `a follow-up failure keeps the previously loaded list visible`() = runTest(dispatcher) {
        var failNext = false
        val repo = ScriptedSalonRepository { _, _ ->
            if (failNext) Result.failure(RuntimeException("network"))
            else Result.success(page(salons(1..3), page = 0, totalPages = 1))
        }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        val cached = successData(vm)

        failNext = true
        vm.load("spa")
        advanceUntilIdle()

        assertTrue("previous Success retained on a follow-up failure", vm.state is UiState.Success)
        assertEquals(cached, (vm.state as UiState.Success).data)
        assertFalse(vm.isSearching)
    }

    @Test
    fun `a follow-up 401 keeps the list but still raises isUnauthorized`() = runTest(dispatcher) {
        var fail401 = false
        val repo = ScriptedSalonRepository { _, _ ->
            if (fail401) Result.failure(BackendApiException(401, null))
            else Result.success(page(salons(1..3), page = 0, totalPages = 1))
        }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        val cached = successData(vm)

        fail401 = true
        vm.load("spa")
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertEquals(cached, (vm.state as UiState.Success).data)
        assertTrue(vm.isUnauthorized)
    }

    // ---- 5. pagination ---------------------------------------

    @Test
    fun `loadMore appends the next page and flips canLoadMore off at the last page`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { p, _ ->
            when (p) {
                0 -> Result.success(page(salons(1..20), page = 0, totalPages = 2))
                1 -> Result.success(page(salons(21..40), page = 1, totalPages = 2))
                else -> Result.failure(AssertionError("unexpected page $p"))
            }
        }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        assertEquals(20, successData(vm).size)
        assertTrue("page 0 of 2 → more to load", vm.canLoadMore)

        vm.loadMore()
        advanceUntilIdle()

        assertEquals("second page appended, not replaced", 40, successData(vm).size)
        assertEquals(listOf("s1", "s21"), listOf(successData(vm).first().id, successData(vm)[20].id))
        assertFalse("page 1 of 2 → no more", vm.canLoadMore)
        assertFalse(vm.isLoadingMore)
        assertEquals(listOf(0, 1), repo.pagesRequested)
    }

    // ---- 6. loadMore guards ---------------------------------

    @Test
    fun `loadMore is a no-op while a page is already loading`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { p, _ ->
            when (p) {
                0 -> Result.success(page(salons(1..20), page = 0, totalPages = 3))
                1 -> Result.success(page(salons(21..40), page = 1, totalPages = 3))
                else -> Result.failure(AssertionError("unexpected page $p"))
            }
        }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()

        vm.loadMore() // sets isLoadingMore, launches the page-1 request
        assertTrue(vm.isLoadingMore)
        vm.loadMore() // guarded — must not fire a second request

        advanceUntilIdle()
        assertEquals("page 1 requested exactly once", listOf(0, 1), repo.pagesRequested)
    }

    @Test
    fun `loadMore is a no-op when canLoadMore is false`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { p, _ -> Result.success(page(salons(1..10), page = p, totalPages = 1)) }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.canLoadMore)

        vm.loadMore()
        advanceUntilIdle()

        assertEquals("only the initial page was ever requested", listOf(0), repo.pagesRequested)
    }

    @Test
    fun `loadMore before the first successful load does nothing`() = runTest(dispatcher) {
        val repo = ScriptedSalonRepository { _, _ -> Result.failure(RuntimeException("network")) }
        val vm = SalonListViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Error)

        vm.loadMore()
        advanceUntilIdle()

        assertEquals("no pagination request off a non-Success state", listOf(0), repo.pagesRequested)
    }
}
