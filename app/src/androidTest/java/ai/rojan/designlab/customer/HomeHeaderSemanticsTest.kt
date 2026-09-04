package ai.rojan.designlab.customer

import ai.rojan.designlab.screens.customer.HomeHeader
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 5B-2B — the Home header notifications control keeps its visible
 * glass chip but exposes a >= 48dp interaction / semantics target and
 * Role.Button. Instrumentation test; runs on a device/emulator when one
 * is available.
 */
@RunWith(AndroidJUnit4::class)
class HomeHeaderSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notificationsButton_hasRoleButtonAndAtLeast48dpTarget() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
                RojanTheme {
                    HomeHeader(
                        displayName = "سارا",
                        onNotificationsClick = { clicked = true },
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("اعلان‌ها")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(clicked)
    }
}
