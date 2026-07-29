package ai.rojan.designlab.manager

import ai.rojan.designlab.manager.navigation.ManagerDestinations
import ai.rojan.designlab.manager.navigation.managerNavGraph
import ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen
import ai.rojan.designlab.manager.screens.splash.ManagerSplashScreen
import ai.rojan.designlab.ui.theme.RojanTheme
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * One-off visual-verification aid for the Manager Dashboard v1.0 UI pass
 * (RQG "provide a screenshot" requirement) — renders the screen in
 * isolation (no navigation/Activity wiring needed) and writes PNGs to
 * the app's external files dir for `adb pull`. Not a real regression
 * test (no assertions) — safe to remove once the screenshots have been
 * reviewed.
 *
 * Final audit pass: captures both the initial viewport (Header/Salon
 * Identity/KPI/Quick Actions) and a scrolled-down viewport (AI Insight/
 * Calendar Preview, which sit below the fold and were never actually
 * seen in earlier passes), plus a 1.3x font-scale pass (via
 * [CompositionLocalProvider]/[LocalDensity] — no device accessibility
 * settings touched) to check for text clipping/overflow at larger
 * system font sizes.
 */
@RunWith(AndroidJUnit4::class)
class ManagerDashboardScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureManagerDashboard() {
        composeTestRule.setContent {
            RojanTheme {
                ManagerDashboardScreen()
            }
        }
        captureTopAndScrolled(suffix = "")
    }

    @Test
    fun captureManagerDashboardLargeFont() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides androidx.compose.ui.unit.Density(
                    density = density.density,
                    fontScale = 1.3f,
                ),
            ) {
                RojanTheme {
                    ManagerDashboardScreen()
                }
            }
        }
        captureTopAndScrolled(suffix = "_largefont")
    }

    @Test
    fun captureManagerSplash() {
        composeTestRule.setContent {
            RojanTheme {
                ManagerSplashScreen()
            }
        }
        composeTestRule.waitForIdle()
        saveBitmap("manager_splash.png")
    }

    @Test
    fun captureManagerProfile() {
        composeTestRule.setContent {
            RojanTheme {
                ManagerProfileScreen()
            }
        }
        composeTestRule.waitForIdle()
        saveBitmap("manager_profile.png")
    }

    @Test
    fun captureManagerCalendar() {
        composeTestRule.setContent {
            RojanTheme {
                ManagerCalendarScreen()
            }
        }
        composeTestRule.waitForIdle()
        saveBitmap("manager_calendar_daily.png")

        composeTestRule.onNodeWithText("هفتگی").performClick()
        composeTestRule.waitForIdle()
        saveBitmap("manager_calendar_weekly.png")
    }

    /**
     * The actual thing this pass needs verified: tapping Dashboard's
     * "مشاهده تقویم کامل" CTA, through the real [managerNavGraph] +
     * [NavHostController] (not just the standalone screen), lands on
     * [ManagerDestinations.CALENDAR]. Runs on the connected device like
     * every other test here — `managerNavGraph` isn't hooked into the
     * app's real entry point yet (a separate, later step), so this is
     * the correct way to exercise the wiring itself on real hardware.
     */
    @Test
    fun captureManagerCalendarEntryNavigation() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            RojanTheme {
                NavHost(navController = navController, startDestination = ManagerDestinations.DASHBOARD) {
                    managerNavGraph(navController)
                }
            }
        }
        composeTestRule.waitForIdle()

        repeat(2) {
            composeTestRule.onRoot().performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }
        saveBitmap("manager_nav_dashboard_before_click.png")

        composeTestRule.onNodeWithText("مشاهده تقویم کامل").performClick()
        composeTestRule.waitForIdle()

        assertEquals(ManagerDestinations.CALENDAR, navController.currentDestination?.route)
        saveBitmap("manager_nav_after_calendar_click.png")
    }

    private fun captureTopAndScrolled(suffix: String) {
        composeTestRule.waitForIdle()
        saveBitmap("manager_dashboard_top$suffix.png")

        repeat(2) {
            composeTestRule.onRoot().performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }
        saveBitmap("manager_dashboard_bottom$suffix.png")
    }

    private fun saveBitmap(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), fileName)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
