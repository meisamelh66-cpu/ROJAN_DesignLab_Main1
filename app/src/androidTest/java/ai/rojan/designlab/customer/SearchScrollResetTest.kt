package ai.rojan.designlab.customer

import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.screens.booking.SalonListScreen
import ai.rojan.designlab.screens.search.SearchScreen
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T2-1 regression: changing the search query while scrolled down must land
 * the new result set at the top of the list, while paginating (loadMore)
 * must NOT move the user.
 *
 * Instrumentation test — runs on a device/emulator when one is available.
 * It drives the real query-change path (typing into the field, letting the
 * debounce elapse, a fresh page-0 result set arriving) rather than calling
 * any scroll API directly.
 */
@RunWith(AndroidJUnit4::class)
class SearchScrollResetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---- fake backend -------------------------------------------------

    /**
     * page 0 + no name filter -> [initial]; page 0 + a name filter ->
     * [search]; page >= 1 -> [nextPage] (appended by loadMore). Every set
     * is long enough that scroll position is observable through which rows
     * are composed.
     */
    private class FakeSalonRepository : SalonRepository {
        val initial = salons("SalonInitial", 25)
        val search = salons("SalonSearch", 25)
        val nextPage = salons("SalonPage1", 20)

        override suspend fun browseSalons(
            page: Int,
            size: Int,
            nameFilter: String?,
            sortDirection: String,
        ): Result<PagedResult<Salon>> {
            val content = when {
                page >= 1 -> nextPage
                !nameFilter.isNullOrBlank() -> search
                else -> initial
            }
            // totalPages = 3 keeps canLoadMore true throughout.
            return Result.success(PagedResult(content, page, size, 50L, 3))
        }

        override suspend fun getSalon(salonId: String): Result<Salon> = error("not used")

        companion object {
            fun salons(prefix: String, count: Int) = List(count) { i ->
                Salon(
                    id = "$prefix-$i",
                    name = "$prefix-$i",
                    description = null,
                    phone = "0",
                    email = null,
                    address = "addr",
                )
            }
        }
    }

    private fun render(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
                RojanTheme(content = content)
            }
        }
    }

    private fun awaitText(text: String) = composeTestRule.waitUntil(TIMEOUT_MS) {
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    // ---- A. re-query resets scroll ---------------------------------

    @Test
    fun reQuery_resetsScrollToTop_onSearchScreen() {
        val viewModel = SalonListViewModel(FakeSalonRepository())
        render {
            SearchScreen(onBackClick = {}, onSalonClick = {}, viewModel = viewModel)
        }

        // initial 25-salon result set is loaded
        awaitText("SalonInitial-0")

        // scroll well down the list
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(18)
        awaitText("SalonInitial-18")
        composeTestRule.onAllNodesWithText("SalonInitial-0").assertCountEquals(0)

        // change the query -> debounce -> fresh page-0 result set
        composeTestRule.onNode(hasSetTextAction()).performTextInput("spa")
        awaitText("SalonSearch-0")

        // the new first result is at the top, visible
        composeTestRule.onNodeWithText("SalonSearch-0").assertIsDisplayed()
        // a deep row of the NEW set is not composed -> we are at the top, not
        // still parked at the old offset (this is the assertion that fails
        // against the pre-fix code)
        composeTestRule.onAllNodesWithText("SalonSearch-18").assertCountEquals(0)
        // old set is fully gone
        composeTestRule.onAllNodesWithText("SalonInitial-18").assertCountEquals(0)
    }

    @Test
    fun reQuery_resetsScrollToTop_onSalonListScreen() {
        val viewModel = SalonListViewModel(FakeSalonRepository())
        render {
            SalonListScreen(
                selectedServiceIds = emptyList(),
                onBackClick = {},
                onSalonSelected = {},
                viewModel = viewModel,
            )
        }

        awaitText("SalonInitial-0")
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(18)
        awaitText("SalonInitial-18")

        composeTestRule.onNode(hasSetTextAction()).performTextInput("spa")
        awaitText("SalonSearch-0")

        composeTestRule.onNodeWithText("SalonSearch-0").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("SalonSearch-18").assertCountEquals(0)
    }

    // ---- B. pagination must NOT reset scroll ----------------------

    @Test
    fun pagination_preservesScrollPosition() {
        val viewModel = SalonListViewModel(FakeSalonRepository())
        render {
            SearchScreen(onBackClick = {}, onSalonClick = {}, viewModel = viewModel)
        }

        awaitText("SalonInitial-0")

        // scroll near the end -> triggers loadMore() (lastVisible >= total - 4)
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(22)
        awaitText("SalonInitial-22")

        // the appended page arrives
        awaitText("SalonPage1-0")

        // the user is still where they were — NOT yanked to the top
        composeTestRule.onNodeWithText("SalonInitial-22").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("SalonInitial-0").assertCountEquals(0)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
