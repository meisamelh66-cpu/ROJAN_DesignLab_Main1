package ai.rojan.designlab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 5B-3b — executable proof of the behaviour the stable-key sweep
 * protects: with a stable `key` on a dynamic `LazyColumn`/`LazyRow`, an
 * item's own remembered state stays attached to its data object when
 * earlier items are removed or filtered out; without a key, remembered
 * state is retained by slot position and visibly leaks onto whatever data
 * now occupies that slot.
 *
 * This mirrors, in miniature, every list touched by 5B-3b that carries
 * per-item `remember` state (e.g. AppointmentsScreen's cancel-confirm
 * dialog) or a per-item entrance animation.
 *
 * Instrumentation test — runs on a device/emulator when one is available.
 */
@RunWith(AndroidJUnit4::class)
class LazyListStableKeyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** One row with its own remembered on/off state, toggled by a click. */
    @Composable
    private fun ToggleRow(id: String) {
        var on by remember { mutableStateOf(false) }
        Text(
            text = "$id:${if (on) "ON" else "OFF"}",
            modifier = Modifier
                .fillMaxWidth()
                .testTag("row-$id")
                .clickable { on = !on },
        )
    }

    @Test
    fun stableKey_perItemState_tracksTheDataItemThroughRemoval() {
        val ids = mutableStateListOf("A", "B", "C", "D", "E")
        composeTestRule.setContent {
            LazyColumn {
                items(ids, key = { it }) { id -> ToggleRow(id) }
            }
        }

        // turn C on
        composeTestRule.onNodeWithTag("row-C").performClick()
        composeTestRule.onNodeWithTag("row-C").assertTextEquals("C:ON")

        // remove the first item — C shifts from slot 2 to slot 1
        composeTestRule.runOnIdle { ids.remove("A") }

        // C keeps its own state; its new and old neighbours stay off
        composeTestRule.onNodeWithTag("row-C").assertTextEquals("C:ON")
        composeTestRule.onNodeWithTag("row-B").assertTextEquals("B:OFF")
        composeTestRule.onNodeWithTag("row-D").assertTextEquals("D:OFF")
    }

    @Test
    fun withoutKey_perItemState_leaksToWhateverTakesTheSlot() {
        val ids = mutableStateListOf("A", "B", "C", "D", "E")
        composeTestRule.setContent {
            LazyColumn {
                // deliberately keyless — the pre-5B-3b shape
                items(ids) { id -> ToggleRow(id) }
            }
        }

        composeTestRule.onNodeWithTag("row-C").performClick()
        composeTestRule.onNodeWithTag("row-C").assertTextEquals("C:ON")

        composeTestRule.runOnIdle { ids.remove("A") }

        // slot 2 now holds D, and the remembered "on" state stayed with the slot
        composeTestRule.onNodeWithTag("row-D").assertTextEquals("D:ON")
        composeTestRule.onNodeWithTag("row-C").assertTextEquals("C:OFF")
    }
}
