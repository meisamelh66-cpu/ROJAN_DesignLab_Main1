package ai.rojan.designlab.ui

import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 5B-2A — semantics contracts for the shared accessibility
 * primitives. Instrumentation tests over the Compose semantics tree; run
 * on a device/emulator when one is available.
 */
@RunWith(AndroidJUnit4::class)
class SharedAccessibilitySemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun wrap(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
                RojanTheme { content() }
            }
        }
    }

    @Test
    fun rtlSectionHeader_exposesHeading() {
        wrap { RtlSectionHeader(text = "بخش نمونه") }
        composeTestRule.onNodeWithText("بخش نمونه")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun glassBackButton_exposesRoleButtonAndClickAction() {
        var clicked = false
        wrap { GlassBackButton(onClick = { clicked = true }) }
        composeTestRule.onNodeWithContentDescription("بازگشت")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun rojanPressable_withRoleButton_exposesRoleButton() {
        wrap {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .rojanPressable(onClick = {}, role = Role.Button),
            ) {
                Text("دکمهٔ نمونه")
            }
        }
        composeTestRule.onNodeWithText("دکمهٔ نمونه")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun rojanPressable_withoutRole_hasNoRole() {
        wrap {
            Box(modifier = Modifier.size(24.dp).rojanPressable(onClick = {})) {
                Text("بدون نقش")
            }
        }
        composeTestRule.onNodeWithText("بدون نقش")
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun politeLiveRegionErrorPattern_isExposed() {
        // The exact semantics the three auth error Texts (AuthScreen /
        // ManagerOtpAuthScreen / ReceptionOtpAuthScreen) now apply.
        wrap {
            Text(
                text = "کد نادرست است",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        composeTestRule.onNodeWithText("کد نادرست است")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }
}
