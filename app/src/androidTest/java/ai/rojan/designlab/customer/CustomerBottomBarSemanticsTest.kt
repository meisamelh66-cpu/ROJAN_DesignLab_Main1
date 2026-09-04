package ai.rojan.designlab.customer

import ai.rojan.designlab.screens.customer.CustomerBottomBar
import ai.rojan.designlab.screens.customer.CustomerHomeTab
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 5B-1 — accessibility semantics for [CustomerBottomBar], the
 * primary Customer navigation control. Instrumentation test over the
 * Compose semantics tree; runs on a device/emulator when one is
 * available.
 */
@RunWith(AndroidJUnit4::class)
class CustomerBottomBarSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun setBar(
        active: CustomerHomeTab,
        onTab: (CustomerHomeTab) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
                RojanTheme {
                    CustomerBottomBar(activeTab = active, onTabSelected = onTab)
                }
            }
        }
    }

    @Test
    fun exposesFiveTabsWithRoleTab() {
        setBar(active = CustomerHomeTab.SEARCH)
        composeTestRule.onAllNodes(isSelectable()).assertCountEquals(5)
        composeTestRule.onAllNodes(tabRole).assertCountEquals(5)
    }

    @Test
    fun activeTabIsSelected_othersAreNot() {
        setBar(active = CustomerHomeTab.SEARCH)
        composeTestRule.onNodeWithContentDescription("جستجو").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("پروفایل").assertIsNotSelected()
        composeTestRule.onNodeWithContentDescription("خانه").assertIsNotSelected()
    }

    @Test
    fun homeTabReflectsActiveState() {
        setBar(active = CustomerHomeTab.HOME)
        composeTestRule.onNodeWithContentDescription("خانه").assertIsSelected()
    }

    @Test
    fun everyTabHasAClickActionAndReportsSelection() {
        val clicks = mutableListOf<CustomerHomeTab>()
        setBar(active = CustomerHomeTab.HOME, onTab = { clicks += it })

        listOf(
            "پروفایل" to CustomerHomeTab.PROFILE,
            "علاقه‌ها" to CustomerHomeTab.FAVORITES,
            "نوبت‌ها" to CustomerHomeTab.BOOKINGS,
            "جستجو" to CustomerHomeTab.SEARCH,
        ).forEach { (contentDescription, tab) ->
            composeTestRule.onNodeWithContentDescription(contentDescription)
                .assertHasClickAction()
                .performClick()
            assertEquals(tab, clicks.last())
        }
    }

    @Test
    fun homeFabRemainsClickable() {
        val clicks = mutableListOf<CustomerHomeTab>()
        setBar(active = CustomerHomeTab.SEARCH, onTab = { clicks += it })
        composeTestRule.onNodeWithContentDescription("خانه")
            .assertHasClickAction()
            .performClick()
        assertEquals(CustomerHomeTab.HOME, clicks.last())
    }
}
